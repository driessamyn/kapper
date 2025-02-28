package net.samyn.kapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.ResultSet

class KapperInstanceTest {
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
    fun `query default implementation assumes auto-mapper`() {
        val k = mockk<Kapper>(relaxed = true)
        val autoMapper = slot<(ResultSet, Map<String, Field>) -> Foo>()
        // call original from interface so we can capture the auto-mapper that is used
        every { k.query<Foo>(any(), any(), any(), any()) } answers { callOriginal() }
        every { k.query(any(), any(), any(), capture(autoMapper), any()) } returns emptyList()
        val connection = mockk<Connection>(relaxed = true)
        val queryTemplate = "FOO"
        k.query(Foo::class.java, connection, queryTemplate, emptyMap())
        val expectedAutoMapper = createMapper(Foo::class.java)::createInstance
        // slightly dodgy equality check, but signature is internal to the std lib. String should validate the function and the owner
        autoMapper.captured.toString() shouldBe expectedAutoMapper.toString()
        verify { k.query(Foo::class.java, connection, queryTemplate, autoMapper.captured, emptyMap()) }
    }

    @Test
    fun `query default implementation throws when auto-mapper cannot be created`() {
        val k = mockk<Kapper>(relaxed = true)
        every { k.query<Foo>(any(), any(), any(), any()) } answers { callOriginal() }
//        every { k.query(any(), any(), any(), any<(ResultSet, Map<String, Field>) -> Foo>(), any()) } returns emptyList()
        val ex = RuntimeException("test")
        mockkStatic("net.samyn.kapper.MapperFactoryKt") {
            every { createMapper(Foo::class.java) } throws ex
            shouldThrow<Exception> {
                k.query(Foo::class.java, mockk<Connection>(relaxed = true), "FOO", emptyMap())
            }.cause!! should {
                // Need to figure out why this is wrapped in an invocation exception
                it.shouldBeInstanceOf<KapperMappingException>()
                it.cause shouldBe ex
            }
        }
    }

    @Test
    fun `querySingle default implementation assumes auto-mapper`() {
        val k = mockk<Kapper>(relaxed = true)
        val autoMapper = slot<(ResultSet, Map<String, Field>) -> Foo>()
        // call original from interface so we can capture the auto-mapper that is used
        every { k.querySingle<Foo>(any(), any(), any(), any()) } answers { callOriginal() }
        every { k.querySingle(any(), any(), any(), capture(autoMapper), any()) } returns Foo(1, "foo")
        val connection = mockk<Connection>(relaxed = true)
        val queryTemplate = "FOO"
        k.querySingle(Foo::class.java, connection, queryTemplate, emptyMap())
        val expectedAutoMapper = createMapper(Foo::class.java)::createInstance
        // slightly dodgy equality check, but signature is internal to the std lib. String should validate the function and the owner
        autoMapper.captured.toString() shouldBe expectedAutoMapper.toString()
        verify { k.querySingle(Foo::class.java, connection, queryTemplate, autoMapper.captured, emptyMap()) }
    }

    @Test
    fun `querySingle default implementation throws when auto-mapper cannot be created`() {
        val k = mockk<Kapper>(relaxed = true)
        every { k.querySingle<Foo>(any(), any(), any(), any()) } answers { callOriginal() }
        every { k.querySingle(any(), any(), any(), any<(ResultSet, Map<String, Field>) -> Foo>(), any()) } returns Foo(1, "foo")
        val ex = RuntimeException("test")
        mockkStatic("net.samyn.kapper.MapperFactoryKt") {
            every { createMapper(Foo::class.java) } throws ex
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
