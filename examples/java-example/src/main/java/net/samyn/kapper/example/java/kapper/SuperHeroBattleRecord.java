package net.samyn.kapper.example.java.kapper;

import java.time.LocalDateTime;

public record SuperHeroBattleRecord(
        String superhero,
        String villain,
        LocalDateTime date
) {}

