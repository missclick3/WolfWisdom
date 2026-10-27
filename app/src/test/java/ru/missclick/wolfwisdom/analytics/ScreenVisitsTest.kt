package ru.missclick.wolfwisdom.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenVisitsTest {
    @Test
    fun screenVisitSource_initialMainVisit_isAppLaunch() {
        assertEquals("app_launch", screenVisitSource(null, "main-1", null, "main"))
    }

    @Test
    fun screenVisitSource_sameEntryAfterRecreation_doesNotCreateVisit() {
        assertNull(screenVisitSource("saved-1", "saved-1", "main-1", "saved"))
    }

    @Test
    fun screenVisitSource_menuNavigation_recordsMenuSource() {
        assertEquals("menu", screenVisitSource("main-1", "saved-1", "main-1", "saved"))
    }

    @Test
    fun screenVisitSource_returnToExistingEntry_createsBackVisit() {
        assertEquals("back", screenVisitSource("saved-1", "main-1", null, "main"))
    }

    @Test
    fun screenVisitSource_errorNavigation_isInternal() {
        assertEquals("internal", screenVisitSource("saved-1", "error-1", "saved-1", "error"))
    }
}
