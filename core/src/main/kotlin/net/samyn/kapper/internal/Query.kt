@file:JvmSynthetic

package net.samyn.kapper.internal

import net.samyn.kapper.KapperParseException

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
                    throw KapperParseException("'$c' is not a valid character part of the template token.")
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
