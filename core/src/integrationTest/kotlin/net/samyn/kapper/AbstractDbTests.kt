package net.samyn.kapper

import net.samyn.kapper.internal.DbFlavour
import net.samyn.kapper.internal.getDbFlavour
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MSSQLServerContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.use
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractDbTests {
    companion object {
        init {
            Class.forName("org.sqlite.JDBC")
        }

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

        private val postgresql by lazy {
            PostgreSQLContainer("postgres:16").also { it.start() }
        }

        private val mysql by lazy {
            MySQLContainer("mysql:8.4").also { it.start() }
        }

//        @Container
//        val oracle = OracleContainer("gvenzl/oracle-free:23.4-slim-faststart")

        private val msSqlServer by lazy {
            MSSQLServerContainer("mcr.microsoft.com/mssql/server:2017-CU12")
                .acceptLicense().also { it.start() }
        }

        private val connections = ConcurrentHashMap<DbFlavour, Connection>()

        private fun getConnection(container: JdbcDatabaseContainer<*>): Connection {
            Class.forName(container.driverClassName)
            return DriverManager.getConnection(container.jdbcUrl, container.username, container.password)
        }

        val dbs =
            mapOf(
                DbFlavour.POSTGRESQL to { getConnection(postgresql) },
                DbFlavour.MYSQL to { getConnection(mysql) },
                DbFlavour.SQLITE to { DriverManager.getConnection("jdbc:sqlite::memory:") },
//                "Oracle" to oracle,
//                DbFlavour.MSSQLSERVER to { getConnection(msSqlServer) },
            ).filter {
                // by default run against SQLite only
                //  this allows parallel runs for different int tests.
                when (System.getProperty("db", "").uppercase()) {
                    "" -> it.key == DbFlavour.SQLITE || it.key == DbFlavour.POSTGRESQL
                    "ALL" -> true
                    else -> it.key == DbFlavour.valueOf(System.getProperty("db").uppercase())
                }
            }

        @JvmStatic
        fun databaseContainers(): List<Arguments> {
            println("--------------------------------")
            println("Running tests against:")
            val connections =
                connections
                    .map {
                        println("   ${it.key}")
                        arguments(named(it.key.toString(), it.value))
                    }
            println("--------------------------------")
            return connections
        }
    }

    private fun convertDbColumnType(
        name: String,
        flavour: DbFlavour,
        suffix: String = "",
    ) = specialTypes[name]?.get(flavour) ?: (name + suffix)

    @OptIn(ExperimentalUuidApi::class)
    val testId = UUID.randomUUID().toKotlinUuid().toHexString()
    val superman = SuperHero(UUID.randomUUID(), "Superman", "superman@dc.com", 86)
    val batman = SuperHero(UUID.randomUUID(), "Batman", "batman@dc.com", 85)
    val spiderMan = SuperHero(UUID.randomUUID(), "Spider-man", "spider@marvel.com", 62)

    @BeforeAll
    fun setup() {
        dbs.forEach { container ->
            setupDatabase(connections.computeIfAbsent(container.key) { container.value() })
        }
    }

    @AfterAll
    fun tearDown() {
        connections.forEach { (_, connection) ->
            connection.close()
        }
        connections.clear()
    }

    protected open fun setupDatabase(connection: Connection) {
        val dbFlavour = connection.getDbFlavour()
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE super_heroes_$testId (
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
                INSERT INTO super_heroes_$testId (id, name, email, age) VALUES
                    ('${superman.id}', '${superman.name}', '${superman.email}', ${superman.age}),
                    ('${batman.id}', '${batman.name}', '${batman.email}', ${batman.age}),
                    ('${spiderMan.id}', '${spiderMan.name}', '${spiderMan.email}', ${spiderMan.age});
                """.trimIndent().also {
                    println(it)
                },
            )
            statement.execute(
                """
                CREATE TABLE types_test_$testId (
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
