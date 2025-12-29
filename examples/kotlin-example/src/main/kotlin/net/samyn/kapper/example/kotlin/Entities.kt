package net.samyn.kapper.example.kotlin

import java.time.LocalDateTime
import java.util.UUID

data class SuperHero(
    val id: UUID,
    val name: String,
    val email: String? = null,
    val age: Int? = null,
)

data class SuperHeroBattle(
    val superhero: String,
    val villain: String,
    val date: LocalDateTime,
)

data class Villain(
    val id: UUID,
    val name: String,
)

data class PopularMovie(
    val title: String,
    val grossed: Long,
    val comparedToAnnualAverage: Double,
    val allTimeRanking: Int,
)
