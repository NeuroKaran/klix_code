package com.klix.code.data.sandbox

import android.content.Context
import android.os.Build
import android.system.Os
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

enum class SandboxStatus {
    NOT_INSTALLED,
    DOWNLOADING,
    EXTRACTING,
    READY,
    ERROR
}

class SandboxManager(private val context: Context) {

    private val _status = MutableStateFlow(if (isInstalled()) SandboxStatus.READY else SandboxStatus.NOT_INSTALLED)
    val status: StateFlow<SandboxStatus> = _status.asStateFlow()

    private val _statusMessage = MutableStateFlow(
        if (isInstalled()) "Bionic 16 KB sandbox ready" else "Sandbox not installed"
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

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    internal var tarGzExtractor: TarGzExtractor = TarGzExtractor()

    private data class PreconfiguredToolchain(
        val id: String,
        val label: String,
        val verifyCommand: String
    )

    private val preconfiguredToolchains = listOf(
        PreconfiguredToolchain("nodejs", "Node.js", "node --version"),
        PreconfiguredToolchain("python3", "Python 3", "python3 --version"),
        PreconfiguredToolchain("c/c++", "C/C++", "clang --version")
    )

    init {
        refreshInstalledTools()
    }

    fun setToolInstalling(toolId: String, installing: Boolean) {
        val current = _installingTools.value.toMutableSet()
        if (installing) {
            current.add(toolId.lowercase())
        } else {
            current.remove(toolId.lowercase())
        }
        _installingTools.value = current
    }

    fun refreshInstalledTools() {
        val rootfs = getRootfsDir()
        if (!rootfs.exists()) {
            _installedTools.value = mapOf(
                "nodejs" to false,
                "python3" to false,
                "git" to false,
                "c/c++" to false
            )
            return
        }

        val map = mapOf(
            "nodejs" to isToolInstalled("nodejs"),
            "python3" to isToolInstalled("python3"),
            "git" to isToolInstalled("git"),
            "c/c++" to isToolInstalled("c/c++")
        )
        _installedTools.value = map
    }

    fun getSandboxDir(): File = File(context.filesDir, "sandbox").apply { if (!exists()) mkdirs() }
    fun getRootfsDir(): File = File(getSandboxDir(), "rootfs")
    fun getBinDir(): File = File(getSandboxDir(), "bin").apply { if (!exists()) mkdirs() }
    fun getBuildDir(): File = File(getSandboxDir(), "build").apply { if (!exists()) mkdirs() }

    private fun hasNativeLibDir(): Boolean {
        val dir = try {
            context.applicationInfo?.nativeLibraryDir
        } catch (_: Throwable) {
            null
        }
        return !dir.isNullOrBlank()
    }

    fun getExecutablePath(toolId: String): String? {
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
                    File(rootfs, "usr/bin/clang").exists() ||
                        File(rootfs, "usr/bin/gcc").exists() ||
                        hasSysroot
                }
            }
            "dash", "sh" -> {
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
                when {
                    abi.startsWith("arm64") -> return "aarch64"
                    abi.startsWith("x86_64") -> return "x86_64"
                }
            }
        }
        val osArch = (System.getProperty("os.arch", "aarch64") ?: "aarch64").lowercase()
        return when {
            osArch.contains("aarch64") || osArch.contains("arm64") -> "aarch64"
            osArch.contains("x86_64") || osArch.contains("amd64") -> "x86_64"
            else -> "aarch64"
        }
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

        val arch = detectArch()
        val clangTarget = if (arch == "x86_64") "x86_64-linux-android26" else "aarch64-linux-android26"
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
            "LD_LIBRARY_PATH" to nativeDir,
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
            _statusMessage.value = "Preparing bundled Bionic 16 KB runtime..."

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

            _progress.value = 0.4f
            _statusMessage.value = "Extracting Bionic stdlib & sysroot..."

            val rootfsDir = getRootfsDir()
            if (rootfsDir.exists()) {
                rootfsDir.deleteRecursively()
            }
            rootfsDir.mkdirs()

            tarGzExtractor.extract(tarGzFile, rootfsDir, allowAbsoluteSymlinks = false).getOrThrow()

            _progress.value = 0.85f
            _statusMessage.value = "Configuring Bionic environment..."
            setupBasicEnvironment(rootfsDir)

            tarGzFile.delete()

            _status.value = SandboxStatus.READY
            _progress.value = 1.0f
            _statusMessage.value = "Bionic 16 KB sandbox ready"
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

    fun launchInstallAll(onComplete: ((Result<Unit>) -> Unit)? = null) {
        managerScope.launch {
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
        _installAllMessage.value = "Preparing Bionic 16 KB sandbox..."

        try {
            val needsRootFs = !isInstalled() || _status.value != SandboxStatus.READY
            if (needsRootFs) {
                val monitorJob = launch {
                    while (true) {
                        val currentProgress = _progress.value
                        val currentMessage = _statusMessage.value
                        _installAllProgress.value = (0.05f + currentProgress * 0.65f).coerceIn(0.05f, 0.70f)
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
                    val err = rootResult.exceptionOrNull()?.message ?: "Failed to install Bionic sandbox"
                    _installAllMessage.value = err
                    return@withContext Result.failure(rootResult.exceptionOrNull() ?: IllegalStateException(err))
                }
            }

            refreshInstalledTools()
            val missingTools = preconfiguredToolchains.filter { !isToolInstalled(it.id) }
            if (missingTools.isNotEmpty()) {
                val executionProvider = SandboxExecutionProvider(context, this@SandboxManager)
                val failedTools = mutableListOf<String>()

                for ((index, tool) in missingTools.withIndex()) {
                    setToolInstalling(tool.id, true)
                    _installAllMessage.value = "Verifying ${tool.label} (${index + 1}/${missingTools.size})..."
                    _installAllProgress.value = 0.70f + 0.30f * (index.toFloat() / missingTools.size)

                    val cmdResult = try {
                        executionProvider.runCommand(tool.verifyCommand)
                    } finally {
                        setToolInstalling(tool.id, false)
                    }

                    refreshInstalledTools()
                    if (!cmdResult.isSuccess || !isToolInstalled(tool.id)) {
                        failedTools.add(tool.label)
                    }
                }

                if (failedTools.isNotEmpty()) {
                    val msg = "Failed to verify: ${failedTools.joinToString(", ")}"
                    _installAllMessage.value = msg
                    return@withContext Result.failure(IllegalStateException(msg))
                }
            }

            _installAllProgress.value = 1.0f
            _installAllMessage.value = "All Bionic toolchains ready"
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = e.message ?: "Sandbox setup failed"
            _installAllMessage.value = msg
            Result.failure(e)
        } finally {
            preconfiguredToolchains.forEach { setToolInstalling(it.id, false) }
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

        // Create symlinks inside sandbox/bin -> nativeLibraryDir/lib*.so
        val binDir = getBinDir()
        val linkPairs = listOf(
            "dash" to "dash",
            "sh" to "dash",
            "node" to "node",
            "python3" to "python3",
            "python" to "python3",
            "clang" to "clang",
            "lld" to "lld",
            "ld.lld" to "lld"
        )
        for ((linkName, toolId) in linkPairs) {
            val targetPath = getExecutablePath(toolId) ?: continue
            val linkFile = File(binDir, linkName)
            try {
                if (linkFile.exists() || isSymlink(linkFile)) {
                    linkFile.delete()
                }
                Os.symlink(targetPath, linkFile.absolutePath)
            } catch (_: Throwable) {}
        }

        // Create an executable dash wrapper for clang++ (POSIX shells forbid '+' in function names,
        // and wrapper script ensures C++ driver flags work both in PATH subprocesses and dash).
        val dashPath = getExecutablePath("dash")
        val clangPath = getExecutablePath("clang")
        val clangXxFile = File(binDir, "clang++")
        if (clangPath != null) {
            try {
                if (clangXxFile.exists() || isSymlink(clangXxFile)) {
                    clangXxFile.delete()
                }
                if (dashPath != null) {
                    clangXxFile.writeText(
                        """
                        #!$dashPath
                        exec "${'$'}KLIX_NATIVE_LIB_DIR/libclang.so" --driver-mode=g++ \
                            --target="${'$'}KLIX_CLANG_TARGET" \
                            --sysroot="${'$'}KLIX_SYSROOT" \
                            -resource-dir "${'$'}KLIX_RESOURCE_DIR" \
                            -B "${'$'}KLIX_BIN_DIR" \
                            -fuse-ld=lld \
                            -rtlib=compiler-rt \
                            -stdlib=libc++ \
                            -Wl,-z,max-page-size=16384 \
                            -L"${'$'}KLIX_NATIVE_LIB_DIR" \
                            "${'$'}@"
                        """.trimIndent() + "\n"
                    )
                    Os.chmod(clangXxFile.absolutePath, 0b111_101_101)
                } else {
                    Os.symlink(clangPath, clangXxFile.absolutePath)
                }
            } catch (_: Throwable) {}
        }

        val envScript = File(etcDir, "klix_env.sh")
        envScript.writeText(
            """
            # KlixCode Bionic 16 KB runtime environment
            node() {
                "${'$'}KLIX_NATIVE_LIB_DIR/libnode.so" "${'$'}@"
            }
            python3() {
                "${'$'}KLIX_NATIVE_LIB_DIR/libpython3.so" "${'$'}@"
            }
            python() {
                "${'$'}KLIX_NATIVE_LIB_DIR/libpython3.so" "${'$'}@"
            }
            clang() {
                "${'$'}KLIX_NATIVE_LIB_DIR/libclang.so" \
                    --target="${'$'}KLIX_CLANG_TARGET" \
                    --sysroot="${'$'}KLIX_SYSROOT" \
                    -resource-dir "${'$'}KLIX_RESOURCE_DIR" \
                    -B "${'$'}KLIX_BIN_DIR" \
                    -fuse-ld=lld \
                    -rtlib=compiler-rt \
                    -Wl,-z,max-page-size=16384 \
                    -L"${'$'}KLIX_NATIVE_LIB_DIR" \
                    "${'$'}@"
            }
            clangxx() {
                "${'$'}KLIX_NATIVE_LIB_DIR/libclang.so" --driver-mode=g++ \
                    --target="${'$'}KLIX_CLANG_TARGET" \
                    --sysroot="${'$'}KLIX_SYSROOT" \
                    -resource-dir "${'$'}KLIX_RESOURCE_DIR" \
                    -B "${'$'}KLIX_BIN_DIR" \
                    -fuse-ld=lld \
                    -rtlib=compiler-rt \
                    -stdlib=libc++ \
                    -Wl,-z,max-page-size=16384 \
                    -L"${'$'}KLIX_NATIVE_LIB_DIR" \
                    "${'$'}@"
            }
            alias clang++='clangxx'
            """.trimIndent() + "\n"
        )
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
            refreshInstalledTools()
            true
        } catch (_: Exception) {
            false
        }
    }
}
