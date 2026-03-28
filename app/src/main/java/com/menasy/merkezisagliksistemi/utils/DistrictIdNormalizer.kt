package com.menasy.merkezisagliksistemi.utils

import java.text.Normalizer
import java.util.Locale

fun buildDistrictId(cityId: String, districtName: String): String {
    val normalizedDistrict = districtName
        .lowercase(Locale.ROOT)
        .replace("ı", "i")
        .normalizeToAscii()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .replace(Regex("_+"), "_")

    return "${cityId.lowercase(Locale.ROOT)}_$normalizedDistrict"
}

private fun String.normalizeToAscii(): String {
    return Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
}
