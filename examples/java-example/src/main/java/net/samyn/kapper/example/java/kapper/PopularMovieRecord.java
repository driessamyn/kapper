package net.samyn.kapper.example.java.kapper;

public record PopularMovieRecord(
        String title,
        long grossed,
        double comparedToAnnualAverage,
        int allTimeRanking
) {}

