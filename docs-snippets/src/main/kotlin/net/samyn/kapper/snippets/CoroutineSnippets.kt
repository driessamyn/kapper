package net.samyn.kapper.snippets

import kotlinx.coroutines.flow.collect
import net.samyn.kapper.coroutines.queryAsFlow
import net.samyn.kapper.coroutines.withConnection
import net.samyn.kapper.query
import javax.sql.DataSource

// region: coroutines
suspend fun listUsers(dataSource: DataSource): List<User> =
    dataSource.withConnection { connection ->
        connection.query<User>("SELECT id, name, email, status, created_at FROM users")
    }
// endregion

// region: flow
suspend fun streamUsers(
    dataSource: DataSource,
    onUser: suspend (User) -> Unit,
) = dataSource.withConnection { connection ->
    // queryAsFlow is cold: the query runs when the flow is collected,
    // and the connection stays open for the duration of the collection.
    connection.queryAsFlow<User>("SELECT id, name, email, status, created_at FROM users")
        .collect { onUser(it) }
}
// endregion
