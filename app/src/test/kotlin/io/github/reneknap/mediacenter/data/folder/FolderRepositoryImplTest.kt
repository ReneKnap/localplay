package io.github.reneknap.mediacenter.data.folder

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FolderRepositoryImplTest {
    private lateinit var dataSource: FakeFolderPreferencesDataSource
    private lateinit var access: FakeFolderAccess
    private lateinit var repository: FolderRepositoryImpl

    @Before
    fun setUp() {
        dataSource = FakeFolderPreferencesDataSource()
        access = FakeFolderAccess()
        repository = FolderRepositoryImpl(dataSource, access)
    }

    @Test
    fun `folders flow emits empty list when data source is empty`() =
        runTest {
            repository.folders.test {
                assertEquals(emptyList<FolderEntry>(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `folders flow marks entries as reachable when access reports readable`() =
        runTest {
            val uri = "content://music/album"
            access.setReachable(uri, isReachable = true)
            dataSource.save(listOf(FolderEntry(uri, "Album", isReachable = false)))

            repository.folders.test {
                val emitted = awaitItem()
                assertEquals(1, emitted.size)
                assertTrue(emitted[0].isReachable)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `folders flow marks entries as unreachable when access reports unreadable`() =
        runTest {
            val uri = "content://music/deleted"
            dataSource.save(listOf(FolderEntry(uri, "Deleted", isReachable = true)))

            repository.folders.test {
                val emitted = awaitItem()
                assertFalse(emitted[0].isReachable)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `addFolder takes persistable permission, resolves display name, and persists`() =
        runTest {
            val uri = "content://music/new"
            access.setReachable(uri, isReachable = true)
            access.setDisplayName(uri, "New Music")

            repository.addFolder(uri)

            assertEquals(listOf(uri), access.takenPermissions)
            repository.folders.test {
                val emitted = awaitItem()
                assertEquals(1, emitted.size)
                assertEquals(uri, emitted[0].uri)
                assertEquals("New Music", emitted[0].displayName)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `addFolder does not duplicate when the same uri is added twice`() =
        runTest {
            val uri = "content://music/album"
            access.setReachable(uri, isReachable = true)
            access.setDisplayName(uri, "Album")

            repository.addFolder(uri)
            repository.addFolder(uri)

            repository.folders.test {
                assertEquals(1, awaitItem().size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `removeFolder releases persistable permission and removes the entry`() =
        runTest {
            val uri = "content://music/album"
            access.setReachable(uri, isReachable = true)
            access.setDisplayName(uri, "Album")
            repository.addFolder(uri)

            repository.removeFolder(uri)

            assertEquals(listOf(uri), access.releasedPermissions)
            repository.folders.test {
                assertEquals(emptyList<FolderEntry>(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
