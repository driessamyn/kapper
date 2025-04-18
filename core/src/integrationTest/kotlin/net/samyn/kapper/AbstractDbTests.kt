package net.samyn.kapper

import net.samyn.kapper.internal.DbFlavour
import net.samyn.kapper.internal.getDbFlavour
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.provider.Arguments.arguments
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.use

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractDbTests {
    companion object {
        val specialTypes =
            mapOf(
                "UUID" to
                    mapOf(
                        DbFlavour.MYSQL to "VARCHAR(36)",
                    ),
                "CLOB" to
                    mapOf(
                        DbFlavour.MYSQL to "TEXT",
                        DbFlavour.POSTGRESQL to "TEXT",
                    ),
                "BINARY" to
                    mapOf(
                        DbFlavour.POSTGRESQL to "BYTEA",
                    ),
                "VARBINARY" to
                    mapOf(
                        DbFlavour.POSTGRESQL to "BYTEA",
                    ),
                "BLOB" to
                    mapOf(
                        DbFlavour.POSTGRESQL to "BYTEA",
                    ),
                "FLOAT" to
                    mapOf(
                        DbFlavour.POSTGRESQL to "NUMERIC",
                    ),
                "REAL" to
                    mapOf(
                        DbFlavour.MYSQL to "FLOAT",
                    ),
            )

        @Container
        val postgresql = PostgreSQLContainer("postgres:16")

        @Container
        val mysql = MySQLContainer("mysql:8.4")

//        @Container
//        val oracle = OracleContainer("gvenzl/oracle-free:23.4-slim-faststart")
//
//        @Container
//        val msSqlServer =
//            MSSQLServerContainer("mcr.microsoft.com/mssql/server:2017-CU12").acceptLicense()

        val allContainers =
            mapOf(
                "PostgreSQL" to postgresql,
                "MySQL" to mysql,
//                "Oracle" to oracle,
//                "MSSQLServer" to msSqlServer,
            )

        @JvmStatic
        fun databaseContainers() =
            allContainers.map {
                arguments(named(it.key, getConnection(it.value)))
            }

        private val connections = ConcurrentHashMap<Class<*>, Connection>()

        private fun getConnection(container: JdbcDatabaseContainer<*>) =
            connections.computeIfAbsent(container.javaClass) {
                Class.forName(container.driverClassName)
                DriverManager.getConnection(container.jdbcUrl, container.username, container.password)
            }
    }

    private fun convertDbColumnType(
        name: String,
        flavour: DbFlavour,
        suffix: String = "",
    ) = specialTypes[name]?.get(flavour) ?: (name + suffix)

    val superman = SuperHero(UUID.randomUUID(), "Superman", "superman@dc.com", 86)
    val batman = SuperHero(UUID.randomUUID(), "Batman", "batman@dc.com", 85)
    val spiderMan = SuperHero(UUID.randomUUID(), "Spider-man", "spider@marvel.com", 62)

    @BeforeAll
    fun setup() {
        allContainers.values.forEach { container ->
            setupDatabase(getConnection(container))
        }
    }

    @AfterAll
    fun tearDown() {
        connections.forEach { (_, connection) ->
            connection.close()
        }
        connections.clear()
        allContainers.values.forEach { container ->
            container.stop()
        }
    }

    protected open fun setupDatabase(connection: Connection) {
        val dbFlavour = connection.getDbFlavour()
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE super_heroes (
                    id ${convertDbColumnType("UUID", dbFlavour)} PRIMARY KEY,
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
                    t_uuid ${convertDbColumnType("UUID", dbFlavour)},
                    t_char CHAR,
                    t_varchar VARCHAR(120),
                    t_clob ${convertDbColumnType("CLOB", dbFlavour)},
                    t_binary ${convertDbColumnType("BINARY", dbFlavour, "(16)")},
                    t_varbinary ${convertDbColumnType("VARBINARY", dbFlavour, "(128)")},
                    t_large_binary ${convertDbColumnType("BLOB", dbFlavour)},
                    t_numeric NUMERIC(12,6),
                    t_decimal DECIMAL(12,6),
                    t_smallint SMALLINT,
                    t_int INT,
                    t_bigint BIGINT,
                    t_float ${convertDbColumnType("FLOAT", dbFlavour, "(8)")},
                    t_real ${convertDbColumnType("REAL", dbFlavour)},
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

    data class SuperHero(val id: UUID, val name: String, val email: String? = null, val age: Int? = null)

    class Villain {
        var id: String? = null
        var name: String? = null
    }
}
