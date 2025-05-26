package net.samyn.kapper;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// This test class is used to test the usability of the Java API of Kapper
class KapperJavaApiTest {
    @Mock
    private ResultSetMetaData mockMetaData;

    @Mock
    private ResultSet mockResultSet;

    @Mock
    private PreparedStatement mockStatement;

    @Mock
    private Connection mockConnection;

    private final AutoCloseable mocks;

   class TestEntity {
       private final int id;
       private final String name;

       public TestEntity(int id, String name) {
           this.id = id;
           this.name = name;
       }

       public int id() {
           return id;
       }

       public String name() {
           return name;
       }

       @Override
       public boolean equals(Object o) {
           if (this == o) return true;
           if (o == null || getClass() != o.getClass()) return false;
           TestEntity that = (TestEntity) o;
           return id == that.id && java.util.Objects.equals(name, that.name);
       }

       @Override
       public int hashCode() {
           return java.util.Objects.hash(id, name);
       }

       @Override
       public String toString() {
           return "TestEntity{id=" + id + ", name='" + name + "'}";
       }
   }

    public record AutomappedTestEntity(int id, String name) { }

    KapperJavaApiTest() {
        mocks = MockitoAnnotations.openMocks(this);
        setupMocks();
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    private void setupMocks() {
        try {
            // Setup metadata
            when(mockMetaData.getColumnCount()).thenReturn(2);
            when(mockMetaData.getColumnLabel(1)).thenReturn("id");
            when(mockMetaData.getColumnLabel(2)).thenReturn("name");
            when(mockMetaData.getColumnType(1)).thenReturn(java.sql.Types.INTEGER);
            when(mockMetaData.getColumnType(2)).thenReturn(java.sql.Types.VARCHAR);
            when(mockMetaData.getColumnTypeName(1)).thenReturn("INTEGER");
            when(mockMetaData.getColumnTypeName(2)).thenReturn("VARCHAR");

            // Setup result set
            when(mockResultSet.getMetaData()).thenReturn(mockMetaData);

            // Setup statement
            when(mockStatement.executeQuery()).thenReturn(mockResultSet);

            // Setup connection
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

            // Setup connection metadata
            var mockConnMetaData = mock(java.sql.DatabaseMetaData.class);
            when(mockConnMetaData.getDatabaseProductName()).thenReturn("driesDB");
            when(mockConnection.getMetaData()).thenReturn(mockConnMetaData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGetInstance() {
        assertNotNull(Kapper.getInstance().getClass());
    }

    @Nested
    class QueryTests {
        @Test
        void testQueryWithCustomMapper() throws Exception {
            class TestEntityMapper implements Mapper<TestEntity> {
                @NotNull
                @Override
                public TestEntity createInstance(@NotNull ResultSet resultSet, @NotNull Map<String, Field> fields) {
                    try {
                        return new TestEntity(
                                resultSet.getInt("id"),
                                resultSet.getString("name")
                        );
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            Kapper.getMapperRegistry().registerIfAbsent(TestEntity.class, new TestEntityMapper());

            // Setup result set behavior
            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getInt("id")).thenReturn(1, 2);
            when(mockResultSet.getString("name")).thenReturn("Test1", "Test2");

            Map<String, Object> args = Map.of("id", 1);

            List<TestEntity> result = Kapper.getInstance().query(
                    TestEntity.class,
                    mockConnection,
                    "SELECT * FROM test_table where id = :id",
                    args
            );

            // Assertions
            assertEquals(2, result.size());
            assertEquals(new TestEntity(1, "Test1"), result.get(0));
            assertEquals(new TestEntity(2, "Test2"), result.get(1));

            // Verify interactions
            verify(mockStatement).setInt(1, 1);
            verify(mockStatement).close();
            verify(mockResultSet).close();
        }

        @Test
        void testQueryWithMapperFunc() throws Exception {
            // Setup result set behavior
            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getInt("id")).thenReturn(1, 2);
            when(mockResultSet.getString("name")).thenReturn("Test1", "Test2");

            Map<String, Object> args = Map.of("id", 1);

            List<TestEntity> result = Kapper.getInstance().query(
                    TestEntity.class,
                    mockConnection,
                    "SELECT * FROM test_table where id = :id",
                    (rs, rowNum) -> {
                        try {
                            return new TestEntity(
                                    rs.getInt("id"),
                                    rs.getString("name")
                            );
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    args
            );

            // Assertions
            assertEquals(2, result.size());
            assertEquals(new TestEntity(1, "Test1"), result.get(0));
            assertEquals(new TestEntity(2, "Test2"), result.get(1));

            // Verify interactions
            verify(mockStatement).setInt(1, 1);
            verify(mockStatement).close();
            verify(mockResultSet).close();
        }

        @Test
        void testQueryWithAutoMapperForRecord() throws Exception {
            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getInt(1)).thenReturn(1, 2);
            when(mockResultSet.getString(2)).thenReturn("Test1", "Test2");

            Map<String, Object> args = Map.of("id", 1);

            var kapper = Kapper.getInstance();
            var result = kapper.query(
                    AutomappedTestEntity.class,
                    mockConnection,
                    "SELECT * FROM test_table where id = :id",
                    args
            );

            assertEquals(2, result.size());
            assertEquals(new AutomappedTestEntity(1, "Test1"), result.get(0));
            assertEquals(new AutomappedTestEntity(2, "Test2"), result.get(1));
        }
    }

    @Nested
    class QuerySingleTests {
        @Test
        void testQuerySingleWithCustomMapper() throws Exception {
            // Setup result set behavior
            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getInt("id")).thenReturn(1);
            when(mockResultSet.getString("name")).thenReturn("Test1");

            Map<String, Object> args = Map.of("id", 1);

            TestEntity result = Kapper.getInstance().querySingle(
                    TestEntity.class,
                    mockConnection,
                    "SELECT * FROM test_table where id = :id",
                    (rs, rowNum) -> {
                        try {
                            return new TestEntity(
                                    rs.getInt("id"),
                                    rs.getString("name")
                            );
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    args
            );

            // Assertions
            assertEquals(new TestEntity(1, "Test1"), result);

            // Verify interactions
            verify(mockStatement).setInt(1, 1);
            verify(mockStatement).close();
            verify(mockResultSet).close();
        }
    }

    @Nested
    class ExecuteTests {
        @Test
        void testExecuteWithMap() throws Exception {
            // Setup statement behavior
            when(mockStatement.executeUpdate()).thenReturn(1);

            Map<String, Object> args = new HashMap<>();
            args.put("id", 1);
            args.put("name", "Test1");

            int result = Kapper.getInstance().execute(
                    mockConnection,
                    "INSERT INTO test_table(id, name) VALUES(:id, :name)",
                    args
            );

            // Assertions
            assertEquals(1, result);

            // Verify interactions
            verify(mockStatement).setInt(1, 1);
            verify(mockStatement).setString(2, "Test1");
            verify(mockStatement).close();
        }
    }
}