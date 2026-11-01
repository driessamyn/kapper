package net.samyn.kapper.coroutines

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.util.concurrent.Executors
import javax.sql.DataSource
import kotlin.coroutines.ContinuationInterceptor
import kotlin.time.Duration.Companion.seconds

class DatasourceWithConnectionTest {
    @Test
    fun `create connection and pass to block`() {
        val dataSource = mockk<DataSource>(relaxed = true)
        val connection = mockk<Connection>(relaxed = true)
        coEvery { dataSource.connection } returns connection
        coEvery { connection.close() } just Runs
        val dispatcher = Dispatchers.IO

        val result =
            runBlocking {
                dataSource.withConnection(dispatcher) {
                    it shouldBe connection
                    123
                }
            }

        coVerify { dataSource.connection }
        coVerify { connection.close() }
        result shouldBe 123
    }

    @Test
    fun `close connection when exception thrown`() {
        val dataSource = mockk<DataSource>(relaxed = true)
        val connection = mockk<Connection>(relaxed = true)
        coEvery { dataSource.connection } returns connection
        coEvery { connection.close() } just Runs
        val dispatcher = Dispatchers.IO
        val block: suspend (Connection) -> Unit = { throw RuntimeException("test") }

        shouldThrow<RuntimeException> {
            runBlocking {
                dataSource.withConnection(dispatcher, block)
            }
        }
        coVerify { connection.close() }
    }

    @Test
    fun `create connection on given dispatcher`() =
        runTest {
            val dataSource = mockk<DataSource>(relaxed = true)
            val connection = mockk<Connection>(relaxed = true)
            coEvery { dataSource.connection } returns connection
            coEvery { connection.close() } just Runs
            val dispatcher = Dispatchers.Default
            var actualDispatcher: ContinuationInterceptor? = null
            val block: suspend (Connection) -> Unit = {
                actualDispatcher = currentCoroutineContext()[ContinuationInterceptor]
                println("Executing on $actualDispatcher")
            }

            val query =
                launch {
                    dataSource.withConnection(dispatcher, block)
                }
            query.join()
            actualDispatcher shouldBe dispatcher
        }

    @Test
    fun `acquire and close connection on given dispatcher`() =
        runTest {
            val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "acquisition-thread") }
            val dispatcher = executor.asCoroutineDispatcher()
            try {
                val connection = mockk<Connection>(relaxed = true)
                var acquisitionThread: String? = null
                var closeThread: String? = null
                every { connection.close() } answers { closeThread = Thread.currentThread().name }
                val dataSource = mockk<DataSource>(relaxed = true)
                every { dataSource.connection } answers {
                    acquisitionThread = Thread.currentThread().name
                    connection
                }

                dataSource.withConnection(dispatcher) { it shouldBe connection }

                // both the blocking pool checkout and the close run on the given dispatcher,
                // not on the caller's thread
                acquisitionThread shouldBe "acquisition-thread"
                closeThread shouldBe "acquisition-thread"
            } finally {
                executor.shutdown()
            }
        }

    @Test
    fun `create connection on IO dispatcher`() =
        runTest {
            val dataSource = mockk<DataSource>(relaxed = true)
            val connection = mockk<Connection>(relaxed = true)
            coEvery { dataSource.connection } returns connection
            coEvery { connection.close() } just Runs
            var actualDispatcher: ContinuationInterceptor? = null
            val block: suspend (Connection) -> Unit = {
                actualDispatcher = currentCoroutineContext()[ContinuationInterceptor]
                println("Executing on $actualDispatcher")
            }

            val query =
                launch {
                    dataSource.withConnection(block)
                }
            query.join()
            actualDispatcher shouldBe Dispatchers.IO
        }

    @Test
    fun `can continue while block executes`() =
        runTest {
            val scheduler = testScheduler // the scheduler used for this test
            val ioDispatcher = StandardTestDispatcher(scheduler, name = "IO dispatcher")

            val dataSource = mockk<DataSource>(relaxed = true)
            val connection = mockk<Connection>(relaxed = true)
            coEvery { dataSource.connection } returns connection
            var ran = false
            val delay = 10.seconds
            val block: suspend (Connection) -> Unit = {
                delay(delay)
                ran = true
            }

            launch {
                dataSource.withConnection(ioDispatcher, block)
            }
            ran shouldBe false
            ioDispatcher.scheduler.advanceTimeBy(delay + 1.seconds)
            ran shouldBe true
        }
}
