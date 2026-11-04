@file:JvmSynthetic

package net.samyn.kapper.internal.automapper

import net.samyn.kapper.Field
import net.samyn.kapper.KapperMappingException
import net.samyn.kapper.Mapper
import net.samyn.kapper.internal.AutoConverter
import net.samyn.kapper.internal.ColumnContext
import net.samyn.kapper.internal.autoConverter
import net.samyn.kapper.internal.convert
import java.lang.reflect.Constructor
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

    // Resolved once per mapper instance rather than on every row.
    private val canonicalConstructor: Constructor<T> =
        clazz.getDeclaredConstructor(*recordComponents.map { it.type }.toTypedArray()).also {
            it.isAccessible = true
        }

    private fun createInstance(
        columns: List<ColumnValue>,
        fields: Map<String, Field>,
    ): T {
        val args = arrayOfNulls<Any>(recordComponents.size)
        val normalisedColumns = columns.associateBy { it.name.normalisedColumnName() }
        val missing = mutableListOf<String>()
        for ((idx, rc) in recordComponents.withIndex()) {
            val name = rc.name.normalisedColumnName()
            val column = normalisedColumns[name]
            if (column == null) {
                if (rc.type.isPrimitive) {
                    missing.add(name)
                }
                continue
            }
            val value = column.value
            args[idx] =
                if (value == null) {
                    if (rc.type.isPrimitive) {
                        throw KapperMappingException(
                            "Cannot map to ${clazz.name}: record component '$name' is a primitive and cannot be null. " +
                                "Columns returned: ${describeColumns(fields)}.",
                        )
                    }
                    null
                } else if (!rc.type.isInstance(value)) {
                    typesConverter.convert(value, rc.type, ColumnContext(column.name, fields[column.name]))
                } else {
                    value
                }
        }
        if (missing.isNotEmpty()) {
            throw KapperMappingException(mappingFailureMessage(missing, fields))
        }
        return canonicalConstructor.newInstance(*args)
    }

    private fun mappingFailureMessage(
        missing: List<String>,
        fields: Map<String, Field>,
    ): String =
        buildString {
            append("Cannot map to ${clazz.name}: ")
            append("the following non-nullable record components are missing: $missing. ")
            append("Columns returned: ${describeColumns(fields)}. ")
            append("Expected record components: ${recordComponents.map { it.name }}. ")
            append("Hint: alias the columns (e.g. SELECT full_name AS name), or register a custom mapper.")
        }

    override fun createInstance(
        resultSet: ResultSet,
        fields: Map<String, Field>,
    ): T {
        return createInstance(
            fieldsConverter.convert(resultSet, fields),
            fields,
        )
    }
}
