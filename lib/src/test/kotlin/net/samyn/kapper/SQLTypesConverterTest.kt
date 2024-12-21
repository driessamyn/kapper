package net.samyn.kapper

import io.mockk.mockk
import io.mockk.verify
import net.samyn.kapper.internal.DbConnectionUtils.DbFlavour
import net.samyn.kapper.internal.SQLTypesConverter.setParameter
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.sql.PreparedStatement
import java.util.UUID

class SQLTypesConverterTest {
    companion object {
        val statement = mockk<PreparedStatement>(relaxed = true)

        @JvmStatic
        fun parameterTests() =
            listOf(
                arguments(named("STRING", "test"), statement::setString),
                arguments(named("INT", 123), statement::setInt),
                arguments(named("SHORT", (123).toShort()), statement::setShort),
                arguments(named("LONG", 123L), statement::setLong),
                arguments(named("FLOAT", 123.45F), statement::setFloat),
                arguments(named("DOUBLE", 123.45), statement::setDouble),
                arguments(named("BYTE", "test".toByteArray()[0]), statement::setByte),
                arguments(named("BYTE-ARRAY", "test".toByteArray()), statement::setBytes),
                arguments(named("BOOLEAN", true), statement::setBoolean),
            )

        private val uuid = UUID.randomUUID()

        @JvmStatic
        fun parameterWithConvertTests() =
            listOf(
                arguments(named("UUID", uuid), { i: Int, v: Any? -> statement.setObject(i, v) }, uuid, DbFlavour.POSTGRESQL),
                arguments(named("UUID", uuid), statement::setString, uuid.toString(), DbFlavour.MYSQL),
            )
    }

    @ParameterizedTest
    @MethodSource("parameterTests")
    fun `should map values correctly`(
        value: Any?,
        expectedSetter: (Int, Any?) -> Unit,
    ) {
        statement.setParameter(1, value, DbFlavour.UNKNOWN)
        verify { expectedSetter(1, value) }
    }

    @ParameterizedTest
    @MethodSource("parameterWithConvertTests")
    internal fun `should map and convert values correctly`(
        value: Any?,
        expectedSetter: (Int, Any?) -> Unit,
        expectedValue: Any?,
        dbFlavour: DbFlavour,
    ) {
        statement.setParameter(1, value, dbFlavour)
        verify { expectedSetter(1, expectedValue) }
    }
}
