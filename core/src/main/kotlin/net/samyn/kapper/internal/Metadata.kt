@file:JvmSynthetic

package net.samyn.kapper.internal

import net.samyn.kapper.DbFlavour
import net.samyn.kapper.Field
import java.sql.JDBCType
import java.sql.ResultSet

fun ResultSet.extractFields(dbFlavour: DbFlavour): Map<String, Field> =
    (1..this.metaData.columnCount).associate {
        this.metaData.getColumnLabel(it) to
            Field(
                it,
                this.metaData.getColumnType(it).jdbcType(),
                this.metaData.getColumnTypeName(it),
                dbFlavour,
            )
    }

internal fun Int.jdbcType() =
    JDBCType.entries.firstOrNull { it.vendorTypeNumber == this }
        ?: JDBCType.OTHER
