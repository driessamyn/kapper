package net.samyn.kapper

import net.samyn.kapper.internal.DbFlavour
import java.util.UUID

private val specialTypes =
    mapOf(
        "UUID" to
            mapOf(
                DbFlavour.MYSQL to "VARCHAR(36)",
                DbFlavour.MSSQLSERVER to "UNIQUEIDENTIFIER",
                DbFlavour.ORACLE to "RAW(16)",
            ),
        "CLOB" to
            mapOf(
                DbFlavour.MYSQL to "TEXT",
                DbFlavour.POSTGRESQL to "TEXT",
                DbFlavour.MSSQLSERVER to "NVARCHAR(MAX)",
            ),
        "BINARY" to
            mapOf(
                DbFlavour.POSTGRESQL to "BYTEA",
                DbFlavour.ORACLE to "RAW(128)",
            ),
        "VARBINARY" to
            mapOf(
                DbFlavour.POSTGRESQL to "BYTEA",
                DbFlavour.ORACLE to "RAW(128)",
            ),
        "BLOB" to
            mapOf(
                DbFlavour.POSTGRESQL to "BYTEA",
                DbFlavour.MSSQLSERVER to "VARBINARY(MAX)",
            ),
        "FLOAT" to
            mapOf(
                DbFlavour.POSTGRESQL to "NUMERIC",
                DbFlavour.ORACLE to "FLOAT",
            ),
        "REAL" to
            mapOf(
                DbFlavour.MYSQL to "FLOAT",
                DbFlavour.ORACLE to "BINARY_FLOAT",
            ),
        "BOOLEAN" to
            mapOf(
                DbFlavour.MSSQLSERVER to "BIT",
                DbFlavour.ORACLE to "NUMBER(1)",
            ),
        "TIMESTAMP" to
            mapOf(
                DbFlavour.MSSQLSERVER to "DATETIME",
            ),
        "INT" to
            mapOf(
                DbFlavour.ORACLE to "NUMBER(10)",
            ),
        "BIGINT" to
            mapOf(
                DbFlavour.ORACLE to "NUMBER(19)",
            ),
        "DOUBLE PRECISION" to
            mapOf(
                DbFlavour.ORACLE to "BINARY_DOUBLE",
            ),
        "SMALLINT" to
            mapOf(
                DbFlavour.ORACLE to "NUMBER(5)",
            ),
        "TIME" to
            mapOf(
                DbFlavour.ORACLE to "TIMESTAMP",
            ),
        "VARCHAR" to
            mapOf(
                DbFlavour.ORACLE to "VARCHAR2(120)",
            ),
    )

fun convertDbColumnType(
    name: String,
    flavour: DbFlavour,
    suffix: String = "",
) = specialTypes[name]?.get(flavour) ?: (name + suffix)

fun convertUUIDString(
    id: UUID,
    flavour: DbFlavour,
) = when (flavour) {
    DbFlavour.ORACLE -> "HEXTORAW('${id.toString().replace("-", "")}')"
    else -> "'$id'"
}
