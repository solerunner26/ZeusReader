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
