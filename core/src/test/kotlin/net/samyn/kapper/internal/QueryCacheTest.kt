package net.samyn.kapper.internal

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class QueryCacheTest {
    @Test
    fun `parses each template once`() {
        var parseCount = 0
        val cache =
            QueryCache(parser = {
                parseCount++
                Query(it)
            })
        cache("SELECT 1")
        cache("SELECT 1")
        cache("SELECT 1")
        parseCount shouldBe 1
    }

    @Test
    fun `caches distinct templates independently`() {
        var parseCount = 0
        val cache =
            QueryCache(parser = {
                parseCount++
                Query(it)
            })
        cache("SELECT 1")
        cache("SELECT 2")
        cache("SELECT 1")
        cache("SELECT 2")
        parseCount shouldBe 2
    }

    @Test
    fun `returns the cached query`() {
        val cache = QueryCache()
        val first = cache("SELECT * FROM foo WHERE id = :id")
        val second = cache("SELECT * FROM foo WHERE id = :id")
        second shouldBe first
    }

    @Test
    fun `is bounded so unique templates cannot exhaust the heap`() {
        var parseCount = 0
        val cache =
            QueryCache(maxSize = 2, parser = {
                parseCount++
                Query(it)
            })
        cache("SELECT 1")
        cache("SELECT 2")
        cache("SELECT 3")
        cache.size shouldBe 2
        // the first two are retained, the third was parsed but not cached
        cache("SELECT 1")
        cache("SELECT 2")
        parseCount shouldBe 3
        cache("SELECT 3")
        parseCount shouldBe 4
    }

    @Test
    fun `bound is configurable`() {
        val cache = QueryCache(maxSize = 5)
        repeat(10) { cache("SELECT $it") }
        cache.size shouldBe 5
    }

    @Test
    fun `default bound is used`() {
        QueryCache.DEFAULT_MAX_SIZE shouldBe 1000
    }

    @Test
    fun `bound is not exceeded when templates are inserted concurrently`() {
        // The size check and the insert must be atomic: without that, concurrent misses can each
        // pass the check and overshoot maxSize.
        val maxSize = 1
        val workerCount = (Runtime.getRuntime().availableProcessors() * 2).coerceAtLeast(8)
        val ready = CountDownLatch(workerCount)
        val go = AtomicBoolean(false)
        val cache =
            QueryCache(
                maxSize = maxSize,
                parser = {
                    ready.countDown()
                    // Spin rather than park, so every worker leaves the parser at the same instant.
                    while (!go.get()) Thread.onSpinWait()
                    Query(it)
                },
            )
        val workers = (0 until workerCount).map { i -> thread { cache("SELECT $i") } }
        // Bound the wait so a worker that cannot start fails the test rather than hanging it.
        val allStarted = ready.await(30, TimeUnit.SECONDS)
        go.set(true)
        allStarted shouldBe true
        workers.forEach { it.join() }
        cache.size shouldBe maxSize
    }
}
