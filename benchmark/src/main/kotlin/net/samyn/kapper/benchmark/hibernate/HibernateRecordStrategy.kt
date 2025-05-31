package net.samyn.kapper.benchmark.hibernate

import net.samyn.kapper.benchmark.BenchmarkStrategy
import java.sql.Connection
import java.util.UUID

@Suppress("unused")
class HibernateRecordStrategy : HibernateBaseStrategy(), BenchmarkStrategy {
    override fun findHeroById(
        connection: Connection,
        id: UUID,
    ): Any? {
        return getSessionFactory(
            connection,
            listOf(SuperHeroRecordEntity::class.java),
        ).openSession().use {
            it.find(SuperHeroRecordEntity::class.java, id)
        }
    }

    override fun find100Heroes(connection: Connection): List<Any> {
        return getSessionFactory(
            connection,
            listOf(SuperHeroRecordEntity::class.java),
        ).openSession().use {
            it.createQuery("FROM SuperHeroRecordEntity", SuperHeroRecordEntity::class.java).resultList
        }
    }

    override fun insertNewHero(
        connection: Connection,
        id: UUID,
        name: String,
        email: String,
        age: Int,
    ) {
        getSessionFactory(
            connection,
            listOf(SuperHeroRecordEntity::class.java),
        ).openSession().use { session ->
            session.beginTransaction()
            session.persist(SuperHeroRecordEntity(id, name, email, age))
            session.transaction.commit()
        }
    }

    override fun changeHero(
        connection: Connection,
        id: UUID,
        name: String,
        email: String,
        age: Int,
    ) {
        getSessionFactory(
            connection,
            listOf(SuperHeroRecordEntity::class.java),
        ).openSession().use { session ->
            session.beginTransaction()
            val hero = session.find(SuperHeroRecordEntity::class.java, id)
            if (hero != null) {
                session.remove(hero)
                session.persist(SuperHeroRecordEntity(id, name, email, age))
            }
            session.transaction.commit()
        }
    }

    override fun findHeroBattles(
        connection: Connection,
        heroId: UUID,
    ): List<Any> {
        // Not implemented for record entity, could be added if needed
        return emptyList()
    }
}
