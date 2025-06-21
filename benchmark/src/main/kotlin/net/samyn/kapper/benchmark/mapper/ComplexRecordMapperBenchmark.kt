package net.samyn.kapper.benchmark.mapper

import net.samyn.kapper.DbFlavour
import net.samyn.kapper.Field
import net.samyn.kapper.benchmark.ComplexRecord
import net.samyn.kapper.internal.automapper.KotlinDataClassMapper
import net.samyn.kapper.internal.automapper.RecordMapper
import java.sql.JDBCType
import java.util.UUID

abstract class AbstractComplexMapperBenchmark<T : Any>(
    override val numberOfResults: Int,
    private val mapper: net.samyn.kapper.Mapper<T>,
    private val nullableColumns: Set<Int> = setOf(2, 4, 6, 8, 10),
) : MapperBenchmark {
    private val fields: Map<String, Field> =
        (1..25).associate { i ->
            val name = "field$i"
            val jdbcType =
                when (i % 5) {
                    0 -> JDBCType.DOUBLE
                    1 -> JDBCType.VARCHAR
                    2 -> JDBCType.INTEGER
                    3 -> JDBCType.OTHER
                    else -> JDBCType.BOOLEAN
                }
            name to
                Field(
                    i,
                    jdbcType,
                    if (jdbcType == JDBCType.OTHER) "uuid" else jdbcType.name,
                    DbFlavour.UNKNOWN,
                )
        }

    override fun map(): List<Any> =
        ResultSetStub(
            numberOfResults,
            nullableColumns = nullableColumns,
            uuidColumns = fields.filter { it.value.typeName == "uuid" }.map { it.value.columnIndex }.toSet(),
        ).use { rs ->
            generateSequence { if (rs.next()) mapper.createInstance(rs, fields) else null }
                .toList()
        }
}

class ComplexRecordMapperBenchmark(override val numberOfResults: Int) :
    AbstractComplexMapperBenchmark<ComplexRecord>(numberOfResults, RecordMapper(ComplexRecord::class.java))

class ComplexDataClassMapperBenchmark(override val numberOfResults: Int) :
    AbstractComplexMapperBenchmark<ComplexDataClass>(numberOfResults, KotlinDataClassMapper(ComplexDataClass::class.java))

data class ComplexDataClass(
    val field1: String,
    val field2: Int?,
    val field3: UUID?,
    val field4: Boolean?,
    val field5: Double?,
    val field6: String?,
    val field7: Int?,
    val field8: UUID?,
    val field9: Boolean?,
    val field10: Double?,
    val field11: String?,
    val field12: Int?,
    val field13: UUID?,
    val field14: Boolean?,
    val field15: Double?,
    val field16: String?,
    val field17: Int?,
    val field18: UUID?,
    val field19: Boolean?,
    val field20: Double?,
    val field21: String?,
    val field22: Int?,
    val field23: UUID?,
    val field24: Boolean?,
    val field25: Double?,
)
