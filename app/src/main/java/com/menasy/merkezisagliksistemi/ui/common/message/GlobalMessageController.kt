package com.menasy.merkezisagliksistemi.ui.common.message

class GlobalMessageController(
    private val messageView: GlobalMessageView
) {

    fun show(message: UiMessage) {
        messageView.showMessage(message)
    }

    fun dismiss() {
        messageView.dismissMessage()
    }
}
