# 🛡️ Sentry Monitoring & Diagnostics for Klix Code
> Real-time crash reporting, performance tracking (lag/jitter), ANR detection, and device environment metadata for Android.

---

## 📌 Sentry Project Details

- **Organization**: `swaranai`
- **Project**: `android`
- **Project ID**: `4512348243165184`
- **Dashboard URL**: [https://swaranai.sentry.io/issues/?project=4512348243165184](https://swaranai.sentry.io/issues/?project=4512348243165184)
- **Performance URL**: [https://swaranai.sentry.io/performance/?project=4512348243165184](https://swaranai.sentry.io/performance/?project=4512348243165184)

---

## ⚡ What Is Tracked

### 1. Crashes & Stack Traces
- **Uncaught Exceptions**: Automatically caught and tied to the exact file, class, method, and line number.
- **Source Context**: Enabled via `includeSourceContext.set(true)` in `app/build.gradle.kts`. You can see source lines right in the Sentry UI.
- **Crash Screenshots & View Hierarchy**: Automatically captures a snapshot of the UI when a crash occurs to help reproduce visual bugs.

### 2. Lag & Jitter (Slow and Frozen Frames)
- **Frame Metrics**: `io.sentry.traces.frames-tracking.enable` is active.
- **Slow Frames**: Any frame that takes longer than 16ms (60Hz) or 8ms (120Hz).
- **Frozen Frames**: Any UI lockup exceeding 700ms.
- **Where to see it**: Go to Sentry Dashboard → **Performance** → **Screen Loads / Frames** to see frame drop percentages across different phone models.

### 3. ANR (App Not Responding)
- Automatically triggers when the main UI thread freezes for **5000ms (5 seconds)**.
- Captures full thread dump and state to identify background bottlenecks or deadlocks.

### 4. Device Environment Metadata
Attached to every single error and transaction automatically via [`KlixTelemetry.kt`](file:///c:/Users/karan/Personal_projects/klix_code/app/src/main/java/com/klix/code/data/telemetry/KlixTelemetry.kt):
- **Hardware**: Device Model (`Build.MODEL`), Manufacturer (`Build.MANUFACTURER`), CPU Architecture
- **Memory**: Total RAM, Available RAM, Low-RAM tier flag
- **OS**: Android Version, API Level (`Build.VERSION.SDK_INT`)
- **Battery & Thermal State**: Battery percentage, charging state, thermal throttling warnings

---

## 🛠️ How to Use `KlixTelemetry` in Your Code

Import the utility:
```kotlin
import com.klix.code.data.telemetry.KlixTelemetry
```

### 1. Measure Operation Time & Detect Jitter
Wrap slow operations (file parsing, editor syntax highlight, PRoot boot):
```kotlin
val result = KlixTelemetry.measureSpan(
    operation = "sora.syntax_highlight",
    description = "Highlighting $fileName",
    extraData = mapOf("lines" to lineCount, "file_size_kb" to fileSize / 1024)
) {
    // Your code here...
}
```
*If this takes >500ms, Sentry automatically flags a performance warning with jitter diagnostics.*

### 2. Record Custom Breadcrumbs (Debug Trails)
```kotlin
KlixTelemetry.recordBreadcrumb(
    category = "editor.action",
    message = "User opened file: $filePath",
    data = mapOf("extension" to ext, "size" to size)
)
```

### 3. Track Agent Loop Execution
```kotlin
KlixTelemetry.recordAgentStep(
    turn = currentTurn,
    toolName = "execute_command",
    model = "claude-3.7-sonnet",
    durationMs = 1240,
    isSuccess = true
)
```

### 4. Track Linux Sandbox (PRoot) Commands
```kotlin
KlixTelemetry.recordSandboxCommand(
    command = "npm install",
    exitCode = 0,
    durationMs = 4500
)
```

### 5. Capture Handled Errors with Context
```kotlin
try {
    // Model API call or IO
} catch (e: Exception) {
    KlixTelemetry.recordError(
        throwable = e,
        contextMessage = "Failed to communicate with Anthropic API",
        metadata = mapOf("model" to selectedModel, "prompt_chars" to prompt.length)
    )
}
```

---

## 🔒 Security & Privacy
- Sensitive credentials, API keys, and local secrets in `Keystore` are never sent to Sentry.
- Auth token for ProGuard mapping uploads is stored locally in `sentry.properties` which is ignored in `.gitignore`.
