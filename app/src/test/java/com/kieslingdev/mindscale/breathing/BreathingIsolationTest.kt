package com.kieslingdev.mindscale.breathing

import com.kieslingdev.mindscale.data.BreathingSessionDao
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The structural proof behind "never offered to you unprompted"
 * (`docs/specs/SPEC-paced-breathing.md`, D-1, D-10; Invariants 1, 2).
 *
 * A convention that the pacer must not react to recorded data would survive exactly until
 * someone had a good idea. These tests make it a property of the code instead: the
 * ViewModel cannot be constructed with a source of recorded data, and no file in the
 * feature so much as names one.
 *
 * The same scan enforces the other absolute: no retention or fast-breathing path exists,
 * tested or untested. Schmidt (2000) found breathing retraining trending toward poorer
 * outcomes in panic and Barlow discourages respiratory control as a safety behaviour, so
 * both rules are safety rules rather than preferences.
 */
class BreathingIsolationTest {

    /** Anything that would let this feature see what the user recorded. */
    private val forbiddenSources = listOf(
        "EntryDao",
        "SleepDao",
        "MarkerDao",
        "EpisodeSourceDao",
        "TrackSettingsDao",
        "ProfileDao",
        "SafetyPlanDao",
        "DataControlDao",
        "EpisodeEngine",
        "deriveInsights",
        "InsightsViewModel",
        "TrackViewModel"
    )

    /** Anything that would pace a breath hold or a fast rate. */
    private val forbiddenMechanics = listOf(
        "HOLD",
        "Hold",
        "retention",
        "Retention",
        "retain",
        "hyperventil",
        "WimHof",
        "Tummo",
        "boxBreathing",
        "BOX_BREATHING"
    )

    @Test
    fun theViewModelCannotBeGivenASourceOfRecordedData() {
        val constructors = BreathingViewModel::class.java.declaredConstructors
        assertEquals(
            "BreathingViewModel must have exactly one constructor",
            1, constructors.size
        )
        assertEquals(
            listOf(BreathingSessionDao::class.java, BreathingClock::class.java),
            constructors.single().parameterTypes.toList()
        )
    }

    /** Write-only in practice: the pacer cannot react even to its own history. */
    @Test
    fun theSessionDaoExposesNoObservingRead() {
        val names = BreathingSessionDao::class.java.declaredMethods.map { it.name }.toSet()
        assertEquals(setOf("insert", "count"), names)
    }

    @Test
    fun noFileInTheBreathingPackageNamesASourceOfRecordedData() {
        forEachBreathingSource { file, code ->
            forbiddenSources.forEach { token ->
                assertFalse(
                    "${file.path} must not reference $token in code",
                    code.contains(token)
                )
            }
        }
    }

    @Test
    fun noFileInTheBreathingPackageContainsARetentionOrFastBreathingPath() {
        forEachBreathingSource { file, code ->
            forbiddenMechanics.forEach { token ->
                assertFalse(
                    "${file.path} must not contain $token in code",
                    code.contains(token)
                )
            }
        }
    }

    /** The guard must not be so eager, or so narrow, that it silently stops guarding. */
    @Test
    fun theScanActuallyReachesTheFeatureSources() {
        val seen = mutableListOf<String>()
        forEachBreathingSource { file, _ -> seen += file.name }
        assertTrue(
            "Expected the breathing sources, found $seen",
            seen.containsAll(
                listOf(
                    "BreathingPacer.kt",
                    "BreathingContent.kt",
                    "BreathingClock.kt",
                    "BreathingViewModel.kt",
                    "BreathingScreen.kt"
                )
            )
        )
    }

    @Test
    fun theCommentStripperKeepsCodeAndDropsOnlyComments() {
        assertEquals("val a = 1", stripComments("val a = 1 // EntryDao"))
        assertEquals("\nval b = 2", stripComments("/* EntryDao */\nval b = 2"))
        assertTrue(stripComments("""val u = "a//b" + EntryDao""").contains("EntryDao"))
    }

    private fun forEachBreathingSource(block: (File, String) -> Unit) {
        val root = File(productionSourceRoot(), "java/com/kieslingdev/mindscale/breathing")
        assertTrue("Missing ${root.absolutePath}", root.isDirectory)
        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { block(it, stripComments(it.readText())) }
    }

    private fun stripComments(text: String): String {
        val withoutBlocks = text.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
        return withoutBlocks.lineSequence().joinToString("\n") { line ->
            var quoted = false
            var index = 0
            while (index < line.length) {
                val c = line[index]
                when {
                    c == '\\' && quoted -> index++
                    c == '"' -> quoted = !quoted
                    !quoted && c == '/' && index + 1 < line.length && line[index + 1] == '/' ->
                        return@joinToString line.substring(0, index).trimEnd()
                }
                index++
            }
            line
        }
    }

    /** Gradle runs unit tests with the module directory as the working directory. */
    private fun productionSourceRoot(): File =
        listOf(File("src/main"), File("app/src/main"))
            .firstOrNull { it.isDirectory }
            ?: error("Could not locate app/src/main from ${File("").absolutePath}")
}
