package net.samyn.kapper

import io.kotest.matchers.collections.shouldNotBeEmpty
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.Connection

class SqlInjectionTest : AbstractDbTests() {
    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `cannot inject SQL with parameter`(connection: Connection) {
        connection.execute(
            "UPDATE super_heroes_$testId SET name = 'foo' WHERE name = :name",
            "name" to "bar; DROP TABLE super_heroes",
        )

        connection.query<SuperHero>("SELECT * FROM super_heroes_$testId").shouldNotBeEmpty()
    }
}
