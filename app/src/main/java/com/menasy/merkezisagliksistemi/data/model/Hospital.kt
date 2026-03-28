package com.menasy.merkezisagliksistemi.data.model

data class Hospital(
    val id: String = "",              // KURUM KODU
    val cityId: String = "",          // sehir id
    val name: String = "",            // KURUM ADI
    val district: String = "",        // ILCE
    val detsisCode: String = "",      // DETSIS KODU
    val bedCount: Int = 0             // Tescil Edilen Yatak Sayisi
)