import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { MODELS } from './models.js';

{
const $ = s => document.querySelector(s);
const esc = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = n => '$' + Math.round(n).toLocaleString('en-US');
const clamp = (x, a = 0, b = 1) => Math.min(b, Math.max(a, x));
const lerp = (a, b, t) => a + (b - a) * t;
const ease = t => t < .5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
const buzz = p => { try { navigator.vibrate && navigator.vibrate(p); } catch (e) {} };

/* ======================================================================
   PROJECTS — a renovation can be one thing, rooms, the outside or the yard
   ====================================================================== */
const P = {
  home: { name: 'Whole home', icon: '⌂', view: 'home' },
  kitchen: {
    name: 'Kitchen remodel', icon: '🍳', scale: 'Rooms', view: 'rooms', record: 'Walls',
    stops: ['Plan', 'Quotes', 'Demo', 'Build', 'Finish'], here: 1, where: 'Quotes · demolition starts Dec 15',
    areas: { kitchen: 'Kitchen', living: 'Living room', entry: 'Hallway' },
    needs: [
      { id: 'quotes', area: 'kitchen', kind: 'compare', k: 'DUE TODAY', title: 'Compare contractor quotes', p: 'Like for like — Carter leaves out electrical, so Marco’s number is added.',
        table: [['', 'Carter + Marco', 'Ridge'], ['Estimate', '$48,200', '$55,400'], ['Electrical', '$6,800', 'incl.'], ['Haul-away', 'not in writing', 'incl.'], ['Total', '$55,000', '$55,400']], picks: ['Carter + Marco', 'Ridge'], note: 'Ridge and Marco numbers are samples.' },
      { id: 'tile', area: 'kitchen', kind: 'choose', k: 'PICK BY DEC 5', title: 'Backsplash tile', p: 'Tile takes about 3 weeks to arrive, so this sets the install date (sample).',
        opts: [{ n: 'Matte white zellige', s: '$14 / sq ft · sample', c: 'linear-gradient(135deg,#f3eee6,#e2d9cb)', vote: 'You like this' }, { n: 'Sage subway', s: '$9 / sq ft · sample', c: 'linear-gradient(135deg,#b9c5a9,#94a585)', vote: 'Your partner likes this' }] },
      { id: 'sky', area: 'entry', kind: 'choose', k: 'BEFORE FRAMING', title: 'Skylight size', p: 'The hallway is the darkest part of the house. You measured it 3 days ago.', cost: [1150, 780],
        opts: [{ n: '2 × 4 ft', s: '+$1,150 change order · sample', c: 'linear-gradient(#cfe6ff,#9cc3e8)' }, { n: '2 × 2 ft', s: '+$780 change order · sample', c: 'linear-gradient(#e2efff,#b9d4ee)' }] }
    ],
    waiting: [{ who: 'Jamie Carter', what: 'Updated estimate', when: 'Friday' }],
    money: { budget: 60000, rows: [['Best quote, like for like', 55000, '#6b4bd1']], watch: ['Electrical isn’t in Jamie’s estimate', 'Marco’s $6,800 is counted above (sample). If he quotes higher, the cushion shrinks.'] },
    agreed: [
      { area: 'living', ok: true, t: 'Keep the original oak floors', s: 'Your decision · Plan' },
      { area: 'kitchen', ok: true, t: 'Wall between kitchen and living room is not load-bearing', s: 'Jamie · site visit notes' },
      { area: 'kitchen', ok: true, t: 'Island moves 40 cm toward the window', s: 'Priya · Floor plan v3 (PDF)' },
      { area: 'kitchen', ok: true, t: 'Cabinets ship in three weeks', s: 'Jamie · your voice note today' },
      { area: null, ok: false, id: 'haul', t: 'Demolition haul-away is included', s: 'Only said out loud — nothing in writing yet', draft: 'Hi Jamie — confirming what we talked about: haul-away of all demolition debris is included in your $48,200 estimate (EST-2026-0118). Could you reply to confirm? Thanks!', to: 'Jamie' }
    ],
    photos: [{ area: 'kitchen', t: 'Kitchen back wall · before', d: 'Nov 20 · site visit (sample photo)', pos: [2.2, 1.3, -2.85], draw: 'studs' }, { area: 'entry', t: 'Hallway ceiling · measured for skylight', d: '3 days ago (sample photo)', pos: [.7, 2.5, 1.6], draw: 'sky' }],
    coming: ['ABOUT JAN 12 (SAMPLE)', 'Before drywall: walk the walls', 'Atlas reminds you the day before drywall goes up, with a checklist: each wall wide, then close on outlets, pipes and wiring.']
  },
  roof: {
    name: 'New roof', icon: '🏠', scale: 'Outside', view: 'roof', record: 'Roof deck',
    stops: ['Inspect', 'Claim & quotes', 'Tear-off', 'Install', 'Final check'], here: 1, where: 'Claim & quotes · adjuster visit pending',
    areas: { front: 'Front slope', back: 'Back slope', garageRoof: 'Garage roof', chimney: 'Chimney flashing' },
    needs: [
      { id: 'adj', area: 'front', kind: 'task', k: 'DUE TODAY', title: 'Call insurance about the roof claim', p: 'From your tasks. Ask when the adjuster can come, and whether they want your hail photos first.' },
      { id: 'shingle', area: 'back', kind: 'choose', k: 'BEFORE THE ORDER', title: 'Shingle colour', p: 'Both are the same line and price (sample). Seen from the street, the front slope shows most.',
        opts: [{ n: 'Charcoal', s: 'Same price · sample', c: 'repeating-linear-gradient(0deg,#3b3f47 0 9px,#2c2f35 9px 11px)' }, { n: 'Weathered wood', s: 'Same price · sample', c: 'repeating-linear-gradient(0deg,#7a6b5d 0 9px,#62564a 9px 11px)' }] },
      { id: 'flash', area: 'chimney', kind: 'choose', k: 'WHILE THE ROOF IS OPEN', title: 'Redo the chimney flashing too?', p: 'It’s the most common leak spot, and cheapest to fix while the shingles are off (sample prices).', cost: [650, 0],
        opts: [{ n: 'Yes, redo it', s: '+$650 · sample', c: 'linear-gradient(#b5654a,#8f4b36)' }, { n: 'Leave as is', s: '$0', c: 'linear-gradient(#d8d2c8,#bdb6aa)' }] }
    ],
    waiting: [{ who: 'Insurance', what: 'Adjuster appointment', when: 'after your call' }],
    money: { budget: 18000, rows: [['Roofer’s quote (sample)', 16800, '#6b4bd1']], covered: 'Insurance may cover $14,500 of this, less your $1,000 deductible (pending the adjuster, sample).', watch: ['Decking is the usual surprise', 'Rotted sheets found during tear-off are often billed per sheet. Get that price in writing before work starts.'] },
    agreed: [
      { area: 'front', ok: true, t: 'Hail damage on the front slope', s: 'Your photos · 12 pictures' },
      { area: null, ok: false, id: 'tear', t: 'Full tear-off down to the deck — no layover', s: 'Said by the roofer · not in writing', draft: 'Hi — confirming from our call: the job is a full tear-off down to the roof deck, not a layover. Could you add that line to the quote? Thanks!', to: 'the roofer' },
      { area: null, ok: false, id: 'deck', t: 'Price per sheet for any rotted decking', s: 'Not discussed yet', draft: 'Hi — before we sign, could you put the price per sheet for replacing any rotted decking into the quote? Thanks!', to: 'the roofer' }
    ],
    photos: [{ area: 'front', t: 'Front slope · hail hits', d: 'Your photo · Nov 2 (sample)', pos: [-1.5, 4.0, 1.6], draw: 'hail' }],
    coming: ['DURING TEAR-OFF', 'Photograph the bare deck', 'With the shingles off you can see the deck once. Atlas reminds you that morning, then reminds you to register the shingle warranty after install.']
  },
  siding: {
    name: 'New siding', icon: '🧱', scale: 'Outside', view: 'siding', record: 'Under siding',
    stops: ['Pick', 'Quotes', 'Remove', 'Wrap & install', 'Trim & paint'], here: 0, where: 'Pick · materials and colour',
    areas: { sFront: 'Front', sLeft: 'Left side', sBack: 'Back', sGarage: 'Garage' },
    needs: [
      { id: 'mat', area: 'sFront', kind: 'choose', k: 'FIRST', title: 'Material', p: 'Fiber cement costs more and lasts longer; vinyl is cheaper and lighter (sample prices for this house).', cost: [0, 0],
        opts: [{ n: 'Fiber cement', s: '≈ $21,000 · sample', c: 'repeating-linear-gradient(0deg,#cfd6c7 0 12px,#b9c2b0 12px 14px)' }, { n: 'Vinyl', s: '≈ $14,500 · sample', c: 'repeating-linear-gradient(0deg,#e6e8e3 0 12px,#d2d5cf 12px 14px)' }] },
      { id: 'col', area: 'sLeft', kind: 'choose', k: 'WITH THE MATERIAL', title: 'Colour', p: 'Tap the house to see it on every side.',
        opts: [{ n: 'Sage', s: 'Pairs with the oak door', c: '#9fae8f', paint: 0x9fae8f }, { n: 'Warm white', s: 'Brightest from the street', c: '#efe8dc', paint: 0xefe8dc }] },
      { id: 'lead', area: null, kind: 'task', k: 'BEFORE ANY QUOTE', title: 'Check the year the house was built', p: 'If it’s before 1978, the old paint may contain lead — hire a crew that is lead-safe certified.' }
    ],
    waiting: [],
    money: { budget: 25000, rows: [], watch: ['No quotes yet', 'When quotes arrive, Atlas lines them up like for like: wrap, trim, flashing and haul-away.'] },
    agreed: [{ area: null, ok: false, id: 'wrap', t: 'House wrap and window flashing included', s: 'Ask every bidder', draft: 'Hi — could your quote list house wrap and flashing at every window and door as included items? Thanks!', to: 'each bidder' }],
    photos: [],
    coming: ['WHEN THE OLD SIDING COMES OFF', 'Photograph the sheathing on every side', 'It’s the only time you’ll see rot, gaps or old wiring holes. Atlas pins each photo to its side of the house.']
  },
  yard: {
    name: 'Garden redesign', icon: '🌿', scale: 'Yard', view: 'yard', record: 'Underground',
    stops: ['Design', 'Quotes', 'Call 811 & dig', 'Build', 'Plant'], here: 0, where: 'Design · native planting, a small patio, drainage',
    areas: { patio: 'Patio', beds: 'Garden beds', drain: 'Fence drainage', lawn: 'Back lawn' },
    needs: [
      { id: 'patio', area: 'patio', kind: 'choose', k: 'DESIGN', title: 'Patio surface', p: 'Pavers can be lifted for repairs; poured concrete is cheaper to start (sample prices).', cost: [0, 0],
        opts: [{ n: 'Pavers', s: '≈ $4,200 · sample', c: 'repeating-linear-gradient(90deg,#c8b8a2 0 22px,#a8977f 22px 24px)' }, { n: 'Concrete', s: '≈ $3,100 · sample', c: 'linear-gradient(#d4d1cb,#bab6ae)' }] },
      { id: 'drain', area: 'drain', kind: 'choose', k: 'DESIGN', title: 'Water along the fence', p: 'Your notes say it pools there after rain.',
        opts: [{ n: 'French drain', s: '≈ $1,600 · sample', c: 'repeating-linear-gradient(90deg,#9a9488 0 6px,#7d776c 6px 9px)' }, { n: 'Regrade the soil', s: '≈ $700 · sample', c: 'linear-gradient(#8b6b4b,#6e543a)' }] },
      { id: '811', area: 'lawn', kind: 'task', k: 'BEFORE ANY DIGGING', title: 'Call 811 to mark buried lines', p: 'It’s free. Give them a few days before the dig date — Atlas will set the reminder.' }
    ],
    waiting: [],
    money: { budget: 8000, rows: [], watch: ['Plants are the easy part to trim', 'If patio and drainage run over, the native planting can be phased into spring.'] },
    agreed: [{ area: null, ok: true, t: 'Native planting, a small patio and better drainage along the fence', s: 'Your project notes' },
      { area: 'lawn', ok: false, id: 'sprink', t: 'Sprinkler lines repaired if the crew cuts them', s: 'Not in writing', draft: 'Hi — could the quote say that any sprinkler lines cut during the work will be repaired as part of the job? Thanks!', to: 'the landscaper' }],
    photos: [{ area: 'lawn', t: 'Sprinkler line · back lawn', d: 'Dug up last spring (sample photo)', pos: [3.2, .1, -7.5], draw: 'sprinkler' }],
    coming: ['AFTER YOU CALL 811', 'Photograph the locate marks', 'The paint and flags fade in days. Atlas pins your photos to the yard, so you know where the gas and power lines run — for good.']
  },
  heater: {
    name: 'Water heater', icon: '🔥', scale: 'One item', view: 'item', record: 'Label',
    stops: ['Choose', 'Buy', 'Install', 'Done'], here: 0, where: 'Choose · the old tank is from 2011',
    areas: { wh: 'Water heater' },
    needs: [
      { id: 'type', area: 'wh', kind: 'choose', k: 'CHOOSE', title: 'Tank or tankless', p: 'Tankless may need a bigger gas line or vent — ask before you compare price (sample prices).', cost: [0, 0],
        opts: [{ n: '50-gal tank', s: '≈ $1,900 installed · sample', c: 'linear-gradient(#f2f2f2,#d8d8d8)' }, { n: 'Tankless', s: '≈ $3,800 installed · sample', c: 'linear-gradient(#e8e4dc,#c8c2b6)' }] },
      { id: 'permit', area: 'wh', kind: 'task', k: 'BEFORE INSTALL', title: 'Check whether your city needs a permit', p: 'Many cities do for water heaters. Atlas can look up your city’s page if you ask.' }
    ],
    waiting: [],
    money: { budget: 2500, rows: [], watch: ['Haul-away and fittings', 'Ask that removing the old tank and new fittings are in the price.'] },
    agreed: [{ area: 'wh', ok: false, id: 'haulwh', t: 'Old tank hauled away', s: 'Not in writing', draft: 'Hi — could you confirm the price includes removing and hauling away the old water heater? Thanks!', to: 'the plumber' }],
    photos: [{ area: 'wh', t: 'Old tank label', d: 'Read by Atlas: installed 2011, 40 gal (sample)', pos: [6.6, 1.3, -2.3], draw: 'plate' }],
    coming: ['AFTER INSTALL', 'Snap the new label', 'Atlas reads the model, serial and warranty, and starts a yearly “flush the tank” reminder as its own Orbit journey.']
  }
};
const ORDER = ['home', 'kitchen', 'roof', 'siding', 'yard', 'heater'];
const S = { p: 'home', area: null, tab: 'needs', xray: false, picked: {} };
const cur = () => P[S.p];

/* ======================================================================
   3D — the whole property: house, garage, yard, street
   ====================================================================== */
const canvas = $('#c');
const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
renderer.setPixelRatio(Math.min(devicePixelRatio, 2)); renderer.shadowMap.enabled = true; renderer.shadowMap.type = THREE.PCFSoftShadowMap;
renderer.toneMapping = THREE.ACESFilmicToneMapping; renderer.toneMappingExposure = 1.05;
const scene = new THREE.Scene(); scene.fog = new THREE.Fog(0xf3efe8, 38, 70);
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
function mesh(g, m, x = 0, y = 0, z = 0, p) { const o = new THREE.Mesh(g, m); o.position.set(x, y, z); o.castShadow = o.receiveShadow = true; (p || scene).add(o); return o; }
function box(w, h, d, m, x, y, z, p) { return mesh(new THREE.BoxGeometry(w, h, d), m, x, y, z, p); }
function bbox(w, h, d, m, x, y, z, p) { const g = new THREE.BoxGeometry(w, h, d); g.translate(0, h / 2, 0); return mesh(g, m, x, y, z, p); }
function grp(p, x = 0, y = 0, z = 0) { const g = new THREE.Group(); g.position.set(x, y, z); (p || scene).add(g); return g; }
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
const gar = grp(scene, 0, 0, 0);
bbox(3.6, .15, 5.0, mats.found, 5.8, 0, -.55, gar);
const gW = grp(gar, 0, .15, 0); const GH = 2.4;
function gwall(w, d, x, z, out) { const g = new THREE.BoxGeometry(w, GH, d); g.translate(0, GH / 2, 0); const m = new THREE.Mesh(g, face(null, out)); m.position.set(x, 0, z); m.castShadow = m.receiveShadow = true; gW.add(m); return part('sGarage', m); }
const gFront = gwall(3.6, .18, 5.8, 1.9, 4); gwall(3.6, .18, 5.8, -3.0, 5); gwall(.18, 5.0, 7.55, -.55, 0);
const gd = box(2.6, 2.0, .06, mats.gdoor, 5.8, 1.15, 2.0, gar); box(2.85, .1, .1, mats.trim, 5.8, 2.2, 2.0, gar);
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

/* ---- x-ray extras for rooms and siding ---- */
const studs = grp(house, 0, FY, 0); studs.visible = false;
const studM = new THREE.MeshBasicMaterial({ color: 0xd9b77e });
for (let x = -3.85; x <= 3.9; x += .4) box(.05, H, .08, studM, x, H / 2, -3.0, studs);
box(5, .035, .05, new THREE.MeshBasicMaterial({ color: 0xd94a3d }), 1.4, 1.0, -2.98, studs); box(.035, .8, .05, new THREE.MeshBasicMaterial({ color: 0xd94a3d }), 3.0, .6, -2.98, studs); box(3.6, .035, .05, new THREE.MeshBasicMaterial({ color: 0x2f6fd9 }), 2.0, 1.25, -2.98, studs); box(.07, H, .07, new THREE.MeshBasicMaterial({ color: 0x9aa3ad }), 3.4, H / 2, -2.96, studs);
const sheath = grp(house, 0, FY, 0); sheath.visible = false;
[[8.0, .06, 0, 2.86], [8.0, .06, 0, -2.86], [.06, 5.8, -3.86, 0]].forEach(([w, d, x, z]) => bbox(w, H, d, mats.osb, x, 0, z, sheath));

/* ---- furniture and yard models ---- */
const GLB = {}; const loader = new GLTFLoader();
function b64(s) { const bin = atob(s); const u = new Uint8Array(bin.length); for (let i = 0; i < bin.length; i++) u[i] = bin.charCodeAt(i); return u.buffer; }
async function loadAll() { await Promise.all(Object.keys(MODELS).map(k => new Promise((res, rej) => loader.parse(b64(MODELS[k]), './', g => { GLB[k] = g; res(); }, rej)))); }
function inst(name, size, by = 'h', parent) { const o = GLB[name].scene.clone(true); o.updateMatrixWorld(true); const b = new THREE.Box3().setFromObject(o); const d = b.getSize(new THREE.Vector3()); o.scale.multiplyScalar(size / (by === 'h' ? d.y : d.x)); o.updateMatrixWorld(true); const b2 = new THREE.Box3().setFromObject(o); const c = b2.getCenter(new THREE.Vector3()); o.position.set(-c.x, -b2.min.y, -c.z); o.traverse(m => { if (m.isMesh) m.castShadow = m.receiveShadow = true; }); const w = new THREE.Group(); w.add(o); (parent || scene).add(w); return w; }
function put(n, s, x, y, z, ry = 0, by = 'h', parent) { const o = inst(n, s, by, parent); o.position.set(x, y, z); o.rotation.y = ry; return o; }
function furnish() {
  const F = grp(house, 0, FY + .04, 0);
  put('rug_rectangle_stripes_A', 2.2, -2.0, 0, 1.3, 0, 'w', F); put('couch_pillows', .8, -2.0, 0, .35, 0, 'h', F); put('table_low', .34, -2.0, 0, 1.3, 0, 'h', F); put('armchair', .78, -3.2, 0, 2.2, 2.3, 'h', F); put('lamp_standing', 1.4, -3.5, 0, .3, 0, 'h', F); put('shelf_B_large_decorated', .5, -.9, 0, 2.6, Math.PI, 'h', F);
  put('bed_double_A', .7, -2.0, 0, -1.7, 0, 'h', F); put('cabinet_small', .6, -.6, 0, -2.6, 0, 'h', F); put('cactus_medium_A', .45, .8, 0, 2.3, 0, 'h', F);
  [[1.5, -.3], [2.3, -.3]].forEach(([x, z]) => put('chair_A_wood', .85, x, 0, z, Math.PI, 'h', F));
  put('car_stationwagon', 1.5, 5.8, 0, 5.2, Math.PI, 'h');
  [[-8.5, 6.5, 'tree_oak', 3.6], [9.2, 6, 'tree_fat', 3.0], [-9, -8.6, 'tree_fat', 3.4], [8.8, -9.5, 'tree_oak', 3.8], [-6.5, -10.3, 'tree_oak', 3.2]].forEach(([x, z, n, h]) => put(n, h, x, 0, z));
  [[-3.4, 3.7], [-1.8, 3.7], [2.6, 3.6], [3.4, 3.6], [-4.6, 2.0], [-4.6, 0]].forEach(([x, z]) => put('bush', .6, x, 0, z));
  beds.forEach(b => { for (let i = -1; i <= 1; i++) put('bush', .55, b.position.x + i * 1.0, .2, b.position.z); });
  for (let x = -10.5; x <= 10.5; x += 1.5) put('fence', 1.5, x, 0, -10.6, 0, 'w');
  for (let z = -9.8; z <= -3.8; z += 1.5) { put('fence', 1.5, -11.2, 0, z, Math.PI / 2, 'w'); put('fence', 1.5, 11.2, 0, z, Math.PI / 2, 'w'); }
  put('bench', .6, 6.5, 0, -6.2, -Math.PI / 2);
}

/* ---- tags, project badges, photo pins ---- */
const AREA_TAG = { sFront: [0, 2.6, 3.4], sLeft: [-4.5, 2.6, 0], sBack: [0, 2.6, -3.4], sGarage: [7.9, 2.2, -.5] };
const HOME_TAG = { kitchen: [2.6, FY + H + 2.3, -.6], roof: [-1.2, FY + H + 2.4, .6], siding: [-4.5, 1.8, 1.8], yard: [0, 1.1, -6.6], heater: [6.75, 3.3, -2.35] };
const tagsEl = $('#tags'); let tagEls = []; let pinEls = [];
function clearTags() { tagEls.forEach(t => t.el.remove()); pinEls.forEach(t => t.el.remove()); tagEls = []; pinEls = []; }
function buildTags() {
  clearTags(); const p = cur();
  if (S.p === 'home') { ORDER.slice(1).forEach(k => { const q = P[k]; const n = q.needs.length; const el = document.createElement('button'); el.className = 'tag'; el.innerHTML = `<span class="n ${n ? '' : 'z'}">${n || '✓'}</span><span>${q.icon} ${esc(q.name)}</span>`; el.addEventListener('click', () => go(k)); tagsEl.appendChild(el); tagEls.push({ el, pos: HOME_TAG[k] }); }); return; }
  Object.entries(p.areas).forEach(([k, name]) => { const n = p.needs.filter(x => x.area === k).length; const el = document.createElement('button'); el.className = 'tag' + (S.area === k ? ' sel' : ''); el.innerHTML = `<span class="n ${n ? '' : 'z'}">${n || '✓'}</span><span>${esc(name)}</span>`; el.addEventListener('click', () => focusArea(k)); tagsEl.appendChild(el); tagEls.push({ el, pos: AREA_TAG[k] || TAGPOS[k] || [0, 2, 0] }); });
  p.photos.forEach(ph => { const el = document.createElement('button'); el.className = 'pin'; el.textContent = '📷'; el.setAttribute('aria-label', ph.t); el.addEventListener('click', () => openPhoto(ph)); tagsEl.appendChild(el); pinEls.push({ el, pos: ph.pos }); });
  if (S.p === 'roof') { const el = document.createElement('button'); el.className = 'pin'; el.style.background = '#7a4a2a'; el.textContent = '!'; el.setAttribute('aria-label', 'Possible soft decking'); el.addEventListener('click', () => toast('Sample: the roofer flagged a soft spot here — that’s what the per-sheet price covers')); tagsEl.appendChild(el); pinEls.push({ el, pos: rotPos }); }
}
const pv = new THREE.Vector3();
function place(el, pos) { pv.set(...pos).project(camera); if (pv.z > 1) { el.style.display = 'none'; return; } const r = canvas.getBoundingClientRect(); el.style.display = ''; el.style.left = ((pv.x + 1) / 2 * r.width) + 'px'; el.style.top = ((1 - pv.y) / 2 * r.height) + 'px'; }
function placeAll() { tagEls.forEach(t => S.xray && S.p !== 'home' ? (t.el.style.display = 'none') : place(t.el, t.pos)); pinEls.forEach(t => S.xray ? place(t.el, t.pos) : (t.el.style.display = 'none')); }

/* ---- highlights ---- */
const glow = new THREE.Group(); scene.add(glow); const glowMat = new THREE.LineBasicMaterial({ color: 0xff9a3c, transparent: true, opacity: .9, depthTest: false });
function highlight() {
  glow.clear(); if (S.p === 'home') return; const p = cur();
  Object.keys(p.areas).forEach(k => { if (S.area && S.area !== k) return; (PARTS[k] || []).forEach(m => { m.updateMatrixWorld(true); const e = new THREE.LineSegments(new THREE.EdgesGeometry(m.geometry, 30), glowMat); e.matrixAutoUpdate = false; e.matrix.copy(m.matrixWorld); e.renderOrder = 5; glow.add(e); }); });
}

/* ---- views: what the house shows for each kind of renovation ---- */
const CAM = {
  home: [[16, 15, 21], [1, .5, -1.5]], rooms: [[7.5, 14, 11], [.6, 0, .2]], roof: [[-10, 11, 13], [0, 3.2, 0]], siding: [[-12, 5.5, 12.5], [.5, 1.8, 0]], yard: [[11, 11, -18], [0, 0, -6.5]], item: [[6.2, 3.6, 6.5], [6.6, 1.0, -2.0]]
};
const AREACAM = { kitchen: [[6, 9, 7], [2, 0, -1.2]], living: [[-6, 9, 9], [-2, 0, 1.2]], entry: [[3, 8, 9], [.7, 0, 1.7]], front: [[-4, 9, 14], [-.5, 3.4, 1.5]], back: [[-4, 10, -13], [-.5, 3.4, -1.5]], garageRoof: [[12, 9, 6], [5.8, 3, -.5]], chimney: [[-8, 9, -9], [-2.4, 4.4, -1.6]], sFront: [[-3, 3.5, 12], [0, 1.6, 3]], sLeft: [[-12, 3.5, 2], [-4, 1.6, 0]], sBack: [[-3, 4, -12], [0, 1.6, -3]], sGarage: [[13, 3.5, 3], [7.5, 1.4, -.5]], patio: [[6, 7, -15], [0, 0, -5.6]], beds: [[4, 6, -3], [0, 0, -9.6]], drain: [[-3, 6, -14], [-9.8, 0, -6]], lawn: [[11, 6, -11], [4.5, 0, -7.2]], wh: [[6.4, 2.8, 4.5], [6.75, 1.0, -2.35]] };
function applyView() {
  const v = S.p === 'home' ? 'home' : cur().view;
  const cut = v === 'rooms';
  extWalls.scale.y = inWalls.scale.y = cut ? 1.15 / H : 1; extWalls.position.y = inWalls.position.y = FY;
  roof.visible = !cut; wins.visible = !cut; porch.children[porch.children.length - 1].visible = !cut;
  groof.visible = v !== 'item'; gd.visible = v !== 'item'; gFront.scale.y = v === 'item' ? .35 : 1;
  yard.visible = true;
  const x = S.xray;
  mats.siding.opacity = x && (v === 'siding' || v === 'rooms') ? .22 : 1; mats.siding.depthWrite = !(x && (v === 'siding' || v === 'rooms'));
  mats.paint.transparent = x && v === 'rooms'; mats.paint.opacity = x && v === 'rooms' ? .22 : 1; mats.paint.depthWrite = !(x && v === 'rooms'); mats.paint.needsUpdate = true;
  mats.roof.opacity = x && v === 'roof' ? .18 : 1; mats.roof.depthWrite = !(x && v === 'roof');
  mats.grass.opacity = x && v === 'yard' ? .45 : 1; mats.grass.depthWrite = !(x && v === 'yard');
  studs.visible = x && v === 'rooms'; sheath.visible = x && v === 'siding'; deck.visible = x && v === 'roof'; under.visible = x && v === 'yard';
  highlight();
}

/* ---- camera ---- */
let fly = null;
function flyTo(c, d = 1.1) { const t1 = new THREE.Vector3(...c[1]); const off = new THREE.Vector3(...c[0]).sub(t1); off.multiplyScalar(clamp(.72 / Math.max(.3, camera.aspect), 1, 1.75)); fly = { p0: camera.position.clone(), t0: controls.target.clone(), p1: t1.clone().add(off), t1, s: performance.now(), d: d * 1000 }; }
function resize() { const r = $('#map').getBoundingClientRect(); if (!r.width) return; renderer.setSize(r.width, r.height, false); camera.aspect = r.width / r.height; camera.fov = r.width < r.height ? 46 : 34; const sh = $('#sheet').offsetHeight, top = $('.hd').offsetHeight - 10; camera.setViewOffset(r.width, r.height, 0, (sh - top) / 2, r.width, r.height); camera.updateProjectionMatrix(); }
addEventListener('resize', resize); new ResizeObserver(resize).observe($('#sheet')); new ResizeObserver(resize).observe($('.hd'));

/* ---- tapping the property ---- */
const ray = new THREE.Raycaster(), ndc = new THREE.Vector2(); let down = null;
canvas.addEventListener('pointerdown', e => { down = [e.clientX, e.clientY]; fly = null; });
canvas.addEventListener('pointerup', e => {
  if (!down || Math.hypot(e.clientX - down[0], e.clientY - down[1]) > 8) return; const r = canvas.getBoundingClientRect(); ndc.set((e.clientX - r.left) / r.width * 2 - 1, -((e.clientY - r.top) / r.height) * 2 + 1); ray.setFromCamera(ndc, camera);
  if (S.p === 'home') { const map = { kitchen: ['kitchen', 'living', 'entry', 'bed', 'bath'], roof: ['front', 'back', 'garageRoof', 'chimney'], siding: ['sFront', 'sLeft', 'sBack', 'sRight', 'sGarage'], yard: ['patio', 'beds', 'drain', 'lawn'], heater: ['wh'] }; const objs = []; Object.entries(map).forEach(([pk, ks]) => ks.forEach(k => (PARTS[k] || []).forEach(m => { m.userData.proj = pk; objs.push(m); }))); const h = ray.intersectObjects(objs); if (h.length) go(h[0].object.userData.proj); return; }
  const keys = Object.keys(cur().areas); const objs = []; keys.forEach(k => (PARTS[k] || []).forEach(m => { m.userData.ak = k; objs.push(m); })); const h = ray.intersectObjects(objs); if (h.length) focusArea(h[0].object.userData.ak); else if (S.area) focusArea(S.area);
});
function focusArea(k) { S.area = S.area === k ? null : k; buzz(8); flyTo(S.area ? (AREACAM[S.area] || CAM[cur().view]) : CAM[cur().view]); buildTags(); highlight(); render(); }
function go(k) { S.p = k; S.area = null; S.tab = 'needs'; if (S.xray) { S.xray = false; $('#xray').classList.remove('on'); } buzz(10); applyView(); flyTo(CAM[k === 'home' ? 'home' : P[k].view], 1.3); buildTags(); render(); }
$('#xray').addEventListener('click', () => { if (S.p === 'home') { toast('Pick a project first — x-ray shows what’s hidden for that job'); return; } S.xray = !S.xray; $('#xray').classList.toggle('on', S.xray); if (S.xray) S.tab = 'record'; else if (S.tab === 'record') S.tab = 'needs'; applyView(); render(); buzz(10); });

/* ---- sheet ---- */
const inArea = x => !S.area || x.area === S.area || x.area === null;
function moneyOf(p) { const rows = p.money.rows.map(r => ({ l: r[0], v: r[1], c: r[2] })); let change = 0; p.needs.concat(p.done || []).forEach(n => { if (n.cost && S.picked[S.p + n.id] !== undefined) change += n.cost[S.picked[S.p + n.id]]; }); const used = rows.reduce((s, r) => s + r.v, 0) + change; return { rows, change, left: p.money.budget - used, budget: p.money.budget }; }
function render() {
  const p = cur();
  $('#projects').innerHTML = ORDER.map(k => { const q = P[k]; const n = k === 'home' ? ORDER.slice(1).reduce((s, x) => s + P[x].needs.length, 0) : q.needs.length; return `<button class="${S.p === k ? 'on' : ''}" data-p="${k}">${q.icon} ${esc(q.name)}<span class="c ${n ? '' : 'z'}">${n || '✓'}</span></button>`; }).join('');
  $('#projects').querySelectorAll('button').forEach(b => b.addEventListener('click', () => go(b.dataset.p)));
  if (S.p === 'home') {
    $('#ptitle').textContent = 'Your home'; $('#where').textContent = '5 projects · from one water heater to the whole yard';
    $('#trail').innerHTML = ''; $('#traill').innerHTML = '';
    $('#scope').textContent = 'Everything going on'; $('#scopeSub').textContent = 'Tap a project — on the house or below';
    $('#four').style.display = 'none';
    $('#sbody').innerHTML = ORDER.slice(1).map(k => { const q = P[k]; const n = q.needs.length; return `<button class="proj" data-p="${k}"><span class="ic">${q.icon}</span><span><b>${esc(q.name)}<span class="scope">${esc(q.scale)}</span></b><small>Stop ${q.here + 1} of ${q.stops.length} · ${esc(q.where)}</small></span><span class="c ${n ? '' : 'z'}">${n || '✓'}</span></button>`; }).join('') + `<p class="mini">A renovation can be one thing, a room, the outside or the yard. Atlas picks the scale from what you tell it, and the map zooms to fit.</p>`;
    $('#sbody').querySelectorAll('[data-p]').forEach(b => b.addEventListener('click', () => go(b.dataset.p)));
    return;
  }
  $('#four').style.display = '';
  $('#ptitle').textContent = p.name; $('#where').textContent = `Stop ${p.here + 1} of ${p.stops.length} · ${p.where}`;
  $('#trail').innerHTML = p.stops.map((s, i) => `${i ? `<b class="${i <= p.here ? 'done' : ''}"></b>` : ''}<i class="${i < p.here ? 'done' : i === p.here ? 'here' : ''}"></i>`).join('');
  $('#traill').innerHTML = p.stops.map(s => `<span>${esc(s)}</span>`).join('');
  const m = moneyOf(p); const needs = p.needs.filter(inArea); const ag = p.agreed.filter(inArea); const ph = p.photos.filter(inArea);
  $('#scope').textContent = S.area ? p.areas[S.area] : p.name; $('#scopeSub').textContent = S.area ? `${needs.length} need you · tap it again for the whole ${p.scale === 'One item' ? 'job' : 'project'}` : `${p.scale} · tap a part to focus · ＋ adds anything`;
  $('#four').innerHTML = [['needs', needs.length, 'Needs you', 'needs'], ['money', money(m.left), 'Left', ''], ['agreed', `${ag.filter(a => a.ok).length}✓ ${ag.filter(a => !a.ok).length}?`, 'Agreed', ''], ['record', ph.length, p.record, '']].map(([k, v, l, c]) => `<button class="${c} ${S.tab === k ? 'on' : ''}" data-k="${k}"><b>${esc(v)}</b><span>${esc(l)}</span></button>`).join('');
  $('#four').querySelectorAll('button').forEach(b => b.addEventListener('click', () => { const k = b.dataset.k; if ((k === 'record') !== S.xray) { S.xray = k === 'record'; $('#xray').classList.toggle('on', S.xray); applyView(); } S.tab = k; render(); }));
  const B = $('#sbody');
  if (S.tab === 'needs') B.innerHTML = needsHTML(p, needs);
  if (S.tab === 'money') B.innerHTML = moneyHTML(p, m);
  if (S.tab === 'agreed') B.innerHTML = `<div class="card">${ag.map(a => `<div class="agree"><span class="ic ${a.ok ? 'ok' : 'q'}">${a.ok ? '✓' : '?'}</span><div style="flex:1"><b>${esc(a.t)}</b><small>${esc(a.s)}</small>${!a.ok && a.draft ? `<div class="row"><button class="btn g" data-off="${a.id}">Make it official</button></div>` : ''}</div></div>`).join('') || '<p>Nothing yet.</p>'}</div><p class="mini">✓ in writing, with where it came from · ? said but not written down</p>`;
  if (S.tab === 'record') B.innerHTML = `<div class="card"><div class="k v">${esc(p.record.toUpperCase())}</div><h3>${ph.length ? `${ph.length} photo${ph.length > 1 ? 's' : ''} pinned where taken` : 'Nothing hidden recorded yet'}</h3><p>Tap a 📷 on the map. Years from now, this is how you know what’s ${S.p === 'yard' ? 'under the lawn' : S.p === 'heater' ? 'on the label' : 'behind it'}.</p></div><div class="card"><div class="k">COMING · ${esc(p.coming[0])}</div><h3>${esc(p.coming[1])}</h3><p>${esc(p.coming[2])}</p></div>`;
  bind(B, p);
}
function needsHTML(p, list) {
  let h = list.map(n => {
    if (n.kind === 'choose') return `<div class="card"><div class="k">${esc(n.k)}${n.area ? ' · ' + esc(p.areas[n.area]) : ''}</div><h3>${esc(n.title)}</h3><p>${esc(n.p)}</p><div class="opts">${n.opts.map((o, i) => `<button class="opt" data-ch="${n.id}" data-i="${i}"><div class="sw" style="background:${o.c}"></div><b>${esc(o.n)}</b><small>${esc(o.s)}</small>${o.vote ? `<div class="vote">${esc(o.vote)}</div>` : ''}</button>`).join('')}</div></div>`;
    if (n.kind === 'compare') return `<div class="card"><div class="k">${esc(n.k)} · ${esc(p.areas[n.area])}</div><h3>${esc(n.title)}</h3><p>${esc(n.p)}</p><table class="cmp">${n.table.map((r, i) => `<tr>${r.map(c => i === 0 || i === n.table.length - 1 ? `<th>${esc(c)}</th>` : `<td>${esc(c)}</td>`).join('')}</tr>`).join('')}</table><div class="mini">${esc(n.note)}</div><div class="row">${n.picks.map((x, i) => `<button class="btn ${i ? 'g' : ''}" data-cmp="${n.id}" data-i="${i}">Go with ${esc(x)}</button>`).join('')}</div></div>`;
    return `<div class="card"><div class="k">${esc(n.k)}</div><h3>${esc(n.title)}</h3><p>${esc(n.p)}</p><div class="row"><button class="btn g" data-done="${n.id}">Done</button></div></div>`;
  }).join('');
  if (!list.length) h = `<div class="card"><div class="k g">ALL CLEAR</div><h3>Nothing needs you here</h3><p>New things show up when they need a decision or a date.</p></div>`;
  if (!S.area && p.waiting.length) h += `<div class="card"><div class="k v">WAITING ON OTHERS</div>${p.waiting.map(w => `<h3>${esc(w.what)}</h3><p>${esc(w.who)} · ${esc(w.when)}. Atlas nudges you if it doesn’t arrive.</p>`).join('')}</div>`;
  return h;
}
function moneyHTML(p, m) {
  const seg = (v, c) => v > 0 ? `<div style="width:${v / m.budget * 100}%;background:${c}"></div>` : '';
  return `<div class="card"><div class="k v">BUDGET ${money(m.budget)}${p.money.rows.length ? '' : ' · SAMPLE'}</div><h3>${money(m.left)} ${p.money.rows.length ? 'left for surprises' : 'to spend — no quotes yet'}</h3><div class="mbar">${m.rows.map(r => seg(r.v, r.c)).join('')}${seg(m.change, '#d9822b')}${seg(Math.max(0, m.left), '#e9e4f3')}</div>
  <div class="legend">${m.rows.map(r => `<span><i class="dot" style="background:${r.c}"></i>${esc(r.l)}</span><span>${money(r.v)}</span>`).join('')}<span><i class="dot" style="background:#d9822b"></i>Change orders &amp; add-ons</span><span>${money(m.change)}</span><span><i class="dot" style="background:#e9e4f3"></i>Left</span><span>${money(m.left)}</span></div>
  ${p.money.covered ? `<p class="mini">${esc(p.money.covered)}</p>` : ''}</div><div class="card"><div class="k">WATCH</div><h3>${esc(p.money.watch[0])}</h3><p>${esc(p.money.watch[1])}</p></div>`;
}
function bind(B, p) {
  B.querySelectorAll('[data-ch]').forEach(b => b.addEventListener('click', () => { const n = p.needs.find(x => x.id === b.dataset.ch), i = +b.dataset.i; const o = n.opts[i]; S.picked[S.p + n.id] = i; p.needs = p.needs.filter(x => x !== n); (p.done = p.done || []).push(n); p.agreed.unshift({ area: n.area, ok: true, t: `${n.title}: ${o.n}`, s: 'Your decision · just now' });
    const before = { siding: mats.siding.color.getHex(), roof: mats.roof.color.getHex(), patio: patio.material };
    if (o.paint) mats.siding.color.setHex(o.paint); if (n.id === 'shingle') mats.roof.color.setHex(i ? 0x8a7b6c : 0x4a4f58); if (n.id === 'patio') patio.material = i ? mats.conc.clone() : new THREE.MeshStandardMaterial({ map: T.brick(), color: 0xe8dccb, roughness: .9 });
    buzz([12, 20, 30]); toast(`${n.title}: ${o.n}${n.cost && n.cost[i] ? ' · added to money' : ''}`, () => { p.needs.push(n); p.done = p.done.filter(x => x !== n); p.agreed.shift(); delete S.picked[S.p + n.id]; mats.siding.color.setHex(before.siding); mats.roof.color.setHex(before.roof); patio.material = before.patio; buildTags(); render(); }); buildTags(); render(); }));
  B.querySelectorAll('[data-cmp]').forEach(b => b.addEventListener('click', () => { const n = p.needs.find(x => x.id === b.dataset.cmp), i = +b.dataset.i; p.needs = p.needs.filter(x => x !== n); p.agreed.unshift({ area: n.area, ok: true, t: `Going with ${n.picks[i]}`, s: 'Your decision · compared like for like' }); buzz([15, 25, 40]); toast('Quotes compared · decision recorded', () => { p.needs.unshift(n); p.agreed.shift(); buildTags(); render(); }); buildTags(); render(); }));
  B.querySelectorAll('[data-done]').forEach(b => b.addEventListener('click', () => { const n = p.needs.find(x => x.id === b.dataset.done); p.needs = p.needs.filter(x => x !== n); buzz(12); toast(`${n.title} · done`, () => { p.needs.push(n); buildTags(); render(); }); buildTags(); render(); }));
  B.querySelectorAll('[data-off]').forEach(b => b.addEventListener('click', () => { const a = p.agreed.find(x => x.id === b.dataset.off); modal(`<h3>Make it official</h3><p class="mini" style="margin:0">Atlas drafted this from your notes. You send it — Atlas never sends for you.</p><div class="draft">${esc(a.draft)}</div><div class="row"><button class="btn" id="cp">Copy &amp; mark as asked</button><button class="btn g" id="cx">Cancel</button></div>`, () => { $('#cp').addEventListener('click', () => { try { navigator.clipboard.writeText(a.draft); } catch (e) {} a.s = 'Asked in writing just now · waiting for the reply'; p.waiting.push({ who: a.to[0].toUpperCase() + a.to.slice(1), what: 'Confirm: ' + a.t.toLowerCase(), when: 'this week' }); closeModal(); toast(`Copied · paste it to ${a.to}`); render(); }); $('#cx').addEventListener('click', closeModal); }); }));
}

/* ---- photos (drawn stand-ins) ---- */
function openPhoto(ph) {
  const c = document.createElement('canvas'); c.width = 640; c.height = 400; const g = c.getContext('2d'); const d = ph.draw;
  if (d === 'studs') { g.fillStyle = '#e9dfcf'; g.fillRect(0, 0, 640, 400); g.fillStyle = '#d9b77e'; for (let x = 20; x < 640; x += 90) g.fillRect(x, 0, 22, 400); g.strokeStyle = '#d94a3d'; g.lineWidth = 6; g.beginPath(); g.moveTo(0, 230); g.lineTo(480, 230); g.lineTo(480, 320); g.stroke(); g.strokeStyle = '#2f6fd9'; g.beginPath(); g.moveTo(80, 150); g.lineTo(640, 150); g.stroke(); g.fillStyle = '#9aa3ad'; g.fillRect(560, 120, 18, 280); }
  if (d === 'sky') { g.fillStyle = '#efe9df'; g.fillRect(0, 0, 640, 400); g.fillStyle = '#cfe6ff'; g.fillRect(200, 100, 240, 180); g.setLineDash([10, 8]); g.strokeStyle = '#1d1830'; g.lineWidth = 3; g.strokeRect(200, 100, 240, 180); g.setLineDash([]); g.fillStyle = '#1d1830'; g.font = 'bold 26px Roboto,sans-serif'; g.fillText('48″ × 24″ between rafters', 170, 330); }
  if (d === 'hail') { g.fillStyle = '#4b5059'; g.fillRect(0, 0, 640, 400); for (let r = 0; r < 12; r++) for (let k = 0; k < 12; k++) { g.fillStyle = (r + k) % 2 ? '#454a52' : '#50555e'; g.fillRect(k * 56 - (r % 2) * 28, r * 34, 54, 32); } g.fillStyle = 'rgba(210,200,180,.85)'; [[120, 90], [300, 160], [420, 70], [210, 260], [520, 300], [90, 330]].forEach(([x, y]) => { g.beginPath(); g.arc(x, y, 9, 0, 7); g.fill(); }); g.strokeStyle = '#ffd34d'; g.lineWidth = 3; g.strokeRect(80, 50, 80, 80); }
  if (d === 'sprinkler') { g.fillStyle = '#6e5038'; g.fillRect(0, 0, 640, 400); g.fillStyle = '#8fbf6a'; g.fillRect(0, 0, 640, 80); g.strokeStyle = '#2b2b2b'; g.lineWidth = 14; g.beginPath(); g.moveTo(0, 240); g.lineTo(640, 210); g.stroke(); g.fillStyle = '#3a8ad9'; g.fillRect(300, 190, 40, 50); }
  if (d === 'plate') { g.fillStyle = '#f1f1ee'; g.fillRect(0, 0, 640, 400); g.fillStyle = '#ffd34d'; g.fillRect(150, 60, 340, 280); g.fillStyle = '#1d1830'; g.font = 'bold 28px Roboto,sans-serif'; g.fillText('WATER HEATER', 200, 110); g.font = '22px monospace'; ['MODEL  XG40T06', 'SERIAL 1103A0042', 'MFG    MAR 2011', 'CAP    40 GAL', 'GAS    NATURAL'].forEach((t, i) => g.fillText(t, 190, 160 + i * 34)); }
  modal(`<h3>${esc(ph.t)}</h3><div class="mini" style="margin:0">${esc(ph.d)}</div><img class="photo" alt="${esc(ph.t)}" src="${c.toDataURL()}"><div class="row"><button class="btn g" id="cx">Close</button></div>`, () => $('#cx').addEventListener('click', closeModal));
}

/* ---- capture: no typing ---- */
const DEMO = {
  home: ['Jamie: cabinets moved to Dec 18', 'kitchen'],
  kitchen: ['Jamie: cabinet delivery moved to Dec 18', 'kitchen'], roof: ['Roofer: we can start the week of Jan 5', 'front'], siding: ['Neighbor: our siding guy was great — Ridge Exteriors', 'sFront'], yard: ['Landscaper: pavers come in charcoal or sand', 'patio'], heater: ['Plumber: I can swap it Thursday morning', 'wh']
};
$('#fab').addEventListener('click', () => modal(`<h3>Add anything</h3><p class="mini" style="margin:0">Atlas works out which project and part it belongs to — the water heater, the roof, the kitchen — and you can move it after.</p><div class="cap"><button id="cS"><span>💬</span>Shared text</button><button id="cP"><span>📷</span>Photo</button><button id="cV"><span>🎙</span>Voice</button></div><p class="mini">Demo: each button plays an example.</p>`, () => {
  $('#cS').addEventListener('click', () => { closeModal(); const [txt, area] = DEMO[S.p]; const target = S.p === 'home' ? 'kitchen' : S.p; const q = P[target]; q.agreed.unshift({ area, ok: true, t: txt.split(': ')[1], s: txt.split(':')[0] + ' · text you shared just now' }); buzz(12); toast(`Filed to ${q.name} · ${q.areas[area]}`, () => { q.agreed.shift(); render(); }); if (S.p !== 'home') S.tab = 'agreed'; render(); });
  $('#cV').addEventListener('click', () => { closeModal(); const target = S.p === 'home' ? 'roof' : S.p; const q = P[target]; const area = Object.keys(q.areas)[0]; const n = { id: 'v' + Date.now(), area, kind: 'task', k: 'FROM YOUR VOICE NOTE', title: target === 'roof' ? 'Send the adjuster your hail photos' : 'Ask for that in writing', p: 'Heard in your voice note just now.' }; q.needs.push(n); buzz(12); toast(`Voice note → a to-do in ${q.name}`, () => { q.needs = q.needs.filter(x => x !== n); buildTags(); render(); }); buildTags(); render(); });
  $('#cP').addEventListener('click', () => { closeModal(); if (S.p === 'home') { toast('Atlas saw a roof in the photo → filed to New roof'); P.roof.photos.push({ area: 'front', t: 'Front slope · new photo', d: 'Just now (sample)', pos: [1.5, 4.0, 1.7], draw: 'hail' }); render(); return; } const q = cur(); const area = Object.keys(q.areas)[0]; const pos = (AREA_TAG[area] || TAGPOS[area] || [0, 2, 0]).slice(); pos[0] += .8; const ph = { area, t: `${q.areas[area]} · new photo`, d: 'Just now (sample photo)', pos, draw: { rooms: 'studs', roof: 'hail', siding: 'studs', yard: 'sprinkler', item: 'plate' }[q.view] }; q.photos.push(ph); buildTags(); buzz(12); toast(`Photo pinned to ${q.areas[area]}`, () => { q.photos = q.photos.filter(x => x !== ph); buildTags(); render(); }); if (!S.xray) $('#xray').click(); else render(); });
}));

/* ---- modal, toast ---- */
function modal(html, fn) { $('#mcard').innerHTML = html; $('#modal').classList.add('show'); fn && fn(); }
function closeModal() { $('#modal').classList.remove('show'); }
$('#modal').addEventListener('click', e => { if (e.target.id === 'modal') closeModal(); });
let tt; function toast(msg, undo) { const t = $('#toast'); t.innerHTML = `<span>${esc(msg)}</span>${undo ? '<button id="un">Undo</button>' : ''}`; t.classList.add('show'); if (undo) $('#un').addEventListener('click', () => { undo(); t.classList.remove('show'); }); clearTimeout(tt); tt = setTimeout(() => t.classList.remove('show'), 4200); }
$('#grab').addEventListener('click', () => $('#sheet').classList.toggle('tall'));

/* ---- router + loop ---- */
function route() { const h = location.hash === '#journeys' ? 'ten' : 'reno'; $('#reno').classList.toggle('on', h === 'reno'); $('#ten').classList.toggle('on', h === 'ten'); $('#t1').classList.toggle('on', h === 'reno'); $('#t2').classList.toggle('on', h === 'ten'); if (h === 'reno') resize(); }
addEventListener('hashchange', route);
function frame() {
  if (fly) { const k = ease(clamp((performance.now() - fly.s) / fly.d)); camera.position.lerpVectors(fly.p0, fly.p1, k); controls.target.lerpVectors(fly.t0, fly.t1, k); if (k >= 1) fly = null; }
  controls.update(); glowMat.opacity = .55 + Math.sin(performance.now() / 300) * .35;
  placeAll(); renderer.render(scene, camera); requestAnimationFrame(frame);
}

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
renderTen(); route(); applyView(); buildTags(); render();
camera.position.set(26, 24, 32); flyTo(CAM.home, 1.6);
loadAll().then(furnish).catch(e => console.error(e));
frame();
}
