package net.samyn.kapper.api;

import net.samyn.kapper.Kapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

/**
 * This tests just the usability of the API from JAVA classes
 */
public class JavaUsabilityTest {

    private final Connection _connction = mock(Connection.class);

    private final Kapper _kapper = mock(Kapper.class);

    class SuperHero {
        String name;
        int age;
    }

    @Test
    @Disabled("TODO")
    public void canQuerySingle() {
        assertDoesNotThrow(() -> {
            _kapper.querySingle(
                    SuperHero.class,
                    _connction,
                    "SELECT * FROM super_hero " +
                            "WHERE name = :name and age = :age",
                    Map.of(
                            "name", "Batman",
                            "age", 30
                    )
            );
        });
    }
}
