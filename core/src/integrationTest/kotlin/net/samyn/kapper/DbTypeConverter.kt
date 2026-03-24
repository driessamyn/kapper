package net.samyn.kapper

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
                DbFlavour.DUCKDB to "VARCHAR",
                DbFlavour.CLICKHOUSE to "String",
            ),
        "BINARY" to
            mapOf(
                DbFlavour.POSTGRESQL to "BYTEA",
                DbFlavour.ORACLE to "RAW(128)",
                DbFlavour.DUCKDB to "BLOB",
                DbFlavour.CLICKHOUSE to "String",
            ),
        "VARBINARY" to
            mapOf(
                DbFlavour.POSTGRESQL to "BYTEA",
                DbFlavour.ORACLE to "RAW(128)",
                DbFlavour.DUCKDB to "BLOB",
                DbFlavour.CLICKHOUSE to "String",
            ),
        "BLOB" to
            mapOf(
                DbFlavour.POSTGRESQL to "BYTEA",
                DbFlavour.MSSQLSERVER to "VARBINARY(MAX)",
                DbFlavour.CLICKHOUSE to "String",
            ),
        "FLOAT" to
            mapOf(
                DbFlavour.POSTGRESQL to "NUMERIC",
                DbFlavour.ORACLE to "FLOAT",
                DbFlavour.CLICKHOUSE to "Float32",
            ),
        "REAL" to
            mapOf(
                DbFlavour.MYSQL to "FLOAT",
                DbFlavour.ORACLE to "BINARY_FLOAT",
                DbFlavour.CLICKHOUSE to "Float32",
            ),
        "BOOLEAN" to
            mapOf(
                DbFlavour.MSSQLSERVER to "BIT",
                DbFlavour.ORACLE to "NUMBER(1)",
                DbFlavour.CLICKHOUSE to "Bool",
            ),
        "TIMESTAMP" to
            mapOf(
                DbFlavour.MSSQLSERVER to "DATETIME",
                DbFlavour.CLICKHOUSE to "DateTime",
            ),
        "INT" to
            mapOf(
                DbFlavour.ORACLE to "NUMBER(10)",
                DbFlavour.CLICKHOUSE to "Int32",
            ),
        "BIGINT" to
            mapOf(
                DbFlavour.ORACLE to "NUMBER(19)",
                DbFlavour.CLICKHOUSE to "Int64",
            ),
        "DOUBLE PRECISION" to
            mapOf(
                DbFlavour.ORACLE to "BINARY_DOUBLE",
                DbFlavour.CLICKHOUSE to "Float64",
            ),
        "SMALLINT" to
            mapOf(
                DbFlavour.ORACLE to "NUMBER(5)",
                DbFlavour.CLICKHOUSE to "Int16",
            ),
        "TIME" to
            mapOf(
                DbFlavour.ORACLE to "TIMESTAMP",
                DbFlavour.CLICKHOUSE to "String",
            ),
        "VARCHAR" to
            mapOf(
                DbFlavour.ORACLE to "VARCHAR2(120)",
                DbFlavour.CLICKHOUSE to "String",
            ),
        "CHAR" to
            mapOf(
                DbFlavour.CLICKHOUSE to "String",
            ),
        "NUMERIC" to
            mapOf(
                DbFlavour.CLICKHOUSE to "Decimal(12,6)",
            ),
        "DECIMAL" to
            mapOf(
                DbFlavour.CLICKHOUSE to "Decimal(12,6)",
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
