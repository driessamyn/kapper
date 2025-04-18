package net.samyn.kapper

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import net.samyn.kapper.internal.DbFlavour
import net.samyn.kapper.internal.getDbFlavour
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

class ExecuteTests : AbstractDbTests() {
    @OptIn(ExperimentalUuidApi::class)
    val table = "super_heros_${UUID.randomUUID().toKotlinUuid().toHexString()}"

    override fun setupDatabase(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE $table (
                    id ${if (connection.getDbFlavour() == DbFlavour.MYSQL) "VARCHAR(36)" else "UUID"} PRIMARY KEY,
                    name VARCHAR(100),
                    email VARCHAR(100),
                    age INT
                );
                """.trimIndent().also {
                    println(it)
                },
            )
        }
    }

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `SQL Insert single`(connection: Connection) {
        val results =
            connection.execute(
                """
                INSERT INTO $table(id, name, email, age) VALUES(:id, :name, :email, :age);
                """.trimIndent(),
                "id" to superman.id,
                "name" to superman.name,
                "email" to superman.email,
                "age" to superman.age,
            )

        results.shouldBe(1)

        connection.prepareStatement("SELECT name, email, age  FROM $table WHERE id = '${superman.id}'").use { stmt ->
            val resultSet = stmt.executeQuery()
            assertSoftly(resultSet) {
                next().shouldBe(true)
                getString(1).shouldBe(superman.name)
                getString(2).shouldBe(superman.email)
                getInt(3).shouldBe(superman.age)
            }
        }
    }

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `SQL Update single`(connection: Connection) {
        connection.createStatement().use { stmt ->
            stmt.execute("INSERT INTO $table(id, name) VALUES('${batman.id}', 'foo');")
        }

        val results =
            connection.execute(
                "UPDATE $table SET name = :name, email = :email, age = :age WHERE id = :id;",
                "id" to batman.id,
                "name" to batman.name,
                "email" to batman.email,
                "age" to batman.age,
            )

        results.shouldBe(1)

        connection.prepareStatement("SELECT name, email, age FROM $table WHERE id = '${batman.id}'").use { stmt ->
            val resultSet = stmt.executeQuery()
            assertSoftly(resultSet) {
                next().shouldBe(true)
                getString(1).shouldBe(batman.name)
                getString(2).shouldBe(batman.email)
                getInt(3).shouldBe(batman.age)
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
                stmt.execute("INSERT INTO $table(id, name) VALUES('$it', '$name');")
            }
        }

        val results =
            connection.execute(
                "UPDATE $table SET name = :newName WHERE name = :name;",
                "name" to name,
                "newName" to "bar",
            )

        results.shouldBe(ids.size)
    }

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `SQL Delete single`(connection: Connection) {
        connection.createStatement().use { stmt ->
            stmt.execute("INSERT INTO $table(id, name) VALUES('${spiderMan.id}', 'foo');")
        }

        val results =
            connection.execute(
                "DELETE FROM $table WHERE id = :id;",
                "id" to spiderMan.id,
            )

        results.shouldBe(1)

        connection.prepareStatement("SELECT * FROM $table WHERE id = '${batman.id}'").use { stmt ->
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
                stmt.execute("INSERT INTO $table(id, name) VALUES('$it', '$name');")
            }
        }

        val results =
            connection.execute(
                "DELETE FROM $table WHERE name = :name;",
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
                    INSERT INTO $table(id, name, email) VALUES(:id, :name, :email);
                    """.trimIndent(),
                    "id" to UUID.randomUUID(),
                    "name" to "thor",
                    "email" to "thor@world.com",
                ) +
                    connection.execute(
                        """
                        INSERT INTO $table(id, name, email) VALUES(:id, :name, :email);
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
                        INSERT INTO $table(id, name, email) VALUES(:id, :name, :email);
                        """.trimIndent(),
                        "id" to id,
                        "name" to "thor",
                        "email" to "thor@world.com",
                    )
                }
            }
        }
        connection.query<SuperHero>(
            "SELECT * FROM $table WHERE id = :id",
            "id" to id,
        ).shouldBeEmpty()
    }
}
