package net.samyn.kapper

import java.sql.JDBCType

/**
 * Represents a field in a DB row.
 * @param columnIndex index of the column.
 * @param type JDBCType
 * @param typeName of the DB type as returned by the JDBC driver.
 * @param dbFlavour the database flavour.
 */
data class Field(val columnIndex: Int, val type: JDBCType, val typeName: String, val dbFlavour: DbFlavour)
