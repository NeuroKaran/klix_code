package com.klix.code.data.sandbox

import android.os.Build
import android.system.Os
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystemException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission
import java.util.EnumSet

internal interface TarPlatformOps {
    fun setMode(path: Path, mode: Int)
    fun createSymlink(linkPath: Path, target: String)
    fun createHardLink(linkPath: Path, targetPath: Path)
}

internal object SystemTarPlatformOps : TarPlatformOps {

    internal fun isAndroidRuntime(): Boolean {
        val vmName = System.getProperty("java.vm.name")
        return vmName != null &&
            vmName.equals("Dalvik", ignoreCase = true) &&
            Build.VERSION.SDK_INT >= 26
    }

    internal fun isWindowsHost(): Boolean {
        return !isAndroidRuntime() &&
            (System.getProperty("os.name")?.contains("win", ignoreCase = true) == true)
    }

    override fun setMode(path: Path, mode: Int) {
        val maskedMode = mode and 0x1FF
        if (isAndroidRuntime()) {
            Os.chmod(path.toAbsolutePath().toString(), maskedMode)
            return
        }

        val posixView = Files.getFileAttributeView(
            path,
            PosixFileAttributeView::class.java,
            LinkOption.NOFOLLOW_LINKS
        )
        if (posixView != null) {
            posixView.setPermissions(modeToPosixPermissions(maskedMode))
            return
        }

        // Non-POSIX JVM (e.g. Windows host): best-effort File permission flags
        val file = path.toFile()
        val ownerRead = (maskedMode and 0b100_000_000) != 0
        val ownerWrite = (maskedMode and 0b010_000_000) != 0
        val ownerExec = (maskedMode and 0b001_000_000) != 0
        val anyRead = (maskedMode and 0b100_100_100) != 0
        val anyExec = (maskedMode and 0b001_001_001) != 0
        val allCanRead = (maskedMode and 0b000_100_100) != 0
        val allCanWrite = (maskedMode and 0b000_010_010) != 0
        val allCanExec = (maskedMode and 0b000_001_001) != 0

        file.setReadable(ownerRead || anyRead, ownerRead && !allCanRead)
        file.setWritable(ownerWrite, ownerWrite && !allCanWrite)
        file.setExecutable(ownerExec || anyExec, ownerExec && !allCanExec)
    }

    override fun createSymlink(linkPath: Path, target: String) {
        if (isAndroidRuntime()) {
            Os.symlink(target, linkPath.toAbsolutePath().toString())
            return
        }

        if (Files.exists(linkPath, LinkOption.NOFOLLOW_LINKS)) {
            throw FileAlreadyExistsException(linkPath.toString())
        }

        try {
            Files.createSymbolicLink(linkPath, Paths.get(target))
        } catch (e: FileAlreadyExistsException) {
            throw e
        } catch (e: FileSystemException) {
            // Windows hosts without Developer Mode / SeCreateSymbolicLinkPrivilege throw FileSystemException.
            // Never swallow symlink failures when target exists, or on Android / POSIX JVMs.
            if (!isWindowsHost() || Files.exists(linkPath, LinkOption.NOFOLLOW_LINKS)) {
                throw e
            }
        }
    }

    override fun createHardLink(linkPath: Path, targetPath: Path) {
        if (isAndroidRuntime()) {
            Os.link(targetPath.toAbsolutePath().toString(), linkPath.toAbsolutePath().toString())
            return
        }
        Files.createLink(linkPath, targetPath)
    }
}

internal fun modeToPosixPermissions(mode: Int): Set<PosixFilePermission> {
    val perms = EnumSet.noneOf(PosixFilePermission::class.java)
    if ((mode and 0b100_000_000) != 0) perms.add(PosixFilePermission.OWNER_READ)
    if ((mode and 0b010_000_000) != 0) perms.add(PosixFilePermission.OWNER_WRITE)
    if ((mode and 0b001_000_000) != 0) perms.add(PosixFilePermission.OWNER_EXECUTE)
    if ((mode and 0b000_100_000) != 0) perms.add(PosixFilePermission.GROUP_READ)
    if ((mode and 0b000_010_000) != 0) perms.add(PosixFilePermission.GROUP_WRITE)
    if ((mode and 0b000_001_000) != 0) perms.add(PosixFilePermission.GROUP_EXECUTE)
    if ((mode and 0b000_000_100) != 0) perms.add(PosixFilePermission.OTHERS_READ)
    if ((mode and 0b000_000_010) != 0) perms.add(PosixFilePermission.OTHERS_WRITE)
    if ((mode and 0b000_000_001) != 0) perms.add(PosixFilePermission.OTHERS_EXECUTE)
    return perms
}

internal class TarGzExtractor(
    private val platformOps: TarPlatformOps = SystemTarPlatformOps
) {
    private data class PendingHardlink(
        val linkPath: Path,
        val targetPath: Path,
        val rawLinkName: String,
        val rawTargetName: String
    )

    private class NonClosingInputStream(delegate: InputStream) : FilterInputStream(delegate) {
        override fun close() {
            // Keep underlying buffered GZIP stream open across concatenated TAR members.
        }
    }

    private class TrackingTarInputStream(delegate: InputStream) : FilterInputStream(delegate) {
        var bytesSinceReset: Long = 0L
            private set
        var nonZeroSeenSinceReset: Boolean = false
            private set
        var totalBytesRead: Long = 0L
            private set

        private var markedBytesSinceReset: Long = 0L
        private var markedNonZeroSeenSinceReset: Boolean = false
        private var markedTotalBytesRead: Long = 0L
        private val skipBuffer = ByteArray(4096)

        fun resetCheck() {
            bytesSinceReset = 0L
            nonZeroSeenSinceReset = false
        }

        override fun read(): Int {
            val b = `in`.read()
            if (b >= 0) {
                bytesSinceReset++
                totalBytesRead++
                if (b != 0) {
                    nonZeroSeenSinceReset = true
                }
            }
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = `in`.read(b, off, len)
            if (n > 0) {
                bytesSinceReset += n
                totalBytesRead += n
                if (!nonZeroSeenSinceReset) {
                    for (i in off until (off + n)) {
                        if (b[i] != 0.toByte()) {
                            nonZeroSeenSinceReset = true
                            break
                        }
                    }
                }
            }
            return n
        }

        override fun skip(n: Long): Long {
            if (n <= 0L) return 0L
            var remaining = n
            var skipped = 0L
            while (remaining > 0L) {
                val toRead = minOf(remaining, skipBuffer.size.toLong()).toInt()
                val r = read(skipBuffer, 0, toRead)
                if (r == -1) break
                skipped += r
                remaining -= r
            }
            return skipped
        }

        override fun mark(readlimit: Int) {
            markedBytesSinceReset = bytesSinceReset
            markedNonZeroSeenSinceReset = nonZeroSeenSinceReset
            markedTotalBytesRead = totalBytesRead
            `in`.mark(readlimit)
        }

        override fun reset() {
            `in`.reset()
            bytesSinceReset = markedBytesSinceReset
            nonZeroSeenSinceReset = markedNonZeroSeenSinceReset
            totalBytesRead = markedTotalBytesRead
        }
    }

    fun extract(
        tarGzFile: File,
        destDir: File,
        allowAbsoluteSymlinks: Boolean = false
    ): Result<Unit> {
        return try {
            extractInternal(tarGzFile, destDir, allowAbsoluteSymlinks)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractInternal(
        tarGzFile: File,
        destDir: File,
        allowAbsoluteSymlinks: Boolean
    ) {
        if (!tarGzFile.exists() || !tarGzFile.isFile) {
            throw IOException("Archive file does not exist: ${tarGzFile.absolutePath}")
        }
        if (destDir.exists() && !destDir.isDirectory) {
            throw IOException("Destination exists and is not a directory: ${destDir.absolutePath}")
        }
        if (!destDir.exists() && !destDir.mkdirs()) {
            throw IOException("Failed to create destination directory: ${destDir.absolutePath}")
        }

        val destRoot = destDir.toPath().toAbsolutePath().normalize()
        val destRealRoot = destRoot.toRealPath()

        val pendingHardlinks = mutableListOf<PendingHardlink>()
        val pendingDirModes = mutableListOf<Pair<Path, Int>>()
        val createdSymlinks = mutableSetOf<Path>()
        val buffer = ByteArray(8192)

        BufferedInputStream(FileInputStream(tarGzFile)).use { bis ->
            GzipCompressorInputStream(bis, true).use { gzis ->
                BufferedInputStream(gzis).use { tarStream ->
                    do {
                        val trackingStream = TrackingTarInputStream(NonClosingInputStream(tarStream))
                        TarArchiveInputStream(trackingStream, TAR_RECORD_SIZE).use { tarIn ->
                            trackingStream.resetCheck()
                            var entry: TarArchiveEntry? = tarIn.nextEntry
                            while (entry != null) {
                                processEntry(
                                    tarIn = tarIn,
                                    entry = entry,
                                    destRoot = destRoot,
                                    destRealRoot = destRealRoot,
                                    allowAbsoluteSymlinks = allowAbsoluteSymlinks,
                                    pendingHardlinks = pendingHardlinks,
                                    pendingDirModes = pendingDirModes,
                                    createdSymlinks = createdSymlinks,
                                    buffer = buffer
                                )
                                trackingStream.resetCheck()
                                entry = tarIn.nextEntry
                            }
                            if (trackingStream.totalBytesRead == 0L) {
                                throw IOException("Empty or truncated TAR archive")
                            }
                            if (trackingStream.totalBytesRead < TAR_RECORD_SIZE ||
                                (trackingStream.nonZeroSeenSinceReset &&
                                    (trackingStream.totalBytesRead % TAR_RECORD_SIZE != 0L ||
                                        trackingStream.bytesSinceReset < TAR_RECORD_SIZE))
                            ) {
                                throw IOException(
                                    "Truncated or corrupted TAR record (${trackingStream.bytesSinceReset} bytes)"
                                )
                            }
                        }
                    } while (hasNextTarMember(tarStream))
                }
            }
        }

        resolvePendingHardlinks(destRoot, destRealRoot, pendingHardlinks, createdSymlinks)

        pendingDirModes
            .sortedByDescending { it.first.nameCount }
            .forEach { (dirPath, mode) ->
                if (Files.isDirectory(dirPath, LinkOption.NOFOLLOW_LINKS)) {
                    platformOps.setMode(dirPath, mode)
                }
            }
    }

    private fun hasNextTarMember(stream: BufferedInputStream): Boolean {
        val record = ByteArray(TAR_RECORD_SIZE)
        while (true) {
            stream.mark(TAR_RECORD_SIZE)
            var totalRead = 0
            while (totalRead < record.size) {
                val read = stream.read(record, totalRead, record.size - totalRead)
                if (read == -1) {
                    break
                }
                totalRead += read
            }
            if (totalRead == 0) {
                return false
            }
            val hasNonZero = (0 until totalRead).any { record[it] != 0.toByte() }
            if (totalRead < record.size) {
                if (hasNonZero) {
                    throw IOException("Corrupted trailing TAR data")
                }
                return false
            }
            if (hasNonZero) {
                stream.reset()
                return true
            }
        }
    }

    private fun processEntry(
        tarIn: TarArchiveInputStream,
        entry: TarArchiveEntry,
        destRoot: Path,
        destRealRoot: Path,
        allowAbsoluteSymlinks: Boolean,
        pendingHardlinks: MutableList<PendingHardlink>,
        pendingDirModes: MutableList<Pair<Path, Int>>,
        createdSymlinks: MutableSet<Path>,
        buffer: ByteArray
    ) {
        val rawName = entry.name ?: ""
        if (rawName.indexOf('\u0000') >= 0) {
            throw SecurityException("Tar entry contains NUL byte")
        }

        val normalizedRaw = rawName.replace('\\', '/')
        if (isAbsoluteOrDrivePath(normalizedRaw)) {
            throw SecurityException("Absolute tar entry path is not allowed: $rawName")
        }

        val isSymbolicLink = entry.isSymbolicLink
        val isHardLink = entry.isLink
        val isRealDirectory = entry.isDirectory && !isSymbolicLink && !isHardLink

        val cleanName = stripLeadingCurrentDir(normalizedRaw)
        val effectiveName = if (isRealDirectory) cleanName else cleanName.trimEnd('/')
        if (effectiveName.isEmpty() || effectiveName == ".") {
            if (isRealDirectory) {
                return
            }
            throw SecurityException("Invalid empty/root non-directory tar entry: $rawName")
        }

        val outPath = destRoot.resolve(effectiveName).normalize()
        if (!outPath.startsWith(destRoot) || outPath == destRoot) {
            throw SecurityException("Tar entry escapes destination directory: $rawName")
        }

        when {
            isSymbolicLink -> {
                val target = entry.linkName ?: ""
                if (target.isBlank() || target.indexOf('\u0000') >= 0) {
                    throw IOException("Invalid symlink target for $rawName")
                }
                val normalizedTarget = target.replace('\\', '/')
                if (DRIVE_LETTER_REGEX.matches(normalizedTarget)) {
                    throw SecurityException("Drive-letter symlink target is not allowed: $rawName -> $target")
                }
                if (normalizedTarget.startsWith("/")) {
                    if (!allowAbsoluteSymlinks) {
                        throw SecurityException("Absolute symlink target is not allowed: $rawName -> $target")
                    }
                } else {
                    val parent = outPath.parent ?: destRoot
                    val resolvedTarget = parent.resolve(normalizedTarget).normalize()
                    if (!resolvedTarget.startsWith(destRoot)) {
                        throw SecurityException("Symlink target escapes destination: $rawName -> $target")
                    }
                }

                assertNoSymlinkAncestors(destRoot, outPath, createdSymlinks)
                val parent = outPath.parent ?: destRoot
                Files.createDirectories(parent)
                if (!parent.toRealPath().startsWith(destRealRoot)) {
                    throw SecurityException("Symlink parent resolves outside destination: $rawName")
                }
                pendingHardlinks.removeAll { it.linkPath == outPath }
                pendingDirModes.removeAll { it.first == outPath }
                unlinkBeforeOverwrite(outPath, createdSymlinks, pendingHardlinks, pendingDirModes)
                createdSymlinks.add(outPath)
                platformOps.createSymlink(outPath, target)
            }

            isHardLink -> {
                val rawTarget = entry.linkName ?: ""
                if (rawTarget.isBlank() || rawTarget.indexOf('\u0000') >= 0) {
                    throw IOException("Invalid hardlink target for $rawName")
                }
                val normalizedTarget = rawTarget.replace('\\', '/')
                if (isAbsoluteOrDrivePath(normalizedTarget)) {
                    throw SecurityException("Absolute hardlink target is not allowed: $rawName -> $rawTarget")
                }
                val cleanTarget = stripLeadingCurrentDir(normalizedTarget).trimEnd('/')
                if (cleanTarget.isEmpty() || cleanTarget == ".") {
                    throw SecurityException("Invalid hardlink target: $rawName -> $rawTarget")
                }
                val targetPath = destRoot.resolve(cleanTarget).normalize()
                if (!targetPath.startsWith(destRoot) || targetPath == destRoot) {
                    throw SecurityException("Hardlink target escapes destination: $rawName -> $rawTarget")
                }

                assertNoSymlinkAncestors(destRoot, outPath, createdSymlinks)
                pendingHardlinks.removeAll { it.linkPath == outPath }
                pendingDirModes.removeAll { it.first == outPath }
                pendingHardlinks += PendingHardlink(
                    linkPath = outPath,
                    targetPath = targetPath,
                    rawLinkName = rawName,
                    rawTargetName = rawTarget
                )
            }

            isRealDirectory -> {
                assertNoSymlinkAncestors(destRoot, outPath, createdSymlinks)
                pendingHardlinks.removeAll { it.linkPath == outPath }
                pendingDirModes.removeAll { it.first == outPath }
                if ((Files.exists(outPath, LinkOption.NOFOLLOW_LINKS) &&
                        !Files.isDirectory(outPath, LinkOption.NOFOLLOW_LINKS)) ||
                    outPath in createdSymlinks
                ) {
                    unlinkBeforeOverwrite(outPath, createdSymlinks, pendingHardlinks, pendingDirModes)
                }
                Files.createDirectories(outPath)
                if (!outPath.toRealPath().startsWith(destRealRoot)) {
                    throw SecurityException("Directory resolves outside destination: $rawName")
                }
                pendingDirModes += outPath to extractPermMode(entry.mode, isDir = true)
            }

            entry.isFile -> {
                assertNoSymlinkAncestors(destRoot, outPath, createdSymlinks)
                val parent = outPath.parent ?: destRoot
                Files.createDirectories(parent)
                if (!parent.toRealPath().startsWith(destRealRoot)) {
                    throw SecurityException("File parent resolves outside destination: $rawName")
                }
                unlinkBeforeOverwrite(outPath, createdSymlinks, pendingHardlinks, pendingDirModes)

                FileOutputStream(outPath.toFile()).use { fos ->
                    var read: Int
                    while (tarIn.read(buffer).also { read = it } != -1) {
                        fos.write(buffer, 0, read)
                    }
                }
                platformOps.setMode(outPath, extractPermMode(entry.mode, isDir = false))
            }

            else -> {
                // Drain and skip unsupported special entries safely
                while (tarIn.read(buffer) != -1) {
                    // discard
                }
            }
        }
    }

    private fun unlinkBeforeOverwrite(
        outPath: Path,
        createdSymlinks: MutableSet<Path>,
        pendingHardlinks: MutableList<PendingHardlink>,
        pendingDirModes: MutableList<Pair<Path, Int>>
    ) {
        pendingHardlinks.removeAll { it.linkPath.startsWith(outPath) }
        pendingDirModes.removeAll { it.first.startsWith(outPath) }

        if (Files.isSymbolicLink(outPath) || outPath in createdSymlinks) {
            Files.deleteIfExists(outPath)
            createdSymlinks.remove(outPath)
        } else if (Files.isDirectory(outPath, LinkOption.NOFOLLOW_LINKS)) {
            Files.walkFileTree(outPath, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (SystemTarPlatformOps.isWindowsHost()) {
                        dir.toFile().setWritable(true)
                    } else {
                        platformOps.setMode(dir, 0b111_000_000)
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (!attrs.isSymbolicLink && SystemTarPlatformOps.isWindowsHost()) {
                        file.toFile().setWritable(true)
                    }
                    Files.deleteIfExists(file)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    if (exc != null) throw exc
                    Files.deleteIfExists(dir)
                    return FileVisitResult.CONTINUE
                }
            })
            createdSymlinks.removeAll { it.startsWith(outPath) }
        } else if (Files.exists(outPath, LinkOption.NOFOLLOW_LINKS)) {
            if (SystemTarPlatformOps.isWindowsHost()) {
                outPath.toFile().setWritable(true)
            }
            Files.deleteIfExists(outPath)
        }
    }

    private fun resolvePendingHardlinks(
        destRoot: Path,
        destRealRoot: Path,
        pendingHardlinks: MutableList<PendingHardlink>,
        createdSymlinks: MutableSet<Path>
    ) {
        val pendingDirModesNoop = mutableListOf<Pair<Path, Int>>()
        var remaining = pendingHardlinks.toMutableList()
        while (remaining.isNotEmpty()) {
            var madeProgress = false
            val nextRemaining = mutableListOf<PendingHardlink>()
            for (item in remaining) {
                assertNoSymlinkAncestors(destRoot, item.linkPath, createdSymlinks)
                assertNoSymlinkAncestors(destRoot, item.targetPath, createdSymlinks)

                if (Files.isSymbolicLink(item.targetPath) || item.targetPath in createdSymlinks) {
                    throw SecurityException("Hardlink target cannot be a symlink: ${item.rawLinkName} -> ${item.rawTargetName}")
                }

                if (Files.exists(item.targetPath, LinkOption.NOFOLLOW_LINKS)) {
                    if (Files.isDirectory(item.targetPath, LinkOption.NOFOLLOW_LINKS)) {
                        throw IOException("Hardlink target is a directory: ${item.rawLinkName} -> ${item.rawTargetName}")
                    }
                    if (!item.targetPath.toRealPath(LinkOption.NOFOLLOW_LINKS).startsWith(destRealRoot)) {
                        throw SecurityException("Hardlink target resolves outside destination: ${item.rawLinkName} -> ${item.rawTargetName}")
                    }

                    if (item.linkPath == item.targetPath) {
                        madeProgress = true
                        continue
                    }

                    val parent = item.linkPath.parent ?: destRoot
                    Files.createDirectories(parent)
                    if (!parent.toRealPath().startsWith(destRealRoot)) {
                        throw SecurityException("Hardlink parent resolves outside destination: ${item.rawLinkName}")
                    }

                    unlinkBeforeOverwrite(item.linkPath, createdSymlinks, pendingHardlinks, pendingDirModesNoop)
                    platformOps.createHardLink(item.linkPath, item.targetPath)
                    madeProgress = true
                } else {
                    nextRemaining.add(item)
                }
            }
            if (!madeProgress) {
                val unresolved = nextRemaining.joinToString(", ") { "${it.rawLinkName} -> ${it.rawTargetName}" }
                throw IOException("Unresolved hardlink target(s): $unresolved")
            }
            remaining = nextRemaining
        }
    }

    private fun assertNoSymlinkAncestors(
        destRoot: Path,
        targetPath: Path,
        createdSymlinks: Set<Path>
    ) {
        val rel = destRoot.relativize(targetPath)
        var current = destRoot
        for (i in 0 until (rel.nameCount - 1)) {
            current = current.resolve(rel.getName(i))
            if (current in createdSymlinks || Files.isSymbolicLink(current)) {
                throw SecurityException("Tar entry traverses symlink directory: $current")
            }
        }
    }

    private fun stripLeadingCurrentDir(path: String): String {
        var result = path
        while (result.startsWith("./")) {
            result = result.removePrefix("./")
        }
        return result
    }

    private fun isAbsoluteOrDrivePath(path: String): Boolean {
        return path.startsWith("/") || DRIVE_LETTER_REGEX.matches(path)
    }

    private fun extractPermMode(entryMode: Int, isDir: Boolean): Int {
        return if (entryMode == 0) {
            if (isDir) DEFAULT_DIR_MODE else DEFAULT_FILE_MODE
        } else {
            entryMode and 0x1FF
        }
    }

    companion object {
        private const val TAR_RECORD_SIZE = 512
        private const val DEFAULT_DIR_MODE = 0b111_101_101 // 0755
        private const val DEFAULT_FILE_MODE = 0b110_100_100 // 0644
        private val DRIVE_LETTER_REGEX = Regex("^[A-Za-z]:.*")
    }
}

