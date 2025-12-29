package net.samyn.kapper.example.kotlin.kapper

import net.samyn.kapper.example.kotlin.PopularMovie
import net.samyn.kapper.example.kotlin.SuperHero
import net.samyn.kapper.example.kotlin.SuperHeroBattle
import net.samyn.kapper.example.kotlin.Villain
import net.samyn.kapper.execute
import net.samyn.kapper.query
import net.samyn.kapper.querySingle
import net.samyn.kapper.withTransaction
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class SuperHeroRepository(
    private val dataSource: DataSource,
) {
    private val dbType: DbType

    init {
        dbType =
            dataSource.connection.use {
                when (it.metaData.databaseProductName) {
                    "MySQL" -> DbType.MYSQL
                    else -> DbType.POSTGRESQL
                }
            }
    }

    enum class DbType {
        POSTGRESQL,
        MYSQL,
    }

    // List all superheroes
    fun list(): List<SuperHero> =
        dataSource.connection.use {
            it.query<SuperHero>("SELECT * FROM super_heroes")
        }

    // Find a superhero by ID
    fun findById(id: UUID) =
        dataSource.connection.use {
            it.querySingle<SuperHero>(
                "SELECT * FROM super_heroes WHERE id = :id",
                "id" to id,
            )
        }

    // Find a superhero by name
    fun findByName(name: String) =
        dataSource.connection.use {
            it.querySingle<SuperHero>(
                "SELECT * FROM super_heroes WHERE name = :name",
                "name" to name,
            )
        }

    // Find battles involving a specific superhero
    fun findBattles(superHeroName: String) =
        dataSource.connection.use {
            it.query<SuperHeroBattle>(
                """
                SELECT s.name as superhero, v.name as villain, b.battle_date as date
                FROM super_heroes as s
                INNER JOIN battles as b on s.id = b.super_hero_id
                INNER JOIN villains as v on v.id = b.villain_id
                WHERE s.name = :name 
                """.trimIndent(),
                "name" to superHeroName,
            )
        }

    // Insert a new superhero
    fun insertHero(superHero: SuperHero) =
        dataSource.connection.use {
            it.execute(
                """
                INSERT INTO super_heroes(id, name, email, age) 
                VALUES (:id, :name, :email, :age)
                """.trimIndent(),
                "id" to superHero.id,
                "name" to superHero.name,
                "email" to superHero.email,
                "age" to superHero.age,
            )
        }

    // Insert a new battle involving a superhero and a villain
    fun insertBattle(
        superHero: SuperHero,
        villain: Villain,
        date: LocalDateTime,
    ) = dataSource.withTransaction {
        it.execute(
            """
            INSERT INTO super_heroes(id, name, email, age) 
            VALUES (:id, :name, :email, :age)
            ${ignoreConflict("id")}
            """.trimIndent(),
            "id" to superHero.id,
            "name" to superHero.name,
            "email" to superHero.email,
            "age" to superHero.age,
        )
        it.execute(
            """
            INSERT INTO villains(id, name) 
            VALUES (:id, :name)
            ${ignoreConflict("id")}
            """.trimIndent(),
            "id" to villain.id,
            "name" to villain.name,
        )
        it.execute(
            """
            INSERT INTO battles(super_hero_id, villain_id, battle_date, updated_ts)
            VALUES (:super_hero_id, :villain_id, :date, NOW())
            """.trimIndent(),
            "super_hero_id" to superHero.id,
            "villain_id" to villain.id,
            "date" to date,
        )
    }

    fun findPopularMovies(): List<PopularMovie> =
        dataSource.connection.use {
            // example of complex query and custom mapper
            var allTimeRank = 1
            it.query(
                """
                 SELECT
                 title,
                 release_date, 
                 gross_worldwide, 
                 AVG(gross_worldwide) OVER() AS total_average_gross,
                 AVG(gross_worldwide) OVER(PARTITION BY EXTRACT(YEAR FROM release_date)) AS average_annual_gross
                FROM movies 
                ORDER BY gross_worldwide DESC
                LIMIT 3
                """.trimIndent(),
                { rs, _ ->
                    val gross = rs.getLong("gross_worldwide")
                    val annualAvgGross = rs.getInt("average_annual_gross")
                    PopularMovie(
                        rs.getString("title"),
                        gross,
                        gross / annualAvgGross.toDouble(),
                        allTimeRank++,
                    )
                },
            )
        }

    // example of something non-trivial that impacts the Query
    //  in this case, we want to support both mySQL and PostgreSQL in their
    //  own way of doing an insert and ignoring the duplicate.
    private fun ignoreConflict(updateCol: String) =
        if (DbType.MYSQL == dbType) {
            "ON DUPLICATE KEY UPDATE $updateCol=$updateCol"
        } else {
            "ON CONFLICT DO NOTHING"
        }
}
