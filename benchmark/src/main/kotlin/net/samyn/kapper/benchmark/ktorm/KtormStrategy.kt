package net.samyn.kapper.benchmark.ktorm

import net.samyn.kapper.DbFlavour
import net.samyn.kapper.benchmark.BenchmarkStrategy
import net.samyn.kapper.benchmark.ISuperHero
import net.samyn.kapper.internal.getDbFlavour
import org.ktorm.database.Database
import org.ktorm.dsl.QueryRowSet
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.innerJoin
import org.ktorm.dsl.insert
import org.ktorm.dsl.limit
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.dsl.update
import org.ktorm.dsl.where
import org.ktorm.schema.SqlType
import org.ktorm.schema.Table
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import org.ktorm.support.postgresql.PostgreSqlDialect
import org.ktorm.support.sqlite.SQLiteDialect
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDateTime
import java.util.UUID

data class SuperHero(override val id: UUID, override val name: String, override val email: String?, override val age: Int?) : ISuperHero

data class SuperHeroBattle(val superhero: String, val villain: String, val date: LocalDateTime)

class KtormStrategy : BenchmarkStrategy {
    private var database: Database? = null
    private lateinit var superHeroes: SuperheroesTable
    private lateinit var villains: VillainsTable
    private lateinit var battles: Battles

    private fun getDatabase(connection: Connection) =
        // similar hack to Hibernate so we can re-use the connection
        database ?: run {
            val dbType = connection.getDbFlavour()
            superHeroes = SuperheroesTable(dbType)
            villains = VillainsTable(dbType)
            battles = Battles(dbType)
            Database.connect(
                dialect =
                    when (dbType) {
                        DbFlavour.SQLITE -> SQLiteDialect()
                        DbFlavour.POSTGRESQL -> PostgreSqlDialect()
                        else -> throw IllegalStateException("Unsupported database type")
                    },
            ) {
                object : Connection by connection {
                    override fun close() {
                        // Override the close function and do nothing, keep the connection open.
                    }
                }
            }
        }

    class CustomUUIDSqlType(
        private val dbType: DbFlavour,
    ) : SqlType<UUID>(Types.VARCHAR, "custom_uuid") {
        override fun doSetParameter(
            ps: PreparedStatement,
            index: Int,
            parameter: UUID,
        ) {
            if (dbType == DbFlavour.SQLITE) {
                ps.setString(index, parameter.toString())
            } else {
                ps.setObject(index, parameter)
            }
        }

        override fun doGetResult(
            rs: ResultSet,
            index: Int,
        ): UUID? {
            return if (dbType == DbFlavour.SQLITE) {
                UUID.fromString(rs.getString(index))
            } else {
                rs.getObject(index) as UUID?
            }
        }
    }

    // mappings
    class SuperheroesTable(dbType: DbFlavour) : Table<Nothing>("super_heroes") {
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

    class VillainsTable(dbType: DbFlavour) : Table<Nothing>("villains") {
        val id = registerColumn("id", CustomUUIDSqlType(dbType)).primaryKey()
        val name = varchar("name")
    }

    class Battles(dbType: DbFlavour) : Table<Nothing>("battles") {
        val superHeroId = registerColumn("super_hero_id", CustomUUIDSqlType(dbType)).primaryKey()
        val villainId = registerColumn("villain_id", CustomUUIDSqlType(dbType)).primaryKey()
        val battleDate = datetime("battle_date")
    }

    override fun findHeroById(
        connection: Connection,
        id: UUID,
    ): Any? =
        getDatabase(connection)
            .from(superHeroes)
            .select()
            .where { superHeroes.id eq id }
            .map { row ->
                superHeroes.toDomain(row)
            }.firstOrNull()

    override fun find100Heroes(connection: Connection): List<Any> =
        getDatabase(connection).from(superHeroes).select().limit(100).map { row ->
            superHeroes.toDomain(row)
        }

    override fun insertNewHero(
        connection: Connection,
        id: UUID,
        name: String,
        email: String,
        age: Int,
    ) {
        getDatabase(connection).insert(superHeroes) {
            set(superHeroes.id, id)
            set(superHeroes.name, name)
            set(superHeroes.email, email)
            set(superHeroes.age, age)
        }
    }

    override fun changeHero(
        connection: Connection,
        id: UUID,
        name: String,
        email: String,
        age: Int,
    ) {
        getDatabase(connection).update(superHeroes) {
            set(superHeroes.name, name)
            set(superHeroes.email, email)
            set(superHeroes.age, age)
            where { superHeroes.id eq id }
        }
    }

    override fun findHeroBattles(
        connection: Connection,
        heroId: UUID,
    ) = getDatabase(connection)
        .from(superHeroes)
        .innerJoin(battles, on = superHeroes.id eq battles.superHeroId)
        .innerJoin(villains, on = battles.villainId eq villains.id)
        .select()
        .where { superHeroes.id eq heroId }
        .map {
            SuperHeroBattle(
                it[superHeroes.name] as String,
                it[villains.name] as String,
                it[battles.battleDate] as LocalDateTime,
            )
        }
}
