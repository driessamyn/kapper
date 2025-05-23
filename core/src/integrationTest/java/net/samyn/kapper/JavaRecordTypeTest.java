package net.samyn.kapper;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

public record JavaRecordTypeTest(
        UUID t_uuid,
        char t_char,
        String t_varchar,
        String t_clob,
        byte[] t_binary,
        byte[] t_varbinary,
        byte[] t_large_binary,
        float t_numeric,
        float t_decimal,
        int t_smallint,
        int t_int,
        long t_bigint,
        float t_float,
        float t_real,
        double t_double,
        Date t_date,
        LocalDate t_local_date,
        LocalTime t_local_time,
        Instant t_timestamp,
        boolean t_boolean
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JavaRecordTypeTest that)) return false;
        return t_char == that.t_char
                && Float.compare(that.t_numeric, t_numeric) == 0
                && Float.compare(that.t_decimal, t_decimal) == 0
                && t_smallint == that.t_smallint
                && t_int == that.t_int
                && t_bigint == that.t_bigint
                && Float.compare(that.t_float, t_float) == 0
                && Float.compare(that.t_real, t_real) == 0
                && Double.compare(that.t_double, t_double) == 0
                && t_boolean == that.t_boolean
                && t_uuid.equals(that.t_uuid)
                && t_varchar.equals(that.t_varchar)
                && t_clob.equals(that.t_clob)
                && Arrays.equals(t_binary, that.t_binary)
                && Arrays.equals(t_varbinary, that.t_varbinary)
                && Arrays.equals(t_large_binary, that.t_large_binary)
                && t_date.equals(that.t_date)
                && t_local_date.equals(that.t_local_date)
                && t_local_time.equals(that.t_local_time)
                && t_timestamp.equals(that.t_timestamp);
    }

    @Override
    public int hashCode() {
        int result = t_uuid.hashCode();
        result = 31 * result + Character.hashCode(t_char);
        result = 31 * result + t_varchar.hashCode();
        result = 31 * result + t_clob.hashCode();
        result = 31 * result + Arrays.hashCode(t_binary);
        result = 31 * result + Arrays.hashCode(t_varbinary);
        result = 31 * result + Arrays.hashCode(t_large_binary);
        result = 31 * result + Float.hashCode(t_numeric);
        result = 31 * result + Float.hashCode(t_decimal);
        result = 31 * result + Integer.hashCode(t_smallint);
        result = 31 * result + Integer.hashCode(t_int);
        result = 31 * result + Long.hashCode(t_bigint);
        result = 31 * result + Float.hashCode(t_float);
        result = 31 * result + Float.hashCode(t_real);
        result = 31 * result + Double.hashCode(t_double);
        result = 31 * result + t_date.hashCode();
        result = 31 * result + t_local_date.hashCode();
        result = 31 * result + t_local_time.hashCode();
        result = 31 * result + t_timestamp.hashCode();
        result = 31 * result + Boolean.hashCode(t_boolean);
        return result;
    }

    public static JavaRecordTypeTest from(@NotNull TypesTest.TypeTest testData) {
        return new JavaRecordTypeTest(
                testData.getT_uuid(),
                testData.getT_char(),
                testData.getT_varchar(),
                testData.getT_clob(),
                testData.getT_binary(),
                testData.getT_varbinary(),
                testData.getT_large_binary(),
                testData.getT_numeric(),
                testData.getT_decimal(),
                testData.getT_smallint(),
                testData.getT_int(),
                testData.getT_bigint(),
                testData.getT_float(),
                testData.getT_real(),
                testData.getT_double(),
                testData.getT_date(),
                testData.getT_local_date(),
                testData.getT_local_time(),
                testData.getT_timestamp(),
                testData.getT_boolean()
        );
    }
}
