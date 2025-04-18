package net.samyn.kapper.benchmark.hibernate

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.util.UUID

@Entity
@Table(name = "villains")
class VillainEntity(
    @Id
    @Column(name = "id")
    var id: UUID,
    @Column(name = "name")
    var name: String,
) {
    constructor() : this(UUID.randomUUID(), "")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as VillainEntity

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
