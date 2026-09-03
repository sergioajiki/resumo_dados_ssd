const WebSocket = require('ws');

const APP_ID = '10f9b380-d7a4-426c-ae4e-8f6b7d3bd3fb';
const HOST   = 'paineispublicos.saude.ms.gov.br';
const URL    = `wss://${HOST}/app/${APP_ID}`;

const ws = new WebSocket(URL, { rejectUnauthorized: false });
let msgId = 1;
let docHandle = null;

function send(handle, method, params = {}) {
  const msg = { jsonrpc: '2.0', id: msgId++, handle, method, params };
  ws.send(JSON.stringify(msg));
}

ws.on('open', () => {
  send(-1, 'OpenDoc', { qDocName: APP_ID });
});

ws.on('message', (raw) => {
  const msg = JSON.parse(raw);
  if (!msg.id) return;

  if (docHandle === null && msg.result && msg.result.qReturn) {
    docHandle = msg.result.qReturn.qHandle;
    console.log(`Doc handle: ${docHandle}`);
    send(docHandle, 'GetTablesAndKeys', {
      qWindowSize: { qcx: 0, qcy: 0 },
      qNullSize:   { qcx: 0, qcy: 0 },
      qCellHeight: 0,
      qSyntheticMode: false,
      qIncludeSysVars: true
    });
    return;
  }

  if (msg.result && msg.result.qtr) {
    const tables = msg.result.qtr;
    console.log(`\nTotal de tabelas: ${tables.length}\n`);
    tables.forEach(t => {
      const fields = (t.qFields || []).map(f => f.qName).join(', ');
      console.log(`TABLE: ${t.qName} (${(t.qFields||[]).length} campos)`);
      console.log(`  Campos: ${fields}`);
      console.log();
    });
    ws.close();
  }
});

ws.on('error', e => console.error('Erro WebSocket:', e.message));
