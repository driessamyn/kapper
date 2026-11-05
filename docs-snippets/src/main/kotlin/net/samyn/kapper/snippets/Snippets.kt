package net.samyn.kapper.snippets

import java.time.Instant
import java.util.UUID

// region: prelude
data class User(
    val id: UUID,
    val name: String,
    val email: String?,
    val status: String,
    val createdAt: Instant,
)

data class UserWithOrders(
    val id: UUID,
    val name: String,
    val orderCount: Int,
)
// endregion
