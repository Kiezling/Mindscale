package com.kieslingdev.mindscale.ui.theme

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `docs/specs/SPEC-remaining-screens-visual.md` D-14 and Invariant 12 — the closing audit
 * `SPEC-visual-foundation.md` D-14 promised to Phase 18.
 *
 * That decision said the 246 hardcoded dimension literals would not all be retired at once, that
 * they would be converted screen by screen in the phase that already restyles each screen, and that
 * "Phase 18's closing audit asserts that no `.dp` or `.sp` literal outside `ui/theme` remains that
 * is not either a token reference or a documented one-off."
 *
 * This is that assertion, written as a source scan. Two things it is deliberately not:
 *
 * - **Not a lint rule.** A custom lint check is a toolchain change, which D-1 forbids.
 * - **Not a count.** 138 literals exist in `app/src/main/java`; 26 are the scale definitions in
 *   `ui/theme` itself and 39 are already documented named constants (Insights 31, Track 6, Log 2).
 *   Counting them would fail on the documented ones and pass on a screen that quietly reintroduced
 *   an undocumented `16.dp`. The audit distinguishes *documented* from *undocumented* instead.
 *
 * A literal is documented when it is the initialiser of a `private val` whose declaration carries a
 * KDoc block or a `//` comment on the line above. That is the shape Phase 16 and Phase 17 used for
 * every value they could not put on the scale — `RowNumeralColumnWidth`, `CircleSize`, the raster's
 * cell geometry — and it is the shape that makes a one-off reviewable rather than mysterious.
 */
class MsDimensionAuditTest {

    private companion object {
        /**
         * Walked from the module directory rather than from a resource, because the thing under
         * audit is the source tree itself. Gradle runs JVM tests with the module as the working
         * directory; the fallback covers a run rooted at the repository.
         */
        val SOURCE_ROOT: File = listOf(
            File("src/main/java/com/kieslingdev/mindscale"),
            File("app/src/main/java/com/kieslingdev/mindscale")
        ).first { it.isDirectory }

        /** The scale's own home. Everything here is a definition, not a use. */
        const val EXCLUDED_DIRECTORY = "ui/theme"

        /**
         * The documented one-offs the app is expected to hold, by file and count.
         *
         * This is an exact expectation rather than a floor, and that is the point: a later phase
         * cannot delete a comment to make [noUndocumentedDimensionLiteralSurvivesOutsideTheThemePackage]
         * pass, and cannot add a screen constant without this test naming the file it landed in.
         * If a screen legitimately earns another one-off, update this map in the same commit and
         * say why in the governing spec.
         *
         * Every entry is chart or figure geometry that a *spacing* scale should not absorb —
         * Insights' raster rows, plot area, bar wells and legend swatches; Track's numpad pad and
         * entry dot; Log's numeral column and row indent; Breathing's 224 dp pacing circle.
         */
        val EXPECTED_DOCUMENTED = mapOf(
            "InsightsScreen.kt" to 31,
            "TrackScreen.kt" to 6,
            "LogScreen.kt" to 2,
            "BreathingScreen.kt" to 1
        )

        val LITERAL = Regex("""\b\d+(?:\.\d+)?\.(?:dp|sp)\b""")

        /** `private val Name: Dp = 34.dp` or `private val Name = 34.dp`. */
        val DOCUMENTED_CONSTANT = Regex("""^\s*private val \w+(\s*:\s*[\w.]+)?\s*=\s*.*""")
    }

    private data class Finding(val file: String, val line: Int, val text: String)

    /**
     * Every `.kt` file under `app/src/main/java` except `ui/theme`, with its lines classified.
     * Returns undocumented findings first and the documented ones grouped by file, so a failure
     * message names the exact site rather than a number.
     */
    private fun scan(): Pair<List<Finding>, Map<String, Int>> {
        val undocumented = mutableListOf<Finding>()
        val documented = mutableMapOf<String, Int>()

        SOURCE_ROOT.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.invariantSeparatorsPath.contains(EXCLUDED_DIRECTORY) }
            .sortedBy { it.invariantSeparatorsPath }
            .forEach { file ->
                val lines = file.readLines()
                var inBlockComment = false
                lines.forEachIndexed { index, raw ->
                    val line = raw.trim()

                    // Comment tracking first: a literal quoted inside KDoc is prose, not code.
                    val wasInBlockComment = inBlockComment
                    if (inBlockComment) {
                        if (line.contains("*/")) inBlockComment = false
                    } else if (line.startsWith("/*")) {
                        inBlockComment = !line.contains("*/")
                    }
                    if (wasInBlockComment || line.startsWith("*") || line.startsWith("//")) return@forEachIndexed

                    // Strip a trailing line comment so `foo(8.dp) // was 10.dp` counts once.
                    val code = line.substringBefore("//")
                    if (!LITERAL.containsMatchIn(code)) return@forEachIndexed

                    val hasComment = DOCUMENTED_CONSTANT.matches(code) && precededByComment(lines, index)
                    val count = LITERAL.findAll(code).count()
                    if (hasComment) {
                        documented[file.name] = (documented[file.name] ?: 0) + count
                    } else {
                        LITERAL.findAll(code).forEach { match ->
                            undocumented += Finding(file.name, index + 1, match.value)
                        }
                    }
                }
            }
        return undocumented to documented
    }

    /**
     * A comment heading the declaration, skipping blank lines **and sibling `private val`
     * declarations**.
     *
     * The second part is what makes this match how the code is actually written. Phase 17 groups
     * its chart geometry as one KDoc over a run of related constants — four raster row heights
     * under "Raster row heights, by how many days the range covers" — and requiring a comment
     * immediately above each line would either fail that or push the codebase toward one comment
     * per constant, which is noise rather than documentation. What D-14 asks for is that a reader
     * can see why a value is not on the scale, and a heading over its group does that.
     */
    private fun precededByComment(lines: List<String>, index: Int): Boolean {
        var i = index - 1
        while (i >= 0 && (lines[i].isBlank() || DOCUMENTED_CONSTANT.matches(lines[i].substringBefore("//")))) i--
        if (i < 0) return false
        val above = lines[i].trim()
        // `/**…*/` on one line, the closing `*/` of a block, a continuation `*`, or a `//` line.
        return above.startsWith("//") || above.startsWith("/*") ||
            above.startsWith("*/") || above.startsWith("*")
    }

    /**
     * The audit itself. Failure names every site, because "there are 12 undocumented literals" is
     * not something a reader can act on and "SettingsScreen.kt:167 holds 14.dp" is.
     */
    @Test
    fun noUndocumentedDimensionLiteralSurvivesOutsideTheThemePackage() {
        val (undocumented, _) = scan()
        assertTrue(
            "SPEC-visual-foundation.md D-14 and SPEC-remaining-screens-visual.md D-14 require every " +
                ".dp/.sp literal outside ui/theme to be a token reference or a documented one-off. " +
                "${undocumented.size} are neither:\n" +
                undocumented.joinToString("\n") { "  ${it.file}:${it.line} — ${it.text}" },
            undocumented.isEmpty()
        )
    }

    /**
     * The other half of the audit, and the reason it is not a count: the documented one-offs are
     * expected to *exist*. Asserting the exact set means a later phase cannot quietly delete a
     * comment to make the first test pass, and cannot add a new undocumented screen constant
     * without this test noticing which file it landed in.
     */
    @Test
    fun theDocumentedOneOffsAreExactlyTheOnesTheSpecNames() {
        val (_, documented) = scan()
        assertEquals(
            "The documented one-offs moved. If a screen legitimately gained or lost one, update " +
                "EXPECTED_DOCUMENTED in the same commit and say why in the spec — do not relax it.",
            EXPECTED_DOCUMENTED.toSortedMap(),
            documented.toSortedMap()
        )
    }

    /** The scan has to be looking at something, or both tests above pass vacuously. */
    @Test
    fun theScanActuallyReadsTheSourceTree() {
        val files = SOURCE_ROOT.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.invariantSeparatorsPath.contains(EXCLUDED_DIRECTORY) }
            .count()
        assertTrue("Expected the main source tree, found $files .kt files", files > 50)
    }
}
