package net.samyn.kapper

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test

class ExceptionsTest {
    private val cause = Exception("cause")

    @Test
    fun `KapperMappingException should contain cause`() {
        val exception = KapperMappingException("message", cause)
        try {
            throw exception
        } catch (exception: Exception) {
            exception
        } should {
            it.shouldBeSameInstanceAs(exception)
            it.message shouldBe "message"
            it.cause shouldBe cause
        }
    }

    @Test
    fun `KapperUnsupportedOperationException should contain cause`() {
        val exception = KapperUnsupportedOperationException("message", cause)
        try {
            throw exception
        } catch (exception: Exception) {
            exception
        } should {
            it.shouldBeSameInstanceAs(exception)
            it.message shouldBe "message"
            it.cause shouldBe cause
        }
    }

    @Test
    fun `KapperParseException should contain cause`() {
        val exception = KapperParseException("message", cause)
        try {
            throw exception
        } catch (exception: Exception) {
            exception
        } should {
            it.shouldBeSameInstanceAs(exception)
            it.message shouldBe "message"
            it.cause shouldBe cause
        }
    }

    @Test
    fun `KapperResultException should contain cause`() {
        val exception = KapperResultException("message", cause)
        try {
            throw exception
        } catch (exception: Exception) {
            exception
        } should {
            it.shouldBeSameInstanceAs(exception)
            it.message shouldBe "message"
            it.cause shouldBe cause
        }
    }

    @Test
    fun `KapperQueryException should contain cause`() {
        val exception = KapperQueryException("message", cause)
        try {
            throw exception
        } catch (exception: Exception) {
            exception
        } should {
            it.shouldBeSameInstanceAs(exception)
            it.message shouldBe "message"
            it.cause shouldBe cause
        }
    }
}
