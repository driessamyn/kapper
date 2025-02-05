package net.samyn.kapper

import io.kotest.matchers.collections.shouldNotBeEmpty
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.testcontainers.containers.JdbcDatabaseContainer

class SqlInjectionTest : AbstractDbTests() {
    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `cannot inject SQL with parameter`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            connection.execute(
                "UPDATE super_heroes SET name = 'foo' WHERE name = :name;",
                "name" to "bar; DROP TABLE super_heroes;",
            )

            connection.query<SuperHero>("SELECT * FROM super_heroes").shouldNotBeEmpty()
        }
    }
}
