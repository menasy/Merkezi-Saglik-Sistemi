package com.menasy.merkezisagliksistemi.ui.common.error

class AppException(
    val reason: AppErrorReason,
    cause: Throwable? = null
) : Exception(reason.name, cause)
