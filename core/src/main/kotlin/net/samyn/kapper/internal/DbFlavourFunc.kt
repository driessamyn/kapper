@file:JvmSynthetic

package net.samyn.kapper.internal

import net.samyn.kapper.DbFlavour
import java.sql.Connection

fun Connection.getDbFlavour(): DbFlavour {
    val productName = this.metaData.databaseProductName
    return when {
        productName.contains("postgres", ignoreCase = true) ||
            productName.contains("enterprisedb", ignoreCase = true) -> DbFlavour.POSTGRESQL
        productName.contains("mysql", ignoreCase = true) -> DbFlavour.MYSQL
        productName.contains("sqlite", ignoreCase = true) -> DbFlavour.SQLITE
        productName.contains("oracle", ignoreCase = true) -> DbFlavour.ORACLE
        productName.contains("sql server", ignoreCase = true) ||
            productName.contains("mssql", ignoreCase = true) -> DbFlavour.MSSQLSERVER
        else -> DbFlavour.UNKNOWN
    }
}
