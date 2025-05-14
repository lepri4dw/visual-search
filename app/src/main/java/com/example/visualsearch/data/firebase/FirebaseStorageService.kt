package com.example.visualsearch.data.firebase

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.visualsearch.auth.AuthManager
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * Сервис для работы с Firebase Storage
 */
class FirebaseStorageService {
    private val TAG = "FirebaseStorageService"
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    private val authManager = AuthManager.getInstance()

    /**
     * Загрузка изображения в Firebase Storage из Bitmap
     * @param bitmap Изображение для загрузки
     * @return URL загруженного изображения или null в случае ошибки
     */
    suspend fun uploadImageFromBitmap(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val baos = ByteArrayOutputStream()
            // Сжимаем изображение с качеством 85% для экономии места
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val data = baos.toByteArray()

            // Получаем ID пользователя или используем "anonymous" если пользователь не авторизован
            val userId = authManager.getCurrentUserId().ifEmpty { "anonymous" }
            
            // Создаем уникальный путь для изображения
            val imageFileName = "${UUID.randomUUID()}.jpg"
            val imagesRef = storageRef.child("users/$userId/images/$imageFileName")
            
            // Выполняем загрузку
            val uploadTask = imagesRef.putBytes(data).await()
            
            // Получаем URL загруженного файла
            val downloadUrl = imagesRef.downloadUrl.await()
            Log.d(TAG, "Image uploaded successfully: ${downloadUrl.toString()}")
            
            return@withContext downloadUrl.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image", e)
            return@withContext null
        }
    }

    /**
     * Загрузка изображения в Firebase Storage из файла
     * @param file Файл изображения для загрузки
     * @return URL загруженного изображения или null в случае ошибки
     */
    suspend fun uploadImageFromFile(file: File): String? = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: ${file.absolutePath}")
                return@withContext null
            }
            
            // Получаем ID пользователя или используем "anonymous" если пользователь не авторизован
            val userId = authManager.getCurrentUserId().ifEmpty { "anonymous" }
            
            // Создаем уникальный путь для изображения
            val imageFileName = "${UUID.randomUUID()}_${file.name}"
            val imagesRef = storageRef.child("users/$userId/images/$imageFileName")
            
            // Выполняем загрузку
            val uploadTask = imagesRef.putFile(Uri.fromFile(file)).await()
            
            // Получаем URL загруженного файла
            val downloadUrl = imagesRef.downloadUrl.await()
            Log.d(TAG, "Image uploaded successfully: ${downloadUrl.toString()}")
            
            return@withContext downloadUrl.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image", e)
            return@withContext null
        }
    }

    /**
     * Удаление изображения из Firebase Storage
     * @param imageUrl URL изображения для удаления
     * @return true если удаление выполнено успешно, false в случае ошибки
     */
    suspend fun deleteImage(imageUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Получаем ссылку на файл из URL
            val fileRef = storage.getReferenceFromUrl(imageUrl)
            fileRef.delete().await()
            Log.d(TAG, "Image deleted successfully: $imageUrl")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image: $imageUrl", e)
            return@withContext false
        }
    }

    companion object {
        @Volatile
        private var instance: FirebaseStorageService? = null

        fun getInstance(): FirebaseStorageService {
            return instance ?: synchronized(this) {
                instance ?: FirebaseStorageService().also { instance = it }
            }
        }
    }
} 