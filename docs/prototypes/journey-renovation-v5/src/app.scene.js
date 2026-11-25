
/* ======================================================================
   3D — the whole property: house, garage, yard, street
   ====================================================================== */
const canvas = $('#c');
const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
renderer.setPixelRatio(Math.min(devicePixelRatio, 2)); renderer.shadowMap.enabled = true; renderer.shadowMap.type = THREE.PCFSoftShadowMap;
renderer.toneMapping = THREE.ACESFilmicToneMapping; renderer.toneMappingExposure = 1.05;
const scene = new THREE.Scene(); scene.fog = new THREE.Fog(0xf3efe8, 38, 70);
const mainRoot = new THREE.Group(); scene.add(mainRoot); const cabinRoot = new THREE.Group(); scene.add(cabinRoot); const condoRoot = new THREE.Group(); scene.add(condoRoot); let ROOT = mainRoot;
const camera = new THREE.PerspectiveCamera(40, 1, .1, 200);
const controls = new OrbitControls(camera, canvas);
Object.assign(controls, { enableDamping: true, dampingFactor: .08, enablePan: false, minDistance: 4, maxDistance: 42, minPolarAngle: .25, maxPolarAngle: 1.25, rotateSpeed: .6 });
scene.add(new THREE.HemisphereLight(0xfdf6ec, 0x8e9a78, 1.35));
const sun = new THREE.DirectionalLight(0xfff0da, 2.6); sun.position.set(-9, 18, 12); sun.castShadow = true; sun.shadow.mapSize.set(2048, 2048);
Object.assign(sun.shadow.camera, { left: -14, right: 14, top: 14, bottom: -14, near: 1, far: 60 }); sun.shadow.bias = -.0004; sun.shadow.normalBias = .04; scene.add(sun);
const fill = new THREE.DirectionalLight(0xdfe8ff, .5); fill.position.set(10, 8, -8); scene.add(fill);

function tex(w, h, draw, rx = 1, ry = 1) { const c = document.createElement('canvas'); c.width = w; c.height = h; draw(c.getContext('2d'), w, h); const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace; t.wrapS = t.wrapT = THREE.RepeatWrapping; t.repeat.set(rx, ry); t.anisotropy = 8; return t; }
const rnd = (() => { let s = 7; return () => (s = (s * 16807) % 2147483647) / 2147483647; })();
const T = {
  siding: () => tex(256, 256, (g, w, h) => { g.fillStyle = '#fff'; g.fillRect(0, 0, w, h); for (let y = 0; y < h; y += 32) { const gr = g.createLinearGradient(0, y, 0, y + 32); gr.addColorStop(0, 'rgba(0,0,0,.20)'); gr.addColorStop(.12, 'rgba(0,0,0,.02)'); gr.addColorStop(1, 'rgba(0,0,0,.08)'); g.fillStyle = gr; g.fillRect(0, y, w, 32); } }),
  shingle: () => tex(256, 256, (g, w, h) => { for (let r = 0; r < 8; r++) for (let c = 0; c < 9; c++) { const v = 200 + rnd() * 55 | 0; g.fillStyle = `rgb(${v},${v},${v})`; const off = r % 2 ? 14 : 0; g.fillRect(c * 30 - off, r * 32, 29, 31); } g.fillStyle = 'rgba(0,0,0,.25)'; for (let r = 0; r < 8; r++) g.fillRect(0, r * 32 + 29, w, 3); }),
  grass: () => tex(256, 256, (g, w, h) => { g.fillStyle = '#8fbf6a'; g.fillRect(0, 0, w, h); for (let i = 0; i < 2600; i++) { g.fillStyle = `rgba(${60 + rnd() * 60 | 0},${120 + rnd() * 60 | 0},${40 + rnd() * 40 | 0},.35)`; g.fillRect(rnd() * w, rnd() * h, 2, 4); } }),
  planks: () => tex(256, 256, (g, w, h) => { for (let r = 0; r < 8; r++) { const v = rnd() * 25 | 0; g.fillStyle = `rgb(${196 - v},${146 - v},${92 - v})`; g.fillRect(0, r * 32, w, 32); g.fillStyle = 'rgba(0,0,0,.18)'; g.fillRect(0, r * 32, w, 2); g.fillRect((r * 97) % w, r * 32, 2, 32); } }),
  tile: () => tex(256, 256, (g, w, h) => { g.fillStyle = '#eceae4'; g.fillRect(0, 0, w, h); g.fillStyle = '#cfcac0'; for (let i = 0; i <= 4; i++) { g.fillRect(i * 64 - 1, 0, 2, h); g.fillRect(0, i * 64 - 1, w, 2); } }),
  brick: () => tex(256, 256, (g, w, h) => { g.fillStyle = '#d8cfc4'; g.fillRect(0, 0, w, h); for (let r = 0; r < 8; r++) for (let c = 0; c < 5; c++) { const v = rnd() * 30 | 0; g.fillStyle = `rgb(${170 - v},${90 - v},${68 - v})`; g.fillRect(c * 56 - (r % 2 ? 28 : 0) + 2, r * 32 + 2, 52, 28); } }),
  concrete: () => tex(256, 256, (g, w, h) => { g.fillStyle = '#d6d3cd'; g.fillRect(0, 0, w, h); for (let i = 0; i < 1500; i++) { g.fillStyle = `rgba(0,0,0,${rnd() * .06})`; g.fillRect(rnd() * w, rnd() * h, 3, 3); } g.fillStyle = 'rgba(0,0,0,.12)'; g.fillRect(0, h - 2, w, 2); }),
  asphalt: () => tex(256, 256, (g, w, h) => { g.fillStyle = '#5d6066'; g.fillRect(0, 0, w, h); for (let i = 0; i < 2500; i++) { g.fillStyle = `rgba(255,255,255,${rnd() * .06})`; g.fillRect(rnd() * w, rnd() * h, 2, 2); } }),
  garage: () => tex(256, 256, (g, w, h) => { g.fillStyle = '#f4f1ea'; g.fillRect(0, 0, w, h); for (let r = 0; r < 4; r++) { g.fillStyle = 'rgba(0,0,0,.12)'; g.fillRect(0, r * 64, w, 3); for (let c = 0; c < 4; c++) { g.strokeStyle = 'rgba(0,0,0,.08)'; g.strokeRect(c * 64 + 8, r * 64 + 10, 48, 44); } } }),
  osb: () => tex(256, 256, (g, w, h) => { g.fillStyle = '#c9a66b'; g.fillRect(0, 0, w, h); for (let i = 0; i < 500; i++) { g.fillStyle = `rgba(${120 + rnd() * 60 | 0},${80 + rnd() * 40 | 0},30,.35)`; g.save(); g.translate(rnd() * w, rnd() * h); g.rotate(rnd() * 3); g.fillRect(0, 0, 18, 5); g.restore(); } })
};
const mats = {
  siding: new THREE.MeshStandardMaterial({ color: 0xc9d1c3, map: T.siding(), roughness: .85, transparent: true }),
  paint: new THREE.MeshStandardMaterial({ color: 0xf2ede4, roughness: .9 }),
  trim: new THREE.MeshStandardMaterial({ color: 0xffffff, roughness: .6 }),
  roof: new THREE.MeshStandardMaterial({ color: 0x5a5f69, map: T.shingle(), roughness: .9, transparent: true }),
  glass: new THREE.MeshStandardMaterial({ color: 0x9ec4e6, roughness: .08, metalness: .35, envMapIntensity: 1 }),
  door: new THREE.MeshStandardMaterial({ color: 0x8a5a36, roughness: .55 }),
  found: new THREE.MeshStandardMaterial({ color: 0xb8b3aa, map: T.concrete(), roughness: .95 }),
  oak: new THREE.MeshStandardMaterial({ map: T.planks(), roughness: .7 }),
  tile: new THREE.MeshStandardMaterial({ map: T.tile(), roughness: .5 }),
  brick: new THREE.MeshStandardMaterial({ map: T.brick(), roughness: .9 }),
  grass: new THREE.MeshStandardMaterial({ map: T.grass(), roughness: 1, transparent: true }),
  conc: new THREE.MeshStandardMaterial({ map: T.concrete(), roughness: .95 }),
  asphalt: new THREE.MeshStandardMaterial({ map: T.asphalt(), roughness: 1 }),
  gdoor: new THREE.MeshStandardMaterial({ map: T.garage(), roughness: .6 }),
  cab: new THREE.MeshStandardMaterial({ color: 0x5f7a6a, roughness: .6 }), counter: new THREE.MeshStandardMaterial({ color: 0xefece6, roughness: .3 }),
  steel: new THREE.MeshStandardMaterial({ color: 0xd9dcdf, roughness: .3, metalness: .6 }), gutter: new THREE.MeshStandardMaterial({ color: 0xf0f0f0, roughness: .4 }),
  ghost: new THREE.MeshBasicMaterial({ color: 0x86b4ff, transparent: true, opacity: .28, depthWrite: false }),
  osb: new THREE.MeshStandardMaterial({ map: T.osb(), roughness: .9 }), soil: new THREE.MeshStandardMaterial({ color: 0x6e5038, roughness: 1 })
};
mats.grass.map.repeat.set(10, 9); mats.conc.map.repeat.set(2, 3); mats.asphalt.map.repeat.set(8, 1); mats.oak.map.repeat.set(2, 2); mats.tile.map.repeat.set(2, 2);
function mesh(g, m, x = 0, y = 0, z = 0, p) { const o = new THREE.Mesh(g, m); o.position.set(x, y, z); o.castShadow = o.receiveShadow = true; (p || ROOT).add(o); return o; }
function box(w, h, d, m, x, y, z, p) { return mesh(new THREE.BoxGeometry(w, h, d), m, x, y, z, p); }
function bbox(w, h, d, m, x, y, z, p) { const g = new THREE.BoxGeometry(w, h, d); g.translate(0, h / 2, 0); return mesh(g, m, x, y, z, p); }
function grp(p, x = 0, y = 0, z = 0) { const g = new THREE.Group(); g.position.set(x, y, z); (p || ROOT).add(g); return g; }
function uv(m, rx, ry) { const t = m.material.map; if (!t) return; m.material = m.material.clone(); m.material.map = t.clone(); m.material.map.needsUpdate = true; m.material.map.repeat.set(rx, ry); }

const PARTS = {}; const TAGPOS = {};
function part(key, o, pos) { (PARTS[key] = PARTS[key] || []).push(o); if (pos) TAGPOS[key] = pos; return o; }

/* ---- lot, street, paths ---- */
const ground = box(24, .2, 21, mats.grass, 0, -.1, -1); ground.receiveShadow = true;
box(24, .1, 3, mats.asphalt, 0, -.04, 11); box(24, .12, 1.1, mats.conc, 0, .01, 9.0);
box(2.6, .12, 6.2, mats.conc, 5.8, .02, 5.0); // driveway
box(1.2, .12, 4.4, mats.conc, .7, .02, 6.6); // front walk
for (let i = -11; i < 12; i += 3) box(1.4, .02, .14, new THREE.MeshBasicMaterial({ color: 0xf5f0d8 }), i, .02, 11);

/* ---- house ---- */
const FY = .35, H = 2.6, WT = .2;
const house = grp();
bbox(8.2, FY, 6.2, mats.found, 0, 0, 0, house);
const extWalls = grp(house, 0, FY, 0); const inWalls = grp(house, 0, FY, 0);
function face(arr, out) { return [0, 1, 2, 3, 4, 5].map(i => i === out ? mats.siding : (i === 2 ? mats.trim : mats.paint)); }
function xwall(w, d, x, z, out, key) { const g = new THREE.BoxGeometry(w, H, d); g.translate(0, H / 2, 0); const m = new THREE.Mesh(g, face(null, out)); m.position.set(x, 0, z); m.castShadow = m.receiveShadow = true; extWalls.add(m); const sx = (out < 2 ? d : w) / 2, sy = H / 2; const t = mats.siding.map; return part(key, m); }
xwall(8.2, WT, 0, 3.0, 4, 'sFront'); xwall(8.2, WT, 0, -3.0, 5, 'sBack'); xwall(WT, 6.2, -4.0, 0, 1, 'sLeft'); xwall(WT, 6.2, 4.0, 0, 0, 'sRight');
// windows + door
const wins = grp(house, 0, FY, 0);
function win(x, y, z, w, h, ry) { const g = grp(wins, x, y, z); g.rotation.y = ry; box(w + .2, h + .2, .12, mats.trim, 0, 0, 0, g); box(w, h, .1, mats.glass, 0, 0, .03, g); box(.06, h, .14, mats.trim, 0, 0, .04, g); box(w, .06, .14, mats.trim, 0, 0, .04, g); box(w + .35, .08, .25, mats.trim, 0, -h / 2 - .12, .08, g); return g; }
win(-2.2, 1.55, 3.1, 1.6, 1.2, 0); win(2.8, 1.75, 3.1, .8, .8, 0); win(-2.2, 1.55, -3.1, 1.2, 1.1, Math.PI); win(2.2, 1.6, -3.1, 1.8, 1.0, Math.PI); win(-4.1, 1.55, 1.4, 1.2, 1.1, -Math.PI / 2); win(-4.1, 1.55, -1.6, 1.0, 1.0, -Math.PI / 2);
const door = grp(wins, .7, 0, 3.1); box(1.0, 2.1, .1, mats.door, 0, 1.05, 0, door); box(1.25, .12, .16, mats.trim, 0, 2.16, 0, door); box(.12, 2.2, .16, mats.trim, -.56, 1.1, 0, door); box(.12, 2.2, .16, mats.trim, .56, 1.1, 0, door); box(.06, .06, .08, mats.steel, .35, 1.0, .08, door);
// porch
const porch = grp(house, .7, 0, 3.0); box(2.4, FY, 1.3, mats.found, 0, FY / 2, .75, porch); box(2.4, .12, .4, mats.conc, 0, .06, 1.6, porch);
[-1.05, 1.05].forEach(x => box(.12, 2.4, .12, mats.trim, x, FY + 1.2, 1.3, porch)); const pr = box(2.8, .12, 1.6, mats.roof, 0, FY + 2.5, .75, porch); pr.rotation.x = .18;
// floors + interior walls (rooms)
const floorParts = grp(house, 0, FY, 0);
function rfloor(key, x0, x1, z0, z1, m, rx = 2, ry = 2) { const f = box(x1 - x0, .04, z1 - z0, m, (x0 + x1) / 2, .02, (z0 + z1) / 2, floorParts); uv(f, (x1 - x0) / 2.5 * rx / 2, (z1 - z0) / 2.5 * ry / 2); f.userData.area = key; return part(key, f, [(x0 + x1) / 2, 1.6, (z0 + z1) / 2]); }
rfloor('living', -3.9, 0, -.2, 2.9, mats.oak); rfloor('bed', -3.9, 0, -2.9, -.2, mats.oak); rfloor('entry', 0, 1.4, .6, 2.9, mats.oak); rfloor('bath', 1.4, 3.9, .6, 2.9, mats.tile); rfloor('kitchen', 0, 3.9, -2.9, .6, mats.tile);
const iw = (w, d, x, z) => bbox(w, H, d, mats.paint, x, 0, z, inWalls);
iw(3.9, .12, -1.95, -.2); iw(.12, 2.3, 1.4, 1.75); iw(2.5, .12, 2.65, .6); iw(.12, 1.0, 0, 2.4);
// kitchen
const kit = grp(house, 0, FY, 0);
bbox(3.6, .9, .62, mats.cab, 2.05, 0, -2.58, kit); box(3.7, .05, .68, mats.counter, 2.05, .93, -2.58, kit);
bbox(.62, .9, 2.4, mats.cab, 3.58, 0, -1.4, kit); box(.68, .05, 2.5, mats.counter, 3.58, .93, -1.4, kit);
bbox(.8, 1.9, .7, mats.steel, .5, 0, -2.55, kit); bbox(1.8, .9, .9, mats.cab, 1.9, 0, -.9, kit); box(1.95, .05, 1.05, mats.counter, 1.9, .93, -.9, kit);
// bath
bbox(1.6, .55, .8, mats.trim, 3.05, 0, 2.45, kit); bbox(.9, .8, .5, mats.cab, 1.95, 0, 2.6, kit); box(.95, .04, .55, mats.counter, 1.95, .82, 2.6, kit); bbox(.4, .45, .6, mats.trim, 2.75, 0, 1.0, kit);
// gables + roof
const roof = grp(house, 0, FY + H, 0);
const RISE = 1.9, HALF = 3.55, ang = Math.atan2(RISE, HALF), SL = Math.hypot(HALF, RISE);
function gable(x, key) { const s = new THREE.Shape(); s.moveTo(-3.1, 0); s.lineTo(3.1, 0); s.lineTo(0, RISE * 3.1 / HALF); s.closePath(); const g = new THREE.ExtrudeGeometry(s, { depth: WT, bevelEnabled: false }); g.rotateY(Math.PI / 2); const m = mesh(g, mats.siding, x - WT / 2, 0, 0, roof); uv(m, 1, 1); return part(key, m); }
gable(-4.0, 'sLeft'); gable(4.0, 'sRight');
const rB = box(8.9, .16, SL, mats.roof, 0, RISE / 2, -HALF / 2, roof); rB.rotation.x = -ang; uv(rB, 4, 2); part('back', rB, [-.5, FY + H + 2.6, -1.6]);
const rF = box(8.9, .16, SL, mats.roof, 0, RISE / 2, HALF / 2, roof); rF.rotation.x = ang; uv(rF, 4, 2); part('front', rF, [-.5, FY + H + 2.6, 1.6]);
box(9.0, .14, .22, mats.trim, 0, RISE + .04, 0, roof);
const chim = grp(roof, -2.4, 0, -1.6); const ch = box(.7, 2.6, .7, mats.brick, 0, 1.3, 0, chim); box(.85, .12, .85, mats.found, 0, 2.62, 0, chim); part('chimney', ch, [-2.4, FY + H + 3.1, -1.6]);
const flash = box(.9, .06, .9, mats.steel, 0, .55 * 1.6, 0, chim);
[3.62, -3.62].forEach(z => { box(8.9, .14, .14, mats.gutter, 0, -.05, z, roof); });
[[-4.35, 3.62], [4.35, -3.62]].forEach(([x, z]) => box(.1, H + FY, .1, mats.gutter, x, -(H + FY) / 2, z, roof));
// roof deck (x-ray for the roof)
const deck = grp(roof); deck.visible = false; const rotM = new THREE.MeshStandardMaterial({ color: 0x4f3a29, roughness: 1 });
[[rB, -1], [rF, 1]].forEach(([slab, sg]) => { const g = grp(deck); g.position.copy(slab.position); g.rotation.copy(slab.rotation); for (let i = 0; i < 4; i++) for (let j = 0; j < 2; j++) { const bad = sg > 0 && i === 1 && j === 0; box(2.15, .05, SL / 2 - .06, bad ? rotM : mats.osb, -3.3 + i * 2.2, -.1, (j ? 1 : -1) * SL / 4, g); } });
const rotPos = [-1.1, FY + H + 1.1, 2.3];

/* ---- garage ---- */
const gar = grp(ROOT, 0, 0, 0);
bbox(3.6, .15, 5.0, mats.found, 5.8, 0, -.55, gar);
const gW = grp(gar, 0, .15, 0); const GH = 2.4;
function gwall(w, d, x, z, out) { const g = new THREE.BoxGeometry(w, GH, d); g.translate(0, GH / 2, 0); const m = new THREE.Mesh(g, face(null, out)); m.position.set(x, 0, z); m.castShadow = m.receiveShadow = true; gW.add(m); return part('sGarage', m); }
const gFront = gwall(3.6, .18, 5.8, 1.9, 4); gwall(3.6, .18, 5.8, -3.0, 5); gwall(.18, 5.0, 7.55, -.55, 0);
const gd = box(2.6, 2.0, .06, mats.gdoor, 5.8, 1.15, 2.0, gar); const gdTrim = box(2.85, .1, .1, mats.trim, 5.8, 2.2, 2.0, gar);
const groof = grp(gar, 5.8, .15 + GH, -.55); const ga = Math.atan2(1.1, 2.1), gsl = Math.hypot(2.1, 1.1);
const g1 = box(gsl, .14, 5.6, mats.roof, -1.05, .55, 0, groof); g1.rotation.z = ga; uv(g1, 1.5, 3); const g2 = box(gsl, .14, 5.6, mats.roof, 1.05, .55, 0, groof); g2.rotation.z = -ga; uv(g2, 1.5, 3);
part('garageRoof', g1, [5.8, 4.0, -.5]); part('garageRoof', g2);
const gg = new THREE.Shape(); gg.moveTo(-1.8, 0); gg.lineTo(1.8, 0); gg.lineTo(0, 1.0); gg.closePath(); const gge = new THREE.ExtrudeGeometry(gg, { depth: .18, bevelEnabled: false }); const gF = mesh(gge, mats.siding, 0, 0, 2.38, groof); gF.position.z = 2.36 - 2.36 + 2.36; gF.position.set(0, 0, 2.27); const gB = mesh(gge, mats.siding, 0, 0, -2.45, groof);
// water heater (one-item renovation)
const wh = grp(gar, 6.75, .15, -2.35); const tank = mesh(new THREE.CylinderGeometry(.32, .32, 1.5, 24), new THREE.MeshStandardMaterial({ color: 0xf1f1ee, roughness: .45 }), 0, .75, 0, wh); mesh(new THREE.CylinderGeometry(.33, .33, .06, 24), mats.steel, 0, 1.5, 0, wh); [-.12, .12].forEach(x => mesh(new THREE.CylinderGeometry(.03, .03, .9, 8), new THREE.MeshStandardMaterial({ color: 0xb87333, metalness: .7, roughness: .3 }), x, 1.95, 0, wh)); box(.2, .26, .02, new THREE.MeshStandardMaterial({ color: 0xffd34d }), 0, 1.0, .33, wh);
part('wh', tank, [6.75, 2.4, -2.35]);
/* ---- yard ---- */
const yard = grp();
const patio = box(5, .08, 3, mats.ghost, 0, .05, -5.6, yard); part('patio', patio, [0, .8, -5.6]);
const patioEdge = new THREE.LineSegments(new THREE.EdgesGeometry(new THREE.BoxGeometry(5, .6, 3)), new THREE.LineDashedMaterial({ color: 0x5b8ddc, dashSize: .2, gapSize: .14 })); patioEdge.position.set(0, .3, -5.6); patioEdge.computeLineDistances(); yard.add(patioEdge);
const beds = []; [[-5, -9.6], [0, -9.8], [5, -9.6]].forEach(([x, z]) => { const b = box(3.4, .22, 1.0, mats.soil, x, .1, z, yard); beds.push(b); part('beds', b, [0, .9, -9.6]); });
const drain = box(.8, .03, 8, new THREE.MeshStandardMaterial({ color: 0x7a8f63, roughness: 1, transparent: true, opacity: .9 }), -9.9, .02, -6, yard); part('drain', drain, [-9.6, .7, -6]);
const lawn = box(7, .02, 3.2, new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: 0, depthWrite: false }), 4.5, .02, -7.2, yard); part('lawn', lawn, [4.5, .6, -7.2]);
// underground lines (x-ray for the yard)
const under = grp(); under.visible = false;
const line = (pts, c, r = .05) => { const cu = new THREE.CatmullRomCurve3(pts.map(p => new THREE.Vector3(p[0], -.12, p[1]))); mesh(new THREE.TubeGeometry(cu, 40, r, 6, false), new THREE.MeshBasicMaterial({ color: c }), 0, 0, 0, under); };
line([[-4.1, -2.0], [-7, -3], [-11, -3.4]], 0xffd23d, .07); line([[3.9, -2.8], [6, -6], [9, -10]], 0xd94a3d); line([[1, -3.2], [2.5, -6.5], [6, -8], [8, -8.4]], 0x3a8ad9, .04); line([[-1, -3.2], [-3.5, -6.5], [-7, -8]], 0x3a8ad9, .04);

/* ---- lake cabin (A-frame) ---- */
ROOT = cabinRoot;
box(26, .2, 24, mats.grass, 0, -.1, -1);
const lake = box(26, .1, 9, new THREE.MeshStandardMaterial({ color: 0x5d97bf, roughness: .15, metalness: .2 }), 0, -.02, 10);
const pineM = new THREE.MeshStandardMaterial({ color: 0x3f6b4a, roughness: .9, flatShading: true }), trunkM = new THREE.MeshStandardMaterial({ color: 0x6b4a33, roughness: 1 });
function pine(x, z, h) { const g = grp(cabinRoot, x, 0, z); mesh(new THREE.CylinderGeometry(.12, .16, h * .25, 6), trunkM, 0, h * .12, 0, g); for (let i = 0; i < 3; i++) mesh(new THREE.ConeGeometry(h * (.28 - i * .06), h * .42, 7), pineM, 0, h * (.32 + i * .2), 0, g); }
[[-9, -6, 7], [-6, -9, 8], [7, -8, 7.5], [10, -3, 6], [-11, 1, 6.5], [11, 3, 5.5], [-3, -10, 7], [3, -11, 8], [-8, 5, 5]].forEach(a => pine(...a));
const cab = grp(cabinRoot); const AH = 6.4, AW = 3.3, AD = 6.4, aL = Math.hypot(AW, AH), aA = Math.atan2(AH, AW);
const cRoofM = mats.roof.clone(); cRoofM.color = new THREE.Color(0x3d5446); cRoofM.map = mats.roof.map.clone(); cRoofM.map.needsUpdate = true; cRoofM.map.repeat.set(3, 4);
const sL = box(aL, .22, AD, cRoofM, -AW / 2, AH / 2 + .3, -1.2, cab); sL.rotation.z = aA; const sR = box(aL, .22, AD, cRoofM, AW / 2, AH / 2 + .3, -1.2, cab); sR.rotation.z = -aA;
const tri = new THREE.Shape(); tri.moveTo(-AW + .2, 0); tri.lineTo(AW - .2, 0); tri.lineTo(0, AH - .4); tri.closePath();
const woodM = new THREE.MeshStandardMaterial({ color: 0xb98652, map: T.siding(), roughness: .8 }); woodM.map.repeat.set(1, 3);
const back = mesh(new THREE.ExtrudeGeometry(tri, { depth: .15, bevelEnabled: false }), woodM, 0, .3, -4.4, cab);
const front = mesh(new THREE.ExtrudeGeometry(tri, { depth: .08, bevelEnabled: false }), mats.glass, 0, .3, 1.9, cab);
for (let i = -2; i <= 2; i++) box(.08, AH * (1 - Math.abs(i) / 3.2), .14, mats.trim, i * 1.1, .3 + AH * (1 - Math.abs(i) / 3.2) / 2, 2.0, cab);
[1.6, 3.2].forEach(y => box((AW - .2) * 2 * (1 - (y) / (AH - .4)), .08, .14, mats.trim, 0, .3 + y, 2.0, cab));
box(1.0, 2.0, .1, mats.door, 1.2, 1.3, 2.02, cab);
bbox(6.8, .3, 6.6, mats.found, 0, 0, -1.2, cab);
const deckG = grp(cabinRoot, 0, 0, 0); const deckM = mats.oak.clone(); deckM.map = mats.oak.map.clone(); deckM.map.needsUpdate = true; deckM.map.repeat.set(3, 1.5);
const deckTop = box(9, .14, 4, deckM, 0, .95, 4.2, deckG); part('deck', deckTop, [0, 2.1, 4.4]);
for (let x = -4.2; x <= 4.2; x += 2.1) for (const z of [2.4, 6.0]) box(.18, .9, .18, trunkM, x, .45, z, deckG);
for (let x = -4.4; x <= 4.4; x += .35) box(.04, .8, .04, new THREE.MeshStandardMaterial({ color: 0x222222 }), x, 1.4, 6.15, deckG);
box(9, .08, .1, new THREE.MeshStandardMaterial({ color: 0x222222 }), 0, 1.82, 6.15, deckG);
const stairs = grp(cabinRoot, 5.2, 0, 4.6); for (let i = 0; i < 4; i++) box(1.2, .14, .45, deckM, 0, .9 - i * .24, i * .45 - .2, stairs); part('stairs', stairs.children[0], [5.4, 1.6, 4.9]); stairs.children.forEach(c => part('stairs', c));
const pipe = mesh(new THREE.CylinderGeometry(.14, .14, 1.6, 12), new THREE.MeshStandardMaterial({ color: 0x1f1f1f, roughness: .5 }), 1.3, 5.3, -2.4, cab); part('stove', pipe, [1.3, 6.6, -2.4]);
const dock = box(1.6, .12, 5, deckM, -6, .15, 9, cabinRoot);

/* ---- rental condo (unit 4B) ---- */
ROOT = condoRoot;
box(30, .2, 26, mats.conc, 0, -.1, 0);
const facade = tex(256, 256, (g, w, h) => { g.fillStyle = '#d9cfc2'; g.fillRect(0, 0, w, h); g.fillStyle = '#8fb0cc'; for (let r = 0; r < 2; r++) for (let c = 0; c < 2; c++) g.fillRect(c * 128 + 24, r * 128 + 26, 80, 70); g.fillStyle = 'rgba(0,0,0,.12)'; g.fillRect(0, 126, w, 4); }, 7, 2);
const facM = new THREE.MeshStandardMaterial({ map: facade, roughness: .85 });
const FL = 2.6; bbox(14, FL * 3, 10, facM, 0, 0, 0, condoRoot);
const topLeft = bbox(8, FL, 10, facM.clone(), -3, FL * 3, 0, condoRoot); topLeft.material.map = facade.clone(); topLeft.material.map.repeat.set(4, 1); topLeft.material.map.needsUpdate = true;
const unitG = grp(condoRoot, 4, FL * 3, 0);
const uFloor = box(5.8, .1, 9.8, mats.oak.clone(), 0, .05, 0, unitG); part('unit', uFloor, [4, FL * 3 + 1.8, 0]);
const uw = M => M; const uWall = new THREE.MeshStandardMaterial({ color: 0xe9e4da, roughness: .9 });
[[5.8, .16, 0, 4.9], [5.8, .16, 0, -4.9], [.16, 9.8, 2.9, 0]].forEach(([w, d, x, z]) => bbox(w, 1.1, d, uWall, x, .1, z, unitG));
bbox(.12, 1.1, 4, uWall, -.6, .1, -2.9, unitG); bbox(2.4, 1.1, .12, uWall, 1.7, .1, -1, unitG);
const balc = box(5.8, .15, 1.6, mats.conc, 4, FL * 3, 5.8, condoRoot); for (let x = 1.4; x <= 6.8; x += .3) box(.04, .9, .04, new THREE.MeshStandardMaterial({ color: 0x333333 }), x, FL * 3 + .5, 6.55, condoRoot);
box(14.4, .3, 10.4, mats.found, 0, FL * 4 + .15, 0, condoRoot).visible = false;
[[-11, 9], [11, 9], [-11, -9], [12, -6]].forEach(([x, z]) => { const g = grp(condoRoot, x, 0, z); mesh(new THREE.CylinderGeometry(.15, .2, 1.4, 6), trunkM, 0, .7, 0, g); mesh(new THREE.IcosahedronGeometry(1.6, 0), new THREE.MeshStandardMaterial({ color: 0x7fae5c, flatShading: true }), 0, 2.3, 0, g); });
for (let x = -12; x <= 12; x += 3) box(2.2, .02, 4.6, new THREE.MeshBasicMaterial({ color: 0xffffff }), x, .02, 10.5).scale.set(.06, 1, 1);
ROOT = mainRoot;
