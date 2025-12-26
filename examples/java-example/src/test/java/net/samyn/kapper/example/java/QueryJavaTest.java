package net.samyn.kapper.example.java;

import net.samyn.kapper.Kapper;
import net.samyn.kapper.example.java.kapper.SuperHeroRecord;
import net.samyn.kapper.example.DbBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class QueryJavaTest extends DbBase {
    private static final Kapper kapper = Kapper.getInstance();
    private static DataSource dataSource;

    @BeforeAll
    public void setup() {
        super.setup();
        dataSource = getDataSource(getPostgresql()); // You may need to implement TestUtil
        var sql =
                """
                INSERT INTO super_heroes (id, name, email, age) VALUES
                ('%s', 'Superman', 'superman@dc.com', 86),
                ('%s', 'Batman', 'batman@dc.com', 85),
                ('%s', 'Spider-man', 'spider@marvel.com', 62);
            """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void simpleQuery() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            var heroes = kapper.query(
                SuperHeroRecord.class,
                conn,
                "SELECT * FROM super_heroes",
                Map.of()
            );
            assertNotNull(heroes);
            assertFalse(heroes.isEmpty());
            assertTrue(heroes.stream().anyMatch(hero -> "Superman".equals(hero.name())));
        }
    }

    @Test
    public void queryWithParameter() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            var olderHeroes = kapper.query(
                SuperHeroRecord.class,
                conn,
                "SELECT * FROM super_heroes WHERE age > :age",
                Map.of("age", 80)
            );
            assertNotNull(olderHeroes);
            assertFalse(olderHeroes.isEmpty());
        }
    }

    @Test
    public void querySingleResult() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            var hero = kapper.querySingle(
                SuperHeroRecord.class,
                conn,
                "SELECT * FROM super_heroes WHERE name = :name",
                Map.of("name", "Batman")
            );
            assertNotNull(hero);
            assertEquals("Batman", hero.name());
        }
    }
}

