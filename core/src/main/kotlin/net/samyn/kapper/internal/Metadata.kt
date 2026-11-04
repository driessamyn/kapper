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

// Built once from the JDBCType enum, so lookups are O(1) instead of a linear scan per column per query.
private val jdbcTypesByVendorNumber: Map<Int, JDBCType> =
    JDBCType.entries.associateBy { it.vendorTypeNumber }

internal fun Int.jdbcType() =
    jdbcTypesByVendorNumber[this]
        ?: JDBCType.OTHER
