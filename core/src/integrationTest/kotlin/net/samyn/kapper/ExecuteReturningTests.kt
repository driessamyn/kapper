package net.samyn.kapper

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import net.samyn.kapper.internal.getDbFlavour
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.util.UUID

class ExecuteReturningTests : AbstractDbTests() {
    // RETURNING clause support varies by database.
    // MySQL and Oracle use different syntax; MSSQL uses OUTPUT.
    private val returningDbs = setOf(DbFlavour.POSTGRESQL, DbFlavour.DUCKDB, DbFlavour.SQLITE)

    override fun setupDatabase(connection: Connection) {
        super.setupDatabase(connection)
    }

    @Test
    fun `SQL Insert returning mapped record`() {
        assumeTrue(connection.getDbFlavour() in returningDbs, "RETURNING clause not supported on this database")
        val newHero = superman.copy(id = UUID.randomUUID())
        val results =
            connection.executeReturning<SuperHero>(
                """
                INSERT INTO super_heroes_$testId(id, name, email, age)
                VALUES(:id, :name, :email, :age)
                RETURNING *
                """,
                "id" to newHero.id,
                "name" to newHero.name,
                "email" to newHero.email,
                "age" to newHero.age,
            )
        results shouldHaveSize 1
        results[0] shouldBe newHero
    }

    @Test
    fun `SQL Update returning mapped record`() {
        assumeTrue(connection.getDbFlavour() in returningDbs, "RETURNING clause not supported on this database")
        val batmanClone = batman.copy(id = UUID.randomUUID())
        connection.createStatement().use { stmt ->
            stmt.execute(
                "INSERT INTO super_heroes_$testId(id, name) " +
                    "VALUES(${convertUUIDString(batmanClone.id, connection.getDbFlavour())}, 'foo')",
            )
        }
        val updated = batmanClone.copy(name = "Batman Updated", email = "batman@wayne.com", age = 42)
        val results =
            connection.executeReturning<SuperHero>(
                "UPDATE super_heroes_$testId SET name = :name, email = :email, age = :age WHERE id = :id RETURNING *",
                "id" to updated.id,
                "name" to updated.name,
                "email" to updated.email,
                "age" to updated.age,
            )
        results shouldHaveSize 1
        results[0] shouldBe updated
    }

    @Test
    fun `SQL Insert DTO returning mapped record`() {
        assumeTrue(connection.getDbFlavour() in returningDbs, "RETURNING clause not supported on this database")
        val newHero = spiderMan.copy(id = UUID.randomUUID())
        val results =
            connection.executeReturning<SuperHero, SuperHero>(
                """
                INSERT INTO super_heroes_$testId(id, name, email, age)
                VALUES(:id, :name, :email, :age)
                RETURNING *
                """,
                newHero,
                "id" to SuperHero::id,
                "name" to SuperHero::name,
                "email" to SuperHero::email,
                "age" to SuperHero::age,
            )
        results shouldHaveSize 1
        results[0] shouldBe newHero
    }

    @Test
    fun `SQL Insert with custom mapper returning selected fields`() {
        assumeTrue(connection.getDbFlavour() in returningDbs, "RETURNING clause not supported on this database")
        val newHero = batman.copy(id = UUID.randomUUID())
        val results =
            connection.executeReturning(
                """
                INSERT INTO super_heroes_$testId(id, name, email, age)
                VALUES(:id, :name, :email, :age)
                RETURNING *
                """,
                mapper = { rs, _ -> rs.getString("name") },
                "id" to newHero.id,
                "name" to newHero.name,
                "email" to newHero.email,
                "age" to newHero.age,
            )
        results shouldHaveSize 1
        results[0] shouldBe newHero.name
    }
}
