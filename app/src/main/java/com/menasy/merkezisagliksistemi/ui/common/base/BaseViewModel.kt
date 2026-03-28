package com.menasy.merkezisagliksistemi.ui.common.base

import androidx.lifecycle.ViewModel
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorMapper
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import com.menasy.merkezisagliksistemi.ui.common.error.OperationType
import com.menasy.merkezisagliksistemi.ui.common.message.UiEvent
import com.menasy.merkezisagliksistemi.ui.common.message.UiMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

abstract class BaseViewModel : ViewModel() {

    private val _uiEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 32)
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    protected fun publishMessage(message: UiMessage) {
        _uiEvents.tryEmit(UiEvent.ShowMessage(message))
    }

    protected fun publishSuccess(title: String, description: String) {
        publishMessage(UiMessage.success(title = title, description = description))
    }

    protected fun publishInfo(title: String, description: String) {
        publishMessage(UiMessage.info(title = title, description = description))
    }

    protected fun publishWarning(title: String, description: String) {
        publishMessage(UiMessage.warning(title = title, description = description))
    }

    protected fun publishError(
        throwable: Throwable?,
        operationType: OperationType = OperationType.GENERAL
    ) {
        publishMessage(AppErrorMapper.map(throwable = throwable, operationType = operationType))
    }

    protected fun publishError(reason: AppErrorReason) {
        publishMessage(AppErrorMapper.mapReason(reason))
    }
}
