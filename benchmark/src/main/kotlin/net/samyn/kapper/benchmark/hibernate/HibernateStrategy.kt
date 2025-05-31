package net.samyn.kapper.benchmark.hibernate

import net.samyn.kapper.benchmark.BenchmarkStrategy
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider
import java.sql.Connection
import java.util.UUID

class HibernateStrategy : HibernateBaseStrategy(), BenchmarkStrategy {
    override fun findHeroById(
        connection: Connection,
        id: UUID,
    ): Any? {
        return getSessionFactory(
            connection,
            listOf(
                SuperHeroEntity::class.java,
                VillainEntity::class.java,
                SuperHeroBattleEntity::class.java,
            ),
        ).openSession().use {
            it.find(SuperHeroEntity::class.java, id)
        }
    }

    override fun find100Heroes(connection: Connection): List<Any> {
        return getSessionFactory(
            connection,
            listOf(
                SuperHeroEntity::class.java,
                VillainEntity::class.java,
                SuperHeroBattleEntity::class.java,
            ),
        ).openSession().use {
            it.createQuery("FROM SuperHeroEntity", SuperHeroEntity::class.java)
                .setMaxResults(100).list()
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
            listOf(
                SuperHeroEntity::class.java,
                VillainEntity::class.java,
                SuperHeroBattleEntity::class.java,
            ),
        ).openSession().use {
            it.transaction.begin()
            it.merge(SuperHeroEntity(id, name, email, age))
            it.transaction.commit()
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
            listOf(
                SuperHeroEntity::class.java,
                VillainEntity::class.java,
                SuperHeroBattleEntity::class.java,
            ),
        ).openSession().use {
            it.transaction.begin()
            it.merge(SuperHeroEntity(id, name, email, age))
            it.transaction.commit()
        }
    }

    override fun findHeroBattles(
        connection: Connection,
        heroId: UUID,
    ): List<Any> {
        return getSessionFactory(
            connection,
            listOf(
                SuperHeroEntity::class.java,
                VillainEntity::class.java,
                SuperHeroBattleEntity::class.java,
            ),
        ).openSession().use {
            it.createSelectionQuery(
                """
            SELECT b FROM SuperHeroBattleEntity b 
            WHERE b.superHero.id = :id
        """,
                SuperHeroBattleEntity::class.java,
            )
                .setParameter("id", heroId)
                .list()
        }
    }

    class SingleConnectionProvider : ConnectionProvider {
        companion object {
            // This is hacky, but allows us to re-use the same connection so to not influence the
            //  benchmark results with connection management.
            private var connectionSingleton: Connection? = null

            fun setConnection(connection: Connection) {
                if (connectionSingleton == null) {
                    connectionSingleton = connection
                }
            }
        }

        override fun isUnwrappableAs(p0: Class<*>): Boolean {
            return false
        }

        override fun <T : Any?> unwrap(p0: Class<T>): T {
            TODO("Not yet implemented")
        }

        override fun getConnection(): Connection {
            return connectionSingleton!!
        }

        override fun closeConnection(p0: Connection?) {
            // do nothing
        }

        override fun supportsAggressiveRelease(): Boolean {
            return false
        }
    }
}
