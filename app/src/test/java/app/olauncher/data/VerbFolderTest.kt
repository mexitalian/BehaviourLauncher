package app.olauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VerbFolderTest {

    @Test
    fun serializationRoundTrips() {
        val folder = VerbFolder(
            7L, "Read",
            listOf(Member("com.example.a"), Member("com.example.b", "Sunday reading")),
            "com.example.b"
        )
        assertEquals(folder, VerbFolder.parse(folder.serialized()))
    }

    @Test
    fun roundTripsUnnamedIntention() {
        val folder = VerbFolder(1L, "", emptyList())
        assertEquals(folder, VerbFolder.parse(folder.serialized()))
    }

    @Test
    fun parseRejectsMalformedLines() {
        assertNull(VerbFolder.parse(""))
        assertNull(VerbFolder.parse("not-an-intention"))
        assertNull(VerbFolder.parse("abc\tRead\t\t"))
    }

    @Test
    fun sanitizesDangerousCharacters() {
        val folder = VerbFolder(1L, "Read, now", listOf(Member("com.a", "late~night,read")))
        val parsed = VerbFolder.parse(folder.serialized())
        assertTrue(parsed!!.verb.contains(" "))
        val label = parsed.members.first().label!!
        assertTrue(label.contains("night") && label.contains("read"))
        assertTrue(!label.contains("~") && !label.contains(","))
    }

    @Test
    fun primaryFallsBackToFirstMember() {
        val noDefault = VerbFolder(1L, "Listen", listOf(Member("com.music"), Member("com.podcast")))
        assertEquals("com.music", noDefault.tapApp)
        val withDefault = noDefault.copy(defaultApp = "com.podcast")
        assertEquals("com.podcast", withDefault.tapApp)
        assertNull(VerbFolder(2L, "Empty", emptyList()).tapApp)
    }

    @Test
    fun addMemberIsIdempotentAndKeepsFirstLabel() {
        val folder = VerbFolder(1L, "Talk", listOf(Member("com.dialer")))
        assertEquals(folder, folder.withMemberAdded("com.dialer", "Changed"))
        val added = folder.withMemberAdded("com.sms")
        assertEquals(listOf("com.dialer", "com.sms"), added.members.map { it.packageName })
    }

    @Test
    fun removeMemberClearsPrimaryIfItWasRemoved() {
        val folder = VerbFolder(
            1L, "Watch",
            listOf(Member("com.video"), Member("com.tv")), "com.video"
        )
        val removed = folder.withMemberRemoved("com.video")
        assertEquals(listOf("com.tv"), removed.members.map { it.packageName })
        assertNull(removed.defaultApp)
    }

    @Test
    fun removeMemberKeepsPrimaryIfItStays() {
        val folder = VerbFolder(
            1L, "Watch",
            listOf(Member("com.video"), Member("com.tv")), "com.video"
        )
        val removed = folder.withMemberRemoved("com.tv")
        assertEquals("com.video", removed.defaultApp)
    }

    @Test
    fun renameMemberOnlyTouchesThatMember() {
        val folder = VerbFolder(
            1L, "Read",
            listOf(Member("com.books", "Bedtime"), Member("com.news"))
        )
        val renamed = folder.withMemberRenamed("com.news", "Headlines")
        assertEquals("Bedtime", renamed.members[0].label)
        assertEquals("Headlines", renamed.members[1].label)
        // clearing
        val cleared = renamed.withMemberRenamed("com.news", null)
        assertNull(cleared.members[1].label)
    }
}
