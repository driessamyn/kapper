package net.samyn.kapper

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import net.samyn.kapper.internal.getDbFlavour
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.util.UUID

class ExecuteDtoTests : AbstractDbTests() {
    @Test
    fun `SQL Insert single DTO`() {
        val supermanClone = superman.copy(id = UUID.randomUUID())
        val results =
            connection.execute(
                """
                INSERT INTO super_heroes_$testId(id, name, email, age) VALUES(:id, :name, :email, :age)
                """.trimIndent(),
                supermanClone,
                "id" to SuperHero::id,
                "name" to SuperHero::name,
                "email" to SuperHero::email,
                "age" to SuperHero::age,
            )
        results.shouldBe(1)
        connection.prepareStatement(
            "SELECT name, email, age  FROM super_heroes_$testId " +
                "WHERE id = ${convertUUIDString(supermanClone.id, connection.getDbFlavour())}",
        ).use { stmt ->
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
    fun `SQL Update single DTO`() {
        val batmanClone = batman.copy(id = UUID.randomUUID())
        connection.createStatement().use { stmt ->
            stmt.execute(
                "INSERT INTO super_heroes_$testId(id, name) " +
                    "VALUES(${convertUUIDString(batmanClone.id, connection.getDbFlavour())}, 'foo')",
            )
        }
        val updated = batmanClone.copy(name = "Batman Updated", email = "batman@wayne.com", age = 42)
        val results =
            connection.execute(
                "UPDATE super_heroes_$testId SET name = :name, email = :email, age = :age WHERE id = :id",
                updated,
                "id" to SuperHero::id,
                "name" to SuperHero::name,
                "email" to SuperHero::email,
                "age" to SuperHero::age,
            )
        results.shouldBe(1)
        connection.prepareStatement(
            "SELECT name, email, age FROM super_heroes_$testId " +
                "WHERE id = ${convertUUIDString(batmanClone.id, connection.getDbFlavour())}",
        ).use { stmt ->
            val resultSet = stmt.executeQuery()
            assertSoftly(resultSet) {
                next().shouldBe(true)
                getString(1).shouldBe(updated.name)
                getString(2).shouldBe(updated.email)
                getInt(3).shouldBe(updated.age)
            }
        }
    }

    @Test
    fun `SQL Insert multiple DTOs with executeAll`() {
        val heroes =
            listOf(
                superman.copy(id = UUID.randomUUID()),
                batman.copy(id = UUID.randomUUID()),
                spiderMan.copy(id = UUID.randomUUID()),
            )
        val results =
            connection.executeAll(
                "INSERT INTO super_heroes_$testId(id, name, email, age) VALUES(:id, :name, :email, :age)",
                heroes,
                "id" to SuperHero::id,
                "name" to SuperHero::name,
                "email" to SuperHero::email,
                "age" to SuperHero::age,
            )
        results.shouldBe(intArrayOf(1, 1, 1))
    }

    @Test
    fun `SQL Update multiple DTOs with executeAll`() {
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
        val updatedHeroes = ids.map { SuperHero(it, "bar", "bar@bar.com", 99) }
        val results =
            connection.executeAll(
                "UPDATE super_heroes_$testId SET name = :name, email = :email, age = :age WHERE id = :id",
                updatedHeroes,
                "id" to SuperHero::id,
                "name" to SuperHero::name,
                "email" to SuperHero::email,
                "age" to SuperHero::age,
            )
        results.shouldBe(intArrayOf(1, 1, 1))
    }

    @Test
    fun `with TX and DTO completes`() {
        connection.withTransaction {
            val hero1 = superman.copy(id = UUID.randomUUID())
            val hero2 = batman.copy(id = UUID.randomUUID())
            val results =
                connection.execute(
                    """
                    INSERT INTO super_heroes_$testId(id, name, email) VALUES(:id, :name, :email)
                    """.trimIndent(),
                    hero1,
                    "id" to SuperHero::id,
                    "name" to SuperHero::name,
                    "email" to SuperHero::email,
                ) +
                    connection.execute(
                        """
                        INSERT INTO super_heroes_$testId(id, name, email) VALUES(:id, :name, :email)
                        """.trimIndent(),
                        hero2,
                        "id" to SuperHero::id,
                        "name" to SuperHero::name,
                        "email" to SuperHero::email,
                    )
            results shouldBe 2
        }
    }

    @Test
    fun `with TX and DTO rolls back`() {
        val id = UUID.randomUUID()
        shouldThrow<SQLException> {
            connection.withTransaction {
                repeat(2) {
                    val hero = superman.copy(id = id)
                    connection.execute(
                        """
                        INSERT INTO super_heroes_$testId(id, name, email) VALUES(:id, :name, :email)
                        """.trimIndent(),
                        hero,
                        "id" to SuperHero::id,
                        "name" to SuperHero::name,
                        "email" to SuperHero::email,
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
