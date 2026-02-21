package com.example.linkit.util

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.min

object ImageUtils{
    private const val MAX_IMAGE_SIZE = 1024 * 1024 // 1MB
    private const val MAX_DIMENSION = 1024 // Max width/height

    fun compressImage(bitmap: Bitmap): String {
        // Resize if too large
        val resizedBitmap = resizeBitmap(bitmap, MAX_DIMENSION, MAX_DIMENSION)

        // Compress with varying quality
        return compressBitmapToBase64(resizedBitmap, MAX_IMAGE_SIZE)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scale = min(scaleWidth, scaleHeight)

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun compressBitmapToBase64(bitmap: Bitmap, maxSizeBytes: Int): String {
        var quality = 90
        var compressedData: ByteArray

        do {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            compressedData = outputStream.toByteArray()
            quality -= 10
        } while (compressedData.size > maxSizeBytes && quality > 10)

        return Base64.encodeToString(compressedData, Base64.NO_WRAP)
    }


}