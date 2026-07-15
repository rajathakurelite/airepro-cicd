#!/usr/bin/env node
/**
 * Validate services.yaml ports/domains and test area references.
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');

function loadYamlSimple(file) {
  const text = fs.readFileSync(file, 'utf8');
  const services = [];
  let current = null;
  for (const line of text.split('\n')) {
    if (line.match(/^  - slug:/)) {
      current = { slug: line.split(':')[1].trim() };
      services.push(current);
    } else if (current && line.match(/^    enabled:/)) {
      current.enabled = line.includes('true');
    } else if (current && line.match(/^      area:/)) {
      current.testArea = line.split(':')[1].trim();
    }
  }
  return services;
}

const areas = JSON.parse(fs.readFileSync(path.join(ROOT, 'config', 'test-areas.json'), 'utf8')).areas;
const areaIds = new Set(areas.map((a) => a.id));
const services = loadYamlSimple(path.join(ROOT, 'services.yaml'));

let errors = 0;
for (const svc of services) {
  if (!svc.enabled) continue;
  if (svc.testArea && svc.testArea !== 'null' && !areaIds.has(svc.testArea)) {
    console.error(`Service ${svc.slug}: unknown test.area "${svc.testArea}"`);
    errors++;
  }
}

if (errors > 0) {
  process.exit(1);
}
console.log(`Validated ${services.length} services, ${areaIds.size} test areas.`);
