package net.samyn.kapper.benchmark.setup

import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager

class PostgreSQLConfig(rows: Int) : DatabaseConfig {
    private val postgreSQLContainer = PostgreSQLContainer("postgres:16")
    override val cachedConnection: Connection

    init {
        postgreSQLContainer.start()
        cachedConnection = createConnection()
        cachedConnection.createTables()
        cachedConnection.insertTestData(rows, rows)
    }

    override fun createConnection(): Connection =
        DriverManager.getConnection(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password,
        )

    override fun close() {
        postgreSQLContainer.stop()
    }
}
