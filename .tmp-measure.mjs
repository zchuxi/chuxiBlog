const list = await (await fetch('http://localhost:9222/json')).json();
const page = list.find(t => t.type === 'page');
const ws = new WebSocket(page.webSocketDebuggerUrl);
let id = 0;
const pending = new Map();
const send = (m, p = {}) => new Promise(res => { const i = ++id; pending.set(i, res); ws.send(JSON.stringify({ id: i, method: m, params: p })); });
ws.onmessage = e => { const m = JSON.parse(e.data); if (m.id && pending.has(m.id)) { pending.get(m.id)(m.result); pending.delete(m.id); } };
await new Promise(r => ws.onopen = r);
await send('Page.enable');
const evalJs = async (expr) => (await send('Runtime.evaluate', { expression: expr, returnByValue: true })).result.value;

await send('Emulation.setDeviceMetricsOverride', { width: 768, height: 800, deviceScaleFactor: 1, mobile: true });
await send('Page.navigate', { url: 'http://localhost:5173/' });
await new Promise(r => setTimeout(r, 2500));
const out = await evalJs(`(() => {
  const bento = document.querySelector('.hero-bento-frame');
  const br = bento.getBoundingClientRect();
  const res = [];
  bento.querySelectorAll('*').forEach(el => {
    const r = el.getBoundingClientRect();
    if (r.right > br.right + 2 || r.left < br.left - 2) {
      const cs = getComputedStyle(el);
      res.push((typeof el.className === 'string' ? el.className : el.tagName).slice(0, 50) + ' pos:' + cs.position + ' L:' + Math.round(r.left - br.left) + ' R:' + Math.round(r.right - br.left) + ' tf:' + cs.transform.slice(0, 40));
    }
  });
  return JSON.stringify({ bentoW: Math.round(br.width), over: res.slice(0, 15) });
})()`);
console.log(out);
ws.close();
process.exit(0);
