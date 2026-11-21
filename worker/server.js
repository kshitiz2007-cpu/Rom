import http from 'node:http';
import { WebSocketServer } from 'ws';
import { spawn } from 'node:child_process';
import { randomUUID } from 'node:crypto';

const PORT = Number(process.env.PORT || 8787);
const WORKER_TOKEN = process.env.WORKER_TOKEN;
const AVD_NAME = process.env.AVD_NAME || 'romstudio_api_35';
const EMULATOR_BIN = process.env.EMULATOR_BIN || 'emulator';
const ADB_BIN = process.env.ADB_BIN || 'adb';
const MAX_CLIENTS = Number(process.env.MAX_STREAM_CLIENTS || 2);
let emulator = null;
let emulatorStarting = false;
let activeClients = 0;
let bootStartedAt = null;

const server = http.createServer((req, res) => {
  const url = new URL(req.url || '/', 'http://localhost');
  if (req.method === 'GET' && url.pathname === '/health') {
    return send(res, 200, { ok: true, service: 'romstudio-emulator-worker', emulatorRunning: !!emulator, emulatorStarting, avd: AVD_NAME });
  }
  if (req.method === 'GET' && url.pathname === '/api/status') {
    if (!authorized(req)) return send(res, 401, { error: 'Unauthorized' });
    return send(res, 200, { emulatorRunning: !!emulator, emulatorStarting, activeStreams: activeClients, avd: AVD_NAME, bootStartedAt });
  }
  if (req.method === 'POST' && url.pathname === '/api/start') {
    if (!authorized(req)) return send(res, 401, { error: 'Unauthorized' });
    if (emulator || emulatorStarting) return send(res, 409, { error: 'Emulator is already running or starting' });
    emulatorStarting = true;
    bootStartedAt = new Date().toISOString();
    emulator = spawn(EMULATOR_BIN, ['-avd', AVD_NAME, '-no-snapshot', '-no-audio', '-gpu', 'auto'], { stdio: 'ignore' });
    emulator.once('error', err => { emulator = null; emulatorStarting = false; console.error('Emulator launch error:', err.message); });
    emulator.once('spawn', () => { emulatorStarting = false; });
    emulator.once('exit', (code, signal) => { console.log('Emulator exited', code, signal); emulator = null; emulatorStarting = false; });
    return send(res, 202, { accepted: true, avd: AVD_NAME });
  }
  if (req.method === 'POST' && url.pathname === '/api/stop') {
    if (!authorized(req)) return send(res, 401, { error: 'Unauthorized' });
    if (emulator) emulator.kill('SIGTERM');
    return send(res, 202, { accepted: true });
  }
  send(res, 404, { error: 'Not found' });
});
const wss = new WebSocketServer({ noServer: true, maxPayload: 1024 * 1024 });
server.on('upgrade', (req, socket, head) => {
  const url = new URL(req.url || '/', 'http://localhost');
  if (url.pathname !== '/stream' || !authorized(req)) { socket.write('HTTP/1.1 401 Unauthorized\r\n\r\n'); socket.destroy(); return; }
  wss.handleUpgrade(req, socket, head, ws => wss.emit('connection', ws, req));
});
wss.on('connection', ws => {
  if (activeClients >= MAX_CLIENTS) { ws.close(1013, 'Stream capacity reached'); return; }
  activeClients++;
  let streamTimer = null;
  const startFrames = () => {
    if (streamTimer) return;
    streamTimer = setInterval(async () => {
      if (ws.readyState !== ws.OPEN || !emulator) return;
      try {
        const result = await adb(['exec-out', 'screencap', '-p'], 4000);
        if (result.length && ws.readyState === ws.OPEN) ws.send(result, { binary: true });
      } catch (e) { ws.send(JSON.stringify({ type: 'stream-error', message: 'Screenshot capture unavailable' })); }
    }, 900);
  };
  ws.send(JSON.stringify({ type: 'worker-status', emulatorRunning: !!emulator, avd: AVD_NAME }));
  ws.on('message', async raw => {
    let msg; try { msg = JSON.parse(raw.toString()); } catch { return; }
    if (msg.type === 'stream-start') startFrames();
    if (msg.type === 'stream-stop' && streamTimer) { clearInterval(streamTimer); streamTimer = null; }
    if (msg.type === 'input' && emulator) {
      const allowed = new Set(['keyevent','tap','swipe','text']);
      if (!allowed.has(msg.action)) return;
      const args = msg.action === 'keyevent' ? ['shell','input','keyevent',String(msg.key).slice(0,4)]
        : msg.action === 'tap' ? ['shell','input','tap',String(Number(msg.x)||0),String(Number(msg.y)||0)]
        : msg.action === 'swipe' ? ['shell','input','swipe',String(Number(msg.x1)||0),String(Number(msg.y1)||0),String(Number(msg.x2)||0),String(Number(msg.y2)||0),String(Math.min(1500,Math.max(50,Number(msg.duration)||250)))]
        : ['shell','input','text',String(msg.text||'').slice(0,160).replace(/[^a-zA-Z0-9 _.,@\-]/g,'')];
      try { await adb(args, 3000); ws.send(JSON.stringify({ type: 'input-ack', action: msg.action })); } catch { ws.send(JSON.stringify({ type: 'input-error', action: msg.action })); }
    }
  });
  ws.on('close', () => { activeClients--; if (streamTimer) clearInterval(streamTimer); });
});
function authorized(req) { const auth = req.headers.authorization || ''; return !!WORKER_TOKEN && auth === 'Bearer ' + WORKER_TOKEN; }
function send(res, code, data) { res.writeHead(code, { 'content-type':'application/json', 'cache-control':'no-store' }); res.end(JSON.stringify(data)); }
function adb(args, timeoutMs) { return new Promise((resolve, reject) => { const p=spawn(ADB_BIN,args,{stdio:['ignore','pipe','pipe']}); const chunks=[]; const timer=setTimeout(()=>{p.kill('SIGKILL');reject(new Error('ADB timeout'));},timeoutMs); p.stdout.on('data',d=>chunks.push(d)); p.stderr.on('data',()=>{}); p.on('error',e=>{clearTimeout(timer);reject(e);}); p.on('close',code=>{clearTimeout(timer);code===0?resolve(Buffer.concat(chunks)):reject(new Error('ADB exit '+code));}); }); }
server.listen(PORT, '0.0.0.0', () => console.log('Emulator worker listening on', PORT));
