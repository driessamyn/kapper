package net.samyn.kapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.samyn.kapper.internal.Mapper
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.reflect.KClass

class MapperTest {
    private val autoMapperMock = mockk<(Any, KClass<*>) -> Any>()
    private val mapper = Mapper<SuperHero>(SuperHero::class.java, autoMapperMock)

    @Test
    fun `should map to super hero`() {
        val batman = SuperHero(UUID.randomUUID(), "Batman", "batman@dc.com", 85)

        val instance =
            mapper.createInstance(
                listOf(
                    Mapper.ColumnValue("id", batman.id),
                    Mapper.ColumnValue("name", batman.name),
                    Mapper.ColumnValue("email", batman.email),
                    Mapper.ColumnValue("age", batman.age),
                ),
            )

        instance.shouldBe(batman)
    }

    @Test
    fun `should map to super hero respecting defaults`() {
        val batman = SuperHero(UUID.randomUUID(), "Batman", "batman@dc.com", 85)

        val instance =
            mapper.createInstance(
                listOf(
                    Mapper.ColumnValue("id", batman.id),
                    Mapper.ColumnValue("name", batman.name),
                ),
            )

        instance.shouldBe(SuperHero(batman.id, "Batman", null, null))
    }

    @Test
    fun `should throw when too many columns`() {
        val ex =
            shouldThrow<KapperMappingException> {
                mapper.createInstance(
                    listOf(
                        Mapper.ColumnValue("id", UUID.randomUUID()),
                        Mapper.ColumnValue("name", "joker"),
                        Mapper.ColumnValue("email", "joker@dc.com"),
                        Mapper.ColumnValue("age", 85),
                        Mapper.ColumnValue("foo", "bar"),
                    ),
                )
            }
        ex.message.shouldContain("foo")
    }

    @Test
    fun `should throw when non-optional are missing`() {
        val ex =
            shouldThrow<KapperMappingException> {
                mapper.createInstance(
                    listOf(
                        Mapper.ColumnValue("name", "joker"),
                        Mapper.ColumnValue("email", "joker@dc.com"),
                        Mapper.ColumnValue("age", 85),
                    ),
                )
            }
        ex.message.shouldContain("id")
    }

    @Test
    fun `should convert when type not known`() {
        every { autoMapperMock(any<Int>(), any<KClass<*>>()) } returns "Foo"
        mapper.createInstance(
            listOf(
                Mapper.ColumnValue("id", UUID.randomUUID()),
                Mapper.ColumnValue("name", 123),
            ),
        )
        verify { autoMapperMock(123, String::class) }
    }

    data class SuperHero(val id: UUID, val name: String, val email: String? = null, val age: Int? = null)
}
