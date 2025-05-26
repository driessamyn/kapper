@file:JvmSynthetic

package net.samyn.kapper.internal.automapper

import net.samyn.kapper.Field
import net.samyn.kapper.KapperMappingException
import net.samyn.kapper.Mapper
import net.samyn.kapper.internal.AutoConverter
import net.samyn.kapper.internal.autoConverter
import java.lang.reflect.Modifier.isPublic
import java.sql.ResultSet

/**
 * Automatically map database records to Java Record classes.
 */
class RecordMapper<T : Any>(
    private val clazz: Class<T>,
    private val typesConverter: AutoConverter = autoConverter,
    private val fieldsConverter: FieldsConverter = FieldsConverter(),
) : Mapper<T> {
    init {
        if (!isPublic(clazz.modifiers)) {
            throw KapperMappingException(
                "Cannot map to non-public record class: ${clazz.name}. " +
                    "Record classes must be public for automatic mapping.",
            )
        }
    }

    private val recordComponents = clazz.recordComponents

    private fun createInstance(columns: List<ColumnValue>): T {
        val args = arrayOfNulls<Any>(recordComponents.size)
        val normalisedColumns = columns.associateBy { it.name.normalisedColumnName() }
        val missing = mutableListOf<String>()
        for ((idx, rc) in recordComponents.withIndex()) {
            val name = rc.name.normalisedColumnName()
            if (!normalisedColumns.containsKey(name)) {
                if (rc.type.isPrimitive) {
                    missing.add(name)
                }
                continue
            }
            val value = normalisedColumns[name]?.value
            args[idx] =
                if (value == null) {
                    if (rc.type.isPrimitive) {
                        throw KapperMappingException("The record component '$name' cannot be null.")
                    }
                    null
                } else if (!rc.type.isInstance(value)) {
                    typesConverter.convert(value, rc.type)
                } else {
                    value
                }
        }
        if (missing.isNotEmpty()) {
            throw KapperMappingException("The following non-nullable record components are missing: $missing")
        }
        val canonical = clazz.getDeclaredConstructor(*recordComponents.map { it.type }.toTypedArray())
        return canonical.newInstance(*args)
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
