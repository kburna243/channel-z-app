package com.example.ui.player

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.MediaItem
import com.example.data.model.MediaSyncUpdate
import com.example.ui.theme.MidnightCanvas
import com.example.player.leadFor

private const val TAG = "VideoPlayerSurface"

/**
 * Hat das Geraet einen Hardware-Decoder fuer AV1?
 *
 * Der Fire TV Stick 4K Max bringt einen mit (OMX.MTK.VIDEO.DECODER.AV1). Auf solchen Geraeten
 * ist es schaedlich, YouTube AV1 zu verbieten: die Wiedergabe faellt dann auf VP9 oder H.264
 * zurueck, was mehr Bandbreite kostet und bei knappem Speicher ins Stottern fuehrt. Auf
 * Geraeten ohne AV1-Hardware bleibt die Sperre dagegen richtig — dort landet AV1 sonst im
 * Software-Decoder.
 */
private val hasAv1HardwareDecoder: Boolean by lazy {
    try {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
            !info.isEncoder &&
                info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AV1, ignoreCase = true) } &&
                !info.name.startsWith("c2.android.", ignoreCase = true) &&
                !info.name.startsWith("OMX.google.", ignoreCase = true)
        }
    } catch (e: Exception) {
        Log.w(TAG, "AV1-Codec-Pruefung fehlgeschlagen: ${e.message}")
        false
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerSurface(
    exoPlayer: ExoPlayer?,
    mediaItem: MediaItem?,
    isPlaying: Boolean = true,
    isMuted: Boolean = false,
    subtitlesEnabled: Boolean = true,
    mediaSyncUpdate: MediaSyncUpdate? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isWebStream = mediaItem?.isWebStream == true
    val currentMediaId = mediaItem?.id ?: ""

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightCanvas)
    ) {
        if (isWebStream && mediaItem != null && currentMediaId.isNotEmpty()) {
            // Rebuild-Token: wird hochgezählt, wenn der WebView-Renderer stirbt (OOM auf Fire TV /
            // Tablets). Ohne das bleibt der WebView für immer eingefroren — genau das Symptom
            // "Video hängt und läuft erst mit dem nächsten Video weiter".
            var rebuildToken by remember(currentMediaId) { mutableIntStateOf(0) }

            // key(currentMediaId, subtitlesEnabled) decouples playback from socket time ticks
            // UND erzwingt einen WebView-Neuaufbau bei Untertitel-Toggle.
            // Bug #3-Fix: unloadModule('captions') via postMessage ist unzuverlässig — der Neuaufbau
            // setzt cc_load_policy im iframe-src verlässlich um (cc_load_policy=0 deaktiviert CC).
            // ponytail: Video springt auf mediaItem.currentTimeSeconds zurück, akzeptabel für seltene Toggle.
            key(currentMediaId, subtitlesEnabled, rebuildToken) {
                val webViewRef = remember { mutableStateOf<WebView?>(null) }

                // Play/Mute NUR bei echter Zustandsänderung senden. Vorher lag das in AndroidView.update,
                // das bei jedem CyTube-mediaUpdate-Tick (~alle 5s) feuerte und dabei auch
                // loadModule('captions') + setOption neu abschickte → Micro-Stutter und Player-Stalls.
                // webViewRef.value als Key: steht der WebView noch nicht (Neuaufbau nach
                // Renderer-Crash oder Videowechsel), ging der Befehl bisher verloren — der
                // Effect lief nur bei isPlaying/isMuted-Wechsel neu. Jetzt feuert er auch,
                // sobald der WebView bereit ist, und holt den Zustand nach.
                LaunchedEffect(webViewRef.value, isPlaying, isMuted) {
                    val webView = webViewRef.value ?: return@LaunchedEffect
                    val playCommand = if (isPlaying) "playVideo" else "pauseVideo"
                    val muteCommand = if (isMuted) "mute" else "unMute"
                    webView.evaluateJavascript(
                        """
                        try {
                            var iframe = document.getElementById('player_iframe');
                            if (iframe && iframe.contentWindow) {
                                iframe.contentWindow.postMessage(JSON.stringify({'event': 'command', 'func': '$playCommand', 'args': []}), '*');
                                iframe.contentWindow.postMessage(JSON.stringify({'event': 'command', 'func': '$muteCommand', 'args': []}), '*');
                            }
                        } catch(e) {}
                        """.trimIndent(),
                        null
                    )
                }

                // CyTube Server mediaUpdate: Zeit-Drift & Play/Pause mit YouTube-Iframe synchronisieren
                LaunchedEffect(mediaSyncUpdate) {
                    val update = mediaSyncUpdate ?: return@LaunchedEffect
                    val webView = webViewRef.value ?: return@LaunchedEffect
                    webView.evaluateJavascript(
                        "try { if (window.syncPlayback) window.syncPlayback(${update.currentTimeSeconds}, ${update.paused}, 3.0); } catch(e) {}",
                        null
                    )
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.BLACK)
                            keepScreenOn = true

                            // Prevent WebView from capturing D-Pad focus or swallowing Compose tablet touch events
                            isFocusable = false
                            isFocusableInTouchMode = false
                            isClickable = false
                            setOnTouchListener { _, _ -> false }

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                cookieManager.setAcceptThirdPartyCookies(this, true)
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    return false
                                }

                                // Ohne diesen Override killt Android den kompletten App-Prozess, wenn der
                                // WebView-Renderer stirbt. Mit true übernehmen wir und bauen neu auf.
                                @TargetApi(Build.VERSION_CODES.O)
                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: RenderProcessGoneDetail?
                                ): Boolean {
                                    Log.w(TAG, "WebView renderer gone (crashed=${detail?.didCrash()}) — rebuilding player")
                                    view?.destroy()
                                    rebuildToken++
                                    return true
                                }
                            }
                            // Der Watchdog laeuft als JavaScript im WebView. Ohne diese Bruecke
                            // bleibt unsichtbar, ob und wann er eingegriffen hat — und genau das
                            // will man wissen, wenn jemand von einer Pufferpause berichtet.
                            webChromeClient = object : WebChromeClient() {
                                override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
                                    val text = msg?.message().orEmpty()
                                    // Praefixe: der Watchdog meldet Stillstand, [cc] die
                                    // Untertitel-Umschaltung, [sync] die Drift-Korrektur.
                                    if (text.startsWith("[watchdog]") || text.startsWith("[cc]") || text.startsWith("[sync]")) {
                                        Log.i(TAG, text)
                                    }
                                    return true
                                }
                            }

                            settings.apply {
                                javaScriptEnabled = true
                                javaScriptCanOpenWindowsAutomatically = true
                                mediaPlaybackRequiresUserGesture = false
                                domStorageEnabled = true
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                allowFileAccess = true
                                allowContentAccess = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                }
                                userAgentString = "Mozilla/5.0 (Linux; Android 11; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            }

                            val startSec = (mediaItem.currentTimeSeconds + leadFor(mediaItem.currentTimeSeconds)).toInt()
                            val cleanYtId = extractYouTubeId(mediaItem.id)
                            val html = buildPlayerHtml(mediaItem, cleanYtId, startSec, isMuted, subtitlesEnabled)
                            loadDataWithBaseURL("https://cytu.be", html, "text/html", "UTF-8", null)
                            webViewRef.value = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    // Ohne destroy() bleibt bei jedem Videowechsel ein WebView samt laufendem
                    // YouTube-Player im Speicher — der Leak treibt den Renderer irgendwann ins OOM.
                    onRelease = { webView ->
                        webViewRef.value = null
                        webView.stopLoading()
                        webView.loadUrl("about:blank")
                        webView.destroy()
                    }
                )
            }
        } else {
            val playerView = remember {
                PlayerView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            }

            DisposableEffect(exoPlayer) {
                playerView.player = exoPlayer
                onDispose {
                    playerView.player = null
                }
            }

            AndroidView(
                factory = { playerView },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    if (view.player != exoPlayer) {
                        view.player = exoPlayer
                    }
                }
            )
        }
    }
}

fun extractYouTubeId(urlOrId: String): String {
    val trimmed = urlOrId.trim()
    if (trimmed.length == 11 && !trimmed.contains("/") && !trimmed.contains("?") && !trimmed.contains("=")) {
        return trimmed
    }
    val pattern = "(?:youtube(?:-nocookie)?\\.com/(?:[^/\\n\\s]+/\\S+/|(?:v|e(?:mbed)?)/|\\S*?[?&]v=)|youtu\\.be/)([a-zA-Z0-9_-]{11})".toRegex()
    val match = pattern.find(trimmed)
    return match?.groupValues?.get(1) ?: trimmed
}

private fun buildPlayerHtml(mediaItem: MediaItem, cleanYtId: String, startSec: Int, isMuted: Boolean, subtitlesEnabled: Boolean): String {
    val muteParam = if (isMuted) 1 else 0
    val ccParam = if (subtitlesEnabled) "&cc_load_policy=1&cc_lang_pref=de" else "&cc_load_policy=0"
    val type = mediaItem.type.lowercase()

    val iframeSrc = when {
        type == "yt" || cleanYtId.length == 11 -> {
            "https://www.youtube.com/embed/$cleanYtId?autoplay=1&controls=0&enablejsapi=1&rel=0&modestbranding=1&iv_load_policy=3&playsinline=1&loop=0&mute=$muteParam&start=$startSec$ccParam"
        }
        type == "tw" -> {
            "https://player.twitch.tv/?channel=${mediaItem.id}&parent=cytu.be&parent=localhost&autoplay=true&muted=${if (isMuted) "true" else "false"}"
        }
        type == "vi" -> {
            "https://player.vimeo.com/video/${mediaItem.id}?autoplay=1&muted=${if (isMuted) 1 else 0}#t=${startSec}s"
        }
        mediaItem.id.startsWith("http") -> {
            mediaItem.id
        }
        else -> {
            "https://www.youtube.com/embed/$cleanYtId?autoplay=1&controls=0&enablejsapi=1&rel=0&modestbranding=1&mute=$muteParam&start=$startSec$ccParam"
        }
    }

    val watchdog = if (iframeSrc.contains("youtube.com/embed/")) YT_WATCHDOG_JS else ""
    val ccOff = if (!subtitlesEnabled && iframeSrc.contains("youtube.com/embed/")) CC_OFF_JS else ""
    // Nur sperren, wenn das Geraet AV1 ohnehin in Software decodieren muesste.
    val av1Block = if (hasAv1HardwareDecoder) "" else AV1_BLOCK_JS

    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            html, body { width: 100%; height: 100%; overflow: hidden; background-color: #000; }
            iframe { width: 100%; height: 100%; position: absolute; top: 0; left: 0; border: none; }
          </style>
          $av1Block
        </head>
        <body>
          <iframe id="player_iframe"
            src="$iframeSrc"
            allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
            allowfullscreen>
          </iframe>
$watchdog
$ccOff
        </body>
        </html>
    """.trimIndent()
}

/**
 * Watchdog gegen stehengebliebenes YouTube-Playback.
 *
 * Der Embed meldet nach dem 'listening'-Handshake laufend infoDelivery-Events mit currentTime und
 * playerState. Bewegt sich currentTime trotz State=PLAYING nicht mehr, ist der Player tot; die App
 * merkte das bisher nie und wartete auf das nächste Video. Eskalation: erst playVideo, dann seek auf
 * die letzte bekannte Position, zuletzt kompletter iframe-Reload ab dieser Position.
 *
 * ponytail: fixes 6s-Raster statt adaptiver Erkennung — reicht, solange CyTube-Videos > 10s sind.
 */
/**
 * Schaltet die Untertitel im laufenden Player ab.
 *
 * Noetig, weil cc_load_policy=0 nichts abschaltet: YouTube kennt nur cc_load_policy=1
 * ("immer einblenden"), jeder andere Wert bedeutet "wie beim Betrachter eingestellt". Bei
 * Videos mit automatischen Untertiteln blieben sie deshalb trotz Neuaufbau sichtbar.
 *
 * Der Befehl geht zweimal raus: der Player meldet sich nicht zuverlaessig zu einem festen
 * Zeitpunkt, und ein verlorener Befehl faellt sonst niemandem auf.
 */
private val CC_OFF_JS = """
          <script>
          (function() {
            var f = document.getElementById('player_iframe');
            function cmd(func, args) {
              try {
                f.contentWindow.postMessage(
                  JSON.stringify({event: 'command', func: func, args: args || []}), '*');
              } catch(e) {}
            }
            function off() {
              // 'captions' beim aelteren Player, 'cc' beim HTML5-Player — welcher laeuft,
              // ist von aussen nicht erkennbar, also beide.
              cmd('unloadModule', ['captions']);
              cmd('unloadModule', ['cc']);
              console.log('[cc] Untertitel abgeschaltet');
            }
            f.addEventListener('load', function() {
              setTimeout(off, 1200);
              setTimeout(off, 3000);
            });
            setTimeout(off, 2500);
          })();
          </script>
"""

private val YT_WATCHDOG_JS = """
          <script>
          (function() {
            var iframe = document.getElementById('player_iframe');
            var lastTime = -1, lastMoveTs = Date.now(), state = -1, strikes = 0;
            var lastSeekTs = 0, pageLoadTs = Date.now();

            function send(func, args) {
              try {
                iframe.contentWindow.postMessage(JSON.stringify({event: 'command', func: func, args: args || []}), '*');
              } catch(e) {}
            }

            function handshake() {
              try {
                iframe.contentWindow.postMessage(JSON.stringify({event: 'listening', id: 'player_iframe', channel: 'widget'}), '*');
              } catch(e) {}
            }

            window.addEventListener('message', function(e) {
              var d = e.data;
              try { if (typeof d === 'string') d = JSON.parse(d); } catch(_) { return; }
              if (!d || !d.info) return;
              if (typeof d.info.playerState === 'number') {
                if (state !== d.info.playerState) {
                  // 3 = Puffern. Nur diesen Wechsel melden, sonst rauscht das Log voll.
                  if (d.info.playerState === 3) console.log('[watchdog] puffert bei ' + Math.round(lastTime) + 's');
                  else if (state === 3) console.log('[watchdog] weiter bei ' + Math.round(lastTime) + 's');
                }
                state = d.info.playerState;
              }
              var t = d.info.currentTime;
              if (typeof t === 'number' && Math.abs(t - lastTime) > 0.25) {
                lastTime = t;
                lastMoveTs = Date.now();
                strikes = 0;
              }
            });

            // CyTube Server Drift & Playback Reconciliation (Sync Engine aus calzoneman/sync)
            window.syncPlayback = function(serverTime, isPaused, threshold) {
              try {
                var now = Date.now();
                // 1. Play / Pause Sync
                if (isPaused && state === 1) {
                  send('pauseVideo');
                } else if (!isPaused && state !== 1 && state !== 3 && state !== 0) {
                  if (state !== 2) console.log('[sync] Player stand bei state=' + state + ', starte');
                  send('playVideo');
                }

                // 2. Time Drift Sync: Keine Seeks während der ersten 25s und mind. 45s Abstand zwischen Seeks
                if (now - pageLoadTs < 25000) return;
                if (state === 3) return; // Buffering

                if (typeof serverTime === 'number' && serverTime >= 0 && lastTime >= 0 && (now - lastSeekTs > 45000)) {
                  var diff = Math.abs(lastTime - serverTime);
                  if (diff > (threshold || 120.0)) {
                    console.log('[sync] Major drift (>120s) korrigiert: local=' + lastTime.toFixed(1) + 's, server=' + serverTime.toFixed(1) + 's (diff=' + diff.toFixed(1) + 's)');
                    lastSeekTs = now;
                    send('seekTo', [serverTime, true]);
                    if (!isPaused && state !== 1) {
                      send('playVideo');
                    }
                    lastTime = serverTime;
                    lastMoveTs = now;
                  }
                }
              } catch(e) {}
            };

            iframe.addEventListener('load', function() {
              handshake();
              setTimeout(handshake, 1500);
            });
            setTimeout(handshake, 2000);

            setInterval(function() {
              // state 1 = PLAYING, 3 = BUFFERING. Gewollte Pausen (2) und Videoende (0) nicht anfassen.
              if (state !== 1 && state !== 3) return;
              // Puffern ist normal und dauert meist unter einer Sekunde. Bleibt die Zeit
              // laenger stehen, haengt der Player wirklich — gemessen am Geraet: 27 Sekunden
              // Stillstand, aus dem er allein nicht mehr herausfand.
              if (Date.now() - lastMoveTs < 7000) return;

              strikes++;
              lastMoveTs = Date.now();
              console.log('[watchdog] Stillstand bei ' + Math.round(lastTime) + 's, Versuch ' + strikes);
              // Beim Puffern hilft playVideo nichts — der Player laeuft ja bereits, er kommt
              // nur nicht weiter. Was ihn loest, ist der kleine Sprung. Deshalb hier zuerst.
              if (strikes === 1 && state === 3 && lastTime > 0) {
                send('seekTo', [lastTime + 0.5, true]);
                send('playVideo');
              } else if (strikes === 1) {
                send('playVideo');
              } else if (strikes === 2) {
                if (lastTime > 0) send('seekTo', [lastTime + 1.5, true]);
                send('playVideo');
              } else {
                var resumeAt = lastTime > 0 ? Math.floor(lastTime) : 0;
                var src = iframe.src.replace(/([?&])start=[0-9]+/, '${'$'}1start=' + resumeAt);
                if (src.indexOf('start=') === -1) src += '&start=' + resumeAt;
                console.log('[watchdog] Neuaufbau ab ' + resumeAt + 's');
                strikes = 0;
                lastTime = -1;
                iframe.src = src;
              }
            }, 3000);
          })();
          </script>
""".trimIndent()

/**
 * Sperrt AV1 gegenueber YouTube, damit die Wiedergabe auf einem hardwarebeschleunigten Codec
 * landet. Wird nur eingesetzt, wenn das Geraet keinen AV1-Decoder in Hardware hat — siehe
 * hasAv1HardwareDecoder.
 */
private val AV1_BLOCK_JS = """
          <script>
            try {
              if (window.MediaSource && MediaSource.isTypeSupported) {
                var origIsTypeSupported = MediaSource.isTypeSupported.bind(MediaSource);
                MediaSource.isTypeSupported = function(type) {
                  if (type && (type.indexOf('av01') !== -1 || type.indexOf('av1') !== -1)) {
                    return false;
                  }
                  return origIsTypeSupported(type);
                };
              }
              if (window.HTMLMediaElement && HTMLMediaElement.prototype.canPlayType) {
                var origCanPlayType = HTMLMediaElement.prototype.canPlayType;
                HTMLMediaElement.prototype.canPlayType = function(type) {
                  if (type && (type.indexOf('av01') !== -1 || type.indexOf('av1') !== -1)) {
                    return '';
                  }
                  return origCanPlayType.call(this, type);
                };
              }
            } catch(e) {}
          </script>
""".trimIndent()
