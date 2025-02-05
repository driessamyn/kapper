package net.samyn.kapper.internal

import net.samyn.kapper.Args
import net.samyn.kapper.KapperParseException
import net.samyn.kapper.internal.DbConnectionUtils.getDbFlavour
import net.samyn.kapper.internal.SQLTypesConverter.setParameter
import java.sql.Connection
import java.sql.PreparedStatement

internal fun Args.setParameters(
    query: Query,
    stmt: PreparedStatement,
    connection: Connection,
) {
    this.forEach { a ->
        val indexes =
            query.tokens[a.key]
                ?: throw KapperParseException("Token with name '${a.key}' not found in template")
        indexes.forEach { i ->
            stmt.setParameter(i, a.value, connection.getDbFlavour())
        }
    }
}
