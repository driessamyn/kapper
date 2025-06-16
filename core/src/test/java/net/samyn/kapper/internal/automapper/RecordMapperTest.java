package net.samyn.kapper.internal.automapper;

import net.samyn.kapper.Field;
import net.samyn.kapper.KapperMappingException;
import net.samyn.kapper.internal.AutoConverter;
import net.samyn.kapper.DbFlavour;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.JDBCType;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecordMapperTest {
    private final AutoCloseable mocks;

    @Mock
    FieldsConverter fieldsConverter;

    @Mock
    AutoConverter autoConverter;

    @Mock
    ResultSet rs;

    Map<String, Field> fields = new HashMap<>();

    SuperHeroRecord hero = new SuperHeroRecord(UUID.randomUUID(), "Batman", "batman@dc.com", 85);

    RecordMapperTest() {
        mocks = MockitoAnnotations.openMocks(this);

        fields.put("id", new Field(1, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN));
        fields.put("name", new Field(2, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN));
        fields.put("email", new Field(3, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN));
        fields.put("age", new Field(4, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN));

        when(fieldsConverter.convert(any(), any())).thenReturn(List.of(
                new net.samyn.kapper.internal.automapper.ColumnValue("id", hero.id()),
                new net.samyn.kapper.internal.automapper.ColumnValue("name", hero.name()),
                new net.samyn.kapper.internal.automapper.ColumnValue("email", hero.email()),
                new net.samyn.kapper.internal.automapper.ColumnValue("age", hero.age())
        ));

        when(autoConverter.convert(hero.age(), int.class)).thenReturn(hero.age());
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "email", "EMAIL", "eMail", "e-mail", "e_mail" })
    void testRecordMapping(String emailParam) {
        fields.remove("email");
        fields.put(emailParam, new Field(3, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN));

        when(fieldsConverter.convert(any(), any())).thenReturn(List.of(
                new net.samyn.kapper.internal.automapper.ColumnValue("id", hero.id()),
                new net.samyn.kapper.internal.automapper.ColumnValue("name", hero.name()),
                new net.samyn.kapper.internal.automapper.ColumnValue(emailParam, hero.email()),
                new net.samyn.kapper.internal.automapper.ColumnValue("age", hero.age())
        ));

        RecordMapper<SuperHeroRecord> mapper = new RecordMapper<>(SuperHeroRecord.class, autoConverter, fieldsConverter);
        SuperHeroRecord mapped = mapper.createInstance(rs, fields);
        assertEquals(mapped, hero);
    }

    @Test
    void shouldThrowWhenNonOptionalAreMissing() {
        when(fieldsConverter.convert(any(), any())).thenReturn(List.of(
                new net.samyn.kapper.internal.automapper.ColumnValue("id", hero.id()),
                new net.samyn.kapper.internal.automapper.ColumnValue("name", hero.name()),
                new net.samyn.kapper.internal.automapper.ColumnValue("email", hero.email())
        ));

        RecordMapper<SuperHeroRecord> mapper = new RecordMapper<>(SuperHeroRecord.class, autoConverter, fieldsConverter);
        Exception ex = assertThrows(KapperMappingException.class, () -> mapper.createInstance(rs, fields));
        assertTrue(ex.getMessage().contains("age"));
        assertTrue(ex.getMessage().contains("missing"));
    }

    @Test
    void shouldConvertWhenTypeNotKnown() {
        when(fieldsConverter.convert(any(), any())).thenReturn(List.of(
                new net.samyn.kapper.internal.automapper.ColumnValue("id", hero.id()),
                new net.samyn.kapper.internal.automapper.ColumnValue("name", 123),
                new net.samyn.kapper.internal.automapper.ColumnValue("email", hero.email()),
                new net.samyn.kapper.internal.automapper.ColumnValue("age", hero.age())
        ));
        when(autoConverter.convert(123, String.class)).thenReturn("Foo");

        RecordMapper<SuperHeroRecord> mapper = new RecordMapper<>(SuperHeroRecord.class, autoConverter, fieldsConverter);
        mapper.createInstance(rs, fields);
        verify(autoConverter).convert(123, String.class);
    }

    @Test
    void shouldSetToNullWhenValueNull() {
        when(fieldsConverter.convert(any(), any())).thenReturn(List.of(
                new net.samyn.kapper.internal.automapper.ColumnValue("id", hero.id()),
                new net.samyn.kapper.internal.automapper.ColumnValue("name", hero.name()),
                new net.samyn.kapper.internal.automapper.ColumnValue("email", null),
                new net.samyn.kapper.internal.automapper.ColumnValue("age", hero.age())
        ));

        RecordMapper<SuperHeroRecord> mapper = new RecordMapper<>(SuperHeroRecord.class, autoConverter, fieldsConverter);
        SuperHeroRecord mapped = mapper.createInstance(rs, fields);
        assertEquals(new SuperHeroRecord(hero.id(), hero.name(), null, hero.age()), mapped);
    }

    @Test
    void nullableIsOptional() {
        when(fieldsConverter.convert(any(), any())).thenReturn(List.of(
                new net.samyn.kapper.internal.automapper.ColumnValue("id", hero.id()),
                new net.samyn.kapper.internal.automapper.ColumnValue("name", hero.name()),
                new net.samyn.kapper.internal.automapper.ColumnValue("age", hero.age())
        ));

        RecordMapper<SuperHeroRecord> mapper = new RecordMapper<>(SuperHeroRecord.class, autoConverter, fieldsConverter);
        SuperHeroRecord mapped = mapper.createInstance(rs, fields);
        assertEquals(new SuperHeroRecord(hero.id(), hero.name(), null, hero.age()), mapped);
    }

    @Test
    void primitiveCannotBeNull() {
        when(fieldsConverter.convert(any(), any())).thenReturn(List.of(
                new net.samyn.kapper.internal.automapper.ColumnValue("id", hero.id()),
                new net.samyn.kapper.internal.automapper.ColumnValue("name", hero.name()),
                new net.samyn.kapper.internal.automapper.ColumnValue("age", null)
        ));

        RecordMapper<SuperHeroRecord> mapper = new RecordMapper<>(SuperHeroRecord.class, autoConverter, fieldsConverter);
        Exception ex =
                assertThrows(
                        KapperMappingException.class,
                        () -> mapper.createInstance(rs, fields));
        assertTrue(ex.getMessage().contains("age"));
    }

    @Test
    void shouldSkipWhenNoPropertyFound() {
        fields.put("extra", new Field(4, JDBCType.BIT, "SomeType", DbFlavour.UNKNOWN));

        RecordMapper<SuperHeroRecord> mapper = new RecordMapper<>(SuperHeroRecord.class, autoConverter, fieldsConverter);
        SuperHeroRecord mapped = mapper.createInstance(rs, fields);
        assertEquals(mapped, hero);
    }

    static class NonPublicRecordHolder {
        // package-private record (not public)
        record NonPublicRecord(String value) {}
    }

    @Test
    void shouldThrowForNonPublicRecord() {
        Class<?> clazz = NonPublicRecordHolder.NonPublicRecord.class;
        Exception ex = assertThrows(
            RuntimeException.class,
            () -> new RecordMapper<>(clazz, autoConverter, fieldsConverter)
        );
        assertTrue(ex.getMessage().contains("Cannot map to non-public record class"));
    }
}

