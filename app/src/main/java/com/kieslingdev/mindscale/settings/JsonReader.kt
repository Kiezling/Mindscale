package com.kieslingdev.mindscale.settings

/**
 * A bounded, strict, dependency-free JSON reader for Phase 12 import.
 *
 * It exists for the same reason the Phase 4 encoder in `DataExport.kt` is hand-written:
 * no serialization dependency is justified, and import needs stricter behavior than a
 * general-purpose parser gives. In particular it refuses duplicate object keys, refuses
 * trailing content, refuses unescaped control characters and unpaired surrogates in
 * strings, refuses `NaN`/`Infinity`/leading zeros/leading `+`, and enforces a nesting
 * depth and total value budget so a hostile file cannot exhaust the stack or the heap
 * (see `docs/specs/SPEC-import-restore.md`, D-5 and D-6).
 *
 * Numbers keep their raw lexeme. Nothing is coerced here; the caller decides whether an
 * integer was required and applies the frozen bounds.
 */
sealed interface JsonValue {
    data class Obj(val members: Map<String, JsonValue>) : JsonValue
    data class Arr(val items: List<JsonValue>) : JsonValue
    data class Str(val value: String) : JsonValue
    data class Num(val lexeme: String) : JsonValue
    data class Bool(val value: Boolean) : JsonValue
    data object Null : JsonValue
}

/** Structural failure with a 1-based position. Position is structure, never content. */
class JsonSyntaxException(val line: Int, val column: Int) :
    Exception(null, null, false, false)

fun parseJson(text: String): JsonValue = JsonReader(text).readDocument()

private class JsonReader(private val text: String) {

    private var index = 0
    private var valueBudget = MAX_JSON_VALUES

    fun readDocument(): JsonValue {
        skipWhitespace()
        val value = readValue(depth = 1)
        skipWhitespace()
        if (index != text.length) fail()
        return value
    }

    private fun readValue(depth: Int): JsonValue {
        if (depth > MAX_JSON_DEPTH) fail()
        if (--valueBudget < 0) fail()
        return when (peek()) {
            '{' -> readObject(depth)
            '[' -> readArray(depth)
            '"' -> JsonValue.Str(readString())
            't' -> JsonValue.Bool(readLiteral("true", true))
            'f' -> JsonValue.Bool(readLiteral("false", false))
            'n' -> readLiteral("null", JsonValue.Null)
            else -> readNumber()
        }
    }

    private fun readObject(depth: Int): JsonValue.Obj {
        expect('{')
        // LinkedHashMap: member order is retained so a caller can rely on it, but the
        // format never depends on ordering.
        val members = LinkedHashMap<String, JsonValue>()
        skipWhitespace()
        if (peek() == '}') {
            index++
            return JsonValue.Obj(members)
        }
        while (true) {
            skipWhitespace()
            if (peek() != '"') fail()
            val keyStart = index
            val key = readString()
            skipWhitespace()
            expect(':')
            skipWhitespace()
            val value = readValue(depth + 1)
            // A duplicate key is syntactically permitted by JSON but is ambiguous, and
            // ambiguity is never resolved silently (D-6).
            if (members.put(key, value) != null) fail(keyStart)
            skipWhitespace()
            when (next()) {
                ',' -> Unit
                '}' -> return JsonValue.Obj(members)
                else -> fail(index - 1)
            }
        }
    }

    private fun readArray(depth: Int): JsonValue.Arr {
        expect('[')
        val items = ArrayList<JsonValue>()
        skipWhitespace()
        if (peek() == ']') {
            index++
            return JsonValue.Arr(items)
        }
        while (true) {
            skipWhitespace()
            items += readValue(depth + 1)
            skipWhitespace()
            when (next()) {
                ',' -> Unit
                ']' -> return JsonValue.Arr(items)
                else -> fail(index - 1)
            }
        }
    }

    private fun readString(): String {
        expect('"')
        val builder = StringBuilder()
        while (true) {
            val c = next()
            when {
                c == '"' -> return builder.toString()
                c == '\\' -> readEscape(builder)
                // Unescaped control characters are invalid JSON. The encoder always
                // escapes them, so their presence means corruption or hand-editing.
                c.code < 0x20 -> fail(index - 1)
                else -> builder.append(c)
            }
        }
    }

    private fun readEscape(builder: StringBuilder) {
        when (next()) {
            '"' -> builder.append('"')
            '\\' -> builder.append('\\')
            '/' -> builder.append('/')
            'b' -> builder.append('\b')
            'f' -> builder.append('\u000C')
            'n' -> builder.append('\n')
            'r' -> builder.append('\r')
            't' -> builder.append('\t')
            'u' -> builder.append(readUnicodeEscape())
            else -> fail(index - 1)
        }
    }

    /** Reads one `\uXXXX`, requiring a complete surrogate pair when one is started. */
    private fun readUnicodeEscape(): String {
        val high = readHex4()
        if (!high.isHighSurrogate()) {
            if (high.isLowSurrogate()) fail(index - 4)
            return high.toString()
        }
        if (index + 1 >= text.length || text[index] != '\\' || text[index + 1] != 'u') {
            fail(index - 4)
        }
        index += 2
        val low = readHex4()
        if (!low.isLowSurrogate()) fail(index - 4)
        return charArrayOf(high, low).concatToString()
    }

    private fun readHex4(): Char {
        if (index + 4 > text.length) fail()
        var value = 0
        repeat(4) {
            val digit = Character.digit(text[index], 16)
            if (digit < 0) fail()
            value = value * 16 + digit
            index++
        }
        return value.toChar()
    }

    private fun <T> readLiteral(literal: String, value: T): T {
        if (!text.startsWith(literal, index)) fail()
        index += literal.length
        return value
    }

    /**
     * Strict RFC 8259 grammar: optional `-`, no leading `+`, no leading zeros, no bare
     * `.`, and no `NaN`/`Infinity`. The lexeme is kept verbatim so the caller can insist
     * on an integer without ever seeing a coerced value.
     */
    private fun readNumber(): JsonValue.Num {
        val start = index
        if (peek() == '-') index++
        val intStart = index
        if (index >= text.length || !text[index].isAsciiDigit()) fail()
        if (text[index] == '0') {
            index++
        } else {
            while (index < text.length && text[index].isAsciiDigit()) index++
        }
        if (index == intStart) fail()
        if (index < text.length && text[index] == '.') {
            index++
            val fractionStart = index
            while (index < text.length && text[index].isAsciiDigit()) index++
            if (index == fractionStart) fail()
        }
        if (index < text.length && (text[index] == 'e' || text[index] == 'E')) {
            index++
            if (index < text.length && (text[index] == '+' || text[index] == '-')) index++
            val exponentStart = index
            while (index < text.length && text[index].isAsciiDigit()) index++
            if (index == exponentStart) fail()
        }
        return JsonValue.Num(text.substring(start, index))
    }

    private fun skipWhitespace() {
        while (index < text.length) {
            when (text[index]) {
                ' ', '\t', '\n', '\r' -> index++
                else -> return
            }
        }
    }

    private fun peek(): Char {
        if (index >= text.length) fail()
        return text[index]
    }

    private fun next(): Char {
        if (index >= text.length) fail()
        return text[index++]
    }

    private fun expect(expected: Char) {
        if (next() != expected) fail(index - 1)
    }

    private fun fail(at: Int = index): Nothing {
        var line = 1
        var column = 1
        var scan = 0
        val limit = minOf(at, text.length)
        while (scan < limit) {
            if (text[scan] == '\n') {
                line++
                column = 1
            } else {
                column++
            }
            scan++
        }
        throw JsonSyntaxException(line, column)
    }

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
}

/** Requires an integer-valued number that fits in [Long]; never rounds or truncates. */
internal fun JsonValue.Num.asLongOrNull(): Long? {
    if (lexeme.any { it == '.' || it == 'e' || it == 'E' }) return null
    return lexeme.toLongOrNull()
}

internal fun JsonValue.Num.asIntOrNull(): Int? = asLongOrNull()?.let { value ->
    if (value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) value.toInt() else null
}
