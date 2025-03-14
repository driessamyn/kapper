package net.samyn.kapper.coroutines

import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer

fun createDataSource(postgresql: PostgreSQLContainer<*>) =
    PGSimpleDataSource().also {
        println("JDBC URL: ${postgresql.jdbcUrl}")
        it.setUrl(postgresql.jdbcUrl)
        it.user = postgresql.username
        it.password = postgresql.password
    }
