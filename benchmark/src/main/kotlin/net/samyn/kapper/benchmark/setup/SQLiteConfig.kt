package net.samyn.kapper.benchmark.setup

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager

class SQLiteConfig(private val rows: Int) : DatabaseConfig {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override val cachedConnection: Connection
        get() = singletonConnection

    private val singletonConnection: Connection

    init {
        logger.info("Creating SQLite DB")
        Class.forName("org.sqlite.JDBC")
        // Use an in-memory SQLite database, so continue to re-use the same connection
        singletonConnection = DriverManager.getConnection("jdbc:sqlite::memory:")
    }

    init {
        logger.info("Creating test data")
        createConnection().use {
            it.createTables()
            it.insertTestData(rows, rows)
        }
    }

    override fun createConnection() =
        object : Connection by singletonConnection {
            override fun close() {
                // Do nothing, as we want to re-use the same connection until the test is complete
            }
        }

    override fun close() {
        logger.info("Closing SQLite DB")
        singletonConnection.close()
    }
}
