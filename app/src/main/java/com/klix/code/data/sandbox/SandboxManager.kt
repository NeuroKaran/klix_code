package com.klix.code.data.sandbox

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

enum class SandboxStatus {
    NOT_INSTALLED,
    DOWNLOADING,
    EXTRACTING,
    READY,
    ERROR
}

class SandboxManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val _status = MutableStateFlow(if (isInstalled()) SandboxStatus.READY else SandboxStatus.NOT_INSTALLED)
    val status: StateFlow<SandboxStatus> = _status.asStateFlow()

    private val _statusMessage = MutableStateFlow(
        if (isInstalled()) "Alpine Linux sandbox ready" else "Sandbox not installed"
    )
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    fun getSandboxDir(): File = File(context.filesDir, "sandbox").apply { if (!exists()) mkdirs() }
    fun getRootfsDir(): File = File(getSandboxDir(), "rootfs")
    fun getBinDir(): File = File(getSandboxDir(), "bin").apply { if (!exists()) mkdirs() }

    fun isInstalled(): Boolean {
        val rootfs = getRootfsDir()
        val busybox = File(rootfs, "bin/busybox")
        val alpineRelease = File(rootfs, "etc/alpine-release")
        val binSh = File(rootfs, "bin/sh")
        return rootfs.exists() && (busybox.exists() || alpineRelease.exists() || binSh.exists())
    }

    fun detectArch(): String {
        val abis = try { Build.SUPPORTED_ABIS } catch (_: Throwable) { null }
        if (abis != null && abis.isNotEmpty()) {
            for (abi in abis) {
                when {
                    abi.startsWith("arm64") -> return "aarch64"
                    abi.startsWith("x86_64") -> return "x86_64"
                    abi.startsWith("armeabi") -> return "armv7"
                    abi.startsWith("x86") -> return "x86"
                }
            }
        }
        val osArch = (System.getProperty("os.arch", "aarch64") ?: "aarch64").lowercase()
        return when {
            osArch.contains("aarch64") || osArch.contains("arm64") -> "aarch64"
            osArch.contains("x86_64") || osArch.contains("amd64") -> "x86_64"
            osArch.contains("arm") -> "armv7"
            osArch.contains("x86") -> "x86"
            else -> "aarch64"
        }
    }

    fun getDownloadUrl(arch: String = detectArch()): String {
        val version = "3.21.3"
        return "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/$arch/alpine-minirootfs-$version-$arch.tar.gz"
    }

    suspend fun installSandbox(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _status.value = SandboxStatus.DOWNLOADING
            _progress.value = 0.05f
            _statusMessage.value = "Connecting to Alpine Linux repository..."

            val arch = detectArch()
            val url = getDownloadUrl(arch)
            val tarGzFile = File(getSandboxDir(), "alpine-rootfs-$arch.tar.gz")

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Download failed with HTTP ${response.code}")
                }
                val body = response.body ?: throw IllegalStateException("Empty response body")
                val totalBytes = body.contentLength()
                var bytesReadTotal = 0L

                _statusMessage.value = "Downloading Alpine Linux ($arch)..."

                body.byteStream().use { input ->
                    FileOutputStream(tarGzFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesReadTotal += read
                            if (totalBytes > 0) {
                                _progress.value = 0.05f + 0.65f * (bytesReadTotal.toFloat() / totalBytes)
                            }
                        }
                        output.flush()
                    }
                }
            }

            // Extract RootFS
            _status.value = SandboxStatus.EXTRACTING
            _statusMessage.value = "Extracting root filesystem..."
            _progress.value = 0.75f

            val rootfsDir = getRootfsDir()
            if (rootfsDir.exists()) {
                rootfsDir.deleteRecursively()
            }
            rootfsDir.mkdirs()

            extractTarGz(tarGzFile, rootfsDir)

            // Setup basic config
            setupBasicEnvironment(rootfsDir)

            // Clean up tarball to save device storage
            tarGzFile.delete()

            _status.value = SandboxStatus.READY
            _progress.value = 1.0f
            _statusMessage.value = "Alpine Linux sandbox ready"
            Result.success(Unit)
        } catch (e: Exception) {
            _status.value = SandboxStatus.ERROR
            _statusMessage.value = "Installation error: ${e.message}"
            Result.failure(e)
        }
    }

    private fun setupBasicEnvironment(rootfsDir: File) {
        val etcDir = File(rootfsDir, "etc").apply { if (!exists()) mkdirs() }
        val resolvConf = File(etcDir, "resolv.conf")
        val dnsServers = mutableListOf("8.8.8.8", "1.1.1.1")
        try {
            val p = Runtime.getRuntime().exec(arrayOf("getprop", "net.dns1"))
            val d1 = p.inputStream.bufferedReader().readLine()?.trim()
            if (!d1.isNullOrBlank() && d1.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                dnsServers.add(0, d1)
            }
        } catch (_: Throwable) {}
        resolvConf.writeText(dnsServers.distinct().joinToString("\n") { "nameserver $it" } + "\n")

        val hostsFile = File(etcDir, "hosts")
        if (!hostsFile.exists()) {
            hostsFile.writeText("127.0.0.1 localhost\n::1 localhost\n")
        }

        // Setup tmp directory with read/write/exec permissions
        val tmpDir = File(rootfsDir, "tmp").apply { if (!exists()) mkdirs() }
        tmpDir.setReadable(true, false)
        tmpDir.setWritable(true, false)
        tmpDir.setExecutable(true, false)

        val workspaceDir = File(rootfsDir, "workspace").apply { if (!exists()) mkdirs() }
        workspaceDir.setReadable(true, false)
        workspaceDir.setWritable(true, false)
        workspaceDir.setExecutable(true, false)

        // Setup profile environment
        val profile = File(etcDir, "profile")
        profile.writeText(
            """
            export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
            export HOME=/root
            export TERM=xterm-256color
            export LANG=C.UTF-8
            export PS1='\u@klix:\w\$ '
            """.trimIndent()
        )
    }

    private fun extractTarGz(tarGzFile: File, targetDir: File) {
        // Extract using GZIPInputStream + full TAR header parser (files, dirs, symlinks, hardlinks)
        GZIPInputStream(BufferedInputStream(FileInputStream(tarGzFile))).use { gzis ->
            val buffer = ByteArray(8192)
            val header = ByteArray(512)

            while (true) {
                var headerBytesRead = 0
                while (headerBytesRead < 512) {
                    val r = gzis.read(header, headerBytesRead, 512 - headerBytesRead)
                    if (r == -1) break
                    headerBytesRead += r
                }
                if (headerBytesRead < 512) break

                // Check for null block (end of archive)
                if (header.all { it == 0.toByte() }) break

                // Read file name (first 100 bytes of tar header)
                val rawName = String(header, 0, 100, Charsets.UTF_8).trim('\u0000', ' ')
                val cleanName = rawName.removePrefix("./").removePrefix("/")
                if (cleanName.isEmpty() || cleanName == ".") continue

                // Size is stored in octal string at offset 124, 12 bytes
                val sizeStr = String(header, 124, 12, Charsets.UTF_8).trim('\u0000', ' ')
                val size = sizeStr.toLongOrNull(8) ?: 0L

                // Type flag at offset 156
                val typeFlag = header[156].toInt().toChar()

                val outFile = File(targetDir, cleanName)

                when (typeFlag) {
                    '5' -> { // Directory
                        outFile.mkdirs()
                    }
                    '0', '\u0000' -> { // Regular file
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            var remaining = size
                            while (remaining > 0) {
                                val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                                val r = gzis.read(buffer, 0, toRead)
                                if (r == -1) break
                                fos.write(buffer, 0, r)
                                remaining -= r
                            }
                        }
                        outFile.setReadable(true, false)
                        outFile.setWritable(true, false)
                        outFile.setExecutable(true, false)

                        // Skip tar padding to 512-byte boundary
                        val padding = (512 - (size % 512)) % 512
                        if (padding > 0) {
                            var skipped = 0L
                            while (skipped < padding) {
                                val s = gzis.read(buffer, 0, (padding - skipped).toInt())
                                if (s == -1) break
                                skipped += s
                            }
                        }
                    }
                    '2' -> { // Symbolic link
                        val linkTarget = String(header, 157, 100, Charsets.UTF_8).trim('\u0000', ' ')
                        if (linkTarget.isNotEmpty()) {
                            outFile.parentFile?.mkdirs()
                            if (outFile.exists()) {
                                outFile.delete()
                            }
                            createSymlink(linkTarget, outFile, targetDir)
                        }
                    }
                    '1' -> { // Hard link
                        val linkTarget = String(header, 157, 100, Charsets.UTF_8).trim('\u0000', ' ')
                        if (linkTarget.isNotEmpty()) {
                            outFile.parentFile?.mkdirs()
                            if (outFile.exists()) {
                                outFile.delete()
                            }
                            val sourceFile = File(targetDir, linkTarget.removePrefix("./").removePrefix("/"))
                            if (sourceFile.exists()) {
                                try {
                                    sourceFile.copyTo(outFile, overwrite = true)
                                    outFile.setExecutable(true, false)
                                } catch (_: Throwable) {}
                            }
                        }
                    }
                    else -> {
                        // Skip unsupported block types (e.g. pax headers) and skip padding
                        var remaining = size
                        while (remaining > 0) {
                            val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                            val r = gzis.read(buffer, 0, toRead)
                            if (r == -1) break
                            remaining -= r
                        }
                        val padding = (512 - (size % 512)) % 512
                        if (padding > 0) {
                            var skipped = 0L
                            while (skipped < padding) {
                                val s = gzis.read(buffer, 0, (padding - skipped).toInt())
                                if (s == -1) break
                                skipped += s
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createSymlink(target: String, linkFile: File, rootfsDir: File) {
        try {
            android.system.Os.symlink(target, linkFile.absolutePath)
        } catch (_: Throwable) {
            try {
                java.nio.file.Files.createSymbolicLink(
                    linkFile.toPath(),
                    java.nio.file.Paths.get(target)
                )
            } catch (_: Throwable) {
                // Fallback: If symlink is restricted on the host filesystem (e.g. Windows unit tests),
                // copy the target binary so the executable file exists
                try {
                    val resolvedTarget = if (target.startsWith("/")) {
                        File(rootfsDir, target.removePrefix("/"))
                    } else {
                        File(linkFile.parentFile, target)
                    }
                    if (resolvedTarget.exists() && !resolvedTarget.isDirectory) {
                        resolvedTarget.copyTo(linkFile, overwrite = true)
                        linkFile.setExecutable(true, false)
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    fun resetSandbox(): Boolean {
        return try {
            val rootfs = getRootfsDir()
            if (rootfs.exists()) {
                rootfs.deleteRecursively()
            }
            _status.value = SandboxStatus.NOT_INSTALLED
            _statusMessage.value = "Sandbox not installed"
            _progress.value = 0f
            true
        } catch (_: Exception) {
            false
        }
    }
}
