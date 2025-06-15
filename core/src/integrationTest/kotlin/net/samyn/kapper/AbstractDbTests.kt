package net.samyn.kapper

import net.samyn.kapper.internal.DbFlavour
import net.samyn.kapper.internal.getDbFlavour
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.Parameter
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MSSQLServerContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.oracle.OracleContainer
import java.sql.Connection
import java.sql.DriverManager
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ParameterizedClass
@MethodSource("databaseContainers")
abstract class AbstractDbTests {
    @Parameter
    protected lateinit var connection: Connection

    companion object {
        init {
            Class.forName("org.sqlite.JDBC")
        }

        private val postgresql by lazy {
            PostgreSQLContainer("postgres:16").also { it.start() }
        }

        private val mysql by lazy {
            MySQLContainer("mysql:8.4").also { it.start() }
        }

        private val oracle by lazy {
            OracleContainer("gvenzl/oracle-free:23-slim-faststart")
                // pfff, this container is sloooow
                .withStartupTimeout(Duration.ofMinutes(2))
                .also { it.start() }
        }

        private val msSqlServer by lazy {
            MSSQLServerContainer("mcr.microsoft.com/mssql/server:2017-CU12")
                .acceptLicense()
                .also {
                    // pfff, this container is noisy
                    val logger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.SQLServerConnection")
                    logger.level = Level.SEVERE
                    it.start()
                }
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
                DbFlavour.MSSQLSERVER to { getConnection(msSqlServer) },
                DbFlavour.ORACLE to { getConnection(oracle) },
            ).filter {
                // by default run against SQLite and PG only
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

    val testId = randomUpperCaseString(10) // pfff, Oracle has a limit of 30 chars for table names
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
                    age ${convertDbColumnType("INT", dbFlavour)}
                )
                """.trimIndent().also {
                    println("------------ $dbFlavour --------------")
                    println(it)
                },
            )
            statement.execute(
                """
                INSERT INTO super_heroes_$testId (id, name, email, age) VALUES
                    (${convertUUIDString(superman.id, dbFlavour)}, '${superman.name}', '${superman.email}', ${superman.age}),
                    (${convertUUIDString(batman.id, dbFlavour)}, '${batman.name}', '${batman.email}', ${batman.age}),
                    (${convertUUIDString(spiderMan.id, dbFlavour)}, '${spiderMan.name}', '${spiderMan.email}', ${spiderMan.age});
                """,
            )
        }
    }

    data class SuperHero(val id: UUID, val name: String, val email: String? = null, val age: Int? = null)

    class Villain {
        var id: String? = null
        var name: String? = null
    }
}

fun randomUpperCaseString(size: Int): String {
    val charPool = ('A'..'Z') + ('0'..'9')
    return (1..size)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}
