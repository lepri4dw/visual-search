package com.example.visualsearch.data.repository

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.visualsearch.auth.AuthManager
import com.example.visualsearch.data.entity.ScanHistoryEntity
import com.example.visualsearch.data.firebase.FirebaseStorageService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

/**
 * Репозиторий для работы с историей сканирований в Firestore
 */
class FirestoreScanRepository {
    private val TAG = "FirestoreScanRepository"
    private val db = FirebaseFirestore.getInstance()
    private val scanCollection = db.collection("scan_history")
    private val authManager = AuthManager.getInstance()
    private val storageService = FirebaseStorageService.getInstance()
    
    // Регистрация слушателя для возможности отмены подписки
    private var snapshotListener: ListenerRegistration? = null

    // LiveData для хранения списка сканирований пользователя
    private val _userScans = MutableLiveData<List<ScanHistoryEntity>>()
    val userScans: LiveData<List<ScanHistoryEntity>> = _userScans

    // Подписываемся на обновления при создании репозитория
    init {
        setupUserScansListener()
        
        // Наблюдаем за изменениями в текущем пользователе
        authManager.currentUser.observeForever { user ->
            Log.d(TAG, "Изменение пользователя: ${user?.uid ?: "не авторизован"}")
            // Обновляем слушатель при изменении пользователя
            updateUserScansListener()
        }
    }
    
    /**
     * Обновление слушателя при изменении статуса авторизации
     */
    fun updateUserScansListener() {
        // Отменяем предыдущую подписку
        snapshotListener?.remove()
        
        // Настраиваем новый слушатель
        setupUserScansListener()
        
        // Выполняем немедленную загрузку данных
        val userId = authManager.getCurrentUserId()
        if (userId.isNotEmpty()) {
            Log.d(TAG, "Обновление слушателя для пользователя: $userId")
            loadUserScansImmediately(userId)
        }
    }

    /**
     * Немедленная загрузка сканирований пользователя без слушателя
     * Это нужно для получения данных без использования составного запроса, требующего индекс
     */
    private fun loadUserScansImmediately(userId: String) {
        Log.d(TAG, "loadUserScansImmediately: загружаем данные для пользователя $userId")
        
        // Выполняем простой запрос без сортировки, чтобы не требовался индекс
        scanCollection
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d(TAG, "Получен снапшот, пустой: ${snapshot.isEmpty}, документов: ${snapshot.documents.size}")

                if (!snapshot.isEmpty) {
                    val scans = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data
                            if (data != null) {
                                val scan = ScanHistoryEntity()
                                
                                scan.firestoreId = doc.id
                                scan.userId = data["user_id"] as? String ?: ""
                                
                                // Проверяем, что сканирование принадлежит текущему пользователю
                                if (scan.userId != userId) {
                                    return@mapNotNull null
                                }
                                
                                scan.query = data["query"] as? String ?: ""
                                scan.productType = data["product_type"] as? String ?: ""
                                scan.modelName = data["model_name"] as? String ?: ""
                                scan.brand = data["brand"] as? String ?: ""
                                scan.color = data["color"] as? String ?: ""
                                scan.imagePath = data["image_path"] as? String ?: ""
                                
                                // Обработка даты (Timestamp в Firestore)
                                val timestamp = data["scan_date"] as? com.google.firebase.Timestamp
                                scan.scanDate = timestamp?.toDate() ?: Date()
                                
                                Log.d(TAG, "Преобразован документ: ${doc.id}, объект: $scan")
                                scan
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting document ${doc.id}", e)
                            null
                        }
                    }
                    
                    // Сортируем вручную по дате (самые новые вверху)
                    val sortedScans = scans.sortedByDescending { it.scanDate }
                    
                    Log.d(TAG, "Обработано ${sortedScans.size} сканирований, отправляем в LiveData")
                    _userScans.postValue(sortedScans)
                } else {
                    Log.d(TAG, "Пустой результат, отправляем пустой список в LiveData")
                    _userScans.postValue(emptyList())
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Ошибка при загрузке сканирований", e)
                _userScans.postValue(emptyList())
            }
    }

    /**
     * Настройка слушателя изменений для сканирований пользователя
     */
    private fun setupUserScansListener() {
        val userId = authManager.getCurrentUserId()
        // Если пользователь не авторизован, возвращаем пустой список
        if (userId.isEmpty()) {
            Log.d(TAG, "setupUserScansListener: пользователь не авторизован, возвращаем пустой список")
            _userScans.postValue(emptyList())
            return
        }

        Log.d(TAG, "setupUserScansListener: настраиваем слушатель для пользователя $userId")

        try {
            // Настраиваем слушатель для коллекции в Firestore, только с фильтром по user_id, без сортировки
            snapshotListener = scanCollection
                .whereEqualTo("user_id", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Ошибка при загрузке сканирований", error)
                        return@addSnapshotListener
                    }

                    Log.d(TAG, "Получен снапшот, пустой: ${snapshot?.isEmpty}, документов: ${snapshot?.documents?.size}")

                    if (snapshot != null && !snapshot.isEmpty) {
                        val scans = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data
                                if (data != null) {
                                    val scan = ScanHistoryEntity()
                                    
                                    scan.firestoreId = doc.id
                                    scan.userId = data["user_id"] as? String ?: ""
                                    
                                    // Проверяем, что сканирование принадлежит текущему пользователю
                                    if (scan.userId != userId) {
                                        Log.d(TAG, "Пропускаем сканирование другого пользователя: ${doc.id}")
                                        return@mapNotNull null
                                    }
                                    
                                    scan.query = data["query"] as? String ?: ""
                                    scan.productType = data["product_type"] as? String ?: ""
                                    scan.modelName = data["model_name"] as? String ?: ""
                                    scan.brand = data["brand"] as? String ?: ""
                                    scan.color = data["color"] as? String ?: ""
                                    scan.imagePath = data["image_path"] as? String ?: ""
                                    
                                    // Обработка даты (Timestamp в Firestore)
                                    val timestamp = data["scan_date"] as? com.google.firebase.Timestamp
                                    scan.scanDate = timestamp?.toDate() ?: Date()
                                    
                                    Log.d(TAG, "Преобразован документ: ${doc.id}, объект: $scan")
                                    scan
                                } else {
                                    Log.e(TAG, "Документ без данных: ${doc.id}")
                                    null
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error converting document ${doc.id}", e)
                                null
                            }
                        }
                        
                        // Сортируем вручную по дате (самые новые вверху)
                        val sortedScans = scans.sortedByDescending { it.scanDate }
                        
                        Log.d(TAG, "Обработано ${sortedScans.size} сканирований, отправляем в LiveData")
                        _userScans.postValue(sortedScans)
                    } else {
                        Log.d(TAG, "Пустой результат, отправляем пустой список в LiveData")
                        _userScans.postValue(emptyList())
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при настройке слушателя", e)
            // Пробуем загрузить данные напрямую
            loadUserScansImmediately(userId)
        }
    }

    /**
     * Добавление нового сканирования в историю
     * @return ID добавленного документа или null в случае ошибки
     */
    suspend fun insertScan(
        query: String,
        productType: String,
        modelName: String,
        brand: String,
        color: String,
        imagePath: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val userId = authManager.getCurrentUserId()
            
            // Проверяем, авторизован ли пользователь
            if (userId.isEmpty()) {
                Log.d(TAG, "Пользователь не авторизован, отменяем сохранение")
                return@withContext null
            }
            
            // Проверяем, нужно ли загрузить изображение в Firebase Storage
            val imageUrl = if (imagePath.startsWith("http")) {
                // Если путь уже является URL, используем его как есть
                imagePath
            } else {
                // Загружаем локальный файл в Firebase Storage
                val file = File(imagePath)
                storageService.uploadImageFromFile(file) ?: return@withContext null
            }

            // Создаем объект сканирования
            val scan = ScanHistoryEntity(
                userId = userId,
                query = query,
                productType = productType,
                modelName = modelName,
                brand = brand,
                color = color,
                imagePath = imageUrl,
                scanDate = Date()
            )

            // Добавляем документ в Firestore
            val docRef = scanCollection.add(scan.toMap()).await()
            Log.d(TAG, "Scan added with ID: ${docRef.id}")
            
            // Сохраняем Firestore ID
            scan.firestoreId = docRef.id
            
            return@withContext docRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error adding scan", e)
            return@withContext null
        }
    }
    
    /**
     * Сохранение сканирования с изображением
     */
    suspend fun insertScanWithBitmap(
        query: String,
        productType: String,
        modelName: String,
        brand: String,
        color: String,
        bitmap: Bitmap
    ): String? = withContext(Dispatchers.IO) {
        try {
            val userId = authManager.getCurrentUserId()
            
            // Проверяем, авторизован ли пользователь
            if (userId.isEmpty()) {
                Log.d(TAG, "Пользователь не авторизован, отменяем сохранение")
                return@withContext null
            }
            
            // Загружаем изображение в Firebase Storage
            val imageUrl = storageService.uploadImageFromBitmap(bitmap) ?: return@withContext null

            // Создаем объект сканирования
            val scan = ScanHistoryEntity(
                userId = userId,
                query = query,
                productType = productType,
                modelName = modelName,
                brand = brand,
                color = color,
                imagePath = imageUrl,
                scanDate = Date()
            )

            // Добавляем документ в Firestore
            val docRef = scanCollection.add(scan.toMap()).await()
            Log.d(TAG, "Scan added with ID: ${docRef.id}")
            
            // Сохраняем Firestore ID
            scan.firestoreId = docRef.id
            
            return@withContext docRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error adding scan with bitmap", e)
            return@withContext null
        }
    }

    /**
     * Получение информации о сканировании по ID
     */
    suspend fun getScanById(scanId: String): ScanHistoryEntity? = withContext(Dispatchers.IO) {
        try {
            // Получаем текущего пользователя
            val currentUserId = authManager.getCurrentUserId()
            if (currentUserId.isEmpty()) {
                Log.d(TAG, "Пользователь не авторизован, отменяем получение данных")
                return@withContext null
            }
            
            val document = scanCollection.document(scanId).get().await()
            if (document.exists()) {
                val data = document.data
                if (data != null) {
                    // Проверяем, принадлежит ли сканирование текущему пользователю
                    val userId = data["user_id"] as? String ?: ""
                    if (userId != currentUserId) {
                        Log.d(TAG, "Сканирование принадлежит другому пользователю, отменяем получение данных")
                        return@withContext null
                    }
                    
                    val scan = ScanHistoryEntity()
                    
                    // Устанавливаем ID документа
                    scan.firestoreId = document.id
                    
                    // Маппим поля из документа в модель
                    scan.userId = userId
                    scan.query = data["query"] as? String ?: ""
                    scan.productType = data["product_type"] as? String ?: ""
                    scan.modelName = data["model_name"] as? String ?: ""
                    scan.brand = data["brand"] as? String ?: ""
                    scan.color = data["color"] as? String ?: ""
                    scan.imagePath = data["image_path"] as? String ?: ""
                    
                    // Обработка даты (Timestamp в Firestore)
                    val timestamp = data["scan_date"] as? com.google.firebase.Timestamp
                    scan.scanDate = timestamp?.toDate() ?: Date()
                    
                    return@withContext scan
                }
            }
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting scan by ID", e)
            return@withContext null
        }
    }

    /**
     * Удаление сканирования из истории
     */
    suspend fun deleteScan(scanId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Получаем текущего пользователя
            val currentUserId = authManager.getCurrentUserId()
            if (currentUserId.isEmpty()) {
                Log.d(TAG, "Пользователь не авторизован, отменяем удаление")
                return@withContext false
            }
            
            // Получаем документ, чтобы узнать URL изображения и владельца
            val document = scanCollection.document(scanId).get().await()
            if (document.exists()) {
                val data = document.data
                val userId = data?.get("user_id") as? String ?: ""
                
                // Проверяем, принадлежит ли сканирование текущему пользователю
                if (userId != currentUserId) {
                    Log.d(TAG, "Сканирование принадлежит другому пользователю, отменяем удаление")
                    return@withContext false
                }
                
                val imagePath = data?.get("image_path") as? String ?: ""
                
                // Удаляем документ из Firestore
                scanCollection.document(scanId).delete().await()
                
                // Если есть URL изображения, удаляем его из Storage
                if (imagePath.startsWith("https://")) {
                    storageService.deleteImage(imagePath)
                }
                
                Log.d(TAG, "Scan deleted: $scanId")
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting scan", e)
            return@withContext false
        }
    }

    /**
     * Удаление всех сканирований текущего пользователя
     */
    suspend fun deleteUserScans(): Boolean = withContext(Dispatchers.IO) {
        try {
            val userId = authManager.getCurrentUserId()
            if (userId.isEmpty()) {
                Log.d(TAG, "Пользователь не авторизован, отменяем удаление")
                return@withContext false
            }
            
            // Получаем все сканирования пользователя
            val documents = scanCollection
                .whereEqualTo("user_id", userId)
                .get()
                .await()
            
            // Удаляем каждый документ и связанное изображение
            var success = true
            for (document in documents) {
                try {
                    val data = document.data
                    val documentUserId = data["user_id"] as? String ?: ""
                    
                    // Дополнительная проверка, что документ принадлежит текущему пользователю
                    if (documentUserId != userId) {
                        Log.d(TAG, "Пропускаем документ другого пользователя: ${document.id}")
                        continue
                    }
                    
                    val imagePath = data["image_path"] as? String ?: ""
                    
                    // Удаляем документ
                    scanCollection.document(document.id).delete().await()
                    
                    // Удаляем изображение, если оно хранится в Firebase Storage
                    if (imagePath.startsWith("https://")) {
                        storageService.deleteImage(imagePath)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting scan: ${document.id}", e)
                    success = false
                }
            }
            
            Log.d(TAG, "All user scans deleted: $userId")
            return@withContext success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user scans", e)
            return@withContext false
        }
    }

    companion object {
        @Volatile
        private var instance: FirestoreScanRepository? = null

        fun getInstance(): FirestoreScanRepository {
            return instance ?: synchronized(this) {
                instance ?: FirestoreScanRepository().also { instance = it }
            }
        }
    }
} 