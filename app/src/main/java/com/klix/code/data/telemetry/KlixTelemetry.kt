package com.klix.code.data.telemetry

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import io.sentry.Breadcrumb
import io.sentry.ISpan
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.SpanStatus

/**
 * Centralized telemetry and observability service for Klix Code.
 * Handles crash reporting, environment metadata, and performance tracking (lag/jitter/ANR).
 */
object KlixTelemetry {

    private const val TAG = "KlixTelemetry"

    /**
     * Initializes device and hardware environment metadata tags in the Sentry scope.
     * This provides immediate context for crashes and slow frame/jitter events.
     */
    fun initEnvironmentMetadata(context: Context) {
        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)

            val totalRamMb = memInfo.totalMem / (1024 * 1024)
            val availRamMb = memInfo.availMem / (1024 * 1024)

            Sentry.configureScope { scope ->
                // Hardware & OS Environment tags
                scope.setTag("device_model", Build.MODEL)
                scope.setTag("device_manufacturer", Build.MANUFACTURER)
                scope.setTag("android_api", Build.VERSION.SDK_INT.toString())
                scope.setTag("cpu_abi", Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown")
                scope.setTag("ram_tier", if (totalRamMb < 4096) "low_ram" else "standard_ram")

                // Dynamic environment extras
                scope.setExtra("ram_total_mb", totalRamMb.toString())
                scope.setExtra("ram_available_mb", availRamMb.toString())
                scope.setExtra("is_low_ram_device", memInfo.lowMemory.toString())
            }
        } catch (e: Exception) {
            // Failsafe: never crash app during telemetry initialization
        }
    }

    /**
     * Updates active AI provider and model context in Sentry
     */
    fun setAiContext(provider: String, model: String) {
        Sentry.configureScope { scope ->
            scope.setTag("ai_provider", provider)
            scope.setTag("ai_model", model)
        }
    }

    /**
     * Updates workspace context (e.g., sandbox vs local)
     */
    fun setWorkspaceContext(workspacePath: String, fileCount: Int = 0) {
        Sentry.configureScope { scope ->
            scope.setTag("workspace_type", if (workspacePath.startsWith("/workspace")) "sandbox" else "storage")
            scope.setExtra("workspace_file_count", fileCount.toString())
        }
    }

    /**
     * Adds an informational or diagnostic breadcrumb leading up to an event/crash
     */
    fun recordBreadcrumb(
        category: String,
        message: String,
        data: Map<String, Any?> = emptyMap(),
        level: SentryLevel = SentryLevel.INFO
    ) {
        val breadcrumb = Breadcrumb().apply {
            this.category = category
            this.message = message
            this.level = level
            data.forEach { (key, value) ->
                if (value != null) this.setData(key, value)
            }
        }
        Sentry.addBreadcrumb(breadcrumb)
    }

    /**
     * Measures an operation block and reports execution time & status as a Sentry transaction span.
     * Useful for tracking editor lag, file indexing, and proot command execution.
     */
    inline fun <T> measureSpan(
        operation: String,
        description: String,
        extraData: Map<String, Any?> = emptyMap(),
        block: () -> T
    ): T {
        val startTime = SystemClock.elapsedRealtime()
        val span: ISpan? = Sentry.getSpan()?.startChild(operation, description)
            ?: Sentry.startTransaction(operation, description)

        extraData.forEach { (k, v) ->
            if (v != null) span?.setData(k, v)
        }

        return try {
            val result = block()
            span?.status = SpanStatus.OK
            result
        } catch (t: Throwable) {
            span?.status = SpanStatus.INTERNAL_ERROR
            recordError(t, "Error during operation: $description", extraData)
            throw t
        } finally {
            val elapsedMs = SystemClock.elapsedRealtime() - startTime
            span?.setData("duration_ms", elapsedMs)
            span?.finish()

            // If the operation took unusually long (>500ms for UI, >3000ms for background), record a jitter breadcrumb
            if (elapsedMs > 500) {
                recordBreadcrumb(
                    category = "performance.jitter",
                    message = "Slow operation detected: $description took ${elapsedMs}ms",
                    data = mapOf("duration_ms" to elapsedMs, "op" to operation),
                    level = SentryLevel.WARNING
                )
            }
        }
    }

    /**
     * Captures a non-fatal or handled exception with rich metadata
     */
    fun recordError(
        throwable: Throwable,
        contextMessage: String? = null,
        metadata: Map<String, Any?> = emptyMap()
    ) {
        Sentry.withScope { scope ->
            if (!contextMessage.isNullOrBlank()) {
                scope.setExtra("context_message", contextMessage)
            }
            metadata.forEach { (key, value) ->
                if (value != null) scope.setExtra(key, value.toString())
            }
            Sentry.captureException(throwable)
        }
    }

    /**
     * Logs agent turn events to monitor multi-step workflow health
     */
    fun recordAgentStep(
        turn: Int,
        toolName: String?,
        model: String,
        durationMs: Long,
        isSuccess: Boolean
    ) {
        recordBreadcrumb(
            category = "agent.step",
            message = "Agent turn $turn tool: ${toolName ?: "direct_response"} (success: $isSuccess, ${durationMs}ms)",
            data = mapOf(
                "turn" to turn,
                "tool" to (toolName ?: "none"),
                "model" to model,
                "duration_ms" to durationMs,
                "is_success" to isSuccess
            )
        )
    }

    /**
     * Logs sandbox (proot) command executions
     */
    fun recordSandboxCommand(
        command: String,
        exitCode: Int,
        durationMs: Long
    ) {
        val sanitizedCmd = if (command.length > 80) command.take(80) + "..." else command
        recordBreadcrumb(
            category = "sandbox.command",
            message = "Exec: $sanitizedCmd (exit: $exitCode, ${durationMs}ms)",
            data = mapOf(
                "exit_code" to exitCode,
                "duration_ms" to durationMs
            ),
            level = if (exitCode != 0) SentryLevel.WARNING else SentryLevel.INFO
        )
    }
}
