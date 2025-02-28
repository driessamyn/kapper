package net.samyn.kapper.coroutines

import net.samyn.kapper.query
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer

fun createDataSource(postgresql: PostgreSQLContainer<*>) =
    PGSimpleDataSource().also {
        println("JDBC URL: ${postgresql.jdbcUrl}")
        it.setUrl(postgresql.jdbcUrl)
        it.user = postgresql.username
        it.password = postgresql.password
    }.also {
        it.connection.query<String>(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
            { rs, _ -> rs.getString("table_name") },
        ).forEach(::println)
    }
