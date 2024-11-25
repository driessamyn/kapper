package net.samyn.kapper.api;

import net.samyn.kapper.Kapper;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

/**
 * This tests just the usability of the API from JAVA classes
 */
public class JavaUsabilityTest {

    private final Connection _connection = mock(Connection.class);

    private final Kapper _kapper = mock(Kapper.class);

    static class SuperHero {
        SuperHero(String name, int age) {
            this.name = name;
            this.age = age;
        }

        String name;
        int age;
    }

    @Test
    public void canQuery() {
        assertDoesNotThrow(() -> {
            _kapper.query(
                    SuperHero.class,
                    _connection,
                    "SELECT * FROM super_hero " +
                            "WHERE name = :name and age = :age",
                    Map.of(
                            "name", "Batman",
                            "age", 30
                    )
            );
        });
    }

    @Test
    public void canQueryUsingMapper() {
        assertDoesNotThrow(() -> {
            _kapper.querySingle(
                    SuperHero.class,
                    _connection,
                    "SELECT * FROM super_hero " +
                            "WHERE name = :name and age = :age",
                    (resultSet, fields) -> {
                        try {
                            return new SuperHero(resultSet.getString("name"), resultSet.getInt("age"));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    Map.of(
                            "name", "Batman",
                            "age", 30
                    )
            );
        });
    }

    @Test
    public void canQuerySingle() {
        assertDoesNotThrow(() -> {
            _kapper.querySingle(
                    SuperHero.class,
                    _connection,
                    "SELECT * FROM super_hero " +
                            "WHERE name = :name and age = :age",
                    Map.of(
                            "name", "Batman",
                            "age", 30
                    )
            );
        });
    }

    @Test
    public void canQuerySingleUsingMapper() {
        assertDoesNotThrow(() -> {
            _kapper.querySingle(
                    SuperHero.class,
                    _connection,
                    "SELECT * FROM super_hero " +
                            "WHERE name = :name and age = :age",
                    (resultSet, fields) -> {
                        try {
                            return new SuperHero(resultSet.getString("name"), resultSet.getInt("age"));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    Map.of(
                            "name", "Batman",
                            "age", 30
                    )
            );
        });
    }
}
