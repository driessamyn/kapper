package net.samyn.kapper.benchmark.setup

import java.lang.AutoCloseable
import java.sql.Connection

enum class DatabaseType {
    SQLITE,
    POSTGRESQL,
}

fun createDatabaseConfig(
    type: DatabaseType,
    rows: Int,
): DatabaseConfig {
    return when (type) {
        DatabaseType.SQLITE -> SQLiteConfig(rows)
        DatabaseType.POSTGRESQL -> PostgreSQLConfig(rows)
    }
}

interface DatabaseConfig : AutoCloseable {
    val cachedConnection: Connection

    fun createConnection(): Connection
}
