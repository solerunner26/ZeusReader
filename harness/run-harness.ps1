param(
    [ValidateSet("all", "android", "server", "pixel")]
    [string]$Target = "all"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

Write-Host "=== ZeusReader Harness (Target: $Target | Device: Google Pixel 10 Pro) ===" -ForegroundColor Cyan

if ($Target -in @("all", "android")) {
    if (Test-Path "$Root\android\gradlew.bat") {
        Write-Host "`n[1/3] Running Android unit tests & lint..." -ForegroundColor Yellow
        Push-Location "$Root\android"
        .\gradlew.bat testDebugUnitTest lintDebug --stacktrace
        Pop-Location
    } else {
        Write-Host "`n[1/3] Skipping Android (Phase 0 scaffold not created yet)." -ForegroundColor DarkGray
    }
}

if ($Target -in @("all", "server")) {
    if ((Test-Path "$Root\server\pyproject.toml") -or (Test-Path "$Root\server\requirements.txt")) {
        Write-Host "`n[2/3] Running FastAPI / Connector pytest suite..." -ForegroundColor Yellow
        Push-Location "$Root\server"
        if (Get-Command pytest -ErrorAction SilentlyContinue) {
            pytest -q
        } else {
            python -m pytest -q
        }
        Pop-Location
    } else {
        Write-Host "`n[2/3] Skipping Server (not initialized yet)." -ForegroundColor DarkGray
    }
}

if ($Target -in @("all", "pixel")) {
    Write-Host "`n[3/3] Checking connected Google Pixel 10 Pro via ADB..." -ForegroundColor Yellow
    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if ($adb) {
        $devices = adb devices | Select-String "\tdevice$"
        if ($devices) {
            $model = adb shell getprop ro.product.model
            Write-Host "Connected device: $model" -ForegroundColor Green
            if (Test-Path "$Root\android\gradlew.bat") {
                Push-Location "$Root\android"
                .\gradlew.bat installDebug
                Pop-Location
            }
        } else {
            Write-Host "No physical Pixel 10 Pro detected over USB/Wireless ADB. Skipping device install." -ForegroundColor DarkYellow
        }
    } else {
        Write-Host "ADB not found in PATH. Install Android Platform-Tools." -ForegroundColor DarkYellow
    }
}

Write-Host "`n=== Harness Complete ===" -ForegroundColor Green
