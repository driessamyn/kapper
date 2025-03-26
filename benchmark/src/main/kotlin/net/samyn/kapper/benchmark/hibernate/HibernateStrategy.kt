package net.samyn.kapper.benchmark.hibernate

import net.samyn.kapper.benchmark.BenchmarkStrategy
import net.samyn.kapper.internal.DbFlavour
import net.samyn.kapper.internal.getDbFlavour
import org.hibernate.SessionFactory
import org.hibernate.cfg.AvailableSettings
import org.hibernate.cfg.Configuration
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider
import java.sql.Connection
import java.util.UUID

class HibernateStrategy : BenchmarkStrategy {
    private var sessionFactory: SessionFactory? = null

    private fun getSessionFactory(connection: Connection): SessionFactory {
        if (sessionFactory == null) {
            SingleConnectionProvider.setConnection(connection)
            sessionFactory =
                Configuration()
                    .addAnnotatedClass(SuperHeroEntity::class.java)
                    .addAnnotatedClass(VillainEntity::class.java)
                    .addAnnotatedClass(SuperHeroBattleEntity::class.java)
                    .setProperty(
                        AvailableSettings.CONNECTION_PROVIDER,
                        SingleConnectionProvider::class.java.name,
                    )
                    .setProperty(AvailableSettings.SHOW_SQL, false)
                    .setProperty(AvailableSettings.FORMAT_SQL, false).also {
                        if (connection.getDbFlavour() == DbFlavour.SQLITE) {
                            it.setProperty("hibernate.type.preferred_uuid_jdbc_type", "VARCHAR")
                        }
                    }
                    .buildSessionFactory()
        }
        return sessionFactory!!
    }

    override fun findHeroById(
        connection: Connection,
        id: UUID,
    ): Any? {
        return getSessionFactory(connection).openSession().use {
            it.find(SuperHeroEntity::class.java, id)
        }
    }

    override fun find100Heroes(connection: Connection): List<Any> {
        return getSessionFactory(connection).openSession().use {
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
        getSessionFactory(connection).openSession().use {
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
        getSessionFactory(connection).openSession().use {
            it.transaction.begin()
            it.merge(SuperHeroEntity(id, name, email, age))
            it.transaction.commit()
        }
    }

    override fun findHeroBattles(
        connection: Connection,
        heroId: UUID,
    ): List<Any> {
        return getSessionFactory(connection).openSession().use {
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
