package net.samyn.kapper.internal.automapper;

import java.util.UUID;

public record SuperHeroRecord(
        UUID id,
        String name,
        String email,
        int age
) { }
