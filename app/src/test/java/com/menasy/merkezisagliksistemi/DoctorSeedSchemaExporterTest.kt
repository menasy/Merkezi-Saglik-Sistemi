package com.menasy.merkezisagliksistemi

import com.menasy.merkezisagliksistemi.data.model.seedData.DoctorSeedSchemaExporter
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DoctorSeedSchemaExporterTest {

    @Test
    fun exportDoctorsSchemaJsonToBuildReports() {
        val outputFile = File("build/reports/seed/DoctorsSchema.json")
        DoctorSeedSchemaExporter.writeTo(outputFile)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0L)
    }
}
