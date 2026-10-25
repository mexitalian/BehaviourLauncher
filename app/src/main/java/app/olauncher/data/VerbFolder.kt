package app.olauncher.data

/**
 * A verb-named folder shown on the Intently home screen.
 *
 * Serialization is a simple hand-rolled format (tab-separated fields,
 * comma-separated app list) so the model is pure Kotlin and testable
 * on the JVM without Android or JSON dependencies:
 *
 *   id<TAB>verb<TAB>defaultApp<TAB>pkg1,pkg2,...
 */
data class VerbFolder(
    val id: Long,
    val verb: String,
    val apps: List<String>,
    val defaultApp: String? = null
) {

    /** The app launched on a plain tap: explicit default, else first member. */
    val tapApp: String? get() = defaultApp ?: apps.firstOrNull()

    fun withAppAdded(packageName: String): VerbFolder {
        if (apps.contains(packageName)) return this
        return copy(apps = apps + packageName)
    }

    fun withAppRemoved(packageName: String): VerbFolder {
        if (!apps.contains(packageName)) return this
        val newApps = apps - packageName
        val newDefault = when {
            defaultApp == packageName -> null
            else -> defaultApp
        }
        return copy(apps = newApps, defaultApp = newDefault)
    }

    fun serialized(): String {
        val sanitizedVerb = verb.replace(ESCAPED_CHARS, " ")
        val appsJoined = apps.joinToString(",")
        return listOf(id.toString(), sanitizedVerb, defaultApp ?: "", appsJoined)
            .joinToString(DELIM)
    }

    companion object {
        private const val DELIM = "\t"
        private val ESCAPED_CHARS = Regex("[\\t\\n\\r,]")

        fun parse(line: String): VerbFolder? {
            val parts = line.split(DELIM)
            if (parts.size != 4) return null
            val id = parts[0].toLongOrNull() ?: return null
            val verb = parts[1].trim()
            if (verb.isEmpty()) return null
            val defaultApp = parts[2].ifEmpty { null }
            val apps = parts[3].split(",").filter { it.isNotBlank() }
            return VerbFolder(id, verb, apps, defaultApp)
        }
    }
}
