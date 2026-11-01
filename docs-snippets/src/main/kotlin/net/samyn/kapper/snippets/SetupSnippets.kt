package net.samyn.kapper.snippets

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import net.samyn.kapper.KapperMappingException
import net.samyn.kapper.query
import javax.sql.DataSource

// region: connection-setup
fun dataSource(url: String, user: String, secret: String): DataSource =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = url
            username = user
            password = secret
        },
    )
// endregion

// region: error-handling
fun safeQuery(dataSource: DataSource): List<User> =
    try {
        dataSource.connection.use {
            it.query<User>("SELECT id, name, email, status, created_at FROM users")
        }
    } catch (e: KapperMappingException) {
        // The message names the target class, the columns returned, and the mismatch.
        emptyList()
    }
// endregion

// region: testing
// kapper-test provides an in-memory harness so tests need no Docker.
fun testHarness(): DataSource =
    KapperTest.dataSource(
        ddl = "CREATE TABLE users(id UUID PRIMARY KEY, name VARCHAR(100) NOT NULL)",
    )
// endregion
