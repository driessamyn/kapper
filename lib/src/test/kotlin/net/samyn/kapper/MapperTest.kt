package net.samyn.kapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.samyn.kapper.internal.Mapper
import org.junit.jupiter.api.Test
import java.sql.JDBCType
import java.sql.ResultSet
import java.util.UUID
import kotlin.reflect.KClass

class MapperTest {
    private val autoMapperMock = mockk<(Any, KClass<*>) -> Any>()
    private val resultSet = mockk<ResultSet>()
    val sqlTypeConverterMock = mockk<(JDBCType, String, ResultSet, String) -> Any>()

    @Test
    fun `should map to super hero`() {
        val batman = SuperHero(UUID.randomUUID(), "Batman", "batman@dc.com", 85)
        val fields =
            mapOf(
                "id" to Field(JDBCType.BIT, "SomeType"),
                "name" to Field(JDBCType.BIT, "SomeType"),
                "email" to Field(JDBCType.BIT, "SomeType"),
                "age" to Field(JDBCType.BIT, "SomeType"),
            )
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<String>("id")) } returns
            batman.id
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<String>("name")) } returns
            batman.name
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<String>("email")) } returns
            batman.email!!
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<String>("age")) } returns
            batman.age!!

        val mapper = Mapper<SuperHero>(SuperHero::class.java, autoMapperMock, sqlTypeConverterMock)
        val instance = mapper.createInstance(resultSet, fields)

        instance.shouldBe(batman)
    }

    @Test
    fun `should map to super hero respecting defaults`() {
        val batman = SuperHero(UUID.randomUUID(), "Batman", "batman@dc.com", 85)
        val fields =
            mapOf(
                "id" to Field(JDBCType.BIT, "SomeType"),
                "name" to Field(JDBCType.BIT, "SomeType"),
            )
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<String>("id")) } returns
            batman.id
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<String>("name")) } returns
            batman.name

        val mapper = Mapper<SuperHero>(SuperHero::class.java, autoMapperMock, sqlTypeConverterMock)
        val instance = mapper.createInstance(resultSet, fields)

        instance.shouldBe(SuperHero(batman.id, "Batman", null, null))
    }

    @Test
    fun `should throw when too many columns`() {
        val fields =
            mapOf(
                "id" to Field(JDBCType.BIT, "SomeType"),
                "name" to Field(JDBCType.BIT, "SomeType"),
                "email" to Field(JDBCType.BIT, "SomeType"),
                "age" to Field(JDBCType.BIT, "SomeType"),
                "foo" to Field(JDBCType.BIT, "SomeType"),
            )
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<String>("id")) } returns
            UUID.randomUUID()
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<String>("name")) } returns
            "joker"
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<String>("email")) } returns
            "joker@dc.com"
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<String>("age")) } returns
            85
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<String>("foo")) } returns
            "bar"

        val mapper = Mapper<SuperHero>(SuperHero::class.java, autoMapperMock, sqlTypeConverterMock)
        val ex =
            shouldThrow<KapperMappingException> {
                mapper.createInstance(resultSet, fields)
            }
        ex.message.shouldContain("foo")
    }

    @Test
    fun `should throw when non-optional are missing`() {
        val batman = SuperHero(UUID.randomUUID(), "Batman", "batman@dc.com", 85)
        val fields =
            mapOf(
                "name" to Field(JDBCType.BIT, "SomeType"),
                "email" to Field(JDBCType.BIT, "SomeType"),
                "age" to Field(JDBCType.BIT, "SomeType"),
            )
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<String>("name")) } returns
            batman.name
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<String>("email")) } returns
            batman.email!!
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<String>("age")) } returns
            batman.age!!

        val mapper = Mapper<SuperHero>(SuperHero::class.java, autoMapperMock, sqlTypeConverterMock)
        val ex =
            shouldThrow<KapperMappingException> {
                mapper.createInstance(resultSet, fields)
            }
        ex.message.shouldContain("id")
    }

    @Test
    fun `should convert when type not known`() {
        every { autoMapperMock(any<Int>(), any<KClass<*>>()) } returns "Foo"
        val fields =
            mapOf(
                "id" to Field(JDBCType.BIT, "SomeType"),
                "name" to Field(JDBCType.BIT, "SomeType"),
            )
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<String>("id")) } returns
            UUID.randomUUID()
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<String>("name")) } returns
            123

        val mapper = Mapper<SuperHero>(SuperHero::class.java, autoMapperMock, sqlTypeConverterMock)
        mapper.createInstance(resultSet, fields)
        verify { autoMapperMock(123, String::class) }
    }

    data class SuperHero(val id: UUID, val name: String, val email: String? = null, val age: Int? = null)
}
