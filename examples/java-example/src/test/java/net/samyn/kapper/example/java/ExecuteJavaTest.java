package net.samyn.kapper.example.java;

import net.samyn.kapper.Kapper;
import net.samyn.kapper.example.java.kapper.SuperHeroRecord;
import net.samyn.kapper.example.java.kapper.SuperHeroRecordRepository;
import net.samyn.kapper.example.DbBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ExecuteJavaTest extends DbBase {
    private static final Kapper kapper = Kapper.getInstance();
    private DataSource dataSource;
    private SuperHeroRecordRepository repo;


    @BeforeAll
    public void setup() {
        super.setup();
        dataSource = getDataSource(getPostgresql());
        try {
            repo = new SuperHeroRecordRepository(dataSource);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(1)
    public void executeInsert() throws Exception {
        SuperHeroRecord hero = new SuperHeroRecord(
                UUID.randomUUID(), "Jman", "jman@dc.com", 85);
        var rows = repo.insertHero(hero);
        assertTrue(rows > 0);
    }

    @Test
    @Order(2)
    public void executeUpdate() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            var rows = kapper.execute(
                    conn,
                    """
                    UPDATE super_heroes
                    SET age = 86
                    WHERE name = :name
                    """,
                    Map.of("name", "Jman")
            );
            assertTrue(rows > 0);
        }
    }

    @Test
    @Order(3)
    public void executeDelete() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            kapper.execute(
                    conn,
                    """
                       DELETE FROM super_heroes
                       WHERE name = :name
                       """,
                    Map.of("name", "Jman")
            );
        }
    }
}

