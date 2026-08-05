package com.kieslingdev.mindscale.safety

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A structural guard on the whole production tree
 * (`docs/specs/SPEC-safety-card.md`, D-7, Invariant 4).
 *
 * MindScale must never be able to place a call by itself. `ACTION_DIAL` opens the dialer
 * pre-filled and stops there; the immediate-dial constant would need a telephony
 * permission this app does not have and will not add. A scan is the honest test for
 * "nowhere in the codebase", and it fails loudly if a later change reaches for the
 * convenient-looking constant.
 *
 * Comments are stripped before scanning, so documentation may name what is forbidden —
 * as `SafetyIntents.kt` does — while code may not use it.
 */
class NoAutoDialSourceTest {

    private val banned = listOf(
        "ACTION_CALL",
        "android.intent.action.CALL",
        "CALL_PHONE",
        "SEND_SMS",
        "PROCESS_OUTGOING_CALLS"
    )

    @Test
    fun noProductionCodeUsesTheImmediateDialActionOrATelephonyPermission() {
        val main = productionSourceRoot()
        val sources = main.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }
            .toList()
        assertTrue(
            "Expected to find the production sources under ${main.absolutePath}",
            sources.size > 20
        )

        sources.forEach { file ->
            val code = stripComments(file.readText(), file.extension)
            banned.forEach { token ->
                assertFalse("${file.path} must not contain $token in code", code.contains(token))
            }
        }
    }

    /** The stripper must not be so eager that the guard silently stops guarding. */
    @Test
    fun theCommentStripperKeepsCodeAndDropsOnlyComments() {
        assertEquals("val a = 1\n", stripComments("val a = 1 // ACTION_CALL\n", "kt"))
        assertEquals("\nval b = 2", stripComments("/* ACTION_CALL */\nval b = 2", "kt"))
        // A `//` inside a string literal is not a comment.
        assertTrue(stripComments("""val u = "https://x" + ACTION_CALL""", "kt").contains("ACTION_CALL"))
        assertFalse(stripComments("<!-- CALL_PHONE -->\n<manifest/>", "xml").contains("CALL_PHONE"))
    }

    private fun stripComments(text: String, extension: String): String {
        if (extension == "xml") return text.replace(Regex("<!--[\\s\\S]*?-->"), "")
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
