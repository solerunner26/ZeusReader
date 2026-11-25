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
