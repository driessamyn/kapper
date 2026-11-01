package net.samyn.kapper.snippets

import net.samyn.kapper.execute
import net.samyn.kapper.withTransaction
import java.util.UUID
import javax.sql.DataSource

// region: transaction-with-value
fun createWithAudit(dataSource: DataSource, user: User): UUID =
    dataSource.withTransaction { connection ->
        connection.execute(
            "INSERT INTO users(id, name) VALUES(:id, :name)",
            "id" to user.id,
            "name" to user.name,
        )
        connection.execute(
            "INSERT INTO audit(user_id, action) VALUES(:id, 'CREATE')",
            "id" to user.id,
        )
        user.id
    }
// endregion

// region: nested-transaction
fun nested(dataSource: DataSource, user: User) =
    dataSource.withTransaction { connection ->
        connection.execute(
            "INSERT INTO users(id, name) VALUES(:id, :name)",
            "id" to user.id,
            "name" to user.name,
        )
        // A nested withTransaction joins the enclosing transaction instead of committing it.
        connection.withTransaction {
            connection.execute(
                "INSERT INTO audit(user_id, action) VALUES(:id, 'CREATE')",
                "id" to user.id,
            )
        }
    }
// endregion
