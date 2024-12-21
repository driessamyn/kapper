package net.samyn.kapper

import java.sql.JDBCType

/**
 * Represents a field in a DB row.
 * @param type JDBCType
 * @param name of the DB type as returned by the JDBC driver.
 */
data class Field(val type: JDBCType, val typeName: String)
