package net.samyn.kapper.snippets

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val name: String,
    val email: String?,
    val status: Status,
    val createdAt: Instant,
)

enum class Status { ACTIVE, INACTIVE, SUSPENDED }

data class UserWithOrders(
    val id: UUID,
    val name: String,
    val orderCount: Int,
)
