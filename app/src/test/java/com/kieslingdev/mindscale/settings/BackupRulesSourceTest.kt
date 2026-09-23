package com.kieslingdev.mindscale.settings

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** R-6: automatic cloud backup and device transfer must not copy local app data. */
class BackupRulesSourceTest {
    private fun appFile(path: String): File {
        val candidates = listOf(File(path), File("app/$path"))
        return candidates.firstOrNull(File::exists)
            ?: error("Could not locate $path")
    }

    @Test
    fun manifestDisablesBackupAndPointsToBothRuleFiles() {
        val manifest = appFile("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:allowBackup=\"false\""))
        assertTrue(manifest.contains("android:fullBackupContent=\"@xml/backup_rules\""))
        assertTrue(manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""))
    }

    @Test
    fun legacyRulesExcludeEverySupportedStorageDomain() {
        assertExcluded(appFile("src/main/res/xml/backup_rules.xml").readText())
    }

    @Test
    fun cloudAndDeviceTransferRulesExcludeEverySupportedStorageDomain() {
        val xml = appFile("src/main/res/xml/data_extraction_rules.xml").readText()
        val cloud = xml.substringAfter("<cloud-backup>").substringBefore("</cloud-backup>")
        val transfer = xml.substringAfter("<device-transfer>").substringBefore("</device-transfer>")
        assertExcluded(cloud)
        assertExcluded(transfer)
    }

    private fun assertExcluded(xml: String) {
        val domains = listOf(
            "root", "file", "database", "sharedpref", "external",
            "device_root", "device_file", "device_database", "device_sharedpref"
        )
        domains.forEach { domain ->
            assertEquals(
                "<exclude domain=\"$domain\" path=\".\" />",
                xml.lines().firstOrNull { it.trim() == "<exclude domain=\"$domain\" path=\".\" />" }?.trim()
            )
        }
    }
}
