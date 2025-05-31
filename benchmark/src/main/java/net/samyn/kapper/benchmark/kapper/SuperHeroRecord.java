package net.samyn.kapper.benchmark.kapper;

import net.samyn.kapper.benchmark.ISuperHero;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record SuperHeroRecord(UUID id, String name, String email, Integer age) implements ISuperHero {
    // these are only needed because we implement the interface ISuperHero to make the integration test easier
    @Override
    public @NotNull UUID getId() {
        return id();
    }

    @Override
    public @NotNull String getName() {
        return name();
    }

    @Override
    public @Nullable String getEmail() {
        return email();
    }

    @Override
    public @Nullable Integer getAge() {
        return age();
    }
}

