package io.github.reneknap.mediacenter.data.folder

class FakeFolderAccess : FolderAccess {

    private val reachableUris = mutableSetOf<String>()
    private val displayNames = mutableMapOf<String, String>()

    val takenPermissions: MutableList<String> = mutableListOf()
    val releasedPermissions: MutableList<String> = mutableListOf()

    fun setReachable(uri: String, isReachable: Boolean) {
        if (isReachable) reachableUris.add(uri) else reachableUris.remove(uri)
    }

    fun setDisplayName(uri: String, name: String) {
        displayNames[uri] = name
    }

    override fun takePersistable(uri: String) {
        takenPermissions.add(uri)
    }

    override fun releasePersistable(uri: String) {
        releasedPermissions.add(uri)
    }

    override fun canRead(uri: String): Boolean = uri in reachableUris

    override fun displayName(uri: String): String? = displayNames[uri]
}
