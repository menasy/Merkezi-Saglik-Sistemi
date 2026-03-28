package com.menasy.merkezisagliksistemi.ui.common.message

sealed interface UiEvent {
    data class ShowMessage(val message: UiMessage) : UiEvent
}
