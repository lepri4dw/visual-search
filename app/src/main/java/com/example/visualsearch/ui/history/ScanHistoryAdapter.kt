package com.example.visualsearch.ui.history

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.visualsearch.data.entity.ScanHistoryEntity
import com.example.visualsearch.databinding.ItemScanHistoryBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ScanHistoryAdapter(
    private val context: Context,
    private val onItemClick: (ScanHistoryEntity) -> Unit
) : ListAdapter<ScanHistoryEntity, ScanHistoryAdapter.ScanViewHolder>(ScanDiffCallback()) {

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
    }

    inner class ScanViewHolder(private val binding: ItemScanHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(scan: ScanHistoryEntity) {
            // Load image thumbnail
            val imageFile = File(scan.imagePath)
            if (imageFile.exists()) {
                Glide.with(context)
                    .load(imageFile)
                    .centerCrop()
                    .into(binding.imageViewScanThumbnail)
            }

            // Set text information
            binding.textViewProductType.text = scan.productType
            binding.textViewQuery.text = scan.query

            // Format and display date
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            binding.textViewDate.text = dateFormat.format(scan.scanDate)
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