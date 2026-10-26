package app.olauncher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * SharedPreferences-backed store for the user's intentions.
 * Follows the same persistence pattern as [Prefs].
 */
class FolderStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_FILENAME, 0)

    private var nextId: Long
        get() = prefs.getLong(NEXT_ID, 1L)
        set(value) = prefs.edit { putLong(NEXT_ID, value) }

    val folders: List<VerbFolder>
        get() {
            val raw = prefs.getString(FOLDERS, null) ?: return emptyList()
            return raw.split("\n").mapNotNull { VerbFolder.parse(it) }
        }

    fun createFolder(verb: String): VerbFolder {
        val id = nextId
        nextId = id + 1
        val folder = VerbFolder(id, verb.trim(), emptyList())
        save(folders + folder)
        return folder
    }

    fun update(folder: VerbFolder) {
        val current = folders
        val index = current.indexOfFirst { it.id == folder.id }
        if (index == -1) return
        save(current.toMutableList().apply { set(index, folder) })
    }

    fun delete(folderId: Long) {
        save(folders.filter { it.id != folderId })
    }

    fun reorder(folderIdsInOrder: List<Long>) {
        val byId = folders.associateBy { it.id }
        val reordered = folderIdsInOrder.mapNotNull { byId[it] }
        val unlisted = folders.filter { it.id !in folderIdsInOrder.toSet() }
        save(reordered + unlisted)
    }

    fun addMember(folderId: Long, packageName: String, label: String? = null) =
        mutate(folderId) { it.withMemberAdded(packageName, label) }

    fun removeMember(folderId: Long, packageName: String) =
        mutate(folderId) { it.withMemberRemoved(packageName) }

    fun replaceMember(folderId: Long, oldPackage: String, newPackage: String) =
        mutate(folderId) { it.withMemberReplaced(oldPackage, newPackage) }

    fun renameMember(folderId: Long, packageName: String, label: String?) =
        mutate(folderId) { it.withMemberRenamed(packageName, label) }

    fun setDefaultApp(folderId: Long, packageName: String) =
        mutate(folderId) { it.copy(defaultApp = packageName) }

    /** Drop references to apps that are no longer installed. */
    fun pruneApps(installedPackages: Set<String>) {
        val pruned = folders.map { folder ->
            val kept = folder.members.filter { installedPackages.contains(it.packageName) }
            val keptDefault = folder.defaultApp?.takeIf { installedPackages.contains(it) }
            if (kept == folder.members && keptDefault == folder.defaultApp) folder
            else folder.copy(members = kept, defaultApp = keptDefault)
        }
        if (pruned != folders) save(pruned)
    }

    /** Remove intentions that were entered but never given a name or an app. */
    fun cleanupEmptyUnnamed() {
        val empties = folders.filter { it.verb.isBlank() && it.members.isEmpty() }
        if (empties.isNotEmpty())
            save(folders.filterNot { it.verb.isBlank() && it.members.isEmpty() })
    }

    private fun mutate(folderId: Long, transform: (VerbFolder) -> VerbFolder) {
        val current = folders
        val index = current.indexOfFirst { it.id == folderId }
        if (index == -1) return
        save(current.toMutableList().apply { set(index, transform(current[index])) })
    }

    private fun save(newFolders: List<VerbFolder>) {
        val raw = newFolders.joinToString("\n") { it.serialized() }
        prefs.edit { putString(FOLDERS, raw).apply() }
    }

    companion object {
        private const val PREFS_FILENAME = "intently.folders"
        private const val FOLDERS = "FOLDERS"
        private const val NEXT_ID = "NEXT_ID"
    }
}
