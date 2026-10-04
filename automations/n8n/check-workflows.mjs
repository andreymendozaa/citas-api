// Local sanity check for the exported n8n workflows (no network): valid JSON, connections reference existing
// nodes, Code nodes compile, and no credential ids, webhook ids or real-looking secrets are versioned.
// Usage: node automations/n8n/check-workflows.mjs
import { readFileSync, readdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = dirname(fileURLToPath(import.meta.url));
let failures = 0;
const fail = (file, msg) => { failures++; console.error(`FAIL ${file}: ${msg}`); };

for (const file of readdirSync(dir).filter((f) => /^WF-\d{3}-.*\.json$/.test(f))) {
  const text = readFileSync(join(dir, file), 'utf8');
  let wf;
  try { wf = JSON.parse(text); } catch (e) { fail(file, `invalid JSON: ${e.message}`); continue; }
  const names = new Set(wf.nodes.map((n) => n.name));
  if (!wf.name.startsWith('Andrey - ')) fail(file, 'workflow name must start with "Andrey - "');
  for (const [from, conn] of Object.entries(wf.connections)) {
    if (!names.has(from)) fail(file, `connection from unknown node "${from}"`);
    for (const branch of conn.main) for (const c of branch) if (!names.has(c.node)) fail(file, `connection to unknown node "${c.node}"`);
  }
  for (const node of wf.nodes) {
    for (const [type, cred] of Object.entries(node.credentials || {})) {
      if (cred.id) fail(file, `${node.name}: credential id must not be versioned (${type})`);
      if (!cred.name.startsWith('Andrey - ')) fail(file, `${node.name}: credential name must start with "Andrey - "`);
    }
    if (node.webhookId) fail(file, `${node.name}: webhookId must not be versioned`);
    if (node.type === 'n8n-nodes-base.code') {
      try { new Function('$input', '$', '$execution', '$getWorkflowStaticData', 'DateTime', `return (async () => {${node.parameters.jsCode}\n})`); }
      catch (e) { fail(file, `${node.name}: code does not compile: ${e.message}`); }
    }
  }
  if (/@(?!citas\.test)[a-z0-9-]+\.[a-z]{2,}/i.test(text)) fail(file, 'contains an e-mail address; use <<LAB_RECIPIENT_EMAIL>>');
  if (/https?:\/\/(?!localhost)[^"<\s]*(trycloudflare|n8n)/i.test(text)) fail(file, 'contains an instance or tunnel URL');
  if (/Bearer [A-Za-z0-9._-]{20,}/.test(text)) fail(file, 'contains a literal bearer token');
  console.log(`checked ${file}: ${wf.nodes.length} nodes`);
}
if (failures) { console.error(`${failures} problem(s)`); process.exit(1); }
console.log('OK');
