package net.samyn.kapper.example.kotlin.hibernate

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import net.samyn.kapper.example.kotlin.PopularMovie
import net.samyn.kapper.example.kotlin.kapper.SuperHeroRepository.DbType
import org.hibernate.Hibernate
import org.hibernate.SessionFactory
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.cfg.AvailableSettings
import org.hibernate.cfg.Configuration
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "super_heroes")
class SuperHeroEntity(
    @Id
    @Column(name = "id")
    var id: UUID,
    @Column(name = "name")
    var name: String,
    @Column(name = "email")
    var email: String?,
    @Column(name = "age")
    var age: Int?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as SuperHeroEntity

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}

@Entity
@Table(name = "villains")
class VillainEntity(
    @Id
    @Column(name = "id")
    var id: UUID,
    @Column(name = "name")
    var name: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as VillainEntity

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}

@Entity
@Table(name = "battles")
class SuperHeroBattleEntity(
    @Id
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.MERGE])
    @JoinColumn(name = "super_hero_id")
    var superHero: SuperHeroEntity,
    @Id
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.MERGE])
    @JoinColumn(name = "villain_id")
    var villain: VillainEntity,
    @Id
    @Column(name = "battle_date")
    var date: LocalDateTime,
) {
    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_ts")
    val updateTimestamp: Instant? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as SuperHeroBattleEntity

        return superHero == other.superHero && villain == other.villain
    }

    override fun hashCode(): Int = javaClass.hashCode()
}

public class SuperHeroHibernateQueries(
    private val jdbcUser: String,
    private val jdbcPassword: String,
    private val jdbcUrl: String,
) {
    private val sessionFactory: SessionFactory =
        Configuration()
            .addAnnotatedClass(SuperHeroEntity::class.java)
            .addAnnotatedClass(VillainEntity::class.java)
            .addAnnotatedClass(SuperHeroBattleEntity::class.java)
            .setProperty(AvailableSettings.JAKARTA_JDBC_URL, jdbcUrl)
            .setProperty(AvailableSettings.JAKARTA_JDBC_USER, jdbcUser)
            .setProperty(AvailableSettings.JAKARTA_JDBC_PASSWORD, jdbcPassword) // Automatic schema export
            .setProperty(AvailableSettings.SHOW_SQL, true)
            .setProperty(AvailableSettings.FORMAT_SQL, true)
            .also {
                if (jdbcUrl.contains(DbType.MYSQL.name, ignoreCase = true)) {
                    // glad I had Claud AI to help me with this little bit of "magic" :(
                    it.setProperty("hibernate.type.preferred_uuid_jdbc_type", "VARCHAR")
                }
            }.buildSessionFactory()

    fun list(): List<SuperHeroEntity> =
        sessionFactory.openSession().use {
            it.createQuery("FROM SuperHeroEntity", SuperHeroEntity::class.java).list()
        }

    // Find a superhero by ID
    fun findById(id: UUID) =
        sessionFactory.openSession().use {
            it.find(SuperHeroEntity::class.java, id) as SuperHeroEntity
        }

    // Find a superhero by name
    fun findByName(name: String) =
        sessionFactory.openSession().use {
            it
                .createSelectionQuery(
                    "FROM SuperHeroEntity WHERE name = :name",
                    SuperHeroEntity::class.java,
                ).setParameter("name", name)
                .uniqueResult()
        }

    // Find battles involving a specific superhero
    fun findBattles(superHeroName: String) =
        sessionFactory.openSession().use {
            it
                .createSelectionQuery(
                    """
            SELECT b FROM SuperHeroBattleEntity b 
            WHERE b.superHero.name = :name
        """,
                    SuperHeroBattleEntity::class.java,
                ).setParameter("name", superHeroName)
                .list()
        }

    // Insert a new superhero
    fun insertHero(superHero: SuperHeroEntity) =
        sessionFactory.openSession().use {
            it.transaction.begin()
            it.merge(superHero)
            it.transaction.commit()
        }

    // Insert a new battle involving a superhero and a villain
    fun insertBattle(
        superHero: SuperHeroEntity,
        villain: VillainEntity,
        date: LocalDateTime,
    ) = sessionFactory.openSession().use { session ->
        // Thanks Claude AI for the help with this :)
        try {
            session.transaction.begin()
            val managedHero = session.merge(superHero)
            val managedVillain = session.merge(villain)
            val battle = SuperHeroBattleEntity(managedHero, managedVillain, date)
            session.persist(battle)

            session.transaction.commit()
        } catch (e: Exception) {
            session.transaction.rollback()
            throw e
        }
    }

    fun findPopularMovies(): List<PopularMovie> =
        sessionFactory.openSession().use {
            var allTimeRank = 1
            return it
                .createNativeQuery(
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
                    Array<Any>::class.java,
                ).resultList
                .map { row ->
                    val result = row as Array<*>
                    PopularMovie(
                        result[0] as String,
                        result[2] as Long,
                        (result[2] as Long).toDouble() / (result[3] as BigDecimal).toDouble(),
                        allTimeRank++,
                    )
                }
        }
}
