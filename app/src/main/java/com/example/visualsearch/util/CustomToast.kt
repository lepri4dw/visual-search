package com.example.visualsearch.util

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.cardview.widget.CardView
import com.example.visualsearch.R

enum class ToastType {
    SUCCESS, ERROR, INFO, WARNING
}

object CustomToast {
    fun showToast(
        context: Context,
        message: String,
        duration: Int = Toast.LENGTH_SHORT,
        type: ToastType = ToastType.INFO
    ) {
        val inflater = LayoutInflater.from(context)
        val layout: View = inflater.inflate(R.layout.custom_toast, null)

        // Get views
        val cardView = layout.findViewById<CardView>(R.id.toast_container)
        val icon = layout.findViewById<ImageView>(R.id.toast_icon)
        val text = layout.findViewById<TextView>(R.id.toast_text)

        // Set icon and card background based on type
        when (type) {
            ToastType.SUCCESS -> {
                icon.setImageResource(R.drawable.ic_success)
                cardView.setCardBackgroundColor(context.getColor(R.color.success_background))
                icon.setColorFilter(context.getColor(R.color.success_color))
            }
            ToastType.ERROR -> {
                icon.setImageResource(R.drawable.ic_error)
                cardView.setCardBackgroundColor(context.getColor(R.color.error_background))
                icon.setColorFilter(context.getColor(R.color.error_color))
            }
            ToastType.WARNING -> {
                icon.setImageResource(R.drawable.ic_warning)
                cardView.setCardBackgroundColor(context.getColor(R.color.warning_background))
                icon.setColorFilter(context.getColor(R.color.warning_color))
            }
            ToastType.INFO -> {
                icon.setImageResource(R.drawable.ic_info)
                cardView.setCardBackgroundColor(context.getColor(R.color.info_background))
                icon.setColorFilter(context.getColor(R.color.info_color))
            }
        }

        // Set text
        text.text = message

        // Create and show toast
        val toast = Toast(context)
        toast.setGravity(Gravity.BOTTOM or Gravity.FILL_HORIZONTAL, 0, 64)
        toast.duration = duration
        toast.view = layout
        toast.show()
    }
}

// Usage example:
// CustomToast.showToast(context, "Image saved successfully!", type = ToastType.SUCCESS)
// CustomToast.showToast(context, "Failed to connect to server", type = ToastType.ERROR)
// CustomToast.showToast(context, "Tips: Try to use better lighting", type = ToastType.INFO)