package net.samyn.kapper.internal.automapper

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.samyn.kapper.DbFlavour
import net.samyn.kapper.Field
import org.junit.jupiter.api.Test
import java.sql.JDBCType
import java.sql.ResultSet

class SQLTypesNullConverterTest {
    val resultSet =
        mockk<ResultSet>(relaxed = true) {
            every { wasNull() } returns true
        }

    @Test
    fun `null bool returns null`() {
        val field = Field(99, JDBCType.BOOLEAN, "BOOLEAN", DbFlavour.UNKNOWN)
        sqlTypesConverter.convert(field, resultSet) shouldBe null
    }

    @Test
    fun `null int returns null`() {
        val field = Field(99, JDBCType.INTEGER, "INTEGER", DbFlavour.UNKNOWN)
        sqlTypesConverter.convert(field, resultSet) shouldBe null
    }

    @Test
    fun `null long returns null`() {
        val field = Field(99, JDBCType.BIGINT, "BIGINT", DbFlavour.UNKNOWN)
        sqlTypesConverter.convert(field, resultSet) shouldBe null
    }

    @Test
    fun `null float returns null`() {
        val field = Field(99, JDBCType.FLOAT, "FLOAT", DbFlavour.UNKNOWN)
        sqlTypesConverter.convert(field, resultSet) shouldBe null
    }

    @Test
    fun `null double returns null`() {
        val field = Field(99, JDBCType.DOUBLE, "DOUBLE", DbFlavour.UNKNOWN)
        sqlTypesConverter.convert(field, resultSet) shouldBe null
    }
}
