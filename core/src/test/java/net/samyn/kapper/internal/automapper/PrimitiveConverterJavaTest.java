package net.samyn.kapper.internal.automapper;

import org.junit.jupiter.api.Test;

import static net.samyn.kapper.internal.AutoConverterKt.convertToPrimitive;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PrimitiveConverterJavaTest {

    @Test
    void testConvertInt() {
        int value = 42;
        var converted = convertToPrimitive(value);
        assertInstanceOf(Integer.class, converted);
    }

    @Test
    void testConvertLong() {
        long value = 42L;
        var converted = convertToPrimitive(value);
        assertInstanceOf(Long.class, converted);
    }

    @Test
    void testConvertDouble() {
        double value = 3.14;
        var converted = convertToPrimitive(value);
        assertInstanceOf(Double.class, converted);
    }

    @Test
    void testConvertFloat() {
        float value = 2.71f;
        var converted = convertToPrimitive(value);
        assertInstanceOf(Float.class, converted);
    }

    @Test
    void testConvertBoolean() {
        boolean value = true;
        var converted = convertToPrimitive(value);
        assertInstanceOf(Boolean.class, converted);
    }

    @Test
    void testConvertByte() {
        byte value = 0x1A;
        var converted = convertToPrimitive(value);
        assertInstanceOf(Byte.class, converted);
    }

    @Test
    void testConvertShort() {
        short value = 123;
        var converted = convertToPrimitive(value);
        assertInstanceOf(Short.class, converted);
    }

    @Test
    void testConvertChar() {
        char value = 'A';
        var converted = convertToPrimitive(value);
        assertInstanceOf(Character.class, converted);
    }
}
