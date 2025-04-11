package net.samyn.kapper.benchmark.hibernate

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.hibernate.Hibernate
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.time.LocalDateTime

@Entity
@Table(name = "battles")
class SuperHeroBattleEntity(
    @Id
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.MERGE])
    @JoinColumn(name = "super_hero_id")
    var superHero: SuperHeroEntity,
    @Id
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.MERGE])
    @JoinColumn(name = "villain_id")
    var villain: VillainEntity,
    @Id
    @Column(name = "battle_date")
    var date: LocalDateTime,
) {
    constructor() : this(SuperHeroEntity(), VillainEntity(), LocalDateTime.now())

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_ts")
    val updateTimestamp: Instant? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as SuperHeroBattleEntity

        return superHero == other.superHero && villain == other.villain
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
