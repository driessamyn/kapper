package net.samyn.kapper.internal.automapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.types.shouldBeInstanceOf
import net.samyn.kapper.KapperMappingException
import org.junit.jupiter.api.Test

class AutoMapperFactoryTest {
    data class Foo(val name: String)

    @Test
    fun `when data class use KotlinDataClassMapper`() {
        createAutoMapper(Foo::class.java).shouldBeInstanceOf<KotlinDataClassMapper<*>>()
    }

    @Test
    fun `when record class use RecordMapper`() {
        createAutoMapper(SuperHeroRecord::class.java).shouldBeInstanceOf<RecordMapper<*>>()
    }

    class RegularClass(val name: String)

    @Test
    fun `when regular class throw`() {
        shouldThrow<KapperMappingException> {
            createAutoMapper(RegularClass::class.java)
        }
    }
}
