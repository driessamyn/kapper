@file:JvmSynthetic

package net.samyn.kapper.internal

import net.samyn.kapper.Args
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
