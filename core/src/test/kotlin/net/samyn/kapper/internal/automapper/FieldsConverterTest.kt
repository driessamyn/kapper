package net.samyn.kapper.internal.automapper

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.samyn.kapper.DbFlavour
import net.samyn.kapper.Field
import org.junit.jupiter.api.Test
import java.sql.JDBCType
import java.sql.ResultSet

class FieldsConverterTest {
    private val sqlTypesConverter = mockk<SQLTypesConverter>()
    private val resultSet = mockk<ResultSet>(relaxed = true)
    private val fieldsConverter = FieldsConverter(sqlTypesConverter)

    @Test
    fun `should convert all fields to column values`() {
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.INTEGER, "Int", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.VARCHAR, "String", DbFlavour.UNKNOWN),
            )
        every { sqlTypesConverter.convert(fields["id"]!!, resultSet) } returns 42
        every { sqlTypesConverter.convert(fields["name"]!!, resultSet) } returns "Bruce"

        val result = fieldsConverter.convert(resultSet, fields)
        result shouldBe
            listOf(
                ColumnValue("id", 42),
                ColumnValue("name", "Bruce"),
            )
    }

    @Test
    fun `should handle empty fields map`() {
        val result = fieldsConverter.convert(resultSet, emptyMap())
        result shouldBe emptyList()
    }

    @Test
    fun `should allow null values from converter`() {
        val fields =
            mapOf(
                "email" to Field(1, JDBCType.VARCHAR, "String", DbFlavour.UNKNOWN),
            )
        every { sqlTypesConverter.convert(fields["email"]!!, resultSet) } returns null
        val result = fieldsConverter.convert(resultSet, fields)
        result shouldBe listOf(ColumnValue("email", null))
    }
}
