package net.samyn.kapper

import io.kotest.matchers.shouldBe
import net.samyn.kapper.internal.getDbFlavour
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.util.UUID

class ArrayTypesTest : AbstractDbTests() {
    private val arrayDbs = setOf(DbFlavour.POSTGRESQL, DbFlavour.DUCKDB)

    override fun setupDatabase(connection: Connection) {
        super.setupDatabase(connection)
        val dbFlavour = connection.getDbFlavour()
        if (dbFlavour !in arrayDbs) return
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE array_test_$testId (
                    id ${convertDbColumnType("UUID", dbFlavour)} PRIMARY KEY,
                    int_array INTEGER[],
                    text_array TEXT[]
                )
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `can insert and retrieve integer arrays`() {
        assumeTrue(connection.getDbFlavour() in arrayDbs, "Array types not supported")
        val id = UUID.randomUUID()
        val tags = listOf(10, 20, 30)

        connection.execute(
            "INSERT INTO array_test_$testId (id, int_array) VALUES (:id, :tags)",
            "id" to id,
            "tags" to tags,
        )

        val result =
            connection.querySingle<IntArrayResult>(
                "SELECT id, int_array FROM array_test_$testId WHERE id = :id",
                "id" to id,
            )
        result.shouldBe(IntArrayResult(id, tags))
    }

    @Test
    fun `can insert and retrieve text arrays`() {
        assumeTrue(connection.getDbFlavour() in arrayDbs, "Array types not supported")
        val id = UUID.randomUUID()
        val names = listOf("alpha", "beta", "gamma")

        connection.execute(
            "INSERT INTO array_test_$testId (id, text_array) VALUES (:id, :names)",
            "id" to id,
            "names" to names,
        )

        val result =
            connection.querySingle<TextArrayResult>(
                "SELECT id, text_array FROM array_test_$testId WHERE id = :id",
                "id" to id,
            )
        result.shouldBe(TextArrayResult(id, names))
    }

    @Test
    fun `can handle null arrays`() {
        assumeTrue(connection.getDbFlavour() in arrayDbs, "Array types not supported")
        val id = UUID.randomUUID()

        connection.execute(
            "INSERT INTO array_test_$testId (id) VALUES (:id)",
            "id" to id,
        )

        val result =
            connection.querySingle<NullableArrayResult>(
                "SELECT id, int_array FROM array_test_$testId WHERE id = :id",
                "id" to id,
            )
        result.shouldBe(NullableArrayResult(id, null))
    }

    @Test
    fun `can handle empty arrays`() {
        assumeTrue(connection.getDbFlavour() in arrayDbs, "Array types not supported")
        val id = UUID.randomUUID()
        val empty = emptyList<String>()
        val dbFlavour = connection.getDbFlavour()

        // Empty arrays need special handling — insert via SQL literal
        val emptyArrayLiteral =
            when (dbFlavour) {
                DbFlavour.DUCKDB -> "[]::TEXT[]"
                else -> "'{}'::TEXT[]"
            }
        connection.createStatement().use { stmt ->
            val idSql = convertUUIDString(id, dbFlavour)
            stmt.execute(
                "INSERT INTO array_test_$testId (id, text_array) VALUES ($idSql, $emptyArrayLiteral)",
            )
        }

        val result =
            connection.querySingle<NullableTextArrayResult>(
                "SELECT id, text_array FROM array_test_$testId WHERE id = :id",
                "id" to id,
            )
        result.shouldBe(NullableTextArrayResult(id, empty))
    }

    @Test
    fun `can handle arrays with null elements`() {
        val dbFlavour = connection.getDbFlavour()
        assumeTrue(dbFlavour in arrayDbs, "Array types not supported")
        val id = UUID.randomUUID()

        val insertSql =
            when (dbFlavour) {
                DbFlavour.DUCKDB -> "INSERT INTO array_test_$testId (id, int_array) VALUES ('$id', [1, NULL, 3])"
                else -> "INSERT INTO array_test_$testId (id, int_array) VALUES ('$id', '{1,NULL,3}')"
            }
        connection.createStatement().use { stmt ->
            stmt.execute(insertSql)
        }

        val result =
            connection.querySingle<NullableElementArrayResult>(
                "SELECT id, int_array FROM array_test_$testId WHERE id = :id",
                "id" to id,
            )
        result!!.id.shouldBe(id)
        result.intArray.shouldBe(listOf(1, null, 3))
    }

    data class IntArrayResult(val id: UUID, val intArray: List<Int>)

    data class TextArrayResult(val id: UUID, val textArray: List<String>)

    data class NullableArrayResult(val id: UUID, val intArray: List<Int>? = null)

    data class NullableTextArrayResult(val id: UUID, val textArray: List<String>? = null)

    data class NullableElementArrayResult(val id: UUID, val intArray: List<Int?>)
}
