package app.olauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VerbFolderTest {

    @Test
    fun serializationRoundTrips() {
        val folder = VerbFolder(7L, "Read", listOf("com.example.a", "com.example.b"), "com.example.b")
        assertEquals(folder, VerbFolder.parse(folder.serialized()))
    }

    @Test
    fun parseRejectsMalformedLines() {
        assertNull(VerbFolder.parse(""))
        assertNull(VerbFolder.parse("not-a-folder"))
        assertNull(VerbFolder.parse("abc\tRead\t\t"))
    }

    @Test
    fun tapAppFallsBackToFirstMember() {
        val noDefault = VerbFolder(1L, "Listen", listOf("com.music", "com.podcast"))
        assertEquals("com.music", noDefault.tapApp)
        val withDefault = noDefault.copy(defaultApp = "com.podcast")
        assertEquals("com.podcast", withDefault.tapApp)
        assertNull(VerbFolder(2L, "Empty", emptyList()).tapApp)
    }

    @Test
    fun addAppIsIdempotent() {
        val folder = VerbFolder(1L, "Talk", listOf("com.dialer"))
        val added = folder.withAppAdded("com.dialer")
        assertEquals(folder, added)
        assertEquals(listOf("com.dialer", "com.sms"), folder.withAppAdded("com.sms").apps)
    }

    @Test
    fun removeAppClearsDefaultIfItWasRemoved() {
        val folder = VerbFolder(1L, "Watch", listOf("com.video", "com.tv"), "com.video")
        val removed = folder.withAppRemoved("com.video")
        assertEquals(listOf("com.tv"), removed.apps)
        assertNull(removed.defaultApp)
        assertFalse(removed.apps.contains("com.video"))
    }

    @Test
    fun removeAppKeepsDefaultIfItStays() {
        val folder = VerbFolder(1L, "Watch", listOf("com.video", "com.tv"), "com.video")
        val removed = folder.withAppRemoved("com.tv")
        assertEquals("com.video", removed.defaultApp)
        assertTrue(removed.apps.contains("com.video"))
    }
}
