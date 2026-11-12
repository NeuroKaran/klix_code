package com.klix.code

import android.content.Context
import android.net.Uri
import com.klix.code.data.repository.WorkspaceRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

class WorkspaceRepositoryIsolationTest {

    private lateinit var mockContext: Context
    private lateinit var sandboxRoot: File
    private lateinit var workspaceDir: File
    private lateinit var outsideDir: File
    private lateinit var repository: WorkspaceRepositoryImpl

    private fun mockUri(file: File, scheme: String = "file"): Uri {
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(scheme)
        `when`(uri.path).thenReturn(file.absolutePath)
        `when`(uri.toString()).thenReturn("$scheme://${file.absolutePath}")
        return uri
    }

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        val tempRoot = File(System.getProperty("java.io.tmpdir"), "klix_repo_iso_${System.currentTimeMillis()}")
        tempRoot.mkdirs()
        `when`(mockContext.filesDir).thenReturn(tempRoot)

        sandboxRoot = File(tempRoot, "sandbox/rootfs/workspaces").apply { mkdirs() }
        workspaceDir = File(sandboxRoot, "project-alpha").apply { mkdirs() }
        outsideDir = File(tempRoot, "host_phone_storage").apply { mkdirs() }

        repository = WorkspaceRepositoryImpl(mockContext)
    }

    @Test
    fun testRejectsContentUriScheme() = runBlocking {
        val contentUri = mock(Uri::class.java)
        `when`(contentUri.scheme).thenReturn("content")
        `when`(contentUri.path).thenReturn("/storage/emulated/0/DCIM/photo.jpg")
        `when`(contentUri.toString()).thenReturn("content://media/external/images/media/123")

        val tree = repository.loadTree(contentUri)
        assertNull("content:// URIs must be strictly rejected", tree)

        val read = repository.readFile(contentUri)
        assertEquals("", read)

        val created = repository.createFile(contentUri, "hack.txt")
        assertNull(created)
    }

    @Test
    fun testPreventPathTraversalInFindDocument() = runBlocking {
        val secretFile = File(outsideDir, "secret.txt").apply { writeText("super_secret") }
        val wsUri = mockUri(workspaceDir)

        // Attempt to traverse out of workspace directory into outsideDir
        val doc = repository.findDocumentByPath(wsUri, "../../host_phone_storage/secret.txt")
        assertNull("Path traversal via .. must be blocked", doc)
    }

    @Test
    fun testPreventPathTraversalInFindOrCreateDocument() = runBlocking {
        val wsUri = mockUri(workspaceDir)

        val doc = repository.findOrCreateDocumentByPath(wsUri, "../../host_phone_storage/escaped.txt")
        assertNull("Creating document outside workspace root must be blocked", doc)
        assertFalse(File(outsideDir, "escaped.txt").exists())
    }

    @Test
    fun testPreventPathTraversalInCreateFile() = runBlocking {
        val wsUri = mockUri(workspaceDir)

        val created = repository.createFile(wsUri, "../escaped_file.txt")
        assertNull(created)
        assertFalse(File(sandboxRoot, "escaped_file.txt").exists())
    }

    @Test
    fun testPreventPathTraversalInCreateDirectory() = runBlocking {
        val wsUri = mockUri(workspaceDir)

        val created = repository.createDirectory(wsUri, "../escaped_dir")
        assertNull(created)
        assertFalse(File(sandboxRoot, "escaped_dir").exists())
    }

    @Test
    fun testPreventPathTraversalInRename() = runBlocking {
        val targetFile = File(workspaceDir, "legit.txt").apply { writeText("hello") }
        val targetUri = mockUri(targetFile)

        val renamed = repository.rename(targetUri, "../escaped_rename.txt")
        assertFalse(renamed)
        assertTrue(targetFile.exists())
        assertFalse(File(sandboxRoot, "escaped_rename.txt").exists())
    }

    @Test
    fun testReadAndWriteWithinWorkspace() = runBlocking {
        val targetFile = File(workspaceDir, "main.py")
        val fileUri = mockUri(targetFile)

        repository.writeFile(fileUri, "print('sandbox isolated')\n")
        assertTrue(targetFile.exists())

        val content = repository.readFile(fileUri)
        assertEquals("print('sandbox isolated')\n", content)
    }

    @Test
    fun testSearchFilesScopedStrictlyWithinRoot() = runBlocking {
        File(workspaceDir, "App.kt").writeText("fun testKlixSearch() { println(42) }")
        File(outsideDir, "Secret.kt").writeText("fun testKlixSearch() { println('exposed') }")

        val wsUri = mockUri(workspaceDir)
        val results = repository.searchFiles(wsUri, "testKlixSearch")

        assertEquals(1, results.size)
        assertEquals("App.kt", results[0].filePath)
    }

    @Test
    fun testRejectsHostPhoneStoragePathsEvenWithFileScheme() = runBlocking {
        val outsideFile = File(outsideDir, "photo.jpg").apply { writeText("fake image") }
        val outsideUri = mockUri(outsideFile)

        assertNull("Loading tree on outside host storage must be rejected", repository.loadTree(mockUri(outsideDir)))
        assertEquals("Reading file from outside host storage must return empty", "", repository.readFile(outsideUri))
        assertNull("Creating file in outside directory must return null", repository.createFile(mockUri(outsideDir), "test.txt"))
        assertNull("Creating directory in outside directory must return null", repository.createDirectory(mockUri(outsideDir), "sub"))
        assertFalse("Deleting outside file must return false", repository.delete(outsideUri))
        assertFalse("Renaming outside file must return false", repository.rename(outsideUri, "renamed.jpg"))
        assertTrue("Searching outside directory must return empty", repository.searchFiles(mockUri(outsideDir), "fake").isEmpty())

        // Test explicit Android host storage paths
        val sdcardUri = mock(Uri::class.java)
        `when`(sdcardUri.scheme).thenReturn("file")
        `when`(sdcardUri.path).thenReturn("/sdcard/DCIM/photo.jpg")
        `when`(sdcardUri.toString()).thenReturn("file:///sdcard/DCIM/photo.jpg")
        assertNull(repository.loadTree(sdcardUri))
        assertEquals("", repository.readFile(sdcardUri))

        val emulatedUri = mock(Uri::class.java)
        `when`(emulatedUri.scheme).thenReturn("file")
        `when`(emulatedUri.path).thenReturn("/storage/emulated/0/Documents/pass.txt")
        `when`(emulatedUri.toString()).thenReturn("file:///storage/emulated/0/Documents/pass.txt")
        assertNull(repository.loadTree(emulatedUri))
        assertEquals("", repository.readFile(emulatedUri))
    }

    @Test
    fun testRecentProjectsFiltersOutHostPhoneStorage() = runBlocking {
        val outsideFile = File(outsideDir, "test_proj").apply { mkdirs() }
        val outsideUri = mockUri(outsideFile)
        val validFile = File(workspaceDir, "sub_proj").apply { mkdirs() }
        val validUri = mockUri(validFile)

        repository.persistProjectPermission(outsideUri, "Host Phone Storage Project")
        repository.persistProjectPermission(validUri, "Valid Sandbox Project")

        val recents = repository.getRecentProjects()
        // Outside project must be filtered out by uriToFile boundary check
        assertFalse("Host storage projects must not appear in recents", recents.any { it.name == "Host Phone Storage Project" })
        assertTrue("Valid sandbox project must appear in recents", recents.any { it.name == "Valid Sandbox Project" })
    }
}
