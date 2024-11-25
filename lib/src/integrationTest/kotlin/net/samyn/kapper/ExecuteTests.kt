package net.samyn.kapper

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MySQLContainer
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

class ExecuteTests : AbstractDbTests() {
    @OptIn(ExperimentalUuidApi::class)
    val table = "super_heros_${UUID.randomUUID().toKotlinUuid().toHexString()}"

    override fun setupDatabase(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE $table (
                        id ${if (container is MySQLContainer) "VARCHAR(36)" else "UUID"} PRIMARY KEY,
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
    }

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `SQL Insert single`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
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
    }

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `SQL Update single`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
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
    }

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `SQL Update multiple`(container: JdbcDatabaseContainer<*>) {
        val ids = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        val name = "foo-${UUID.randomUUID()}"
        createConnection(container).use { connection ->
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
    }

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `SQL Delete single`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
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
    }

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `SQL Delete multiple`(container: JdbcDatabaseContainer<*>) {
        val ids = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        val name = "bar-${UUID.randomUUID()}"
        createConnection(container).use { connection ->
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
    }
}
