package net.samyn.kapper

import java.sql.PreparedStatement

/**
 * Represents a dynamic set of parameters for a SQL query or statement.
 */
class DynamicParameters(vararg params: Pair<String, Any?>) {
    private val parameterMap = params.toMap()

    /**
     * Bind the parameters to a prepared statement.
     *
     * @param statement The prepared statement to bind the parameters to.
     */
    fun bindParameters(statement: PreparedStatement) {
//        parameterMap.forEach { (name, value) ->
//            val index = statement.metaData.getColumnIndex(name)
//            when (value) {
//                null -> statement.setNull(index, Types.NULL)
//                else -> statement.setObject(index, value)
//            }
//        }
    }

    /**
     * Create DynamicParameters from a Kotlin data class instance.
     *
     * @param obj The Kotlin data class instance to create DynamicParameters from.
     * @return DynamicParameters populated with the properties of the data class instance.
     */
    companion object {
        inline fun <reified T : Any> from(obj: T): DynamicParameters {
//            return DynamicParameters(*T::class.memberProperties
//                .map { it.name to it.get(obj) }
//                .toTypedArray()
//            )
            TODO()
        }
    }
}
