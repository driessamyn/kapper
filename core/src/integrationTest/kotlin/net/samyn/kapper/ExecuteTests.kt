package net.samyn.kapper

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID

class ExecuteTests : AbstractDbTests() {
    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `SQL Insert single`(connection: Connection) {
        val supermanClone = superman.copy(id = UUID.randomUUID())
        val results =
            connection.execute(
                """
                INSERT INTO super_heroes_$testId(id, name, email, age) VALUES(:id, :name, :email, :age);
                """.trimIndent(),
                "id" to supermanClone.id,
                "name" to supermanClone.name,
                "email" to supermanClone.email,
                "age" to supermanClone.age,
            )

        results.shouldBe(1)

        connection.prepareStatement("SELECT name, email, age  FROM super_heroes_$testId WHERE id = '${supermanClone.id}'").use { stmt ->
            val resultSet = stmt.executeQuery()
            assertSoftly(resultSet) {
                next().shouldBe(true)
                getString(1).shouldBe(supermanClone.name)
                getString(2).shouldBe(supermanClone.email)
                getInt(3).shouldBe(supermanClone.age)
            }
        }
    }

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `SQL Update single`(connection: Connection) {
        val batmanClone = batman.copy(id = UUID.randomUUID())
        connection.createStatement().use { stmt ->
            stmt.execute("INSERT INTO super_heroes_$testId(id, name) VALUES('${batmanClone.id}', 'foo');")
        }

        val results =
            connection.execute(
                "UPDATE super_heroes_$testId SET name = :name, email = :email, age = :age WHERE id = :id;",
                "id" to batmanClone.id,
                "name" to batmanClone.name,
                "email" to batmanClone.email,
                "age" to batmanClone.age,
            )

        results.shouldBe(1)

        connection.prepareStatement("SELECT name, email, age FROM super_heroes_$testId WHERE id = '${batmanClone.id}'").use { stmt ->
            val resultSet = stmt.executeQuery()
            assertSoftly(resultSet) {
                next().shouldBe(true)
                getString(1).shouldBe(batmanClone.name)
                getString(2).shouldBe(batmanClone.email)
                getInt(3).shouldBe(batmanClone.age)
            }
        }
    }

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `SQL Update multiple`(connection: Connection) {
        val ids = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        val name = "foo-${UUID.randomUUID()}"
        connection.createStatement().use { stmt ->
            ids.forEach {
                stmt.execute("INSERT INTO super_heroes_$testId(id, name) VALUES('$it', '$name');")
            }
        }

        val results =
            connection.execute(
                "UPDATE super_heroes_$testId SET name = :newName WHERE name = :name;",
                "name" to name,
                "newName" to "bar",
            )

        results.shouldBe(ids.size)
    }

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `SQL Delete single`(connection: Connection) {
        val spidermanClone = spiderMan.copy(id = UUID.randomUUID())
        connection.createStatement().use { stmt ->
            stmt.execute("INSERT INTO super_heroes_$testId(id, name) VALUES('${spidermanClone.id}', 'foo');")
        }

        val results =
            connection.execute(
                "DELETE FROM super_heroes_$testId WHERE id = :id;",
                "id" to spidermanClone.id,
            )

        results.shouldBe(1)

        connection.prepareStatement("SELECT * FROM super_heroes_$testId WHERE id = '${spidermanClone.id}'").use { stmt ->
            val resultSet = stmt.executeQuery()
            assertSoftly(resultSet) {
                next().shouldBe(false)
            }
        }
    }

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `SQL Delete multiple`(connection: Connection) {
        val ids = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        val name = "bar-${UUID.randomUUID()}"
        connection.createStatement().use { stmt ->
            ids.forEach {
                stmt.execute("INSERT INTO super_heroes_$testId(id, name) VALUES('$it', '$name');")
            }
        }

        val results =
            connection.execute(
                "DELETE FROM super_heroes_$testId WHERE name = :name;",
                "name" to name,
            )

        results.shouldBe(ids.size)
    }

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `with TX completes`(connection: Connection) {
        connection.withTransaction {
            val results =
                connection.execute(
                    """
                    INSERT INTO super_heroes_$testId(id, name, email) VALUES(:id, :name, :email);
                    """.trimIndent(),
                    "id" to UUID.randomUUID(),
                    "name" to "thor",
                    "email" to "thor@world.com",
                ) +
                    connection.execute(
                        """
                        INSERT INTO super_heroes_$testId(id, name, email) VALUES(:id, :name, :email);
                        """.trimIndent(),
                        "id" to UUID.randomUUID(),
                        "name" to "thor",
                        "email" to "thor@world.com",
                    )
            results shouldBe 2
        }
    }

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `with TX rolls back`(connection: Connection) {
        val id = UUID.randomUUID()
        shouldThrow<SQLException> {
            connection.withTransaction {
                repeat(2) {
                    connection.execute(
                        """
                        INSERT INTO super_heroes_$testId(id, name, email) VALUES(:id, :name, :email);
                        """.trimIndent(),
                        "id" to id,
                        "name" to "thor",
                        "email" to "thor@world.com",
                    )
                }
            }
        }
        connection.query<SuperHero>(
            "SELECT * FROM super_heroes_$testId WHERE id = :id",
            "id" to id,
        ).shouldBeEmpty()
    }
}
