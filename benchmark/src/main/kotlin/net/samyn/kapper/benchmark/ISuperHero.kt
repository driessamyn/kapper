package net.samyn.kapper.benchmark

import java.util.UUID

interface ISuperHero {
    val id: UUID
    val name: String
    val email: String?
    val age: Int?
}
