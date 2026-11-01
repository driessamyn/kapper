package net.samyn.kapper.internal.automapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.samyn.kapper.DbFlavour
import net.samyn.kapper.Field
import net.samyn.kapper.KapperMappingException
import net.samyn.kapper.internal.AutoConverter
import net.samyn.kapper.internal.autoConverter
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.sql.JDBCType
import java.sql.ResultSet
import java.util.UUID

class KotlinDataClassMapperTest {
    private val autoTypesConverterMock = mockk<AutoConverter>(relaxed = true)
    private val resultSet = mockk<ResultSet>(relaxed = true)
    private val fieldsConverterMock = mockk<FieldsConverter>(relaxed = true)

    @ParameterizedTest
    @ValueSource(strings = ["email", "EMAIL", "eMail", "e-mail", "e_mail"])
    fun `should map to super hero case insensitive`(emailParam: String) {
        val batman = SuperHero(UUID.randomUUID(), "Batman", "batman@dc.com", 85)
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                emailParam to Field(3, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "age" to Field(4, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
            )
        every { fieldsConverterMock.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("id", batman.id),
                ColumnValue("name", batman.name),
                ColumnValue(emailParam, batman.email),
                ColumnValue("age", batman.age),
            )
        val kotlinDataClassMapper = KotlinDataClassMapper(SuperHero::class.java, autoTypesConverterMock, fieldsConverterMock)
        val instance = kotlinDataClassMapper.createInstance(resultSet, fields)
        instance.shouldBe(batman)
    }

    @Test
    fun `should map to super hero respecting defaults`() {
        val batman = SuperHero(UUID.randomUUID(), "Batman", "batman@dc.com", 85)
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
            )
        every { fieldsConverterMock.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("id", batman.id),
                ColumnValue("name", batman.name),
            )
        val kotlinDataClassMapper = KotlinDataClassMapper(SuperHero::class.java, autoTypesConverterMock, fieldsConverterMock)
        val instance = kotlinDataClassMapper.createInstance(resultSet, fields)
        instance.shouldBe(SuperHero(batman.id, "Batman", null, null))
    }

    @Test
    fun `should throw when non-optional are missing`() {
        val fields =
            mapOf(
                "name" to Field(1, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "email" to Field(2, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "age" to Field(3, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
            )
        every { fieldsConverterMock.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("name", "Batman"),
                ColumnValue("email", "batman@dc.com"),
                ColumnValue("age", 85),
            )
        val kotlinDataClassMapper = KotlinDataClassMapper(SuperHero::class.java, autoTypesConverterMock, fieldsConverterMock)
        val ex =
            shouldThrow<KapperMappingException> {
                kotlinDataClassMapper.createInstance(resultSet, fields)
            }
        ex.message.shouldContain("id")
    }

    @Test
    fun `should convert when type not known`() {
        every { autoTypesConverterMock.convert(any(), any()) } returns "Foo"
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
            )
        every { fieldsConverterMock.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("id", UUID.randomUUID()),
                ColumnValue("name", 123),
            )
        val kotlinDataClassMapper = KotlinDataClassMapper(SuperHero::class.java, autoTypesConverterMock, fieldsConverterMock)
        kotlinDataClassMapper.createInstance(resultSet, fields)
        verify { autoTypesConverterMock.convert(123, String::class.java) }
    }

    @Test
    fun `should set to null when value null`() {
        val batman = SuperHero(UUID.randomUUID(), "Batman", "batman@dc.com", 85)
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "email" to Field(3, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
            )
        every { fieldsConverterMock.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("id", batman.id),
                ColumnValue("name", batman.name),
                ColumnValue("email", null),
            )
        val kotlinDataClassMapper = KotlinDataClassMapper(SuperHero::class.java, autoTypesConverterMock, fieldsConverterMock)
        val instance = kotlinDataClassMapper.createInstance(resultSet, fields)
        instance.shouldBe(SuperHero(batman.id, "Batman", null, null))
    }

    abstract class NoPrimaryConstructor {
        constructor(id: UUID)
        constructor(name: String)
    }

    @Test
    fun `should throw when no primary constructor`() {
        shouldThrow<KapperMappingException> {
            KotlinDataClassMapper(NoPrimaryConstructor::class.java, autoTypesConverterMock, fieldsConverterMock)
        }
    }

    @Test
    fun `should skip when no property found`() {
        val batman = SuperHero(UUID.randomUUID(), "Batman", "batman@dc.com", 85)
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "email" to Field(3, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "extra" to Field(4, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
            )
        every { fieldsConverterMock.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("id", batman.id),
                ColumnValue("name", batman.name),
                ColumnValue("email", batman.email),
                ColumnValue("extra", 1234),
            )
        val kotlinDataClassMapper = KotlinDataClassMapper(SuperHero::class.java, autoTypesConverterMock, fieldsConverterMock)
        val instance = kotlinDataClassMapper.createInstance(resultSet, fields)
        instance.shouldBe(batman.copy(age = null))
    }

    data class SuperHero(val id: UUID, val name: String, val email: String? = null, val age: Int? = null)

    data class HeroWithTags(val id: UUID, val name: String, val tags: List<Int>)

    @Test
    fun `should preserve original list when elements already match`() {
        val id = UUID.randomUUID()
        val tags = arrayListOf(1, 2, 3)
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "tags" to Field(3, JDBCType.ARRAY, "INTEGER[]", DbFlavour.UNKNOWN),
            )
        every { fieldsConverterMock.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("id", id),
                ColumnValue("name", "Batman"),
                ColumnValue("tags", tags),
            )
        val mapper = KotlinDataClassMapper(HeroWithTags::class.java, autoTypesConverterMock, fieldsConverterMock)
        val instance = mapper.createInstance(resultSet, fields)
        instance.tags.shouldBeSameInstanceAs(tags)
    }

    data class HeroWithLongTags(val id: UUID, val name: String, val tags: List<Int>)

    @Test
    fun `should coerce Long list elements to declared element type`() {
        val id = UUID.randomUUID()
        val tags = arrayListOf(1L, 2L, 3L)
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "tags" to Field(3, JDBCType.ARRAY, "BIGINT[]", DbFlavour.UNKNOWN),
            )
        every { fieldsConverterMock.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("id", id),
                ColumnValue("name", "Batman"),
                ColumnValue("tags", tags),
            )
        val mapper = KotlinDataClassMapper(HeroWithLongTags::class.java, autoConverter, fieldsConverterMock)
        val instance = mapper.createInstance(resultSet, fields)
        instance.shouldBe(HeroWithLongTags(id, "Batman", listOf(1, 2, 3)))
    }

    data class HeroWithMixedNullableTags(val id: UUID, val name: String, val tags: List<Int?>)

    @Test
    fun `should coerce mixed list with null elements to declared element type`() {
        val id = UUID.randomUUID()
        val tags = arrayListOf<Any?>(null, 1, 2L)
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "tags" to Field(3, JDBCType.ARRAY, "INTEGER[]", DbFlavour.UNKNOWN),
            )
        every { fieldsConverterMock.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("id", id),
                ColumnValue("name", "Batman"),
                ColumnValue("tags", tags),
            )
        val mapper = KotlinDataClassMapper(HeroWithMixedNullableTags::class.java, autoConverter, fieldsConverterMock)
        val instance = mapper.createInstance(resultSet, fields)
        instance.shouldBe(HeroWithMixedNullableTags(id, "Batman", listOf(null, 1, 2)))
    }

    data class HeroWithStarTags(val id: UUID, val name: String, val tags: List<*>)

    @Test
    fun `should map star-projection list without element coercion`() {
        val id = UUID.randomUUID()
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "tags" to Field(3, JDBCType.ARRAY, "INTEGER[]", DbFlavour.UNKNOWN),
            )
        every { fieldsConverterMock.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("id", id),
                ColumnValue("name", "Batman"),
                ColumnValue("tags", listOf(1, 2, 3)),
            )
        val mapper = KotlinDataClassMapper(HeroWithStarTags::class.java, autoConverter, fieldsConverterMock)
        val instance = mapper.createInstance(resultSet, fields)
        instance.shouldBe(HeroWithStarTags(id, "Batman", listOf(1, 2, 3)))
    }

    data class GenericHero<T>(val id: UUID, val name: String, val tags: List<T>)

    @Test
    fun `should map list with generic element type without coercion`() {
        val id = UUID.randomUUID()
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "tags" to Field(3, JDBCType.ARRAY, "INTEGER[]", DbFlavour.UNKNOWN),
            )
        every { fieldsConverterMock.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("id", id),
                ColumnValue("name", "Batman"),
                ColumnValue("tags", listOf(1, 2, 3)),
            )
        val mapper = KotlinDataClassMapper(GenericHero::class.java, autoConverter, fieldsConverterMock)
        val instance = mapper.createInstance(resultSet, fields)
        instance.shouldBe(GenericHero(id, "Batman", listOf(1, 2, 3)))
    }

    @Test
    fun `should route non-list value for list property through whole-value conversion`() {
        val id = UUID.randomUUID()
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "tags" to Field(3, JDBCType.ARRAY, "INTEGER[]", DbFlavour.UNKNOWN),
            )
        every { fieldsConverterMock.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("id", id),
                ColumnValue("name", "Batman"),
                ColumnValue("tags", "not-a-list"),
            )
        every { autoTypesConverterMock.convert("not-a-list", List::class.java) } returns emptyList<Int>()
        val mapper = KotlinDataClassMapper(HeroWithTags::class.java, autoTypesConverterMock, fieldsConverterMock)
        val instance = mapper.createInstance(resultSet, fields)
        instance.tags.shouldBe(emptyList<Int>())
        verify { autoTypesConverterMock.convert("not-a-list", List::class.java) }
    }

    data class HeroWithShortTags(val id: UUID, val name: String, val tags: List<Short>)

    @Test
    fun `should coerce Long list elements to Short`() {
        val id = UUID.randomUUID()
        val tags = arrayListOf(1L, 2L, 3L)
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "tags" to Field(3, JDBCType.ARRAY, "BIGINT[]", DbFlavour.UNKNOWN),
            )
        every { fieldsConverterMock.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("id", id),
                ColumnValue("name", "Batman"),
                ColumnValue("tags", tags),
            )
        val mapper = KotlinDataClassMapper(HeroWithShortTags::class.java, autoConverter, fieldsConverterMock)
        val instance = mapper.createInstance(resultSet, fields)
        instance.shouldBe(HeroWithShortTags(id, "Batman", listOf(1.toShort(), 2.toShort(), 3.toShort())))
    }

    @Test
    fun `should map list property without triggering converter`() {
        val id = UUID.randomUUID()
        val tags = arrayListOf(1, 2, 3)
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "tags" to Field(3, JDBCType.ARRAY, "INTEGER[]", DbFlavour.UNKNOWN),
            )
        every { fieldsConverterMock.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("id", id),
                ColumnValue("name", "Batman"),
                ColumnValue("tags", tags),
            )
        val mapper = KotlinDataClassMapper(HeroWithTags::class.java, autoTypesConverterMock, fieldsConverterMock)
        val instance = mapper.createInstance(resultSet, fields)
        instance.shouldBe(HeroWithTags(id, "Batman", listOf(1, 2, 3)))
        verify(exactly = 0) { autoTypesConverterMock.convert(any(), any()) }
    }

    data class HeroWithNullableTags(val id: UUID, val name: String, val tags: List<Int>? = null)

    @Test
    fun `should map null list property`() {
        val id = UUID.randomUUID()
        val fields =
            mapOf(
                "id" to Field(1, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "name" to Field(2, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN),
                "tags" to Field(3, JDBCType.ARRAY, "INTEGER[]", DbFlavour.UNKNOWN),
            )
        every { fieldsConverterMock.convert(resultSet, fields) } returns
            listOf(
                ColumnValue("id", id),
                ColumnValue("name", "Batman"),
                ColumnValue("tags", null),
            )
        val mapper = KotlinDataClassMapper(HeroWithNullableTags::class.java, autoTypesConverterMock, fieldsConverterMock)
        val instance = mapper.createInstance(resultSet, fields)
        instance.shouldBe(HeroWithNullableTags(id, "Batman", null))
    }
}
