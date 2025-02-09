@file:JvmSynthetic

package net.samyn.kapper.internal

import java.sql.Connection

internal object DbConnectionUtils {
    fun Connection.getDbFlavour(): DbFlavour {
        return when (this.metaData.databaseProductName.lowercase()) {
            "postgresql" -> DbFlavour.POSTGRESQL
            "mysql" -> DbFlavour.MYSQL
            else -> DbFlavour.UNKNOWN
        }
    }

    enum class DbFlavour {
        POSTGRESQL,
        MYSQL,
        UNKNOWN,
    }
}
