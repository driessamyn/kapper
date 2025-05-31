package net.samyn.kapper.benchmark.hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "super_heroes")
public record SuperHeroRecordEntity(
        @Id
        @Column(name = "id")
        UUID id,
        @Column(name = "name")
        String name,
        @Column(name = "email")
        String email,
        @Column(name = "age")
        Integer age
) {}
