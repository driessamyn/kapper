package net.samyn.kapper.example.java.kapper;

import net.samyn.kapper.Kapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SuperHeroRecordRepository {
    private static final Kapper kapper = Kapper.getInstance();
    private final DataSource dataSource;
    private final DbType dbType;

    public SuperHeroRecordRepository(DataSource dataSource) throws SQLException {
        this.dataSource = dataSource;
        try (Connection conn = dataSource.getConnection()) {
            String dbName = conn.getMetaData().getDatabaseProductName();
            if ("MySQL".equals(dbName)) {
                dbType = DbType.MYSQL;
            } else {
                dbType = DbType.POSTGRESQL;
            }
        }
    }

    public enum DbType {
        POSTGRESQL,
        MYSQL
    }

    public List<SuperHeroRecord> list() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return  kapper.query(SuperHeroRecord.class, conn, "SELECT * FROM super_heroes", Map.of());
        }
    }

    public SuperHeroRecord findById(UUID id) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return kapper.querySingle(SuperHeroRecord.class, conn, "SELECT * FROM super_heroes WHERE id = :id", Map.of("id", id));
        }
    }

    public SuperHeroRecord findByName(String name) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return kapper.querySingle(SuperHeroRecord.class, conn, "SELECT * FROM super_heroes WHERE name = :name", Map.of("name", name));
        }
    }

    public List<SuperHeroBattleRecord> findBattles(String superHeroName) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return kapper.query(SuperHeroBattleRecord.class,
                    conn,
                """
                SELECT s.name as superhero, v.name as villain, b.battle_date as date
                FROM super_heroes as s
                INNER JOIN battles as b on s.id = b.super_hero_id
                INNER JOIN villains as v on v.id = b.villain_id
                WHERE s.name = :name
                """,
                    Map.of("name", superHeroName)
            );
        }
    }

    public int insertHero(SuperHeroRecord superHero) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return kapper.execute(
                    conn,
                """
                INSERT INTO super_heroes(id, name, email, age)
                VALUES (:id, :name, :email, :age)
                """,
                    Map.of(
                "id", superHero.id(),
                "name", superHero.name(),
                "email", superHero.email(),
                "age", superHero.age()
                    )
            );
        }
    }

    public void insertBattle(SuperHeroRecord superHero, VillainRecord villain, LocalDateTime date) throws SQLException {
        try(Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
            kapper.execute(
                    conn,
                """
                INSERT INTO super_heroes(id, name, email, age)
                VALUES (:id, :name, :email, :age)
                """ + ignoreConflict("id"),
                Map.of(
                "id", superHero.id(),
                "name", superHero.name(),
                "email", superHero.email(),
                "age", superHero.age()
                )
            );
            kapper.execute(
                    conn,
                """
                INSERT INTO villains(id, name)
                VALUES (:id, :name)
                """ + ignoreConflict("id"),
                Map.of(
                "id", villain.id(),
                "name", villain.name()
                )
            );
            kapper.execute(
                    conn,
                """
                INSERT INTO battles(super_hero_id, villain_id, battle_date, updated_ts)
                VALUES (:super_hero_id, :villain_id, :date, NOW())
                """,
                Map.of(
                "super_hero_id", superHero.id(),
                "villain_id", villain.id(),
                "date", date
                )
            );

            conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<PopularMovieRecord> findPopularMovies() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return kapper.query(PopularMovieRecord.class,
                conn,
                """
                SELECT
                 title,
                 release_date,
                 gross_worldwide,
                 AVG(gross_worldwide) OVER() AS total_average_gross,
                 AVG(gross_worldwide) OVER(PARTITION BY EXTRACT(YEAR FROM release_date)) AS average_annual_gross
                FROM movies
                ORDER BY gross_worldwide DESC
                LIMIT 3
                """,
                Map.of()
            );
        }
    }

    private String ignoreConflict(String updateCol) {
        if (dbType == DbType.MYSQL) {
            return " ON DUPLICATE KEY UPDATE " + updateCol + "=" + updateCol;
        } else {
            return " ON CONFLICT DO NOTHING";
        }
    }
}

