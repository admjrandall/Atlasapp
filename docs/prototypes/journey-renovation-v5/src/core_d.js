
/* ======================================================================
   your property as objects — lot, house, roof, windows, doors, garage,
   driveway, fences, trees, bushes, beds, sheds… each one tappable,
   measurable, movable, with its own history and projects.
   Feet in a lot frame: x across (− left, + right, seen from the street),
   z back from the front lot line (0 = sidewalk, negative = toward the back).
   ====================================================================== */
const FT = .3048;
const CUST = {};
const custOf = h => (HOMES[h] && HOMES[h].model === 'custom') ? CUST[h] : null;
function camOf(v) { const c = custOf(S.home); if (c) { const p = cur(); if (p && p.home === S.home) { const k = Object.keys(p.areas || {})[0]; if (k && c.cam[k]) return c.cam[k]; } return S.plan ? c.cam.plan : c.cam.home; } return CAM[v] || CAM.main; }
function areaCam(k) { const c = custOf(S.home); if (c) return c.cam[k] || null; return AREACAM[k]; }
const SRC = { record: 'Public record', outline: 'Outline data', you: 'You', est: 'Estimate', photo: 'Your photo', app: 'Atlas', ai: '✦ AI' };
const WOULD = 'In the app these come from Montana’s public data: lot lines from the Montana Cadastral parcels, house and garage outlines from the State Library’s lidar building footprints, and facts (living area, stories, roof, other buildings) from the Department of Revenue record card.';

/* ---- object types ---- */
const OT = {
  lot: { n: 'Lot', ic: '📐', place: 'Yard', g: 'Yard' }, house: { n: 'House', ic: '🏠', place: 'Outside', g: 'House' }, roof: { n: 'Roof', ic: '🔺', place: 'Outside', g: 'House' },
  window: { n: 'Window', ic: '🪟', place: 'Outside', g: 'House' }, door: { n: 'Door', ic: '🚪', place: 'Outside', g: 'House' },
  garage: { n: 'Garage', ic: '🚗', place: 'Outside', g: 'Garage & drive' }, drive: { n: 'Concrete', ic: '⬜', place: 'Yard', g: 'Garage & drive' }, walk: { n: 'Walkway', ic: '⬜', place: 'Yard', g: 'Garage & drive' },
  fence: { n: 'Fence', ic: '🧱', place: 'Yard', g: 'Yard' }, lawn: { n: 'Lawn', ic: '🌱', place: 'Yard', g: 'Yard' }, tree: { n: 'Tree', ic: '🌳', place: 'Yard', g: 'Trees & plants' }, bush: { n: 'Bush', ic: '🌿', place: 'Yard', g: 'Trees & plants' },
  bed: { n: 'Garden bed', ic: '🌷', place: 'Yard', g: 'Trees & plants' }, shed: { n: 'Shed', ic: '🛖', place: 'Yard', g: 'Yard' }, patio: { n: 'Patio', ic: '◻️', place: 'Yard', g: 'Yard' }, deck: { n: 'Deck', ic: '🪵', place: 'Yard', g: 'Yard' },
  pool: { n: 'Pool', ic: '🏊', place: 'Yard', g: 'Yard' }, photo: { n: 'From a photo', ic: '🖼', place: 'Yard', g: 'Trees & plants' }
};
const GROUPS = ['House', 'Garage & drive', 'Trees & plants', 'Yard'];
const RECT = new Set(['house', 'garage', 'drive', 'walk', 'lawn', 'bed', 'shed', 'patio', 'deck', 'pool']);
const isRect = o => RECT.has(o.type);
let oseq = 0; const oid = t => t[0] + (Date.now() % 1e6).toString(36) + (oseq++);

/* ---- what Atlas can suggest for each kind of thing: steps fit the job; effects change the model when it’s done ---- */
const SUG = {
  tree: [['Remove', '🪓', ['Get quotes', 'Call 811 (for the stump)', 'Remove', 'Stump & clean-up'], 'remove'], ['Trim / prune', '✂️', ['Get quotes', 'Trim', 'Clean up']], ['Check its health', '🩺', ['Arborist visit', 'Treat', 'Follow up']]],
  bush: [['Remove', '🪓', ['Plan', 'Dig out', 'Clean up'], 'remove'], ['Trim', '✂️', ['Trim', 'Clean up']], ['Replace', '🌱', ['Pick a plant', 'Remove the old one', 'Plant']]],
  photo: [['Remove', '🪓', ['Plan', 'Remove', 'Clean up'], 'remove'], ['Trim / care', '✂️', ['Do it', 'Clean up']]],
  house: [['New siding', '🧱', ['Pick', 'Quotes', 'Remove', 'Wrap & install', 'Trim & paint']], ['Exterior paint', '🎨', ['Pick colours', 'Quotes', 'Prep', 'Paint']], ['Room addition', '📐', ['Design', 'Permit', 'Quotes', 'Build', 'Finish']]],
  roof: [['New roof', '🏠', ['Inspect', 'Quotes', 'Tear-off', 'Install', 'Final check']], ['Roof repair', '🔧', ['Inspect', 'Quote', 'Repair']], ['Gutters', '💧', ['Measure', 'Quotes', 'Install']]],
  window: [['Replace this window', '🪟', ['Measure', 'Quotes', 'Order', 'Install']], ['Repair / reseal', '🔧', ['Inspect', 'Repair']]],
  door: [['Replace the door', '🚪', ['Measure', 'Pick', 'Order', 'Install']], ['Paint the door', '🎨', ['Pick a colour', 'Paint']]],
  garage: [['New garage door', '🚗', ['Measure', 'Quotes', 'Install']], ['Re-roof the garage', '🏠', ['Quotes', 'Tear-off', 'Install']], ['Paint the garage', '🎨', ['Pick colours', 'Prep', 'Paint']], ['Tear it down', '🪓', ['Permit', 'Quotes', 'Demolish', 'Clean up'], 'remove']],
  drive: [['Replace the concrete', '🏗', ['Quotes', 'Tear out', 'Pour', 'Cure']], ['Repair cracks / seal', '🔧', ['Clean', 'Repair', 'Seal']], ['Widen it', '↔️', ['Plan', 'Permit', 'Quotes', 'Pour']]],
  walk: [['Replace the walkway', '🏗', ['Quotes', 'Tear out', 'Pour']], ['Repair', '🔧', ['Repair']]],
  fence: [['Replace the fence', '🧱', ['Measure', 'Quotes', 'Remove the old', 'Install']], ['Repair a section', '🔧', ['Inspect', 'Repair']], ['Remove it', '🪓', ['Plan', 'Remove', 'Clean up'], 'remove']],
  lawn: [['Landscaping', '🌿', ['Design', 'Quotes', 'Call 811 & dig', 'Build', 'Plant']], ['Sprinkler system', '💦', ['Design', 'Call 811', 'Install', 'Test']], ['New sod / reseed', '🌱', ['Prep', 'Sod or seed', 'Water in']]],
  bed: [['Replant', '🌷', ['Plan', 'Clear', 'Plant']], ['Remove', '🪓', ['Remove', 'Grass over'], 'remove']],
  shed: [['Repair / paint', '🔧', ['Inspect', 'Repair', 'Paint']], ['Remove', '🪓', ['Empty it', 'Demolish', 'Clean up'], 'remove']],
  patio: [['Replace', '🏗', ['Design', 'Quotes', 'Build']], ['Remove', '🪓', ['Remove', 'Grass over'], 'remove']],
  deck: [['Rebuild', '🪚', ['Measure', 'Materials', 'Build', 'Seal']], ['Seal / stain', '🎨', ['Clean', 'Seal']], ['Remove', '🪓', ['Remove', 'Clean up'], 'remove']],
  pool: [['Resurface', '🏊', ['Drain', 'Resurface', 'Refill']], ['Remove', '🪓', ['Permit', 'Demolish', 'Fill & grade'], 'remove']],
  lot: [['Survey the lot lines', '📐', ['Order a survey', 'Mark the pins']]]
};
const PLACE_OF_T = t => (OT[t] || OT.photo).place;

/* ---- the lookup returns objects, the way the real one would ---- */
function objsFor816() {
  const H = { id: 'house', type: 'house', name: 'House', x: -9, z: -39.5, w: 34, d: 27, stories: 1, roof: 'gable', entry: 'stoop', planter: true, hist: [{ y: 1975, t: 'Built', s: 'Public record' }] };
  const win = (name, wall, at, w, h, sill = 3) => ({ id: oid('w'), type: 'window', name, host: 'house', wall, at, w, h, sill });
  return [
    { id: 'lot', type: 'lot', name: 'Lot', w: 62, d: 121 }, H,
    { id: 'roof', type: 'roof', name: 'Roof', host: 'house', hist: [] },
    win('Living room window', 'front', 5.8, 7, 4.2), win('Front window', 'front', 21.1, 6, 4.2), win('Small front window', 'front', 29.6, 3.2, 3.3, 3.5),
    win('Back window (left)', 'back', 6.8, 3.6, 3), win('Back window (right)', 'back', 27.2, 3.6, 3), win('Left side window', 'left', 17.5, 3.3, 3), win('Right side window', 'right', 8, 3.3, 3),
    { id: oid('d'), type: 'door', name: 'Front door', host: 'house', wall: 'front', at: 13.6, w: 3, h: 6.8, kind: 'front' },
    { id: oid('d'), type: 'door', name: 'Side door', host: 'house', wall: 'right', at: 21, w: 3, h: 6.6, kind: 'side' },
    { id: 'garage', type: 'garage', name: 'Garage', x: 17, z: -69, w: 24, d: 24, cars: 2, attached: false },
    { id: oid('c'), type: 'drive', name: 'Driveway', x: 19, z: -28.5, w: 18, d: 57 },
    { id: oid('c'), type: 'drive', name: 'Parking pad', x: 17, z: -52, w: 26, d: 10 },
    { id: oid('c'), type: 'walk', name: 'Front walk', x: -1.2, z: -21, w: 22.4, d: 3.3 },
    { id: oid('l'), type: 'lawn', name: 'Front yard', x: -10.5, z: -13, w: 41, d: 25 },
    { id: oid('l'), type: 'lawn', name: 'Back yard', x: 0, z: -89, w: 61, d: 63 },
    { id: oid('f'), type: 'fence', name: 'Fence · right side', x1: 31, z1: -40, x2: 31, z2: -121, h: 6 },
    { id: oid('f'), type: 'fence', name: 'Fence · back', x1: 31, z1: -121, x2: -31, z2: -121, h: 6 },
    { id: oid('f'), type: 'fence', name: 'Fence · left side', x1: -31, z1: -121, x2: -31, z2: -58, h: 6 },
    { id: oid('t'), type: 'tree', name: 'Big tree in the back', kind: 'oak', x: -14, z: -92, h: 30, spread: 24 },
    { id: oid('t'), type: 'tree', name: 'Evergreen behind the house', kind: 'pine', x: -6, z: -64, h: 24, spread: 11 },
    { id: oid('b'), type: 'bush', name: 'Bush by the steps', kind: 'shrub', x: -21, z: -24, h: 3, spread: 4 }
  ];
}
const LOOKUP = {
  '816 maywood': () => ({
    address: '816 Maywood Dr, Billings, MT 59102', found: true, area: 'West End · homes built 1973–1977 · lots about 7,500 sq ft',
    records: { built: 1975, beds: 2, baths: 1.5, lot: 7492 }, objs: objsFor816(), colors: { siding: '#ebeae5', trim: '#4b5763', roof: '#7c7f81' },
    facts: [['Built', '1975', 'record'], ['Bedrooms / baths', '2 · 1.5', 'record'], ['Lot', '7,492 sq ft', 'record'], ['House outline', '≈ 34 × 27 ft · one story', 'est'], ['Roof', 'Gable, low pitch — check this', 'est'], ['Garage', 'Separate, 2-car, behind on the right', 'you'], ['Driveway', 'Along the right side to the garage, with a pad', 'you'], ['Fence', 'White vinyl, right side and back yard', 'you'], ['Front', 'Stoop with railings, planter bed', 'you']]
  })
};
function objsFromBox(w, d, gar) {
  const hx = gar.type !== 'none' && gar.side === 'left' ? 6 : -6, hz = -25 - d / 2; const o = [{ id: 'lot', type: 'lot', name: 'Lot', w: 62, d: 121 }, { id: 'house', type: 'house', name: 'House', x: hx, z: hz, w, d, stories: 1, roof: 'gable', entry: 'stoop', planter: false, hist: [] }, { id: 'roof', type: 'roof', name: 'Roof', host: 'house', hist: [] }];
  [.2, .65, .86].forEach((f, i) => o.push({ id: oid('w'), type: 'window', name: i ? 'Front window' : 'Living room window', host: 'house', wall: 'front', at: f * w, w: i === 2 ? 3.2 : 6, h: 4, sill: 3 }));
  [.25, .75].forEach(f => o.push({ id: oid('w'), type: 'window', name: 'Back window', host: 'house', wall: 'back', at: f * w, w: 3.6, h: 3, sill: 3 }));
  o.push({ id: oid('d'), type: 'door', name: 'Front door', host: 'house', wall: 'front', at: .42 * w, w: 3, h: 6.8, kind: 'front' });
  if (gar.type !== 'none') { const gw = gar.cars === 1 ? 13 : 24, att = gar.type === 'attached'; const gx = att ? (gar.side === 'left' ? hx - w / 2 - gw / 2 : hx + w / 2 + gw / 2) : (gar.side === 'left' ? -31 + gw / 2 + 2 : 31 - gw / 2 - 2); const gz = att ? hz + d / 2 - 12 : hz - d / 2 - 16; o.push({ id: 'garage', type: 'garage', name: 'Garage', x: gx, z: gz, w: gw, d: 24, cars: gar.cars, attached: att }); const dw = gar.cars === 1 ? 11 : 18; o.push({ id: oid('c'), type: 'drive', name: 'Driveway', x: gx, z: (gz + 12) / 2, w: dw, d: -(gz + 12) }); }
  o.push({ id: oid('l'), type: 'lawn', name: 'Back yard', x: 0, z: -95, w: 61, d: 50 }, { id: oid('t'), type: 'tree', name: 'Tree', kind: 'oak', x: -20, z: -95, h: 26, spread: 20 });
  return o;
}
function lookupAddress(q) {
  const k = q.toLowerCase().replace(/\b(drive|dr\.?)\b/g, '').replace(/[^\w ]/g, ' ').replace(/\s+/g, ' ').trim();
  const hit = Object.keys(LOOKUP).find(x => k.startsWith(x) || k.includes(x)); if (hit) return LOOKUP[hit]();
  return { address: q.trim(), found: false, records: null, objs: null, facts: [] };
}
const O = (h, id) => (HOMES[h].objs || []).find(o => o.id === id);
const lotOf = h => O(h, 'lot') || { w: 62, d: 121 };

/* ---- 3D from the objects ---- */
const spriteTex = {};
function sidingMat(base, w, h) { const m = base.clone(); m.map = base.map.clone(); m.map.needsUpdate = true; m.map.repeat.set(Math.max(1, w / 2.2), Math.max(1, h / 1.1)); return m; }
function pendingRemove(h, id) { return PROJ.some(p => p.home === h && p.status !== 'done' && p.effect && p.effect.obj === id && p.effect.kind === 'remove'); }
function buildCustom(h) {
  const H0 = HOMES[h]; const objs = H0.objs; const col = H0.colors || { siding: '#d9cbb0', trim: '#f4f1ea', roof: '#6b5a4c' };
  if (CUST[h]) { const old = CUST[h]; Object.entries(old.P).forEach(([k, l]) => { PARTS[k] = (PARTS[k] || []).filter(m => !l.includes(m)); }); scene.remove(old.g); old.g.traverse(o => { if (o.geometry) o.geometry.dispose(); }); }
  const g = new THREE.Group(); scene.add(g); g.visible = false; const prev = ROOT;
  const L = lotOf(h); const X = x => x * FT, Z = z => (z + L.d / 2) * FT;
  const M = { siding: [], trim: [], roof: [] }; const P = {}, OG = {}, tag = {}, cam = {};
  const sid = mats.siding.clone(); sid.transparent = false;
  const sm = (w, hh) => { const m = sidingMat(sid, w, hh); m.color.set(col.siding); M.siding.push(m); return m; };
  const trimD = new THREE.MeshStandardMaterial({ color: col.trim, roughness: .6 }); M.trim.push(trimD);
  const white = new THREE.MeshStandardMaterial({ color: 0xf6f5f0, roughness: .6 });
  const roofM = mats.roof.clone(); roofM.map = mats.roof.map.clone(); roofM.map.needsUpdate = true; roofM.map.repeat.set(6, 4); roofM.color.set(col.roof); roofM.transparent = false; roofM.side = THREE.DoubleSide; M.roof.push(roofM);
  const vinyl = new THREE.MeshStandardMaterial({ color: 0xf7f7f4, roughness: .5 }), block = new THREE.MeshStandardMaterial({ color: 0x9a6a55, roughness: .9 }), bark = new THREE.MeshStandardMaterial({ color: 0x6b4f36 });
  const lawnM = mats.grass.clone(); lawnM.transparent = false; lawnM.map = mats.grass.map.clone(); lawnM.map.needsUpdate = true; lawnM.map.repeat.set(4, 4); lawnM.color.set(0xeefae4);
  const grp2 = id => { const og = new THREE.Group(); og.userData.oid = id; g.add(og); OG[id] = og; ROOT = og; return og; };
  const pk = (id, m) => { m.userData.oid = id; (P[id] = P[id] || []).push(m); (PARTS[id] = PARTS[id] || []).push(m); return m; };
  ROOT = g;
  // ground, street, sidewalk, lot lines
  box((L.w + 70) * FT, .2, (L.d + 20) * FT, mats.grass, 0, -.1, Z(-L.d / 2) - 2 * FT);
  box((L.w + 70) * FT, .12, 5 * FT, mats.conc, 0, .01, Z(2.5)); box((L.w + 70) * FT, .1, 28 * FT, mats.asphalt, 0, -.04, Z(19));
  for (let i = -45; i < 46; i += 12) box(1.4, .02, .14, new THREE.MeshBasicMaterial({ color: 0xf5f0d8 }), i * FT, .02, Z(19));
  const ll = new THREE.LineSegments(new THREE.EdgesGeometry(new THREE.BoxGeometry(L.w * FT, .02, L.d * FT)), new THREE.LineDashedMaterial({ color: 0x7fa86a, dashSize: .5, gapSize: .35 })); ll.position.set(0, .07, Z(-L.d / 2)); ll.computeLineDistances(); g.add(ll);
  const house = objs.find(o => o.type === 'house');
  const FYc = .4; const wallH = house ? (house.stories === 2 ? 5.0 : house.stories === 1.5 ? 3.5 : 2.6) : 2.6;
  const wallInfo = {};
  objs.forEach(o => {
    if (o.type === 'lot' || o.type === 'window' || o.type === 'door' || o.type === 'roof') return;
    grp2(o.id);
    if (o.type === 'lawn') { pk(o.id, box(o.w * FT, .02, o.d * FT, lawnM, X(o.x), .012, Z(o.z))); tag[o.id] = [X(o.x), .5, Z(o.z)]; }
    else if (o.type === 'drive' || o.type === 'walk' || o.type === 'patio') { pk(o.id, box(o.w * FT, .12, o.d * FT, o.type === 'patio' ? mats.found : mats.conc, X(o.x), .03 + (o.type === 'walk' ? .005 : 0), Z(o.z))); tag[o.id] = [X(o.x), .6, Z(o.z)]; }
    else if (o.type === 'bed') { pk(o.id, box(o.w * FT, .25, o.d * FT, mats.soil, X(o.x), .12, Z(o.z))); for (let i = 0; i < Math.max(2, Math.round(o.w / 4)); i++) pk(o.id, mesh(new THREE.IcosahedronGeometry(.35, 1), new THREE.MeshStandardMaterial({ color: [0x5d8a45, 0xc46b8a, 0xe0b84a][i % 3] }), X(o.x - o.w / 2 + (i + .5) * o.w / Math.max(2, Math.round(o.w / 4))), .45, Z(o.z))); tag[o.id] = [X(o.x), 1, Z(o.z)]; }
    else if (o.type === 'deck') { pk(o.id, box(o.w * FT, .25, o.d * FT, mats.oak, X(o.x), .5, Z(o.z))); tag[o.id] = [X(o.x), 1.2, Z(o.z)]; }
    else if (o.type === 'pool') { pk(o.id, box(o.w * FT, .1, o.d * FT, new THREE.MeshStandardMaterial({ color: 0x59b6e6, roughness: .2 }), X(o.x), .04, Z(o.z))); pk(o.id, box((o.w + 3) * FT, .06, (o.d + 3) * FT, mats.conc, X(o.x), .02, Z(o.z))); tag[o.id] = [X(o.x), .8, Z(o.z)]; }
    else if (o.type === 'shed') { const sh = (o.h || 8) * FT; pk(o.id, bbox(o.w * FT, sh, o.d * FT, sm(o.w * FT, sh), X(o.x), 0, Z(o.z))); const r = box(o.w * FT + .3, .1, o.d * FT + .3, roofM, X(o.x), sh + .05, Z(o.z)); r.rotation.x = .12; pk(o.id, r); tag[o.id] = [X(o.x), sh + .8, Z(o.z)]; }
    else if (o.type === 'fence') { const len = Math.hypot(o.x2 - o.x1, o.z2 - o.z1) * FT, hh = (o.h || 6) * FT; const m = box(len, hh, .06, vinyl, X((o.x1 + o.x2) / 2), hh / 2, Z((o.z1 + o.z2) / 2)); m.rotation.y = -Math.atan2(o.z2 - o.z1, o.x2 - o.x1); pk(o.id, m); const n = Math.max(1, Math.round(len / 1.8)); for (let i = 0; i <= n; i++) { const t = i / n; pk(o.id, box(.12, hh + .1, .12, vinyl, X(o.x1 + (o.x2 - o.x1) * t), (hh + .1) / 2, Z(o.z1 + (o.z2 - o.z1) * t))); } tag[o.id] = [X((o.x1 + o.x2) / 2), hh + .6, Z((o.z1 + o.z2) / 2)]; }
    else if (o.type === 'tree' || o.type === 'bush') { const H1 = o.h * FT, S1 = (o.spread || o.h * .6) * FT; const leaf = new THREE.MeshStandardMaterial({ color: o.kind === 'pine' ? 0x2f5e3a : o.type === 'bush' ? 0x4d7f3e : o.kind === 'maple' ? 0x6f8f3a : o.kind === 'ornamental' ? 0x8c5a7a : 0x4f7d3c, roughness: .9 }); if (o.type === 'tree') pk(o.id, box(Math.max(.2, H1 * .03), H1 * .45, Math.max(.2, H1 * .03), bark, X(o.x), H1 * .22, Z(o.z))); if (o.kind === 'pine') pk(o.id, mesh(new THREE.ConeGeometry(S1 / 2, H1 * .8, 9), leaf, X(o.x), H1 * .58, Z(o.z))); else if (o.type === 'bush') pk(o.id, mesh(new THREE.IcosahedronGeometry(S1 / 2, 1), leaf, X(o.x), H1 * .45, Z(o.z))).scale.y = H1 / S1; else { pk(o.id, mesh(new THREE.IcosahedronGeometry(S1 * .42, 1), leaf, X(o.x), H1 * .68, Z(o.z))); pk(o.id, mesh(new THREE.IcosahedronGeometry(S1 * .3, 1), leaf, X(o.x) + S1 * .25, H1 * .58, Z(o.z) + S1 * .15)); } tag[o.id] = [X(o.x), H1 + .5, Z(o.z)]; }
    else if (o.type === 'photo') { if (!spriteTex[o.id]) { const t = new THREE.TextureLoader().load(o.img); t.colorSpace = THREE.SRGBColorSpace; spriteTex[o.id] = t; } const sp = new THREE.Sprite(new THREE.SpriteMaterial({ map: spriteTex[o.id], transparent: true })); sp.center.set(.5, 0); sp.scale.set((o.w || 3) * FT, o.h * FT, 1); sp.position.set(X(o.x), 0, Z(o.z)); OG[o.id].add(sp); pk(o.id, sp); tag[o.id] = [X(o.x), o.h * FT + .4, Z(o.z)]; }
    else if (o.type === 'house' || o.type === 'garage') {
      const W = o.w * FT, D = o.d * FT, cx = X(o.x), cz = Z(o.z), isH = o.type === 'house', wh = isH ? wallH : 2.6, base = isH ? FYc : .15;
      bbox(W + .2, base, D + .2, isH ? mats.found : mats.conc, cx, 0, cz);
      const wall = (w, x, z, ry) => { const m = box(w, wh, .2, sm(w, wh), x, base + wh / 2, z); m.rotation.y = ry; pk(o.id, m); };
      wall(W, cx, cz + D / 2, 0); wall(W, cx, cz - D / 2, 0); wall(D, cx - W / 2, cz, Math.PI / 2); wall(D, cx + W / 2, cz, Math.PI / 2);
      [[-1, 1], [1, 1], [-1, -1], [1, -1]].forEach(([a, b]) => pk(o.id, box(.18, wh, .18, white, cx + a * W / 2, base + wh / 2, cz + b * D / 2)));
      wallInfo[o.id] = { cx, cz, W, D, base, wh };
      const top = base + wh, ov = .5;
      if (isH) {
        ROOT = grp2('roof'); const roofO = objs.find(r => r.type === 'roof') || { id: 'roof' };
        const pitch = o.roof === 'flat' ? 0 : .3, rise = (D / 2 + ov) * pitch;
        if (o.roof === 'flat') pk('roof', box(W + ov * 2, .25, D + ov * 2, roofM, cx, top + .12, cz));
        else if (o.roof === 'hip') { const a = [cx - W / 2 - ov, top, cz + D / 2 + ov], b = [cx + W / 2 + ov, top, cz + D / 2 + ov], c = [cx + W / 2 + ov, top, cz - D / 2 - ov], d = [cx - W / 2 - ov, top, cz - D / 2 - ov]; const rx = Math.max(.2, W / 2 - D / 2), r1 = [cx - rx, top + rise, cz], r2 = [cx + rx, top + rise, cz]; const face = pts => { const pos = []; for (let i = 1; i < pts.length - 1; i++) pos.push(...pts[0], ...pts[i], ...pts[i + 1]); const geo = new THREE.BufferGeometry(); geo.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3)); const uvA = []; for (let i = 0; i < pos.length; i += 3) uvA.push(pos[i] / 1.6, pos[i + 2] / 1.6 + pos[i + 1] / 1.6); geo.setAttribute('uv', new THREE.Float32BufferAttribute(uvA, 2)); geo.computeVertexNormals(); const m = new THREE.Mesh(geo, roofM); m.castShadow = m.receiveShadow = true; OG.roof.add(m); pk('roof', m); }; face([a, b, r2, r1]); face([c, d, r1, r2]); face([d, a, r1]); face([b, c, r2]); }
        else { const SL = Math.hypot(D / 2 + ov, rise), ang = Math.atan2(rise, D / 2 + ov); const rF = box(W + ov * 2, .14, SL, roofM, cx, top + rise / 2, cz + (D / 2 + ov) / 2); rF.rotation.x = ang; pk('roof', rF); const rB = box(W + ov * 2, .14, SL, roofM, cx, top + rise / 2, cz - (D / 2 + ov) / 2); rB.rotation.x = -ang; pk('roof', rB); box(W + ov * 2 + .1, .1, .14, roofM, cx, top + rise + .02, cz); ROOT = OG[o.id]; const sh = new THREE.Shape(); sh.moveTo(-D / 2, 0); sh.lineTo(D / 2, 0); sh.lineTo(0, rise * D / 2 / (D / 2 + ov)); sh.closePath(); [-W / 2, W / 2].forEach(x => { const m = new THREE.Mesh(new THREE.ExtrudeGeometry(sh, { depth: .2, bevelEnabled: false }), sm(D, rise)); m.rotation.y = Math.PI / 2; m.position.set(cx + x - .1, top, cz); m.castShadow = true; OG[o.id].add(m); pk(o.id, m); }); }
        ROOT = OG.roof; [cz + D / 2 + ov, cz - D / 2 - ov].forEach(z => pk('roof', box(W + ov * 2, .2, .06, white, cx, top - .03, z)));
        tag.roof = [cx, top + rise + .6, cz]; cam.roof = [[cx - 4, top + 12, cz + D / 2 + 12], [cx, top, cz]];
        ROOT = OG[o.id];
        if (o.entry === 'stoop' || o.entry === 'porch') { const fd = objs.find(d0 => d0.type === 'door' && d0.wall === 'front' && d0.host === o.id); const dx = fd ? cx - W / 2 + fd.at * FT : cx; for (let i = 0; i < 3; i++) pk(o.id, box(1.7, .14, 1.2 - i * .3, mats.conc, dx, .07 + i * .13, cz + D / 2 + .7 - i * .15)); [-.82, .82].forEach(q => { pk(o.id, box(.04, .9, .04, white, dx + q, FYc + .45, cz + D / 2 + .9)); pk(o.id, box(.04, .04, 1.2, white, dx + q, FYc + .9, cz + D / 2 + .7)); }); if (o.entry === 'porch') { [-1.1, 1.1].forEach(q => pk(o.id, box(.14, 2.5, .14, white, dx + q, FYc + 1.25, cz + D / 2 + 1.5))); const pr = box(3.0, .12, 1.9, roofM, dx, FYc + 2.6, cz + D / 2 + .9); pr.rotation.x = .16; pk(o.id, pr); }
          if (o.planter) { const pw = Math.max(1, cx + W / 2 - dx - 1.4); const bx = dx + .9 + pw / 2; pk(o.id, box(pw, .45, .12, block, bx, .22, cz + D / 2 + 1.25)); pk(o.id, box(.12, .45, 1.0, block, bx + pw / 2, .22, cz + D / 2 + .75)); pk(o.id, box(pw, .38, .95, mats.soil, bx, .19, cz + D / 2 + .75)); } }
        tag[o.id] = [cx, 2, cz + D / 2 + .6];
        cam[o.id] = [[cx + W * .5, 5 + W * .25, cz + D / 2 + W * .9 + 6], [cx, 1.4, cz]];
      } else {
        const gr = (W / 2 + .35) * .42, gSL = Math.hypot(W / 2 + .35, gr), gang = Math.atan2(gr, W / 2 + .35);
        const r1 = box(gSL, .13, D + .6, roofM, cx - (W / 2 + .35) / 2, top + gr / 2, cz); r1.rotation.z = gang; pk(o.id, r1); const r2 = box(gSL, .13, D + .6, roofM, cx + (W / 2 + .35) / 2, top + gr / 2, cz); r2.rotation.z = -gang; pk(o.id, r2);
        const tri = new THREE.Shape(); tri.moveTo(-W / 2, 0); tri.lineTo(W / 2, 0); tri.lineTo(0, gr * W / 2 / (W / 2 + .35)); tri.closePath();
        [cz + D / 2 - .2, cz - D / 2].forEach(z => { const m = new THREE.Mesh(new THREE.ExtrudeGeometry(tri, { depth: .2, bevelEnabled: false }), sm(W, gr)); m.position.set(cx, top, z); m.castShadow = true; OG[o.id].add(m); pk(o.id, m); });
        const dw = o.cars === 1 ? 2.75 : 4.9; pk(o.id, box(dw, 2.15, .08, mats.gdoor, cx, base + 1.08, cz + D / 2 + .11)); pk(o.id, box(dw + .2, 2.28, .05, white, cx, base + 1.12, cz + D / 2 + .1));
        if (!o.attached) pk(o.id, box(.85, 2.0, .08, white, cx - W / 2 + .7, 1.15, cz + D / 2 + .11));
        tag[o.id] = [cx, top + gr + .7, cz]; cam[o.id] = [[cx + 5, 4.5, cz + D / 2 + 11], [cx, 1.4, cz]];
      }
    }
    if (!cam[o.id]) { const p = tag[o.id] || [X(o.x || 0), 1, Z(o.z || 0)]; const span = Math.max(o.w || o.spread || 8, o.d || o.h || 8) * FT; cam[o.id] = [[p[0] + span * .6 + 4, span * .5 + 5, p[2] + span + 8], [p[0], Math.min(1.5, p[1] * .5), p[2]]]; }
  });
  // windows + doors live on a host wall
  objs.filter(o => o.type === 'window' || o.type === 'door').forEach(o => {
    const wi = wallInfo[o.host]; if (!wi) return; ROOT = grp2(o.id);
    const ww = o.w * FT, hh = o.h * FT, along = (o.wall === 'front' || o.wall === 'back') ? wi.W : wi.D; const a = clamp(o.at * FT, ww / 2, along - ww / 2);
    let x, z, ry = 0; const y = o.type === 'door' ? wi.base + hh / 2 : wi.base + (o.sill || 3) * FT + hh / 2;
    if (o.wall === 'front') { x = wi.cx - wi.W / 2 + a; z = wi.cz + wi.D / 2 + .08; } else if (o.wall === 'back') { x = wi.cx - wi.W / 2 + a; z = wi.cz - wi.D / 2 - .08; } else if (o.wall === 'left') { x = wi.cx - wi.W / 2 - .08; z = wi.cz + wi.D / 2 - a; ry = Math.PI / 2; } else { x = wi.cx + wi.W / 2 + .08; z = wi.cz + wi.D / 2 - a; ry = Math.PI / 2; }
    if (o.type === 'door') { const d = box(ww, hh, .09, white, x, y, z); d.rotation.y = ry; pk(o.id, d); const f = box(ww + .16, hh + .12, .06, trimD, x, y + .03, z + (ry ? 0 : -.02)); f.rotation.y = ry; pk(o.id, f); if (o.kind === 'front') { const gl = box(.36, .45, .1, mats.glass, x, y + .55, z + (ry ? 0 : .04)); gl.rotation.y = ry; } }
    else { const fr = box(ww + .14, hh + .14, .07, trimD, x, y, z); fr.rotation.y = ry; pk(o.id, fr); const gl = box(ww - .04, hh - .04, .09, mats.glass, x, y, z); gl.rotation.y = ry; pk(o.id, gl); const sl = box(ww + .2, .06, .12, white, x, y - hh / 2 - .06, z); sl.rotation.y = ry; }
    const out = o.wall === 'front' ? [0, 0, 1] : o.wall === 'back' ? [0, 0, -1] : o.wall === 'left' ? [-1, 0, 0] : [1, 0, 0];
    tag[o.id] = [x + out[0] * .4, y + hh / 2 + .4, z + out[2] * .4]; cam[o.id] = [[x + out[0] * 7 + 1.5, y + 1.5, z + out[2] * 7 + (out[2] ? 0 : 1.5)], [x, y, z]];
  });
  ROOT = prev;
  // whole-property camera + plan camera
  const bx = [], bz = []; objs.forEach(o => { if (o.type === 'lot' || o.type === 'lawn' || o.type === 'fence') return; if (o.x != null) { bx.push(X(o.x - (o.w || 6) / 2), X(o.x + (o.w || 6) / 2)); bz.push(Z(o.z - (o.d || 6) / 2), Z(o.z + (o.d || 6) / 2)); } });
  bz.push(Z(0)); const cx = (Math.min(...bx) + Math.max(...bx)) / 2, cz = (Math.min(...bz) + Math.max(...bz)) / 2, span = Math.max(Math.max(...bx) - Math.min(...bx), Math.max(...bz) - Math.min(...bz));
  cam.home = [[cx + span * .55, span * .62 + 4, cz + span * .95 + 4], [cx, 1, cz]];
  cam.plan = [[0, L.d * FT * 1.35 + 8, Z(-L.d / 2) + .01], [0, 0, Z(-L.d / 2)]];
  CUST[h] = { g, M, P, OG, tag, cam, X, Z, L };
  objVis(h);
  return CUST[h];
}
function objVis(h) {
  const c = CUST[h]; if (!c) return; const y = S.year ?? TODAY, timeOn = S.year != null;
  (HOMES[h].objs || []).forEach(o => { const og = c.OG[o.id]; if (!og) return;
    let vis = true; if (o.status === 'removed') vis = timeOn ? y < (o.removedYear || TODAY) : false; if (o.status === 'planned') vis = !timeOn || y >= (o.year || TODAY + 1);
    og.visible = vis; const ghost = (o.status === 'planned' || pendingRemove(h, o.id)) && vis;
    og.traverse(m => { if (!m.isMesh && !m.isSprite) return; if (ghost && !m.userData.gm) { m.userData.base = m.material; const gm = m.material.clone(); gm.transparent = true; gm.opacity = .4; gm.depthWrite = false; m.material = gm; m.userData.gm = 1; } else if (!ghost && m.userData.gm) { m.material = m.userData.base; m.userData.gm = 0; } });
  });
}
function decorateCustom() {}
function setCustomColors(h) { const c = CUST[h]; if (!c) return; const col = HOMES[h].colors || {}; if (col.siding) c.M.siding.forEach(m => m.color.set(col.siding)); if (col.trim) c.M.trim.forEach(m => m.color.set(col.trim)); if (col.roof) c.M.roof.forEach(m => m.color.set(col.roof)); }
function rebuild(h = S.home) { if (!HOMES[h] || HOMES[h].model !== 'custom') return; buildCustom(h); setCustomColors(h); applyView(); buildTags(); }
function objName(o) { return o.name || (OT[o.type] || OT.photo).n; }
function sizeText(o) { if (o.type === 'fence') return `${Math.round(Math.hypot(o.x2 - o.x1, o.z2 - o.z1))} ft long · ${o.h || 6} ft tall`; if (o.type === 'tree' || o.type === 'bush' || o.type === 'photo') return `${o.h} ft tall${o.spread ? ` · ${o.spread} ft wide` : o.w ? ` · ${o.w} ft wide` : ''}`; if (o.type === 'window' || o.type === 'door') return `${o.w} × ${o.h} ft · ${o.wall} wall`; if (o.type === 'roof') { const hs = (HOMES[S.home].objs || []).find(x => x.type === 'house'); return hs ? `${hs.roof[0].toUpperCase() + hs.roof.slice(1)} · over ${hs.w} × ${hs.d} ft` : ''; } if (o.type === 'lot') return `${o.w} × ${o.d} ft · ${(o.w * o.d).toLocaleString()} sq ft`; if (o.w) return `${o.w} × ${o.d} ft`; return ''; }

/* ---- find your house ---- */
function openFindHome() {
  let st = 'ask', res = null, q = '', gar = { type: null, cars: 2, side: 'right' };
  const redraw = () => fullStack[fullStack.length - 1].draw();
  full('Add a home', () => {
    const B = $('#fBody');
    if (st === 'ask') {
      B.innerHTML = `<div class="card"><div class="k v">FIND YOUR HOUSE</div><h3>What’s the address?</h3><p>Atlas looks up your lot, the outlines of your buildings and the public record, then builds a 3D model you can adjust.</p><div class="inl" style="margin-top:10px"><input id="faQ" placeholder="e.g. 816 Maywood Dr, Billings, MT" value="${esc(q)}" autocomplete="street-address" enterkeyhint="search"><button class="go" id="faGo">Find</button></div></div>
        <div class="card"><div class="k">OR</div><div class="row" style="margin-top:4px"><button class="btn g" id="faManual">Start from a typical house</button><button class="btn g" id="faOld">Use a ready-made model</button></div></div>
        <p class="src2">This prototype can’t reach the data services, so the lookup is simulated. ${WOULD} Never from Google Maps imagery.</p>`;
      const go = () => { q = $('#faQ').value.trim(); if (!q) { $('#faQ').focus(); return; } st = 'search'; redraw(); };
      $('#faGo').addEventListener('click', go); $('#faQ').addEventListener('keydown', e => { if (e.key === 'Enter') go(); }); setTimeout(() => $('#faQ')?.focus(), 80);
      $('#faManual').addEventListener('click', () => { res = { address: 'My house', found: false, records: null, objs: null, facts: [] }; st = 'result'; redraw(); });
      $('#faOld').addEventListener('click', addHome);
    } else if (st === 'search') {
      const steps = ['Matching the address', 'Lot lines', 'Building outlines', 'Public record', 'Building your 3D model'];
      B.innerHTML = `<div class="card"><div class="k v">LOOKING UP</div><h3>${esc(q)}</h3><div class="lsteps">${steps.map((s, i) => `<div data-ls="${i}"><i></i>${s}</div>`).join('')}</div></div>`;
      res = lookupAddress(q); let i = 0; const tick = () => { const el = B.querySelector(`[data-ls="${i}"]`); if (!el) return; el.classList.add('done'); i++; if (i < steps.length) setTimeout(tick, 330); else setTimeout(() => { st = 'result'; if (fullStack.length && fullStack[fullStack.length - 1].title === 'Add a home') redraw(); }, 380); }; setTimeout(tick, 300);
    } else {
      const known = !!res.found; if (!known && gar.type) res.objs = objsFromBox(40, 28, gar);
      const objs = res.objs;
      B.innerHTML = `<div class="card found"><div class="k ${res.found ? 'g' : 'v'}">${res.found ? '✓ MATCHED YOUR ADDRESS' : 'NO RECORD FOUND'}</div><h3>${esc(res.address)}</h3>${res.area ? `<p>${esc(res.area)}</p>` : ''}${objs ? sitePlanSVG(objs) : ''}${objs ? `<div class="legend2"><span><i style="background:#d9cfe9;border-color:#6b4bd1"></i>House</span><span><i style="background:#ece6f6;border-color:#6b4bd1"></i>Garage</span><span><i style="background:#e4e2dc"></i>Concrete</span><span><i style="background:#fff;border-color:#9a9a9a"></i>Fence</span></div>` : ''}</div>
        ${res.facts.length ? `<div class="card"><div class="k v">WHAT ATLAS FOUND</div>${res.facts.map(([k, v, c]) => `<div class="fact"><span class="fk2">${esc(k)}</span><b>${esc(v)}</b><em class="sb ${c}">${SRC[c]}</em></div>`).join('')}<p class="src2">${WOULD}</p></div>` : ''}
        ${!known ? `<div class="card"><div class="k v">NOTHING ON FILE — TWO QUICK QUESTIONS</div><div class="srow"><span>Garage</span><div class="chipset">${[['none', 'None'], ['attached', 'Attached'], ['separate', 'Separate, behind']].map(([k, l]) => `<button class="${gar.type === k ? 'on' : ''}" data-gt="${k}">${l}</button>`).join('')}</div></div>${gar.type && gar.type !== 'none' ? `<div class="srow"><span>Which side</span><div class="chipset">${['left', 'right'].map(k => `<button class="${gar.side === k ? 'on' : ''}" data-gs="${k}">${k[0].toUpperCase() + k.slice(1)}</button>`).join('')}</div></div><div class="srow"><span>Size</span><div class="chipset">${[1, 2].map(k => `<button class="${gar.cars === k ? 'on' : ''}" data-gc="${k}">${k}-car</button>`).join('')}</div></div>` : ''}<p class="mini">Atlas asks instead of guessing. You can change everything on the model next.</p></div>` : ''}
        ${objs ? `<div class="card"><div class="k v">NAME IT</div><input class="inp" id="faN" value="${esc(res.found ? res.address.split(',')[0] : 'My house')}" style="margin-top:6px"></div><div class="row"><button class="btn" id="faBuild" style="flex:1">Build my house</button><button class="btn g" id="faBack">Wrong house</button></div>` : `<div class="row"><button class="btn g" id="faBack">Back</button></div>`}`;
      B.querySelectorAll('[data-gt]').forEach(b => b.addEventListener('click', () => { gar.type = b.dataset.gt; redraw(); }));
      B.querySelectorAll('[data-gs]').forEach(b => b.addEventListener('click', () => { gar.side = b.dataset.gs; redraw(); }));
      B.querySelectorAll('[data-gc]').forEach(b => b.addEventListener('click', () => { gar.cars = +b.dataset.gc; redraw(); }));
      $('#faBack').addEventListener('click', () => { st = 'ask'; gar.type = null; redraw(); });
      const fb = $('#faBuild'); if (fb) fb.addEventListener('click', () => { const r = res.records; const id = 'h' + Date.now();
        HOMES[id] = { name: $('#faN').value.trim() || 'My house', icon: '🏡', kind: r ? `House · ${r.beds} bed · built ${r.built}` : 'House', model: 'custom', custom: true, address: res.address, records: r, facts: res.facts, objs: JSON.parse(JSON.stringify(objs)), colors: { ...(res.colors || { siding: '#d9cbb0', trim: '#f4f1ea', roof: '#6b5a4c' }) }, onboard: 'check' };
        if (r) THINGS['t' + id] = { home: id, name: 'House', place: 'Outside', hist: [{ y: r.built, t: 'House built', s: 'Public record' }] };
        buildCustom(id); save(); S.htab = 'house'; goHome(id); selectObj('house', true); toast('Built from what Atlas found — check the house below'); });
    }
  });
}
function sitePlanSVG(objs) {
  const L = objs.find(o => o.type === 'lot') || { w: 62, d: 121 }, sc = 2.1, pad = 8, top = 22; const px = x => pad + (x + L.w / 2) * sc, pz = z => top + (-z) * sc;
  const r = (o, fill, stroke, dash) => `<rect x="${px(o.x - o.w / 2)}" y="${pz(o.z + o.d / 2)}" width="${o.w * sc}" height="${o.d * sc}" fill="${fill}" stroke="${stroke}" stroke-width="1.4" ${dash ? 'stroke-dasharray="3 2"' : ''}/>`;
  let s = `<rect x="${pad}" y="${pz(0)}" width="${L.w * sc}" height="${L.d * sc}" fill="#eef5e6" stroke="#9fbf86" stroke-dasharray="4 3"/><rect x="0" y="${top - 12}" width="${L.w * sc + pad * 2}" height="9" fill="#d6d3cd"/><text x="${pad + 4}" y="${top - 15}" font-size="10" fill="#5d5672">Street</text>`;
  objs.filter(o => o.type === 'drive' || o.type === 'walk' || o.type === 'patio').forEach(o => s += r(o, '#e4e2dc', 'none'));
  objs.filter(o => o.type === 'house').forEach(o => s += r(o, '#d9cfe9', '#6b4bd1') + `<text x="${px(o.x)}" y="${pz(o.z) + 4}" font-size="10" text-anchor="middle" fill="#3b2f6b">House</text>`);
  objs.filter(o => o.type === 'garage' || o.type === 'shed').forEach(o => s += r(o, '#ece6f6', '#6b4bd1', o.attached) + `<text x="${px(o.x)}" y="${pz(o.z) + 4}" font-size="10" text-anchor="middle" fill="#3b2f6b">${o.type === 'shed' ? 'Shed' : 'Garage'}</text>`);
  objs.filter(o => o.type === 'fence').forEach(o => s += `<line x1="${px(o.x1)}" y1="${pz(o.z1)}" x2="${px(o.x2)}" y2="${pz(o.z2)}" stroke="#9a9a9a" stroke-width="2.5" stroke-linecap="round"/><line x1="${px(o.x1)}" y1="${pz(o.z1)}" x2="${px(o.x2)}" y2="${pz(o.z2)}" stroke="#fff" stroke-width="1.2"/>`);
  objs.filter(o => o.type === 'tree' || o.type === 'bush').forEach(o => s += `<circle cx="${px(o.x)}" cy="${pz(o.z)}" r="${(o.spread || 6) * sc / 2}" fill="${o.kind === 'pine' ? '#5f8c62' : '#7fae6a'}" opacity=".8"/>`);
  return `<svg class="fp" viewBox="0 0 ${L.w * sc + pad * 2} ${L.d * sc + top + pad}" role="img" aria-label="Site plan">${s}</svg>`;
}

/* ---- the object card (bottom card) ---- */
function objCardHTML(h, id) {
  const o = O(h, id); if (!o) return ''; const t = OT[o.type] || OT.photo; const hs = (HOMES[h].objs || []).find(x => x.type === 'house');
  const num = (k, v, l, u = 'ft', min = 0, max = 200, step = .5) => `<div class="frow"><span class="fk">${l}</span><input class="flat num2" type="number" inputmode="decimal" step="${step}" min="${min}" max="${max}" data-ok="${k}" data-fk="ok${k}" value="${v ?? ''}"><span class="u">${u}</span></div>`;
  const chips = (k, opts, cur) => `<div class="frow"><span class="fk">${opts.l}</span><div class="chipset">${opts.o.map(([v, l]) => `<button class="${cur === v ? 'on' : ''}" data-oc="${k}" data-ov="${v}">${l}</button>`).join('')}</div></div>`;
  let size = '';
  if (o.type === 'house') size = num('w', o.w, 'Width') + num('d', o.d, 'Depth') + chips('stories', { l: 'Stories', o: [[1, 'One'], [1.5, 'Split'], [2, 'Two']] }, o.stories) + chips('roof', { l: 'Roof', o: [['gable', 'Gable'], ['hip', 'Hip'], ['flat', 'Flat']] }, o.roof) + chips('entry', { l: 'Front entry', o: [['stoop', 'Steps'], ['porch', 'Porch'], ['none', 'None']] }, o.entry) + colourRow(h, 'siding', 'Siding colour');
  else if (o.type === 'roof') size = chips('roof', { l: 'Shape', o: [['gable', 'Gable'], ['hip', 'Hip'], ['flat', 'Flat']] }, hs && hs.roof) + colourRow(h, 'roof', 'Colour');
  else if (o.type === 'garage') size = num('w', o.w, 'Width') + num('d', o.d, 'Depth') + chips('cars', { l: 'Size', o: [[1, '1-car'], [2, '2-car']] }, o.cars) + chips('attached', { l: 'Type', o: [[true, 'Attached'], [false, 'Separate']] }, o.attached);
  else if (o.type === 'window' || o.type === 'door') size = num('w', o.w, 'Width') + num('h', o.h, 'Height') + (o.type === 'window' ? num('sill', o.sill || 3, 'Sill height') : '') + num('at', Math.round(o.at * 2) / 2, o.wall === 'front' || o.wall === 'back' ? 'From left corner' : 'From front corner') + chips('wall', { l: 'Wall', o: [['front', 'Front'], ['back', 'Back'], ['left', 'Left'], ['right', 'Right']] }, o.wall) + (o.type === 'window' ? colourRow(h, 'trim', 'Frame colour') : '');
  else if (o.type === 'tree' || o.type === 'bush') size = num('h', o.h, 'Height') + num('spread', o.spread, 'Spread') + chips('kind', { l: 'Kind', o: o.type === 'tree' ? [['oak', 'Shade'], ['maple', 'Maple'], ['pine', 'Evergreen'], ['ornamental', 'Ornamental']] : [['shrub', 'Shrub'], ['flowering', 'Flowering']] }, o.kind);
  else if (o.type === 'photo') size = num('h', o.h, 'Height') + num('w', o.w, 'Width');
  else if (o.type === 'fence') size = num('h', o.h || 6, 'Height') + `<div class="frow"><span class="fk">Length</span><b style="font-size:14px">${Math.round(Math.hypot(o.x2 - o.x1, o.z2 - o.z1))} ft</b><span class="u">drag the ends in plan view</span></div>`;
  else if (o.type === 'lot') size = num('w', o.w, 'Frontage') + num('d', o.d, 'Depth');
  else if (o.type === 'shed') size = num('w', o.w, 'Width') + num('d', o.d, 'Depth') + num('h', o.h || 8, 'Height');
  else if (o.w != null) size = num('w', o.w, 'Width') + num('d', o.d, 'Length');
  if (o.x != null && o.type !== 'lot') { const L = lotOf(h); size += `<div class="frow"><span class="fk">Position</span><span class="pos">${Math.round(-(o.z + (o.d || 0) / 2))} ft from the street · ${Math.round(o.x - (o.w || o.spread || 0) / 2 + L.w / 2)} ft from the left line</span></div>`; }
  const projs = PROJ.filter(p => p.home === h && p.areas && p.areas[id]);
  const hist = (o.hist || []).slice().sort((a, b) => a.y - b.y);
  const sug = SUG[o.type] || SUG.photo;
  const status = o.status === 'removed' ? `<div class="obst rm">Removed ${o.removedYear || ''} — kept in the history</div>` : o.status === 'planned' ? `<div class="obst pl">Planned ${o.year || ''} — not there yet</div>` : pendingRemove(h, id) ? '<div class="obst pl">Being removed — a project is open</div>' : '';
  return `${status}<div class="card"><div class="k v">START A PROJECT ON THIS</div><div class="sugs">${sug.map(([n, ic], i) => `<button data-sg="${i}"><span>${ic}</span>${esc(n)}</button>`).join('')}<button data-sg="own"><span>✏️</span>Something else…</button></div></div>
    ${projs.length ? `<div class="card"><div class="k v">PROJECTS ON THIS</div>${projs.map(projRow).join('')}</div>` : ''}
    <div class="card"><div class="lh"><div class="k v">${o.type === 'roof' || o.type === 'house' ? 'SHAPE & SIZE' : 'SIZE & PLACE'}</div>${(o.x != null || o.x1 != null) && !S.plan ? '<button class="addb" id="toPlan">Move in plan view</button>' : ''}</div><div class="frow"><span class="fk">Name</span><input class="flat" data-on="1" data-fk="oname" value="${esc(objName(o))}"></div>${size}</div>
    ${hist.length ? `<div class="card"><div class="k v">HISTORY</div><div class="hist">${hist.map(r => `<div><b>${r.y} · ${esc(r.t)}</b><small>${esc(r.s || '')}</small></div>`).join('')}</div></div>` : ''}
    <div class="card"><div class="k">FIX THE MODEL</div><div class="row" style="margin-top:4px">${o.type !== 'house' && o.type !== 'lot' && o.type !== 'roof' ? (o.status === 'removed' ? '<button class="btn g" id="obBack">It’s back — show it again</button>' : '<button class="btn g" id="obGone">It’s gone</button>') : ''}<button class="btn g" id="obPhoto">📷 Add a photo of it</button>${o.type !== 'house' && o.type !== 'lot' && o.type !== 'roof' ? '<button class="btn g danger" id="obDel">Delete — added by mistake</button>' : ''}</div><p class="mini">“It’s gone” keeps it in the history (removed by a past owner, storm…). A project to remove it does the same when the project is finished.</p></div>`;
}
function colourRow(h, k, label) { const cur = (HOMES[h].colors || {})[k]; const pal = k === 'roof' ? COLOURS.shingle : k === 'trim' ? COLOURS.paint.concat([{ n: 'Slate', hex: '#4b5763' }, { n: 'Black', hex: '#2b2b2b' }]) : COLOURS.siding.concat([{ n: 'White', hex: '#ebeae5' }, { n: 'Tan', hex: '#d9cbb0' }]); return `<div class="frow"><span class="fk">${label}</span><div class="sws">${pal.map(c => `<button class="${cur && cur.toLowerCase() === c.hex.toLowerCase() ? 'on' : ''}" data-col="${k}" data-hex="${c.hex}" title="${esc(c.n)}" style="background:${c.hex}"></button>`).join('')}</div></div>`; }
function bindObjCard(B, h, id) {
  const o = O(h, id); if (!o) return; flatKeys(B);
  const host = o.type === 'roof' ? (HOMES[h].objs || []).find(x => x.type === 'house') : o;
  const done = (msg, undo) => { save(); rebuild(h); render(); if (msg) toast(msg, undo ? () => { undo(); save(); rebuild(h); render(); } : null); };
  B.querySelectorAll('[data-ok]').forEach(inp => inp.addEventListener('change', () => { const k = inp.dataset.ok; const v = parseFloat(inp.value); if (isNaN(v) || v <= 0) { inp.value = o[k]; return; } const was = o[k]; o[k] = v; if (k === 'w' && o.type === 'garage' && o.attached) { } done(`${objName(o)} · ${inp.closest('.frow').querySelector('.fk').textContent.toLowerCase()} ${v} ft`, () => o[k] = was); }));
  B.querySelectorAll('[data-oc]').forEach(b => b.addEventListener('click', () => { const k = b.dataset.oc; let v = b.dataset.ov; v = v === 'true' ? true : v === 'false' ? false : isNaN(+v) ? v : +v; const tgt = (k === 'roof' && o.type === 'roof') ? host : o; const was = tgt[k]; tgt[k] = v; const f = (HOMES[h].facts || []).find(x => x[0] === (k === 'roof' ? 'Roof' : 'House outline')); if (f) f[2] = 'you'; done(null); buzz(6); }));
  B.querySelectorAll('[data-col]').forEach(b => b.addEventListener('click', () => { const k = b.dataset.col; const was = HOMES[h].colors[k]; HOMES[h].colors[k] = b.dataset.hex; setCustomColors(h); save(); render(); toast(`${k[0].toUpperCase() + k.slice(1)} colour changed`, () => { HOMES[h].colors[k] = was; setCustomColors(h); save(); render(); }); }));
  const nm = B.querySelector('[data-on]'); if (nm) nm.addEventListener('change', () => { const v = nm.value.trim(); if (!v) { nm.value = objName(o); return; } const was = o.name; o.name = v; save(); buildTags(); toast('Name saved', () => { o.name = was; save(); render(); }); });
  B.querySelectorAll('[data-sg]').forEach(b => b.addEventListener('click', () => { if (b.dataset.sg === 'own') { openCatalogFor(h, id); return; } const sg = (SUG[o.type] || SUG.photo)[+b.dataset.sg]; projectFor(h, id, sg); }));
  bindList(B);
  const tp = $('#toPlan'); if (tp) tp.addEventListener('click', () => setPlan(true));
  const gone = $('#obGone'); if (gone) gone.addEventListener('click', () => modal(`<h3>${esc(objName(o))} is gone?</h3><p class="mini" style="margin:0">It comes off the model and stays in the history.</p><div class="lbl">When was it removed?</div><input class="inp" id="gyY" inputmode="numeric" value="${TODAY}"><div class="row"><button class="btn" id="gyOk">Remove from the model</button><button class="btn g" id="cx">Cancel</button></div>`, () => { $('#cx').addEventListener('click', closeModal); $('#gyOk').addEventListener('click', () => { const y = parseInt($('#gyY').value) || TODAY; const was = { s: o.status, y: o.removedYear }; o.status = 'removed'; o.removedYear = y; (o.hist = o.hist || []).push({ y, t: 'Removed', s: 'You marked it gone' }); closeModal(); S.obj = null; done(`${objName(o)} removed · kept in history`, () => { o.status = was.s; o.removedYear = was.y; o.hist.pop(); }); }); }));
  const back = $('#obBack'); if (back) back.addEventListener('click', () => { o.status = 'exists'; done(`${objName(o)} is back on the model`); });
  const del = $('#obDel'); if (del) del.addEventListener('click', () => { const L0 = HOMES[h].objs; const i = L0.indexOf(o); L0.splice(i, 1); S.obj = null; done(`Deleted ${objName(o)}`, () => L0.splice(i, 0, o)); });
  $('#obPhoto').addEventListener('click', () => pickPhoto(src => { (o.photos = o.photos || []).push({ src, d: 'Added just now' }); (o.hist = o.hist || []).push({ y: TODAY, t: 'Photo added', s: 'You' }); save(); render(); toast('Photo saved with ' + objName(o)); }));
}

/* ---- projects start from a thing ---- */
function projectFor(h, id, sg) {
  const o = O(h, id); const [label, , steps, effect] = sg; const place = PLACE_OF_T(o.type);
  const on = objName(o), low = on.replace(/^./, c => c.toLowerCase()), noun = (OT[o.type] || OT.photo).n.toLowerCase();
  let nm; const ref = label.match(/\b(this|the) (window|fence|garage|door|concrete|walkway)\b/i);
  if (/^(Remove|Remove it|Tear it down)$/.test(label)) nm = `Remove ${low.toLowerCase().includes(noun) ? 'the ' + low : low}`;
  else if (ref) nm = low.toLowerCase().includes(ref[2].toLowerCase()) ? label.replace(ref[0], 'the ' + low) : `${label} · ${on}`;
  else nm = on.toLowerCase() === noun ? label : `${label} — ${low}`;
  create({ name: nm[0].toUpperCase() + nm.slice(1), group: place, home: h, place, when: 'planned', areaKeys: [], areas: { [id]: objName(o) }, steps: steps.slice(), year: TODAY, cost: 0, budget: 0, effect: effect ? { kind: effect, obj: id } : null, thingObj: id });
  S.obj = null; objVis(h);
}
function openCatalogFor(h, id) { S.pendingObj = { h, id }; openCatalog('planned'); }
function applyEffect(p, on) { if (!p.effect) return; const o = O(p.home, p.effect.obj); if (!o) return; if (p.effect.kind === 'add') { o.status = on ? 'exists' : 'planned'; if (on) (o.hist = o.hist || []).push({ y: p.year || TODAY, t: 'Added', s: p.name }); } if (p.effect.kind === 'remove') { if (on) { o.status = 'removed'; o.removedYear = p.year || TODAY; (o.hist = o.hist || []).push({ y: o.removedYear, t: 'Removed', s: p.name }); } else { o.status = 'exists'; if (o.hist && o.hist.length && o.hist[o.hist.length - 1].t === 'Removed') o.hist.pop(); } } if (custOf(p.home)) { objVis(p.home); } }

/* ---- “Your house”: everything on the property as a list ---- */
function thingsHTML(h) {
  const objs = (HOMES[h].objs || []).filter(o => o.type !== 'lot');
  const by = {}; objs.forEach(o => { const gk = (OT[o.type] || OT.photo).g; (by[gk] = by[gk] || []).push(o); });
  const hint = HOMES[h].onboard === 'done' || PROJ.some(p => p.home === h) ? '' : `<div class="week"><div class="k v">YOUR HOUSE IS SET UP</div><p style="margin:4px 0 0;font-size:14px">Tap anything — on the model or in this list — to start a project on it, fix its size, or mark it gone.</p></div>`;
  const row = o => { const pr = PROJ.filter(p => p.home === h && p.areas && p.areas[o.id] && p.status !== 'done').length; return `<button class="proj obj ${o.status === 'removed' ? 'gone' : ''}" data-ob="${o.id}"><span class="ic">${(OT[o.type] || OT.photo).ic}</span><span style="flex:1;min-width:0"><b>${esc(objName(o))}</b><small>${esc(sizeText(o))}${o.status === 'removed' ? ' · removed ' + (o.removedYear || '') : o.status === 'planned' ? ' · planned' : pendingRemove(h, o.id) ? ' · being removed' : ''}</small></span>${pr ? `<span class="c">${pr}</span>` : '<span class="go">›</span>'}</button>`; };
  return hint + `<div class="lh"><div class="k">${objs.filter(o => o.status !== 'removed').length} THINGS AT ${esc(HOMES[h].name.toUpperCase())}</div><div style="display:flex;gap:6px"><button class="addb" id="thPlan">Plan view</button><button class="addb" id="thAdd">＋ Add</button></div></div>` +
    GROUPS.filter(gk => by[gk]).map(gk => `<div class="grp ${COL['o' + gk] ? 'shut' : ''}"><button class="gh2" data-gk="o${gk}"><span class="chev">▾</span><span class="t">${gk}</span><em>${by[gk].length}</em></button><div class="gl">${by[gk].map(row).join('')}</div></div>`).join('') +
    `<button class="proj obj" data-ob="lot"><span class="ic">📐</span><span style="flex:1"><b>Lot</b><small>${esc(sizeText(lotOf(h)))}</small></span><span class="go">›</span></button>`;
}
function bindThings(B, h) { B.querySelectorAll('[data-ob]').forEach(b => b.addEventListener('click', () => selectObj(b.dataset.ob))); const tp = $('#thPlan'); if (tp) tp.addEventListener('click', () => setPlan(true)); const ta = $('#thAdd'); if (ta) ta.addEventListener('click', () => openAddObject(h)); bindGroups(B); }
function selectObj(id, quiet) { S.obj = id; if (S.home && HOMES[S.home].onboard === 'check' && id !== 'house') HOMES[S.home].onboard = 'done'; const c = custOf(S.home); if (c && !S.plan && !quiet) flyTo(c.cam[id] || c.cam.home, .9); highlight(); buildTags(); render(); arm(); if (sheetState === 'peek') setSheet('half'); $('#sbody').scrollTop = 0; }
function closeObj() { S.obj = null; const c = custOf(S.home); if (c && !S.plan) flyTo(c.cam.home, .9); highlight(); buildTags(); render(); }

/* ---- one ＋ per screen: on a home it starts a project ---- */
function openNewProject() {
  const h = S.home, cst = HOMES[h].model === 'custom';
  if (!cst) { openCatalog('planned'); return; }
  full('New project', () => {
    const objs = (HOMES[h].objs || []).filter(o => o.type !== 'lot' && o.status !== 'removed');
    const by = {}; objs.forEach(o => { const gk = (OT[o.type] || OT.photo).g; (by[gk] = by[gk] || []).push(o); });
    $('#fBody').innerHTML = `<div class="card"><div class="k v">WHAT’S IT ABOUT?</div><div class="row" style="margin-top:4px"><button class="btn" id="npPick" style="flex:1">👆 Tap it on my house</button><button class="btn g" id="npCat">Search all kinds</button></div><p class="mini">Starting from the thing means the project knows what it’s about — the right steps, the right spot on the map, and the right history.</p></div>
      ${GROUPS.filter(gk => by[gk]).map(gk => `<div class="gh">${gk.toUpperCase()}</div>${by[gk].map(o => `<button class="proj obj" data-np="${o.id}"><span class="ic">${(OT[o.type] || OT.photo).ic}</span><span style="flex:1;min-width:0"><b>${esc(objName(o))}</b><small>${(SUG[o.type] || SUG.photo).map(s => s[0]).slice(0, 3).join(' · ')}</small></span><span class="go">›</span></button>`).join('')}`).join('')}
      <div class="row" style="margin-top:14px"><button class="btn g" id="npAddObj">＋ It’s something new (plant a tree, add a shed…)</button></div>
      <div class="row"><button class="btn g" id="npPast">🕓 Log one already done</button></div>`;
    $('#npPick').addEventListener('click', () => { while (fullStack.length) popFull(true); S.pick = true; render(); toast('Tap the thing the project is about'); });
    $('#npCat').addEventListener('click', () => openCatalog('planned'));
    $('#npPast').addEventListener('click', () => openCatalog('done'));
    $('#npAddObj').addEventListener('click', () => openAddObject(h, true));
    $('#fBody').querySelectorAll('[data-np]').forEach(b => b.addEventListener('click', () => { while (fullStack.length) popFull(true); selectObj(b.dataset.np); }));
  });
}

/* ---- plan view: from straight above; drag to move, handles to resize, exact numbers in the card ---- */
const hEl = $('#handles'); const plane0 = new THREE.Plane(new THREE.Vector3(0, 1, 0), 0); const gp = new THREE.Vector3();
function setPlan(on) {
  S.plan = on; const c = custOf(S.home); if (!c) return;
  controls.enableRotate = !on; controls.minPolarAngle = on ? 0 : .25; controls.maxPolarAngle = on ? .02 : 1.25; controls.maxDistance = on ? 140 : 95;
  $('#planbar').classList.toggle('on', on); $('#map').classList.toggle('plan', on);
  flyTo(on ? planCam() : (S.obj && c.cam[S.obj]) || c.cam.home, .9); if (on) { if (sheetState === 'full') setSheet('half'); arm(); }
  buildTags(); render();
}
function planCam() { const c = custOf(S.home); const o = S.obj && O(S.home, S.obj); if (!o || o.type === 'house' && !S.plan) return c.cam.plan; const cx = o.x != null ? o.x : o.x1 != null ? (o.x1 + o.x2) / 2 : null; if (cx == null) return c.cam.plan; const cz = o.z != null ? o.z : (o.z1 + o.z2) / 2; const span = Math.max(45, (o.w || o.spread || Math.hypot((o.x2 || 0) - (o.x1 || 0), (o.z2 || 0) - (o.z1 || 0)) || 10) * 2.6); return [[c.X(cx), span * FT * 1.6 + 4, c.Z(cz) + .01], [c.X(cx), 0, c.Z(cz)]]; }
function groundAt(cx, cy) { const r = canvas.getBoundingClientRect(); ndc.set((cx - r.left) / r.width * 2 - 1, -((cy - r.top) / r.height) * 2 + 1); ray.setFromCamera(ndc, camera); return ray.ray.intersectPlane(plane0, gp) ? { x: gp.x / FT, z: gp.z / FT - lotOf(S.home).d / 2 } : null; }
const snap = v => Math.round(v * 2) / 2;
let drag = null, rbT = 0;
function liveRebuild() { if (rbT) return; rbT = requestAnimationFrame(() => { rbT = 0; buildCustom(S.home); setCustomColors(S.home); applyView(); }); }
function pickObj(e) { const c = custOf(S.home); if (!c) return null; const r = canvas.getBoundingClientRect(); ndc.set((e.clientX - r.left) / r.width * 2 - 1, -((e.clientY - r.top) / r.height) * 2 + 1); ray.setFromCamera(ndc, camera); const meshes = []; Object.values(c.P).forEach(l => l.forEach(m => { if (isShown(m)) meshes.push(m); })); const hit = ray.intersectObjects(meshes, false); if (!hit.length) return null; const pri = hit.find(x => { const o = O(S.home, x.object.userData.oid); return o && o.type !== 'lawn'; }) || hit[0]; return pri.object.userData.oid; }
canvas.addEventListener('pointerdown', e => { if (!S.plan || !custOf(S.home)) return; const id = pickObj(e); const o = id && O(S.home, id); if (!o || o.type === 'lawn' || o.type === 'lot' || o.type === 'roof' || o.type === 'window' || o.type === 'door') return; if (S.obj !== id) { S.obj = id; render(); } const g0 = groundAt(e.clientX, e.clientY); if (!g0) return; controls.enabled = false; drag = { kind: 'move', id, g0, o0: JSON.parse(JSON.stringify(o)) }; canvas.setPointerCapture(e.pointerId); }, true);
canvas.addEventListener('pointermove', e => { if (!drag || drag.kind !== 'move') return; const g1 = groundAt(e.clientX, e.clientY); if (!g1) return; const o = O(S.home, drag.id), a = drag.o0; const dx = snap(g1.x - drag.g0.x), dz = snap(g1.z - drag.g0.z); if (o.type === 'fence') { o.x1 = a.x1 + dx; o.x2 = a.x2 + dx; o.z1 = a.z1 + dz; o.z2 = a.z2 + dz; } else { o.x = a.x + dx; o.z = a.z + dz; } liveRebuild(); });
canvas.addEventListener('pointerup', e => { if (!drag || drag.kind !== 'move') return; const moved = JSON.stringify(O(S.home, drag.id)) !== JSON.stringify(drag.o0); const a = drag.o0, id = drag.id; drag = null; controls.enabled = true; if (moved) { save(); rebuild(); render(); toast(`Moved ${objName(O(S.home, id))}`, () => { Object.assign(O(S.home, id), a); save(); rebuild(); render(); }); } else render(); });
function q0h() { return canvas.getBoundingClientRect().height; }
function placeHandles() {
  hEl.innerHTML = ''; if (!S.plan || !S.obj) return; const c = custOf(S.home); const o = O(S.home, S.obj); if (!c || !o) return; const L = lotOf(S.home);
  const top = $('.hd').offsetHeight + ($('#planbar').offsetHeight || 0) + 6, bot = q0h() - sheetCover(); const H = (x, z, cls, kind, lbl) => { const q = proj2d([c.X(x), .1, c.Z(z)]); if (q.y < top || q.y > bot) return; const b = document.createElement('button'); b.className = 'hd2 ' + cls; b.style.left = q.x + 'px'; b.style.top = q.y + 'px'; b.setAttribute('aria-label', lbl); b.addEventListener('pointerdown', ev => { ev.stopPropagation(); ev.preventDefault(); controls.enabled = false; b.setPointerCapture(ev.pointerId); drag = { kind, id: o.id, o0: JSON.parse(JSON.stringify(o)) }; }); b.addEventListener('pointermove', ev => { if (!drag || drag.kind !== kind) return; const g1 = groundAt(ev.clientX, ev.clientY); if (!g1) return; resizeTo(o, drag.o0, kind, snap(g1.x), snap(g1.z)); liveRebuild(); }); b.addEventListener('pointerup', () => { if (!drag) return; const a = drag.o0; drag = null; controls.enabled = true; save(); rebuild(); render(); toast(`${objName(o)} · ${sizeText(o)}`, () => { Object.assign(o, a); save(); rebuild(); render(); }); }); hEl.appendChild(b); };
  if (isRect(o)) H(o.x + o.w / 2, o.z - o.d / 2, 'rs', 'corner', 'Resize');
  else if (o.type === 'fence') { H(o.x1, o.z1, 'end', 'e1', 'Move this end'); H(o.x2, o.z2, 'end', 'e2', 'Move this end'); }
  else if (o.type === 'tree' || o.type === 'bush') H(o.x + (o.spread || 6) / 2, o.z, 'rs', 'spread', 'Change the spread');
  else if (o.type === 'photo') H(o.x + (o.w || 3) / 2, o.z, 'rs', 'pw', 'Change the width');
  // live measurements
  const cx = o.x != null ? o.x : (o.x1 + o.x2) / 2, cz = o.z != null ? o.z : (o.z1 + o.z2) / 2; const q = proj2d([c.X(cx), .1, c.Z(cz)]);
  const m = document.createElement('div'); m.className = 'meas'; m.style.left = q.x + 'px'; m.style.top = q.y + 'px';
  const fromSt = o.z != null ? Math.round(-(o.z + (o.d || 0) / 2)) : null, fromL = o.x != null ? Math.round(o.x - (o.w || o.spread || 0) / 2 + L.w / 2) : null;
  if (q.y < top || q.y > bot) return; m.innerHTML = `<b>${esc(sizeText(o))}</b>${fromSt != null ? `<span>${fromSt} ft from street · ${fromL} ft from left line</span>` : ''}`; hEl.appendChild(m);
}
function resizeTo(o, a, kind, x, z) {
  if (kind === 'corner') { const left = a.x - a.w / 2, frontZ = a.z + a.d / 2; const w = Math.max(2, x - left), d = Math.max(2, frontZ - z); o.w = w; o.d = d; o.x = left + w / 2; o.z = frontZ - d / 2; }
  else if (kind === 'e1') { o.x1 = x; o.z1 = z; } else if (kind === 'e2') { o.x2 = x; o.z2 = z; }
  else if (kind === 'spread') o.spread = Math.max(1, snap(Math.abs(x - a.x) * 2)); else if (kind === 'pw') o.w = Math.max(1, snap(Math.abs(x - a.x) * 2));
}

/* ---- add something: from a list, a photo, a saved/AI image, or words ---- */
const ADDS = [['tree', 'oak', 'Shade tree', '🌳', 26, 22], ['tree', 'maple', 'Maple', '🍁', 22, 18], ['tree', 'pine', 'Evergreen', '🌲', 20, 10], ['tree', 'ornamental', 'Ornamental tree', '🌸', 12, 10], ['bush', 'shrub', 'Bush', '🌿', 3, 4], ['bush', 'flowering', 'Flowering bush', '🌺', 5, 5], ['bed', null, 'Garden bed', '🌷', 0, 0], ['shed', null, 'Shed', '🛖', 0, 0], ['patio', null, 'Patio', '◻️', 0, 0], ['deck', null, 'Deck', '🪵', 0, 0], ['fence', null, 'Fence run', '🧱', 0, 0], ['pool', null, 'Pool', '🏊', 0, 0], ['window', null, 'Window', '🪟', 0, 0]];
function newObj(h, t, kind, name, hh, sp, where) {
  const L = lotOf(h); const c = custOf(h); let x = 0, z = -L.d * .75; if (S.plan && c) { x = controls.target.x / FT; z = controls.target.z / FT - L.d / 2; } if (where) { x = where.x; z = where.z; }
  const o = { id: oid(t[0]), type: t, name, status: 'exists', hist: [] };
  if (t === 'tree' || t === 'bush') Object.assign(o, { kind, x, z, h: hh, spread: sp });
  else if (t === 'photo') Object.assign(o, { x, z, h: hh });
  else if (t === 'fence') Object.assign(o, { x1: x - 10, z1: z, x2: x + 10, z2: z, h: 6 });
  else if (t === 'window') { const hs = (HOMES[h].objs || []).find(q => q.type === 'house'); Object.assign(o, { host: hs ? hs.id : 'house', wall: 'back', at: hs ? hs.w / 2 : 10, w: 3, h: 3, sill: 3 }); }
  else Object.assign(o, { x, z, w: { bed: 12, shed: 10, patio: 14, deck: 14, pool: 16 }[t] || 10, d: { bed: 4, shed: 8, patio: 12, deck: 12, pool: 30 }[t] || 10, h: t === 'shed' ? 8 : undefined });
  return o;
}
function openAddObject(h, planned) {
  let when = planned ? 'planned' : 'exists';
  full('Add to your property', () => {
    $('#fBody').innerHTML = `<div class="seg" id="aoW"><button class="${when === 'exists' ? 'on' : ''}" data-w="exists">It’s there now</button><button class="${when === 'planned' ? 'on' : ''}" data-w="planned">I’m planning it</button></div>
      <div class="card"><div class="k v">FROM A PHOTO OR IMAGE</div><div class="row" style="margin-top:4px"><button class="btn" id="aoPhoto" style="flex:1">📷 Photo, saved image, or AI image</button><button class="btn g" id="aoWords">✏️ Describe it</button></div><p class="mini">A bush in your yard, one you saw online, or one an AI drew — Atlas cuts it out and stands it on your lot at its real height.</p></div>
      <div class="card"><div class="k v">OR PICK ONE</div><div class="addgrid">${ADDS.map((a, i) => `<button data-ad="${i}"><span>${a[3]}</span>${a[2]}</button>`).join('')}</div></div>`;
    $('#aoW').querySelectorAll('button').forEach(b => b.addEventListener('click', () => { when = b.dataset.w; fullStack[fullStack.length - 1].draw(); }));
    $('#fBody').querySelectorAll('[data-ad]').forEach(b => b.addEventListener('click', () => { const a = ADDS[+b.dataset.ad]; placeNew(h, newObj(h, a[0], a[1], a[2], a[4], a[5]), when); }));
    $('#aoPhoto').addEventListener('click', () => pickPhoto(src => openPhotoObject(h, src, when)));
    $('#aoWords').addEventListener('click', () => describeObject(h, when));
  });
}
function placeNew(h, o, when) {
  if (when === 'planned') { o.status = 'planned'; o.year = TODAY + 1; }
  HOMES[h].objs.push(o); while (fullStack.length) popFull(true); save(); rebuild(h);
  if (when === 'planned' && o.type !== 'window') { const plant = o.type === 'tree' || o.type === 'bush' || o.type === 'photo'; create({ name: `${plant ? 'Plant' : 'Add'} ${objName(o).replace(/^./, c => c.toLowerCase())}`, group: 'Yard', home: h, place: 'Yard', when: 'planned', areaKeys: [], areas: { [o.id]: objName(o) }, steps: plant ? ['Pick a spot', 'Buy', 'Call 811', 'Plant', 'Water in'] : ['Design', 'Quotes', 'Build'], year: TODAY + 1, cost: 0, budget: 0, effect: { kind: 'add', obj: o.id }, thingObj: o.id }); return; }
  S.obj = o.id; if (o.type !== 'window') { if (S.plan) { flyTo(planCam(), .8); render(); } else setPlan(true); } else { render(); } toast(`${objName(o)} added — ${o.type === 'window' ? 'set its wall and size below' : 'drag it where it goes'}`, () => { HOMES[h].objs = HOMES[h].objs.filter(x => x !== o); S.obj = null; save(); rebuild(h); render(); });
}
const WORDS = [[/maple/i, 'tree', 'maple', 22, 18], [/(oak|elm|ash|cottonwood|linden|shade tree)/i, 'tree', 'oak', 28, 24], [/(spruce|pine|fir|juniper tree|cedar tree|evergreen)/i, 'tree', 'pine', 20, 10], [/(crab ?apple|cherry|redbud|lilac tree|ornamental|flowering tree)/i, 'tree', 'ornamental', 14, 12], [/(lilac|hydrangea|rose|spirea|forsythia|flowering)/i, 'bush', 'flowering', 6, 6], [/(boxwood|yew|juniper|burning bush|bush|shrub|hedge)/i, 'bush', 'shrub', 3, 4], [/\btree\b/i, 'tree', 'oak', 22, 18], [/(garden bed|raised bed|flower bed|bed)/i, 'bed', null, 0, 0], [/shed/i, 'shed', null, 0, 0], [/patio|pavers/i, 'patio', null, 0, 0], [/deck/i, 'deck', null, 0, 0], [/fence/i, 'fence', null, 0, 0], [/pool|hot tub|spa/i, 'pool', null, 0, 0]];
function parseWords(t, h) {
  const r = WORDS.find(w => w[0].test(t)); if (!r) return null; const L = lotOf(h);
  const hm = t.match(/(\d+(?:\.\d+)?)\s*(?:ft|feet|foot|')\s*(?:tall|high)?/i); const nm = t.replace(/\b(a|an|the|about|around)\b/ig, ' ').replace(/(\d+(?:\.\d+)?)\s*(ft|feet|foot|')\s*(tall|high)?/ig, ' ').replace(/\b(in|by|near|along|at|on)\b.*$/i, '').replace(/\s+/g, ' ').trim();
  let where = null; if (/back fence|back of the yard|back yard|backyard/i.test(t)) where = { x: /left/i.test(t) ? -L.w / 3 : /right/i.test(t) ? L.w / 3 : 0, z: -L.d + 8 }; else if (/front yard|front lawn|by the street/i.test(t)) where = { x: /right/i.test(t) ? 10 : -16, z: -10 }; else if (/left side/i.test(t)) where = { x: -L.w / 2 + 4, z: -L.d / 2 }; else if (/right side/i.test(t)) where = { x: L.w / 2 - 4, z: -L.d / 2 };
  return { t: r[1], kind: r[2], h: hm ? parseFloat(hm[1]) : r[3], sp: r[4] && hm ? Math.max(2, parseFloat(hm[1]) * r[4] / r[3]) : r[4], name: nm ? nm[0].toUpperCase() + nm.slice(1) : (OT[r[1]] || OT.photo).n, where };
}
function describeObject(h, when) {
  full('Describe it', () => {
    $('#fBody').innerHTML = `<div class="card"><div class="k v">IN YOUR OWN WORDS</div><textarea class="bigin" id="dwT" placeholder="e.g. 6 ft lilac bush by the back fence, left side"></textarea><div id="dwR"></div></div><p class="src2">Atlas reads the kind, size and spot from your words. When it can’t tell, ✦ AI helps (in the app) — and you always confirm before anything is added.</p>`;
    const upd = () => { const r = parseWords($('#dwT').value, h); $('#dwR').innerHTML = !$('#dwT').value.trim() ? '' : r ? `<div class="understood"><div class="k" style="font-size:11px;font-weight:800">ATLAS UNDERSTOOD <em class="sb app">Atlas</em></div><div class="fact"><span class="fk2">What</span><b>${esc(r.name)} · ${(OT[r.t] || OT.photo).n.toLowerCase()}</b></div>${r.h ? `<div class="fact"><span class="fk2">Size</span><b>${r.h} ft tall${r.sp ? ` · ${Math.round(r.sp)} ft wide` : ''}</b></div>` : ''}<div class="fact"><span class="fk2">Where</span><b>${r.where ? 'Where you said — adjust in plan view' : 'Middle of the back yard — drag it after'}</b></div><div class="row"><button class="btn" id="dwOk" style="flex:1">Add it</button></div></div>` : `<div class="understood"><div class="k" style="font-size:11px;font-weight:800">NOT SURE WHAT THAT IS</div><p style="margin:4px 0 0">Try a kind of plant or thing (“lilac bush”, “shed”, “maple”). In the app, <em class="sb ai">✦ AI</em> would ask a follow-up question here.</p></div>`; const ok = $('#dwOk'); if (ok) ok.addEventListener('click', () => { const o = newObj(h, r.t, r.kind, r.name, r.h, r.sp, r.where); placeNew(h, o, when); }); };
    $('#dwT').addEventListener('input', upd); setTimeout(() => $('#dwT')?.focus(), 80);
  });
}
function openPhotoObject(h, src, when) {
  let base = null, cut = null, W0 = 0, H0 = 0, taps = [], cat = null, name = '', hh = null, guess = null; const img = new Image();
  const erase = (sx, sy, tol) => { const d = base.data, m = cut; const i0 = (sy * W0 + sx) * 4; const r0 = d[i0], g0 = d[i0 + 1], b0 = d[i0 + 2]; const st = [sx + sy * W0]; const seen = new Uint8Array(W0 * H0); const t2 = tol * tol; while (st.length) { const p = st.pop(); if (seen[p]) continue; seen[p] = 1; const i = p * 4; const dr = d[i] - r0, dg = d[i + 1] - g0, db = d[i + 2] - b0; if (dr * dr + dg * dg + db * db > t2) continue; m[p] = 0; const x = p % W0, y = (p / W0) | 0; if (x > 0) st.push(p - 1); if (x < W0 - 1) st.push(p + 1); if (y > 0) st.push(p - W0); if (y < H0 - 1) st.push(p + W0); } };
  const recompute = () => { cut = new Uint8Array(W0 * H0).fill(1); taps.forEach(t => { if (t.auto) { for (let x = 0; x < W0; x += 6) { erase(x, 0, t.tol); erase(x, H0 - 1, t.tol); } for (let y = 0; y < H0; y += 6) { erase(0, y, t.tol); erase(W0 - 1, y, t.tol); } } else erase(t.x, t.y, t.tol); }); };
  const outURL = () => { const c = document.createElement('canvas'); let minX = W0, minY = H0, maxX = 0, maxY = 0; for (let p = 0; p < cut.length; p++) if (cut[p]) { const x = p % W0, y = (p / W0) | 0; if (x < minX) minX = x; if (x > maxX) maxX = x; if (y < minY) minY = y; if (y > maxY) maxY = y; } if (maxX <= minX) { minX = 0; minY = 0; maxX = W0 - 1; maxY = H0 - 1; } const w = maxX - minX + 1, hgt = maxY - minY + 1; c.width = w; c.height = hgt; const x = c.getContext('2d'); const out = x.createImageData(w, hgt); for (let yy = 0; yy < hgt; yy++) for (let xx = 0; xx < w; xx++) { const p = (yy + minY) * W0 + xx + minX, i = p * 4, j = (yy * w + xx) * 4; out.data[j] = base.data[i]; out.data[j + 1] = base.data[i + 1]; out.data[j + 2] = base.data[i + 2]; out.data[j + 3] = cut[p] ? 255 : 0; } x.putImageData(out, 0, 0); return { url: c.toDataURL('image/png'), ar: w / hgt }; };
  const guessKind = () => { let gsum = 0, n = 0, bright = 0; for (let p = 0; p < cut.length; p += 9) if (cut[p]) { const i = p * 4; const r = base.data[i], g = base.data[i + 1], b = base.data[i + 2]; if (g > r * 1.05 && g > b * 1.05) gsum++; bright += r + g + b; n++; } const green = n ? gsum / n : 0; return green > .35 ? { t: 'bush', why: `${Math.round(green * 100)}% green — looks like a plant` } : { t: 'photo', why: 'Not mostly green — maybe furniture, art or a structure' }; };
  const draw = () => {
    const B = $('#fBody'); if (!base) { B.innerHTML = '<p class="mini">Opening the image…</p>'; return; }
    guess = guessKind(); if (!cat) cat = guess.t === 'bush' ? 'bush' : 'other';
    B.innerHTML = `<div class="card"><div class="k v">1 · CUT IT OUT</div><p style="margin:2px 0 8px">Tap the background to clear it, or let Atlas clear from the edges.</p><div class="rvwrap chk"><canvas id="poC" aria-label="Your image — tap the background to clear it"></canvas></div><div class="row"><button class="btn g" id="poAuto">✂️ Clear from the edges <em class="sb app">Atlas</em></button><button class="btn g" id="poUndo" ${taps.length ? '' : 'disabled'}>Undo</button></div><p class="mini">In the app, <em class="sb ai">✦ AI</em> outlines the object for you in one step; tapping is the fallback when it misses.</p></div>
      <div class="card"><div class="k v">2 · WHAT IS IT?</div><p style="margin:2px 0 6px">Atlas guess: <b>${esc(guess.why)}</b> <em class="sb app">Atlas</em></p><div class="chipset">${[['tree', '🌳 Tree'], ['bush', '🌿 Bush'], ['flowers', '🌷 Flowers'], ['other', '📦 Something else']].map(([k, l]) => `<button class="${cat === k ? 'on' : ''}" data-pc="${k}">${l}</button>`).join('')}</div><p class="mini">In the app, <em class="sb ai">✦ AI</em> names it too — e.g. “Dwarf Alberta spruce, grows to ~8 ft, full sun” — and adds its care.</p><div class="frow"><span class="fk">Name</span><input class="flat" id="poN" value="${esc(name || (cat === 'tree' ? 'Tree' : cat === 'bush' ? 'Bush' : cat === 'flowers' ? 'Flowers' : 'Item'))}"></div><div class="frow"><span class="fk">Height</span><input class="flat num2" id="poH" type="number" inputmode="decimal" step=".5" value="${hh ?? ({ tree: 18, bush: 4, flowers: 2, other: 4 }[cat])}"><span class="u">ft — the real height</span></div></div>
      <div class="row"><button class="btn" id="poOk" style="flex:1">Stand it on my lot</button></div>`;
    const cv = $('#poC'); cv.width = W0; cv.height = H0; const ctx = cv.getContext('2d'); const out = new ImageData(new Uint8ClampedArray(base.data), W0, H0); for (let p = 0; p < cut.length; p++) if (!cut[p]) out.data[p * 4 + 3] = 0; ctx.putImageData(out, 0, 0);
    cv.addEventListener('click', e => { const r = cv.getBoundingClientRect(); taps.push({ x: Math.round((e.clientX - r.left) / r.width * W0), y: Math.round((e.clientY - r.top) / r.height * H0), tol: 40 }); recompute(); draw(); });
    $('#poAuto').addEventListener('click', () => { taps.push({ auto: 1, tol: 46 }); recompute(); draw(); });
    $('#poUndo').addEventListener('click', () => { taps.pop(); recompute(); draw(); });
    B.querySelectorAll('[data-pc]').forEach(b => b.addEventListener('click', () => { cat = b.dataset.pc; name = ''; hh = null; draw(); }));
    $('#poN').addEventListener('input', e => name = e.target.value); $('#poH').addEventListener('input', e => hh = parseFloat(e.target.value) || null);
    $('#poOk').addEventListener('click', () => { const { url, ar } = outURL(); const H1 = parseFloat($('#poH').value) || 4; const o = newObj(h, 'photo', null, $('#poN').value.trim() || 'Item', H1, 0); Object.assign(o, { img: url, w: Math.max(1, Math.round(H1 * ar * 2) / 2), cat, src: 'photo' }); delete o.spread; placeNew(h, o, when); });
  };
  full('Add from an image', () => { draw(); if (base) return; img.onload = () => { const s = Math.min(1, 520 / Math.max(img.width, img.height)); W0 = Math.round(img.width * s); H0 = Math.round(img.height * s); const c = document.createElement('canvas'); c.width = W0; c.height = H0; const x = c.getContext('2d'); x.drawImage(img, 0, 0, W0, H0); base = x.getImageData(0, 0, W0, H0); recompute(); draw(); }; img.src = src; });
}

function photoPrompt(h) {
  modal(`<h3>Add a photo of the front?</h3><p style="margin:0;color:var(--sub)">Atlas takes your real siding, trim and roof colours from it — and you can try new colours on the photo itself.</p><div class="row" style="flex-direction:column;align-items:stretch"><button class="btn" id="ppTake">📷 Take or choose a photo</button><button class="btn g" id="ppSample">Use a sample photo for now</button><button class="btn g" id="cx">Skip</button></div><p class="mini">Stand across the street so the whole front fits. Your photo stays with this home.</p>`, () => {
    $('#ppTake').addEventListener('click', () => { closeModal(); pickPhoto(src => { HOMES[h].photo = src; HOMES[h].photoSample = false; HOMES[h].marks = {}; save(); openRealView(h, 'mark'); }); });
    $('#ppSample').addEventListener('click', () => { closeModal(); HOMES[h].photo = samplePhoto(); HOMES[h].photoSample = true; HOMES[h].marks = {}; save(); openRealView(h, 'mark'); });
    $('#cx').addEventListener('click', closeModal);
  });
}
function samplePhoto() {
  const c = document.createElement('canvas'); c.width = 960; c.height = 600; const g = c.getContext('2d');
  const sky = g.createLinearGradient(0, 0, 0, 330); sky.addColorStop(0, '#9cc6ea'); sky.addColorStop(1, '#dbe9f4'); g.fillStyle = sky; g.fillRect(0, 0, 960, 600);
  g.fillStyle = '#6f9a4f'; g.fillRect(0, 430, 960, 170); g.fillStyle = '#7fa95c'; for (let i = 0; i < 900; i++) g.fillRect(Math.random() * 960, 430 + Math.random() * 170, 2, 3);
  g.fillStyle = '#b9b5ad'; g.beginPath(); g.moveTo(640, 600); g.lineTo(700, 455); g.lineTo(900, 455); g.lineTo(960, 600); g.fill();
  // tree
  g.fillStyle = '#5a4632'; g.fillRect(70, 260, 22, 190); g.fillStyle = '#4f7a3a'; g.beginPath(); g.arc(80, 230, 85, 0, 7); g.fill(); g.fillStyle = '#5d8a45'; g.beginPath(); g.arc(120, 200, 55, 0, 7); g.fill();
  // house body (tan lap siding)
  const sidY = 290, sidH = 165; g.fillStyle = '#c8b48f'; g.fillRect(170, sidY, 520, sidH); g.fillRect(690, 320, 230, 135);
  for (let y = sidY; y < sidY + sidH; y += 9) { g.fillStyle = 'rgba(0,0,0,.10)'; g.fillRect(170, y, 520, 1.5); } for (let y = 320; y < 455; y += 9) { g.fillStyle = 'rgba(0,0,0,.10)'; g.fillRect(690, y, 230, 1.5); }
  // roofs (brown shingles)
  g.fillStyle = '#5e4a3c'; g.beginPath(); g.moveTo(140, sidY + 4); g.lineTo(430, 190); g.lineTo(720, sidY + 4); g.closePath(); g.fill();
  g.beginPath(); g.moveTo(675, 322); g.lineTo(805, 262); g.lineTo(940, 322); g.closePath(); g.fill();
  g.strokeStyle = 'rgba(0,0,0,.18)'; g.lineWidth = 1; for (let y = 200; y < sidY; y += 8) { g.beginPath(); g.moveTo(430 - (y - 190) * 2.9, y); g.lineTo(430 + (y - 190) * 2.9, y); g.stroke(); }
  // trim
  g.fillStyle = '#efe9dc'; g.fillRect(140, sidY, 580, 8); g.fillRect(675, 320, 265, 7); g.fillRect(168, sidY, 6, sidH); g.fillRect(686, 320, 6, 135); g.fillRect(916, 320, 6, 135);
  // windows + door + garage
  [[215, 330], [330, 330], [560, 330]].forEach(([x, y]) => { g.fillStyle = '#efe9dc'; g.fillRect(x - 6, y - 6, 92, 76); g.fillStyle = '#6d8296'; g.fillRect(x, y, 80, 64); g.fillStyle = 'rgba(255,255,255,.25)'; g.fillRect(x + 6, y + 6, 30, 20); g.fillStyle = '#efe9dc'; g.fillRect(x + 38, y, 4, 64); });
  g.fillStyle = '#efe9dc'; g.fillRect(452, 322, 64, 133); g.fillStyle = '#7a4e2e'; g.fillRect(458, 328, 52, 127); g.fillStyle = '#d8b25a'; g.fillRect(500, 392, 5, 5);
  g.fillStyle = '#efe9dc'; g.fillRect(712, 345, 186, 110); for (let y = 351; y < 452; y += 25) { g.fillStyle = 'rgba(0,0,0,.08)'; g.fillRect(716, y, 178, 2); }
  g.fillStyle = '#8e8a82'; g.fillRect(160, 452, 770, 6);
  [[230, 460], [300, 462], [380, 460], [560, 462], [620, 458]].forEach(([x, y]) => { g.fillStyle = '#3f6e35'; g.beginPath(); g.arc(x, y, 26, Math.PI, 0); g.fill(); });
  g.fillStyle = 'rgba(0,0,0,.55)'; g.fillRect(10, 566, 300, 24); g.fillStyle = '#fff'; g.font = '14px Roboto,sans-serif'; g.fillText('Sample photo — not your house', 20, 583);
  return c.toDataURL('image/jpeg', .9);
}

/* ---- real view: mark areas, then try colours on the photo ---- */
const LAYERS = [['siding', 'Siding', '#8c6cf0'], ['trim', 'Trim', '#ff9a3c'], ['roof', 'Roof', '#3aa0ff'], ['door', 'Front door', '#2fbf71']];
const PAL = { siding: () => COLOURS.siding, trim: () => COLOURS.paint, roof: () => COLOURS.shingle, door: () => [{ n: 'Hale Navy', code: 'HC-154', m: 'Benjamin Moore', hex: '#434b56' }, { n: 'Tricorn Black', code: 'SW 6258', m: 'Sherwin-Williams', hex: '#2f2f30' }, { n: 'Caliente', code: 'AF-290', m: 'Benjamin Moore', hex: '#9c3b34' }, { n: 'Evergreen Fog', code: 'SW 9130', m: 'Sherwin-Williams', hex: '#95978a' }] };
function openRealView(h, mode = 'colour') {
  const H0 = HOMES[h]; if (!H0.photo) { photoPrompt(h); return; }
  H0.marks = H0.marks || {}; H0.try = H0.try || {};
  let layer = 'siding', tol = 34, base = null, masks = {}, Wd = 0, Ht = 0, hold = false;
  const img = new Image();
  const calcMask = ly => { const taps = H0.marks[ly] || []; const m = new Uint8Array(Wd * Ht); taps.forEach(t => flood(m, Math.round(t.x * Wd), Math.round(t.y * Ht), t.tol, ly)); masks[ly] = m; };
  const flood = (m, sx, sy, tl, ly) => { const d = base.data; const i0 = (sy * Wd + sx) * 4; const r0 = d[i0], g0 = d[i0 + 1], b0 = d[i0 + 2]; const other = LAYERS.map(l => l[0]).filter(k => k !== ly && masks[k]); const st = [sx + sy * Wd]; const seen = new Uint8Array(Wd * Ht); const t2 = tl * tl; while (st.length) { const p = st.pop(); if (seen[p]) continue; seen[p] = 1; if (other.some(k => masks[k][p])) continue; const i = p * 4; const dr = d[i] - r0, dg = d[i + 1] - g0, db = d[i + 2] - b0; if (dr * dr * .9 + dg * dg * 1.2 + db * db * .9 > t2) continue; m[p] = 1; const x = p % Wd, y = (p / Wd) | 0; if (x > 0) st.push(p - 1); if (x < Wd - 1) st.push(p + 1); if (y > 0) st.push(p - Wd); if (y < Ht - 1) st.push(p + Wd); } };
  const median = ly => { const m = masks[ly]; if (!m) return null; const d = base.data; const rs = [], gs = [], bs = []; for (let p = 0; p < m.length; p += 7) if (m[p]) { rs.push(d[p * 4]); gs.push(d[p * 4 + 1]); bs.push(d[p * 4 + 2]); } if (rs.length < 20) return null; const md = a => a.sort((x, y) => x - y)[a.length >> 1]; return '#' + [md(rs), md(gs), md(bs)].map(v => v.toString(16).padStart(2, '0')).join(''); };
  const paint = () => {
    const cv = $('#rvC'); if (!cv || !base) return; const ctx = cv.getContext('2d'); const out = new ImageData(new Uint8ClampedArray(base.data), Wd, Ht); const d = out.data;
    if (!hold) LAYERS.forEach(([ly]) => { const hex = H0.try[ly]; const m = masks[ly]; if (!hex || !m) return; const tr = parseInt(hex.slice(1, 3), 16), tg = parseInt(hex.slice(3, 5), 16), tb = parseInt(hex.slice(5, 7), 16); let sum = 0, n = 0; for (let p = 0; p < m.length; p += 5) if (m[p]) { const i = p * 4; sum += .3 * d[i] + .59 * d[i + 1] + .11 * d[i + 2]; n++; } const Lr = Math.max(1, sum / Math.max(1, n)); for (let p = 0; p < m.length; p++) if (m[p]) { const i = p * 4; const L = (.3 * base.data[i] + .59 * base.data[i + 1] + .11 * base.data[i + 2]) / Lr; const k = Math.pow(L, .9); d[i] = Math.min(255, tr * k) * .9 + base.data[i] * .1; d[i + 1] = Math.min(255, tg * k) * .9 + base.data[i + 1] * .1; d[i + 2] = Math.min(255, tb * k) * .9 + base.data[i + 2] * .1; } });
    if (mode === 'mark' && !hold) LAYERS.forEach(([ly, , c]) => { const m = masks[ly]; if (!m) return; const cr = parseInt(c.slice(1, 3), 16), cg = parseInt(c.slice(3, 5), 16), cb = parseInt(c.slice(5, 7), 16); const a = ly === layer ? .5 : .28; for (let p = 0; p < m.length; p++) if (m[p]) { const i = p * 4; d[i] = d[i] * (1 - a) + cr * a; d[i + 1] = d[i + 1] * (1 - a) + cg * a; d[i + 2] = d[i + 2] * (1 - a) + cb * a; } });
    ctx.putImageData(out, 0, 0);
  };
  const draw = () => {
    const B = $('#fBody'); const marked = ly => (H0.marks[ly] || []).length;
    B.innerHTML = `${H0.photoSample ? '<p class="mini" style="margin:0 0 8px">Using a sample photo — <button class="lnk" id="rvOwn">use your own</button></p>' : ''}
      <div class="seg rvseg"><button class="${mode === 'mark' ? 'on' : ''}" data-m="mark">1 · Mark the parts</button><button class="${mode === 'colour' ? 'on' : ''}" data-m="colour">2 · Try colours</button></div>
      <div class="rvwrap"><canvas id="rvC" aria-label="Photo of your house"></canvas>${mode === 'colour' ? '<button class="hold" id="rvHold">Hold to see before</button>' : ''}</div>
      <div class="chipset rvl">${LAYERS.map(([ly, l, c]) => `<button class="${ly === layer ? 'on' : ''}" data-ly="${ly}"><span class="dot2" style="background:${mode === 'mark' ? c : (H0.try[ly] || median(ly) || '#ddd')}"></span>${l}${marked(ly) ? ' ✓' : ''}</button>`).join('')}</div>
      ${mode === 'mark' ? `<div class="card"><b style="font-size:15px">Tap the ${LAYERS.find(l => l[0] === layer)[1].toLowerCase()} in the photo.</b><p style="margin:2px 0 8px">Tap a few spots until it’s all covered. In the app, Atlas outlines these for you automatically.</p><div class="srow"><span>Precision</span><input type="range" min="14" max="70" value="${tol}" id="rvT" aria-label="How far each tap spreads"><b>${tol < 28 ? 'Tight' : tol > 48 ? 'Loose' : 'Normal'}</b></div><div class="row"><button class="btn g" id="rvUndo" ${marked(layer) ? '' : 'disabled'}>Undo tap</button><button class="btn g" id="rvClr" ${marked(layer) ? '' : 'disabled'}>Clear ${LAYERS.find(l => l[0] === layer)[1].toLowerCase()}</button></div></div>
        <div class="row"><button class="btn" id="rvUse" style="flex:1" ${LAYERS.some(([ly]) => marked(ly)) ? '' : 'disabled'}>${H0.photoSample ? 'Try the sample photo’s colours on the model' : 'Use my photo’s colours on the model'}</button></div>`
      : `<div class="card">${marked(layer) ? `<div class="k v">${LAYERS.find(l => l[0] === layer)[1].toUpperCase()} COLOURS</div><div class="pal">${[{ n: 'As it is now', hex: '' }].concat(PAL[layer]()).map(c => `<button class="${(H0.try[layer] || '') === c.hex ? 'on' : ''}" data-hx="${c.hex}"><span class="sw" style="background:${c.hex || median(layer) || '#ddd'}"></span><b>${esc(c.n)}</b><small>${esc(c.code ? c.code + ' · ' + c.m : c.m || 'Your photo')}</small></button>`).join('')}</div><p class="src2">Colour names from the makers’ current lines · on-screen colour is approximate — check a real sample.</p>` : `<p style="margin:0">Mark the ${LAYERS.find(l => l[0] === layer)[1].toLowerCase()} first (step 1), then try colours here.</p>`}</div>
        <div class="row"><button class="btn" id="rvApply" style="flex:1">Show these colours on my 3D house</button><button class="btn g" id="rvReset">Reset</button></div>`}`;
    const cv = $('#rvC'); cv.width = Wd; cv.height = Ht; paint();
    B.querySelectorAll('[data-m]').forEach(b => b.addEventListener('click', () => { mode = b.dataset.m; draw(); }));
    B.querySelectorAll('[data-ly]').forEach(b => b.addEventListener('click', () => { layer = b.dataset.ly; draw(); }));
    const own = $('#rvOwn'); if (own) own.addEventListener('click', () => pickPhoto(src => { H0.photo = src; H0.photoSample = false; H0.marks = {}; H0.try = {}; save(); fullStack.pop(); showTop(); openRealView(h, 'mark'); }));
    if (mode === 'mark') {
      cv.addEventListener('click', e => { const r = cv.getBoundingClientRect(); const x = (e.clientX - r.left) / r.width, y = (e.clientY - r.top) / r.height; (H0.marks[layer] = H0.marks[layer] || []).push({ x, y, tol }); calcMask(layer); save(); buzz(6); draw(); });
      $('#rvT').addEventListener('input', e => { tol = +e.target.value; e.target.nextElementSibling.textContent = tol < 28 ? 'Tight' : tol > 48 ? 'Loose' : 'Normal'; });
      $('#rvUndo').addEventListener('click', () => { (H0.marks[layer] || []).pop(); calcMask(layer); save(); draw(); });
      $('#rvClr').addEventListener('click', () => { H0.marks[layer] = []; calcMask(layer); save(); draw(); });
      $('#rvUse').addEventListener('click', () => { const was = { ...H0.colors }; LAYERS.forEach(([ly]) => { if (ly === 'door') return; const m = median(ly); if (m) H0.colors[ly] = m; }); setCustomColors(h); save(); toast('Your model now uses the colours from your photo', () => { H0.colors = was; setCustomColors(h); save(); }); mode = 'colour'; draw(); });
    } else {
      B.querySelectorAll('[data-hx]').forEach(b => b.addEventListener('click', () => { H0.try[layer] = b.dataset.hx; save(); paint(); B.querySelectorAll('[data-hx]').forEach(x => x.classList.toggle('on', x === b)); const lb = B.querySelector(`[data-ly="${layer}"] .dot2`); if (lb) lb.style.background = b.dataset.hx || median(layer) || '#ddd'; buzz(5); }));
      const hb = $('#rvHold'); const on = v => { hold = v; paint(); hb.classList.toggle('on', v); }; hb.addEventListener('pointerdown', e => { e.preventDefault(); on(true); }); ['pointerup', 'pointerleave', 'pointercancel'].forEach(ev => hb.addEventListener(ev, () => hold && on(false)));
      $('#rvApply').addEventListener('click', () => { const was = { ...H0.colors }; LAYERS.forEach(([ly]) => { if (ly === 'door') return; H0.colors[ly] = H0.try[ly] || median(ly) || H0.colors[ly]; }); setCustomColors(h); save(); while (fullStack.length) popFull(true); render(); toast('Showing your chosen colours on the 3D house', () => { H0.colors = was; setCustomColors(h); save(); }); });
      $('#rvReset').addEventListener('click', () => { H0.try = {}; save(); draw(); });
    }
  };
  full('Real view', () => { $('#fBody').innerHTML = '<p class="mini">Loading your photo…</p>'; if (base) { draw(); return; } img.onload = () => { const s = Math.min(1, 900 / Math.max(img.width, img.height)); Wd = Math.round(img.width * s); Ht = Math.round(img.height * s); const c = document.createElement('canvas'); c.width = Wd; c.height = Ht; const x = c.getContext('2d'); x.drawImage(img, 0, 0, Wd, Ht); base = x.getImageData(0, 0, Wd, Ht); LAYERS.forEach(([ly]) => calcMask(ly)); draw(); }; img.src = H0.photo; });
}

$('#pbDone').addEventListener('click', () => setPlan(false)); $('#pbAdd').addEventListener('click', () => openAddObject(S.home));
