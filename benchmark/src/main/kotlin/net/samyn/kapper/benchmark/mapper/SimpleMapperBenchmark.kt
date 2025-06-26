package net.samyn.kapper.benchmark.mapper

import net.samyn.kapper.DbFlavour
import net.samyn.kapper.Field
import net.samyn.kapper.Mapper
import net.samyn.kapper.benchmark.SimpleRecord
import net.samyn.kapper.internal.automapper.KotlinDataClassMapper
import net.samyn.kapper.internal.automapper.RecordMapper
import java.sql.JDBCType
import java.sql.ResultSet
import java.util.UUID

abstract class AbstractSimpleMapperBenchmark<T : Any>(
    override val numberOfResults: Int,
    private val mapper: Mapper<T>,
) : MapperBenchmark {
    private val fields: Map<String, Field> =
        mapOf(
            "id" to Field(1, JDBCType.OTHER, "UUID", DbFlavour.UNKNOWN),
            "name" to Field(2, JDBCType.VARCHAR, "VARCHAR", DbFlavour.UNKNOWN),
            "email" to Field(3, JDBCType.VARCHAR, "VARCHAR", DbFlavour.UNKNOWN),
            "age" to Field(4, JDBCType.INTEGER, "INTEGER", DbFlavour.UNKNOWN),
        )

    override fun map(): List<Any> =
        ResultSetStub(numberOfResults, uuidColumns = setOf(1)).use { rs ->
            generateSequence { if (rs.next()) mapper.createInstance(rs, fields) else null }
                .toList()
        }
}

class SimpleRecordMapperBenchmark(override val numberOfResults: Int) :
    AbstractSimpleMapperBenchmark<SimpleRecord>(numberOfResults, RecordMapper(SimpleRecord::class.java))

class SimpleDataClassMapperBenchmark(override val numberOfResults: Int) :
    AbstractSimpleMapperBenchmark<SimpleDataClass>(numberOfResults, KotlinDataClassMapper(SimpleDataClass::class.java))

class SimpleCustomMapperBenchmark(override val numberOfResults: Int) :
    AbstractSimpleMapperBenchmark<SimpleDataClass>(numberOfResults, CustomSimpleMapper())

data class SimpleDataClass(
    val id: UUID,
    val name: String,
    val email: String?,
    val age: Int?,
)

class CustomSimpleMapper : Mapper<SimpleDataClass> {
    override fun createInstance(
        resultSet: ResultSet,
        fields: Map<String, Field>,
    ): SimpleDataClass =
        SimpleDataClass(
            id = UUID.fromString(resultSet.getString(1)),
            name = resultSet.getString(2)!!,
            email = resultSet.getString(3),
            age = resultSet.getInt(4),
        )
}
