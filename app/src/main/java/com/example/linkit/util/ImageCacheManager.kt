package com.example.linkit.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageCacheManager @Inject constructor(
    private val context: Context
) {

    private val cacheDir = File(context.cacheDir, "profile_images")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    suspend fun downloadAndCacheImage(imageUrl: String, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL(imageUrl)
            val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())

            val file = File(cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.close()

            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun getCachedImagePath(fileName: String): String? {
        val file = File(cacheDir, fileName)
        return if (file.exists()) file.absolutePath else null
    }

    fun loadCachedImage(localPath: String): Bitmap? {
        return try {
            val file = File(localPath)
            if (file.exists()) {
                BitmapFactory.decodeFile(localPath)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}
