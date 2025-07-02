package net.samyn.kapper.internal

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import net.samyn.kapper.KapperParseException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

class QueryParserTest {
    @ParameterizedTest
    @MethodSource("provideQueryTemplates")
    fun `should parse query templates correctly`(
        template: String,
        expectedSql: String,
        expectedTokens: Map<String, Int>,
    ) {
        val (actualSql, actualTokens) = parseQuery(template)
        assertSoftly {
            actualSql.shouldBe(expectedSql)
            actualTokens.shouldContainExactly(expectedTokens)
        }
    }

    @Test
    fun `when no tokens return empty map`() {
        val template = "SELECT * FROM super_hero"
        val query = parseQuery(template)
        query.shouldBe(template to emptyMap())
    }

    @Test
    fun `when token is at the beginning of the query`() {
        val template = ":id = 1 AND name = 'John'"
        val (sql, tokens) = parseQuery(template)
        sql.shouldBe("? = 1 AND name = 'John'")
        tokens.shouldContainExactly(mapOf("id" to listOf(1)))
    }

    @Test
    fun `when tokens are repeated`() {
        val template = "WHERE id = :id OR parent_id = :id"
        val (sql, tokens) = parseQuery(template)
        sql.shouldBe("WHERE id = ? OR parent_id = ?")
        tokens.shouldContainExactly(mapOf("id" to listOf(1, 2)))
    }

    @Test
    fun `when token contains numbers`() {
        val template = "WHERE id = :id1 AND name = :name2"
        val (sql, tokens) = parseQuery(template)
        sql.shouldBe("WHERE id = ? AND name = ?")
        tokens.shouldContainExactly(
            mapOf("id1" to listOf(1), "name2" to listOf(2)),
        )
    }

    @Test
    fun `when query is very large`() {
        val largeTemplate = "SELECT * FROM table WHERE " + (1..1000).joinToString(" AND ") { "column$it = :param$it" }
        val (sql, tokens) = parseQuery(largeTemplate)
        sql.shouldBe("SELECT * FROM table WHERE " + (1..1000).joinToString(" AND ") { "column$it = ?" })
        tokens.size.shouldBe(1000)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "!", "@", "#", "$", "%", "^", "&", "*", "(", "+", "=", "{", "}", "[", "]", ":", "'",
            "\"", "<", ">", ".", "?", "/", "\\", "|", "~", "`",
        ],
    )
    fun `when invalid token character is used`(invalidChar: String) {
        val template = "WHERE id = :id$invalidChar AND name = :name"
        assertThrows<KapperParseException> {
            parseQuery(template)
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "a", "B", "1", "-", "_",
        ],
    )
    fun `when valid token character is used`(validChar: String) {
        val template = "WHERE id=:$validChar"
        val (sql, tokens) = parseQuery(template)
        sql.shouldBe("WHERE id=?")
        tokens.shouldContainExactly(
            mapOf(validChar to listOf(1)),
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings = [" ", ",", ")", ";", "\n", "\t", "\r"],
    )
    fun `when valid token separator character is used`(invalidChar: String) {
        val template = "WHERE id = :id$invalidChar AND name = :name"
        assertDoesNotThrow {
            parseQuery(template)
        }
    }

    @Test
    fun `when token start but no name, ignore`() {
        val template = "WHERE : @ id = :id"
        val (sql, tokens) = parseQuery(template)
        sql.shouldBe("WHERE : @ id = ?")
        tokens.shouldContainExactly(
            mapOf("id" to listOf(1)),
        )
    }

    @Test
    fun `when token start at end of query, ignore`() {
        val template = "WHERE id = :id AND :"
        val (sql, tokens) = parseQuery(template)
        sql.shouldBe("WHERE id = ? AND :")
        tokens.shouldContainExactly(
            mapOf("id" to listOf(1)),
        )
    }

    @Test
    fun `strip query padding`() {
        val template = "    SELECT foo   "
        val (sql, _) = parseQuery(template)
        sql.shouldBe("SELECT foo")
    }

    @Test
    fun `double colon should not be recognised as token`() {
        val template = "SELECT id::text, title, created_at as createdAt, content FROM blogs;"
        val (sql, tokens) = parseQuery(template)
        sql.shouldBe("SELECT id::text, title, created_at as createdAt, content FROM blogs;")
        tokens.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("validTokenStarts")
    fun `valid token starts`(
        template: String,
        expectedSql: String,
    ) {
        val (sql, tokens) = parseQuery(template)
        sql.shouldBe(expectedSql)
        tokens.shouldContainExactly(
            mapOf("id" to listOf(1)),
        )
    }

    companion object {
        @JvmStatic
        fun provideQueryTemplates() =
            listOf(
                Arguments.of(
                    "SELECT * FROM super_hero WHERE id = :id",
                    "SELECT * FROM super_hero WHERE id = ?",
                    mapOf("id" to listOf(1)),
                ),
                Arguments.of(
                    "SELECT * FROM super_hero WHERE id = @id",
                    "SELECT * FROM super_hero WHERE id = ?",
                    mapOf("id" to listOf(1)),
                ),
                Arguments.of(
                    "INSERT INTO super_hero(id, name) VALUES (:id, :name) RETURNING *",
                    "INSERT INTO super_hero(id, name) VALUES (?, ?) RETURNING *",
                    mapOf("id" to listOf(1), "name" to listOf(2)),
                ),
                Arguments.of(
                    "UPDATE super_hero SET name = :name WHERE id = :id",
                    "UPDATE super_hero SET name = ? WHERE id = ?",
                    mapOf("name" to listOf(1), "id" to listOf(2)),
                ),
                Arguments.of(
                    "UPDATE super_hero SET name = :name WHERE id = :id;",
                    "UPDATE super_hero SET name = ? WHERE id = ?;",
                    mapOf("name" to listOf(1), "id" to listOf(2)),
                ),
            )

        @JvmStatic
        fun validTokenStarts() =
            listOf(
                Arguments.of("WHERE id=:id", "WHERE id=?"),
                Arguments.of("WHERE id= :id", "WHERE id= ?"),
                Arguments.of("WHERE id IN( :id )", "WHERE id IN( ? )"),
                Arguments.of("WHERE id IN(:id)", "WHERE id IN(?)"),
                Arguments.of("WHERE id IN(:id,'foo')", "WHERE id IN(?,'foo')"),
                Arguments.of("WHERE id IN('foo',:id)", "WHERE id IN('foo',?)"),
                Arguments.of("WHERE id IN('foo', :id)", "WHERE id IN('foo', ?)"),
                Arguments.of("WHERE id IN('foo',:id,'bar')", "WHERE id IN('foo',?,'bar')"),
                Arguments.of("WHERE id IN('foo', :id, 'bar')", "WHERE id IN('foo', ?, 'bar')"),
            )
    }
}
