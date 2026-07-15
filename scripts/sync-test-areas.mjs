#!/usr/bin/env node
/**
 * Sync config/test-areas.json from airepro_cicd/scripts/export-test-areas.js
 *
 * Usage:
 *   node scripts/sync-test-areas.mjs
 *   node scripts/sync-test-areas.mjs --airepro-cicd=C:/Users/Amplify/airepro_cicd
 */
import { execSync } from 'child_process';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');
const OUT = path.join(ROOT, 'config', 'test-areas.json');

const cicdRoot = process.argv.find((a) => a.startsWith('--airepro-cicd='))?.split('=')[1]
  || process.env.AIREPRO_CICD_ROOT
  || path.resolve(ROOT, '..', 'airepro_cicd');

if (!fs.existsSync(path.join(cicdRoot, 'scripts', 'export-test-areas.js'))) {
  console.error(`airepro_cicd not found at ${cicdRoot}`);
  process.exit(1);
}

const json = execSync('node scripts/export-test-areas.js', {
  cwd: cicdRoot,
  encoding: 'utf8',
});

fs.writeFileSync(OUT, json.trim() + '\n', 'utf8');
console.log(`Wrote ${OUT}`);
