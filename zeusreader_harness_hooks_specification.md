# HARNESS_AND_HOOKS.md — ZeusReader Agent Harness & Guard Hooks

> **One-shot bootstrap:** Drop this file into the `ZeusReader` repo root. Gemini CLI will automatically create all files below during Phase 0 (or anytime you ask: *"Create all files defined in HARNESS_AND_HOOKS.md and activate git hooks"*).
> **Target Device:** Google Pixel 10 Pro | **Host OS:** Windows 11

---

## 1. Directory Structure

```text
ZeusReader/
├── .gemini/
│   ├── settings.json              # Gemini CLI hook registry
│   └── hooks/
│       ├── guard-secrets.js       # BeforeTool: blocks keys, .env, & book binaries
│       ├── guard-ui-rules.js      # BeforeTool: blocks deprecated Material 2 imports
│       └── after-edit-log.js      # AfterTool: logs modified files for phase handoff
├── .githooks/
│   └── pre-commit                 # Blocks secrets & >5MB book files before git push
└── harness/
    ├── device-profile.json        # Target hardware: Google Pixel 10 Pro
    └── run-harness.ps1            # Master test runner (Android + Server + Pixel ADB)
```

---

## 2. File: `.gemini/settings.json`

```json
{
  "$schema": "https://raw.githubusercontent.com/google-gemini/gemini-cli/main/schemas/settings.schema.json",
  "hooks": {
    "BeforeTool": [
      {
        "matcher": "write_file|replace",
        "hooks": [
          {
            "name": "guard-secrets",
            "type": "command",
            "command": "node \"$GEMINI_PROJECT_DIR/.gemini/hooks/guard-secrets.js\"",
            "timeout": 5000
          },
          {
            "name": "guard-ui-rules",
            "type": "command",
            "command": "node \"$GEMINI_PROJECT_DIR/.gemini/hooks/guard-ui-rules.js\"",
            "timeout": 5000
          }
        ]
      }
    ],
    "AfterTool": [
      {
        "matcher": "write_file|replace",
        "hooks": [
          {
            "name": "after-edit-log",
            "type": "command",
            "command": "node \"$GEMINI_PROJECT_DIR/.gemini/hooks/after-edit-log.js\"",
            "timeout": 3000
          }
        ]
      }
    ]
  }
}
```

---

## 3. File: `.gemini/hooks/guard-secrets.js`

```javascript
#!/usr/bin/env node
const fs = require('fs');

const input = JSON.parse(fs.readFileSync(0, 'utf-8'));
const filePath = (input.tool_input?.file_path || input.tool_input?.path || '').toLowerCase();
const content = input.tool_input?.content || input.tool_input?.new_string || '';

const blockedExtensions = ['.jks', '.keystore', '.p12', '.epub', '.pdf', '.azw3', '.mobi'];
const isSampleAsset = filePath.includes('androidtest/assets/samples');

if (!isSampleAsset && blockedExtensions.some(ext => filePath.endsWith(ext))) {
  console.log(JSON.stringify({
    decision: 'deny',
    reason: `Blocked write to ${filePath}. Keystores and book files must stay outside tracked code (except small test fixtures in androidTest/assets/samples).`
  }));
  process.exit(0);
}

const secretPatterns = [
  /-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----/,
  /TAILSCALE_AUTHKEY\s*=\s*tskey-/,
  /STORE_PASSWORD\s*=\s*["']?[^"'\s]{6,}/
];

if (secretPatterns.some(rx => rx.test(content))) {
  console.log(JSON.stringify({
    decision: 'deny',
    reason: 'Potential hardcoded secret or signing password detected. Move secrets to local .env or local.properties.'
  }));
  process.exit(0);
}

console.log(JSON.stringify({ decision: 'allow' }));
```

---

## 4. File: `.gemini/hooks/guard-ui-rules.js`

```javascript
#!/usr/bin/env node
const fs = require('fs');

const input = JSON.parse(fs.readFileSync(0, 'utf-8'));
const filePath = (input.tool_input?.file_path || input.tool_input?.path || '').toLowerCase();
const content = input.tool_input?.content || input.tool_input?.new_string || '';

if (filePath.endsWith('.kt')) {
  // Block deprecated Material 2 imports (allow material.icons)
  if (/import\s+androidx\.compose\.material\.(?!icons\.)/.test(content)) {
    console.log(JSON.stringify({
      decision: 'deny',
      reason: 'Deprecated Material 2 import detected. Use androidx.compose.material3.* per GEMINI.md.'
    }));
    process.exit(0);
  }
}

console.log(JSON.stringify({ decision: 'allow' }));
```

---

## 5. File: `.gemini/hooks/after-edit-log.js`

```javascript
#!/usr/bin/env node
const fs = require('fs');
const path = require('path');

const input = JSON.parse(fs.readFileSync(0, 'utf-8'));
const filePath = input.tool_input?.file_path || input.tool_input?.path || 'unknown';
const logDir = path.join(process.env.GEMINI_PROJECT_DIR || process.cwd(), '.gemini', 'logs');

fs.mkdirSync(logDir, { recursive: true });
fs.appendFileSync(
  path.join(logDir, 'edits.jsonl'),
  JSON.stringify({ ts: new Date().toISOString(), tool: input.tool_name, file: filePath }) + '\n'
);

console.log(JSON.stringify({}));
```

---

## 6. File: `harness/device-profile.json`

```json
{
  "deviceName": "Google Pixel 10 Pro",
  "targetApiLevel": 36,
  "minApiLevel": 26,
  "supportsHardwareGlassBlur": true,
  "refreshRateHz": 120,
  "oledTrueBlackSupported": true,
  "primaryScripts": ["en", "hi", "gu"],
  "benchmarkTargets": {
    "coldStartMsMax": 1500,
    "openCachedBookMsMax": 1000,
    "pageTurnMsMax": 100
  }
}
```

---

## 7. File: `harness/run-harness.ps1`

```powershell
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
        pytest -q
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
```

---

## 8. File: `.githooks/pre-commit`

```sh
#!/bin/sh
# Block sensitive files and large binaries (>5MB) before commit
FORBIDDEN=$(git diff --cached --name-only | grep -E '\.(jks|keystore|env|p12)$')
if [ -n "$FORBIDDEN" ]; then
  echo "ERROR: Attempted to commit forbidden secret/keystore file:"
  echo "$FORBIDDEN"
  exit 1
fi

LARGE_FILES=$(git diff --cached --name-only | while read -r f; do
  if [ -f "$f" ] && [ "$(wc -c < "$f")" -gt 5242880 ]; then
    echo "$f"
  fi
done)

if [ -n "$LARGE_FILES" ]; then
  echo "ERROR: File exceeds 5MB limit (keep book files out of git):"
  echo "$LARGE_FILES"
  exit 1
fi
```

---

## 9. One-Time Activation Commands (Windows 11 PowerShell)

```powershell
# 1. Point Git to the tracked hooks folder
git config core.hooksPath .githooks

# 2. Launch Gemini CLI and verify hooks are loaded
gemini
# Inside Gemini CLI, run: /hooks panel
```