package net.samyn.kapper.internal.automapper

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class MapperUtilsTest {
    @ParameterizedTest
    @ValueSource(
        strings = [
            "name",
            "NAME",
            "nAmE",
            "n-a-m-e",
            "n_a_m_e",
            "n-a-m_E",
        ],
    )
    fun normalise(name: String) {
        val normalised = name.normalisedColumnName()
        normalised shouldBe "name"
    }
}
