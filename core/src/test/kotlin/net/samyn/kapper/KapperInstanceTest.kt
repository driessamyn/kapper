package net.samyn.kapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.ResultSet

class KapperInstanceTest {
    private val fooMapper = mockk<Mapper<Foo>>()

    init {
        Kapper.mapperRegistry.registerIfAbsent(Foo::class.java, fooMapper)
    }

    @Test
    fun `createInstance should return a new Kapper instance`() {
        val k1 = Kapper.createInstance()
        val k2 = Kapper.createInstance()
        k1 shouldNotBeSameInstanceAs k2
    }

    @Test
    fun `createInstance should return singleton Kapper instance`() {
        val k1 = Kapper.instance
        val k2 = Kapper.instance
        k1 shouldBeSameInstanceAs k2
    }

    data class Foo(val id: Int, val name: String)

    @Test
    fun `query default implementation assumes registered mapper`() {
        val k = mockk<Kapper>(relaxed = true)
        val mapper = slot<(ResultSet, Map<String, Field>) -> Foo>()
        // call original from interface so we can capture the mapper that is used
        every { k.query<Foo>(any(), any(), any(), any()) } answers { callOriginal() }
        every { k.query(any(), any(), any(), capture(mapper), any()) } returns emptyList()
        val connection = mockk<Connection>(relaxed = true)
        val queryTemplate = "FOO"
        k.query(Foo::class.java, connection, queryTemplate, emptyMap())
        verify { k.query(Foo::class.java, connection, queryTemplate, mapper.captured, emptyMap()) }
        // slightly dodgy equality check, but signature is internal to the std lib. String should validate the function and the owner
        mapper.captured.toString() shouldBe fooMapper::createInstance.toString()
    }

    @Test
    fun `query default implementation throws when mapper cannot be created`() {
        val k = mockk<Kapper>(relaxed = true)
        every { k.query<Foo>(any(), any(), any(), any()) } answers { callOriginal() }
        val ex = RuntimeException("test")
        mockkObject(Kapper.mapperRegistry) {
            every { Kapper.mapperRegistry.get(Foo::class.java) } throws ex
            shouldThrow<Exception> {
                k.query(Foo::class.java, mockk<Connection>(relaxed = true), "FOO", emptyMap())
            }.cause!! should {
                it.shouldBeInstanceOf<KapperMappingException>()
                it.cause shouldBe ex
            }
        }
    }

    @Test
    fun `querySingle default implementation assumes registered mapper`() {
        val k = mockk<Kapper>(relaxed = true)
        val mapper = slot<(ResultSet, Map<String, Field>) -> Foo>()
        // call original from interface so we can capture the auto-mapper that is used
        every { k.querySingle<Foo>(any(), any(), any(), any()) } answers { callOriginal() }
        every { k.querySingle(any(), any(), any(), capture(mapper), any()) } returns Foo(1, "foo")
        val connection = mockk<Connection>(relaxed = true)
        val queryTemplate = "FOO"
        k.querySingle(Foo::class.java, connection, queryTemplate, emptyMap())
        verify { k.querySingle(Foo::class.java, connection, queryTemplate, mapper.captured, emptyMap()) }
        // slightly dodgy equality check, but signature is internal to the std lib. String should validate the function and the owner
        mapper.captured.toString() shouldBe fooMapper::createInstance.toString()
    }

    @Test
    fun `querySingle default implementation throws when auto-mapper cannot be created`() {
        val k = mockk<Kapper>(relaxed = true)
        every { k.querySingle<Foo>(any(), any(), any(), any()) } answers { callOriginal() }
        every { k.querySingle(any(), any(), any(), any<(ResultSet, Map<String, Field>) -> Foo>(), any()) } returns Foo(1, "foo")
        val ex = RuntimeException("test")
        mockkObject(Kapper.mapperRegistry) {
            every { Kapper.mapperRegistry.get(Foo::class.java) } throws ex
            shouldThrow<Exception> {
                k.querySingle(Foo::class.java, mockk<Connection>(relaxed = true), "FOO", emptyMap())
            }.cause!! should {
                // Need to figure out why this is wrapped in an invocation exception
                it.shouldBeInstanceOf<KapperMappingException>()
                it.cause shouldBe ex
            }
        }
    }
}
