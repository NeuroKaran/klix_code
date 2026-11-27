package com.klix.code.data.sandbox

data class PtySession(
    val pid: Int,
    val masterFd: Int
)

interface PtyTransport {
    val isAvailable: Boolean
    fun openAndForkPty(argv: Array<String>, envp: Array<String>, cwd: String, rows: Int, cols: Int): PtySession
    fun readPty(masterFd: Int, buffer: ByteArray): Int
    fun writePty(masterFd: Int, data: ByteArray): Int
    fun resizePty(masterFd: Int, rows: Int, cols: Int): Int
    fun sendSignal(pid: Int, signal: Int): Int
    fun waitPid(pid: Int): Int
    fun closeFd(masterFd: Int)
}

object PtyBridge : PtyTransport {
    @Volatile
    private var nativeLoaded: Boolean = false

    @Volatile
    private var loadFailure: Throwable? = null

    init {
        try {
            System.loadLibrary("klix_pty")
            nativeLoaded = true
        } catch (t: UnsatisfiedLinkError) {
            loadFailure = t
            nativeLoaded = false
        }
    }

    override val isAvailable: Boolean
        get() = nativeLoaded

    private fun ensureLoaded() {
        if (!nativeLoaded) {
            try {
                System.loadLibrary("klix_pty")
                nativeLoaded = true
                loadFailure = null
            } catch (t: UnsatisfiedLinkError) {
                loadFailure = t
                throw UnsatisfiedLinkError("libklix_pty.so unavailable: ${t.message}")
            }
        }
    }

    override fun openAndForkPty(
        argv: Array<String>,
        envp: Array<String>,
        cwd: String,
        rows: Int,
        cols: Int
    ): PtySession {
        ensureLoaded()
        return nativeOpenAndForkPty(argv, envp, cwd, rows, cols)
    }

    override fun readPty(masterFd: Int, buffer: ByteArray): Int {
        ensureLoaded()
        return nativeReadPty(masterFd, buffer)
    }

    override fun writePty(masterFd: Int, data: ByteArray): Int {
        ensureLoaded()
        return nativeWritePty(masterFd, data)
    }

    override fun resizePty(masterFd: Int, rows: Int, cols: Int): Int {
        ensureLoaded()
        return nativeResizePty(masterFd, rows, cols)
    }

    override fun sendSignal(pid: Int, signal: Int): Int {
        ensureLoaded()
        return nativeSendSignal(pid, signal)
    }

    override fun waitPid(pid: Int): Int {
        ensureLoaded()
        return nativeWaitPid(pid)
    }

    override fun closeFd(masterFd: Int) {
        if (!nativeLoaded) return
        nativeCloseFd(masterFd)
    }

    private external fun nativeOpenAndForkPty(
        argv: Array<String>,
        envp: Array<String>,
        cwd: String,
        rows: Int,
        cols: Int
    ): PtySession

    private external fun nativeReadPty(masterFd: Int, buffer: ByteArray): Int

    private external fun nativeWritePty(masterFd: Int, data: ByteArray): Int

    private external fun nativeResizePty(masterFd: Int, rows: Int, cols: Int): Int

    private external fun nativeSendSignal(pid: Int, signal: Int): Int

    private external fun nativeWaitPid(pid: Int): Int

    private external fun nativeCloseFd(masterFd: Int)
}
