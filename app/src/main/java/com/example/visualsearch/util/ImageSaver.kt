package com.example.visualsearch.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageSaver {
    private const val TAG = "ImageSaver"
    private const val DIRECTORY_NAME = "scan_images"
    
    fun saveImageToInternalStorage(context: Context, bitmap: Bitmap): String? {
        val directory = File(context.filesDir, DIRECTORY_NAME)
        if (!directory.exists()) {
            directory.mkdir()
        }
        
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "SCAN_$timeStamp.jpg"
        val file = File(directory, fileName)
        
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Error saving image", e)
            null
        }
    }
    
    fun deleteImage(path: String) {
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
    }
}
