import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { MODELS } from './models.js';

{
const $ = s => document.querySelector(s);
const clamp = (x, a = 0, b = 1) => Math.min(b, Math.max(a, x));
const lerp = (a, b, t) => a + (b - a) * t;
const ease = t => t < .5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
const buzz = p => { try { navigator.vibrate && navigator.vibrate(p); } catch (e) {} };
const money = n => '$' + Math.round(n).toLocaleString('en-US');
const esc = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

/* ---------- renderer ---------- */
const canvas = $('#c');
const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
renderer.shadowMap.enabled = true; renderer.shadowMap.type = THREE.PCFSoftShadowMap;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(40, 1, .1, 300);
scene.add(new THREE.HemisphereLight(0xfff4e6, 0x9c8f7c, 1.5));
const sun = new THREE.DirectionalLight(0xfff0dc, 2.4); sun.castShadow = true; sun.shadow.mapSize.set(2048, 2048);
Object.assign(sun.shadow.camera, { left: -9, right: 9, top: 9, bottom: -9, near: 1, far: 40 }); sun.shadow.bias = -.0005; sun.shadow.normalBias = .03;
scene.add(sun, sun.target);

const M = (c, o = {}) => new THREE.MeshStandardMaterial({ color: c, roughness: .8, flatShading: true, ...o });
function mesh(geo, mat, x = 0, y = 0, z = 0, parent) { const m = new THREE.Mesh(geo, mat); m.position.set(x, y, z); m.castShadow = m.receiveShadow = true; (parent || scene).add(m); return m; }
function box(w, h, d, mat, x, y, z, parent) { return mesh(new THREE.BoxGeometry(w, h, d), mat, x, y, z, parent); }
function group(parent, x = 0, y = 0, z = 0) { const g = new THREE.Group(); g.position.set(x, y, z); (parent || scene).add(g); return g; }
function tex(w, h, draw) { const c = document.createElement('canvas'); c.width = w; c.height = h; const g = c.getContext('2d'); draw(g, w, h); const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace; t.anisotropy = 4; t.userData = { c, g }; return t; }
function labelTex(text, bg, fg = '#fff', w = 256, h = 128, size = 30) { return tex(w, h, (g) => { g.fillStyle = bg; g.fillRect(0, 0, w, h); g.fillStyle = fg; g.font = `bold ${size}px Roboto, sans-serif`; g.textAlign = 'center'; g.textBaseline = 'middle'; const lines = text.split('\n'); lines.forEach((l, i) => g.fillText(l, w / 2, h / 2 + (i - (lines.length - 1) / 2) * size * 1.15)); }); }

/* ---------- models ---------- */
const GLB = {}; const loader = new GLTFLoader();
function b64(s) { const bin = atob(s); const u = new Uint8Array(bin.length); for (let i = 0; i < bin.length; i++) u[i] = bin.charCodeAt(i); return u.buffer; }
const NEED = ['couch_pillows', 'armchair', 'lamp_standing', 'table_low', 'rug_rectangle_stripes_A', 'cactus_medium_A', 'crate', 'dumpster', 'truck_flat', 'shelf_B_large_decorated'];
async function loadAll() { await Promise.all(NEED.map(k => new Promise((res, rej) => loader.parse(b64(MODELS[k]), './', g => { GLB[k] = g; res(); }, rej)))); }
function inst(name, size, by = 'h') { const src = GLB[name]; const o = src.scene.clone(true); o.updateMatrixWorld(true); const b = new THREE.Box3().setFromObject(o); const d = b.getSize(new THREE.Vector3()); o.scale.multiplyScalar(size / (by === 'h' ? d.y : d.x)); o.updateMatrixWorld(true); const b2 = new THREE.Box3().setFromObject(o); const c = b2.getCenter(new THREE.Vector3()); o.position.set(-c.x, -b2.min.y, -c.z); o.traverse(m => { if (m.isMesh) m.castShadow = m.receiveShadow = true; }); const w = new THREE.Group(); w.add(o); w.userData.src = src; w.userData.inner = o; return w; }

/* ---------- journey state (example workspace) ---------- */
const ST = [
  { key: 'plan', name: 'Plan & design', fit: 8.6, ox: 0, cam: [0, 11.5, 6.2], tgt: [0, 0, .5], done: true },
  { key: 'quotes', name: 'Quotes', fit: 6.2, ox: 40, cam: [0, 4.6, 8.4], tgt: [0, 1.1, -.2], done: false },
  { key: 'demo', name: 'Demolition', fit: 7.6, ox: 80, cam: [1.2, 3.8, 9.6], tgt: [.3, 1.2, 0], done: false },
  { key: 'build', name: 'Build', fit: 7.8, ox: 120, cam: [.8, 4.2, 9.6], tgt: [.3, 1.4, 0], done: false },
  { key: 'finish', name: 'Finish', fit: 6.6, ox: 160, cam: [4.6, 4.8, 8.2], tgt: [0, 1.1, -.4], done: false }
];
let cur = 1;
const here = () => { const i = ST.findIndex(s => !s.done); return i < 0 ? 5 : i; };

/* ---------- labels ---------- */
const labelsEl = $('#labels'); const labels = [];
function label(st, html, pos, cls = '') { const el = document.createElement('div'); el.className = 'lab ' + cls; el.innerHTML = html; labelsEl.appendChild(el); const L = { el, st, pos, show: () => true, set(h, c) { if (h !== undefined && h !== L._h) { el.innerHTML = h; L._h = h; } if (c !== undefined) el.className = 'lab ' + c; } }; labels.push(L); return L; }
const pv = new THREE.Vector3();
function toScreen(v) { const q = v.clone().project(camera); return [(q.x + 1) / 2 * innerWidth, (1 - q.y) / 2 * innerHeight]; }
function placeLabels() { labels.forEach(L => { const vis = L.st === cur && !flight && L.show(); if (!vis) { L.el.style.display = 'none'; return; } const p = typeof L.pos === 'function' ? L.pos() : L.pos; pv.copy(p).project(camera); if (pv.z > 1) { L.el.style.display = 'none'; return; } L.el.style.display = ''; L.el.style.left = ((pv.x + 1) / 2 * innerWidth) + 'px'; L.el.style.top = ((1 - pv.y) / 2 * innerHeight) + 'px'; }); }

/* ---------- drag + physics core ---------- */
const drags = []; // meshes/groups with userData.drag
function draggable(obj, st, opts) { obj.userData.drag = { st, ...opts }; obj.traverse(m => { if (m.isMesh) m.userData.dragRoot = obj; }); drags.push(obj); }
const taps = []; function tappable(obj, st, fn) { obj.traverse(m => { if (m.isMesh) m.userData.tapRoot = obj; }); obj.userData.tap = { st, fn }; taps.push(obj); }
const bodies = [];
function body(o, v, w, floor = 0, opts = {}) { const b = { o, v: v.clone(), w: w.clone(), floor, half: opts.half ?? .22, alive: true, bounds: opts.bounds, onLand: opts.onLand }; bodies.push(b); return b; }
function stepBodies(dt) {
  for (const b of bodies) { if (!b.alive) continue; b.v.y -= 22 * dt; b.o.position.addScaledVector(b.v, dt); b.o.rotation.x += b.w.x * dt; b.o.rotation.y += b.w.y * dt; b.o.rotation.z += b.w.z * dt;
    const fl = typeof b.floor === 'function' ? b.floor(b.o.position) : b.floor;
    if (b.o.position.y < fl + b.half) { b.o.position.y = fl + b.half; if (Math.abs(b.v.y) > 1.2) buzz(4); b.v.y *= -.32; b.v.x *= .72; b.v.z *= .72; b.w.multiplyScalar(.6); if (b.v.length() < .25 && b.onLand) { b.onLand(b); } }
    if (b.bounds) { const p = b.o.position, B = b.bounds; if (p.x < B[0]) { p.x = B[0]; b.v.x *= -.5; } if (p.x > B[1]) { p.x = B[1]; b.v.x *= -.5; } if (p.z < B[2]) { p.z = B[2]; b.v.z *= -.5; } if (p.z > B[3]) { p.z = B[3]; b.v.z *= -.5; } } }
}
const tweens = [];
function tween(dur, fn, done) { tweens.push({ t0: clock.elapsedTime, dur: reduce ? .01 : dur, fn, done }); }
const clock = new THREE.Clock();

/* ---------- floors per station ---------- */
function floorFor(ox, color, w = 16, d = 12) { const f = mesh(new THREE.BoxGeometry(w, .4, d), M(color), ox, -.2, 0); f.receiveShadow = true; return f; }

/* =====================================================================
   1 PLAN — drag the kitchen island; live clearances (1 unit = 2 ft)
   ===================================================================== */
const plan = {};
function buildPlan() {
  const ox = ST[0].ox; floorFor(ox, 0xe9dfcf);
  const bp = tex(1024, 768, (g, w, h) => { g.fillStyle = '#21539e'; g.fillRect(0, 0, w, h); g.strokeStyle = 'rgba(255,255,255,.12)'; for (let x = 0; x < w; x += 32) { g.beginPath(); g.moveTo(x, 0); g.lineTo(x, h); g.stroke(); } for (let y = 0; y < h; y += 32) { g.beginPath(); g.moveTo(0, y); g.lineTo(w, y); g.stroke(); } g.fillStyle = 'rgba(255,255,255,.75)'; g.font = 'bold 30px Roboto, sans-serif'; g.fillText('FLOOR PLAN v3 · KITCHEN', 30, 50); g.font = '22px Roboto, sans-serif'; g.fillText('living room', 90, 690); g.fillText('window', 820, 50); });
  const sheet = mesh(new THREE.PlaneGeometry(10.4, 7.8), new THREE.MeshStandardMaterial({ map: bp, roughness: .9 }), ox, .01, .4); sheet.rotation.x = -Math.PI / 2; sheet.receiveShadow = true;
  const wm = M(0xffffff); const R = { x0: ox - 4.4, x1: ox + 3.9, z0: -2.7, z1: 3.1 }; plan.R = R;
  box(R.x1 - R.x0, .5, .14, wm, (R.x0 + R.x1) / 2, .25, R.z0); box(.14, .5, R.z1 - R.z0, wm, R.x0, .25, (R.z0 + R.z1) / 2); box(.14, .5, R.z1 - R.z0, wm, R.x1, .25, (R.z0 + R.z1) / 2);
  box(1.6, .5, .16, M(0x9fd0ff, { transparent: true, opacity: .7 }), ox + 2.4, .25, R.z0); // window
  const cm = M(0xf3efe8), top = M(0x5d5a63);
  const cBack = group(scene, ox + .8, 0, R.z0 + .36); box(6.2, .45, .66, cm, 0, .23, 0, cBack); box(6.3, .06, .72, top, 0, .48, 0, cBack);
  const cRight = group(scene, R.x1 - .36, 0, -.4); box(.66, .45, 3.8, cm, 0, .23, 0, cRight); box(.72, .06, 3.9, top, 0, .48, 0, cRight);
  plan.backEdge = R.z0 + .72; plan.rightEdge = R.x1 - .72;
  const isl = group(scene, ox + 1.4, 0, -.7); plan.isl = isl; plan.w = 2.2; plan.d = 1.0;
  const body1 = box(2.2, .45, 1.0, M(0xc9955c), 0, .23, 0, isl); const top1 = box(2.32, .07, 1.1, M(0xf4f1ea), 0, .49, 0, isl);
  plan.body = body1; plan.top = top1;
  const dm = new THREE.MeshBasicMaterial({ color: 0xffffff }); plan.dimB = box(.04, .02, 1, dm, 0, .6, 0); plan.dimR = box(1, .02, .04, dm, 0, .6, 0); plan.dimB.castShadow = plan.dimR.castShadow = false;
  plan.lb = label(0, '', () => plan.dimB.position); plan.lr = label(0, '', () => plan.dimR.position);
  plan.li = label(0, 'Island · tap to turn', () => isl.position.clone().add(new THREE.Vector3(0, .9, 0)), 'v');
  draggable(isl, 0, { plane: .5, move(p) { isl.position.x = clamp(p.x, R.x0 + plan.w / 2 + .1, plan.rightEdge - plan.w / 2 - .05); isl.position.z = clamp(p.z, plan.backEdge + plan.d / 2 + .05, R.z1 - plan.d / 2 - .1); planUpdate(); }, drop() { planUpdate(true); } });
  tappable(isl, 0, () => { const w = plan.w; plan.w = plan.d; plan.d = w; tween(.35, k => { isl.rotation.y = lerp(isl.userData.rot0 ?? 0, (isl.userData.rot0 ?? 0) + Math.PI / 2, ease(k)); }, () => { isl.userData.rot0 = (isl.userData.rot0 ?? 0) + Math.PI / 2; }); buzz(10); planUpdate(); });
  plan.okB = plan.okR = false; plan.saved = false; planUpdate();
}
function planUpdate(final) {
  const p = plan.isl.position; const gB = (p.z - plan.d / 2) - plan.backEdge; const gR = plan.rightEdge - (p.x + plan.w / 2);
  const inB = Math.round(gB * 24), inR = Math.round(gR * 24);
  plan.dimB.position.set(p.x, .6, plan.backEdge + gB / 2); plan.dimB.scale.z = Math.max(.01, gB);
  plan.dimR.position.set(plan.rightEdge - gR / 2, .6, p.z); plan.dimR.scale.x = Math.max(.01, gR);
  const okB = inB >= 42, okR = inR >= 42;
  if (okB !== plan.okB || okR !== plan.okR) { buzz(okB && okR ? [20, 30, 20] : 12); }
  plan.okB = okB; plan.okR = okR;
  plan.lb.set(`${inB}″ ${okB ? '✓' : '<small>need 42</small>'}`, 'lab ' + (okB ? 'ok' : 'bad')); plan.lr.set(`${inR}″ ${okR ? '✓' : '<small>need 42</small>'}`, 'lab ' + (okR ? 'ok' : 'bad'));
  if (cur === 0) renderCard();
}

/* =====================================================================
   2 QUOTES — a balance scale: put the quotes on the pans
   ===================================================================== */
const Q = { pans: [[], []], angle: 0, av: 0, asked: false, picked: null };
const QUOTES = {
  carter: { t: 'Carter Build Co.', amt: 48200, note: 'Excludes electrical', real: true },
  ridge: { t: 'Ridge Builders', amt: 55400, note: 'Includes electrical · sample', real: false },
  marco: { t: 'Marco Ruiz · electrical', amt: 6800, note: 'sample', real: false }
};
function paperTex(key) { const q = QUOTES[key]; return tex(256, 320, (g, w, h) => { g.fillStyle = key === 'marco' ? '#fff7e6' : '#fffdf8'; g.fillRect(0, 0, w, h); g.fillStyle = key === 'carter' ? '#6b4bd1' : key === 'ridge' ? '#2f7d6d' : '#d9822b'; g.fillRect(0, 0, w, 54); g.fillStyle = '#fff'; g.font = 'bold 22px Roboto,sans-serif'; g.fillText(key === 'marco' ? 'ELECTRICAL' : 'ESTIMATE', 16, 36); g.fillStyle = '#222'; g.font = 'bold 24px Roboto,sans-serif'; g.fillText(q.t.split(' ·')[0], 16, 96); g.fillStyle = '#ccc'; for (let i = 0; i < 6; i++) g.fillRect(16, 126 + i * 20, 130 + (i * 41) % 80, 8); g.fillStyle = '#222'; g.font = 'bold 40px Roboto,sans-serif'; g.fillText(money(q.amt), 16, 290); }); }
function buildQuotes() {
  const ox = ST[1].ox; floorFor(ox, 0xddd2c2);
  const wood = M(0xb78655); box(9, .3, 5, wood, ox, -.15, .3);
  const metal = M(0x8c8f99, { metalness: .4, roughness: .45, flatShading: false }); const brass = M(0xc9a24c, { metalness: .6, roughness: .35, flatShading: false });
  const sc = group(scene, ox, 0, -1.0); Q.sc = sc;
  mesh(new THREE.CylinderGeometry(.6, .8, .2, 24), metal, 0, .1, 0, sc); mesh(new THREE.CylinderGeometry(.07, .09, 2.5, 12), metal, 0, 1.35, 0, sc);
  const beam = group(sc, 0, 2.6, 0); Q.beam = beam; box(3.6, .1, .12, brass, 0, 0, 0, beam); mesh(new THREE.SphereGeometry(.14, 16, 12), brass, 0, 0, 0, beam);
  const needle = box(.04, .7, .04, M(0x6b4bd1), 0, -.4, .08, beam);
  Q.panG = [0, 1].map(i => { const g = group(scene, 0, 0, 0); const pan = mesh(new THREE.CylinderGeometry(.85, .7, .1, 28), brass, 0, 0, 0, g); [0, 1, 2].forEach(k => { const a = k / 3 * Math.PI * 2; const s = mesh(new THREE.CylinderGeometry(.012, .012, 1.3, 4), M(0x555555), Math.cos(a) * .45, .65, Math.sin(a) * .45, g); s.rotation.z = Math.cos(a) * -.33; s.rotation.x = Math.sin(a) * .33; s.castShadow = false; }); return g; });
  Q.papers = {};
  [['carter', -2.1, 1.5], ['marco', 0, 1.8], ['ridge', 2.1, 1.5]].forEach(([k, x, z]) => {
    const p = group(scene, ox + x, .06, z); const pm = mesh(new THREE.BoxGeometry(1.0, .02, 1.25), [M(0xffffff), M(0xffffff), new THREE.MeshStandardMaterial({ map: paperTex(k), roughness: .7 }), M(0xffffff), M(0xffffff), M(0xffffff)], 0, 0, 0, p);
    p.userData.key = k; p.userData.home = p.position.clone(); p.userData.on = -1; Q.papers[k] = p;
    if (k === 'ridge') p.visible = false;
    const pl = label(1, k === 'carter' ? 'Carter · $48,200' : k === 'ridge' ? 'Ridge · $55,400' : 'Marco · electrical', () => p.position.clone().add(new THREE.Vector3(0, .35, .75))); pl.show = () => p.visible && p.userData.on < 0 && !(dragging && dragging.obj === p);
    draggable(p, 1, { plane: 1.25, start() { if (p.userData.on >= 0) { Q.pans[p.userData.on] = Q.pans[p.userData.on].filter(x => x !== k); p.userData.on = -1; } }, move(pt) { p.position.set(pt.x, 1.25, pt.z); p.rotation.set(-.3, 0, 0); }, drop() { quoteDrop(p); } });
  });
  // the empty slot: ask Ridge Builders for a quote
  const slot = mesh(new THREE.PlaneGeometry(1.0, 1.25), new THREE.MeshBasicMaterial({ map: labelTex('Ask Ridge\nBuilders', 'rgba(107,75,209,.12)', '#6b4bd1', 256, 320, 34), transparent: true }), ox + 2.1, .02, 1.5); slot.rotation.x = -Math.PI / 2; slot.castShadow = false; Q.slot = slot;
  tappable(slot, 1, () => { if (Q.asked) return; Q.asked = true; slot.visible = false; const p = Q.papers.ridge; p.visible = true; const from = new THREE.Vector3(ox + 9, 4, -2); const to = p.userData.home.clone(); buzz(10); toast('Asked Ridge Builders · their quote just came in (sample)'); tween(1.1, k => { p.position.lerpVectors(from, to, ease(k)); p.position.y += Math.sin(k * Math.PI) * 2; p.rotation.y = (1 - k) * 4; }, () => { p.rotation.set(0, 0, 0); renderCard(); }); });
  Q.lL = label(1, '', () => Q.panG[0].position.clone().add(new THREE.Vector3(0, -.45, .6)), 'big'); Q.lR = label(1, '', () => Q.panG[1].position.clone().add(new THREE.Vector3(0, -.45, .6)), 'big');
  Q.lD = label(1, '', () => new THREE.Vector3(ox, 3.35, -1), 'v'); Q.lW = label(1, 'Excludes electrical', () => Q.panG[0].position.clone().add(new THREE.Vector3(0, .55, 0)), 'bad');
  Q.lL.show = () => Q.pans[0].length > 0; Q.lR.show = () => Q.pans[1].length > 0; Q.lD.show = () => Q.pans[0].length && Q.pans[1].length; Q.lW.show = () => Q.pans.some(p => p.includes('carter')) && !Q.pans.some(p => p.includes('marco'));
  Q.lP = label(1, 'Drop a quote<br>on a pan', () => Q.panG[0].position.clone().add(new THREE.Vector3(0, .5, 0))); Q.lP.show = () => !Q.pans[0].length && !Q.pans[1].length;
}
function panPos(i) { const a = Q.angle; const L = 1.7; const ox = ST[1].ox; return new THREE.Vector3(ox + (i ? 1 : -1) * L * Math.cos(a), 2.6 + (i ? -1 : 1) * L * Math.sin(a) - 1.35, -1.0); }
function quoteDrop(p) {
  const k = p.userData.key; let best = -1, bd = 1.3;
  bd = 90; const sp = toScreen(p.position); [0, 1].forEach(i => { const q = toScreen(panPos(i)); const d = Math.hypot(sp[0] - q[0], sp[1] - q[1]); if (d < bd) { bd = d; best = i; } });
  if (best < 0) { const from = p.position.clone(), to = p.userData.home; tween(.4, t => { p.position.lerpVectors(from, to, ease(t)); }, () => p.rotation.set(0, 0, 0)); renderCard(); return; }
  // marco's electrical only joins a quote that excludes electrical
  if (k === 'marco' && !Q.pans[best].includes('carter')) { toast('Marco’s number belongs with the quote that excludes electrical'); const from = p.position.clone(), to = p.userData.home; tween(.4, t => p.position.lerpVectors(from, to, ease(t)), () => p.rotation.set(0, 0, 0)); buzz([10, 30, 10]); return; }
  if ((k === 'carter' || k === 'ridge') && Q.pans[best].some(x => x !== 'marco')) best = 1 - best;
  Q.pans[best].push(k); p.userData.on = best; buzz(18); Q.av += (best ? -1 : 1) * .02 * (QUOTES[k].amt / 20000);
  renderCard();
}
function quotesTick(dt) {
  const w = Q.pans.map(arr => arr.reduce((s, k) => s + QUOTES[k].amt, 0));
  const target = clamp((w[0] - w[1]) / 18000, -.32, .32) * (w[0] && w[1] ? 1 : .5);
  Q.av += ((-target) - Q.angle) * 30 * dt; Q.av *= Math.pow(.04, dt); Q.angle += Q.av * dt * 4; Q.beam.rotation.z = -Q.angle;
  [0, 1].forEach(i => { const pp = panPos(i); Q.panG[i].position.copy(pp); Q.pans[i].forEach((k, j) => { const p = Q.papers[k]; if (dragging && dragging.obj === p) return; p.position.set(pp.x + (j % 2 ? .12 : -.08), pp.y + .08 + j * .03, pp.z + (j ? .1 : 0)); p.rotation.set(0, j * .25, 0); }); });
  const tot = w.map((v, i) => Q.pans[i].length ? money(v) : '');
  Q.lL.set(tot[0] + (Q.pans[0].includes('marco') ? ' <small>with electrical</small>' : ''), Q.picked === 0 ? 'lab big ok' : 'lab big'); Q.lR.set(tot[1] + (Q.pans[1].includes('marco') ? ' <small>with electrical</small>' : ''), Q.picked === 1 ? 'lab big ok' : 'lab big');
  if (w[0] && w[1]) { const d = Math.abs(w[0] - w[1]); const cheaper = w[0] < w[1] ? 0 : 1; const name = q => q.includes('carter') ? 'Carter' : 'Ridge'; Q.lD.set(d < 1000 ? `Nearly even · ${money(d)} apart` : `${name(Q.pans[cheaper])} is ${money(d)} less`); }
}

/* =====================================================================
   3 DEMOLITION — knock out what blocks it
   ===================================================================== */
const D = { bricks: [], blockers: [], cleared: 0, down: false, hauled: 0 };
const BLOCKERS = [
  { id: 'insurance', t: 'Call insurance about roof claim', short: 'Insurance\ncall', real: true },
  { id: 'haul', t: 'Haul-away is in both quotes', short: 'Haul-away\nin quotes', real: true },
  { id: 'permit', t: 'Building permit approved', short: 'Permit\napproved', real: true },
  { id: 'utilities', t: 'Utilities shut-off booked', short: 'Utilities\noff', real: false }];
function buildDemo() {
  const ox = ST[2].ox; floorFor(ox, 0xc9d7b4);
  const brickM = [M(0xb5654a), M(0xa85c43), M(0xbd7357)];
  const BW = .9, BH = .45; const cols = 7, rows = 5; const slots = { '1,1': 0, '5,1': 1, '2,3': 2, '4,3': 3 };
  for (let r = 0; r < rows; r++) for (let c = 0; c < cols; c++) {
    const x = ox - 2.7 + c * BW + (r % 2 ? BW / 2 : 0) - .2; const y = BH / 2 + r * BH; const key = c + ',' + r; const bi = slots[key];
    let m;
    if (bi !== undefined) { const b = BLOCKERS[bi]; m = box(BW - .04, BH - .04, .5, [M(0xe39a3b), M(0xe39a3b), M(0xe39a3b), M(0xe39a3b), new THREE.MeshStandardMaterial({ map: labelTex(b.short, '#e39a3b', '#fff', 256, 128, 34) }), M(0xe39a3b)], x, y, 0); m.userData.blocker = bi; D.blockers.push(m); tappable(m, 2, () => askBlocker(m)); const bm = m; const bl = label(2, b.short.replace('\n', ' '), () => bm.position.clone().add(new THREE.Vector3(0, 0, .3)), 'bad tap'); bl.show = () => !bm.userData.out; bl.el.addEventListener('click', () => askBlocker(bm)); }
    else m = box(BW - .04, BH - .04, .5, brickM[(c + r) % 3], x, y, 0);
    m.userData.home = m.position.clone(); D.bricks.push(m);
  }
  const dump = inst('dumpster', 1.3); dump.position.set(ox + 4.4, 0, 2.2); dump.rotation.y = -.5; scene.add(dump); D.dump = dump;
  D.lc = label(2, '', () => new THREE.Vector3(ox + .3, 2.75, 0), 'v');
  D.lh = label(2, '', () => dump.position.clone().add(new THREE.Vector3(0, 1.6, 0))); D.lh.show = () => D.down;
}
function askBlocker(m) {
  if (m.userData.out) return; const b = BLOCKERS[m.userData.blocker];
  ask(`<h3>${esc(b.t)}</h3><p>${b.real ? 'From this journey.' : 'Sample step.'} Mark it done to knock this brick out.</p><div class="acts"><button class="btn" id="yes">Mark done</button><button class="btn ghost" id="no">Not yet</button></div>`, () => {
    $('#yes').addEventListener('click', () => { closeAsk(); m.userData.out = true; D.cleared++; buzz([15, 20, 40]);
      body(m, new THREE.Vector3((Math.random() - .5) * 2, 3, 5.5), new THREE.Vector3(4, 2, 1), 0, { half: .2 });
      toast(`${b.t} · done`); if (D.cleared === 4) setTimeout(collapse, 500); renderCard(); });
    $('#no').addEventListener('click', closeAsk);
  });
}
function collapse() {
  D.down = true; buzz([40, 30, 80]); const ox = ST[2].ox;
  D.bricks.forEach((m, i) => { if (m.userData.out) return; const r = m.position.y; body(m, new THREE.Vector3((m.position.x - ox) * .6 + (Math.random() - .5), 1 + Math.random() * 2, 2 + r * 1.2 + Math.random()), new THREE.Vector3(Math.random() * 6, Math.random() * 3, Math.random() * 6), 0, { half: .2, bounds: [ox - 7, ox + 7, -4, 5] }); m.userData.rubble = true; draggable(m, 2, { plane: .6, start() { const b = bodies.find(b => b.o === m); if (b) b.alive = false; }, move(p) { m.position.set(p.x, .8, p.z); }, drop(vel) { const bb = bodies.find(b => b.o === m); const v = vel.clone(); v.y = 3 + vel.length() * .15; if (bb) { bb.alive = true; bb.v.copy(v); } else body(m, v, new THREE.Vector3(3, 2, 1), 0, { half: .2 }); } }); });
  toast('Nothing blocks demolition now · Dec 15 is go'); ST[2].done = true; renderAll();
}
function demoTick() {
  if (!D.dump) return; const d = D.dump.position; let n = 0;
  bodies.forEach(b => { const o = b.o; if (!o.userData.rubble) return; const inside = Math.hypot(o.position.x - d.x, o.position.z - d.z) < .8; b.floor = inside ? .55 : 0; if (inside && o.position.y < 1.4) n++; });
  if (n !== D.hauled) { if (n > D.hauled) buzz(12); D.hauled = n; }
  D.lc.set(D.down ? 'Wall’s down · ready for Dec 15' : `${D.cleared} of 4 blockers cleared`, D.down ? 'lab ok' : 'lab v');
  D.lh.set(`Fling rubble in · ${D.hauled} hauled`);
}

/* =====================================================================
   4 BUILD — drag each panel into its bay, in order
   ===================================================================== */
const B = { bays: [], panels: [], placed: 0, inspected: false };
const PANELS = [
  { id: 'frame', t: 'Framing', c: 0xd9b77e },
  { id: 'elec', t: 'Electrical', c: 0x8d9097, locked: true },
  { id: 'insul', t: 'Insulation', c: 0xf2a7b8 },
  { id: 'dry', t: 'Drywall', c: 0xf4f1ea }];
function buildBuild() {
  const ox = ST[3].ox; floorFor(ox, 0xd6cdbf); box(7.4, .25, 1.2, M(0xa7a39b), ox + .3, .12, -.6);
  const stud = M(0xd9b77e);
  for (let i = 0; i < 4; i++) { const x = ox - 2.1 + i * 1.6; [-0.75, 0, .75].forEach(dx => box(.09, 2.4, .12, stud, x + dx, 1.45, -.6)); box(1.6, .09, .14, stud, x, 2.68, -.6);
    const slot = mesh(new THREE.PlaneGeometry(1.45, 2.3), new THREE.MeshBasicMaterial({ color: 0x6b4bd1, transparent: true, opacity: .12, depthWrite: false }), x, 1.45, -.5); slot.castShadow = false; B.bays.push({ x, slot }); }
  const truck = inst('truck_flat', 1.6); truck.position.set(ox + 5.2, 0, 2.8); truck.rotation.y = -1.2; scene.add(truck);
  PANELS.forEach((P, i) => {
    const g = group(scene, ox - 2.4 + i * 1.65, .08, 2.6); const pm = box(1.4, .08, 2.2, P.id === 'elec' ? M(P.c, { metalness: .2 }) : M(P.c), 0, 0, 0, g);
    const lt = mesh(new THREE.PlaneGeometry(1.2, .5), new THREE.MeshBasicMaterial({ map: labelTex(`${i + 1} · ${P.t}`, 'rgba(255,255,255,.92)', '#1d1830', 384, 160, 44), transparent: true }), 0, .05, 0, g); lt.rotation.x = -Math.PI / 2; lt.castShadow = false;
    if (P.id === 'elec') { for (let k = 0; k < 3; k++) { const wire = box(1.2, .03, .04, M([0xd94a3d, 0x2f6fd9, 0x3d9a6b][k]), 0, .06, -.6 - k * .15, g); } }
    g.userData.i = i; g.userData.home = g.position.clone(); B.panels.push(g);
    const gl = label(3, `${i + 1} · ${P.t}`, () => g.position.clone().add(new THREE.Vector3(0, .1, .9))); gl.show = () => !g.userData.placed && !(dragging && dragging.obj === g) && !(P.locked && !B.inspected);
    draggable(g, 3, { plane: 1.3, start() { if (g.userData.placed) return false; if (P.locked && !B.inspected) { askInspection(); return false; } }, move(p) { g.position.set(p.x, 1.3, p.z); g.rotation.x = lerp(g.rotation.x, -Math.PI / 2 * clamp((2.4 - p.z) / 2.4), .3); }, drop() { buildDrop(g); } });
  });
  B.lock = label(3, '🔒 Book inspection', () => B.panels[1].position.clone().add(new THREE.Vector3(0, .5, 0)), 'bad tap'); B.lock.show = () => !B.inspected;
  B.lock.el.addEventListener('click', askInspection);
  B.ln = label(3, '', () => new THREE.Vector3(ox + .3, 3.3, -.6), 'v');
}
function askInspection() { ask(`<h3>Book electrical inspection</h3><p>Electrical can’t go in until the inspection is booked. It’s blocked on Marco scheduling the panel upgrade.</p><div class="acts"><button class="btn" id="yes">Booked it</button><button class="btn ghost" id="no">Not yet</button></div>`, () => { $('#yes').addEventListener('click', () => { closeAsk(); B.inspected = true; buzz([15, 20, 15]); toast('Electrical inspection booked · panel unlocked'); renderCard(); }); $('#no').addEventListener('click', closeAsk); }); }
function buildDrop(g) {
  const i = g.userData.i; const bay = B.bays[i]; const near = Math.abs(g.position.x - bay.x) < .9 && g.position.z < 1.4;
  const back = () => { const from = g.position.clone(), r0 = g.rotation.x; tween(.45, t => { g.position.lerpVectors(from, g.userData.home, ease(t)); g.rotation.x = lerp(r0, 0, ease(t)); }); };
  if (!near) { const wrong = B.bays.findIndex(b => Math.abs(g.position.x - b.x) < .7) ; if (wrong >= 0 && g.position.z < 1.4) toast(`That bay is for ${PANELS[wrong].t}`); back(); buzz([8, 30, 8]); return; }
  if (i !== B.placed) { toast(`${PANELS[i].t} goes after ${PANELS[B.placed].t}`); back(); buzz([8, 30, 8]); return; }
  g.userData.placed = true; B.placed++; buzz(25); const from = g.position.clone(), r0 = g.rotation.x;
  tween(.35, t => { g.position.lerpVectors(from, new THREE.Vector3(bay.x, 1.45, -.47 + i * .02), ease(t)); g.rotation.x = lerp(r0, -Math.PI / 2, ease(t)); }, () => { bay.slot.visible = false; if (B.placed === 4) { ST[3].done = true; toast('Build complete · ready for finishes'); buzz([30, 40, 70]); roofDrop(); renderAll(); } else renderCard(); });
}
function roofDrop() { const ox = ST[3].ox; const r = box(7.0, .2, 1.6, M(0x3f4b5e), ox + .3, 7, -.6); tween(.8, t => { r.position.y = lerp(7, 2.85, t * t); }, () => buzz(30)); }
function buildTick() { B.ln.set(B.placed === 4 ? 'Framed, wired, insulated, closed up' : `Next: ${PANELS[B.placed].t}`, B.placed === 4 ? 'lab ok' : 'lab v'); }

/* =====================================================================
   5 FINISH — paint with your finger, move the furniture in
   ===================================================================== */
const F = { color: '#9fae8f', walls: [], placed: new Set(), crateOpen: false };
const SWATCHES = [['Warm white', '#f1e9dc'], ['Sage', '#9fae8f'], ['Clay', '#c98f6b'], ['Ink blue', '#43507a']];
function buildFinish() {
  const ox = ST[4].ox; floorFor(ox, 0xe3d9cb);
  box(6.2, .1, 4.6, M(0xc9955c), ox, .05, -.6);
  [[6.2, ox, -2.9, 0], [4.6, ox - 3.1, -.6, Math.PI / 2]].forEach(([w, x, z, ry]) => {
    const t = tex(512, 220, (g, W, H) => { g.fillStyle = '#e6e2da'; g.fillRect(0, 0, W, H); g.fillStyle = 'rgba(0,0,0,.05)'; for (let i = 0; i < 30; i++) g.fillRect((i * 97) % W, (i * 53) % H, 40, 20); });
    const m = mesh(new THREE.PlaneGeometry(w, 2.7), new THREE.MeshStandardMaterial({ map: t, roughness: .9 }), x, 1.4, z); m.rotation.y = ry; m.userData.paint = t; m.userData.grid = new Uint8Array(24 * 10); F.walls.push(m);
  });
  box(6.3, .2, .2, M(0xffffff), ox, .1, -2.85);
  const shelf = box(6.4, .25, .9, M(0xbfa98c), ox, .12, 2.6);
  const items = [['couch_pillows', .85, -2.2], ['armchair', .8, -.9], ['table_low', .36, .2], ['lamp_standing', 1.5, 1.2], ['cactus_medium_A', .5, 2.2]];
  F.items = items.map(([n, h, x]) => { const o = inst(n, h); o.position.set(ox + x, .25, 2.6); scene.add(o); o.userData.home = o.position.clone();
    draggable(o, 4, { plane: .25, move(p) { o.position.set(p.x, .9, p.z); }, drop() { const p = o.position; if (p.z < 1.5 && p.z > -2.7 && p.x > ox - 3 && p.x < ox + 3.1) { F.placed.add(n); tween(.3, t => { o.position.y = lerp(.9, .1, t * t); }, () => { buzz(14); renderCard(); }); } else { F.placed.delete(n); const from = p.clone(); tween(.4, t => o.position.lerpVectors(from, o.userData.home, ease(t))); renderCard(); } } }); return o; });
  const crate = inst('crate', .5); crate.position.set(ox + 2.5, .1, .8); scene.add(crate); F.crate = crate;
  const mx = new THREE.AnimationMixer(crate.userData.inner); const clip = crate.userData.src.animations.find(a => a.name === 'open'); F.crateAct = mx.clipAction(clip); F.crateAct.setLoop(THREE.LoopOnce); F.crateAct.clampWhenFinished = true; mixers.push(mx);
  tappable(crate, 4, () => { if (F.crateOpen) return; F.crateOpen = true; F.crateAct.reset().play(); buzz(12); toast('Tile samples · one day late · matte white, sage, clay'); });
  F.lp = label(4, '', () => new THREE.Vector3(ox - 1.2, 2.95, -2.9), 'v');
  F.lc = label(4, 'Tile samples', () => crate.position.clone().add(new THREE.Vector3(0, .8, 0)), 'bad'); F.lc.show = () => !F.crateOpen;
}
let lastPaint = null;
function paintAt(hit, start) { const m = hit.object; const t = m.userData.paint; const { c, g } = t.userData; const u = hit.uv.x, v = 1 - hit.uv.y; const X = u * c.width, Y = v * c.height;
  g.strokeStyle = g.fillStyle = F.color; g.lineWidth = 56; g.lineCap = 'round'; g.lineJoin = 'round'; g.beginPath(); if (!start && lastPaint && lastPaint.m === m) { g.moveTo(lastPaint.X, lastPaint.Y); g.lineTo(X, Y); g.stroke(); } else { g.arc(X, Y, 28, 0, Math.PI * 2); g.fill(); } lastPaint = { m, X, Y }; t.needsUpdate = true;
  if (Math.random() < .15) buzz(3); const prev = m.userData.prevUV || [u, v]; for (let k = 0; k <= 8; k++) { const uu = lerp(start ? u : prev[0], u, k / 8), vv = lerp(start ? v : prev[1], v, k / 8); const gx = Math.min(23, Math.floor(uu * 24)), gy = Math.min(9, Math.floor(vv * 10)); for (let dx = -1; dx <= 1; dx++) for (let dy = -1; dy <= 1; dy++) { const x = gx + dx, y = gy + dy; if (x >= 0 && x < 24 && y >= 0 && y < 10) m.userData.grid[y * 24 + x] = 1; } } m.userData.prevUV = [u, v]; }
function painted() { let s = 0, n = 0; F.walls.forEach(w => { w.userData.grid.forEach(v => { s += v; n++; }); }); return s / n; }
function finishTick() { F.lp.set(`${Math.round(painted() * 100)}% painted · ${F.placed.size} of 5 moved in`); }

/* ---------- UI: trail ---------- */
function renderTrail() {
  const h = here(); const xs = ST.map((s, i) => 8 + i * 21);
  $('#trail').innerHTML = `<svg viewBox="0 0 100 62" preserveAspectRatio="none"><path d="M8 22 C 18 8, 22 36, 29 22 S 44 8, 50 22 S 64 36, 71 22 S 86 8, 92 22" fill="none" stroke="#d8d0ea" stroke-width="2.4" stroke-dasharray="1 2.4" stroke-linecap="round" vector-effect="non-scaling-stroke"/></svg>` +
    ST.map((s, i) => `<button class="stop ${s.done ? 'done' : i === h ? 'here' : ''} ${i === cur ? 'on' : ''}" style="left:${xs[i]}%" data-i="${i}"><i>${s.done ? '✓' : i + 1}</i><span>${esc(s.name)}</span></button>`).join('');
  document.querySelectorAll('.stop').forEach(b => b.addEventListener('click', () => go(+b.dataset.i)));
  const d = ST.filter(s => s.done).length; $('#prog').style.width = d / 5 * 100 + '%'; $('#sub').textContent = d === 5 ? 'Trail · finished' : `Trail · stop ${h + 1} of 5 · ${d * 20}% of the way`;
}

/* ---------- UI: card ---------- */
function chip(t, cls = '') { return `<span class="chip ${cls}">${t}</span>`; }
function renderCard() {
  const s = ST[cur], h = here(); const kick = s.done ? 'DONE' : cur === h ? 'YOU ARE HERE' : 'LATER · TRY IT NOW';
  let doTxt = '', res = '', chips = '', acts = '';
  if (cur === 0) { doTxt = 'Drag the island. Tap it to turn it. Both sides need 42″ to walk past.'; const ok = plan.okB && plan.okR; res = ok ? 'Both aisles clear 42″ — the clearance from your saved kitchen-layouts link.' : 'Too tight. The kitchen-layouts link you saved says leave at least 42″.'; chips = chip(plan.okB ? 'Back aisle ✓' : 'Back aisle', plan.okB ? 'ok' : 'bad') + chip(plan.okR ? 'Side aisle ✓' : 'Side aisle', plan.okR ? 'ok' : 'bad') + (plan.saved ? chip('Floor plan v4 saved', 'ok') : ''); acts = `<button class="btn" id="savePlan" ${ok && !plan.saved ? '' : 'disabled'}>${plan.saved ? 'Saved' : 'Save as floor plan v4'}</button>`; }
  if (cur === 1) { const both = Q.pans.flat().filter(k => k !== 'marco').length === 2; const fair = !Q.pans.flat().includes('carter') || Q.pans.flat().includes('marco');
    doTxt = !Q.asked ? 'Drag Jamie’s estimate onto a pan. Tap the empty slot to ask Ridge Builders.' : !both ? 'Put both quotes on the scale.' : !fair ? 'Carter leaves out electrical. Drag Marco’s number onto Carter’s pan.' : 'Pick the one you want.';
    res = both && fair ? 'Without electrical Carter looked $7,200 cheaper. With Marco’s number it’s $400 more. Ridge includes electrical (sample numbers).' : 'Jamie’s estimate leaves out electrical — compare like for like.';
    chips = chip('✓ Carter estimate', 'ok') + chip(Q.asked ? '✓ Second quote' : 'Second quote', Q.asked ? 'ok' : '') + chip(both && fair ? '✓ Line by line' : 'Line by line', both && fair ? 'ok' : '') + (Q.picked !== null ? chip('Chose ' + (Q.pans[Q.picked].includes('carter') ? 'Carter + Marco' : 'Ridge'), 'ok') : '');
    if (both && fair && Q.picked === null) { const nm = i => Q.pans[i].includes('carter') ? 'Carter + Marco' : 'Ridge'; acts = `<button class="btn" id="pickL">Go with ${nm(0)}</button><button class="btn ghost" id="pickR">Go with ${nm(1)}</button>`; } }
  if (cur === 2) { doTxt = D.down ? 'Fling the rubble into the dumpster, just for fun.' : 'Tap each orange brick. Mark it done and it pops out. Clear all four and the wall comes down.'; res = D.down ? 'Demolition is clear to start Dec 15.' : 'Demolition starts Dec 15. These four things stand in the way.'; chips = BLOCKERS.map((b, i) => chip((D.blockers[i].userData.out ? '✓ ' : '') + b.short.replace('\n', ' '), D.blockers[i].userData.out ? 'ok' : '')).join(''); }
  if (cur === 3) { doTxt = B.placed === 4 ? 'All four in. The roof’s on.' : 'Drag each panel up into its bay, in order. Electrical is locked until the inspection is booked.'; res = B.inspected ? 'Inspection booked — Marco can rough in the wiring.' : 'Electrical inspection is blocked until Marco schedules the panel upgrade.'; chips = PANELS.map((p, i) => chip((i < B.placed ? '✓ ' : '') + p.t, i < B.placed ? 'ok' : (p.locked && !B.inspected ? 'bad' : ''))).join(''); }
  if (cur === 4) { const pc = painted(); doTxt = 'Pick a colour and paint the walls with your finger. Drag furniture off the shelf into the room.'; res = 'Colours come from your Kitchen inspiration photo. Tile samples are a day late.'; chips = chip(`${Math.round(pc * 100)}% painted`, pc > .5 ? 'ok' : '') + chip(`${F.placed.size} of 5 in`, F.placed.size >= 3 ? 'ok' : ''); acts = `<button class="btn" id="saveFin" ${pc > .5 && F.placed.size >= 3 && !s.done ? '' : 'disabled'}>${s.done ? 'Saved as finish picks' : 'Save finish picks'}</button>`; }
  $('#card').innerHTML = `<div class="kick">${kick} · STOP ${cur + 1}</div><h2>${esc(s.name)}</h2><div class="do"><b>👆</b><span>${esc(doTxt)}</span></div><div class="result">${esc(res)}</div><div class="chips">${chips}</div>${acts ? `<div class="acts">${acts}</div>` : ''}`;
  const sp = $('#savePlan'); if (sp) sp.addEventListener('click', () => { plan.saved = true; buzz([15, 20, 30]); toast('Saved as Floor plan v4 · Priya will see it'); renderCard(); });
  const pl = $('#pickL'), pr = $('#pickR'); [pl, pr].forEach((b, i) => b && b.addEventListener('click', () => { Q.picked = i; ST[1].done = true; buzz([20, 30, 60]); toast('Compare contractor quotes · done'); renderAll(); }));
  const sf = $('#saveFin'); if (sf) sf.addEventListener('click', () => { ST[4].done = true; buzz([30, 40, 80]); toast('Finish picks saved · journey complete'); renderAll(); });
  layoutTray();
}
function renderAll() { renderTrail(); renderCard(); }
function layoutTray() { const tray = $('#tray'); if (cur !== 4) { tray.innerHTML = ''; return; } if (!tray.children.length) { tray.innerHTML = SWATCHES.map(([n, c]) => `<button class="sw ${c === F.color ? 'on' : ''}" style="background:${c}" aria-label="${n}" data-c="${c}"></button>`).join(''); tray.querySelectorAll('.sw').forEach(b => b.addEventListener('click', () => { F.color = b.dataset.c; tray.querySelectorAll('.sw').forEach(x => x.classList.toggle('on', x === b)); buzz(8); })); } tray.style.bottom = ($('#card').offsetHeight + 18) + 'px'; }

/* ---------- toast / ask / hint ---------- */
let tt; function toast(m) { const t = $('#toast'); t.innerHTML = `<span>${esc(m)}</span>`; t.classList.add('show'); clearTimeout(tt); tt = setTimeout(() => t.classList.remove('show'), 3200); }
function ask(html, bind) { $('#asksheet').innerHTML = html; $('#ask').classList.add('show'); bind(); }
function closeAsk() { $('#ask').classList.remove('show'); }
$('#ask').addEventListener('click', e => { if (e.target.id === 'ask') closeAsk(); });
const HINTS = ['Drag the island', 'Drag a quote onto the scale', 'Tap an orange brick', 'Drag panel 1 into its bay', 'Paint the wall with your finger'];
const hinted = new Set();
function showHint() { const h = $('#hint'); if (hinted.has(cur)) { h.style.opacity = 0; return; } h.innerHTML = `<span class="hand">👆</span>${HINTS[cur]}`; h.style.opacity = 1; h.style.top = '128px'; }

/* ---------- camera ---------- */
let flight = null, yaw = 0, yawV = 0;
const camPos = new THREE.Vector3(), camTgt = new THREE.Vector3();
function stationCam(i, out, tgt) { const s = ST[i]; tgt.set(s.ox + s.tgt[0], s.tgt[1], s.tgt[2]); const off = new THREE.Vector3(s.cam[0] - s.tgt[0], s.cam[1] - s.tgt[1], s.cam[2] - s.tgt[2]); const hf = 2 * Math.atan(Math.tan(THREE.MathUtils.degToRad(camera.fov) / 2) * camera.aspect); const fitD = (s.fit / 2) / Math.tan(hf / 2) + 1.5; off.setLength(Math.max(off.length(), fitD)); off.applyAxisAngle(new THREE.Vector3(0, 1, 0), yaw); out.copy(tgt).add(off); }
function go(i) { if (i === cur && !flight) return; const p0 = camera.position.clone(), t0 = camTgt.clone(); cur = i; yaw = 0; const p1 = new THREE.Vector3(), t1 = new THREE.Vector3(); stationCam(i, p1, t1); const mid = p0.clone().lerp(p1, .5); mid.y += 8; flight = { p0, t0, p1, t1, mid, start: clock.elapsedTime, dur: reduce ? .01 : 1.1 }; sun.position.set(ST[i].ox + 6, 14, 8); sun.target.position.set(ST[i].ox, 0, 0); closeAsk(); renderAll(); showHint(); }

/* ---------- pointer ---------- */
const ray = new THREE.Raycaster(), ndc = new THREE.Vector2(); let dragging = null, down = null, painting = false;
const plane = new THREE.Plane(), hitP = new THREE.Vector3();
function setRay(e) { ndc.set(e.clientX / innerWidth * 2 - 1, -(e.clientY / innerHeight) * 2 + 1); ray.setFromCamera(ndc, camera); }
function rootOf(o, key) { while (o) { if (o.userData[key]) return o.userData[key]; o = o.parent; } return null; }
canvas.addEventListener('pointerdown', e => {
  if (flight) return; setRay(e); canvas.setPointerCapture(e.pointerId); hinted.add(cur); $('#hint').style.opacity = 0;
  down = { x: e.clientX, y: e.clientY, t: performance.now() };
  if (cur === 4) { const wh = ray.intersectObjects(F.walls); if (wh.length && !ray.intersectObjects(F.items, true).length) { painting = true; paintAt(wh[0], true); return; } }
  const hits = ray.intersectObjects(drags.filter(d => d.userData.drag.st === cur && d.visible), true);
  if (hits.length) { const obj = hits[0].object.userData.dragRoot; const D0 = obj.userData.drag; if (D0.start && D0.start() === false) { dragging = null; return; } plane.set(new THREE.Vector3(0, 1, 0), -D0.plane); ray.ray.intersectPlane(plane, hitP); const off = obj.position.clone().sub(hitP); off.y = 0; dragging = { obj, off, last: hitP.clone(), lt: performance.now(), vel: new THREE.Vector3(), moved: false }; buzz(6); }
});
canvas.addEventListener('pointermove', e => {
  if (!down) return; setRay(e);
  if (painting) { const wh = ray.intersectObjects(F.walls); if (wh.length) paintAt(wh[0]); return; }
  if (dragging) { if (!ray.ray.intersectPlane(plane, hitP)) return; const now = performance.now(); const dt = Math.max(1, now - dragging.lt) / 1000; dragging.vel.copy(hitP).sub(dragging.last).divideScalar(dt).multiplyScalar(.5).add(dragging.vel.clone().multiplyScalar(.5)); dragging.last.copy(hitP); dragging.lt = now; if (Math.hypot(e.clientX - down.x, e.clientY - down.y) > 6) dragging.moved = true; if (dragging.moved) dragging.obj.userData.drag.move(hitP.clone().add(dragging.off)); return; }
  // empty space: gentle orbit
  const dx = e.movementX || 0; yawV = clamp(yawV - dx * .004, -.08, .08);
});
canvas.addEventListener('pointerup', e => {
  const wasTap = down && Math.hypot(e.clientX - down.x, e.clientY - down.y) < 8; down = null;
  if (painting) { painting = false; renderCard(); return; }
  if (dragging) { const d = dragging; dragging = null; if (d.moved) { d.obj.userData.drag.drop(d.vel.clampLength(0, 14)); return; } }
  if (wasTap) { setRay(e); const th = ray.intersectObjects(taps.filter(t => t.userData.tap.st === cur && t.visible), true); if (th.length) { const r = rootOf(th[0].object, 'tapRoot'); if (r) r.userData.tap.fn(); } }
});

/* ---------- loop ---------- */
const mixers = [];
function resize() { const w = innerWidth, h = innerHeight; renderer.setSize(w, h, false); camera.aspect = w / h; camera.fov = w < h ? 44 : 34; const top = 112, bot = $('#card').offsetHeight + 20; camera.setViewOffset(w, h, 0, (bot - top) / 2, w, h); camera.updateProjectionMatrix(); layoutTray(); }
addEventListener('resize', resize); new ResizeObserver(resize).observe($('#card'));
function frame() {
  const dt = Math.min(clock.getDelta(), .05); const t = clock.elapsedTime;
  for (let i = tweens.length - 1; i >= 0; i--) { const tw = tweens[i]; const k = clamp((t - tw.t0) / tw.dur); tw.fn(k); if (k >= 1) { tweens.splice(i, 1); tw.done && tw.done(); } }
  stepBodies(dt); quotesTick(dt); demoTick(); buildTick(); finishTick(); mixers.forEach(m => m.update(dt));
  if (flight) { const k = ease(clamp((t - flight.start) / flight.dur)); const a = flight.p0.clone().lerp(flight.mid, k), b = flight.mid.clone().lerp(flight.p1, k); camera.position.copy(a.lerp(b, k)); camTgt.lerpVectors(flight.t0, flight.t1, k); if (k >= 1) flight = null; }
  else { yaw = clamp(yaw + yawV, -.5, .5); yawV *= .9; if (!down) yaw *= .985; stationCam(cur, camPos, camTgt); camera.position.lerp(camPos, .2); }
  camera.lookAt(camTgt);
  if (Q.papers && !Q.asked) Q.slot.material.opacity = .7 + Math.sin(t * 3) * .3;
  D.blockers && D.blockers.forEach((m, i) => { if (!m.userData.out) m.position.z = m.userData.home.z + Math.max(0, Math.sin(t * 3 + i)) * .06; });
  placeLabels(); renderer.render(scene, camera); requestAnimationFrame(frame);
}

/* ---------- test hook: screen position of named things ---------- */
window.__scr = name => { const m = { island: () => plan.isl.position, carter: () => Q.papers.carter.position, marco: () => Q.papers.marco.position, ridge: () => Q.papers.ridge.position, slot: () => Q.slot.position, panL: () => panPos(0), panR: () => panPos(1),
  wall: () => new THREE.Vector3(ST[4].ox - 1, 1.4, -2.85), couch: () => F.items[0].position, armchair: () => F.items[1].position, lamp: () => F.items[3].position, roomA: () => new THREE.Vector3(ST[4].ox - 1.5, .2, -1.6), roomB: () => new THREE.Vector3(ST[4].ox + .5, .2, -1.2), roomC: () => new THREE.Vector3(ST[4].ox + 1.6, .2, -1.8) };
  let v; const mm = /^(panel|bay)(\d)$/.exec(name); if (mm) v = mm[1] === 'panel' ? B.panels[+mm[2]].position.clone() : new THREE.Vector3(B.bays[+mm[2]].x, 1.45, -.5); else v = m[name]().clone(); v.project(camera); return [(v.x + 1) / 2 * innerWidth, (1 - v.y) / 2 * innerHeight]; };
/* ---------- start ---------- */
loadAll().then(() => {
  buildPlan(); buildQuotes(); buildDemo(); buildBuild(); buildFinish();
  resize(); stationCam(1, camPos, camTgt); camera.position.copy(camPos).add(new THREE.Vector3(0, 6, 6)); sun.position.set(ST[1].ox + 6, 14, 8); sun.target.position.set(ST[1].ox, 0, 0);
  renderAll(); showHint();
  $('#load').style.opacity = 0; setTimeout(() => $('#load').remove(), 600);
  frame();
}).catch(err => { $('#load').textContent = 'Could not set up: ' + err.message; console.error(err); });
}
