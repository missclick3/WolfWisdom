package ru.missclick.wolfwisdom

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SeasonalIconTest(private val date: LocalDate, private val expected: SeasonalIcon) {
    @Test
    fun forDate_seasonBoundary_returnsExpectedIcon() {
        assertEquals(expected, SeasonalIcon.forDate(date))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} -> {1}")
        fun dates(): List<Array<Any>> = listOf(
            "2026-01-01" to SeasonalIcon.NEW_YEAR,
            "2026-01-08" to SeasonalIcon.NEW_YEAR,
            "2026-01-09" to SeasonalIcon.WINTER,
            "2026-02-28" to SeasonalIcon.WINTER,
            "2028-02-29" to SeasonalIcon.WINTER,
            "2026-03-01" to SeasonalIcon.SPRING,
            "2026-04-15" to SeasonalIcon.SPRING,
            "2026-05-31" to SeasonalIcon.SPRING,
            "2026-06-01" to SeasonalIcon.SUMMER,
            "2026-07-15" to SeasonalIcon.SUMMER,
            "2026-08-31" to SeasonalIcon.SUMMER,
            "2026-09-01" to SeasonalIcon.AUTUMN,
            "2026-10-15" to SeasonalIcon.AUTUMN,
            "2026-11-30" to SeasonalIcon.AUTUMN,
            "2026-12-01" to SeasonalIcon.WINTER,
            "2026-12-23" to SeasonalIcon.WINTER,
            "2026-12-24" to SeasonalIcon.NEW_YEAR,
            "2026-12-31" to SeasonalIcon.NEW_YEAR,
            "2027-01-01" to SeasonalIcon.NEW_YEAR,
            "2027-01-08" to SeasonalIcon.NEW_YEAR,
            "2027-01-09" to SeasonalIcon.WINTER
        ).map { (date, icon) -> arrayOf<Any>(LocalDate.parse(date), icon) }
    }
}
