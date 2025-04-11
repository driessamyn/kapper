@file:JvmSynthetic

package net.samyn.kapper.internal

import java.sql.Connection

fun Connection.getDbFlavour(): DbFlavour {
    val productName = this.metaData.databaseProductName.lowercase()
    return if (productName.contains("postgres") || productName.contains("enterprisedb")) {
        DbFlavour.POSTGRESQL
    } else if (productName.contains("mysql")) {
        DbFlavour.MYSQL
    } else if (productName.contains("sqlite")) {
        DbFlavour.SQLITE
    } else {
        DbFlavour.UNKNOWN
    }
}

enum class DbFlavour {
    POSTGRESQL,
    MYSQL,
    SQLITE,
    UNKNOWN,
}
