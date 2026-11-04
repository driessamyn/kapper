@file:JvmSynthetic

package net.samyn.kapper.internal

import net.samyn.kapper.KapperParseException
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a Query.
 *
 * @param template the query template
 * @property sql the JDBC formatted query string with placeholders
 * @property tokens the tokens and their indexes in the query string
 */
data class Query(
    val template: String,
    private val queryParser: (String) -> Pair<String, Map<String, List<Int>>> = ::parseQuery,
) {
    val sql: String
    val tokens: Map<String, List<Int>>

    init {
        val (sql: String, tokens: Map<String, List<Int>>) = queryParser(template)
        this.sql = sql
        this.tokens = tokens
    }
}

/**
 * A bounded, thread-safe cache of parsed [Query] instances keyed on the SQL template.
 *
 * Parsing is a pure function of the template, so the cache cannot go stale. It is bounded so that a
 * caller generating unique SQL on every call cannot exhaust the heap: once [maxSize] distinct
 * templates have been seen, further templates are parsed but not retained.
 *
 * The bound is configurable via [maxSize]. The cache stores the parsed template rather than the
 * final SQL, so callers that derive per-call SQL from a shared template can reuse it.
 */
internal class QueryCache(
    private val maxSize: Int = DEFAULT_MAX_SIZE,
    private val parser: (String) -> Query = { Query(it) },
) : (String) -> Query {
    private val cache = ConcurrentHashMap<String, Query>()
    private val insertLock = Any()

    val size: Int
        get() = cache.size

    override fun invoke(template: String): Query {
        cache[template]?.let { return it }
        val query = parser(template)
        // The size check and the insert must happen under the same lock: otherwise concurrent
        // callers can both pass the check and push the cache past maxSize.
        synchronized(insertLock) {
            if (cache.size < maxSize) {
                cache.putIfAbsent(template, query)
            }
        }
        return cache[template] ?: query
    }

    companion object {
        const val DEFAULT_MAX_SIZE = 1000
    }
}

internal fun parseQuery(template: String): Pair<String, Map<String, List<Int>>> {
    val sqlBuilder = StringBuilder()
    val tokens = mutableMapOf<String, MutableList<Int>>()
    var tokenIndex = 0
    var tokenNameBuilder: StringBuilder? = null

    val trimmed = template.trim()
    trimmed.forEachIndexed { i, c ->
        when {
            // Token start char, and it follows at least one valid token character
            c.isTokenStart() &&
                // doubled-up placeholders should not result in token, this is to support e.g. type casting in PostgreSQL
                trimmed.getOrNull(i - 1)?.isTokenStart() != true &&
                trimmed.getOrNull(i + 1)?.isValidTokenChar() == true -> {
                tokenNameBuilder = StringBuilder()
                tokenIndex++
                sqlBuilder.append('?')
            }
            tokenNameBuilder != null -> {
                // valid token character
                if (c.isValidTokenChar()) {
                    tokenNameBuilder.append(c)
                    // valid token separator character
                } else if (c.isValidTokenSeparator()) {
                    tokens.getOrPut(tokenNameBuilder.toString()) { mutableListOf() }.add(tokenIndex)
                    tokenNameBuilder = null
                    sqlBuilder.append(c)
                } else {
                    throw KapperParseException(
                        buildString {
                            append("'$c' is not a valid character part of the template token.")
                            append('\n')
                            // Show the query up to the end of the offending line, then point the
                            // caret at the offending character on that line. CR and LF both count
                            // as line breaks.
                            val lineBreaks = charArrayOf('\n', '\r')
                            val lineStart = trimmed.lastIndexOfAny(lineBreaks, i - 1) + 1
                            val lineEnd = trimmed.indexOfAny(lineBreaks, i).takeIf { it >= 0 } ?: trimmed.length
                            append(trimmed, 0, lineEnd)
                            append('\n')
                            // Mirror the offending line's prefix so a tab still lines up with the caret.
                            for (index in lineStart until i) {
                                append(if (trimmed[index] == '\t') '\t' else ' ')
                            }
                            append('^')
                        },
                    )
                }
            }
            else -> sqlBuilder.append(c)
        }
    }

    // Handle case where token is at the end of the string
    tokenNameBuilder?.let {
        tokens.getOrPut(tokenNameBuilder.toString()) { mutableListOf() }.add(tokenIndex)
    }

    return sqlBuilder.toString() to tokens
}

private fun Char.isTokenStart() = this == ':' || this == '@'

private fun Char.isValidTokenChar() = this.isLetterOrDigit() || this == '_' || this == '-'

private fun Char.isValidTokenSeparator() =
    this == ' ' || this == ',' || this == ')' || this == ';' || this == '\n' || this == '\t' || this == '\r'
