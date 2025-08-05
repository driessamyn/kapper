@file:JvmSynthetic

package net.samyn.kapper.internal.automapper

import net.samyn.kapper.Field
import net.samyn.kapper.KapperMappingException
import net.samyn.kapper.Mapper
import net.samyn.kapper.internal.AutoConverter
import net.samyn.kapper.internal.autoConverter
import java.sql.ResultSet
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

private data class ReflectionData<T : Any>(
    val constructor: KFunction<T>,
    val properties: Map<String, PropertyData<T>>,
)

private data class PropertyData<T : Any>(
    val kProp: KParameter,
    val isOptional: Boolean,
    val type: KClass<T>,
)

private val reflectionCache = ConcurrentHashMap<Class<*>, ReflectionData<Any>>()

/**
 * Automatically map database records to Kotlin data classes.
 */
class KotlinDataClassMapper<T : Any>(
    clazz: Class<T>,
    private val typesConverter: AutoConverter = autoConverter,
    private val fieldsConverter: FieldsConverter = FieldsConverter(),
) : Mapper<T> {
    // Cast needed due to type erasure
    @Suppress("UNCHECKED_CAST")
    private val reflectionData: ReflectionData<T> =
        reflectionCache.computeIfAbsent(clazz) {
            val constructor =
                it.kotlin.primaryConstructor
                    ?: throw KapperMappingException("No primary constructor found for ${it.name}")
            val properties =
                constructor.parameters.associate { p ->
                    p.name.normalisedColumnName() to
                        PropertyData(
                            p,
                            p.isOptional,
                            p.type.classifier as KClass<T>,
                        )
                } as Map<String, PropertyData<Any>>
            ReflectionData(constructor, properties)
        } as ReflectionData<T>

    private fun createInstance(columns: List<ColumnValue>): T {
        val args = mutableMapOf<KParameter, Any?>()
        val missing = mutableListOf<String>()
        val normalisedColumns = columns.associateBy { it.name.normalisedColumnName() }
        for ((name, prop) in reflectionData.properties) {
            if (!normalisedColumns.containsKey(name)) {
                if (!prop.isOptional) missing.add(name)
                continue
            }
            val value = normalisedColumns[name]?.value
            args[prop.kProp] =
                when {
                    value == null -> null
                    value::class != prop.type -> {
                        typesConverter.convert(value, prop.type.java)
                    }
                    else -> value
                }
        }
        if (missing.isNotEmpty()) {
            throw KapperMappingException("The following properties are non-optional and missing: $missing")
        }
        return reflectionData.constructor.callBy(args)
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
