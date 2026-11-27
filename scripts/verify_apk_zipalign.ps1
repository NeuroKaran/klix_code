param(
    [Parameter(Mandatory = $false)]
    [string]$Apk = "app/build/outputs/apk/debug/app-debug.apk"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -Path $Apk -PathType Leaf)) {
    Write-Error "APK not found at: $Apk"
    exit 1
}

$buildToolsRoot = Join-Path $env:LOCALAPPDATA "Android\Sdk\build-tools"
if (-not (Test-Path -Path $buildToolsRoot -PathType Container)) {
    Write-Error "Android SDK build-tools not found at: $buildToolsRoot"
    exit 1
}

$zipalignCandidates = Get-ChildItem -Path $buildToolsRoot -Filter "zipalign.exe" -Recurse -ErrorAction SilentlyContinue |
    Sort-Object {
        $verText = $_.Directory.Name -replace '[^0-9\.]', ''
        try { [Version]$verText } catch { [Version]"0.0.0" }
    } -Descending

if (-not $zipalignCandidates -or $zipalignCandidates.Count -eq 0) {
    Write-Error "zipalign.exe not found under $buildToolsRoot"
    exit 1
}

$zipalign = $zipalignCandidates[0].FullName
Write-Host "[verify_apk_zipalign] Using $zipalign"
Write-Host "[verify_apk_zipalign] Running: zipalign -c -P 16 -v 4 $Apk"

$output = & $zipalign -c -P 16 -v 4 $Apk 2>&1
$exitCode = $LASTEXITCODE
$output | ForEach-Object { Write-Host $_ }

$joined = ($output | Out-String)
if ($exitCode -ne 0) {
    Write-Error "[verify_apk_zipalign] FAILED: zipalign exited with code $exitCode"
    exit $exitCode
}

if ($joined -notmatch "Verification successful") {
    Write-Error "[verify_apk_zipalign] FAILED: Output did not contain 'Verification successful'"
    exit 1
}

Write-Host "[verify_apk_zipalign] PASS: Verification successful for $Apk"
exit 0
