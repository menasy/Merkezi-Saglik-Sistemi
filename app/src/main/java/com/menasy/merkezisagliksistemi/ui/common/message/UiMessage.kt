package com.menasy.merkezisagliksistemi.ui.common.message

data class UiMessage(
    val type: MessageType,
    val title: String,
    val description: String,
    val autoDismissMillis: Long = DEFAULT_AUTO_DISMISS,
    val isDismissible: Boolean = true
) {
    companion object {
        const val DEFAULT_AUTO_DISMISS = 3_500L

        fun success(
            title: String,
            description: String,
            autoDismissMillis: Long = DEFAULT_AUTO_DISMISS,
            isDismissible: Boolean = true
        ): UiMessage {
            return UiMessage(
                type = MessageType.SUCCESS,
                title = title,
                description = description,
                autoDismissMillis = autoDismissMillis,
                isDismissible = isDismissible
            )
        }

        fun error(
            title: String,
            description: String,
            autoDismissMillis: Long = DEFAULT_AUTO_DISMISS,
            isDismissible: Boolean = true
        ): UiMessage {
            return UiMessage(
                type = MessageType.ERROR,
                title = title,
                description = description,
                autoDismissMillis = autoDismissMillis,
                isDismissible = isDismissible
            )
        }

        fun warning(
            title: String,
            description: String,
            autoDismissMillis: Long = DEFAULT_AUTO_DISMISS,
            isDismissible: Boolean = true
        ): UiMessage {
            return UiMessage(
                type = MessageType.WARNING,
                title = title,
                description = description,
                autoDismissMillis = autoDismissMillis,
                isDismissible = isDismissible
            )
        }

        fun info(
            title: String,
            description: String,
            autoDismissMillis: Long = DEFAULT_AUTO_DISMISS,
            isDismissible: Boolean = true
        ): UiMessage {
            return UiMessage(
                type = MessageType.INFO,
                title = title,
                description = description,
                autoDismissMillis = autoDismissMillis,
                isDismissible = isDismissible
            )
        }
    }
}
