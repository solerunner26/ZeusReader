param([string[]]$Tasks = @('assembleDebug'))
$ErrorActionPreference = 'Stop'
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr' }
Set-Location $PSScriptRoot
$sw = [Diagnostics.Stopwatch]::StartNew()
& .\gradlew.bat @Tasks --console=plain
$code = $LASTEXITCODE
$sw.Stop()
Write-Host ("ELAPSED_SECONDS=" + [math]::Round($sw.Elapsed.TotalSeconds, 1))
exit $code
