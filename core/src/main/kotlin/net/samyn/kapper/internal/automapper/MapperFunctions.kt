@file:JvmSynthetic

package net.samyn.kapper.internal.automapper

import net.samyn.kapper.Field

/**
 * Normalises the column name by converting it to lowercase and removing underscores.
 */
fun String?.normalisedColumnName(): String {
    requireNotNull(this) { "Name cannot be null" }
    return this.lowercase()
        .replace("_", "")
        .replace("-", "")
}

/**
 * Renders the columns returned by a query (with their JDBC types) for use in mapping error messages.
 */
internal fun describeColumns(fields: Map<String, Field>): String =
    if (fields.isEmpty()) {
        "none"
    } else {
        fields.entries.joinToString(", ") { (name, field) -> "$name (${field.type})" }
    }
