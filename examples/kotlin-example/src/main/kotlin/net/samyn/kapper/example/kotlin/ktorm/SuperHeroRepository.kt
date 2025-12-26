package net.samyn.kapper.example.kotlin.ktorm

import net.samyn.kapper.example.kotlin.PopularMovie
import net.samyn.kapper.example.kotlin.SuperHero
import net.samyn.kapper.example.kotlin.SuperHeroBattle
import net.samyn.kapper.example.kotlin.Villain
import org.ktorm.database.Database
import org.ktorm.dsl.AssignmentsBuilder
import org.ktorm.dsl.QueryRowSet
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.innerJoin
import org.ktorm.dsl.insert
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.ktorm.schema.BaseTable
import org.ktorm.schema.SqlType
import org.ktorm.schema.Table
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.timestamp
import org.ktorm.schema.varchar
import org.ktorm.support.mysql.MySqlDialect
import org.ktorm.support.postgresql.PostgreSqlDialect
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import org.ktorm.support.mysql.insertOrUpdate as mySqlInsertOrUpdate
import org.ktorm.support.postgresql.insertOrUpdate as postgresqlInsertOrUpdate

class SuperHeroRepository(
    private val dataSource: DataSource,
) {
    private val database: Database
    private val dbType: DbType

    init {
        dbType =
            dataSource.connection.use {
                when (it.metaData.databaseProductName) {
                    "MySQL" -> DbType.MYSQL
                    else -> DbType.POSTGRESQL
                }
            }
        database =
            Database.connect(
                dataSource,
                dialect =
                    when (dbType) {
                        DbType.MYSQL -> MySqlDialect()
                        DbType.POSTGRESQL -> PostgreSqlDialect()
                    },
            )
    }

    enum class DbType {
        POSTGRESQL,
        MYSQL,
    }

    // to support mysql exception
    class CustomUUIDSqlType(
        private val dbType: DbType,
    ) : SqlType<UUID>(Types.VARCHAR, "custom_uuid") {
        override fun doSetParameter(
            ps: PreparedStatement,
            index: Int,
            parameter: UUID,
        ) {
            if (dbType == DbType.MYSQL) {
                ps.setString(index, parameter.toString())
            } else {
                ps.setObject(index, parameter)
            }
        }

        override fun doGetResult(
            rs: ResultSet,
            index: Int,
        ): UUID? =
            if (dbType == DbType.MYSQL) {
                UUID.fromString(rs.getString(index))
            } else {
                rs.getObject(index) as UUID?
            }
    }

    // mappings
    class SuperheroesTable(
        dbType: DbType,
    ) : Table<Nothing>("super_heroes") {
        val id = registerColumn("id", CustomUUIDSqlType(dbType)).primaryKey()
        val name = varchar("name")
        val email = varchar("email")
        val age = int("age")

        fun toDomain(row: QueryRowSet) =
            SuperHero(
                row[id] as UUID,
                row[name] as String,
                row[email],
                row[age],
            )
    }

    private val superHeroes = SuperheroesTable(dbType)

    class VillainsTable(
        dbType: DbType,
    ) : Table<Nothing>("villains") {
        val id = registerColumn("id", CustomUUIDSqlType(dbType)).primaryKey()
        val name = varchar("name")
    }

    private val villains = VillainsTable(dbType)

    class Battles(
        dbType: DbType,
    ) : Table<Nothing>("battles") {
        val superHeroId = registerColumn("super_hero_id", CustomUUIDSqlType(dbType)).primaryKey()
        val villainId = registerColumn("villain_id", CustomUUIDSqlType(dbType)).primaryKey()
        val battleDate = datetime("battle_date")
        val updatedTs = timestamp("updated_ts")
    }

    private val battles = Battles(dbType)

    // List all superheroes
    fun list(): List<SuperHero> =
        database
            .from(superHeroes)
            .select()
            .map(superHeroes::toDomain)

    // Find a superhero by ID
    fun findById(id: UUID) =
        database
            .from(superHeroes)
            .select()
            .where { superHeroes.id eq id }
            .map(superHeroes::toDomain)
            .firstOrNull()

    // Find a superhero by name
    fun findByName(name: String) =
        database
            .from(superHeroes)
            .select()
            .where { superHeroes.name eq name }
            .map(superHeroes::toDomain)
            .firstOrNull()

    // Find battles involving a specific superhero
    fun findBattles(superHeroName: String) =
        database
            .from(superHeroes)
            .innerJoin(battles, on = superHeroes.id eq battles.superHeroId)
            .innerJoin(villains, on = battles.villainId eq villains.id)
            .select()
            .where { superHeroes.name eq superHeroName }
            .map {
                SuperHeroBattle(
                    it[superHeroes.name] as String,
                    it[villains.name] as String,
                    it[battles.battleDate] as LocalDateTime,
                )
            }

    // Insert a new superhero
    fun insertHero(superHero: SuperHero) =
        database.insert(superHeroes) {
            set(it.id, superHero.id)
            set(it.name, superHero.name)
            set(it.email, superHero.email)
            set(it.age, superHero.age)
        }

    // Insert a new battle involving a superhero and a villain
    private fun <T : BaseTable<*>> Database.dbDependentInsertOrUpdate(
        table: T,
        block: AssignmentsBuilder.(T) -> Unit,
        onDuplicateBlock: AssignmentsBuilder.(T) -> Unit,
    ) {
        when (dbType) {
            DbType.MYSQL -> {
                this.mySqlInsertOrUpdate(table) {
                    block(this, table)
                    onDuplicateKey {
                        onDuplicateBlock(this, table)
                    }
                }
            }
            DbType.POSTGRESQL -> {
                this.postgresqlInsertOrUpdate(table) {
                    block(this, table)
                    onConflict {
                        doNothing()
                    }
                }
            }
        }
    }

    fun insertBattle(
        superHero: SuperHero,
        villain: Villain,
        date: LocalDateTime,
    ) = database.useTransaction {
        database.dbDependentInsertOrUpdate(superHeroes, {
            set(it.id, superHero.id)
            set(it.name, superHero.name)
            set(it.email, superHero.email)
            set(it.age, superHero.age)
        }) {
            set(it.id, superHero.id)
        }
        database.dbDependentInsertOrUpdate(villains, {
            set(it.id, villain.id)
            set(it.name, villain.name)
        }) {
            set(it.id, villain.id)
        }
        database.insert(battles) {
            set(it.superHeroId, superHero.id)
            set(it.villainId, villain.id)
            set(it.battleDate, date)
            set(it.updatedTs, Instant.now())
        }
    }

    fun findPopularMovies(): List<PopularMovie> =
//        database.from(PopularMovies)
//            .select(
//                PopularMovies.title,
//                PopularMovies.releaseDate,
//                PopularMovies.grossWorldwide,
//                avg(PopularMovies.grossWorldwide).over().aliased("total_average_gross"),
//                avg(PopularMovies.grossWorldwide)
//                    .over {
//                        window().partitionBy(
//                            // Cannot work this out, so abandoning the attempt
//                            // ??("EXTRACT(YEAR FROM ${PopularMovies.releaseDate.name})")
//                        )
//                    }
//                    .aliased("average_annual_gross")
//            ).map {
//                PopularMovie(
//                    it[PopularMovies.title] as String,
//                    it[PopularMovies.grossWorldwide] as Long,
//                    it["total_average_gross"] as Double,
//                    it["average_annual_gross"] as Double
//                )
//            }
        database.useConnection { connection ->
            // falling back on native JDBC (or we could have used Kapper ;) )
            connection
                .prepareStatement(
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
                ).use { statement ->
                    statement.executeQuery().use { rs ->
                        val result = mutableListOf<PopularMovie>()
                        while (rs.next()) {
                            result.add(
                                PopularMovie(
                                    rs.getString("title"),
                                    rs.getLong("gross_worldwide"),
                                    rs.getDouble("average_annual_gross"),
                                    rs.getInt("total_average_gross"),
                                ),
                            )
                        }
                        result
                    }
                }
        }
}
