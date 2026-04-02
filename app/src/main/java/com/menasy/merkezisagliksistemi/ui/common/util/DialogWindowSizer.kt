package com.menasy.merkezisagliksistemi.ui.common.util

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog

object DialogWindowSizer {

    fun applyCenteredDialogBounds(
        dialog: AlertDialog,
        contentView: View,
        context: Context
    ) {
        val displayMetrics = context.resources.displayMetrics
        val maxWidth = dpToPx(context, MAX_DIALOG_WIDTH_DP)
        val targetWidth = minOf((displayMetrics.widthPixels * DIALOG_WIDTH_RATIO).toInt(), maxWidth)
        val maxHeight = (displayMetrics.heightPixels * DIALOG_MAX_HEIGHT_RATIO).toInt()

        dialog.window?.setLayout(targetWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

        contentView.post {
            if (contentView.height <= maxHeight) return@post

            val updatedParams = (contentView.layoutParams ?: ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )).apply {
                height = maxHeight
            }
            contentView.layoutParams = updatedParams
        }
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private const val MAX_DIALOG_WIDTH_DP = 720
    private const val DIALOG_WIDTH_RATIO = 0.9f
    private const val DIALOG_MAX_HEIGHT_RATIO = 0.9f
}
