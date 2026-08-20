package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Begrenzt den Bildspeicher von Coil.
 *
 * Das Zielgeraet ist ein Fire TV Stick mit 1,7 GB RAM, von denen im Betrieb kaum 100 MB frei
 * sind — daneben laeuft ein WebView mit YouTube. Coil nimmt sich sonst bis zu einem Viertel des
 * verfuegbaren Speichers fuer den Bildcache; bei Plakaten in Kinoaufloesung ist das zu viel und
 * geht der Videowiedergabe ab. Zwei Plakate im Speicher reichen voellig, der Rest kommt bei
 * Bedarf von der Platte.
 */
class GrindhouseApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.05)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("poster_cache"))
                    .maxSizeBytes(24L * 1024 * 1024)
                    .build()
            }
            .build()
}
