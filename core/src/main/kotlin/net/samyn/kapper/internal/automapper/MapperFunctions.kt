package net.samyn.kapper.internal.automapper

/**
 * Normalises the column name by converting it to lowercase and removing underscores.
 */
fun String?.normalisedColumnName(): String {
    if (this == null) {
        throw IllegalArgumentException("Name cannot be null")
    }
    return this.lowercase()
        .replace("_", "")
        .replace("-", "")
}
