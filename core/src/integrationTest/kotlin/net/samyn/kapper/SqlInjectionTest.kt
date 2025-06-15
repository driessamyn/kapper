package net.samyn.kapper

import io.kotest.matchers.collections.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class SqlInjectionTest : AbstractDbTests() {
    @Test
    fun `cannot inject SQL with parameter`() {
        connection.execute(
            "UPDATE super_heroes_$testId SET name = 'foo' WHERE name = :name",
            "name" to "bar; DROP TABLE super_heroes",
        )
        connection.query<SuperHero>("SELECT * FROM super_heroes_$testId").shouldNotBeEmpty()
    }
}
