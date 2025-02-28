@file:JvmSynthetic

package net.samyn.kapper.internal

import net.samyn.kapper.Field
import java.sql.JDBCType
import java.sql.ResultSet

fun ResultSet.extractFields(): Map<String, Field> =
    (1..this.metaData.columnCount).associate {
        this.metaData.getColumnLabel(it) to
            Field(
                it,
                JDBCType.valueOf(this.metaData.getColumnType(it)),
                this.metaData.getColumnTypeName(it),
            )
    }
