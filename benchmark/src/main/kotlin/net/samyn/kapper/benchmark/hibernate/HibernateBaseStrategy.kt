package net.samyn.kapper.benchmark.hibernate

import net.samyn.kapper.internal.DbFlavour
import net.samyn.kapper.internal.getDbFlavour
import org.hibernate.SessionFactory
import org.hibernate.cfg.AvailableSettings
import org.hibernate.cfg.Configuration
import java.sql.Connection

abstract class HibernateBaseStrategy {
    private var sessionFactory: SessionFactory? = null

    protected fun getSessionFactory(
        connection: Connection,
        annotatedClasses: List<Class<*>>,
    ): SessionFactory {
        if (sessionFactory == null) {
            HibernateStrategy.SingleConnectionProvider.setConnection(connection)
            val config = Configuration()
            annotatedClasses.forEach { config.addAnnotatedClass(it) }
            config.setProperty(
                AvailableSettings.CONNECTION_PROVIDER,
                HibernateStrategy.SingleConnectionProvider::class.java.name,
            )
            config.setProperty(AvailableSettings.SHOW_SQL, false)
            config.setProperty(AvailableSettings.FORMAT_SQL, false)
            if (connection.getDbFlavour() == DbFlavour.SQLITE) {
                config.setProperty("hibernate.type.preferred_uuid_jdbc_type", "VARCHAR")
            }
            sessionFactory = config.buildSessionFactory()
        }
        return sessionFactory!!
    }
}
