@file:JvmSynthetic

package net.samyn.kapper.internal

import net.samyn.kapper.Field
import net.samyn.kapper.KapperMappingException
import net.samyn.kapper.Mapper
import java.sql.JDBCType
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

class KotlinDataClassMapper<T : Any>(
    private val clazz: Class<T>,
    val autoConverter: (Any, KClass<*>) -> Any = AutoConverter()::convert,
    val sqlTypesConverter: (JDBCType, String, ResultSet, Int, DbFlavour) -> Any? = SQLTypesConverter::convertSQLType,
) : Mapper<T> {
    private val constructor: KFunction<T> =
        clazz.kotlin.primaryConstructor
            ?: throw KapperMappingException("No primary constructor found for ${clazz.name}")
    private val properties =
        constructor.parameters.associateBy { it.name?.lowercase() }

    private fun createInstance(columns: List<ColumnValue>): T {
        if (columns.size > properties.size) {
            throw KapperMappingException(
                "Too many tokens provided in the template: ${columns.map { it.name }}. " +
                    "Constructor for ${clazz.name} only has: ${properties.keys}",
            )
        } else if (columns.size < properties.size) {
            val all = columns.map { it.name.lowercase() }
            val missing = properties.filter { !it.value.isOptional && !all.contains(it.key) }
            if (missing.isNotEmpty()) {
                throw KapperMappingException("The following properties are non-optional and missing: ${missing.keys}")
            }
        }
        val args =
            columns.associate {
                val prop =
                    properties[it.name.lowercase()]
                        ?: throw KapperMappingException("No property found for ${it.name}")
                if (it.value == null) {
                    prop to null
                } else if (it.value::class != prop.type.classifier) {
                    prop to autoConverter(it.value, prop.type.classifier as KClass<*>)
                } else {
                    prop to it.value
                }
            }
        val instance = constructor.callBy(args)
        return instance
    }

    override fun createInstance(
        resultSet: ResultSet,
        fields: Map<String, Field>,
    ): T {
        return createInstance(
            fields.map { field ->
                ColumnValue(
                    field.key,
                    sqlTypesConverter(
                        field.value.type,
                        field.value.typeName,
                        resultSet,
                        field.value.columnIndex,
                        field.value.dbFlavour,
                    ),
                )
            },
        )
    }

    data class ColumnValue(val name: String, val value: Any?)
}
