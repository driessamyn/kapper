@file:JvmSynthetic

package net.samyn.kapper.internal.automapper

import net.samyn.kapper.Field
import net.samyn.kapper.KapperMappingException
import net.samyn.kapper.Mapper
import net.samyn.kapper.internal.AutoConverter
import net.samyn.kapper.internal.autoConverter
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

/**
 * Automatically map database records to Kotlin data classes.
 */
class KotlinDataClassMapper<T : Any>(
    clazz: Class<T>,
    private val typesConverter: AutoConverter = autoConverter,
    private val fieldsConverter: FieldsConverter = FieldsConverter(),
) : Mapper<T> {
    private val constructor: KFunction<T> =
        clazz.kotlin.primaryConstructor
            ?: throw KapperMappingException("No primary constructor found for ${clazz.name}")
    private val properties =
        constructor.parameters.associateBy { it.name.normalisedColumnName() }

    private fun createInstance(columns: List<ColumnValue>): T {
        val args = mutableMapOf<kotlin.reflect.KParameter, Any?>()
        val missing = mutableListOf<String>()
        val normalisedColumns = columns.associateBy { it.name.normalisedColumnName() }
        for ((name, prop) in properties) {
            if (!normalisedColumns.containsKey(name)) {
                if (!prop.isOptional) missing.add(name)
                continue
            }
            val value = normalisedColumns[name]?.value
            args[prop] =
                when {
                    value == null -> null
                    value::class != prop.type.classifier -> {
                        typesConverter.convert(value, (prop.type.classifier as KClass<*>).java)
                    }
                    else -> value
                }
        }
        if (missing.isNotEmpty()) {
            throw KapperMappingException("The following properties are non-optional and missing: $missing")
        }
        return constructor.callBy(args)
    }

    override fun createInstance(
        resultSet: ResultSet,
        fields: Map<String, Field>,
    ): T {
        return createInstance(
            fieldsConverter.convert(resultSet, fields),
        )
    }
}
