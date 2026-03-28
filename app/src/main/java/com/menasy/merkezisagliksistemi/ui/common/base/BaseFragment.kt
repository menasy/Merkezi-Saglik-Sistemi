package com.menasy.merkezisagliksistemi.ui.common.base

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorMapper
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import com.menasy.merkezisagliksistemi.ui.common.error.OperationType
import com.menasy.merkezisagliksistemi.ui.common.message.MessageHost
import com.menasy.merkezisagliksistemi.ui.common.message.UiEvent
import com.menasy.merkezisagliksistemi.ui.common.message.UiMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class BaseFragment : Fragment() {

    protected fun observeUiEvents(events: Flow<UiEvent>) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                events.collect { event ->
                    when (event) {
                        is UiEvent.ShowMessage -> showMessage(event.message)
                    }
                }
            }
        }
    }

    protected fun showMessage(message: UiMessage) {
        (activity as? MessageHost)?.showMessage(message)
    }

    protected fun showError(reason: AppErrorReason) {
        showMessage(AppErrorMapper.mapReason(reason))
    }

    protected fun showError(
        throwable: Throwable?,
        operationType: OperationType = OperationType.GENERAL
    ) {
        showMessage(AppErrorMapper.map(throwable, operationType))
    }
}
