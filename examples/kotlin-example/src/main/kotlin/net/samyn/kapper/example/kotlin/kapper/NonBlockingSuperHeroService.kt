package net.samyn.kapper.example.kotlin.kapper

import net.samyn.kapper.coroutines.withConnection
import net.samyn.kapper.example.kotlin.SuperHero
import net.samyn.kapper.execute
import net.samyn.kapper.query
import net.samyn.kapper.withTransaction
import java.lang.Thread.sleep
import javax.sql.DataSource

class NonBlockingSuperHeroService(
    private val dataSource: DataSource,
) {
    // equivalent of the list function in SuperHeroRepository but using coroutines
    //  a sleep is added to query to illustrate the non-blocking nature of the function
    suspend fun listSlowly(): List<SuperHero> =
        dataSource.withConnection {
            val heroes = it.query<SuperHero>("SELECT * FROM super_heroes")
            sleep(1000) // sleeping the IO dispatcher thread before returning ... very very bad!
            heroes
        }

    // very inefficient (bad) way of inserting heroes, sleeping between each insert,
    //   to illustrates the non-blocking nature of the function and the use of transactions.
    suspend fun insertSlowly(heroes: List<SuperHero>) {
        dataSource.withTransaction { conn ->
            heroes.forEach { hero ->
                conn.execute(
                    "INSERT INTO super_heroes (id, name, email) VALUES (:id, :name, :email)",
                    "id" to hero.id,
                    "name" to hero.name,
                    "email" to hero.email,
                )
                sleep(10) // sleeping the IO dispatcher thread ... very very bad!
            }
        }
    }
}
