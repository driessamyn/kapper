package net.samyn.kapper.internal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.samyn.kapper.Field
import net.samyn.kapper.KapperMappingException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.sql.JDBCType
import java.sql.ResultSet
import java.util.UUID
import kotlin.reflect.KClass

class MapperTest {
    private val autoMapperMock = mockk<(Any, KClass<*>) -> Any>(relaxed = true)
    private val resultSet = mockk<ResultSet>(relaxed = true)
    val sqlTypeConverterMock = mockk<(JDBCType, String, ResultSet, Int) -> Any?>(relaxed = true)

    @ParameterizedTest
    @ValueSource(strings = ["email", "EMAIL", "eMail"])
    fun `should map to super hero case insensitive`(emailParam: String) {
        val batman = SuperHero(UUID.randomUUID(), "Batman", "batman@dc.com", 85)
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType"),
                "name" to Field(2, JDBCType.BIT, "SomeType"),
                emailParam to Field(3, JDBCType.BIT, "SomeType"),
                "age" to Field(4, JDBCType.BIT, "SomeType"),
            )
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(1)) } returns
            batman.id
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(2)) } returns
            batman.name
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(3)) } returns
            batman.email!!
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(4)) } returns
            batman.age!!

        val mapper = Mapper(SuperHero::class.java, autoMapperMock, sqlTypeConverterMock)
        val instance = mapper.createInstance(resultSet, fields)

        instance.shouldBe(batman)
    }

    @Test
    fun `should map to super hero respecting defaults`() {
        val batman = SuperHero(UUID.randomUUID(), "Batman", "batman@dc.com", 85)
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType"),
                "name" to Field(2, JDBCType.BIT, "SomeType"),
            )
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(1)) } returns
            batman.id
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(2)) } returns
            batman.name

        val mapper = Mapper(SuperHero::class.java, autoMapperMock, sqlTypeConverterMock)
        val instance = mapper.createInstance(resultSet, fields)

        instance.shouldBe(SuperHero(batman.id, "Batman", null, null))
    }

    @Test
    fun `should throw when too many columns`() {
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType"),
                "name" to Field(2, JDBCType.BIT, "SomeType"),
                "email" to Field(3, JDBCType.BIT, "SomeType"),
                "age" to Field(4, JDBCType.BIT, "SomeType"),
                "foo" to Field(5, JDBCType.BIT, "SomeType"),
            )
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(1)) } returns
            UUID.randomUUID()
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(2)) } returns
            "joker"
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(3)) } returns
            "joker@dc.com"
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(4)) } returns
            85
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(5)) } returns
            "bar"

        val mapper = Mapper(SuperHero::class.java, autoMapperMock, sqlTypeConverterMock)
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
                "name" to Field(1, JDBCType.BIT, "SomeType"),
                "email" to Field(2, JDBCType.BIT, "SomeType"),
                "age" to Field(3, JDBCType.BIT, "SomeType"),
            )
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(1)) } returns
            batman.name
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(2)) } returns
            batman.email!!
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(3)) } returns
            batman.age!!

        val mapper = Mapper(SuperHero::class.java, autoMapperMock, sqlTypeConverterMock)
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
                "id" to Field(1, JDBCType.BIT, "SomeType"),
                "name" to Field(2, JDBCType.BIT, "SomeType"),
            )
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(1)) } returns
            UUID.randomUUID()
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(2)) } returns
            123

        val mapper = Mapper(SuperHero::class.java, autoMapperMock, sqlTypeConverterMock)
        mapper.createInstance(resultSet, fields)
        verify { autoMapperMock(123, String::class) }
    }

    @Test
    fun `should set to null when value null`() {
        val batman = SuperHero(UUID.randomUUID(), "Batman", "batman@dc.com", 85)
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType"),
                "name" to Field(2, JDBCType.BIT, "SomeType"),
                "email" to Field(3, JDBCType.BIT, "SomeType"),
            )
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(1)) } returns
            batman.id
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(2)) } returns
            batman.name
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(3)) } returns
            null

        val mapper = Mapper(SuperHero::class.java, autoMapperMock, sqlTypeConverterMock)
        val instance = mapper.createInstance(resultSet, fields)

        instance.shouldBe(SuperHero(batman.id, "Batman", null, null))
    }

    abstract class NoPrimaryConstructor {
        constructor(id: UUID)
        constructor(name: String)
    }

    @Test
    fun `should throw when no primary constructor`() {
        shouldThrow<KapperMappingException> {
            Mapper(NoPrimaryConstructor::class.java, autoMapperMock, sqlTypeConverterMock)
        }
    }

    @Test
    fun `should throw when no property found`() {
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType"),
                "name" to Field(2, JDBCType.BIT, "SomeType"),
                "foo" to Field(1, JDBCType.BIT, "SomeType"),
            )
        every { sqlTypeConverterMock(any<JDBCType>(), any<String>(), any<ResultSet>(), eq<Int>(1)) } returns
            UUID.randomUUID()

        val mapper = Mapper(SuperHero::class.java, autoMapperMock, sqlTypeConverterMock)
        val ex =
            shouldThrow<KapperMappingException> {
                mapper.createInstance(resultSet, fields)
            }
        ex.message.shouldContain("foo")
    }

    data class SuperHero(val id: UUID, val name: String, val email: String? = null, val age: Int? = null)
}
