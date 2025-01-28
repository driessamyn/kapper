package net.samyn.kapper;

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

// this is the Java equivalent of the Kotlin KapperApiTest
//  its purpose is to test kapper is usable from Java
class KapperJavaApiTest {
    private final Kapper kapper = Kapper.getInstance();

    @Mock
    private ResultSetMetaData mockMetaData;

    @Mock
    private ResultSet mockResultSet;

    @Mock
    private PreparedStatement mockStatement;

    @Mock
    private Connection mockConnection;

    // TODO: support auto-mapping for record classes
    record TestEntity(int id, String name) { }

    KapperJavaApiTest() {
        MockitoAnnotations.openMocks(this);
        setupMocks();
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
        assertNotNull(kapper.getClass());
    }

    @Nested
    class QueryTests {
        // TODO: Kapper currently doesn't support auto-mapping to Java classes
        //  this can be added later.

        @Test
        void testQueryWithCustomMapper() throws Exception {
            // Setup result set behavior
            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getInt("id")).thenReturn(1, 2);
            when(mockResultSet.getString("name")).thenReturn("Test1", "Test2");

            Map<String, Object> args = Map.of("id", 1);

            List<TestEntity> result = kapper.query(
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

            TestEntity result = kapper.querySingle(
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

            int result = kapper.execute(
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