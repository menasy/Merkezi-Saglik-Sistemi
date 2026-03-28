package com.menasy.merkezisagliksistemi.data.repository

import com.menasy.merkezisagliksistemi.data.remote.firebase.ReferenceDataSource

class ReferenceDataRepository(
    private val referenceDataSource: ReferenceDataSource
) {
    suspend fun ensureReferenceDataInitialized(): Result<Unit> {
        return referenceDataSource.ensureReferenceDataInitialized()
    }
}
