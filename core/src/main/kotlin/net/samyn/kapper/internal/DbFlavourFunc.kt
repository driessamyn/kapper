@file:JvmSynthetic

package net.samyn.kapper.internal

import net.samyn.kapper.DbFlavour
import java.sql.Connection

fun Connection.getDbFlavour(): DbFlavour {
    val productName = this.metaData.databaseProductName.lowercase()
    return if (productName.contains("postgres") || productName.contains("enterprisedb")) {
        DbFlavour.POSTGRESQL
    } else if (productName.contains("mysql")) {
        DbFlavour.MYSQL
    } else if (productName.contains("sqlite")) {
        DbFlavour.SQLITE
    } else if (productName.contains("oracle")) {
        DbFlavour.ORACLE
    } else if (productName.contains("sql server") || productName.contains("mssql")) {
        DbFlavour.MSSQLSERVER
    } else {
        DbFlavour.UNKNOWN
    }
}
