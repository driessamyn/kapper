package net.samyn.kapper.internal.automapper

import net.samyn.kapper.Field
import java.sql.ResultSet

class FieldsConverter(
    private val converter: SQLTypesConverter = sqlTypesConverter,
) {
    fun convert(
        resultSet: ResultSet,
        fields: Map<String, Field>,
    ) = fields.map { field ->
        ColumnValue(
            field.key,
            converter.convert(
                field.value,
                resultSet,
            ),
        )
    }
}

data class ColumnValue(val name: String, val value: Any?)
