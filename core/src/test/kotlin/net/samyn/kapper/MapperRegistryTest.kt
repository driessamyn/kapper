package net.samyn.kapper

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.mockk
import net.samyn.kapper.internal.automapper.KotlinDataClassMapper
import org.junit.jupiter.api.Test
import java.sql.ResultSet

class MapperRegistryTest {
    private val mapper = mockk<Mapper<Foo>>()
    private val mapper2 = mockk<Mapper<Foo2>>()

    private val registry = MapperRegistry()

    @Test
    fun `when register inline able to get`() {
        registry.register<Foo>(mapper)

        registry.get(Foo::class.java) shouldBeSameInstanceAs mapper
    }

    @Test
    fun `when register able to get`() {
        registry.register(Foo2::class.java, mapper2)

        registry.get(Foo2::class.java) shouldBeSameInstanceAs mapper2
    }

    @Test
    fun `when duplicate registration throw`() {
        registry.register(Foo::class.java, mapper)

        val exception =
            shouldThrow<IllegalStateException> {
                registry.register(Foo::class.java, mapper)
            }
        exception.message shouldContain "${Foo::class.java} is already registered"
    }

    @Test
    fun `when registerIfAbsent and duplicate do not throw`() {
        registry.register(Foo::class.java, mapper)

        shouldNotThrow<IllegalStateException> {
            registry.registerIfAbsent<Foo>(mapper)
        }

        registry.get(Foo::class.java) shouldBeSameInstanceAs mapper
    }

    @Test
    fun `when registerIfAbsent and duplicate is different throw`() {
        val otherMapper =
            object : Mapper<Foo> {
                override fun createInstance(
                    resultSet: ResultSet,
                    fields: Map<String, Field>,
                ): Foo {
                    TODO("Not yet implemented")
                }
            }
        registry.register(Foo::class.java, mapper)

        val exception =
            shouldThrow<IllegalStateException> {
                registry.registerIfAbsent(Foo::class.java, otherMapper)
            }
        exception.message shouldContain "${Foo::class.java} is already registered"
    }

    @Test
    fun `when no custom registration use auto-mapper`() {
        registry.get(Foo::class.java).javaClass shouldBe KotlinDataClassMapper::class.java
    }

    data class Foo(val name: String)

    data class Foo2(val name: String)
}
