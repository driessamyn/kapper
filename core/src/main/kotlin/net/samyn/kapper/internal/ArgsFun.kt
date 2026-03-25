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
): () -> Unit {
    val cleanups = mutableListOf<() -> Unit>()
    try {
        this.forEach { a ->
            val indexes =
                query.tokens[a.key]
                    ?: throw KapperParseException("Token with name '${a.key}' not found in template")
            indexes.forEach { i ->
                cleanups.add(stmt.setParameter(i, a.value, dbFlavour))
            }
        }
    } catch (e: Exception) {
        cleanups.forEach { it() }
        throw e
    }
    return { cleanups.forEach { it() } }
}

/**
 * Extension function for ArgMappers to set parameters on a PreparedStatement using the provided tokens.
 *
 * @param tokens The map of parameter names to their indices in the SQL statement.
 * @param stmt The PreparedStatement to set parameters on.
 * @param obj The object to extract values from.
 * @param dbFlavour The database flavor for type-specific handling.
 * @return a cleanup function to invoke after statement execution.
 */
internal fun <T : Any> Map<String, (T) -> Any?>.setParameters(
    tokens: Map<String, List<Int>>,
    stmt: PreparedStatement,
    obj: T,
    dbFlavour: DbFlavour,
): () -> Unit {
    val cleanups = mutableListOf<() -> Unit>()
    try {
        this.forEach { (key, fn) ->
            val indexes =
                tokens[key]
                    ?: throw KapperParseException("Token with name '$key' not found in template")
            val value = fn(obj)
            indexes.forEach { i ->
                cleanups.add(stmt.setParameter(i, value, dbFlavour))
            }
        }
    } catch (e: Exception) {
        cleanups.forEach { it() }
        throw e
    }
    return { cleanups.forEach { it() } }
}
