package net.samyn.kapper.benchmark.hibernate

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import net.samyn.kapper.benchmark.ISuperHero
import org.hibernate.Hibernate
import java.util.UUID

@Entity
@Table(name = "super_heroes")
class SuperHeroEntity(
    @Id
    @Column(name = "id")
    override var id: UUID,
    @Column(name = "name")
    override var name: String,
    @Column(name = "email")
    override var email: String?,
    @Column(name = "age")
    override var age: Int?,
) : ISuperHero {
    constructor() : this(UUID.randomUUID(), "", null, null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as SuperHeroEntity

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
