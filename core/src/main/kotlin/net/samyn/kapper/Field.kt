package net.samyn.kapper

import java.sql.JDBCType

/**
 * Represents a field in a DB row.
 * @param columnIndex index of the column.
 * @param type JDBCType
 * @param name of the DB type as returned by the JDBC driver.
 */
data class Field(val columnIndex: Int, val type: JDBCType, val typeName: String)
