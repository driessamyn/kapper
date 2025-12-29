package net.samyn.kapper.example.java;

import net.samyn.kapper.Kapper;
import net.samyn.kapper.example.DbBase;
import net.samyn.kapper.example.java.kapper.SuperHeroRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExecuteWithDtoJavaTest extends DbBase {
    private static final Kapper kapper = Kapper.getInstance();
    private DataSource dataSource;


    @BeforeAll
    public void setup() {
        super.setup();
        dataSource = getDataSource(getPostgresql());
    }

    @Test
    public void executeInsert() throws Exception {
        var hero = new SuperHeroRecord(
                UUID.randomUUID(), "Wally West", "wally@west.com", 85);
        try (Connection conn = dataSource.getConnection()) {
            var rows = kapper.execute(
                    SuperHeroRecord.class,
                    conn,
                    """
                    INSERT INTO super_heroes(id, name, email, age)
                    VALUES (:id, :name, :email, :age)
                    """,
                    hero,
                    Map.of(
                            "id", SuperHeroRecord::id,
                            "name", SuperHeroRecord::name,
                            "email", SuperHeroRecord::email,
                            "age", SuperHeroRecord::age
                    )
            );
            assertTrue(rows > 0);
        }
    }

    @Test
    public void executeBulkInsert() throws Exception {
        var heroes = List.of(
                new SuperHeroRecord(UUID.randomUUID(), "Silver Surfer", "silver@surfer.com", 78),
                new SuperHeroRecord(UUID.randomUUID(), "Plastic Man", "platic@man.com", 54)
        );
        try (Connection conn = dataSource.getConnection()) {
            var rows = kapper.executeAll(
                    SuperHeroRecord.class,
                    conn,
                    """
                    INSERT INTO super_heroes(id, name, email, age)
                    VALUES (:id, :name, :email, :age)
                    """,
                    heroes,
                    Map.of(
                            "id", SuperHeroRecord::id,
                            "name", SuperHeroRecord::name,
                            "email", SuperHeroRecord::email,
                            "age", SuperHeroRecord::age
                    )
            );
            assertTrue(rows[0] > 0);
            assertTrue(rows[1] > 0);
        }
    }
}

