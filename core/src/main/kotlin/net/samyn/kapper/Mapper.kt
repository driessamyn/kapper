package net.samyn.kapper

import java.sql.ResultSet

fun interface Mapper<T : Any> {
    fun createInstance(
        resultSet: ResultSet,
        fields: Map<String, Field>,
    ): T
}
