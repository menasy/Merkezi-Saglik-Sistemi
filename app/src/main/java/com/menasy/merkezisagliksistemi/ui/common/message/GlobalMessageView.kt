package com.menasy.merkezisagliksistemi.ui.common.message

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.ViewGlobalMessageBinding

class GlobalMessageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewGlobalMessageBinding.inflate(LayoutInflater.from(context), this)
    private val handler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    init {
        isVisible = false
        alpha = 0f
        translationY = -dp(20)

        binding.btnDismiss.setOnClickListener {
            dismissMessage()
        }
    }

    fun showMessage(message: UiMessage) {
        applyVisualState(message)
        dismissRunnable?.let { handler.removeCallbacks(it) }

        if (!isVisible) {
            isVisible = true
            alpha = 0f
            translationY = -dp(20)
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        if (message.autoDismissMillis > 0L) {
            dismissRunnable = Runnable { dismissMessage() }
            dismissRunnable?.let { runnable ->
                handler.postDelayed(runnable, message.autoDismissMillis)
            }
        }
    }

    fun dismissMessage() {
        if (!isVisible) return

        dismissRunnable?.let { handler.removeCallbacks(it) }
        animate()
            .alpha(0f)
            .translationY(-dp(12))
            .setDuration(180)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                isVisible = false
            }
            .start()
    }

    override fun onDetachedFromWindow() {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        super.onDetachedFromWindow()
    }

    private fun applyVisualState(message: UiMessage) {
        val style = styleFor(message.type)

        binding.ivTypeIcon.setImageResource(style.iconRes)
        binding.ivTypeIcon.setColorFilter(style.accentColor)
        binding.tvTitle.setTextColor(style.accentColor)
        binding.tvTitle.text = message.title

        binding.tvDescription.text = message.description
        binding.tvDescription.isVisible = message.description.isNotBlank()

        binding.btnDismiss.setImageResource(R.drawable.ic_message_close_24)
        binding.btnDismiss.setColorFilter(style.accentColor)
        binding.btnDismiss.isVisible = message.isDismissible
    }

    private fun styleFor(type: MessageType): MessageStyle {
        return when (type) {
            MessageType.SUCCESS -> MessageStyle(
                accentColor = ContextCompat.getColor(context, R.color.message_success),
                iconRes = R.drawable.ic_message_success_24
            )

            MessageType.ERROR -> MessageStyle(
                accentColor = ContextCompat.getColor(context, R.color.message_error),
                iconRes = R.drawable.ic_message_error_24
            )

            MessageType.WARNING -> MessageStyle(
                accentColor = ContextCompat.getColor(context, R.color.message_warning),
                iconRes = R.drawable.ic_message_warning_24
            )

            MessageType.INFO -> MessageStyle(
                accentColor = ContextCompat.getColor(context, R.color.message_info),
                iconRes = R.drawable.ic_message_info_24
            )
        }
    }

    private fun dp(value: Int): Float {
        return value * resources.displayMetrics.density
    }

    private data class MessageStyle(
        val accentColor: Int,
        val iconRes: Int
    )
}
