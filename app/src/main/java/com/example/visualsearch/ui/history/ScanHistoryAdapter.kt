package com.example.visualsearch.ui.history

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.visualsearch.R
import com.example.visualsearch.data.entity.ScanHistoryEntity
import com.example.visualsearch.databinding.ItemScanHistoryBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ScanHistoryAdapter(
    private val context: Context,
    private val onItemClick: (ScanHistoryEntity) -> Unit
) : ListAdapter<ScanHistoryEntity, ScanHistoryAdapter.ScanViewHolder>(ScanDiffCallback()) {

    private val TAG = "ScanHistoryAdapter"
    private var lastAnimatedPosition = -1

    init {
        Log.d(TAG, "Адаптер инициализирован")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        Log.d(TAG, "onCreateViewHolder вызван")
        val binding = ItemScanHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        val scan = getItem(position)
        Log.d(TAG, "onBindViewHolder: позиция $position, id: ${scan.id}, запрос: ${scan.query}")
        holder.bind(scan)
        
        // Применяем анимацию к элементам при прокрутке
        if (position > lastAnimatedPosition) {
            val animation = AnimationUtils.loadAnimation(context, R.anim.item_animation_from_bottom)
            animation.startOffset = position * 50L // Задержка между элементами
            holder.itemView.startAnimation(animation)
            lastAnimatedPosition = position
        }
    }
    
    override fun onViewDetachedFromWindow(holder: ScanViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
    }

    override fun submitList(list: List<ScanHistoryEntity>?) {
        Log.d(TAG, "submitList: получено ${list?.size ?: 0} элементов")
        list?.forEach { item ->
            Log.d(TAG, "Элемент: id=${item.id}, запрос=${item.query}, imagePath=${item.imagePath}")
        }
        super.submitList(list?.toList()) // Создаем копию списка для безопасности
    }

    inner class ScanViewHolder(private val binding: ItemScanHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                // Безопасное использование позиции
                val currentPosition = adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    // Добавляем эффект нажатия
                    binding.root.animate()
                        .setDuration(150)
                        .scaleX(0.97f)
                        .scaleY(0.97f)
                        .withEndAction {
                            binding.root.animate()
                                .setDuration(150)
                                .scaleX(1f)
                                .scaleY(1f)
                                .start()
                            
                            // Вызываем обработчик после анимации и снова проверяем позицию
                            val finalPosition = adapterPosition
                            if (finalPosition != RecyclerView.NO_POSITION) {
                                onItemClick(getItem(finalPosition))
                            }
                        }
                        .start()
                }
            }
        }

        fun bind(scan: ScanHistoryEntity) {
            try {
                // Загружаем изображение с Firebase Storage или локального хранилища
                val imagePath = scan.imagePath
                Log.d(TAG, "Загружаем изображение: $imagePath")
                
                // Проверяем, является ли путь URL-адресом (Firebase Storage)
                if (imagePath.startsWith("https://")) {
                    // Загружаем изображение из Firebase Storage
                    Log.d(TAG, "Загружаем изображение из Firebase Storage")
                    Glide.with(context)
                        .load(imagePath)
                        .apply(RequestOptions()
                            .transform(CenterCrop())
                            .placeholder(R.drawable.ic_image_placeholder))
                        .into(binding.imageViewScanThumbnail)
                } else {
                    // Загружаем локальное изображение
                    val imageFile = File(imagePath)
                    if (imageFile.exists()) {
                        Log.d(TAG, "Загружаем локальное изображение")
                        Glide.with(context)
                            .load(imageFile)
                            .apply(RequestOptions().transform(CenterCrop()))
                            .into(binding.imageViewScanThumbnail)
                    } else {
                        Log.d(TAG, "Файл не существует, показываем заглушку")
                        // Если файл не существует, показываем заглушку
                        Glide.with(context)
                            .load(R.drawable.ic_image_placeholder)
                            .into(binding.imageViewScanThumbnail)
                    }
                }

                // Устанавливаем информацию о сканировании
                binding.textViewProductType.text = scan.productType.ifEmpty { "Товар" }
                binding.textViewQuery.text = scan.query

                // Форматируем и отображаем дату
                val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                binding.textViewDate.text = dateFormat.format(scan.getDateAsJavaDate())
                
                // Добавляем дополнительную информацию, если она доступна
                val brandInfo = if (scan.brand.isNotEmpty()) {
                    " • ${scan.brand}"
                } else {
                    ""
                }
                
                val colorInfo = if (scan.color.isNotEmpty()) {
                    " • ${scan.color}"
                } else {
                    ""
                }
                
                // Если есть дополнительная информация, добавляем её к основному запросу
                if (brandInfo.isNotEmpty() || colorInfo.isNotEmpty()) {
                    binding.textViewQuery.text = "${scan.query}$brandInfo$colorInfo"
                }
                
                Log.d(TAG, "Привязка выполнена успешно")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при привязке данных", e)
            }
        }
    }

    class ScanDiffCallback : DiffUtil.ItemCallback<ScanHistoryEntity>() {
        override fun areItemsTheSame(oldItem: ScanHistoryEntity, newItem: ScanHistoryEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ScanHistoryEntity, newItem: ScanHistoryEntity): Boolean {
            return oldItem == newItem
        }
    }
}