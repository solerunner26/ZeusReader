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
