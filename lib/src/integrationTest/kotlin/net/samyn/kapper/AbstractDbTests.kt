package net.samyn.kapper

import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.jupiter.api.BeforeAll
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
import kotlin.use

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractDbTests {
    companion object {
        val specialTypes =
            mapOf(
                "UUID" to
                    mapOf(
                        MySQLContainer::class to "VARCHAR(36)",
                    ),
                "CLOB" to
                    mapOf(
                        MySQLContainer::class to "TEXT",
                        PostgreSQLContainer::class to "TEXT",
                    ),
                "BINARY" to
                    mapOf(
                        PostgreSQLContainer::class to "BYTEA",
                    ),
                "VARBINARY" to
                    mapOf(
                        PostgreSQLContainer::class to "BYTEA",
                    ),
                "BLOB" to
                    mapOf(
                        PostgreSQLContainer::class to "BYTEA",
                    ),
                "FLOAT" to
                    mapOf(
                        PostgreSQLContainer::class to "NUMERIC",
                    ),
                "REAL" to
                    mapOf(
                        MySQLContainer::class to "FLOAT",
                    ),
            )

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

    private fun convertDbColumnType(
        name: String,
        container: JdbcDatabaseContainer<*>,
        suffix: String = "",
    ) = specialTypes[name]?.get(container::class) ?: (name + suffix)

    val superman = SuperHero(UUID.randomUUID(), "Superman", "superman@dc.com", 86)
    val batman = SuperHero(UUID.randomUUID(), "Batman", "batman@dc.com", 85)
    val spiderMan = SuperHero(UUID.randomUUID(), "Spider-man", "spider@marvel.com", 62)

    @BeforeAll
    fun setup() {
        allContainers.values.forEach { container ->
            setupDatabase(container)
        }
    }

    protected open fun setupDatabase(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE super_heroes (
                        id ${convertDbColumnType("UUID", container)} PRIMARY KEY,
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
                statement.execute(
                    """
                    CREATE TABLE types_test (
                        t_uuid ${convertDbColumnType("UUID", container)},
                        t_char CHAR,
                        t_varchar VARCHAR(120),
                        t_clob ${convertDbColumnType("CLOB", container)},
                        t_binary ${convertDbColumnType("BINARY", container, "(16)")},
                        t_varbinary ${convertDbColumnType("VARBINARY", container, "(128)")},
                        t_large_binary ${convertDbColumnType("BLOB", container)},
                        t_numeric NUMERIC(12,6),
                        t_decimal DECIMAL(12,6),
                        t_smallint SMALLINT,
                        t_int INT,
                        t_bigint BIGINT,
                        t_float ${convertDbColumnType("FLOAT", container, "(8)")},
                        t_real ${convertDbColumnType("REAL", container)},
                        t_double DOUBLE PRECISION,
                        t_date DATE,
                        t_local_date DATE,
                        t_local_time TIME,
                        t_timestamp TIMESTAMP,
                        t_boolean BOOLEAN
                    )
                    """.trimIndent().also {
                        println(it)
                    },
                )
            }
        }
    }

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    @Order(1)
    fun `database should be running`(container: JdbcDatabaseContainer<*>) {
        container.isRunning.shouldBeTrue()
    }

    protected fun createConnection(container: JdbcDatabaseContainer<*>) =
        DriverManager.getConnection(container.jdbcUrl, container.username, container.password)

    data class SuperHero(val id: UUID, val name: String, val email: String? = null, val age: Int? = null)

    class Villain {
        var id: String? = null
        var name: String? = null
    }
}
