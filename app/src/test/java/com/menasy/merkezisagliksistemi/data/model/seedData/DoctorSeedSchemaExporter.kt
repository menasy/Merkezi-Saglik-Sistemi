package com.menasy.merkezisagliksistemi.data.model.seedData

import com.menasy.merkezisagliksistemi.data.model.Doctor
import java.io.File

/**
 * Test/raporlama için doktor seed listesini okunabilir JSON çıktısı olarak dışa aktarır.
 */
object DoctorSeedSchemaExporter {

    fun toJson(doctorList: List<Doctor> = doctors): String {
        val body = doctorList.joinToString(separator = ",\n") { doctor ->
            buildString {
                append("    {\n")
                append("      \"id\": \"${escapeJson(doctor.id)}\",\n")
                append("      \"userId\": ${nullableString(doctor.userId)},\n")
                append("      \"fullName\": \"${escapeJson(doctor.fullName)}\",\n")
                append("      \"branchId\": \"${escapeJson(doctor.branchId)}\",\n")
                append("      \"hospitalId\": \"${escapeJson(doctor.hospitalId)}\",\n")
                append("      \"roomInfo\": \"${escapeJson(doctor.roomInfo)}\",\n")
                append("      \"slotStartHour\": ${doctor.slotStartHour},\n")
                append("      \"slotEndHour\": ${doctor.slotEndHour},\n")
                append("      \"slotDurationMinutes\": ${doctor.slotDurationMinutes},\n")
                append("      \"canLogin\": ${doctor.canLogin}\n")
                append("    }")
            }
        }

        return buildString {
            append("{\n")
            append("  \"meta\": {\n")
            append("    \"totalDoctors\": ${doctorList.size},\n")
            append("    \"loginEnabledDoctors\": ${doctorList.count { it.canLogin }}\n")
            append("  },\n")
            append("  \"doctors\": [\n")
            append(body)
            append("\n  ]\n")
            append("}\n")
        }
    }

    fun writeTo(file: File, doctorList: List<Doctor> = doctors): File {
        file.parentFile?.mkdirs()
        file.writeText(toJson(doctorList))
        return file
    }

    private fun nullableString(value: String?): String {
        return if (value.isNullOrBlank()) {
            "null"
        } else {
            "\"${escapeJson(value)}\""
        }
    }

    private fun escapeJson(value: String): String {
        return buildString(value.length + 8) {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
    }
}
