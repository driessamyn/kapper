@file:JvmSynthetic

package net.samyn.kapper.internal

import net.samyn.kapper.Args
import net.samyn.kapper.DbFlavour
import net.samyn.kapper.KapperParseException
import net.samyn.kapper.internal.automapper.setParameter
import java.sql.PreparedStatement

@PublishedApi
internal fun Args.setParameters(
    query: Query,
    stmt: PreparedStatement,
    dbFlavour: DbFlavour,
) {
    this.forEach { a ->
        val indexes =
            query.tokens[a.key]
                ?: throw KapperParseException("Token with name '${a.key}' not found in template")
        indexes.forEach { i ->
            stmt.setParameter(i, a.value, dbFlavour)
        }
    }
}

/**
 * Extension function for ArgMappers to set parameters on a PreparedStatement using the provided tokens.
 *
 * @param tokens The map of parameter names to their indices in the SQL statement.
 * @param stmt The PreparedStatement to set parameters on.
 * @param obj The object to extract values from.
 * @param dbFlavour The database flavor for type-specific handling.
 */
internal fun <T : Any> Map<String, (T) -> Any?>.setParameters(
    tokens: Map<String, List<Int>>,
    stmt: PreparedStatement,
    obj: T,
    dbFlavour: DbFlavour,
) {
    this.forEach { (key, fn) ->
        val indexes =
            tokens[key]
                ?: throw KapperParseException("Token with name '$key' not found in template")
        val value = fn(obj)
        indexes.forEach { i ->
            stmt.setParameter(i, value, dbFlavour)
        }
    }
}
