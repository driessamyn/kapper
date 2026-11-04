@file:JvmSynthetic

package net.samyn.kapper.internal

import net.samyn.kapper.DbFlavour
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap

// Cache the product-name -> flavour mapping, keyed on the product-name string.
// Never key this on the Connection: connection pooling and proxy wrappers make per-Connection
// caching unsafe (a pooled Connection can be handed out for a different database).
private val flavourCache = ConcurrentHashMap<String, DbFlavour>()

fun Connection.getDbFlavour(): DbFlavour {
    val productName = this.metaData.databaseProductName
    return flavourCache.computeIfAbsent(productName) { resolveDbFlavour(it) }
}

internal fun resolveDbFlavour(productName: String): DbFlavour =
    when {
        productName.contains("cockroach", ignoreCase = true) -> DbFlavour.POSTGRESQL
        productName.contains("postgres", ignoreCase = true) ||
            productName.contains("enterprisedb", ignoreCase = true) -> DbFlavour.POSTGRESQL
        productName.contains("mysql", ignoreCase = true) ||
            productName.contains("mariadb", ignoreCase = true) ||
            productName.contains("tidb", ignoreCase = true) ||
            productName.contains("starrocks", ignoreCase = true) -> DbFlavour.MYSQL
        productName.contains("sqlite", ignoreCase = true) -> DbFlavour.SQLITE
        productName.contains("oracle", ignoreCase = true) -> DbFlavour.ORACLE
        productName.contains("sql server", ignoreCase = true) ||
            productName.contains("mssql", ignoreCase = true) -> DbFlavour.MSSQLSERVER
        productName.contains("duckdb", ignoreCase = true) -> DbFlavour.DUCKDB
        productName.contains("firebird", ignoreCase = true) -> DbFlavour.FIREBIRD
        else -> DbFlavour.UNKNOWN
    }
