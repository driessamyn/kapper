package net.samyn.kapper.benchmark.kapper

import net.samyn.kapper.Field
import net.samyn.kapper.query
import net.samyn.kapper.querySingle
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID

class KapperNoAutomapStrategy : KapperBaseStrategy() {
    override fun findHeroById(
        connection: Connection,
        id: UUID,
    ): Any? {
        return connection.querySingle<SuperHero>(
            "SELECT id, name, email, age FROM super_heroes WHERE id = :id",
            ::mapHero,
            "id" to id,
        )
    }

    override fun find100Heroes(connection: Connection): List<Any> {
        return connection.query<SuperHero>(
            "SELECT id, name, email, age FROM super_heroes LIMIT 100",
            ::mapHero,
        )
    }

    override fun findHeroBattles(
        connection: Connection,
        heroId: UUID,
    ): List<Any> {
        return connection.query<SuperHeroBattle>(
            """
            SELECT s.name as superhero, v.name as villain, b.battle_date as date
            FROM super_heroes as s
            INNER JOIN battles as b on s.id = b.super_hero_id
            INNER JOIN villains as v on v.id = b.villain_id
            WHERE s.id = :id
            """,
            { resultSet, _ ->
                SuperHeroBattle(
                    resultSet.getString("superhero"),
                    resultSet.getString("villain"),
                    resultSet.getTimestamp("date").toLocalDateTime(),
                )
            },
            "id" to heroId,
        )
    }

    private fun mapHero(
        resultSet: ResultSet,
        fields: Map<String, Field>,
    ): SuperHero =
        SuperHero(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name"),
            resultSet.getString("email"),
            resultSet.getInt("age"),
        )
}
