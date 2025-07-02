package net.samyn.kapper.benchmark

import java.sql.Connection
import java.util.UUID

interface BenchmarkStrategy {
    fun findHeroById(
        connection: Connection,
        id: UUID,
    ): Any?

    fun find100Heroes(connection: Connection): List<Any>

    fun insertNewHero(
        connection: Connection,
        id: UUID,
        name: String,
        email: String,
        age: Int,
    )

    fun insertManyHeroes(connection: Connection)

    fun changeHero(
        connection: Connection,
        id: UUID,
        name: String,
        email: String,
        age: Int,
    )

    fun findHeroBattles(
        connection: Connection,
        heroId: UUID,
    ): List<Any>
}
