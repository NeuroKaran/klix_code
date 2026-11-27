param(
    [switch]$BootEmulator,
    [string]$AvdName = "Pixel_9a",
    [string]$TestClass = "",
    [switch]$SkipCompliance,
    [switch]$KillEmulatorOnExit
)

$ErrorActionPreference = "Stop"

$sdkRoot = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$adb = Join-Path $sdkRoot "platform-tools\adb.exe"
$emulator = Join-Path $sdkRoot "emulator\emulator.exe"

if (-not (Test-Path $adb)) {
    Write-Error "adb.exe not found at $adb"
    exit 1
}

$emulatorProcess = $null

function Get-BootedDevice {
    $lines = & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\sdevice$" }
    return ($lines | Measure-Object).Count -gt 0
}

try {
    if (-not $SkipCompliance) {
        Write-Host "[run_e2e_16kb] Step 1/4: Checking 16 KB ELF alignment..."
        & python scripts/verify_16kb_elf.py --jni-libs app/src/main/jniLibs --check-assets
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

        Write-Host "[run_e2e_16kb] Step 2/4: Checking R5 license compliance..."
        & python scripts/verify_r5_licenses.py
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

        if (Test-Path "app/build/outputs/apk/debug/app-debug.apk") {
            Write-Host "[run_e2e_16kb] Step 2b/4: Checking debug APK zipalign & ELF alignment..."
            & python scripts/verify_16kb_elf.py --apk app/build/outputs/apk/debug/app-debug.apk
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

            & powershell -ExecutionPolicy Bypass -File scripts/verify_apk_zipalign.ps1 -Apk app/build/outputs/apk/debug/app-debug.apk
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        }
    }

    if ($BootEmulator -and -not (Get-BootedDevice)) {
        if (-not (Test-Path $emulator)) {
            Write-Error "emulator.exe not found at $emulator"
            exit 1
        }
        Write-Host "[run_e2e_16kb] Booting AVD '$AvdName'..."
        $emulatorProcess = Start-Process -FilePath $emulator -ArgumentList @("-avd", $AvdName, "-no-snapshot-load", "-no-audio", "-no-boot-anim") -PassThru
        & $adb wait-for-device

        $bootCompleted = ""
        for ($i = 0; $i -lt 120; $i++) {
            $bootCompleted = (& $adb shell getprop sys.boot_completed 2>$null).Trim()
            if ($bootCompleted -eq "1") { break }
            Start-Sleep -Seconds 2
        }
        if ($bootCompleted -ne "1") {
            Write-Error "Emulator failed to finish booting within timeout."
            exit 1
        }
    }

    if (-not (Get-BootedDevice)) {
        Write-Error "No online Android device/emulator detected. Pass -BootEmulator to start '$AvdName'."
        exit 1
    }

    Write-Host "[run_e2e_16kb] Step 3/4: Verifying device PAGESIZE == 16384..."
    $pageSize = (& $adb shell getconf PAGESIZE).Trim()
    Write-Host "[run_e2e_16kb] Device getconf PAGESIZE = $pageSize"
    if ($pageSize -ne "16384") {
        Write-Error "Expected 16 KB page-size device (PAGESIZE=16384), but got '$pageSize'."
        exit 1
    }

    Write-Host "[run_e2e_16kb] Step 4/4: Running connectedDebugAndroidTest..."
    $gradleArgs = @("connectedDebugAndroidTest", "--console=plain")
    if (-not [string]::IsNullOrWhiteSpace($TestClass)) {
        $gradleArgs += "-Pandroid.testInstrumentationRunnerArguments.class=$TestClass"
    }

    & .\gradlew.bat @gradleArgs
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    Write-Host "[run_e2e_16kb] PASS: All 16 KB E2E instrumentation tests succeeded."
    exit 0
} finally {
    if ($KillEmulatorOnExit -and $emulatorProcess -ne $null) {
        Write-Host "[run_e2e_16kb] Shutting down emulator..."
        & $adb emu kill | Out-Null
    }
}
