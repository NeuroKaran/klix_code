package com.klix.code

import android.net.Uri
import com.klix.code.data.repository.WorkspaceRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class WorkspaceCanonicalizationTest {

    private fun mockUri(uriString: String): Uri {
        val uri = mock(Uri::class.java)
        `when`(uri.toString()).thenReturn(uriString)
        return uri
    }

    @Test
    fun testEncodedSpaceParity() {
        val uriEncoded = mockUri("content://com.android.externalstorage.documents/tree/primary%3AMy%20Project")
        val uriDecoded = mockUri("content://com.android.externalstorage.documents/tree/primary:My Project")

        val canon1 = WorkspaceRepositoryImpl.canonicalizeUri(uriEncoded)
        val canon2 = WorkspaceRepositoryImpl.canonicalizeUri(uriDecoded)

        assertEquals("content://com.android.externalstorage.documents/tree/primary:My Project", canon1)
        assertEquals(canon1, canon2)
    }

    @Test
    fun testCjkCharacterParity() {
        // "你好 世界" encoded in UTF-8 is "%E4%BD%A0%E5%A5%BD%20%E4%B8%96%E7%95%8C"
        val uriEncoded = mockUri("file:///storage/emulated/0/%E4%BD%A0%E5%A5%BD%20%E4%B8%96%E7%95%8C")
        val uriDecoded = mockUri("file:///storage/emulated/0/你好 世界")

        val canon1 = WorkspaceRepositoryImpl.canonicalizeUri(uriEncoded)
        val canon2 = WorkspaceRepositoryImpl.canonicalizeUri(uriDecoded)

        assertEquals("file:///storage/emulated/0/你好 世界", canon1)
        assertEquals(canon1, canon2)
    }

    @Test
    fun testTrailingSlashNormalization() {
        val uriWithSlash = mockUri("content://media/external/file/workspace/")
        val uriWithoutSlash = mockUri("content://media/external/file/workspace")

        val canon1 = WorkspaceRepositoryImpl.canonicalizeUri(uriWithSlash)
        val canon2 = WorkspaceRepositoryImpl.canonicalizeUri(uriWithoutSlash)

        assertEquals("content://media/external/file/workspace", canon1)
        assertEquals(canon1, canon2)
    }

    @Test
    fun testPreservesLiteralPlusCharacters() {
        // Folders such as "C++ Projects" must preserve the '+' sign rather than converting to space
        val uriWithPlus = mockUri("file:///home/user/C++%20Projects/app")
        val canon = WorkspaceRepositoryImpl.canonicalizeUri(uriWithPlus)

        assertEquals("file:///home/user/C++ Projects/app", canon)
    }

    @Test
    fun testInvalidEscapeSequenceFallback() {
        // Corrupted escape sequence e.g. %ZZ should gracefully fallback to raw URI without throwing
        val uriWithInvalid = mockUri("file:///storage/emulated/0/Project%ZZ/test/")
        val canon = WorkspaceRepositoryImpl.canonicalizeUri(uriWithInvalid)

        assertEquals("file:///storage/emulated/0/Project%ZZ/test", canon)
    }

    @Test
    fun testMultipleTrailingSlashes() {
        val uriMultipleSlashes = mockUri("file:///storage/project///")
        val canon = WorkspaceRepositoryImpl.canonicalizeUri(uriMultipleSlashes)

        assertEquals("file:///storage/project", canon)
    }
}
