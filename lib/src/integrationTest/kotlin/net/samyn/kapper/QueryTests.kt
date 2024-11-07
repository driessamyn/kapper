package net.samyn.kapper

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.DriverManager
import java.util.UUID

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryTests {
    companion object {
        @Container
        val postgresql = PostgreSQLContainer("postgres:16")

        @Container
        val mysql = MySQLContainer("mysql:8.4")

        val allContainers =
            mapOf(
                "PostgreSQL" to postgresql,
                "MySQL" to mysql,
            )

        @JvmStatic
        fun databaseContainers() = allContainers.map { arguments(named(it.key, it.value)) }
    }

    val superman = SuperHero(UUID.randomUUID(), "Superman", "superman@dc.com", 86)
    val batman = SuperHero(UUID.randomUUID(), "Batman", "batman@dc.com", 85)
    val spiderMan = SuperHero(UUID.randomUUID(), "Spider-man", "spider@marvel.com", 62)

    @BeforeAll
    fun setup() {
        allContainers.values.forEach { container ->
            setupDatabase(container)
        }
    }

    private fun setupDatabase(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE super_heroes (
                        id ${if (container is MySQLContainer) "VARCHAR(36)" else "UUID"} PRIMARY KEY,
                        name VARCHAR(100),
                        email VARCHAR(100),
                        age INT
                    );
                    """.trimIndent().also {
                        println(it)
                    },
                )
                statement.execute(
                    """
                    INSERT INTO super_heroes (id, name, email, age) VALUES
                        ('${superman.id}', '${superman.name}', '${superman.email}', ${superman.age}),
                        ('${batman.id}', '${batman.name}', '${batman.email}', ${batman.age}),
                        ('${spiderMan.id}', '${spiderMan.name}', '${spiderMan.email}', ${spiderMan.age});
                    """.trimIndent().also {
                        println(it)
                    },
                )
            }
        }
    }

    private fun createConnection(container: JdbcDatabaseContainer<*>) =
        DriverManager.getConnection(container.jdbcUrl, container.username, container.password)

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    @Order(1)
    fun `database should be running`(container: JdbcDatabaseContainer<*>) {
        container.isRunning.shouldBeTrue()
    }

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    @Disabled("TODO")
    fun `should query all users`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val heroes = connection.query<SuperHero>("SELECT * FROM super_heroes")
            heroes.shouldContainExactlyInAnyOrder(
                superman,
                batman,
                spiderMan,
            )
        }
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    @Disabled("TODO")
    fun `should query users with condition`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val heroes = connection.query<SuperHero>("SELECT * FROM super_heroes WHERE age > :age", "age" to 80)
            heroes.shouldContainExactlyInAnyOrder(
                superman,
                batman,
            )
        }
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    @Disabled("TODO")
    fun `should query specific columns`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val heroes =
                connection.query<SuperHero>(
                    "SELECT id, name FROM super_heroes WHERE name = :name",
                    "name" to superman,
                )
            heroes.shouldContainExactlyInAnyOrder(
                SuperHero(superman.id, superman.name),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    @Disabled("TODO")
    fun `should handle empty result set`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val heroes =
                connection.query<SuperHero>(
                    "SELECT * FROM super_heroes WHERE name = :name",
                    "name" to "joker",
                )
            heroes.shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    @Disabled("TODO")
    fun `should query with multiple conditions`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val heroes =
                connection.query<SuperHero>(
                    "SELECT * FROM super_heroes WHERE age BETWEEN :fromAge AND :toAge",
                    "fromAge" to 80,
                    "toAge" to 89,
                )
            heroes.shouldContainExactlyInAnyOrder(
                superman,
                batman,
            )
        }
    }

    data class SuperHero(val id: UUID, val name: String, val email: String? = null, val age: Int? = null)
}
