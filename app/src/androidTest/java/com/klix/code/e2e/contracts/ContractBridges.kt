package com.klix.code.e2e.contracts

import com.klix.code.data.model.EditorTab
import com.klix.code.data.sandbox.SandboxExecutionProvider
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.workspace.SandboxWorkspace
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Assert.fail
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Fail-loud reflection bridges for M1-M4 interface contracts defined in PROJECT.md.
 *
 * Allows androidTest to compile cleanly prior to M4 while strictly failing at runtime
 * (AssertionError via fail(), NEVER Assume-skip) if any contract class, method, or
 * return type is missing or malformed.
 */
object ContractBridges {

    data class RunResolutionSnapshot(
        val command: String?,
        val errorMessage: String?,
        val missingToolId: String?,
        val requiresSetup: Boolean
    )

    data class PtySessionSnapshot(
        val pid: Int,
        val masterFd: Int
    )

    private fun resolveObjectOrInstance(className: String): Pair<Class<*>, Any?> {
        val clazz = try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            fail("Missing contract class '$className': ${e.message}")
            throw AssertionError()
        }
        val instance = try {
            clazz.getField("INSTANCE").get(null)
        } catch (_: NoSuchFieldException) {
            try {
                clazz.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
            } catch (_: Throwable) {
                null
            }
        }
        return clazz to instance
    }

    fun resolveRunCommand(
        tab: EditorTab,
        workspace: SandboxWorkspace?,
        sandboxManager: SandboxManager
    ): RunResolutionSnapshot {
        val (clazz, instance) = resolveObjectOrInstance("com.klix.code.data.sandbox.LanguageRunner")
        val method = clazz.methods.firstOrNull { m ->
            m.name == "resolveRunCommand" && m.parameterTypes.size == 3
        } ?: run {
            fail("Missing public LanguageRunner.resolveRunCommand(EditorTab, SandboxWorkspace?, SandboxManager)")
            throw AssertionError()
        }

        val resultObj = try {
            method.invoke(instance, tab, workspace, sandboxManager)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        } ?: run {
            fail("LanguageRunner.resolveRunCommand returned null")
            throw AssertionError()
        }

        val resClass = resultObj.javaClass
        val command = invokeGetter(resClass, resultObj, "getCommand") as? String
        val errorMessage = invokeGetter(resClass, resultObj, "getErrorMessage") as? String
        val missingToolId = invokeGetter(resClass, resultObj, "getMissingToolId") as? String
        val requiresSetup = (invokeGetter(resClass, resultObj, "getRequiresSetup") as? Boolean)
            ?: run {
                fail("RunResolution.requiresSetup is missing or not Boolean on ${resClass.name}")
                false
            }

        return RunResolutionSnapshot(
            command = command,
            errorMessage = errorMessage,
            missingToolId = missingToolId,
            requiresSetup = requiresSetup
        )
    }

    private fun invokeGetter(clazz: Class<*>, target: Any, getterName: String): Any? {
        val m = clazz.methods.firstOrNull { it.name == getterName && it.parameterCount == 0 }
            ?: run {
                fail("Missing getter '$getterName' on ${clazz.name}")
                throw AssertionError()
            }
        return try {
            m.invoke(target)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    fun openAndForkPty(
        argv: Array<String>,
        envp: Array<String>,
        cwd: String,
        rows: Int,
        cols: Int
    ): PtySessionSnapshot {
        val (clazz, instance) = resolveObjectOrInstance("com.klix.code.data.sandbox.PtyBridge")
        val method = clazz.methods.firstOrNull { m ->
            m.name == "openAndForkPty" && m.parameterCount == 5
        } ?: run {
            fail("Missing public PtyBridge.openAndForkPty(argv, envp, cwd, rows, cols)")
            throw AssertionError()
        }

        val sessionObj = try {
            method.invoke(instance, argv, envp, cwd, rows, cols)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        } ?: run {
            fail("PtyBridge.openAndForkPty returned null")
            throw AssertionError()
        }

        val sClass = sessionObj.javaClass
        val pid = (invokeGetter(sClass, sessionObj, "getPid") as? Number)?.toInt()
            ?: run {
                fail("PtySession.pid missing or not numeric")
                -1
            }
        val masterFd = (invokeGetter(sClass, sessionObj, "getMasterFd") as? Number)?.toInt()
            ?: run {
                fail("PtySession.masterFd missing or not numeric")
                -1
            }
        return PtySessionSnapshot(pid = pid, masterFd = masterFd)
    }

    fun readPty(masterFd: Int, buffer: ByteArray): Int {
        val (clazz, instance) = resolveObjectOrInstance("com.klix.code.data.sandbox.PtyBridge")
        val method = clazz.methods.firstOrNull { it.name == "readPty" && it.parameterCount == 2 }
            ?: run {
                fail("Missing public PtyBridge.readPty(masterFd, buffer)")
                throw AssertionError()
            }
        return try {
            (method.invoke(instance, masterFd, buffer) as Number).toInt()
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    fun writePty(masterFd: Int, data: ByteArray): Int {
        val (clazz, instance) = resolveObjectOrInstance("com.klix.code.data.sandbox.PtyBridge")
        val method = clazz.methods.firstOrNull { it.name == "writePty" && it.parameterCount == 2 }
            ?: run {
                fail("Missing public PtyBridge.writePty(masterFd, data)")
                throw AssertionError()
            }
        return try {
            (method.invoke(instance, masterFd, data) as Number).toInt()
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    fun resizePty(masterFd: Int, rows: Int, cols: Int): Int {
        val (clazz, instance) = resolveObjectOrInstance("com.klix.code.data.sandbox.PtyBridge")
        val method = clazz.methods.firstOrNull { it.name == "resizePty" && it.parameterCount == 3 }
            ?: run {
                fail("Missing public PtyBridge.resizePty(masterFd, rows, cols)")
                throw AssertionError()
            }
        return try {
            (method.invoke(instance, masterFd, rows, cols) as Number).toInt()
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    fun sendPtySignal(pid: Int, signal: Int): Int {
        val (clazz, instance) = resolveObjectOrInstance("com.klix.code.data.sandbox.PtyBridge")
        val method = clazz.methods.firstOrNull { it.name == "sendSignal" && it.parameterCount == 2 }
            ?: run {
                fail("Missing public PtyBridge.sendSignal(pid, signal)")
                throw AssertionError()
            }
        return try {
            (method.invoke(instance, pid, signal) as Number).toInt()
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    fun waitPtyPid(pid: Int): Int {
        val (clazz, instance) = resolveObjectOrInstance("com.klix.code.data.sandbox.PtyBridge")
        val method = clazz.methods.firstOrNull { it.name == "waitPid" && it.parameterCount == 1 }
            ?: run {
                fail("Missing public PtyBridge.waitPid(pid)")
                throw AssertionError()
            }
        return try {
            (method.invoke(instance, pid) as Number).toInt()
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    fun closePtyFd(masterFd: Int) {
        val (clazz, instance) = resolveObjectOrInstance("com.klix.code.data.sandbox.PtyBridge")
        val method = clazz.methods.firstOrNull { it.name == "closeFd" && it.parameterCount == 1 }
            ?: run {
                fail("Missing public PtyBridge.closeFd(masterFd)")
                throw AssertionError()
            }
        try {
            method.invoke(instance, masterFd)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    suspend fun extractTarGz(
        sandboxManager: SandboxManager,
        tarGzFile: File,
        destDir: File
    ): Result<Unit> {
        val candidates = SandboxManager::class.java.declaredMethods.filter { m ->
            (m.name == "extractTarGz" || m.name.startsWith("extractTarGz-")) &&
                Modifier.isPublic(m.modifiers)
        }
        val suspendMethod = candidates.firstOrNull { m ->
            m.parameterCount == 3 &&
                m.parameterTypes[0] == File::class.java &&
                m.parameterTypes[1] == File::class.java &&
                Continuation::class.java.isAssignableFrom(m.parameterTypes[2])
        }
        if (suspendMethod != null) {
            val raw = invokeSuspend(suspendMethod, sandboxManager, tarGzFile, destDir)
            return unwrapKotlinResult(raw)
        }

        val blockingMethod = candidates.firstOrNull { m ->
            m.parameterCount == 2 &&
                m.parameterTypes[0] == File::class.java &&
                m.parameterTypes[1] == File::class.java
        }
        if (blockingMethod != null) {
            val raw = try {
                blockingMethod.invoke(sandboxManager, tarGzFile, destDir)
            } catch (e: InvocationTargetException) {
                throw e.targetException
            }
            return unwrapKotlinResult(raw)
        }

        fail(
            "Missing public SandboxManager.extractTarGz(File, File): Result<Unit> contract " +
                "(declared methods: ${SandboxManager::class.java.declaredMethods.filter { it.name.contains("extractTarGz") }.map { "${Modifier.toString(it.modifiers)} ${it.name}(${it.parameterTypes.joinToString { p -> p.simpleName }})" }})"
        )
        throw AssertionError()
    }

    private suspend fun invokeSuspend(method: Method, target: Any, vararg args: Any?): Any? {
        return suspendCancellableCoroutine { cont ->
            val bridgeCont = object : Continuation<Any?> {
                override val context: CoroutineContext = EmptyCoroutineContext
                override fun resumeWith(result: Result<Any?>) {
                    result.fold(
                        onSuccess = { value -> cont.resume(value) },
                        onFailure = { err -> cont.resumeWithException(err) }
                    )
                }
            }
            try {
                val res = method.invoke(target, *args, bridgeCont)
                if (res !== COROUTINE_SUSPENDED) {
                    cont.resume(res)
                }
            } catch (e: InvocationTargetException) {
                cont.resumeWithException(e.targetException)
            } catch (e: Throwable) {
                cont.resumeWithException(e)
            }
        }
    }

    private fun unwrapKotlinResult(raw: Any?): Result<Unit> {
        if (raw is Result<*>) {
            val err = raw.exceptionOrNull()
            return if (err != null) Result.failure(err) else Result.success(Unit)
        }
        if (raw === Unit) {
            return Result.success(Unit)
        }
        if (raw != null && raw.javaClass.name == "kotlin.Result\$Failure") {
            val exField = raw.javaClass.getField("exception")
            val ex = exField.get(raw) as Throwable
            return Result.failure(ex)
        }
        fail("SandboxManager.extractTarGz returned unexpected non-Result type: ${raw?.javaClass?.name}")
        throw AssertionError()
    }

    @Suppress("UNCHECKED_CAST")
    fun getToolchainEnv(sandboxManager: SandboxManager, workingDir: File): Map<String, String> {
        val method = SandboxManager::class.java.methods.firstOrNull { m ->
            m.name == "getToolchainEnv" && m.parameterCount == 1 && m.parameterTypes[0] == File::class.java
        } ?: run {
            fail("Missing public SandboxManager.getToolchainEnv(workingDir: File): Map<String, String>")
            throw AssertionError()
        }
        val res = try {
            method.invoke(sandboxManager, workingDir)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
        if (res !is Map<*, *>) {
            fail("SandboxManager.getToolchainEnv did not return Map<String, String>, got ${res?.javaClass?.name}")
            throw AssertionError()
        }
        return res as Map<String, String>
    }

    fun getExecutablePath(sandboxManager: SandboxManager, toolId: String): String? {
        val method = SandboxManager::class.java.methods.firstOrNull { m ->
            m.name == "getExecutablePath" && m.parameterCount == 1 && m.parameterTypes[0] == String::class.java
        } ?: run {
            fail("Missing public SandboxManager.getExecutablePath(toolId: String): String?")
            throw AssertionError()
        }
        val res = try {
            method.invoke(sandboxManager, toolId)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
        if (res != null && res !is String) {
            fail("SandboxManager.getExecutablePath did not return String?, got ${res.javaClass.name}")
            throw AssertionError()
        }
        return res as String?
    }

    fun resizeTerminal(provider: SandboxExecutionProvider, rows: Int, cols: Int) {
        val method = SandboxExecutionProvider::class.java.methods.firstOrNull { m ->
            m.name == "resizeTerminal" && m.parameterCount == 2
        } ?: run {
            fail("Missing public SandboxExecutionProvider.resizeTerminal(rows: Int, cols: Int)")
            throw AssertionError()
        }
        try {
            method.invoke(provider, rows, cols)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    fun sendRawBytes(provider: SandboxExecutionProvider, bytes: ByteArray) {
        val method = SandboxExecutionProvider::class.java.methods.firstOrNull { m ->
            m.name == "sendRawBytes" && m.parameterCount == 1 && m.parameterTypes[0] == ByteArray::class.java
        } ?: run {
            fail("Missing public SandboxExecutionProvider.sendRawBytes(bytes: ByteArray)")
            throw AssertionError()
        }
        try {
            method.invoke(provider, bytes)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }
}
