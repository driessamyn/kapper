package net.samyn.kapper.internal.automapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import net.samyn.kapper.DbFlavour
import net.samyn.kapper.Field
import net.samyn.kapper.KapperMappingException
import net.samyn.kapper.internal.AutoConverter
import net.samyn.kapper.internal.autoConverter
import org.junit.jupiter.api.Test
import java.sql.JDBCType
import java.sql.ResultSet
import java.util.UUID

class RecordMapperMessageTest {
    private val typesConverter = mockk<AutoConverter>(relaxed = true)
    private val fieldsConverter = mockk<FieldsConverter>(relaxed = true)
    private val resultSet = mockk<ResultSet>(relaxed = true)

    @Test
    fun `mapping failure names class, columns, components and mismatch`() {
        val fields =
            mapOf(
                "name" to Field(1, JDBCType.VARCHAR, "VARCHAR", DbFlavour.UNKNOWN),
                "email" to Field(2, JDBCType.VARCHAR, "VARCHAR", DbFlavour.UNKNOWN),
            )
        every { fieldsConverter.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("name", "Batman"),
                ColumnValue("email", "batman@dc.com"),
            )
        val mapper = RecordMapper(SuperHeroRecord::class.java, typesConverter, fieldsConverter)
        val ex =
            shouldThrow<KapperMappingException> {
                mapper.createInstance(resultSet, fields)
            }
        ex.message.shouldContain(SuperHeroRecord::class.java.name)
        ex.message.shouldContain("id")
        ex.message.shouldContain("age")
        ex.message.shouldContain("name (VARCHAR)")
        ex.message.shouldContain("email (VARCHAR)")
        ex.message.shouldContain("Expected record components")
        ex.message.shouldContain("register a custom mapper")
    }

    @Test
    fun `primitive null failure names the component and the columns returned`() {
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.OTHER, "UUID", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.VARCHAR, "VARCHAR", DbFlavour.UNKNOWN),
                "email" to Field(3, JDBCType.VARCHAR, "VARCHAR", DbFlavour.UNKNOWN),
                "age" to Field(4, JDBCType.INTEGER, "INTEGER", DbFlavour.UNKNOWN),
            )
        every { fieldsConverter.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("id", UUID.randomUUID()),
                ColumnValue("name", "Batman"),
                ColumnValue("email", "batman@dc.com"),
                ColumnValue("age", null),
            )
        val mapper = RecordMapper(SuperHeroRecord::class.java, typesConverter, fieldsConverter)
        val ex =
            shouldThrow<KapperMappingException> {
                mapper.createInstance(resultSet, fields)
            }
        ex.message.shouldContain("age")
        ex.message.shouldContain("primitive")
        ex.message.shouldContain("age (INTEGER)")
    }

    @Test
    fun `maps multiple rows with a single mapper instance`() {
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.OTHER, "UUID", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.VARCHAR, "VARCHAR", DbFlavour.UNKNOWN),
                "email" to Field(3, JDBCType.VARCHAR, "VARCHAR", DbFlavour.UNKNOWN),
                "age" to Field(4, JDBCType.INTEGER, "INTEGER", DbFlavour.UNKNOWN),
            )
        val id = UUID.randomUUID()
        every { fieldsConverter.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("id", id),
                ColumnValue("name", "Batman"),
                ColumnValue("email", "batman@dc.com"),
                ColumnValue("age", 85),
            )
        val mapper = RecordMapper(SuperHeroRecord::class.java, autoConverter, fieldsConverter)
        val expected = SuperHeroRecord(id, "Batman", "batman@dc.com", 85)
        mapper.createInstance(resultSet, fields) shouldBe expected
        mapper.createInstance(resultSet, fields) shouldBe expected
    }
}
