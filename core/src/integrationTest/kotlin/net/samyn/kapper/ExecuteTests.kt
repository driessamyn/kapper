package net.samyn.kapper

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import net.samyn.kapper.internal.getDbFlavour
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.util.UUID

class ExecuteTests : AbstractDbTests() {
    @Test
    fun `SQL Insert single`() {
        val supermanClone = superman.copy(id = UUID.randomUUID())
        val results =
            connection.execute(
                """
                INSERT INTO super_heroes_$testId(id, name, email, age) VALUES(:id, :name, :email, :age)
                """.trimIndent(),
                "id" to supermanClone.id,
                "name" to supermanClone.name,
                "email" to supermanClone.email,
                "age" to supermanClone.age,
            )

        results.shouldBe(1)

        connection.prepareStatement(
            "SELECT name, email, age  FROM super_heroes_$testId " +
                "WHERE id = ${convertUUIDString(supermanClone.id, connection.getDbFlavour())}",
        )
            .use { stmt ->
                val resultSet = stmt.executeQuery()
                resultSet.next().shouldBe(true)
                assertSoftly(resultSet) {
                    getString(1).shouldBe(supermanClone.name)
                    getString(2).shouldBe(supermanClone.email)
                    getInt(3).shouldBe(supermanClone.age)
                }
            }
    }

    @Test
    fun `SQL Update single`() {
        val batmanClone = batman.copy(id = UUID.randomUUID())
        connection.createStatement().use { stmt ->
            stmt.execute(
                "INSERT INTO super_heroes_$testId(id, name) " +
                    "VALUES(${convertUUIDString(batmanClone.id, connection.getDbFlavour())}, 'foo')",
            )
        }

        val results =
            connection.execute(
                "UPDATE super_heroes_$testId SET name = :name, email = :email, age = :age WHERE id = :id",
                "id" to batmanClone.id,
                "name" to batmanClone.name,
                "email" to batmanClone.email,
                "age" to batmanClone.age,
            )

        results.shouldBe(1)

        connection.prepareStatement(
            "SELECT name, email, age FROM super_heroes_$testId " +
                "WHERE id = ${convertUUIDString(batmanClone.id, connection.getDbFlavour())}",
        )
            .use { stmt ->
                val resultSet = stmt.executeQuery()
                assertSoftly(resultSet) {
                    next().shouldBe(true)
                    getString(1).shouldBe(batmanClone.name)
                    getString(2).shouldBe(batmanClone.email)
                    getInt(3).shouldBe(batmanClone.age)
                }
            }
    }

    @Test
    fun `SQL Update multiple`() {
        val ids = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        val name = "foo-${UUID.randomUUID()}"
        connection.createStatement().use { stmt ->
            ids.forEach {
                stmt.execute(
                    "INSERT INTO super_heroes_$testId(id, name) " +
                        "VALUES(${convertUUIDString(it, connection.getDbFlavour())}, '$name')",
                )
            }
        }

        val results =
            connection.execute(
                "UPDATE super_heroes_$testId SET name = :newName WHERE name = :name",
                "name" to name,
                "newName" to "bar",
            )

        results.shouldBe(ids.size)
    }

    @Test
    fun `SQL Delete single`() {
        val spidermanClone = spiderMan.copy(id = UUID.randomUUID())
        connection.createStatement().use { stmt ->
            stmt.execute(
                "INSERT INTO super_heroes_$testId(id, name) " +
                    "VALUES(${convertUUIDString(spidermanClone.id, connection.getDbFlavour())}, 'foo')",
            )
        }

        val results =
            connection.execute(
                "DELETE FROM super_heroes_$testId WHERE id = :id",
                "id" to spidermanClone.id,
            )

        results.shouldBe(1)

        connection.prepareStatement(
            "SELECT * FROM super_heroes_$testId " +
                "WHERE id = ${convertUUIDString(spidermanClone.id, connection.getDbFlavour())}",
        )
            .use { stmt ->
                val resultSet = stmt.executeQuery()
                assertSoftly(resultSet) {
                    next().shouldBe(false)
                }
            }
    }

    @Test
    fun `SQL Delete multiple`() {
        val ids = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        val name = "bar-${UUID.randomUUID()}"
        connection.createStatement().use { stmt ->
            ids.forEach {
                stmt.execute(
                    "INSERT INTO super_heroes_$testId(id, name) " +
                        "VALUES(${convertUUIDString(it, connection.getDbFlavour())}, '$name')",
                )
            }
        }

        val results =
            connection.execute(
                "DELETE FROM super_heroes_$testId WHERE name = :name",
                "name" to name,
            )

        results.shouldBe(ids.size)
    }

    @Test
    fun `with TX completes`() {
        connection.withTransaction {
            val results =
                connection.execute(
                    """
                    INSERT INTO super_heroes_$testId(id, name, email) VALUES(:id, :name, :email)
                    """.trimIndent(),
                    "id" to UUID.randomUUID(),
                    "name" to "thor",
                    "email" to "thor@world.com",
                ) +
                    connection.execute(
                        """
                        INSERT INTO super_heroes_$testId(id, name, email) VALUES(:id, :name, :email)
                        """.trimIndent(),
                        "id" to UUID.randomUUID(),
                        "name" to "thor",
                        "email" to "thor@world.com",
                    )
            results shouldBe 2
        }
    }

    @Test
    fun `with TX rolls back`() {
        val id = UUID.randomUUID()
        shouldThrow<SQLException> {
            connection.withTransaction {
                repeat(2) {
                    connection.execute(
                        """
                        INSERT INTO super_heroes_$testId(id, name, email) VALUES(:id, :name, :email)
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
