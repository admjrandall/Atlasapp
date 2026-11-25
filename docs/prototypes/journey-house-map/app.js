import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { MODELS } from './models.js';

{
const $ = s => document.querySelector(s);
const esc = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = n => '$' + Math.round(n).toLocaleString('en-US');
const clamp = (x, a = 0, b = 1) => Math.min(b, Math.max(a, x));
const ease = t => t < .5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
const buzz = p => { try { navigator.vibrate && navigator.vibrate(p); } catch (e) {} };

/* =================== data (example workspace + marked samples) =================== */
const ROOMS = {
  living: { name: 'Living room', c: [-1.3, 0] },
  hall: { name: 'Hallway', c: [1.0, 0] },
  kitchen: { name: 'Kitchen', c: [3.6, -.1] }
};
const S = {
  room: null, tab: 'needs', xray: false, tall: false,
  needs: [
    { id: 'quotes', room: 'kitchen', kind: 'compare', k: 'DUE TODAY', title: 'Compare contractor quotes', p: 'Make sure both include demo haul-away.' },
    { id: 'tile', room: 'kitchen', kind: 'choose', k: 'PICK BY DEC 5', title: 'Backsplash tile', p: 'Tile needs about 3 weeks to arrive, so this sets the install date (sample).',
      opts: [{ n: 'Matte white zellige', s: '$14 / sq ft · sample', c: 'linear-gradient(135deg,#f3eee6,#e2d9cb)', vote: 'You like this' }, { n: 'Sage subway', s: '$9 / sq ft · sample', c: 'linear-gradient(135deg,#b9c5a9,#94a585)', vote: 'Your partner likes this' }] },
    { id: 'sky', room: 'hall', kind: 'choose', k: 'BEFORE FRAMING', title: 'Skylight size', p: 'Hallway is the darkest part of the house. You measured it 3 days ago.',
      opts: [{ n: '2 × 4 ft', s: '+$1,150 change order · sample', c: 'linear-gradient(#cfe6ff,#9cc3e8)' }, { n: '2 × 2 ft', s: '+$780 change order · sample', c: 'linear-gradient(#e2efff,#b9d4ee)' }] },
    { id: 'insurance', room: null, kind: 'task', k: 'DUE TODAY', title: 'Call insurance about roof claim', p: 'Before demolition starts Dec 15.' }
  ],
  waiting: [{ who: 'Jamie Carter', what: 'Updated estimate', when: 'Friday' }],
  agreed: [
    { room: 'living', ok: true, t: 'Keep the original oak floors', s: 'Your decision · Plan & design' },
    { room: 'kitchen', ok: true, t: 'Wall between kitchen and living room is not load-bearing', s: 'Jamie · site visit notes' },
    { room: 'kitchen', ok: true, t: 'Island moves 40 cm toward the window', s: 'Priya · Floor plan v3 (PDF)' },
    { room: 'kitchen', ok: true, t: 'Cabinets ship in three weeks', s: 'Jamie · your voice note today' },
    { room: null, ok: false, id: 'haul', t: 'Demolition haul-away is included', s: 'Only said out loud — nothing in writing yet' },
    { room: 'kitchen', ok: false, id: 'panel', t: 'Marco upgrades the electrical panel', s: 'Mentioned at the site visit · no price yet' }
  ],
  photos: [
    { id: 'p1', room: 'kitchen', t: 'Kitchen north wall · before', d: 'Nov 20 · site visit (sample photo)', pos: [2.3, 1.1, -2.2] },
    { id: 'p2', room: 'hall', t: 'Hallway ceiling · measured for skylight', d: '3 days ago (sample photo)', pos: [1.0, 2.35, -.4] }
  ],
  picked: { tile: null, sky: null, quotes: null },
  budget: 60000
};
const MONEY = () => {
  const contract = S.picked.quotes === 'ridge' ? 55400 : 48200 + 6800;
  const change = S.picked.sky === 0 ? 1150 : S.picked.sky === 1 ? 780 : 0;
  return { contract, change, paid: 0, left: S.budget - contract - change };
};

/* =================== router =================== */
function route() { const h = location.hash === '#journeys' ? 'ten' : 'reno'; $('#reno').classList.toggle('on', h === 'reno'); $('#ten').classList.toggle('on', h === 'ten'); $('#t1').classList.toggle('on', h === 'reno'); $('#t2').classList.toggle('on', h === 'ten'); if (h === 'reno') resize(); }
addEventListener('hashchange', route);

/* =================== 3D dollhouse =================== */
const canvas = $('#c');
const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
renderer.setPixelRatio(Math.min(devicePixelRatio, 2)); renderer.shadowMap.enabled = true; renderer.shadowMap.type = THREE.PCFSoftShadowMap; renderer.toneMapping = THREE.ACESFilmicToneMapping;
const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(40, 1, .1, 200);
const controls = new OrbitControls(camera, canvas);
Object.assign(controls, { enableDamping: true, dampingFactor: .08, enablePan: false, minDistance: 9, maxDistance: 30, minPolarAngle: .3, maxPolarAngle: 1.15, rotateSpeed: .6 });
controls.target.set(1.2, 0, 0);
scene.add(new THREE.HemisphereLight(0xfff6ea, 0x9c8f7c, 1.5));
const sun = new THREE.DirectionalLight(0xfff1dc, 2.3); sun.position.set(6, 14, 8); sun.castShadow = true; sun.shadow.mapSize.set(2048, 2048); Object.assign(sun.shadow.camera, { left: -10, right: 10, top: 10, bottom: -10 }); sun.shadow.bias = -.0005; sun.shadow.normalBias = .03; scene.add(sun);
const M = (c, o = {}) => new THREE.MeshStandardMaterial({ color: c, roughness: .85, flatShading: true, ...o });
function mesh(g, m, x = 0, y = 0, z = 0, p) { const o = new THREE.Mesh(g, m); o.position.set(x, y, z); o.castShadow = o.receiveShadow = true; (p || scene).add(o); return o; }
function box(w, h, d, m, x, y, z, p) { return mesh(new THREE.BoxGeometry(w, h, d), m, x, y, z, p); }

// ground
mesh(new THREE.CylinderGeometry(11, 11, .5, 48), M(0xb7cf95), 1, -.25, 0).receiveShadow = true;
// floors per room (tappable)
const floors = {}; const outline = {};
function floor(id, x0, x1, z0, z1, color) { const m = box(x1 - x0, .12, z1 - z0, M(color), (x0 + x1) / 2, .06, (z0 + z1) / 2); m.userData.room = id; floors[id] = floors[id] || []; floors[id].push(m);
  const o = new THREE.LineSegments(new THREE.EdgesGeometry(new THREE.BoxGeometry(x1 - x0 - .06, .02, z1 - z0 - .06)), new THREE.LineBasicMaterial({ color: 0xd9822b, transparent: true, opacity: 0 })); o.position.set((x0 + x1) / 2, .14, (z0 + z1) / 2); scene.add(o); outline[id] = outline[id] || []; outline[id].push(o); }
floor('living', -3, .4, -2.25, 2.25, 0xc9955c); // original oak floors
floor('hall', .4, 1.6, -2.25, 2.25, 0xd8cdbb);
floor('kitchen', 1.6, 3, -2.25, 2.25, 0xe8e2d8);
// planned extension (ghost until built)
const planned = box(3, .1, 3.8, new THREE.MeshBasicMaterial({ color: 0x86b4ff, transparent: true, opacity: .25, depthWrite: false }), 4.5, .06, -.1); planned.userData.room = 'kitchen'; floors.kitchen.push(planned);
const plannedEdge = new THREE.LineSegments(new THREE.EdgesGeometry(new THREE.BoxGeometry(3, 1.3, 3.8)), new THREE.LineDashedMaterial({ color: 0x5b8ddc, dashSize: .2, gapSize: .14 })); plannedEdge.position.set(4.5, .65, -.1); plannedEdge.computeLineDistances(); scene.add(plannedEdge);
// walls (cutaway height)
const WH = 1.3, T = .16; const wallMat = M(0xf1ebe0, { transparent: true }); const walls = [];
function wall(w, d, x, z) { const m = box(w, WH, d, wallMat, x, WH / 2 + .12, z); walls.push(m); return m; }
wall(6.2, T, 0, -2.25); wall(6.2, T, 0, 2.25); wall(T, 4.5, -3.05, 0); wall(T, 4.5, 3.05, 0);
wall(T, 4.5, .4, 0); wall(T, 1.6, 1.6, -1.45); wall(T, 1.6, 1.6, 1.45); // interior: hall walls
// the kitchen-living wall that comes down is the hall wall at x=1.6 (opening in middle)
// windows (glass strips on wall tops)
const glass = M(0x9cc3e8, { roughness: .2 }); box(1.1, .5, .18, glass, -2, .95, 2.25); box(1.4, .5, .18, glass, 2.1, .95, 2.25); box(1.2, .5, .18, glass, -1, .95, -2.25);
box(.9, 1.15, .2, M(0x7b5136), -.3, .7, 2.26); // door
// studs + wiring hidden in walls (x-ray)
const inside = new THREE.Group(); scene.add(inside); inside.visible = false;
const studM = new THREE.MeshBasicMaterial({ color: 0xd9b77e }); const wireR = new THREE.MeshBasicMaterial({ color: 0xd94a3d }); const wireB = new THREE.MeshBasicMaterial({ color: 0x2f6fd9 }); const pipe = new THREE.MeshBasicMaterial({ color: 0x9aa3ad });
for (let x = -2.9; x <= 2.9; x += .42) { box(.05, WH, .07, studM, x, WH / 2 + .12, -2.25, inside).castShadow = false; }
box(4.2, .03, .04, wireR, .8, .9, -2.25, inside); box(.03, .7, .04, wireR, 2.4, .55, -2.25, inside); box(2.6, .03, .04, wireB, 1.6, 1.15, -2.25, inside); box(.06, 1.2, .06, pipe, 2.7, .7, -2.24, inside);
// furniture
const GLB = {}; const loader = new GLTFLoader();
function b64(s) { const bin = atob(s); const u = new Uint8Array(bin.length); for (let i = 0; i < bin.length; i++) u[i] = bin.charCodeAt(i); return u.buffer; }
async function loadAll() { await Promise.all(Object.keys(MODELS).map(k => new Promise((res, rej) => loader.parse(b64(MODELS[k]), './', g => { GLB[k] = g; res(); }, rej)))); }
function inst(name, size, by = 'h') { const o = GLB[name].scene.clone(true); o.updateMatrixWorld(true); const b = new THREE.Box3().setFromObject(o); const d = b.getSize(new THREE.Vector3()); o.scale.multiplyScalar(size / (by === 'h' ? d.y : d.x)); o.updateMatrixWorld(true); const b2 = new THREE.Box3().setFromObject(o); const c = b2.getCenter(new THREE.Vector3()); o.position.set(-c.x, -b2.min.y, -c.z); o.traverse(m => { if (m.isMesh) m.castShadow = m.receiveShadow = true; }); const w = new THREE.Group(); w.add(o); scene.add(w); return w; }
function put(n, s, x, z, ry = 0, by = 'h') { const o = inst(n, s, by); o.position.set(x, .12, z); o.rotation.y = ry; return o; }
function furnish() {
  put('rug_rectangle_stripes_A', 2.1, -1.4, .2, 0, 'w'); put('couch_pillows', .8, -1.4, -1.55); put('table_low', .34, -1.4, .1); put('armchair', .78, -2.4, 1.2, 2.3); put('lamp_standing', 1.4, -2.6, -1.7); put('shelf_B_large_decorated', .5, .0, 1.9, Math.PI);
  put('table_medium', .75, 2.3, .2, Math.PI / 2, 'h'); put('chair_A_wood', .85, 2.35, 1.25, Math.PI); put('cabinet_medium_decorated', 1.0, 2.3, -1.85); put('cactus_medium_A', .45, 1.0, 1.7);
  [[-6, -5], [-7.5, 2], [6.5, 5.5], [8, -4], [-3, 6.5]].forEach(([x, z], i) => { const t = inst(i % 2 ? 'tree_fat' : 'tree_oak', 2.4); t.position.set(x, 0, z); });
  [[-2.5, 3], [2.4, 3], [4.8, 2.6]].forEach(([x, z]) => { const b = inst('bush', .5); b.position.set(x, 0, z); });
}

/* ---------- tags + pins (HTML over 3D) ---------- */
const tagsEl = $('#tags'); const tags = {}; const pins = [];
Object.entries(ROOMS).forEach(([id, r]) => { const d = document.createElement('button'); d.className = 'tag'; d.innerHTML = `<span class="n">0</span><span>${esc(r.name)}</span>`; d.addEventListener('click', () => focusRoom(id)); tagsEl.appendChild(d); tags[id] = d; });
function makePins() { pins.forEach(p => p.el.remove()); pins.length = 0; S.photos.forEach(ph => { const d = document.createElement('button'); d.className = 'pin'; d.textContent = '📷'; d.setAttribute('aria-label', ph.t); d.addEventListener('click', () => openPhoto(ph)); tagsEl.appendChild(d); pins.push({ el: d, ph }); }); }
const pv = new THREE.Vector3();
function place(el, x, y, z) { pv.set(x, y, z).project(camera); if (pv.z > 1) { el.style.display = 'none'; return; } el.style.display = ''; const r = canvas.getBoundingClientRect(); el.style.left = ((pv.x + 1) / 2 * r.width) + 'px'; el.style.top = ((1 - pv.y) / 2 * r.height) + 'px'; }
function placeAll() {
  Object.entries(tags).forEach(([id, el]) => { const c = ROOMS[id].c; place(el, c[0], S.xray ? 2.2 : 1.8, c[1]); el.style.display = S.xray ? 'none' : el.style.display; });
  pins.forEach(p => { if (!S.xray) { p.el.style.display = 'none'; return; } place(p.el, ...p.ph.pos); });
}

/* ---------- camera ---------- */
let fly = null;
function flyTo(pos, tgt, dur = 1) { fly = { p0: camera.position.clone(), t0: controls.target.clone(), p1: new THREE.Vector3(...pos), t1: new THREE.Vector3(...tgt), s: performance.now(), d: dur * 1000 }; }
const HOME = { pos: [10.5, 16, 17.5], tgt: [1.5, 0, .2] };
const ROOMCAM = { living: [[-4, 10, 9], [-1.3, 0, 0]], hall: [[3, 10, 9], [1, 0, 0]], kitchen: [[8, 10, 9], [3.4, 0, -.1]] };
function resize() { const r = $('#map').getBoundingClientRect(); if (!r.width) return; renderer.setSize(r.width, r.height, false); camera.aspect = r.width / r.height; camera.fov = r.width < r.height ? 46 : 34; const sh = $('#sheet').offsetHeight, top = 96; camera.setViewOffset(r.width, r.height, 0, (sh - top) / 2, r.width, r.height); camera.updateProjectionMatrix(); }
addEventListener('resize', resize); new ResizeObserver(resize).observe($('#sheet'));

/* ---------- tapping rooms ---------- */
const ray = new THREE.Raycaster(), ndc = new THREE.Vector2(); let down = null;
canvas.addEventListener('pointerdown', e => { down = [e.clientX, e.clientY]; fly = null; });
canvas.addEventListener('pointerup', e => { if (!down || Math.hypot(e.clientX - down[0], e.clientY - down[1]) > 8) return; const r = canvas.getBoundingClientRect(); ndc.set((e.clientX - r.left) / r.width * 2 - 1, -((e.clientY - r.top) / r.height) * 2 + 1); ray.setFromCamera(ndc, camera); const h = ray.intersectObjects(Object.values(floors).flat()); if (h.length) focusRoom(h[0].object.userData.room); else if (S.room) focusRoom(null); });
function focusRoom(id) { S.room = S.room === id ? null : id; buzz(8); if (S.room) flyTo(...ROOMCAM[S.room]); else flyTo(HOME.pos, HOME.tgt); render(); }

$('#xray').addEventListener('click', () => { S.xray = !S.xray; $('#xray').classList.toggle('on', S.xray); $('#xray').setAttribute('aria-pressed', S.xray); inside.visible = S.xray; wallMat.opacity = S.xray ? .22 : 1; wallMat.depthWrite = !S.xray; if (S.xray) { S.tab = 'walls'; flyTo([6, 7, 11], [1.4, .8, -1.2]); } render(); buzz(10); });

/* =================== sheet =================== */
const inRoom = x => !S.room || x.room === S.room || (x.room === null && false);
function counts() { const n = S.needs.filter(inRoom).length; const ag = S.agreed.filter(a => !S.room || a.room === S.room || a.room === null); return { needs: n, ok: ag.filter(a => a.ok).length, q: ag.filter(a => !a.ok).length, photos: S.photos.filter(inRoom).length }; }
function render() {
  const c = counts(), m = MONEY();
  $('#scope').textContent = S.room ? ROOMS[S.room].name : 'Whole house';
  $('#scopeSub').textContent = S.room ? `${c.needs} need you · ${c.q} not in writing · tap the room again for the whole house` : 'Tap a room to focus · tap ＋ to add anything';
  $('#four').innerHTML = [['needs', c.needs, 'Needs you', 'needs'], ['money', money(m.left).replace('$', '$'), 'Left', ''], ['agreed', `${c.ok}✓ ${c.q}?`, 'Agreed', ''], ['walls', c.photos, 'Walls', '']].map(([k, v, l, cls]) => `<button class="${cls} ${S.tab === k ? 'on' : ''}" data-k="${k}"><b>${esc(v)}</b><span>${l}</span></button>`).join('');
  $('#four').querySelectorAll('button').forEach(b => b.addEventListener('click', () => { S.tab = b.dataset.k; if (S.tab === 'walls' && !S.xray) $('#xray').click(); else if (S.tab !== 'walls' && S.xray) { $('#xray').click(); S.tab = b.dataset.k; } render(); }));
  Object.entries(tags).forEach(([id, el]) => { const n = S.needs.filter(x => x.room === id).length; const nEl = el.querySelector('.n'); nEl.textContent = n || '✓'; nEl.classList.toggle('z', !n); el.classList.toggle('sel', S.room === id); });
  Object.entries(outline).forEach(([id, os]) => os.forEach(o => o.material.opacity = S.needs.some(x => x.room === id) ? 1 : 0));
  const done = 1; $('#trail').innerHTML = [0, 1, 2, 3, 4].map(i => `${i ? `<b class="${i <= done ? 'done' : ''}"></b>` : ''}<i class="${i < done ? 'done' : i === done ? 'here' : ''}"></i>`).join('');
  const B = $('#sbody'); B.innerHTML = { needs: needsHTML, money: moneyHTML, agreed: agreedHTML, walls: wallsHTML }[S.tab]();
  bind(B);
}
function needsHTML() {
  const list = S.needs.filter(inRoom);
  let h = list.map(n => {
    if (n.kind === 'choose') return `<div class="card"><div class="k">${esc(n.k)} · ${esc(ROOMS[n.room]?.name || 'House')}</div><h3>${esc(n.title)}</h3><p>${esc(n.p)}</p><div class="opts">${n.opts.map((o, i) => `<button class="opt" data-choose="${n.id}" data-i="${i}"><div class="sw" style="background:${o.c}"></div><b>${esc(o.n)}</b><small>${esc(o.s)}</small>${o.vote ? `<div class="vote">${esc(o.vote)}</div>` : ''}</button>`).join('')}</div><div class="mini">Tap one to choose. ${n.id === 'tile' ? 'Votes come from the shared journey.' : ''}</div></div>`;
    if (n.kind === 'compare') return `<div class="card"><div class="k">${esc(n.k)} · Kitchen</div><h3>${esc(n.title)}</h3><p>Like for like — Carter leaves out electrical, so Marco’s number is added.</p>
      <table class="cmp"><tr><th></th><th>Carter + Marco</th><th>Ridge</th></tr><tr><td>Estimate</td><td>$48,200</td><td>$55,400</td></tr><tr><td>Electrical</td><td>$6,800</td><td>incl.</td></tr><tr><td>Haul-away</td><td>not in writing</td><td>incl.</td></tr><tr><th>Total</th><th>$55,000</th><th>$55,400</th></tr></table>
      <div class="mini">Ridge and Marco numbers are samples.</div><div class="row"><button class="btn" data-pickq="carter">Go with Carter</button><button class="btn g" data-pickq="ridge">Go with Ridge</button></div></div>`;
    return `<div class="card"><div class="k">${esc(n.k)}</div><h3>${esc(n.title)}</h3><p>${esc(n.p)}</p><div class="row"><button class="btn g" data-done="${n.id}">Done</button></div></div>`;
  }).join('');
  if (!list.length) h = `<div class="card"><div class="k g">ALL CLEAR</div><h3>Nothing needs you here</h3><p>New items show up here when they need a decision or a date.</p></div>`;
  if (!S.room) h += `<div class="card"><div class="k v">WAITING ON OTHERS</div>${S.waiting.map(w => `<h3>${esc(w.what)}</h3><p>${esc(w.who)} · expected ${esc(w.when)}. Atlas will nudge you if it doesn’t arrive.</p>`).join('')}</div>`;
  return h;
}
function moneyHTML() {
  const m = MONEY(); const tot = S.budget; const seg = (v, c) => `<div style="width:${v / tot * 100}%;background:${c}"></div>`;
  return `<div class="card"><div class="k v">BUDGET ${money(tot)}</div><h3>${money(m.left)} left for surprises</h3><div class="mbar">${seg(m.paid, '#2f9a66')}${seg(m.contract, '#6b4bd1')}${seg(m.change, '#d9822b')}${seg(Math.max(0, m.left), '#e9e4f3')}</div>
  <div class="legend"><span><i class="dot" style="background:#2f9a66"></i>Paid</span><span>${money(m.paid)}</span><span><i class="dot" style="background:#6b4bd1"></i>${S.picked.quotes ? 'Contract' : 'Best quote, like for like'}</span><span>${money(m.contract)}</span><span><i class="dot" style="background:#d9822b"></i>Change orders</span><span>${money(m.change)}</span><span><i class="dot" style="background:#e9e4f3"></i>Cushion</span><span>${money(m.left)}</span></div>
  <p class="mini">Every change order you approve lands here with its date and who asked for it. Over a third of renovators go over budget — this bar is why you won’t be surprised.</p></div>
  <div class="card"><div class="k">WATCH</div><h3>Electrical isn’t in Jamie’s estimate</h3><p>$6,800 from Marco is counted above (sample). If he quotes higher, the cushion shrinks.</p></div>`;
}
function agreedHTML() {
  const list = S.agreed.filter(a => !S.room || a.room === S.room || a.room === null);
  return `<div class="card">${list.map(a => `<div class="agree"><span class="ic ${a.ok ? 'ok' : 'q'}">${a.ok ? '✓' : '?'}</span><div style="flex:1"><b>${esc(a.t)}</b><small>${esc(a.s)}</small>${!a.ok && a.id ? `<div class="row"><button class="btn g" data-official="${a.id}">Make it official</button></div>` : ''}</div></div>`).join('')}</div>
  <p class="mini">✓ = in writing, with where it came from. ? = said but not written down — the kind of thing that turns into “but you said…”.</p>`;
}
function wallsHTML() {
  const list = S.photos.filter(inRoom);
  return `<div class="card"><div class="k v">INSIDE THE WALLS</div><h3>${list.length} photo${list.length === 1 ? "" : "s"} pinned where taken</h3><p>Tap a 📷 on the house. Years from now, tap a wall to see what’s behind it.</p></div>
  <div class="card"><div class="k">COMING · ABOUT JAN 12 (SAMPLE)</div><h3>Before drywall: walk the walls</h3><p>Atlas will remind you the day before drywall goes up in the kitchen, with a checklist: each wall wide, then close on outlets, pipes and wiring.</p></div>`;
}
function bind(B) {
  B.querySelectorAll('[data-choose]').forEach(b => b.addEventListener('click', () => { const id = b.dataset.choose, i = +b.dataset.i; const n = S.needs.find(x => x.id === id); S.picked[id] = i; const prev = { ...n }; S.needs = S.needs.filter(x => x.id !== id); S.agreed.unshift({ room: n.room, ok: true, t: `${n.title}: ${n.opts[i].n}`, s: 'Your decision · just now' }); buzz([12, 20, 30]); toast(`${n.title}: ${n.opts[i].n}${id === 'sky' ? ' · change order added to money' : ''}`, () => { S.needs.splice(0, 0, prev); S.agreed.shift(); S.picked[id] = null; render(); }); render(); }));
  B.querySelectorAll('[data-pickq]').forEach(b => b.addEventListener('click', () => { const q = b.dataset.pickq; S.picked.quotes = q; const prev = S.needs.find(x => x.id === 'quotes'); S.needs = S.needs.filter(x => x.id !== 'quotes'); S.agreed.unshift({ room: 'kitchen', ok: true, t: q === 'carter' ? 'Carter Build + Marco for electrical' : 'Ridge Builders', s: 'Your decision · compared like for like' }); buzz([15, 25, 40]); toast('Quotes compared · contract amount updated in money', () => { S.needs.unshift(prev); S.agreed.shift(); S.picked.quotes = null; render(); }); render(); }));
  B.querySelectorAll('[data-done]').forEach(b => b.addEventListener('click', () => { const id = b.dataset.done; const prev = S.needs.find(x => x.id === id); S.needs = S.needs.filter(x => x.id !== id); buzz(12); toast(`${prev.title} · done`, () => { S.needs.push(prev); render(); }); render(); }));
  B.querySelectorAll('[data-official]').forEach(b => b.addEventListener('click', () => official(b.dataset.official)));
}
function official(id) {
  const a = S.agreed.find(x => x.id === id);
  const text = id === 'haul' ? 'Hi Jamie — confirming what we talked about: haul-away of all demolition debris is included in your $48,200 estimate (EST-2026-0118). Could you reply to confirm? Thanks!' : 'Hi Marco — Jamie mentioned you’d upgrade the electrical panel as part of the kitchen job. Could you send a price in writing so we can add it to the plan? Thanks!';
  modal(`<h3>Make it official</h3><p class="mini" style="margin:0">Atlas drafted this from your notes. You send it — Atlas never sends for you.</p><div class="draft" id="dr">${esc(text)}</div><div class="row"><button class="btn" id="cp">Copy &amp; mark as asked</button><button class="btn g" id="cx">Cancel</button></div>`, () => {
    $('#cp').addEventListener('click', () => { try { navigator.clipboard.writeText(text); } catch (e) {} a.s = 'Asked in writing just now · waiting for the reply'; closeModal(); toast('Copied · paste it into your text to ' + (id === 'haul' ? 'Jamie' : 'Marco')); S.waiting.push({ who: id === 'haul' ? 'Jamie Carter' : 'Marco Ruiz', what: id === 'haul' ? 'Confirm haul-away in writing' : 'Panel upgrade price', when: 'this week' }); render(); });
    $('#cx').addEventListener('click', closeModal);
  });
}
function openPhoto(ph) {
  const c = document.createElement('canvas'); c.width = 640; c.height = 400; const g = c.getContext('2d');
  g.fillStyle = '#e9dfcf'; g.fillRect(0, 0, 640, 400); g.fillStyle = '#d9b77e'; for (let x = 20; x < 640; x += 90) g.fillRect(x, 0, 22, 400); g.fillRect(0, 0, 640, 18); g.fillRect(0, 382, 640, 18);
  if (ph.room === 'kitchen') { g.strokeStyle = '#d94a3d'; g.lineWidth = 6; g.beginPath(); g.moveTo(0, 230); g.lineTo(480, 230); g.lineTo(480, 320); g.stroke(); g.strokeStyle = '#2f6fd9'; g.beginPath(); g.moveTo(80, 150); g.lineTo(640, 150); g.stroke(); g.fillStyle = '#9aa3ad'; g.fillRect(560, 120, 18, 280); g.fillStyle = '#fff'; g.fillRect(455, 300, 50, 70); }
  else { g.fillStyle = '#cfe6ff'; g.fillRect(200, 110, 240, 180); g.strokeStyle = '#1d1830'; g.setLineDash([10, 8]); g.lineWidth = 3; g.strokeRect(200, 110, 240, 180); g.setLineDash([]); g.fillStyle = '#1d1830'; g.font = 'bold 26px Roboto,sans-serif'; g.fillText('48″ × 24″ between rafters', 170, 330); }
  modal(`<h3>${esc(ph.t)}</h3><div class="mini" style="margin:0">${esc(ph.d)} · pinned to the ${esc(ROOMS[ph.room].name.toLowerCase())}</div><img class="photo" alt="${esc(ph.t)}" src="${c.toDataURL()}"><div class="row"><button class="btn g" id="cx">Close</button></div>`, () => $('#cx').addEventListener('click', closeModal));
}

/* ---------- capture (no typing) ---------- */
$('#fab').addEventListener('click', () => {
  modal(`<h3>Add anything</h3><p class="mini" style="margin:0">Atlas works out which room and stage it belongs to. You can move it after.</p>
  <div class="cap"><button id="cPhoto"><span>📷</span>Photo</button><button id="cVoice"><span>🎙</span>Voice</button><button id="cShare"><span>💬</span>Shared text</button></div><p class="mini">Demo: each button plays an example.</p>`, () => {
    $('#cShare').addEventListener('click', () => { closeModal(); const item = { room: 'kitchen', ok: true, t: 'Cabinet delivery moved to Dec 18', s: 'Jamie · text you shared just now' }; S.agreed.unshift(item); buzz(12); toast('Jamie’s text → Kitchen · Agreed. Delivery is now Dec 18.', () => { S.agreed.shift(); render(); }); S.tab = 'agreed'; render(); });
    $('#cVoice').addEventListener('click', () => { closeModal(); const n = { id: 'marco' + Date.now(), room: 'kitchen', kind: 'task', k: 'FROM YOUR VOICE NOTE', title: 'Ask Marco for the panel upgrade price', p: '“Remind me to ask Marco what the panel upgrade costs.”' }; S.needs.push(n); buzz(12); toast('Voice note → a to-do in the Kitchen', () => { S.needs = S.needs.filter(x => x !== n); render(); }); S.tab = 'needs'; render(); });
    $('#cPhoto').addEventListener('click', () => { closeModal(); const ph = { id: 'p' + Date.now(), room: 'living', t: 'Living room east wall · before', d: 'Just now (sample photo)', pos: [-3.0, 1.0, .4] }; S.photos.push(ph); makePins(); buzz(12); toast('Photo pinned to the living room wall', () => { S.photos = S.photos.filter(x => x !== ph); makePins(); render(); }); if (!S.xray) $('#xray').click(); else render(); });
  });
});

/* ---------- modal + toast ---------- */
function modal(html, bindFn) { $('#mcard').innerHTML = html; $('#modal').classList.add('show'); bindFn && bindFn(); }
function closeModal() { $('#modal').classList.remove('show'); }
$('#modal').addEventListener('click', e => { if (e.target.id === 'modal') closeModal(); });
let tt; function toast(msg, undo) { const t = $('#toast'); t.innerHTML = `<span>${esc(msg)}</span>${undo ? '<button id="un">Undo</button>' : ''}`; t.classList.add('show'); if (undo) $('#un').addEventListener('click', () => { undo(); t.classList.remove('show'); }); clearTimeout(tt); tt = setTimeout(() => t.classList.remove('show'), 4200); }
$('#grab').addEventListener('click', () => { S.tall = !S.tall; $('#sheet').classList.toggle('tall', S.tall); });

/* ---------- loop ---------- */
function frame() {
  if (fly) { const k = ease(clamp((performance.now() - fly.s) / fly.d)); camera.position.lerpVectors(fly.p0, fly.p1, k); controls.target.lerpVectors(fly.t0, fly.t1, k); if (k >= 1) fly = null; }
  controls.update(); const t = performance.now() / 1000;
  Object.values(outline).flat().forEach(o => { if (o.material.opacity > 0) o.material.opacity = .55 + Math.sin(t * 3) * .45; });
  placeAll(); renderer.render(scene, camera); requestAnimationFrame(frame);
}

/* =================== the ten journeys =================== */
const ICONS = {
  trail: '<path d="M8 44 C 20 40, 14 28, 26 26 S 40 14, 46 8" fill="none" stroke="#6b4bd1" stroke-width="3" stroke-dasharray="2 5" stroke-linecap="round"/><circle cx="8" cy="44" r="4" fill="#2f9a66"/><circle cx="26" cy="26" r="5" fill="#6b4bd1"/><path d="M44 4v10l6-3z" fill="#d9822b"/>',
  tree: '<path d="M26 48V24" stroke="#7b5136" stroke-width="4"/><circle cx="26" cy="18" r="12" fill="#9fc48b"/><circle cx="15" cy="26" r="7" fill="#b5d3a3"/><circle cx="37" cy="26" r="7" fill="#b5d3a3"/><circle cx="20" cy="16" r="2.5" fill="#d9822b"/><circle cx="32" cy="22" r="2.5" fill="#d9822b"/>',
  remember: '<path d="M10 44h32" stroke="#c9b8a0" stroke-width="3"/><path d="M26 44V22" stroke="#7b9e6c" stroke-width="3"/><path d="M26 24c-8-2-10-10-6-14 5 1 7 7 6 14zM26 28c7-4 13-2 14 3-5 3-11 1-14-3z" fill="#a7c79a"/><circle cx="26" cy="14" r="4" fill="#e8a6b4"/>',
  crossroads: '<path d="M6 10 C 18 14, 22 24, 26 30 M46 10 C 34 14, 30 24, 26 30 M26 4 V 30" stroke="#b9b0d0" stroke-width="3" fill="none"/><path d="M26 30V48" stroke="#6b4bd1" stroke-width="4"/><rect x="18" y="28" width="16" height="6" rx="2" fill="#6b4bd1"/>',
  river: '<path d="M4 14 C 16 8, 22 22, 34 16 S 48 12, 50 18" stroke="#9cc3e8" stroke-width="4" fill="none"/><path d="M2 28 C 16 22, 22 38, 36 30 S 50 26, 52 32" stroke="#5b8ddc" stroke-width="6" fill="none"/><path d="M4 44 C 18 38, 24 50, 38 44" stroke="#2f5fb0" stroke-width="8" fill="none"/>',
  orbit: '<circle cx="26" cy="26" r="6" fill="#6b4bd1"/><circle cx="26" cy="26" r="13" fill="none" stroke="#c9bdf0" stroke-width="2"/><circle cx="26" cy="26" r="21" fill="none" stroke="#e1d9f6" stroke-width="2"/><circle cx="39" cy="26" r="3.5" fill="#d9822b"/><circle cx="12" cy="13" r="3" fill="#b9b0d0"/>',
  shelf: '<path d="M6 20h40M6 36h40" stroke="#b98a5a" stroke-width="3"/><rect x="10" y="6" width="6" height="14" fill="#6b4bd1"/><rect x="18" y="9" width="5" height="11" fill="#d9822b"/><rect x="25" y="7" width="7" height="13" fill="#9fae8f"/><rect x="12" y="25" width="10" height="11" rx="2" fill="#e8a6b4"/><rect x="26" y="23" width="6" height="13" fill="#5b8ddc"/>',
  voyage: '<path d="M4 40 C 14 36, 24 44, 34 38 S 48 36, 50 38" stroke="#9cc3e8" stroke-width="3" fill="none"/><ellipse cx="12" cy="30" rx="7" ry="3" fill="#cdb98f"/><ellipse cx="38" cy="22" rx="8" ry="3.5" fill="#cdb98f"/><path d="M12 30 C 20 18, 30 30, 38 22" stroke="#6b4bd1" stroke-width="2" stroke-dasharray="3 3" fill="none"/>',
  climb: '<path d="M4 46 L 22 16 L 30 28 L 38 14 L 50 46 Z" fill="#c9c2d9"/><path d="M38 14 l -4 7 h 8 z" fill="#fff"/><path d="M8 44 L 18 34 L 26 36 L 34 22" stroke="#6b4bd1" stroke-width="2.5" fill="none" stroke-dasharray="3 3"/><path d="M38 6v8l5-3z" fill="#d9822b"/>',
  countdown: '<circle cx="26" cy="26" r="20" fill="none" stroke="#efe9ff" stroke-width="6"/><path d="M26 6 A 20 20 0 1 1 9 36" fill="none" stroke="#6b4bd1" stroke-width="6" stroke-linecap="round"/><text x="26" y="31" text-anchor="middle" font-size="13" font-weight="700" fill="#1d1830" font-family="Roboto,sans-serif">12</text>'
};
const TEN = [
  { k: 'trail', n: 'Trail', f: 'Projects with an end · renovation, a move, a job search', job: 'Keep a big project on budget and on time, and remember what everyone agreed.', a: ['Which stop you’re at, what’s next and when', 'Decisions with deadlines, ranked by what they hold up', 'Money left, with change orders landing as they happen', 'What was promised, in writing, with where it came from'], cap: 'Photos, shared texts and emails, PDF quotes, voice notes', one: 'The decision queue, and “make it official” for anything only said out loud', last: 'A record of the house: x-ray walls, paint colours, warranties' },
  { k: 'tree', n: 'Living Tree', f: 'A person or life · grandchild, child, pet', job: 'Know someone well as they grow — and never lose the photo, the size or the idea.', a: ['Their age and season of life', 'Birthdays coming up, and gift ideas you saved', 'Nothing to lose here — just time', 'Sizes, favourites, allergies, doctors, school'], cap: 'Photos of them from your gallery (on device), messages that mention them', one: '“Ask the tree”: what size shoes is she in now? What did she love last year?', last: 'A year in their life, made into a book' },
  { k: 'remember', n: 'Remember', f: 'Someone who died', job: 'Keep the stories before they fade, and get through the hard dates together.', a: ['Gentle — no progress at all', 'An anniversary coming, with a quiet heads-up', 'Stories only you or family still hold', 'What’s been gathered: photos, words, recordings'], cap: 'The obituary (fetched only when asked), your photos and old messages, voice recordings of family stories', one: 'Ask family for one memory each; their answers plant into the room, garden or letter', last: 'A keepsake the family can open on any day' },
  { k: 'crossroads', n: 'Crossroads', f: 'A decision · a purchase, a job offer, a contractor', job: 'Choose well, compare like with like, and remember why you chose.', a: ['How many options are still in', 'Missing facts to decide (a price, a review, a spec)', 'Price and fit against what matters to you', 'What you ruled out, and why'], cap: 'Links, screenshots, quotes and reviews you share; the AI fetches only what you ask for', one: 'Side by side on your own criteria, with gaps evened out — like adding the missing electrical', last: 'The verdict and the reasons, for when you wonder later' },
  { k: 'river', n: 'River', f: 'Learning · a skill, an instrument, a language', job: 'Keep going through the plateau — the point where most people quit.', a: ['How deep you are, and recent practice', 'The next small thing to try', 'Nothing lost but time — no guilt streaks', 'What you’ve learned and where from'], cap: 'Quick recordings or videos of you practising, links and lessons you save', one: 'Then vs now: play your first recording beside this week’s, so a plateau doesn’t feel like failure', last: 'A record of how you got good' },
  { k: 'orbit', n: 'Orbit', f: 'Recurring life admin · taxes, car registration, physicals', job: 'Never pay a late fee for something you do every year.', a: ['Days until it’s due', 'What to gather, with last year’s files ready', 'The fee or fine if it slips', 'What you paid and filed last time'], cap: 'Renewal emails and letters (photo), receipts — dates and amounts read automatically', one: 'Last time vs this time, with a reminder weeks ahead and the documents already attached', last: 'Every year’s paperwork, one tap away' },
  { k: 'shelf', n: 'Shelf', f: 'Things to keep · recipes, books, a wishlist, gift ideas', job: 'Actually use what you save, instead of building a graveyard of saves.', a: ['What’s on the shelf, newest first', 'The right item at the right time — a gift idea before the birthday', 'Nothing urgent', 'What you tried, rated and kept'], cap: 'Share from any app; the recipe or article itself is kept, not just a link that can vanish', one: 'Resurfacing: Atlas brings an item back when it fits — and “make this” turns it into a plan', last: 'Your own cookbook, reading list, gift list' },
  { k: 'voyage', n: 'Voyage', f: 'A trip · before, during, after', job: 'Have every booking in one place, today’s plan in hand, and an album after.', a: ['Before, during or after — and which day', 'The next check-in, booking or document', 'Dates and what it all costs', 'Confirmations, saved offline'], cap: 'Booking emails you share, tickets, photos (placed on the right day and place)', one: 'The “today” card on the trip: where, when, the ticket — without opening five apps', last: 'An album by day and place, built from your photos' },
  { k: 'climb', n: 'Climb', f: 'A measured goal · savings, fitness, recovery', job: 'See if you’re on pace, and what to change to get there.', a: ['Altitude: where you are vs the target', 'Off pace? By how much, and since when', 'When you’ll arrive at the current pace', 'What helped on the way up'], cap: 'Quick numbers, screenshots from your bank or fitness app read on the phone', one: 'Drag the summit date and see the pace it needs — a real what-if', last: 'Proof of how you did it' },
  { k: 'countdown', n: 'Countdown', f: 'A fixed day · wedding, party, a service', job: 'Get to the day with the budget, the guests and the vendors handled.', a: ['Days left, and what each lane still needs', 'Decisions and deadlines by lane — guests, vendors, run of show', 'Budget and RSVPs', 'Booked vendors and their contracts'], cap: 'Vendor emails and contracts, RSVP replies, the venue’s PDF', one: 'Day-of mode: the run of show, who does what and when, shareable with helpers', last: 'An album of the day' }
];
function renderTen() {
  $('#tenwrap').innerHTML = `
  <h2>Every journey answers four questions at a glance</h2>
  <p class="lede">People don’t keep using apps that feel like a second job, or games about fake work. So each journey has one real job, fills itself from what you already have, and gives the same four answers — in its own shape.</p>
  <div class="answers"><div><b>Where am I?</b><span>The shape shows it</span></div><div><b>What needs me?</b><span>Decisions and dates, ranked</span></div><div><b>What’s at stake?</b><span>Money, time, or what could be lost</span></div><div><b>What’s settled?</b><span>Agreed, chosen, kept — with its source</span></div></div>
  <div class="rules"><h3>Rules every journey follows</h3><ol><li>No typing to keep it alive: photos, shares, voice and email fill it.</li><li>Every tap is real work: decide, confirm, send, or look something up.</li><li>Nothing is sent for you. Atlas drafts; you send.</li><li>It stays useful after it ends — a record, an album, an answer.</li><li>No streaks, badges or guilt.</li></ol></div>
  ${TEN.map(j => `<article class="j"><div class="jh"><svg viewBox="0 0 52 52" aria-hidden="true">${ICONS[j.k]}</svg><div><b>${esc(j.n)}</b><small>${esc(j.f)}</small></div></div>
    <p class="job"><strong>Real job:</strong> ${esc(j.job)}</p>
    <div class="g4"><div><i>WHERE AM I</i>${esc(j.a[0])}</div><div><i>WHAT NEEDS ME</i>${esc(j.a[1])}</div><div><i>WHAT’S AT STAKE</i>${esc(j.a[2])}</div><div><i>WHAT’S SETTLED</i>${esc(j.a[3])}</div></div>
    <dl class="jl"><dt>Fills itself from</dt><dd>${esc(j.cap)}</dd><dt>The one interaction</dt><dd>${esc(j.one)}</dd><dt>Afterwards</dt><dd>${esc(j.last)}</dd></dl></article>`).join('')}
  <div class="shared"><h3>Build one engine, ten shapes</h3><p>Most of this is shared. Build it once and each shape only adds its picture and its one interaction.</p><div class="chips"><span>Capture with no typing</span><span>Auto-file to place and stage</span><span>“Needs you” queue</span><span>Agreed, with sources</span><span>Money &amp; dates</span><span>Resurfacing at the right time</span><span>Share with family</span><span>Record afterwards</span></div></div>
  <p class="src">Research behind this: Houzz &amp; Home Study 2026 (budgets, overruns), Houzz homeowner survey (timelines, communication), Houzz couples survey (selections), change-order dispute guidance, before-drywall photo practice, app abandonment and gamification research, Zola First Look Report (wedding pain points), forgotten-renewal surveys, save-for-later research, learning-plateau research. Samples on the renovation tab are marked.</p>`;
}

/* =================== start =================== */
renderTen(); makePins(); render(); route();
camera.position.set(14, 20, 24); flyTo(HOME.pos, HOME.tgt, 1.6);
loadAll().then(() => { furnish(); }).catch(e => console.error(e));
frame();
}
