package com.kieslingdev.mindscale.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Hostile-input coverage for the bounded JSON reader (SPEC-import-restore.md, D-5, D-6). */
class JsonReaderTest {

    private fun failsAt(text: String): JsonSyntaxException =
        assertThrows(JsonSyntaxException::class.java) { parseJson(text) }

    @Test
    fun readsObjectsArraysAndScalars() {
        val value = parseJson("""{"a": [1, "x", true, false, null], "b": {"c": -2.5e3}}""")
        val root = value as JsonValue.Obj
        assertEquals(setOf("a", "b"), root.members.keys)
        assertEquals(5, (root.members["a"] as JsonValue.Arr).items.size)
        assertEquals("-2.5e3", ((root.members["b"] as JsonValue.Obj).members["c"] as JsonValue.Num).lexeme)
    }

    @Test
    fun refusesDuplicateObjectKeys() {
        // Ambiguous rather than malformed, and ambiguity is never resolved silently.
        failsAt("""{"id": 1, "id": 2}""")
    }

    @Test
    fun refusesTrailingContent() {
        failsAt("""{"a": 1} {"b": 2}""")
        failsAt("""{"a": 1}x""")
    }

    @Test
    fun refusesNestingDeeperThanTheBudget() {
        val deep = "[".repeat(MAX_JSON_DEPTH + 1) + "]".repeat(MAX_JSON_DEPTH + 1)
        failsAt(deep)
        val atLimit = "[".repeat(MAX_JSON_DEPTH) + "]".repeat(MAX_JSON_DEPTH)
        parseJson(atLimit)
    }

    @Test
    fun refusesNonJsonNumberForms() {
        failsAt("""{"a": NaN}""")
        failsAt("""{"a": Infinity}""")
        failsAt("""{"a": +1}""")
        failsAt("""{"a": 007}""")
        failsAt("""{"a": .5}""")
        failsAt("""{"a": 1.}""")
        failsAt("""{"a": 1e}""")
    }

    @Test
    fun refusesUnterminatedAndControlCharactersInStrings() {
        failsAt("""{"a": "unterminated""")
        failsAt("{\"a\": \"rawbell\"}")
        failsAt("{\"a\": \"raw\nnewline\"}")
    }

    @Test
    fun decodesEscapesAndCompleteSurrogatePairs() {
        val root = parseJson("""{"a": "\"\\\/\b\f\n\r\tA😀"}""") as JsonValue.Obj
        val decoded = (root.members["a"] as JsonValue.Str).value
        assertTrue(decoded.startsWith("\"\\/"))
        assertTrue(decoded.endsWith("A😀"))
    }

    @Test
    fun refusesLoneSurrogateEscapes() {
        failsAt("""{"a": "\uD83D"}""")
        failsAt("""{"a": "\uDE00"}""")
        failsAt("""{"a": "\uD83Dx"}""")
        failsAt("""{"a": "\u00zz"}""")
    }

    @Test
    fun reportsOneBasedLineAndColumn() {
        val failure = failsAt("{\n  \"a\": 1,\n  \"a\": 2\n}")
        assertEquals(3, failure.line)
        assertEquals(3, failure.column)
    }

    @Test
    fun integerAccessorsRefuseFractionsExponentsAndOverflow() {
        assertEquals(7L, JsonValue.Num("7").asLongOrNull())
        assertEquals(null, JsonValue.Num("7.0").asLongOrNull())
        assertEquals(null, JsonValue.Num("7e0").asLongOrNull())
        assertEquals(null, JsonValue.Num("9223372036854775808").asLongOrNull())
        assertEquals(null, JsonValue.Num("2147483648").asIntOrNull())
        assertEquals(2147483647, JsonValue.Num("2147483647").asIntOrNull())
    }
}
