package net.samyn.kapper.internal

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.sql.Connection

class DbFlavourTest {
    private val connection = mockk<Connection>()

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "PostgreSQL",
                "Postgres",
                "EnterpriseDB",
            ],
    )
    fun `when databaseProductName is postgresql then getDbFlavour returns POSTGRESQL`(value: String) {
        every { connection.metaData.databaseProductName } returns value
        connection.getDbFlavour() shouldBe DbFlavour.POSTGRESQL
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "MySQL",
                "MySQL Server",
                "MySQL Community Server",
                "MySQL Enterprise Server",
            ],
    )
    fun `when databaseProductName is mysql then getDbFlavour returns MYSQL`(value: String) {
        every { connection.metaData.databaseProductName } returns value
        connection.getDbFlavour() shouldBe DbFlavour.MYSQL
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "SQLite",
            ],
    )
    fun `when databaseProductName is sqlite then getDbFlavour returns SQLITE`(value: String) {
        every { connection.metaData.databaseProductName } returns value
        connection.getDbFlavour() shouldBe DbFlavour.SQLITE
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                // Oracle variants
                "Oracle",
                "Oracle Database",
                "Oracle8i",
                "Oracle9i",
                "Oracle Database 10g",
                "Oracle Database 11g",
                "Oracle Database 12c",
                "Oracle Database 18c",
                "Oracle Database 19c",
                "Oracle Database 21c",
            ],
    )
    fun `when databaseProductName is oracle then getDbFlavour returns ORACLE`(value: String) {
        every { connection.metaData.databaseProductName } returns value
        connection.getDbFlavour() shouldBe DbFlavour.ORACLE
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                // Microsoft SQL Server variants
                "Microsoft SQL Server",
                "SQL Server",
                "MS SQL Server",
                "MSSQL",
            ],
    )
    fun `when databaseProductName is sql server then getDbFlavour returns MSSQLSERVER`(value: String) {
        every { connection.metaData.databaseProductName } returns value
        connection.getDbFlavour() shouldBe DbFlavour.MSSQLSERVER
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                // IBM DB2 variants
                "DB2",
                "DB2/NT",
                "DB2/SUN",
                "DB2/LINUX",
                "DB2/AIX",
                "DB2/HP",
                "DB2/2",
                "DB2/6000",
                "DB2 UDB for AS/400",
                "DB2/AS",
                "DB2 Universal Database",

                // Sybase variants
                "Sybase",
                "Adaptive Server Enterprise",
                "ASE",
                "SQL Anywhere",
                "Watcom SQL",

                // Informix variants
                "Informix",
                "Informix Dynamic Server",
                "IDS",

                // Other common databases
                "MariaDB",
                "H2",
                "HSQL Database Engine",
                "HSQLDB",
                "Apache Derby",
                "Firebird",
                "Interbase",
                "Progress",
                "Progress OpenEdge",
                "Ingres",
                "TimesTen",
                "Cache",
                "FrontBase",
                "MonetDB",
                "SAP DB",
                "MaxDB",
                "Mckoi SQL Database",
                "Cloudscape",
                "PointBase",
            ],
    )
    fun `when databaseProductName is unknown then getDbFlavour returns UNKNOWN`(value: String) {
        every { connection.metaData.databaseProductName } returns value
        connection.getDbFlavour() shouldBe DbFlavour.UNKNOWN
    }
}
