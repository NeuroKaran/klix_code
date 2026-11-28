package com.klix.code.data.sandbox

import android.content.Context
import android.os.Build
import android.system.Os
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

enum class SandboxStatus {
    NOT_INSTALLED,
    DOWNLOADING,
    EXTRACTING,
    READY,
    ERROR
}

enum class DownloadPhase {
    CONFIRMATION,
    PREPARING,
    DOWNLOADING,
    VERIFYING,
    EXTRACTING,
    COMPLETED,
    FAILED
}

data class ToolchainDownloadState(
    val toolId: String,
    val displayName: String,
    val subtitle: String,
    val phase: DownloadPhase,
    val progress: Float,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val statusMessage: String = "",
    val errorMessage: String? = null
)

data class OnDemandToolchainSpec(
    val id: String,
    val dirName: String,
    val displayName: String,
    val subtitle: String,
    val archiveFileName: String,
    val downloadUrl: String,
    val expectedBytes: Long,
    val expectedSha256: String,
    val approxSizeLabel: String
)

class SandboxManager(private val context: Context) {

    private val _status = MutableStateFlow(if (isInstalled()) SandboxStatus.READY else SandboxStatus.NOT_INSTALLED)
    val status: StateFlow<SandboxStatus> = _status.asStateFlow()

    private val _statusMessage = MutableStateFlow(
        if (isInstalled()) "Python 3 Bionic sandbox ready" else "Sandbox not installed"
    )
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _installedTools = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val installedTools: StateFlow<Map<String, Boolean>> = _installedTools.asStateFlow()

    private val _installingTools = MutableStateFlow<Set<String>>(emptySet())
    val installingTools: StateFlow<Set<String>> = _installingTools.asStateFlow()

    private val _isInstallingAll = MutableStateFlow(false)
    val isInstallingAll: StateFlow<Boolean> = _isInstallingAll.asStateFlow()

    private val _installAllProgress = MutableStateFlow(0f)
    val installAllProgress: StateFlow<Float> = _installAllProgress.asStateFlow()

    private val _installAllMessage = MutableStateFlow<String?>(null)
    val installAllMessage: StateFlow<String?> = _installAllMessage.asStateFlow()

    private val _activeDownload = MutableStateFlow<ToolchainDownloadState?>(null)
    val activeDownload: StateFlow<ToolchainDownloadState?> = _activeDownload.asStateFlow()

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var activeDownloadJob: Job? = null

    internal var tarGzExtractor: TarGzExtractor = TarGzExtractor()

    internal var httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    internal var onDemandSpecsOverride: Map<String, OnDemandToolchainSpec>? = null

    private val defaultOnDemandSpecs: Map<String, OnDemandToolchainSpec> = mapOf(
        "nodejs" to OnDemandToolchainSpec(
            id = "nodejs",
            dirName = "nodejs",
            displayName = "Node.js",
            subtitle = "Node.js v24 + ICU • 16 KB Bionic",
            archiveFileName = "klix-toolchain-nodejs-arm64-v8a.tar.gz",
            downloadUrl = "https://github.com/NeuroKaran/klix_code/releases/download/toolchains-v1/klix-toolchain-nodejs-arm64-v8a.tar.gz",
            expectedBytes = 30_494_781L,
            expectedSha256 = "3c5d379bafddaab7324a8b3b043dca6e198f26750f722100a6475aa8f4b3c73c",
            approxSizeLabel = "30.5 MB"
        ),
        "c/c++" to OnDemandToolchainSpec(
            id = "c/c++",
            dirName = "cpp",
            displayName = "C / C++",
            subtitle = "Clang / LLVM + LLD + Sysroot • 16 KB",
            archiveFileName = "klix-toolchain-cpp-arm64-v8a.tar.gz",
            downloadUrl = "https://github.com/NeuroKaran/klix_code/releases/download/toolchains-v1/klix-toolchain-cpp-arm64-v8a.tar.gz",
            expectedBytes = 71_176_398L,
            expectedSha256 = "44deeadd82ed1f9ab4eb64ebedae3c8bfa2b561252c0f8cfe63733b3d7a2cc64",
            approxSizeLabel = "71.2 MB"
        )
    )

    private val onDemandToolOrder = listOf("nodejs", "c/c++")

    init {
        refreshInstalledTools()
    }

    fun normalizeToolId(toolId: String): String = when (toolId.lowercase()) {
        "node", "nodejs", "nodejs & npm" -> "nodejs"
        "python", "python3", "python 3 & pip" -> "python3"
        "dash", "sh", "dash shell" -> "dash"
        "build-base", "c/c++ build essentials", "c/c++", "cpp", "c++", "c", "clang", "clang++" -> "c/c++"
        else -> toolId.lowercase()
    }

    fun getOnDemandSpec(toolId: String): OnDemandToolchainSpec? {
        val canonical = normalizeToolId(toolId)
        val specs = onDemandSpecsOverride ?: defaultOnDemandSpecs
        return specs[canonical]
    }

    fun isOnDemandToolchain(toolId: String): Boolean = getOnDemandSpec(toolId) != null

    fun setToolInstalling(toolId: String, installing: Boolean) {
        val canonical = normalizeToolId(toolId)
        val current = _installingTools.value.toMutableSet()
        if (installing) {
            current.add(canonical)
        } else {
            current.remove(canonical)
        }
        _installingTools.value = current
    }

    fun refreshInstalledTools() {
        val rootfs = getRootfsDir()
        if (!rootfs.exists()) {
            _installedTools.value = mapOf(
                "nodejs" to false,
                "python3" to false,
                "dash" to false,
                "git" to false,
                "c/c++" to false
            )
            return
        }

        val map = mapOf(
            "nodejs" to isToolInstalled("nodejs"),
            "python3" to isToolInstalled("python3"),
            "dash" to isToolInstalled("dash"),
            "git" to isToolInstalled("git"),
            "c/c++" to isToolInstalled("c/c++")
        )
        _installedTools.value = map
    }

    fun getSandboxDir(): File = File(context.filesDir, "sandbox").apply { if (!exists()) mkdirs() }
    fun getRootfsDir(): File = File(getSandboxDir(), "rootfs")
    fun getBinDir(): File = File(getSandboxDir(), "bin").apply { if (!exists()) mkdirs() }
    fun getBuildDir(): File = File(getSandboxDir(), "build").apply { if (!exists()) mkdirs() }
    fun getToolchainsDir(): File = File(getSandboxDir(), "toolchains").apply { if (!exists()) mkdirs() }

    fun getToolchainDir(toolId: String): File {
        val dirName = getOnDemandSpec(toolId)?.dirName ?: normalizeToolId(toolId).replace("/", "_")
        return File(getToolchainsDir(), dirName)
    }

    fun getToolchainLibDir(toolId: String): File = File(getToolchainDir(toolId), "lib")

    private fun hasNativeLibDir(): Boolean {
        val dir = try {
            context.applicationInfo?.nativeLibraryDir
        } catch (_: Throwable) {
            null
        }
        return !dir.isNullOrBlank()
    }

    private fun getToolchainExecutablePath(toolId: String, binaryName: String): String? {
        val candidate = File(getToolchainLibDir(toolId), binaryName)
        return if (candidate.exists()) candidate.absolutePath else null
    }

    fun getExecutablePath(toolId: String): String? {
        when (toolId.lowercase()) {
            "node", "nodejs", "nodejs & npm" -> {
                getToolchainExecutablePath("nodejs", "libnode.so")?.let { return it }
            }
            "clang", "clang++", "c", "cpp", "c++", "c/c++", "build-base" -> {
                getToolchainExecutablePath("c/c++", "libclang.so")?.let { return it }
            }
            "lld", "ld.lld" -> {
                getToolchainExecutablePath("c/c++", "ld.lld")?.let { return it }
                getToolchainExecutablePath("c/c++", "liblld.so")?.let { return it }
            }
        }

        val libName = when (toolId.lowercase()) {
            "dash", "sh" -> "libdash.so"
            "node", "nodejs", "nodejs & npm" -> "libnode.so"
            "python", "python3", "python 3 & pip" -> "libpython3.so"
            "clang", "clang++", "c", "cpp", "c++", "c/c++", "build-base" -> "libclang.so"
            "lld", "ld.lld" -> "liblld.so"
            "pty", "klix_pty" -> "libklix_pty.so"
            else -> "lib${toolId.lowercase()}.so"
        }
        val nativeDir = try {
            context.applicationInfo?.nativeLibraryDir
        } catch (_: Throwable) {
            null
        }
        if (!nativeDir.isNullOrBlank()) {
            val f = File(nativeDir, libName)
            if (f.exists()) {
                return f.absolutePath
            }
        }
        return null
    }

    fun isInstalled(): Boolean {
        val rootfs = getRootfsDir()
        if (!rootfs.exists()) return false
        val hasReleaseMarker = File(rootfs, "etc/klix-bionic-release").exists() ||
            File(rootfs, "bin/sh").exists()
        if (!hasReleaseMarker) return false
        return if (hasNativeLibDir()) {
            getExecutablePath("dash") != null || File(rootfs, "bin/sh").exists()
        } else {
            true
        }
    }

    fun isToolInstalled(toolId: String): Boolean {
        val rootfs = getRootfsDir()
        if (!rootfs.exists()) return false
        val nativeDirPresent = hasNativeLibDir()

        return when (toolId.lowercase()) {
            "node", "nodejs", "nodejs & npm" -> {
                if (nativeDirPresent) {
                    getExecutablePath("node") != null && isInstalled()
                } else {
                    getToolchainExecutablePath("nodejs", "libnode.so") != null ||
                        File(rootfs, "usr/bin/node").exists() ||
                        File(rootfs, "bin/node").exists()
                }
            }
            "python", "python3", "python 3 & pip" -> {
                val hasStdlib = File(rootfs, "usr/lib/python3.12").exists() ||
                    File(rootfs, "usr/lib/python3.13").exists() ||
                    File(rootfs, "usr/lib/python3.14").exists() ||
                    File(rootfs, "usr/lib/python3").exists()
                if (nativeDirPresent) {
                    getExecutablePath("python3") != null && hasStdlib
                } else {
                    File(rootfs, "usr/bin/python3").exists() ||
                        File(rootfs, "bin/python3").exists() ||
                        hasStdlib
                }
            }
            "build-base", "c/c++ build essentials", "c/c++", "cpp", "c++", "c" -> {
                val hasSysroot = File(rootfs, "usr/sysroot/usr/include/stdio.h").exists()
                if (nativeDirPresent) {
                    getExecutablePath("clang") != null &&
                        getExecutablePath("lld") != null &&
                        hasSysroot
                } else {
                    (getToolchainExecutablePath("c/c++", "libclang.so") != null && hasSysroot) ||
                        File(rootfs, "usr/bin/clang").exists() ||
                        File(rootfs, "usr/bin/gcc").exists()
                }
            }
            "dash", "sh", "dash shell" -> {
                if (nativeDirPresent) {
                    getExecutablePath("dash") != null && isInstalled()
                } else {
                    File(rootfs, "bin/sh").exists() || isInstalled()
                }
            }
            "git" -> {
                File(rootfs, "usr/bin/git").exists() || File(rootfs, "bin/git").exists()
            }
            else -> {
                if (nativeDirPresent) {
                    getExecutablePath(toolId) != null
                } else {
                    File(rootfs, "usr/bin/$toolId").exists() || File(rootfs, "bin/$toolId").exists()
                }
            }
        }
    }

    fun detectArch(): String {
        val abis = try { Build.SUPPORTED_ABIS } catch (_: Throwable) { null }
        if (abis != null && abis.isNotEmpty()) {
            for (abi in abis) {
                if (abi.startsWith("arm64")) return "aarch64"
            }
        }
        val osArch = (System.getProperty("os.arch", "aarch64") ?: "aarch64").lowercase()
        return if (osArch.contains("x86_64") || osArch.contains("amd64")) "x86_64" else "aarch64"
    }

    fun getToolchainEnv(workingDir: File): Map<String, String> {
        val rootfs = getRootfsDir()
        val binDir = getBinDir()
        val buildDir = getBuildDir()
        val nativeDir = try {
            context.applicationInfo?.nativeLibraryDir?.let { File(it).canonicalPath }
        } catch (_: Throwable) {
            null
        } ?: ""

        val nodeLibDir = getToolchainLibDir("nodejs").absolutePath
        val cppLibDir = getToolchainLibDir("c/c++").absolutePath
        val ldLibraryPath = listOfNotNull(
            nativeDir.takeIf { it.isNotEmpty() },
            nodeLibDir,
            cppLibDir
        ).distinct().joinToString(":")

        val clangTarget = "aarch64-linux-android26"
        val sysrootDir = File(rootfs, "usr/sysroot")
        val clangResourceDir = findClangResourceDir(rootfs)
        val pythonHome = File(rootfs, "usr")
        val pythonStdlib = findPythonStdlibDir(rootfs)
        val homeDir = File(rootfs, "root").apply { if (!exists()) mkdirs() }
        val tmpDir = File(rootfs, "tmp").apply { if (!exists()) mkdirs() }
        val sslCertFile = File(rootfs, "etc/ssl/certs/ca-certificates.crt")
        val envScript = File(rootfs, "etc/klix_env.sh")

        val pathEntries = listOfNotNull(
            binDir.absolutePath,
            nativeDir.takeIf { it.isNotEmpty() },
            "/system/bin",
            "/system/xbin"
        )

        return linkedMapOf(
            "PATH" to pathEntries.joinToString(":"),
            "LD_LIBRARY_PATH" to ldLibraryPath,
            "PYTHONHOME" to pythonHome.absolutePath,
            "PYTHONPATH" to pythonStdlib.absolutePath,
            "PYTHONUNBUFFERED" to "1",
            "PYTHONIOENCODING" to "utf-8",
            "SSL_CERT_FILE" to sslCertFile.absolutePath,
            "HOME" to homeDir.absolutePath,
            "TMPDIR" to tmpDir.absolutePath,
            "TERM" to "xterm-256color",
            "LANG" to "C.UTF-8",
            "PWD" to workingDir.absolutePath,
            "KLIX_NATIVE_LIB_DIR" to nativeDir,
            "KLIX_NODE_LIB_DIR" to nodeLibDir,
            "KLIX_CPP_LIB_DIR" to cppLibDir,
            "KLIX_BIN_DIR" to binDir.absolutePath,
            "KLIX_BUILD_DIR" to buildDir.absolutePath,
            "KLIX_SANDBOX_ROOT" to rootfs.absolutePath,
            "KLIX_SYSROOT" to sysrootDir.absolutePath,
            "KLIX_RESOURCE_DIR" to clangResourceDir.absolutePath,
            "KLIX_CLANG_TARGET" to clangTarget,
            "ENV" to envScript.absolutePath
        )
    }

    private fun findPythonStdlibDir(rootfs: File): File {
        val usrLib = File(rootfs, "usr/lib")
        val candidates = listOf("python3.12", "python3.13", "python3.14", "python3")
        for (name in candidates) {
            val f = File(usrLib, name)
            if (f.exists()) return f
        }
        return File(usrLib, "python3.12")
    }

    private fun findClangResourceDir(rootfs: File): File {
        val clangLibDir = File(rootfs, "usr/lib/clang")
        val child = clangLibDir.listFiles()?.firstOrNull { it.isDirectory }
        return child ?: File(clangLibDir, "21")
    }

    suspend fun installSandbox(): Result<Unit> = withContext(Dispatchers.IO) {
        if (_status.value == SandboxStatus.DOWNLOADING || _status.value == SandboxStatus.EXTRACTING) {
            return@withContext Result.failure(IllegalStateException("RootFS installation is already in progress"))
        }
        try {
            _status.value = SandboxStatus.EXTRACTING
            _progress.value = 0.1f
            _statusMessage.value = "Preparing bundled Python 3 runtime..."

            val sandboxDir = getSandboxDir()
            val tarGzFile = File(sandboxDir, "sandbox-data.tgz")
            context.assets.open("sandbox-data.tgz").use { input ->
                FileOutputStream(tarGzFile).use { output ->
                    val buffer = ByteArray(16384)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }

            _progress.value = 0.45f
            _statusMessage.value = "Extracting Python 3 stdlib & certificates..."

            val rootfsDir = getRootfsDir()
            // Preserve any already-downloaded on-demand C/C++ sysroot/clang headers if reinstalling base Python
            val existingSysroot = File(rootfsDir, "usr/sysroot")
            val existingClangLib = File(rootfsDir, "usr/lib/clang")
            val preservedDir = File(sandboxDir, ".preserved_cpp_usr")
            if (preservedDir.exists()) preservedDir.deleteRecursively()
            if (existingSysroot.exists() || existingClangLib.exists()) {
                preservedDir.mkdirs()
                if (existingSysroot.exists()) existingSysroot.copyRecursively(File(preservedDir, "sysroot"), overwrite = true)
                if (existingClangLib.exists()) existingClangLib.copyRecursively(File(preservedDir, "clang"), overwrite = true)
            }

            if (rootfsDir.exists()) {
                rootfsDir.deleteRecursively()
            }
            rootfsDir.mkdirs()

            tarGzExtractor.extract(tarGzFile, rootfsDir, allowAbsoluteSymlinks = false).getOrThrow()

            if (preservedDir.exists()) {
                val restoredSysroot = File(preservedDir, "sysroot")
                val restoredClang = File(preservedDir, "clang")
                if (restoredSysroot.exists()) {
                    restoredSysroot.copyRecursively(File(rootfsDir, "usr/sysroot"), overwrite = true)
                }
                if (restoredClang.exists()) {
                    restoredClang.copyRecursively(File(rootfsDir, "usr/lib/clang"), overwrite = true)
                }
                preservedDir.deleteRecursively()
            }

            _progress.value = 0.85f
            _statusMessage.value = "Configuring Bionic environment..."
            setupBasicEnvironment(rootfsDir)

            tarGzFile.delete()

            _status.value = SandboxStatus.READY
            _progress.value = 1.0f
            _statusMessage.value = "Python 3 Bionic sandbox ready"
            refreshInstalledTools()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _status.value = SandboxStatus.ERROR
            _statusMessage.value = "Installation error: ${e.message}"
            Result.failure(e)
        }
    }

    fun launchInstallToolchain(toolId: String, onComplete: ((Result<Unit>) -> Unit)? = null) {
        activeDownloadJob?.cancel()
        activeDownloadJob = managerScope.launch {
            val result = installToolchain(toolId)
            if (onComplete != null) {
                withContext(Dispatchers.Main) {
                    onComplete(result)
                }
            }
        }
    }

    fun cancelActiveDownload() {
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        val current = _activeDownload.value
        if (current != null &&
            current.phase != DownloadPhase.COMPLETED &&
            current.phase != DownloadPhase.FAILED
        ) {
            setToolInstalling(current.toolId, false)
            _activeDownload.value = null
        }
    }

    fun dismissDownloadBanner() {
        val current = _activeDownload.value ?: return
        if (current.phase == DownloadPhase.COMPLETED ||
            current.phase == DownloadPhase.FAILED ||
            current.phase == DownloadPhase.CONFIRMATION
        ) {
            _activeDownload.value = null
        }
    }

    suspend fun installToolchain(toolId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val canonical = normalizeToolId(toolId)
        if (canonical == "python3" || canonical == "dash") {
            if (!isInstalled()) {
                _activeDownload.value = ToolchainDownloadState(
                    toolId = canonical,
                    displayName = if (canonical == "python3") "Python 3" else "Dash Shell",
                    subtitle = "Built-in • Bundled offline",
                    phase = DownloadPhase.EXTRACTING,
                    progress = 0.25f,
                    statusMessage = "Extracting built-in Python 3 runtime..."
                )
                val res = installSandbox()
                if (res.isSuccess) {
                    _activeDownload.value = ToolchainDownloadState(
                        toolId = canonical,
                        displayName = if (canonical == "python3") "Python 3" else "Dash Shell",
                        subtitle = "Built-in • Bundled offline",
                        phase = DownloadPhase.COMPLETED,
                        progress = 1f,
                        statusMessage = "Ready!"
                    )
                    delay(1200)
                    if (_activeDownload.value?.phase == DownloadPhase.COMPLETED) {
                        _activeDownload.value = null
                    }
                } else {
                    _activeDownload.value = ToolchainDownloadState(
                        toolId = canonical,
                        displayName = if (canonical == "python3") "Python 3" else "Dash Shell",
                        subtitle = "Built-in • Bundled offline",
                        phase = DownloadPhase.FAILED,
                        progress = 0f,
                        statusMessage = "Setup failed",
                        errorMessage = res.exceptionOrNull()?.message ?: "Failed to extract built-in runtime"
                    )
                }
                return@withContext res
            }
            refreshInstalledTools()
            return@withContext Result.success(Unit)
        }

        val spec = getOnDemandSpec(canonical)
            ?: return@withContext Result.failure(IllegalArgumentException("Unsupported on-demand toolchain: $toolId"))

        if (_installingTools.value.contains(canonical)) {
            return@withContext Result.failure(IllegalStateException("${spec.displayName} installation is already in progress"))
        }

        setToolInstalling(canonical, true)
        val downloadFile = File(getSandboxDir(), "${spec.archiveFileName}.part")
        val stagingDir = File(getSandboxDir(), ".staging_${spec.dirName}")

        try {
            if (!isInstalled()) {
                _activeDownload.value = ToolchainDownloadState(
                    toolId = spec.id,
                    displayName = spec.displayName,
                    subtitle = spec.subtitle,
                    phase = DownloadPhase.PREPARING,
                    progress = 0.04f,
                    totalBytes = spec.expectedBytes,
                    statusMessage = "Preparing base Bionic sandbox..."
                )
                installSandbox().getOrThrow()
            }

            _activeDownload.value = ToolchainDownloadState(
                toolId = spec.id,
                displayName = spec.displayName,
                subtitle = spec.subtitle,
                phase = DownloadPhase.DOWNLOADING,
                progress = 0.05f,
                downloadedBytes = 0L,
                totalBytes = spec.expectedBytes,
                statusMessage = "Connecting to release mirror..."
            )

            val request = Request.Builder().url(spec.downloadUrl).build()
            val response = httpClient.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Download failed (HTTP ${resp.code})")
                }
                val body = resp.body ?: throw IllegalStateException("Empty download response")
                val totalBytes = body.contentLength().takeIf { it > 0L } ?: spec.expectedBytes

                if (downloadFile.exists()) downloadFile.delete()
                var downloaded = 0L
                var lastEmitTime = 0L
                val buffer = ByteArray(32 * 1024)

                body.byteStream().use { input ->
                    FileOutputStream(downloadFile).use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read

                            val now = System.currentTimeMillis()
                            if (now - lastEmitTime >= 80L) {
                                lastEmitTime = now
                                val dlFraction = (downloaded.toFloat() / totalBytes.coerceAtLeast(1L).toFloat())
                                    .coerceIn(0f, 1f)
                                _activeDownload.value = ToolchainDownloadState(
                                    toolId = spec.id,
                                    displayName = spec.displayName,
                                    subtitle = spec.subtitle,
                                    phase = DownloadPhase.DOWNLOADING,
                                    progress = 0.05f + dlFraction * 0.70f,
                                    downloadedBytes = downloaded,
                                    totalBytes = totalBytes,
                                    statusMessage = "Downloading ${spec.displayName}..."
                                )
                            }
                        }
                        output.flush()
                    }
                }

                // Verify SHA-256
                _activeDownload.value = ToolchainDownloadState(
                    toolId = spec.id,
                    displayName = spec.displayName,
                    subtitle = spec.subtitle,
                    phase = DownloadPhase.VERIFYING,
                    progress = 0.78f,
                    downloadedBytes = downloaded,
                    totalBytes = downloaded,
                    statusMessage = "Verifying SHA-256 integrity..."
                )

                if (spec.expectedSha256.isNotBlank()) {
                    val actualSha = computeSha256(downloadFile)
                    if (!actualSha.equals(spec.expectedSha256, ignoreCase = true)) {
                        throw SecurityException("Checksum mismatch for ${spec.archiveFileName}")
                    }
                }

                // Extract into staging dir first, then place lib/ and optional usr/ cleanly
                _activeDownload.value = ToolchainDownloadState(
                    toolId = spec.id,
                    displayName = spec.displayName,
                    subtitle = spec.subtitle,
                    phase = DownloadPhase.EXTRACTING,
                    progress = 0.85f,
                    downloadedBytes = downloaded,
                    totalBytes = downloaded,
                    statusMessage = "Extracting ${spec.displayName} (16 KB Bionic)..."
                )

                if (stagingDir.exists()) stagingDir.deleteRecursively()
                stagingDir.mkdirs()
                tarGzExtractor.extract(downloadFile, stagingDir, allowAbsoluteSymlinks = false).getOrThrow()

                val targetDir = getToolchainDir(spec.id)
                if (targetDir.exists()) targetDir.deleteRecursively()
                targetDir.mkdirs()

                val stagedLib = File(stagingDir, "lib")
                if (stagedLib.exists()) {
                    moveOrCopyTree(stagedLib, File(targetDir, "lib"))
                }
                val stagedUsr = File(stagingDir, "usr")
                if (stagedUsr.exists()) {
                    moveOrCopyTree(stagedUsr, File(getRootfsDir(), "usr"))
                }

                _activeDownload.value = ToolchainDownloadState(
                    toolId = spec.id,
                    displayName = spec.displayName,
                    subtitle = spec.subtitle,
                    phase = DownloadPhase.EXTRACTING,
                    progress = 0.96f,
                    downloadedBytes = downloaded,
                    totalBytes = downloaded,
                    statusMessage = "Configuring shell wrappers..."
                )

                setupBasicEnvironment(getRootfsDir())
                refreshInstalledTools()

                if (!isToolInstalled(spec.id)) {
                    throw IllegalStateException("${spec.displayName} extracted but verification failed")
                }

                _activeDownload.value = ToolchainDownloadState(
                    toolId = spec.id,
                    displayName = spec.displayName,
                    subtitle = spec.subtitle,
                    phase = DownloadPhase.COMPLETED,
                    progress = 1.0f,
                    downloadedBytes = downloaded,
                    totalBytes = downloaded,
                    statusMessage = "${spec.displayName} ready!"
                )
            }

            delay(1200)
            if (_activeDownload.value?.toolId == spec.id &&
                _activeDownload.value?.phase == DownloadPhase.COMPLETED
            ) {
                _activeDownload.value = null
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            if (_activeDownload.value?.toolId == spec.id) {
                _activeDownload.value = null
            }
            throw e
        } catch (e: Exception) {
            val msg = e.message ?: "Failed to install ${spec.displayName}"
            _activeDownload.value = ToolchainDownloadState(
                toolId = spec.id,
                displayName = spec.displayName,
                subtitle = spec.subtitle,
                phase = DownloadPhase.FAILED,
                progress = 0f,
                statusMessage = "Download failed",
                errorMessage = msg
            )
            Result.failure(e)
        } finally {
            if (downloadFile.exists()) downloadFile.delete()
            if (stagingDir.exists()) stagingDir.deleteRecursively()
            setToolInstalling(canonical, false)
        }
    }

    private fun moveOrCopyTree(src: File, dest: File) {
        if (!dest.exists()) dest.mkdirs()
        val children = src.listFiles() ?: return
        for (child in children) {
            val target = File(dest, child.name)
            if (isSymlink(child)) {
                val linkTarget = Os.readlink(child.absolutePath)
                if (target.exists() || isSymlink(target)) target.delete()
                Os.symlink(linkTarget, target.absolutePath)
            } else if (child.isDirectory) {
                moveOrCopyTree(child, target)
            } else {
                if (target.exists() || isSymlink(target)) target.delete()
                if (!child.renameTo(target)) {
                    child.copyTo(target, overwrite = true)
                    target.setExecutable(child.canExecute(), false)
                }
            }
        }
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(32 * 1024)
            while (true) {
                val read = input.read(buf)
                if (read == -1) break
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun launchInstallAll(onComplete: ((Result<Unit>) -> Unit)? = null) {
        activeDownloadJob?.cancel()
        activeDownloadJob = managerScope.launch {
            val result = installAll()
            if (onComplete != null) {
                withContext(Dispatchers.Main) {
                    onComplete(result)
                }
            }
        }
    }

    suspend fun installAll(): Result<Unit> = withContext(Dispatchers.IO) {
        if (_isInstallingAll.value ||
            _status.value == SandboxStatus.DOWNLOADING ||
            _status.value == SandboxStatus.EXTRACTING
        ) {
            return@withContext Result.failure(IllegalStateException("Installation is already in progress"))
        }

        _isInstallingAll.value = true
        _installAllProgress.value = 0.05f
        _installAllMessage.value = "Preparing Python 3 Bionic sandbox..."

        try {
            val needsRootFs = !isInstalled() || _status.value != SandboxStatus.READY
            if (needsRootFs) {
                val monitorJob = launch {
                    while (true) {
                        val currentProgress = _progress.value
                        val currentMessage = _statusMessage.value
                        _installAllProgress.value = (0.05f + currentProgress * 0.25f).coerceIn(0.05f, 0.30f)
                        if (currentMessage.isNotBlank()) {
                            _installAllMessage.value = currentMessage
                        }
                        delay(80)
                    }
                }
                val rootResult = try {
                    installSandbox()
                } finally {
                    monitorJob.cancel()
                }

                if (rootResult.isFailure) {
                    val err = rootResult.exceptionOrNull()?.message ?: "Failed to install Python 3 sandbox"
                    _installAllMessage.value = err
                    return@withContext Result.failure(rootResult.exceptionOrNull() ?: IllegalStateException(err))
                }
            }

            refreshInstalledTools()
            val missingOnDemand = onDemandToolOrder.filter { !isToolInstalled(it) }
            if (missingOnDemand.isNotEmpty()) {
                val failedTools = mutableListOf<String>()
                for ((index, toolId) in missingOnDemand.withIndex()) {
                    val spec = getOnDemandSpec(toolId) ?: continue
                    val baseProgress = 0.30f + 0.70f * (index.toFloat() / missingOnDemand.size)
                    val span = 0.70f / missingOnDemand.size

                    val monitorJob = launch {
                        while (true) {
                            val dl = _activeDownload.value
                            if (dl != null && dl.toolId == toolId) {
                                _installAllProgress.value = (baseProgress + dl.progress * span).coerceIn(0.30f, 0.99f)
                                if (dl.statusMessage.isNotBlank()) {
                                    _installAllMessage.value = dl.statusMessage
                                }
                            }
                            delay(80)
                        }
                    }
                    val res = try {
                        installToolchain(toolId)
                    } finally {
                        monitorJob.cancel()
                    }
                    if (res.isFailure || !isToolInstalled(toolId)) {
                        failedTools.add(spec.displayName)
                    }
                }

                if (failedTools.isNotEmpty()) {
                    val msg = "Failed to download: ${failedTools.joinToString(", ")}"
                    _installAllMessage.value = msg
                    return@withContext Result.failure(IllegalStateException(msg))
                }
            }

            _installAllProgress.value = 1.0f
            _installAllMessage.value = "All runtimes ready"
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = e.message ?: "Sandbox setup failed"
            _installAllMessage.value = msg
            Result.failure(e)
        } finally {
            onDemandToolOrder.forEach { setToolInstalling(it, false) }
            _isInstallingAll.value = false
        }
    }

    internal fun setupBasicEnvironment(rootfsDir: File) {
        val etcDir = File(rootfsDir, "etc").apply { if (!exists()) mkdirs() }
        File(etcDir, "klix-bionic-release").writeText("klix-bionic-16kb=1\n")

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

        val tmpDir = File(rootfsDir, "tmp").apply { if (!exists()) mkdirs() }
        tmpDir.setReadable(true, false)
        tmpDir.setWritable(true, false)
        tmpDir.setExecutable(true, false)

        File(rootfsDir, "root").apply { if (!exists()) mkdirs() }
        File(rootfsDir, "workspaces/default").apply { if (!exists()) mkdirs() }
        getBuildDir()

        val binDir = getBinDir()
        // Bundled executables inside nativeLibraryDir can be directly symlinked
        val baseLinkPairs = listOf(
            "dash" to "dash",
            "sh" to "dash",
            "python3" to "python3",
            "python" to "python3"
        )
        for ((linkName, toolId) in baseLinkPairs) {
            val targetPath = getExecutablePath(toolId) ?: continue
            val linkFile = File(binDir, linkName)
            try {
                if (linkFile.exists() || isSymlink(linkFile)) {
                    linkFile.delete()
                }
                Os.symlink(targetPath, linkFile.absolutePath)
            } catch (_: Throwable) {}
        }

        val dashPath = getExecutablePath("dash")

        // On-demand Node.js wrapper via /system/bin/linker64 (avoids W^X execve denial on app_data_file)
        val nodePath = getExecutablePath("node")
        val nodeFile = File(binDir, "node")
        if (nodePath != null) {
            writeDashLinkerWrapper(
                targetFile = nodeFile,
                dashPath = dashPath,
                binaryPathExpr = "\"\${KLIX_NODE_LIB_DIR}/libnode.so\"",
                extraArgs = "",
                directTargetFallback = nodePath
            )
        }

        // On-demand Clang / Clang++ / LLD wrappers via /system/bin/linker64
        val lldPath = getExecutablePath("lld")
        if (lldPath != null) {
            val lldBinaryExpr = "\"\${KLIX_CPP_LIB_DIR}/ld.lld\""
            writeDashLinkerWrapper(File(binDir, "ld.lld"), dashPath, lldBinaryExpr, "", lldPath)
            writeDashLinkerWrapper(File(binDir, "lld"), dashPath, lldBinaryExpr, "", lldPath)
        }

        val clangPath = getExecutablePath("clang")
        if (clangPath != null) {
            val clangCommonFlags = """
                --target="${'$'}KLIX_CLANG_TARGET" \
                    --sysroot="${'$'}KLIX_SYSROOT" \
                    -resource-dir "${'$'}KLIX_RESOURCE_DIR" \
                    -B "${'$'}KLIX_BIN_DIR" \
                    -fuse-ld=lld \
                    -rtlib=compiler-rt \
                    -Wl,-z,max-page-size=16384 \
                    -L"${'$'}KLIX_NATIVE_LIB_DIR" \
                    -L"${'$'}KLIX_CPP_LIB_DIR"
            """.trimIndent()
            writeDashLinkerWrapper(
                targetFile = File(binDir, "clang"),
                dashPath = dashPath,
                binaryPathExpr = "\"\${KLIX_CPP_LIB_DIR}/libclang.so\"",
                extraArgs = clangCommonFlags,
                directTargetFallback = clangPath
            )
            writeDashLinkerWrapper(
                targetFile = File(binDir, "clang++"),
                dashPath = dashPath,
                binaryPathExpr = "\"\${KLIX_CPP_LIB_DIR}/libclang.so\"",
                extraArgs = "--driver-mode=g++ -stdlib=libc++ \\\n    $clangCommonFlags",
                directTargetFallback = clangPath
            )
        }

        val envScript = File(etcDir, "klix_env.sh")
        envScript.writeText(
            """
            # KlixCode Bionic 16 KB runtime environment (Python 3 bundled, Node.js & Clang on-demand)
            node() {
                /system/bin/linker64 "${'$'}KLIX_NODE_LIB_DIR/libnode.so" "${'$'}@"
            }
            python3() {
                "${'$'}KLIX_NATIVE_LIB_DIR/libpython3.so" "${'$'}@"
            }
            python() {
                "${'$'}KLIX_NATIVE_LIB_DIR/libpython3.so" "${'$'}@"
            }
            clang() {
                /system/bin/linker64 "${'$'}KLIX_CPP_LIB_DIR/libclang.so" \
                    --target="${'$'}KLIX_CLANG_TARGET" \
                    --sysroot="${'$'}KLIX_SYSROOT" \
                    -resource-dir "${'$'}KLIX_RESOURCE_DIR" \
                    -B "${'$'}KLIX_BIN_DIR" \
                    -fuse-ld=lld \
                    -rtlib=compiler-rt \
                    -Wl,-z,max-page-size=16384 \
                    -L"${'$'}KLIX_NATIVE_LIB_DIR" \
                    -L"${'$'}KLIX_CPP_LIB_DIR" \
                    "${'$'}@"
            }
            clangxx() {
                /system/bin/linker64 "${'$'}KLIX_CPP_LIB_DIR/libclang.so" --driver-mode=g++ \
                    --target="${'$'}KLIX_CLANG_TARGET" \
                    --sysroot="${'$'}KLIX_SYSROOT" \
                    -resource-dir "${'$'}KLIX_RESOURCE_DIR" \
                    -B "${'$'}KLIX_BIN_DIR" \
                    -fuse-ld=lld \
                    -rtlib=compiler-rt \
                    -stdlib=libc++ \
                    -Wl,-z,max-page-size=16384 \
                    -L"${'$'}KLIX_NATIVE_LIB_DIR" \
                    -L"${'$'}KLIX_CPP_LIB_DIR" \
                    "${'$'}@"
            }
            alias clang++='clangxx'
            """.trimIndent() + "\n"
        )
    }

    private fun writeDashLinkerWrapper(
        targetFile: File,
        dashPath: String?,
        binaryPathExpr: String,
        extraArgs: String,
        directTargetFallback: String
    ) {
        try {
            if (targetFile.exists() || isSymlink(targetFile)) {
                targetFile.delete()
            }
            if (dashPath != null) {
                val argsBlock = if (extraArgs.isNotBlank()) " $extraArgs" else ""
                targetFile.writeText(
                    """
                    #!$dashPath
                    exec /system/bin/linker64 $binaryPathExpr$argsBlock "${'$'}@"
                    """.trimIndent() + "\n"
                )
                Os.chmod(targetFile.absolutePath, 0b111_101_101)
            } else {
                Os.symlink(directTargetFallback, targetFile.absolutePath)
            }
        } catch (_: Throwable) {}
    }

    private fun isSymlink(file: File): Boolean {
        return try {
            val lstat = Os.lstat(file.absolutePath)
            android.system.OsConstants.S_ISLNK(lstat.st_mode)
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun extractTarGz(tarGzFile: File, destDir: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            tarGzExtractor.extract(tarGzFile, destDir, allowAbsoluteSymlinks = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun resetSandbox(): Boolean {
        return try {
            cancelActiveDownload()
            val sandboxDir = File(context.filesDir, "sandbox")
            if (sandboxDir.exists()) {
                sandboxDir.deleteRecursively()
            }
            _status.value = SandboxStatus.NOT_INSTALLED
            _statusMessage.value = "Sandbox not installed"
            _progress.value = 0f
            _isInstallingAll.value = false
            _installAllProgress.value = 0f
            _installAllMessage.value = null
            _activeDownload.value = null
            refreshInstalledTools()
            true
        } catch (_: Exception) {
            false
        }
    }
}
