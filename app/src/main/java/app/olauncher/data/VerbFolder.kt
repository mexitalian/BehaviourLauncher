package app.olauncher.data

/**
 * A member app of an intention. [label] is the bespoke name the user gave
 * it, if any (null = show the installed app label).
 */
data class Member(val packageName: String, val label: String? = null)

/**
 * An intention on the Intently home screen: a named group of apps.
 * With a single member it behaves as a plain app (tap launches it);
 * more members make it a folder.
 *
 * Serialization (one line, tab-separated; pure Kotlin, JVM-testable):
 *   id<TAB>verb<TAB>defaultApp<TAB>member1,member2,...
 * where each member is `packageName~label` (label part optional).
 */
data class VerbFolder(
    val id: Long,
    val verb: String,
    val members: List<Member>,
    val defaultApp: String? = null
) {

    /** The app launched on a plain tap: explicit primary, else first member. */
    val tapApp: String? get() = defaultApp ?: members.firstOrNull()?.packageName

    fun withMemberAdded(packageName: String, label: String? = null): VerbFolder {
        if (members.any { it.packageName == packageName }) return this
        return copy(members = members + Member(packageName, label?.takeIf { it.isNotBlank() }))
    }

    fun withMemberRemoved(packageName: String): VerbFolder {
        if (members.none { it.packageName == packageName }) return this
        val newMembers = members.filterNot { it.packageName == packageName }
        val newDefault = if (defaultApp == packageName) null else defaultApp
        return copy(members = newMembers, defaultApp = newDefault)
    }

    fun withMemberRenamed(packageName: String, label: String?): VerbFolder {
        val newMembers = members.map { member ->
            if (member.packageName == packageName)
                member.copy(label = label?.takeIf { it.isNotBlank() })
            else
                member
        }
        return copy(members = newMembers)
    }

    fun serialized(): String {
        val sanitizedVerb = verb.replace(SANITIZED_CHARS, " ")
        val membersJoined = members.joinToString(",") { member ->
            val label = member.label?.replace(SANITIZED_CHARS, " ")
            if (label.isNullOrBlank()) member.packageName
            else member.packageName + "~" + label
        }
        return listOf(id.toString(), sanitizedVerb, defaultApp ?: "", membersJoined)
            .joinToString(DELIM)
    }

    companion object {
        private const val DELIM = "\t"
        private val SANITIZED_CHARS = Regex("[\\t\\n\\r,~]")

        fun parse(line: String): VerbFolder? {
            val parts = line.split(DELIM)
            if (parts.size != 4) return null
            val id = parts[0].toLongOrNull() ?: return null
            val verb = parts[1].trim()
            val defaultApp = parts[2].ifEmpty { null }
            val members = parts[3].split(",").filter { it.isNotBlank() }.map { raw ->
                val m = raw.split("~", limit = 2)
                if (m.size == 2) Member(m[0], m[1]) else Member(raw)
            }
            return VerbFolder(id, verb, members, defaultApp)
        }
    }
}
