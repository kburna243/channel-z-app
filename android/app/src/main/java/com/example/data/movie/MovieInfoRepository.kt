package com.example.data.movie

import android.util.Log
import com.example.data.model.MovieInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private const val TAG = "MovieInfoRepository"

/**
 * Beschafft Angaben zum laufenden Film — ohne API-Schluessel.
 *
 * Zwei Quellen mit klarer Aufgabenteilung:
 *
 *  - **Wikidata** loest den rohen Titel auf. Offene API, keine Registrierung, und ueber die
 *    Eigenschaft P345 kommt die IMDb-Kennung heraus. Das ist die Bruecke.
 *  - **IMDb** liefert danach Eckdaten, Plakat, Bewertung und Trivia. Der GraphQL-Endpunkt der
 *    Website akzeptiert eigene Abfragen und braucht keinen Schluessel, ist aber undokumentiert
 *    und nicht fuer fremde Nutzung vorgesehen. Deshalb laesst er sich abschalten
 *    (AppSettings.imdbEnabled); ohne ihn bleiben die Wikidata-Eckdaten uebrig.
 *
 * Ergebnisse werden je Titel gemerkt: CyTube meldet denselben Film waehrend der Laufzeit
 * dutzendfach, nachgefragt wird aber nur einmal.
 */
class MovieInfoRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) {
    private val cache = mutableMapOf<String, MovieInfo>()

    /** Wikidata-Klassen, die als Film oder Serie durchgehen. */
    private val filmClasses = setOf(
        "Q11424",    // Film
        "Q5398426",  // Fernsehserie
        "Q21191270", // Fernsehserien-Episode
        "Q506240",   // Fernsehfilm
        "Q24856",    // Filmreihe
        "Q226730",   // Direct-to-Video
        "Q1261214"   // Kurzfilm
    )

    /**
     * Angaben zu einem YouTube-Video ueber den oEmbed-Dienst — Titel, Kanal und Vorschaubild,
     * ohne Schluessel. Fuer Trailer, Bumper und Werbespots im Kanal ist das die passende Quelle:
     * die stehen in keiner Filmdatenbank, haben aber auf YouTube saubere Angaben.
     */
    suspend fun lookupYouTube(videoId: String): MovieInfo? = withContext(ioDispatcher) {
        if (videoId.length != 11) return@withContext null
        cache["yt|$videoId"]?.let { return@withContext it }
        try {
            val url = "https://www.youtube.com/oembed?url=" +
                "https://www.youtube.com/watch?v=$videoId".urlEncoded() + "&format=json"
            val body = httpGet(url) ?: return@withContext null
            val json = JSONObject(body)
            val info = MovieInfo(
                query = videoId,
                title = json.optString("title").takeIf { it.isNotEmpty() },
                directors = listOfNotNull(json.optString("author_name").takeIf { it.isNotEmpty() }),
                posterUrl = json.optString("thumbnail_url").takeIf { it.isNotEmpty() },
                posterIsWide = true
            )
            if (info.title == null) return@withContext null
            cache["yt|$videoId"] = info
            info
        } catch (e: Exception) {
            Log.w(TAG, "YouTube-oEmbed fehlgeschlagen fuer $videoId: ${e.message}")
            null
        }
    }

    suspend fun lookup(rawTitle: String, useImdb: Boolean): MovieInfo? = withContext(ioDispatcher) {
        val parsed = parseTitle(rawTitle)
        if (parsed.title.length < 2) return@withContext null

        val key = "${parsed.title}|${parsed.year}|$useImdb"
        cache[key]?.let { return@withContext it }

        val fromWikidata = try {
            resolveViaWikidata(parsed)
        } catch (e: Exception) {
            Log.w(TAG, "Wikidata-Abfrage fehlgeschlagen fuer '${parsed.title}': ${e.message}")
            null
        }

        // Wikidata kennt laengst nicht alles. "Werewolf" (1987) taucht dort unter den zehn
        // Suchtreffern gar nicht auf — nur Fabelwesen, Videospiele und ein Stummfilm von 1913.
        // IMDb findet die Serie und nennt dabei die Art des Werks, womit sich Podcasts,
        // Kurzfilme und Videospiele sauber aussortieren lassen.
        val base = fromWikidata ?: (
            if (useImdb) {
                try {
                    resolveViaImdbSearch(parsed)
                } catch (e: Exception) {
                    Log.w(TAG, "IMDb-Suche fehlgeschlagen fuer '${parsed.title}': ${e.message}")
                    null
                }
            } else null
        ) ?: return@withContext null

        val enriched = if (useImdb && base.imdbId != null) {
            try {
                enrichViaImdb(base)
            } catch (e: Exception) {
                Log.w(TAG, "IMDb-Abfrage fehlgeschlagen fuer ${base.imdbId}: ${e.message}")
                base
            }
        } else {
            base
        }

        cache[key] = enriched
        enriched
    }

    /**
     * Resolves movie details directly using a known IMDb tt number (e.g. from WebQueue/MediaCMS catalog).
     */
    suspend fun lookupFromImdbId(imdbId: String, rawTitle: String): MovieInfo? = withContext(ioDispatcher) {
        val cleanId = imdbId.trim()
        if (cleanId.isBlank()) return@withContext null
        val cacheKey = "imdb|$cleanId"
        cache[cacheKey]?.let { return@withContext it }

        val base = MovieInfo(
            query = rawTitle,
            title = rawTitle,
            imdbId = cleanId
        )
        val enriched = try {
            enrichViaImdb(base)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to enrich from direct IMDb ID $cleanId: ${e.message}")
            base
        }
        cache[cacheKey] = enriched
        enriched
    }

    /** Trivia wird erst geholt, wenn jemand sie sehen will — die Listen sind lang. */
    suspend fun loadTrivia(imdbId: String, limit: Int = 25): List<String> = withContext(ioDispatcher) {
        val query = """
            query GHTrivia(${'$'}id: ID!) {
              title(id: ${'$'}id) { trivia(first: $limit) { edges { node { text { plainText } } } } }
            }
        """.trimIndent()
        try {
            val root = imdbQuery("GHTrivia", query, imdbId) ?: return@withContext emptyList()
            val edges = root.optJSONObject("data")?.optJSONObject("title")
                ?.optJSONObject("trivia")?.optJSONArray("edges") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until edges.length()) {
                    val text = edges.optJSONObject(i)?.optJSONObject("node")
                        ?.optJSONObject("text")?.optString("plainText").orEmpty().trim()
                    if (text.isNotEmpty()) add(text)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Trivia-Abfrage fehlgeschlagen fuer $imdbId: ${e.message}")
            emptyList()
        }
    }

    // ---------------------------------------------------------------- Titel

    data class ParsedTitle(
        val title: String,
        val year: Int?,
        val isEpisode: Boolean,
        val season: Int? = null,
        val episode: Int? = null
    )

    /**
     * Schaelt aus einem rohen Titel den Werktitel heraus — bei Serien den Namen der Serie.
     *
     * Der Kanal mischt Dateinamen und YouTube-Titel, und die Folgenkennzeichnung sieht jedes
     * Mal anders aus. Diese Schreibweisen kommen dort tatsaechlich vor:
     *
     *   Archer S01E10 Dial M for Mother.mp4
     *   Werewolf 1x22 Skinwalker
     *   Swamp Thing    Episode10 Season 1   New Acquaintance
     *   Unsolved Mysteries with Robert Stack - Season 1 Episode 20 - Full Episode
     *   The Tomorrow People (1992) | The Rameses Connection Ep. 5 | 4K A.I. Remaster
     *
     * Aus allen fuenf muss der Serienname fallen, sonst sucht die Datenbank ins Leere und die
     * Folge steht ohne Angaben da — waehrend Filme welche haben.
     */
    internal fun parseTitle(raw: String): ParsedTitle {
        var s = raw.trim()
            .replace(Regex("""\.(mp4|mkv|avi|webm|mov|m4v)${'$'}""", RegexOption.IGNORE_CASE), "")
            .replace(
                Regex(
                    """\b(1080p|720p|480p|2160p|4k|bluray|blu-ray|brrip|dvdrip|dvdscr|webrip|web-dl|""" +
                        """hdtv|xvid|divx|x264|x265|h264|h265|hevc|aac|ac3|remastered|uncut|unrated|""" +
                        """extended|directors?.cut|repack|proper)\b""",
                    RegexOption.IGNORE_CASE
                ), " "
            )
            // Zusaetze, mit denen YouTube-Kanaele ihre Uploads schmuecken.
            .replace(
                Regex(
                    """\b(updated\s+)?full\s+episode\b|\ba\.?i\.?\s+remaster(ed)?\b|""" +
                        """\bremaster(ed)?\b|\bfull\s+series\b|\bofficial\b""",
                    RegexOption.IGNORE_CASE
                ), " "
            )

        val yearMatch = Regex("""[\[(]?\b((?:19|20)\d{2})\b[\])]?""").find(s)
        val year = yearMatch?.groupValues?.get(1)?.toIntOrNull()
        if (yearMatch != null) s = s.replaceRange(yearMatch.range, " ")

        // Folgenkennzeichnung in allen Schreibweisen, die im Kanal vorkommen. Die Zahlenpaare
        // sagen, in welcher Fanggruppe Staffel und Folge stehen (0 = nicht angegeben).
        val patterns = listOf(
            Regex("""\bS(\d{1,2})\s?E(\d{1,3})\b""", RegexOption.IGNORE_CASE) to Pair(1, 2),
            Regex("""\bSeason\s*(\d{1,2})\s*[,\-]?\s*Episode\s*(\d{1,3})\b""", RegexOption.IGNORE_CASE) to Pair(1, 2),
            Regex("""\bEpisode\s*(\d{1,3})\s*Season\s*(\d{1,2})\b""", RegexOption.IGNORE_CASE) to Pair(2, 1),
            Regex("""\b(\d{1,2})x(\d{1,3})\b""") to Pair(1, 2),
            Regex("""\bEp(isode)?\.?\s*(\d{1,3})\b""", RegexOption.IGNORE_CASE) to Pair(0, 2)
        )

        var season: Int? = null
        var episodeNo: Int? = null
        var cutAt: Int? = null
        for ((pattern, groups) in patterns) {
            val hit = pattern.find(s) ?: continue
            season = if (groups.first > 0) hit.groupValues.getOrNull(groups.first)?.toIntOrNull() else null
            episodeNo = hit.groupValues.getOrNull(groups.second)?.toIntOrNull()
            cutAt = hit.range.first
            break
        }
        if (cutAt != null) s = s.substring(0, cutAt)

        // Bleibt ein senkrechter Strich uebrig, steht der Serienname im ersten Abschnitt.
        if (s.contains('|')) s = s.substringBefore('|')

        s = s.replace(Regex("""[._]+"""), " ")
            .replace(Regex("""[\[\]()]"""), " ")
            // Szene-Releases haengen den Gruppennamen hinten an ("... x264-GROUP"). Nur mit
            // Leerzeichen davor entfernen, sonst faellt "Spider-Man" mit dem Rest weg.
            .replace(Regex("""\s+[-–—]\s*\w+\s*${'$'}"""), " ")
            .replace(Regex("""\s{2,}"""), " ")
            .trim(' ', '-', '–', '—', ',', '|')

        return ParsedTitle(s, year, cutAt != null, season, episodeNo)
    }

    // ------------------------------------------------------------ Wikidata

    /** Ein Suchtreffer, wie ihn Wikidata liefert — mit der Kurzbeschreibung zum Einordnen. */
    private data class Candidate(val qid: String, val label: String, val description: String)

    private fun resolveViaWikidata(parsed: ParsedTitle): MovieInfo? {
        // Erster Versuch mit dem gefundenen Namen; bringt der nichts, wird ein Zusatz wie
        // "… with Robert Stack" abgeschnitten. Solche Beisaetze stehen oft im YouTube-Titel,
        // aber nicht im Datenbankeintrag.
        val attempts = buildList {
            add(parsed.title)
            val withoutHost = parsed.title.replace(Regex("""\s+with\s+.*${'$'}""", RegexOption.IGNORE_CASE), "").trim()
            if (withoutHost.length in 3 until parsed.title.length) add(withoutHost)
        }

        for (attempt in attempts) {
            val ranked = wikidataSearch(attempt)
                .map { it to scoreCandidate(it, parsed) }
                .filter { it.second > Int.MIN_VALUE }
                .sortedByDescending { it.second }

            for ((candidate, _) in ranked.take(3)) {
                val info = loadEntity(candidate, parsed)
                if (info != null) return info
            }
        }
        return null
    }

    /**
     * Bewertet einen Suchtreffer anhand seiner Kurzbeschreibung.
     *
     * Das spart Abfragen — ohne diese Vorsortierung muesste jede Entitaet einzeln geladen
     * werden — und trifft deutlich besser: bei "Archer" steht die Serie erst an siebter Stelle,
     * hinter Familienname, Vorname, einer Stadt und einem Panzer.
     */
    private fun scoreCandidate(candidate: Candidate, parsed: ParsedTitle): Int {
        val description = candidate.description.lowercase()

        // Staffel-Eintraege sind eigene Objekte in Wikidata und fuehren in die Irre.
        if (description.startsWith("season of") || description.contains("season of ")) {
            return Int.MIN_VALUE
        }

        var score = 0
        val looksLikeSeries = Regex("""series|programme|program\b|sitcom|anime""").containsMatchIn(description)
        val looksLikeFilm = Regex("""\bfilm\b|movie""").containsMatchIn(description)

        if (parsed.isEpisode) {
            if (looksLikeSeries) score += 100
            if (looksLikeFilm) score += 20
        } else {
            if (looksLikeFilm) score += 100
            if (looksLikeSeries) score += 60
        }

        // Erscheinungsjahr steht meist mit in der Beschreibung ("1990 American television series").
        parsed.year?.let { year ->
            if (description.contains(year.toString())) score += 80
        }

        // Genau passender Name schlaegt einen bloss aehnlichen.
        if (candidate.label.equals(parsed.title, ignoreCase = true)) score += 30

        return score
    }

    /** Laedt einen Kandidaten nach und macht daraus die Filminfo — falls er wirklich passt. */
    private fun loadEntity(candidate: Candidate, parsed: ParsedTitle): MovieInfo? {
        val entity = wikidataEntity(candidate.qid) ?: return null
        val claims = entity.optJSONObject("claims") ?: return null

        val instanceOf = claimIds(claims, "P31")
        val imdbId = claimStrings(claims, "P345").firstOrNull()
        if (instanceOf.none { it in filmClasses } && imdbId == null) return null

        val years = claimTimes(claims, "P577")
        // Jahr nur als Ausschlusskriterium nutzen, wenn beide Seiten eines kennen.
        if (parsed.year != null && years.isNotEmpty() && parsed.year !in years) return null

        val directorIds = claimIds(claims, "P57") + claimIds(claims, "P170")
        val genreIds = claimIds(claims, "P136")
        val labels = wikidataLabels(directorIds.take(3) + genreIds.take(4))

        return MovieInfo(
            query = parsed.title,
            season = parsed.season,
            episode = parsed.episode,
            title = entity.optJSONObject("labels")?.optJSONObject("en")?.optString("value")
                ?: parsed.title,
            year = years.firstOrNull() ?: parsed.year,
            runtimeMinutes = claimQuantity(claims, "P2047")?.toInt(),
            directors = directorIds.mapNotNull { labels[it] }.distinct(),
            genres = genreIds.mapNotNull { labels[it] },
            imdbId = imdbId
        )
    }

    private fun wikidataSearch(title: String): List<Candidate> {
        val url = "https://www.wikidata.org/w/api.php?action=wbsearchentities&format=json" +
            "&language=en&uselang=en&limit=10&type=item&search=${title.urlEncoded()}"
        val body = httpGet(url) ?: return emptyList()
        val arr = JSONObject(body).optJSONArray("search") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val obj = arr.optJSONObject(i) ?: return@mapNotNull null
            val qid = obj.optString("id").orEmpty()
            if (qid.isEmpty()) null else Candidate(
                qid = qid,
                label = obj.optString("label").orEmpty(),
                description = obj.optString("description").orEmpty()
            )
        }
    }

    private fun wikidataEntity(qid: String): JSONObject? {
        val body = httpGet("https://www.wikidata.org/wiki/Special:EntityData/$qid.json") ?: return null
        return JSONObject(body).optJSONObject("entities")?.optJSONObject(qid)
    }

    private fun wikidataLabels(ids: List<String>): Map<String, String> {
        if (ids.isEmpty()) return emptyMap()
        val url = "https://www.wikidata.org/w/api.php?action=wbgetentities&format=json" +
            "&props=labels&languages=en&ids=${ids.joinToString("|").urlEncoded()}"
        val body = httpGet(url) ?: return emptyMap()
        val entities = JSONObject(body).optJSONObject("entities") ?: return emptyMap()
        return buildMap {
            for (id in entities.keys()) {
                entities.optJSONObject(id)?.optJSONObject("labels")
                    ?.optJSONObject("en")?.optString("value")
                    ?.takeIf { it.isNotEmpty() }?.let { put(id, it) }
            }
        }
    }

    private fun claimArray(claims: JSONObject, property: String): JSONArray? = claims.optJSONArray(property)

    private fun claimValues(claims: JSONObject, property: String): List<Any> {
        val arr = claimArray(claims, property) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.optJSONObject("mainsnak")?.optJSONObject("datavalue")?.opt("value")
        }
    }

    private fun claimIds(claims: JSONObject, property: String): List<String> =
        claimValues(claims, property).mapNotNull { (it as? JSONObject)?.optString("id") }
            .filter { it.isNotEmpty() }

    private fun claimStrings(claims: JSONObject, property: String): List<String> =
        claimValues(claims, property).mapNotNull { it as? String }.filter { it.isNotEmpty() }

    private fun claimTimes(claims: JSONObject, property: String): List<Int> =
        claimValues(claims, property).mapNotNull { value ->
            (value as? JSONObject)?.optString("time")?.drop(1)?.take(4)?.toIntOrNull()
        }

    private fun claimQuantity(claims: JSONObject, property: String): Double? =
        claimValues(claims, property).firstNotNullOfOrNull { value ->
            (value as? JSONObject)?.optString("amount")?.removePrefix("+")?.toDoubleOrNull()
        }

    // ---------------------------------------------------------------- IMDb

    private fun enrichViaImdb(base: MovieInfo): MovieInfo {
        val id = base.imdbId ?: return base
        val query = """
            query GHInfo(${'$'}id: ID!) {
              title(id: ${'$'}id) {
                titleText { text }
                releaseYear { year }
                runtime { seconds }
                ratingsSummary { aggregateRating voteCount }
                primaryImage { url }
                plot { plotText { plainText } }
              }
            }
        """.trimIndent()

        val title = imdbQuery("GHInfo", query, id)
            ?.optJSONObject("data")?.optJSONObject("title") ?: return base

        val runtimeSeconds = title.optJSONObject("runtime")?.optInt("seconds", 0) ?: 0
        val ratings = title.optJSONObject("ratingsSummary")

        return base.copy(
            title = title.optJSONObject("titleText")?.optString("text")?.takeIf { it.isNotEmpty() }
                ?: base.title,
            year = title.optJSONObject("releaseYear")?.optInt("year")?.takeIf { it > 0 } ?: base.year,
            runtimeMinutes = if (runtimeSeconds > 0) runtimeSeconds / 60 else base.runtimeMinutes,
            rating = ratings?.optDouble("aggregateRating")?.takeIf { !it.isNaN() && it > 0.0 },
            voteCount = ratings?.optInt("voteCount")?.takeIf { it > 0 },
            plot = title.optJSONObject("plot")?.optJSONObject("plotText")
                ?.optString("plainText")?.takeIf { it.isNotEmpty() },
            posterUrl = title.optJSONObject("primaryImage")?.optString("url")?.takeIf { it.isNotEmpty() }
        )
    }

    /**
     * Sucht den Titel direkt bei IMDb und macht aus dem besten Treffer die Grundangaben.
     *
     * Kommt nur zum Zug, wenn Wikidata nichts liefert. Die Anreicherung (Plakat, Handlung,
     * Trivia) laeuft danach wie gewohnt ueber die IMDb-Kennung.
     */
    private fun resolveViaImdbSearch(parsed: ParsedTitle): MovieInfo? {
        val hits = imdbSearch(parsed.title)
        val best = bestImdbHit(hits, parsed) ?: return null

        return MovieInfo(
            query = parsed.title,
            season = parsed.season,
            episode = parsed.episode,
            title = best.title,
            year = best.year ?: parsed.year,
            imdbId = best.id
        )
    }

    /** Ein Suchtreffer von IMDb. `kind` ist die Art des Werks, etwa "TV Series" oder "Movie". */
    internal data class ImdbHit(val id: String, val title: String, val year: Int?, val kind: String)

    private fun bestImdbHit(hits: List<ImdbHit>, parsed: ParsedTitle): ImdbHit? =
        hits.map { it to scoreImdbHit(it, parsed) }
            .filter { it.second > Int.MIN_VALUE }
            .maxByOrNull { it.second }
            ?.first

    /**
     * Auswahl ohne Netz, fuer die Pruefung: nimmt Kennung, Jahr und Art des Werks und sagt,
     * welcher Treffer gemeint ist.
     */
    internal fun chooseImdbHit(
        parsed: ParsedTitle,
        hits: List<Triple<String, Int?, String>>
    ): String? = bestImdbHit(
        hits.map { (id, year, kind) -> ImdbHit(id = id, title = parsed.title, year = year, kind = kind) },
        parsed
    )?.id

    /**
     * Bewertet einen IMDb-Treffer. Die Art des Werks traegt die Entscheidung: bei einer
     * Folgenangabe im Titel ist eine Serie gemeint, sonst ein Film. Podcasts und Kurzfilme
     * tragen denselben Namen wie das gesuchte Werk und muessen raus — bei "Strangeland"
     * stehen zwei Podcasts unter den ersten fuenf Treffern.
     */
    private fun scoreImdbHit(hit: ImdbHit, parsed: ParsedTitle): Int {
        val kind = hit.kind.lowercase()
        if (kind.contains("podcast") || kind.contains("video game") || kind.contains("short")) {
            return Int.MIN_VALUE
        }

        var score = 0
        val isSeries = kind.contains("series") || kind.contains("mini")
        val isFilm = kind.contains("movie") || kind == "video"

        if (parsed.isEpisode) {
            if (isSeries) score += 100
            if (isFilm) score += 10
        } else {
            if (isFilm) score += 100
            if (isSeries) score += 50
        }

        // Das Jahr entscheidet, wenn mehrere Fassungen denselben Namen tragen — "Swamp Thing"
        // gibt es als Film von 1982 und als Serien von 1990 und 2019.
        parsed.year?.let { year -> if (hit.year == year) score += 90 }
        if (hit.title.equals(parsed.title, ignoreCase = true)) score += 40

        return score
    }

    private fun imdbSearch(term: String): List<ImdbHit> {
        val query = """query ChannelZSearch(${'$'}t: String!) {
            mainSearch(first: 5, options: {searchTerm: ${'$'}t, type: [TITLE]}) {
                edges {
                    node {
                        entity {
                            ... on Title {
                                id
                                titleText { text }
                                releaseYear { year }
                                runtime { seconds }
                                titleType { id }
                            }
                        }
                    }
                }
            }
        }""".trimIndent()
        val variables = """{"t": "$term"}"""
        val url = "https://caching.graphql.imdb.com/?operationName=ChannelZSearch" +
            "&variables=" + java.net.URLEncoder.encode(variables, "UTF-8") +
            "&extensions=" + java.net.URLEncoder.encode("""{"persistedQuery":{"version":1,"sha256Hash":"c9f59f6b92f7a09454e5b22b10a26e84d43615e4f454406a6c2f3d5ea4a61352"}}""", "UTF-8")

        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Accept", "application/json")
            .build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) return emptyList()
        val body = resp.body?.string() ?: return emptyList()

        val edges = JSONObject(body).optJSONObject("data")
            ?.optJSONObject("mainSearch")?.optJSONArray("edges") ?: return emptyList()

        return (0 until edges.length()).mapNotNull { i ->
            val entity = edges.optJSONObject(i)?.optJSONObject("node")?.optJSONObject("entity")
                ?: return@mapNotNull null
            val id = entity.optString("id").takeIf { it.startsWith("tt") } ?: return@mapNotNull null
            ImdbHit(
                id = id,
                title = entity.optJSONObject("titleText")?.optString("text").orEmpty(),
                year = entity.optJSONObject("releaseYear")?.optInt("year")?.takeIf { it > 1800 },
                kind = entity.optJSONObject("titleType")?.optString("text").orEmpty()
            )
        }
    }

    private fun imdbQuery(operation: String, query: String, id: String): JSONObject? {
        val url = "https://caching.graphql.imdb.com/?operationName=$operation" +
            "&query=${query.urlEncoded()}" +
            "&variables=${JSONObject().put("id", id).toString().urlEncoded()}"
        val body = httpGet(url, imdbHeaders) ?: return null
        return JSONObject(body)
    }

    /** IMDb verlangt diese Kopfzeilen, sonst antwortet der Endpunkt nicht. */
    private val imdbHeaders = mapOf(
        "Accept" to "application/graphql+json, application/json",
        "Content-Type" to "application/json",
        "x-imdb-client-name" to "imdb-web-next-localized",
        "x-imdb-user-language" to "en-US",
        "x-imdb-user-country" to "US"
    )

    // -------------------------------------------------------------- HTTP

    private fun httpGet(url: String, headers: Map<String, String> = emptyMap()): String? {
        val builder = Request.Builder().url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "en")
        headers.forEach { (k, v) -> builder.header(k, v) }
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "HTTP ${response.code} fuer ${url.take(80)}")
                return null
            }
            return response.body?.string()
        }
    }

    /**
     * URLEncoder kodiert Leerzeichen als "+", was in einem Query-String zwar zulaessig ist, den
     * GraphQL-Parser von IMDb aber mit echten Plus-Zeichen fuettert — Antwort: HTTP 400.
     * Deshalb auf die Prozent-Schreibweise umstellen.
     */
    private fun String.urlEncoded(): String =
        URLEncoder.encode(this, "UTF-8").replace("+", "%20")

    private companion object {
        // Wikimedia verlangt eine benennbare Kennung mit Kontaktmoeglichkeit.
        const val USER_AGENT =
            "ChannelZ-App/1.0 (https://github.com/kburna243/channel-z-app)"
    }
}
