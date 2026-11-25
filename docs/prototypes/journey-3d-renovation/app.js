import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import * as SkeletonUtils from 'three/addons/utils/SkeletonUtils.js';
import { MODELS } from './models.js';

{
const $ = s => document.querySelector(s);
const clamp = (x, a = 0, b = 1) => Math.min(b, Math.max(a, x));
const lerp = (a, b, t) => a + (b - a) * t;
const seg = (a, x0, x1) => clamp((a - x0) / (x1 - x0));
const easeOut = t => 1 - Math.pow(1 - t, 3);
const ease = t => t < .5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
const back = t => { if (t <= 0) return 0; if (t >= 1) return 1; const c = 1.9; return 1 + (c + 1) * Math.pow(t - 1, 3) + c * Math.pow(t - 1, 2); };
const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
const buzz = p => { try { navigator.vibrate && navigator.vibrate(p); } catch (e) {} };

/* ---------- data (from the example workspace) ---------- */
const OVERVIEW = { cam: [17, 17, 23], tgt: [0.6, 0, 0.4] };
const STAGES = [
  { name: 'Plan & design', spot: [-5.4, 4.4], cam: [-14, 9.5, 14.5], tgt: [-4, 0.8, 2.5],
    line: 'Done Nov 3. Priya moved the island 40 cm toward the window.',
    items: [
      { id: 'plan', k: 'pdf', t: 'Floor plan v3', s: 'PDF · Priya Shah' },
      { id: 'link', k: 'link', t: '12 kitchen layouts that work', s: 'houseideas.example · 8 min' },
      { id: 'oak', k: 'decision', t: 'Keep the original oak floors', s: 'Decision' },
      { id: 'approved', k: 'event', t: 'Design approved', s: 'Nov 3' },
      { id: 'priya', k: 'person', t: 'Priya Shah', s: 'Architect · Shah Studio' }] },
  { name: 'Quotes', spot: [4.6, 5.2], cam: [12.5, 9, 17], tgt: [4.5, 0.9, 3.6],
    line: 'Jamie’s estimate leaves out electrical. Get Marco’s number before you compare.',
    items: [
      { id: 'estimate', k: 'pdf', t: 'Contractor estimate', s: '$48,200 · 4 days ago' },
      { id: 'quotes', k: 'task', t: 'Compare contractor quotes', s: 'Due today', alert: true },
      { id: 'jamie', k: 'person', t: 'Jamie Carter', s: 'General contractor' },
      { id: 'waiting', k: 'task', t: 'Expect updated estimate', s: 'Friday' },
      { id: 'mood', k: 'image', t: 'Kitchen inspiration', s: 'Photo' }] },
  { name: 'Demolition', spot: [6.9, -2.8], cam: [17.5, 10.5, -4.5], tgt: [4.5, 0.8, -1.6],
    line: 'Starts Dec 15. Jamie confirmed the kitchen wall isn’t load-bearing.',
    items: [
      { id: 'demo', k: 'event', t: 'Demolition starts', s: 'Dec 15 · in 3 weeks' },
      { id: 'notes', k: 'note', t: 'Site visit notes', s: 'Note' },
      { id: 'insurance', k: 'task', t: 'Call insurance about roof claim', s: 'Due today', alert: true }] },
  { name: 'Build', spot: [1.4, -5.6], cam: [8, 10.5, -18], tgt: [2.6, 1.2, -1.2],
    line: 'Electrical inspection is blocked until Marco schedules the panel upgrade.',
    items: [
      { id: 'inspect', k: 'task', t: 'Book electrical inspection', s: 'Blocked', alert: true },
      { id: 'marco', k: 'person', t: 'Marco Ruiz', s: 'Electrician' },
      { id: 'skylight', k: 'idea', t: 'Skylight in the hallway', s: 'Idea' },
      { id: 'shelves', k: 'idea', t: 'Open shelving instead of upper cabinets', s: 'Idea' }] },
  { name: 'Finish', spot: [-1.1, 3.4], cam: [-5.5, 10, 18.5], tgt: [0.5, 1.1, 0.5],
    line: 'Tile samples are a day late. Lena can pick colours once they’re in.',
    items: [
      { id: 'tiles', k: 'task', t: 'Order tile samples', s: '1 day late', alert: true },
      { id: 'lena', k: 'person', t: 'Lena Brooks', s: 'Interior designer' },
      { id: 'floor', k: 'idea', t: 'Heated bathroom floor', s: 'Idea' },
      { id: 'install', k: 'event', t: 'Kitchen install', s: 'Jan 24' }] }
];
const ICON = { pdf: '📄', link: '🔗', decision: '✔', event: '📅', person: '👤', task: '☑', image: '🖼', note: '📝', idea: '💡' };
const ITEMS = {}; STAGES.forEach((s, i) => s.items.forEach(it => { it.stage = i; ITEMS[it.id] = it; }));
let quotesDone = false;

/* ---------- renderer / scene ---------- */
const canvas = $('#c');
const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
renderer.shadowMap.enabled = true; renderer.shadowMap.type = THREE.PCFSoftShadowMap;
renderer.toneMapping = THREE.ACESFilmicToneMapping; renderer.toneMappingExposure = 1.05;
const scene = new THREE.Scene();
scene.fog = new THREE.Fog(0xdfeaff, 30, 60);
const camera = new THREE.PerspectiveCamera(38, 1, .1, 200);
camera.position.set(14, 12, 20);
const controls = new OrbitControls(camera, canvas);
controls.enableDamping = true; controls.dampingFactor = .08; controls.enablePan = false;
controls.minDistance = 7; controls.maxDistance = 34; controls.minPolarAngle = .45; controls.maxPolarAngle = 1.33;
controls.target.set(0, .8, 0); controls.rotateSpeed = .7;

const hemi = new THREE.HemisphereLight(0xdfeeff, 0x8a7a5a, 1.4); scene.add(hemi);
const sun = new THREE.DirectionalLight(0xfff1dc, 2.6);
sun.position.set(9, 16, 7); sun.castShadow = true;
sun.shadow.mapSize.set(2048, 2048); Object.assign(sun.shadow.camera, { left: -13, right: 13, top: 13, bottom: -13, near: 1, far: 50 });
sun.shadow.bias = -0.0006; sun.shadow.normalBias = .03; scene.add(sun);

const mats = {};
const M = (c, o = {}) => new THREE.MeshStandardMaterial({ color: c, roughness: .85, flatShading: true, ...o });
function mesh(geo, mat, x = 0, y = 0, z = 0, parent) { const m = new THREE.Mesh(geo, mat); m.position.set(x, y, z); m.castShadow = m.receiveShadow = true; if (parent) parent.add(m); return m; }
function box(w, h, d, mat, x, y, z, parent) { return mesh(new THREE.BoxGeometry(w, h, d), mat, x, y, z, parent); }
function baseBox(w, h, d) { const g = new THREE.BoxGeometry(w, h, d); g.translate(0, h / 2, 0); return g; } // origin at the bottom
function group(parent, x = 0, y = 0, z = 0) { const g = new THREE.Group(); g.position.set(x, y, z); (parent || scene).add(g); return g; }

/* ---------- canvas textures ---------- */
function tex(w, h, draw) { const c = document.createElement('canvas'); c.width = w; c.height = h; draw(c.getContext('2d'), w, h); const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace; t.anisotropy = 4; return t; }
const blueprint = tex(512, 384, (g, w, h) => {
  g.fillStyle = '#1f4f9a'; g.fillRect(0, 0, w, h); g.strokeStyle = 'rgba(255,255,255,.18)'; g.lineWidth = 1;
  for (let x = 0; x < w; x += 16) { g.beginPath(); g.moveTo(x, 0); g.lineTo(x, h); g.stroke(); }
  for (let y = 0; y < h; y += 16) { g.beginPath(); g.moveTo(0, y); g.lineTo(w, y); g.stroke(); }
  g.strokeStyle = '#fff'; g.lineWidth = 5; g.strokeRect(60, 70, 300, 230); g.strokeRect(360, 90, 110, 190);
  g.lineWidth = 3; g.beginPath(); g.moveTo(230, 70); g.lineTo(230, 300); g.stroke(); g.setLineDash([10, 8]); g.beginPath(); g.moveTo(360, 150); g.lineTo(470, 150); g.stroke();
  g.setLineDash([]); g.strokeRect(390, 180, 50, 30); g.font = 'bold 26px sans-serif'; g.fillStyle = '#fff'; g.fillText('FLOOR PLAN v3', 60, 50); g.font = '18px sans-serif'; g.fillText('living', 110, 190); g.fillText('kitchen', 380, 260); g.fillText('island ← 40 cm', 300, 340);
});
const paperTex = tex(256, 340, (g, w, h) => {
  g.fillStyle = '#fffdf7'; g.fillRect(0, 0, w, h); g.fillStyle = '#6b4bd1'; g.fillRect(0, 0, w, 46); g.fillStyle = '#fff'; g.font = 'bold 20px sans-serif'; g.fillText('CARTER BUILD CO.', 16, 30);
  g.fillStyle = '#222'; g.font = 'bold 22px sans-serif'; g.fillText('ESTIMATE', 16, 84); g.font = '13px sans-serif'; g.fillStyle = '#555'; g.fillText('EST-2026-0118', 16, 104);
  g.fillStyle = '#ccc'; for (let i = 0; i < 7; i++) g.fillRect(16, 130 + i * 20, 150 + (i * 37) % 60, 7);
  g.fillStyle = '#222'; g.font = 'bold 30px sans-serif'; g.fillText('$48,200', 16, 312);
});
const clipTex = tex(256, 320, (g, w, h) => {
  g.fillStyle = '#fffdf7'; g.fillRect(0, 0, w, h); g.fillStyle = '#222'; g.font = 'bold 22px sans-serif'; g.fillText('Compare quotes', 18, 44);
  ['Carter Build', 'Ridge Builders', 'Line by line'].forEach((s, i) => { g.strokeStyle = '#6b4bd1'; g.lineWidth = 4; g.strokeRect(18, 80 + i * 70, 34, 34); if (i === 0) { g.beginPath(); g.moveTo(24, 98 + i * 70); g.lineTo(34, 108 + i * 70); g.lineTo(48, 86 + i * 70); g.stroke(); } g.fillStyle = '#333'; g.font = '20px sans-serif'; g.fillText(s, 66, 104 + i * 70); });
});
const signTex = tex(256, 160, (g, w, h) => { g.fillStyle = '#ffcf33'; g.fillRect(0, 0, w, h); g.fillStyle = '#222'; for (let i = -2; i < 10; i++) { g.beginPath(); g.moveTo(i * 36, h); g.lineTo(i * 36 + 18, h); g.lineTo(i * 36 + 50, h - 30); g.lineTo(i * 36 + 32, h - 30); g.fill(); } g.font = 'bold 34px sans-serif'; g.fillText('DEMO', 64, 56); g.font = 'bold 26px sans-serif'; g.fillText('DEC 15', 72, 96); });
const screenTex = tex(256, 180, (g, w, h) => { g.fillStyle = '#f3f0fa'; g.fillRect(0, 0, w, h); g.fillStyle = '#6b4bd1'; g.fillRect(0, 0, w, 30); g.fillStyle = '#222'; g.font = 'bold 22px sans-serif'; g.fillText('12 kitchen', 14, 70); g.fillText('layouts', 14, 98); g.fillStyle = '#c9a77c'; g.fillRect(150, 50, 90, 110); });

/* ---------- ghost (future = plan, not real yet) ---------- */
const GHOST = new THREE.MeshBasicMaterial({ color: 0x86b4ff, transparent: true, opacity: .34, depthWrite: false });
function setGhost(obj, on) {
  obj.traverse(m => { if (!m.isMesh || m.userData.noGhost) return; if (!m.userData.orig) m.userData.orig = m.material; m.material = on ? GHOST : m.userData.orig; m.castShadow = !on; });
}

/* ---------- island ---------- */
const island = group();
mesh(new THREE.CylinderGeometry(9.8, 9.8, .7, 40), M(0x8cc46a), 0, -.35, 0, island).receiveShadow = true;
mesh(new THREE.CylinderGeometry(9.8, 3.2, 4.2, 12, 2), M(0x9b7653), 0, -2.8, 0, island);
mesh(new THREE.CylinderGeometry(3.2, .6, 2.2, 9), M(0x7d5d42), 0, -6, 0, island);
for (let i = 0; i < 14; i++) { const a = i / 14 * Math.PI * 2 + .3; const r = mesh(new THREE.DodecahedronGeometry(.5 + (i % 3) * .2, 0), M(0x8e8a84), Math.cos(a) * 8.6, -.7, Math.sin(a) * 8.6, island); r.scale.y = .6; }
// clouds
const clouds = [];
for (let i = 0; i < 6; i++) { const c = group(); const a = i / 6 * Math.PI * 2; c.userData = { a, r: 17 + (i % 3) * 3, y: 7 + (i % 2) * 2.5 }; for (let j = 0; j < 4; j++) mesh(new THREE.IcosahedronGeometry(1.1 + (j % 2) * .5, 0), M(0xffffff, { roughness: 1 }), j * 1.2 - 1.8, (j % 2) * .4, (j % 3) * .3, c).castShadow = false; clouds.push(c); }

/* ---------- trail ---------- */
const trailPts = [[-5.4, 4.4], [0, 6.6], [4.6, 5.2], [7.6, 1.6], [6.9, -2.8], [4.6, -5.8], [1.4, -5.6], [-3.6, -5.6], [-7.1, -1.6], [-4.6, 1.9], [-1.1, 3.4]].map(p => new THREE.Vector3(p[0], 0, p[1]));
const STAGE_PT = [0, 2, 4, 6, 10];
const curve = new THREE.CatmullRomCurve3(trailPts, false, 'catmullrom', .4);
const tAt = i => STAGE_PT[i] / (trailPts.length - 1);
const NST = 90;
const stoneGeo = new THREE.CylinderGeometry(.24, .28, .1, 7);
const stoneMat = new THREE.MeshStandardMaterial({ roughness: .9, flatShading: true });
const stones = new THREE.InstancedMesh(stoneGeo, stoneMat, NST); stones.receiveShadow = true; scene.add(stones);
const stoneT = [];
{ const m4 = new THREE.Matrix4(), q = new THREE.Quaternion(); for (let i = 0; i < NST; i++) { const t = i / (NST - 1); stoneT.push(t); const p = curve.getPoint(t); const off = (i % 2 ? .12 : -.12); q.setFromAxisAngle(new THREE.Vector3(0, 1, 0), i * 1.7); m4.compose(new THREE.Vector3(p.x + off, .05, p.z - off), q, new THREE.Vector3(1, 1, 1)); stones.setMatrixAt(i, m4); } }
const cDone = new THREE.Color(0xf3d38a), cLater = new THREE.Color(0xc9c4b8), cTmp = new THREE.Color();
function paintTrail(tNow) { for (let i = 0; i < NST; i++) { stones.setColorAt(i, stoneT[i] <= tNow ? cDone : cLater); } stones.instanceColor.needsUpdate = true; }
// you-are-here beacon
const beacon = group();
const ring = mesh(new THREE.TorusGeometry(.55, .07, 8, 32), new THREE.MeshBasicMaterial({ color: 0x8f6dff }), 0, .1, 0, beacon); ring.rotation.x = Math.PI / 2; ring.castShadow = false;
const beam = mesh(new THREE.CylinderGeometry(.08, .22, 3, 12, 1, true), new THREE.MeshBasicMaterial({ color: 0xa88bff, transparent: true, opacity: .35, depthWrite: false, side: THREE.DoubleSide }), 0, 1.5, 0, beacon); beam.castShadow = false;
const orb = mesh(new THREE.IcosahedronGeometry(.22, 1), new THREE.MeshStandardMaterial({ color: 0x6b4bd1, emissive: 0x6b4bd1, emissiveIntensity: 1.2 }), 0, 3.1, 0, beacon);

/* ---------- house (always there; the old house gets renovated) ---------- */
const OLD = { wall: new THREE.Color(0xcbbfa6), trim: new THREE.Color(0x8b8070), roof: new THREE.Color(0x77706a) };
const NEW = { wall: new THREE.Color(0xf6f1e7), trim: new THREE.Color(0xffffff), roof: new THREE.Color(0x3f4b5e) };
mats.wall = M(OLD.wall.getHex()); mats.trim = M(OLD.trim.getHex()); mats.roof = M(OLD.roof.getHex());
mats.glass = new THREE.MeshStandardMaterial({ color: 0x9cc3e8, roughness: .15, metalness: .1, emissive: 0xffc46b, emissiveIntensity: 0 });
mats.door = M(0x7b5136); mats.oak = M(0xc9955c); mats.slab = M(0xa7a39b); mats.wood = M(0xb98a5a); mats.stud = M(0xd9b77e);
const house = group(scene, 0, 0, 0);
box(6.5, .3, 4.9, mats.slab, 0, .15, 0, house);
box(6.1, .04, 4.5, mats.oak, 0, .32, 0, house).receiveShadow = true; // original oak floors
const H = 2.5, T = .18;
const walls = group(house, 0, .3, 0);
mesh(baseBox(6.2, H, T), mats.wall, 0, 0, -2.25, walls);
mesh(baseBox(T, H, 4.5), mats.wall, -3.05, 0, 0, walls);
mesh(baseBox(T, H, 4.5), mats.wall, 3.05, 0, 0, walls);
const front = group(walls, 0, 0, 2.25);
mesh(baseBox(6.2, H, T), mats.wall, 0, 0, 0, front);
function win(parent, x, y, z, w, h, rotY = 0) { const g = group(parent, x, y, z); g.rotation.y = rotY; box(w + .16, h + .16, .1, mats.trim, 0, 0, 0, g); box(w, h, .12, mats.glass, 0, 0, .01, g); box(.06, h, .14, mats.trim, 0, 0, .02, g); return g; }
win(front, -2.0, 1.4, .06, 1.1, 1.0); win(front, 1.6, 1.4, .06, 1.4, 1.0);
const door = group(front, -.35, 0, .07); box(.95, 2.0, .1, mats.door, 0, 1.0, 0, door); box(1.1, .1, .14, mats.trim, 0, 2.05, 0, door);
win(walls, -3.11, 1.45, .4, 1.0, 1.0, -Math.PI / 2); win(walls, -1, 1.45, -2.31, 1.2, 1.0, Math.PI);
// gables + roof
const roof = group(house, 0, .3 + H, 0);
function gable(x) { const s = new THREE.Shape(); s.moveTo(-2.34, 0); s.lineTo(2.34, 0); s.lineTo(0, 1.45); s.closePath(); const g = new THREE.ExtrudeGeometry(s, { depth: T, bevelEnabled: false }); g.rotateY(Math.PI / 2); return mesh(g, mats.wall, x - T / 2, 0, 0, roof); }
gable(-3.05); gable(3.05);
const L = Math.hypot(2.65, 1.55), ang = Math.atan2(1.55, 2.65);
const rA = box(6.9, .14, L, mats.roof, 0, .78, -1.3, roof); rA.rotation.x = -ang;
const rB = box(6.9, .14, L, mats.roof, 0, .78, 1.3, roof); rB.rotation.x = ang;
box(.5, .7, .5, M(0x9c5a45), -1.8, 1.3, -.9, roof); // chimney
// interior wall between kitchen and living room (comes down in demolition)
const inWall = mesh(baseBox(T, H - .05, 4.3), M(0xd9cfbf), 1.0, .32, 0, house);
// old lean-to kitchen (torn down)
const leanTo = group(house, 3.95, .0, 0);
mesh(baseBox(1.7, 2.0, 3.2), M(0xa49a86), 0, 0, 0, leanTo);
const lr = box(2.0, .12, 3.5, M(0x6f6862), 0, 2.08, 0, leanTo); lr.rotation.z = -.18;
box(.7, .6, .08, M(0x8fa7b9), .2, 1.2, 1.62, leanTo);

/* ---------- models ---------- */
const GLB = {};
const loader = new GLTFLoader();
function b64(s) { const bin = atob(s); const u = new Uint8Array(bin.length); for (let i = 0; i < bin.length; i++) u[i] = bin.charCodeAt(i); return u.buffer; }
async function loadAll() { await Promise.all(Object.keys(MODELS).map(k => new Promise((res, rej) => loader.parse(b64(MODELS[k]), './', g => { GLB[k] = g; res(); }, rej)))); }
function inst(name, size, by = 'h') {
  const src = GLB[name]; const o = src.animations.length ? SkeletonUtils.clone(src.scene) : src.scene.clone(true);
  o.updateMatrixWorld(true); const b = new THREE.Box3().setFromObject(o); const d = b.getSize(new THREE.Vector3());
  const s = size / (by === 'h' ? d.y : by === 'w' ? d.x : d.z); o.scale.multiplyScalar(s); o.updateMatrixWorld(true);
  const b2 = new THREE.Box3().setFromObject(o); const c = b2.getCenter(new THREE.Vector3()); o.position.x -= c.x; o.position.z -= c.z; o.position.y -= b2.min.y;
  o.traverse(m => { if (m.isMesh) { m.castShadow = m.receiveShadow = true; if (m.material && m.material.map) m.material.map.anisotropy = 4; } });
  const w = new THREE.Group(); w.add(o); w.userData.src = src; w.userData.inner = o; return w;
}
const mixers = [];
function animate(w, clipName) { const src = w.userData.src; const mx = new THREE.AnimationMixer(w.userData.inner); const clip = src.animations.find(a => a.name.includes(clipName)) || src.animations[0]; const act = mx.clipAction(clip); act.play(); mixers.push(mx); return { mx, act, play(n) { const c = src.animations.find(a => a.name.includes(n)); if (!c || c === act.getClip()) return; const a2 = mx.clipAction(c); a2.reset().play(); act.crossFadeTo(a2, .3, false); } }; }

/* ---------- tappables ---------- */
const taps = [];
function tappable(obj, itemId) { obj.traverse(m => { if (m.isMesh) m.userData.item = itemId; }); taps.push(obj); }
const glows = [];
function glowRing(parent, x, z, r = .6) { const m = mesh(new THREE.RingGeometry(r * .82, r, 32), new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: .7, depthWrite: false }), x, .04, z, parent); m.rotation.x = -Math.PI / 2; m.castShadow = false; m.userData.noGhost = true; glows.push(m); return m; }

/* ---------- per-stage worlds ---------- */
const S = []; // { root, parts:[], update(a) }
function pop(obj, a, base = 1) { const k = back(a); obj.scale.setScalar(Math.max(.0001, k * base)); obj.visible = a > 0.001; }

function buildStages() {
  // 0 PLAN: chalk outline, drafting table with blueprint, oak sample, tablet, flag
  { const root = group(); const chalk = new THREE.Group(); root.add(chalk);
    const cm = new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: .8 });
    [[6.4, .06, 0, 2.6, 0], [6.4, .06, 0, -2.6, 0], [.06, 5.2, -3.2, 0, 0], [.06, 5.2, 3.2, 0, 0], [3.2, .06, 4.6, 2.1, 0], [3.2, .06, 4.6, -2.1, 0], [.06, 4.2, 6.2, 0, 0]].forEach(([w, d, x, z]) => { const l = mesh(new THREE.BoxGeometry(w, .02, d), cm, x, .03, z, chalk); l.castShadow = false; });
    const table = group(root, -5.4, 0, 4.5); table.rotation.y = .6;
    box(.12, 1.0, .12, mats.wood, -.7, .5, -.4, table); box(.12, 1.0, .12, mats.wood, .7, .5, -.4, table); box(.12, 1.0, .12, mats.wood, -.7, .5, .4, table); box(.12, 1.0, .12, mats.wood, .7, .5, .4, table);
    const top = box(1.8, .08, 1.2, mats.wood, 0, 1.05, 0, table); top.rotation.x = -.25;
    const bp = mesh(new THREE.PlaneGeometry(1.6, 1.05), new THREE.MeshStandardMaterial({ map: blueprint, roughness: .8 }), 0, 1.11, .01, table); bp.rotation.x = -Math.PI / 2 - .25; tappable(bp, 'plan');
    const tablet = box(.42, .03, .3, M(0x2a2a33), .55, 1.18, -.25, table); tablet.rotation.x = -.25; const scr = mesh(new THREE.PlaneGeometry(.38, .26), new THREE.MeshBasicMaterial({ map: screenTex }), .55, 1.2, -.245, table); scr.rotation.x = -Math.PI / 2 - .25; tappable(tablet, 'link'); tappable(scr, 'link');
    const oak = group(root, -3.6, 0, 6.0); for (let i = 0; i < 5; i++) box(1.4, .1, .22, mats.oak, 0, .05 + i * .1, (i % 2) * .05, oak); tappable(oak, 'oak');
    const flag = group(root, -6.8, 0, 3.0); box(.06, 1.8, .06, M(0xeeeeee), 0, .9, 0, flag); const fl = box(.6, .38, .02, M(0x3d9a6b), .32, 1.6, 0, flag); tappable(flag, 'approved');
    const priya = inst('worker', 1.7); priya.position.set(-6.5, 0, 5.4); priya.rotation.y = 2.3; root.add(priya); tappable(priya, 'priya'); const pa = animate(priya, 'Idle');
    glowRing(root, -5.4, 4.5, 1.3);
    S.push({ root, objs: [table, oak, flag, priya], update(a) { chalk.visible = a > 0; cm.opacity = .8 * seg(a, 0, .3); chalk.scale.set(1, 1, 1); pop(table, seg(a, .1, .45)); pop(oak, seg(a, .3, .6)); pop(flag, seg(a, .55, .85)); pop(priya, seg(a, .2, .5)); fl.rotation.y = Math.sin(clock.elapsedTime * 3) * .2; } });
  }
  // 1 QUOTES: workbench + floating estimate + clipboard, Jamie's van drives in, Jamie
  { const root = group(); const bench = group(root, 4.4, 0, 5.4); bench.rotation.y = -.35;
    box(1.9, .1, .9, mats.wood, 0, .95, 0, bench); [[-.85, -.38], [.85, -.38], [-.85, .38], [.85, .38]].forEach(([x, z]) => box(.1, .9, .1, mats.wood, x, .45, z, bench)); box(1.7, .06, .7, mats.wood, 0, .3, 0, bench);
    const paper = group(bench, -.35, 1.45, 0); const pm = mesh(new THREE.PlaneGeometry(.6, .8), new THREE.MeshStandardMaterial({ map: paperTex, side: THREE.DoubleSide, roughness: .7, emissive: 0xffffff, emissiveMap: paperTex, emissiveIntensity: .25 }), 0, 0, 0, paper); tappable(paper, 'estimate');
    const clip = group(bench, .45, 1.0, .05); const cb = box(.5, .02, .62, M(0x8b5a2b), 0, 0, 0, clip); const cpMat = new THREE.MeshStandardMaterial({ map: clipTex }); const cp = mesh(new THREE.PlaneGeometry(.44, .55), cpMat, 0, .012, 0, clip); cp.rotation.x = -Math.PI / 2; box(.18, .04, .06, M(0xaaaaaa), 0, .02, -.28, clip); clip.rotation.y = .3; tappable(clip, 'quotes');
    const mood = group(bench, .5, 1.0, -.32); const mp = box(.34, .02, .26, M(0xd8b48a), 0, 0, 0, mood); tappable(mood, 'mood');
    const van = inst('delivery_van', 2.0); van.rotation.y = -Math.PI / 2 - .5; root.add(van); tappable(van, 'jamie');
    const jamie = inst('worker', 1.75); jamie.position.set(5.9, 0, 4.4); jamie.rotation.y = -2.2; root.add(jamie); tappable(jamie, 'jamie'); const ja = animate(jamie, 'Idle');
    const ringQ = glowRing(root, 4.4, 5.4, 1.4);
    S.push({ root, objs: [bench, van, jamie], update(a, fin) {
      const gone = easeOut(seg(fin, 0, .35));
      pop(bench, seg(a, 0, .2));
      const dv = easeOut(seg(a, 0, .45)); van.visible = a > 0; van.position.set(lerp(13, 7.1, dv) + gone * 7, 0, lerp(8.5, 2.2, dv) + gone * 6); van.visible = a > 0 && gone < .99;
      pop(jamie, seg(a, .35, .55) * (1 - gone)); paper.visible = a > .35; paper.position.y = 1.45 + Math.sin(clock.elapsedTime * 2) * .06; paper.rotation.y = Math.sin(clock.elapsedTime * .8) * .4; paper.scale.setScalar(back(seg(a, .35, .55)) || .0001);
      clip.visible = a > .5; mood.visible = a > .25; clip.rotation.y = .3; cpMat.color.set(quotesDone ? 0xdff5e8 : 0xffffff);
    } });
  }
  // 2 DEMOLITION: dumpster, digger, lean-to flies into the dumpster, interior wall comes down, sign
  { const root = group(); const dump = inst('dumpster', 1.15); dump.position.set(6.6, 0, -3.6); dump.rotation.y = .4; root.add(dump);
    const digger = inst('digger', 1.8); digger.rotation.y = -2.4; root.add(digger);
    const sign = group(root, 7.4, 0, -.6); box(.07, 1.3, .07, M(0x555555), 0, .65, 0, sign); const sp = mesh(new THREE.PlaneGeometry(.9, .56), new THREE.MeshStandardMaterial({ map: signTex, side: THREE.DoubleSide }), 0, 1.25, .05, sign); sign.rotation.y = 1.2; tappable(sign, 'demo');
    const noteBoard = group(root, 5.6, 0, -4.6); box(.5, .06, .65, M(0x8b5a2b), 0, .9, 0, noteBoard); box(.06, .9, .06, M(0x555555), 0, .45, 0, noteBoard); const np = mesh(new THREE.PlaneGeometry(.44, .56), M(0xfffdf2), 0, .94, 0, noteBoard); np.rotation.x = -Math.PI / 2; noteBoard.rotation.y = .5; tappable(noteBoard, 'notes');
    const phone = group(root, 8.0, 0, -2.0); box(.5, .9, .5, M(0x5d5672), 0, .45, 0, phone); box(.3, .5, .05, M(0x6b4bd1, { emissive: 0x6b4bd1, emissiveIntensity: .6 }), 0, .7, .26, phone); phone.rotation.y = 1.4; tappable(phone, 'insurance');
    const chunks = []; const cg = new THREE.BoxGeometry(.5, .5, .5); const chm = M(0xa49a86);
    for (let i = 0; i < 24; i++) { const c = mesh(cg, chm, 0, 0, 0, root); c.userData.from = new THREE.Vector3(3.4 + (i % 3) * .5, .3 + (Math.floor(i / 3) % 4) * .5, -1.4 + Math.floor(i / 12) * 2.6 + (i % 4) * .2); c.userData.spin = new THREE.Vector3((i * 1.3) % 3, (i * 2.1) % 3, (i * .7) % 3); chunks.push(c); }
    const dust = new THREE.Points(new THREE.BufferGeometry().setAttribute('position', new THREE.Float32BufferAttribute(Array.from({ length: 180 }, (_, i) => [(Math.sin(i * 12.9) * 1.2), Math.abs(Math.sin(i * 3.1)) * 1.6, Math.cos(i * 7.7) * 1.2]).flat(), 3)), new THREE.PointsMaterial({ color: 0xd8cfbf, size: .18, transparent: true, opacity: 0, depthWrite: false }));
    dust.position.set(5.4, .3, -2.2); root.add(dust);
    const target = new THREE.Vector3(6.6, 1.2, -3.6);
    S.push({ root, objs: [dump, digger, sign, noteBoard, phone], update(a, fin) {
      const leave = seg(fin, 0, .3);
      pop(dump, seg(a, 0, .15) * (1 - leave)); pop(sign, seg(a, .05, .2) * (1 - leave)); pop(noteBoard, seg(a, .08, .22) * (1 - leave)); pop(phone, seg(a, .1, .25) * (1 - leave));
      const dd = easeOut(seg(a, 0, .3)); digger.visible = a > 0 && leave < 1; digger.position.set(lerp(11, 6.0, dd) + leave * 6, 0, lerp(-8, -1.3, dd) - leave * 4); digger.rotation.y = -2.4 + Math.sin(clock.elapsedTime * 2) * .04 * (a > .3 && a < .8 ? 1 : 0);
      const ka = seg(a, .3, .85);
      leanTo.visible = a <= .3;
      chunks.forEach((c, i) => { const t = seg(ka, i / 24 * .6, i / 24 * .6 + .4); c.visible = ka > 0 && t < 1; if (!c.visible) return; const f = c.userData.from; c.position.set(lerp(f.x, target.x, ease(t)), lerp(f.y, target.y, t) + Math.sin(t * Math.PI) * 2.2, lerp(f.z, target.z, ease(t))); c.rotation.set(c.userData.spin.x * t * 4, c.userData.spin.y * t * 4, c.userData.spin.z * t * 4); c.scale.setScalar(1 - t * .5); });
      const w = seg(a, .5, 1); inWall.scale.y = Math.max(.001, 1 - ease(w)); inWall.visible = w < 1;
      dust.material.opacity = Math.sin(seg(a, .3, 1) * Math.PI) * .7; dust.rotation.y = clock.elapsedTime * .3;
    } });
  }
  // 3 BUILD: truck with logs, studs rise, walls, roof, skylight, Marco at the panel
  { const root = group(); const truck = inst('truck_flat', 1.9); truck.rotation.y = Math.PI * .1; root.add(truck);
    const logs = group(truck, 0, 1.0, -.3); for (let i = 0; i < 6; i++) box(.18, .18, 2.0, mats.stud, -.4 + (i % 3) * .3, Math.floor(i / 3) * .2, 0, logs);
    const ext = group(root, 0, .3, 0); const studs = [];
    const base = box(3.3, .3, 4.0, mats.slab, 4.6, -.15, -.1, ext);
    const perim = []; for (let x = 3.15; x <= 6.2; x += .5) { perim.push([x, -2.0]); perim.push([x, 1.8]); } for (let z = -1.5; z <= 1.4; z += .5) perim.push([6.2, z]);
    perim.forEach(([x, z]) => { const s = mesh(baseBox(.1, H, .1), mats.stud, x, 0, z, ext); studs.push(s); });
    const plates = [box(3.2, .1, .12, mats.stud, 4.65, H, -2.0, ext), box(3.2, .1, .12, mats.stud, 4.65, H, 1.8, ext), box(.12, .1, 3.9, mats.stud, 6.2, H, -.1, ext)];
    const ew = [mesh(baseBox(3.2, H, T), mats.wall, 4.65, 0, -2.0, ext), mesh(baseBox(3.2, H, T), mats.wall, 4.65, 0, 1.8, ext), mesh(baseBox(T, H, 3.9), mats.wall, 6.2, 0, -.1, ext)];
    const bigWin = group(ext, 6.27, 1.35, -.1); bigWin.rotation.y = Math.PI / 2; box(2.4, 1.5, .1, mats.trim, 0, 0, 0, bigWin); box(2.2, 1.3, .12, mats.glass, 0, 0, .01, bigWin);
    const extRoof = box(3.6, .2, 4.3, mats.roof, 4.65, H + .12, -.1, ext);
    const sky = group(roof, 1.3, 0, -1.25); sky.rotation.x = -ang; const skym = box(.9, .18, .7, mats.trim, 0, .86, 0, sky); box(.7, .2, .5, mats.glass, 0, .88, 0, sky); tappable(sky, 'skylight');
    const panel = group(ext, 6.33, .9, 1.2); panel.rotation.y = Math.PI / 2; box(.45, .6, .1, M(0x8d9097), 0, 0, 0, panel); const ledMat = new THREE.MeshBasicMaterial({ color: 0xff4a3d }); const led = box(.08, .08, .05, ledMat, .12, .2, .06, panel); tappable(panel, 'inspect');
    const marco = inst('worker', 1.75); root.add(marco); tappable(marco, 'marco'); const mw = animate(marco, 'Walk');
    S.push({ root, objs: [truck, ext, marco, sky], update(a, fin) {
      const leave = seg(fin, 0, .3);
      const dt = easeOut(seg(a, 0, .2)); truck.visible = a > 0 && leave < 1; truck.position.set(lerp(12, 7.6, dt) + leave * 5, 0, lerp(-9, -4.8, dt) - leave * 3); logs.visible = a < .5;
      base.visible = a > .05; base.scale.y = Math.max(.01, easeOut(seg(a, .05, .15)));
      studs.forEach((s, i) => { const t = seg(a, .12 + i / studs.length * .35, .18 + i / studs.length * .35); s.visible = t > 0; s.scale.y = Math.max(.001, back(t)); });
      plates.forEach(p => p.visible = a > .5);
      ew.forEach((w, i) => { const t = seg(a, .55 + i * .05, .7 + i * .05); w.visible = t > 0; w.scale.y = Math.max(.001, ease(t)); });
      const wt = seg(a, .7, .8); bigWin.visible = wt > 0; bigWin.scale.setScalar(Math.max(.001, back(wt)));
      const rt = seg(a, .75, .88); extRoof.visible = rt > 0; extRoof.position.y = H + .12 + (1 - easeOut(rt)) * 3;
      pop(sky, seg(a, .86, 1)); pop(panel, seg(a, .6, .7));
      ledMat.color.set(a >= 1 && fin > .3 ? 0x3dd17a : 0xff4a3d);
      marco.visible = a > .1 && fin < .6; const t = clock.elapsedTime * .25; const px = 4.6 + Math.cos(t) * 2.6, pz = -.1 + Math.sin(t) * 3.0;
      marco.position.set(px, 0, pz); marco.rotation.y = -t + Math.PI;
    } });
  }
  // 4 FINISH: fresh colours, lights on, furniture, garden, fence, tile crate
  { const root = group(); const inside = group(root, 0, .34, 0); const furn = [];
    const put = (n, s, x, z, ry = 0, by = 'h') => { const o = inst(n, s, by); o.position.set(x, 0, z); o.rotation.y = ry; inside.add(o); furn.push(o); return o; };
    put('rug_rectangle_stripes_A', 2.2, -1.4, .3, 0, 'w'); put('couch_pillows', .85, -1.4, -1.5); put('table_low', .36, -1.4, .2); put('armchair', .85, -2.5, 1.2, 2.2); put('lamp_standing', 1.6, -2.6, -1.7); put('shelf_B_large_decorated', .55, -.2, -1.95, 0);
    const islandK = put('table_medium', .78, 3.0, -.2, Math.PI / 2); put('chair_A_wood', .9, 3.0, .9, Math.PI); put('chair_A_wood', .9, 3.7, .9, Math.PI); put('cabinet_medium_decorated', 1.05, 4.8, -1.75); put('cactus_medium_A', .45, 5.7, 1.4);
    const shelvesI = group(inside, 5.9, 1.4, -.1); for (let i = 0; i < 2; i++) box(.3, .05, 1.6, mats.oak, 0, i * .45, 0, shelvesI); tappable(shelvesI, 'shelves'); furn.push(shelvesI);
    const heatMat = new THREE.MeshBasicMaterial({ color: 0xff8a4a, transparent: true, opacity: .0, depthWrite: false }); const heat = mesh(new THREE.PlaneGeometry(1.2, 1.2), heatMat, -2.3, .03, 1.4, inside); heat.rotation.x = -Math.PI / 2; heat.castShadow = false; tappable(heat, 'floor');
    const garden = []; const gp = (n, s, x, z, ry = 0, by = 'h') => { const o = inst(n, s, by); o.position.set(x, 0, z); o.rotation.y = ry; root.add(o); garden.push(o); return o; };
    [[-7.4, -4.6], [-5.6, -6.8], [2.8, -8.1], [-8.3, 1.2], [7.9, -5.4]].forEach(([x, z], i) => gp(i % 2 ? 'tree_fat' : 'tree_oak', 2.3 + (i % 3) * .3, x, z, i));
    [[-2.6, 3.0], [1.0, 2.9], [2.4, 3.0], [-4.0, 2.6], [3.2, 2.5]].forEach(([x, z], i) => gp('bush', .55, x, z, i, 'h'));
    for (let i = 0; i < 9; i++) { const a = Math.PI * (.15 + i * .088); const f = gp('fence', 1.5, Math.cos(a) * 8.9, Math.sin(a) * 8.9, -a + Math.PI / 2, 'w'); f.userData.fence = true; }
    const crate = inst('crate', .55); crate.position.set(.7, 0, 3.4); crate.rotation.y = .3; root.add(crate); tappable(crate, 'tiles'); const cra = animate(crate, 'open'); cra.act.paused = true; cra.act.time = 0;
    const lena = inst('worker', 1.7); lena.position.set(-2.1, 0, 3.6); lena.rotation.y = .4; root.add(lena); tappable(lena, 'lena'); animate(lena, 'Idle');
    const calendar = group(root, 1.7, 0, 4.0); box(.06, 1.0, .06, M(0x555555), 0, .5, 0, calendar); box(.5, .4, .04, M(0xffffff), 0, 1.15, 0, calendar); box(.5, .1, .05, M(0x6b4bd1), 0, 1.33, 0, calendar); tappable(calendar, 'install');
    S.push({ root, objs: [inside, crate, lena, calendar, ...garden], crate: cra, update(a) {
      const c = ease(seg(a, 0, .4)); mats.wall.color.copy(OLD.wall).lerp(NEW.wall, c); mats.trim.color.copy(OLD.trim).lerp(NEW.trim, c); mats.roof.color.copy(OLD.roof).lerp(NEW.roof, c);
      mats.glass.emissiveIntensity = .9 * seg(a, .3, .6);
      furn.forEach((f, i) => pop(f, seg(a, .4 + i / furn.length * .4, .48 + i / furn.length * .4)));
      garden.forEach((g, i) => pop(g, seg(a, .5 + (i % 9) / 9 * .4, .6 + (i % 9) / 9 * .4)));
      pop(crate, seg(a, .1, .25)); pop(lena, seg(a, .3, .5)); pop(calendar, seg(a, .2, .35));
      heatMat.opacity = a > .9 ? .18 + Math.sin(clock.elapsedTime * 2) * .08 : 0;
    } });
  }
}

/* ---------- state ---------- */
let T_today = 1.4;      // reality: plan done, quotes in progress
let V = 1.4;            // what you are looking at
let sel = 1;            // stage the sheet shows
let anim = null;        // {from,to,t0,dur,key}
const stageGhost = [false, false, false, false, false];
const hereIdx = () => Math.min(4, Math.floor(T_today));
const clock = new THREE.Clock();

function applyTime() {
  const fin = clamp(V - 4) ; // finish progress drives props leaving
  for (let j = 0; j < 5; j++) {
    const a = clamp(V - j);
    const ghost = j > Math.floor(T_today) ? a > 0 : (j === Math.floor(T_today) && V > T_today + .02);
    if (ghost !== stageGhost[j]) { stageGhost[j] = ghost; setGhost(S[j].root, ghost); }
    S[j].update(a, fin);
  }
  // trail + beacon follow reality; the knob follows the view
  const tNow = tFor(T_today); paintTrail(tNow);
  const p = curve.getPoint(tNow); beacon.position.set(p.x, 0, p.z);
  // sky moves dawn -> day -> golden hour with the view
  sky(V / 5);
  const ahead = V > T_today + .05;
  $('#preview').classList.toggle('show', ahead || V < T_today - .3);
  $('#prevtxt').textContent = ahead ? 'Preview · this part isn’t real yet' : 'Looking back';
}
function tFor(x) { const i = Math.min(3, Math.floor(clamp(x, 0, 4.999))); const f = clamp(x - i); const a = Math.min(4, i); return lerp(tAt(a), tAt(Math.min(4, a + 1)), f); }
const SKY = [[0xffd2b0, 0xfff1e0], [0x9ec5ff, 0xe6f0ff], [0x8ebcff, 0xe2eeff], [0x8fb9f5, 0xffe9cf], [0xf7b583, 0xffe3c2]];
const ca = new THREE.Color(), cb = new THREE.Color(), cc = new THREE.Color(), cd = new THREE.Color();
function sky(u) { const x = clamp(u) * (SKY.length - 1); const i = Math.min(SKY.length - 2, Math.floor(x)); const f = x - i; ca.set(SKY[i][0]).lerp(cb.set(SKY[i + 1][0]), f); cc.set(SKY[i][1]).lerp(cd.set(SKY[i + 1][1]), f);
  $('#sky').style.background = `linear-gradient(#${ca.getHexString()}, #${cc.getHexString()} 72%)`; scene.fog.color.copy(cc); sun.color.set(0xfff1dc).lerp(cd.set(0xffc58a), clamp((u - .7) / .3)); sun.intensity = 2.2 + Math.sin(clamp(u) * Math.PI) * .7; }

/* ---------- camera flights ---------- */
let flight = null; let idleAt = 0;
function flyTo(i, dur = 1.3) { const s = i < 0 ? OVERVIEW : STAGES[i]; flight = { p0: camera.position.clone(), t0v: controls.target.clone(), p1: new THREE.Vector3(...s.cam), t1: new THREE.Vector3(...s.tgt), start: clock.elapsedTime, dur: reduce ? .01 : dur }; }
function animV(to, dur = 1, then) { anim = { from: V, to, start: clock.elapsedTime, dur: reduce ? .01 : dur, then }; }

/* ---------- UI: pins ---------- */
const pinsEl = $('#pins');
const pins = STAGES.map((s, i) => { const d = document.createElement('div'); d.className = 'pin'; d.innerHTML = `<b><i></i><span></span></b>`; d.querySelector('span').textContent = s.name; d.addEventListener('click', e => { e.stopPropagation(); select(i, true); }); pinsEl.appendChild(d); return d; });
const v3 = new THREE.Vector3();
function placePins() { const w = innerWidth, h = innerHeight; const h0 = hereIdx();
  pins.forEach((d, i) => { const s = STAGES[i]; v3.set(s.spot[0], 2.3, s.spot[1]).project(camera); const vis = v3.z < 1; d.style.display = vis ? '' : 'none'; d.style.left = ((v3.x + 1) / 2 * w) + 'px'; d.style.top = ((1 - v3.y) / 2 * h) + 'px';
    d.className = 'pin ' + (i < h0 ? 'done' : i === h0 ? 'here' : 'later') + (i === sel ? ' sel' : ''); d.querySelector('i').textContent = i < h0 ? '✓' : String(i + 1); }); }

/* ---------- UI: dial ---------- */
const track = $('#track'), knob = $('#knob'), fill = $('#fill');
$('#dots').innerHTML = STAGES.map((s, i) => `<div class="dot" style="left:${(i + .5) / 5 * 100}%"></div>`).join('');
function drawDial() { const pct = clamp(V / 5) * 100; knob.style.left = pct + '%'; fill.style.width = clamp(T_today / 5) * 100 + '%'; $('#todaylab').style.left = clamp(T_today / 5) * 100 + '%';
  document.querySelectorAll('.dot').forEach((d, i) => d.classList.toggle('done', i < hereIdx() || T_today >= 4.99));
  const done = T_today >= 4.99; $('#prog').style.width = (done ? 100 : hereIdx() / 5 * 100) + '%'; $('#sub').textContent = done ? 'Trail · finished · kept as a memory' : `Trail · stop ${hereIdx() + 1} of 5 · ${hereIdx() * 20}% of the way`; }
let dragging = false, lastStage = -1;
function dialAt(x) { const r = track.getBoundingClientRect(); V = clamp((x - r.left) / r.width, 0, 1) * 5; V = Math.min(V, 4.999 + .001); const st = Math.min(4, Math.floor(V)); if (st !== lastStage) { if (lastStage >= 0) buzz(12); lastStage = st; if (st !== sel) { sel = st; renderSheet(); } } }
track.addEventListener('pointerdown', e => { dragging = true; anim = null; track.setPointerCapture(e.pointerId); lastStage = Math.floor(V); dialAt(e.clientX); hideHint(); });
track.addEventListener('pointermove', e => { if (dragging) dialAt(e.clientX); });
track.addEventListener('pointerup', () => { dragging = false; });
knob.addEventListener('keydown', e => { if (e.key === 'ArrowRight') V = Math.min(5, V + .25); if (e.key === 'ArrowLeft') V = Math.max(0, V - .25); sel = Math.min(4, Math.floor(V)); renderSheet(); });
$('#backtoday').addEventListener('click', () => { animV(T_today, .9); sel = hereIdx(); renderSheet(); flyTo(sel); });

/* ---------- UI: sheet ---------- */
const sheet = $('#sheet');
function esc(s) { return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
let confirming = false;
function renderSheet() {
  const s = STAGES[sel], h0 = hereIdx(); const st = T_today >= 4.99 ? 'DONE' : sel < h0 ? 'DONE' : sel === h0 ? 'YOU ARE HERE' : 'LATER';
  const open = s.items.filter(i => i.alert && !(i.id === 'quotes' && quotesDone)).length;
  let line = s.line; if (sel === 1 && quotesDone) line = 'Both quotes compared. Ready for demolition.';
  sheet.innerHTML = `<div class="kick">${st} · STOP ${sel + 1}</div><h2>${esc(s.name)}</h2><div class="ai">${esc(line)}</div>
  <div class="tiles">${s.items.map(it => { const done = it.id === 'quotes' && quotesDone; return `<button class="tile ${it.alert && !done ? 'alert' : ''}" data-id="${it.id}"><span class="ic">${done ? '✅' : ICON[it.k]}</span><span><b>${esc(it.t)}</b><small>${esc(done ? 'Done' : it.s)}</small></span></button>`; }).join('')}</div>
  ${T_today >= 4.99 && sel === 4 ? `<div class="acts"><button class="btn ghost" id="replay">▶ Replay the build</button></div>` : sel === h0 ? (confirming ? `<div class="confirm">“Compare contractor quotes” is still open.</div><div class="acts"><button class="btn" id="markgo">Mark done &amp; move on</button><button class="btn ghost" id="anyway">Move on anyway</button></div>` : `<div class="acts"><button class="btn" id="go">Done · move on</button>${open ? `<button class="btn ghost" id="seeopen">${open} open</button>` : ''}</div>`) : ''}`;
  sheet.querySelectorAll('.tile').forEach(b => b.addEventListener('click', () => openItem(b.dataset.id)));
  const go = $('#go'); if (go) go.addEventListener('click', () => { if (sel === 1 && !quotesDone) { confirming = true; renderSheet(); } else moveOn(); });
  const mg = $('#markgo'); if (mg) mg.addEventListener('click', () => { quotesDone = true; confirming = false; buzz(15); moveOn(); });
  const an = $('#anyway'); if (an) an.addEventListener('click', () => { confirming = false; moveOn(); });
  const rp = $('#replay'); if (rp) rp.addEventListener('click', () => { flyTo(-1, 1.2); V = 0; animV(4.999, reduce ? .01 : 9); anim.follow = true; });
  const so = $('#seeopen'); if (so) so.addEventListener('click', () => openItem(s.items.find(i => i.alert).id));
}
function select(i, fly) { sel = i; confirming = false; renderSheet(); if (fly) { flyTo(i); const target = i < hereIdx() ? i + .999 : i === hereIdx() ? T_today : i + .999; animV(target, 1.1); } hideHint(); }

let undo = null;
function moveOn() {
  if (T_today >= 4.99) return; const from = T_today, fromQ = quotesDone; const next = Math.floor(T_today) + 1;
  const to = next >= 4 ? 4.999 : next + .4;
  undo = { T: from, q: fromQ };
  sel = Math.min(4, next); flyTo(sel, 1.6);
  anim = { from: V, to, start: clock.elapsedTime, dur: reduce ? .01 : 2.6, today: true, tfrom: from, then() { buzz([30, 40, 60]); renderSheet(); toast(to > 4.99 ? 'Journey complete 🎉' : `${STAGES[sel].name} started`, true); if (to > 4.99) flyTo(-1, 1.6); } };
  renderSheet();
}
function doUndo() { if (!undo) return; const u = undo; undo = null; quotesDone = u.q; anim = { from: V, to: u.T, start: clock.elapsedTime, dur: reduce ? .01 : 1.4, today: true, tfrom: T_today, then: renderSheet }; sel = Math.floor(u.T); flyTo(sel); renderSheet(); hideToast(); }

let toastTimer;
function toast(msg, withUndo) { const t = $('#toast'); t.innerHTML = `<span>${esc(msg)}</span>${withUndo ? '<button id="undo">Undo</button>' : ''}`; t.classList.add('show'); const u = $('#undo'); if (u) u.addEventListener('click', doUndo); clearTimeout(toastTimer); toastTimer = setTimeout(hideToast, 5000); }
function hideToast() { $('#toast').classList.remove('show'); }

/* ---------- viewer ---------- */
const viewer = $('#viewer'), vcard = $('#vcard');
viewer.addEventListener('click', e => { if (e.target === viewer) closeViewer(); });
function closeViewer() { viewer.classList.remove('show'); }
function openItem(id) {
  const it = ITEMS[id]; if (!it) return; buzz(8);
  let body = '';
  if (id === 'estimate') body = `<div class="facts"><div class="k">PULLED OUT FOR YOU</div><div class="r"><span>Total</span><span>$48,200</span></div><div class="r"><span>Valid until</span><span>Jan 2, 2027</span></div><div class="r"><span>Excludes</span><span>Electrical work</span></div><div class="r"><span>Estimate number</span><span>EST-2026-0118</span></div></div><div class="doc">CARTER BUILD CO. ESTIMATE EST-2026-0118\nKitchen demolition, cabinet allowance revised per site visit, drywall and finish.\nExcludes electrical.</div>`;
  else if (id === 'quotes') body = `<div class="steps"><div class="ok"><i>✓</i>Get Carter Build estimate</div><div class="${quotesDone ? 'ok' : ''}"><i>${quotesDone ? '✓' : ''}</i>Get a second quote · Ridge Builders</div><div class="${quotesDone ? 'ok' : ''}"><i>${quotesDone ? '✓' : ''}</i>Compare line by line</div></div><p class="meta">Make sure both quotes include demo haul-away. Due today.</p><div class="acts"><button class="btn" id="tdone">${quotesDone ? 'Reopen' : 'Mark done'}</button></div>`;
  else if (id === 'jamie') body = `<div class="facts"><div class="r"><span>Company</span><span>Carter Build Co.</span></div><div class="r"><span>Mobile</span><span>(555) 014-2290</span></div><div class="r"><span>Email</span><span>jamie@carterbuild.example</span></div></div><div class="ai">Prefers a text before 8 AM, not calls.</div><div class="ai">Recommended Dana at the tile shop.</div>`;
  else if (id === 'plan') body = `<div class="doc" style="background:#1f4f9a;color:#fff">FLOOR PLAN v3\nRevised layout from Priya: island moved 40 cm toward the window.</div>`;
  else if (id === 'mood') body = `<div class="mood"><div style="grid-row:span 2;background:linear-gradient(135deg,#c8a175,#efe3cf)"></div><div style="background:linear-gradient(#e9e4dc,#cfc6b8)"></div><div style="background:linear-gradient(#8a9a7b,#c7cfb6)"></div></div><p class="meta">Warm wood, white oak shelves, matte tiles.</p>`;
  else if (id === 'link') body = `<p class="meta">A tour of galley, L-shape, U-shape and island layouts, with clearances and work-triangle tips.</p><div class="ai">Leave at least 42 inches between the island and the counters.</div>`;
  else if (id === 'notes') body = `<div class="doc">Jamie confirmed the wall between kitchen and living room is not load-bearing. Marco wants to upgrade the panel.</div>`;
  else if (id === 'tiles') { body = `<p class="meta">Due yesterday. Lena needs them to choose the backsplash.</p>`; if (S[4].crate) { const c = S[4].crate; c.act.paused = false; c.act.reset(); c.act.setLoop(THREE.LoopOnce); c.act.clampWhenFinished = true; c.act.play(); } }
  else body = `<p class="meta">${esc(it.s)}</p>`;
  vcard.innerHTML = `<div class="vhead"><span style="font-size:22px">${ICON[it.k]}</span><div class="grow"><h3>${esc(it.t)}</h3><div class="meta">${esc(STAGES[it.stage].name)} · ${esc(it.s)}</div></div><button id="vclose" aria-label="Close">✕</button></div>${body}<div class="ask"><input placeholder="Ask about this…" aria-label="Ask about this"><button class="btn">Ask</button></div>`;
  $('#vclose').addEventListener('click', closeViewer);
  const td = $('#tdone'); if (td) td.addEventListener('click', () => { quotesDone = !quotesDone; buzz(15); openItem('quotes'); renderSheet(); });
  viewer.classList.add('show');
}

/* ---------- inside (cutaway) ---------- */
let insideOn = false, insideK = 0;
$('#inside').addEventListener('click', () => { insideOn = !insideOn; $('#inside').classList.toggle('on', insideOn); $('#inside').setAttribute('aria-pressed', insideOn); buzz(10); });

/* ---------- tapping the 3D world ---------- */
const ray = new THREE.Raycaster(), ndc = new THREE.Vector2(); let down = null, lastTap = 0;
canvas.addEventListener('pointerdown', e => { down = { x: e.clientX, y: e.clientY, t: performance.now() }; idleAt = clock.elapsedTime; flight = null; hideHint(); });
canvas.addEventListener('pointerup', e => {
  if (!down || Math.hypot(e.clientX - down.x, e.clientY - down.y) > 8) return; down = null;
  const now = performance.now(); if (now - lastTap < 320) { lastTap = 0; clearTimeout(tapTimer); flyTo(-1, 1.2); return; } lastTap = now;
  const ex = e.clientX, ey = e.clientY; clearTimeout(tapTimer); tapTimer = setTimeout(() => singleTap(ex, ey), 300);
});
let tapTimer;
function singleTap(x, y) {
  ndc.set(x / innerWidth * 2 - 1, -(y / innerHeight) * 2 + 1); ray.setFromCamera(ndc, camera);
  const hits = ray.intersectObjects(taps, true).filter(h => h.object.visible && isShown(h.object));
  if (hits.length) { openItem(hits[0].object.userData.item); return; }
  const rh = ray.intersectObjects([roof, walls], true); if (rh.length) { insideOn = !insideOn; $('#inside').classList.toggle('on', insideOn); buzz(10); }
}
function isShown(o) { while (o) { if (!o.visible) return false; o = o.parent; } return true; }
let hintGone = false; function hideHint() { if (hintGone) return; hintGone = true; $('#hint').style.opacity = 0; }
setTimeout(hideHint, 6000);

/* ---------- loop ---------- */
function resize() { const w = innerWidth, h = innerHeight; renderer.setSize(w, h, false); camera.aspect = w / h; camera.fov = w < h ? 46 : 34;
  const bot = document.querySelector('.bottom').offsetHeight, top = 64; const oy = Math.max(0, (bot - top) / 2); camera.setViewOffset(w, h, 0, oy, w, h); camera.updateProjectionMatrix(); }
new ResizeObserver(resize).observe(document.querySelector('.bottom'));
addEventListener('resize', resize);
function frame() {
  const dt = Math.min(clock.getDelta(), .05); const tt = clock.elapsedTime;
  if (anim) { const k = clamp((tt - anim.start) / anim.dur); const e = ease(k); V = lerp(anim.from, anim.to, e); if (anim.today) T_today = lerp(anim.tfrom, anim.to, e); if (anim.follow) { const st = Math.min(4, Math.floor(V)); if (st !== sel) { sel = st; renderSheet(); buzz(10); } } if (k >= 1) { const th = anim.then; anim = null; if (th) th(); drawDial(); } }
  if (flight) { const k = ease(clamp((tt - flight.start) / flight.dur)); camera.position.lerpVectors(flight.p0, flight.p1, k); controls.target.lerpVectors(flight.t0v, flight.t1, k); if (k >= 1) flight = null; }
  else { controls.autoRotate = tt - idleAt > 8 && !reduce; controls.autoRotateSpeed = .35; }
  controls.update();
  applyTime(); drawDial();
  // cutaway: roof lifts and slides back, front wall drops low
  insideK += ((insideOn ? 1 : 0) - insideK) * Math.min(1, dt * 6);
  roof.position.y = .3 + H + insideK * 3.2; roof.position.z = -insideK * 1.5; roof.rotation.x = -insideK * .35; front.scale.y = 1 - insideK * .72;
  mixers.forEach(m => m.update(dt));
  ring.scale.setScalar(1 + Math.sin(tt * 3) * .12); orb.position.y = 3.1 + Math.sin(tt * 2) * .12; beam.material.opacity = .25 + Math.sin(tt * 3) * .1;
  glows.forEach((g, i) => { g.material.opacity = .35 + Math.sin(tt * 2.5 + i) * .25; });
  clouds.forEach(c => { const u = c.userData; const a = u.a + tt * .02; c.position.set(Math.cos(a) * u.r, u.y, Math.sin(a) * u.r); c.rotation.y = -a; });
  placePins();
  renderer.render(scene, camera);
  requestAnimationFrame(frame);
}

/* ---------- start ---------- */
resize();
loadAll().then(() => {
  buildStages(); renderSheet(); drawDial();
  camera.position.set(24, 22, 30); flyTo(-1, 2.4);
  $('#load').style.opacity = 0; setTimeout(() => $('#load').remove(), 600);
  frame();
}).catch(err => { $('#load').textContent = 'Could not load the scene: ' + err.message; console.error(err); });
}
