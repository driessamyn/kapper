package net.samyn.kapper.internal

import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.samyn.kapper.Field
import org.junit.jupiter.api.Test
import java.sql.JDBCType
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.Types

class MetadataTest {
    @Test
    fun `when extractFields create Map`() {
        val mockMetadata =
            mockk<ResultSetMetaData> {
                every { columnCount } returns 2
                every { getColumnLabel(1) } returns "id"
                every { getColumnType(1) } returns Types.INTEGER
                every { getColumnTypeName(1) } returns JDBCType.valueOf(Types.INTEGER).name
                every { getColumnLabel(2) } returns "name"
                every { getColumnType(2) } returns Types.VARCHAR
                every { getColumnTypeName(2) } returns JDBCType.valueOf(Types.VARCHAR).name
            }
        val mockResultSet =
            mockk<ResultSet> {
                every { metaData } returns mockMetadata
            }

        val fields = mockResultSet.extractFields(DbFlavour.UNKNOWN)
        fields.shouldContainExactly(
            mapOf(
                "id" to Field(1, JDBCType.INTEGER, "INTEGER", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.VARCHAR, "VARCHAR", DbFlavour.UNKNOWN),
            ),
        )
    }

    @Test
    fun `when jdbcType returns JDBCType`() {
        val jdbcType = JDBCType.INTEGER.vendorTypeNumber
        val result = jdbcType.jdbcType()
        result shouldBe JDBCType.INTEGER
    }

    @Test
    fun `when jdbcType not known returns OTHER`() {
        val jdbcType = 999
        val result = jdbcType.jdbcType()
        result shouldBe JDBCType.OTHER
    }
}
