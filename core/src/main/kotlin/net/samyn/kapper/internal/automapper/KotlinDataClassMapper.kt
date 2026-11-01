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
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType

private data class ReflectionData<T : Any>(
    val constructor: KFunction<T>,
    val properties: Map<String, PropertyData<T>>,
)

private data class PropertyData<T : Any>(
    val kProp: KParameter,
    val isOptional: Boolean,
    val type: KClass<T>,
    val javaType: Class<*> = type.javaObjectType,
    val listElementType: KClass<*>? = null,
)

/**
 * True if [type] is (or is a subtype of) [kotlin.collections.List].
 */
private fun KClass<*>.isList() = this == List::class || this.starProjectedType.run { isSubtypeOf(List::class.starProjectedType) }

/**
 * Returns the element type class of a [List] property type, or null if the type is not a list.
 * e.g. `List<Int>` returns `Int::class`, `List<Int?>` returns `Int::class`.
 */
private fun KType.listElementClass(): KClass<*>? {
    val classifier = this.classifier as? KClass<*> ?: return null
    if (!classifier.isList()) return null
    val elementType = this.arguments.firstOrNull()?.type ?: return null
    // unwrap nullable element types like Int?
    return elementType.classifier as? KClass<*>
}

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
                            listElementType = p.type.listElementClass(),
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
                    // List<Long> from an INT8[] column must be coerced to List<Int> when the
                    // target property is List<Int> (erasure means List.isInstance() passes).
                    prop.listElementType != null && value is List<*> -> coerceListElements(value, prop.listElementType)
                    !prop.javaType.isInstance(value) -> {
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

    private fun coerceListElements(
        value: List<*>,
        elementType: KClass<*>,
    ): List<*> {
        // Fast path: when every element already matches the declared element type, keep the
        // original list, preserving its concrete type (e.g. MutableList) rather than always
        // rebuilding it as an ArrayList.
        if (value.all { it == null || elementType.isInstance(it) }) return value
        return value.map { element ->
            when {
                element == null -> null
                elementType.isInstance(element) -> element
                else -> typesConverter.convert(element, elementType.java)
            }
        }
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
