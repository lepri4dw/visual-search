package com.example.visualsearch.ui.history

import android.content.Context
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

    private var lastAnimatedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        val binding = ItemScanHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        val scan = getItem(position)
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

    inner class ScanViewHolder(private val binding: ItemScanHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
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
                            
                            // Вызываем обработчик после анимации
                            onItemClick(getItem(position))
                        }
                        .start()
                }
            }
        }

        fun bind(scan: ScanHistoryEntity) {
            // Загружаем изображение с улучшенным форматированием
            val imageFile = File(scan.imagePath)
            if (imageFile.exists()) {
                Glide.with(context)
                    .load(imageFile)
                    .apply(RequestOptions().transform(CenterCrop()))
                    .into(binding.imageViewScanThumbnail)
            }

            // Устанавливаем информацию о сканировании
            binding.textViewProductType.text = scan.productType.ifEmpty { "Товар" }
            binding.textViewQuery.text = scan.query

            // Форматируем и отображаем дату
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            binding.textViewDate.text = dateFormat.format(scan.scanDate)
            
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