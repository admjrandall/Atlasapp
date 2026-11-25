
/* ======================================================================
   your own house: find it → site plan → 3D → photo → real view
   ====================================================================== */
const FT = .3048;
const CUST = {};
const custOf = h => (HOMES[h] && HOMES[h].model === 'custom') ? CUST[h] : null;
function camOf(v) { const c = custOf(S.home); if (c) return c.cam.home; return CAM[v] || CAM.main; }
function areaCam(k) { const c = custOf(S.home); if (c) return c.cam[k] || null; return AREACAM[k]; }

/* Where each fact comes from. conf: record = public record · outline = building/lot outline data · you = you told Atlas · est = estimated · photo = read from your photo */
const SRC = { record: 'Public record', outline: 'Outline data', you: 'You', est: 'Estimate', photo: 'Your photo' };
const WOULD = 'In the app these come from Montana’s public data: lot lines from the Montana Cadastral parcels, house and garage outlines from the State Library’s lidar building footprints, and facts (living area, stories, roof, other buildings) from the Department of Revenue record card.';

/* The lookup returns a whole site, the way the real one would.
   Coordinates in feet: x across the lot (− left, + right, facing the house from the street), z back from the front lot line (0 = sidewalk, negative = toward the back). */
const LOOKUP = {
  '816 maywood': () => ({
    address: '816 Maywood Dr, Billings, MT 59102', found: true, area: 'West End · homes built 1973–1977 · lots about 7,500 sq ft',
    records: { built: 1975, beds: 2, baths: 1.5, lot: 7492 },
    site: {
      lot: { w: 62, d: 121 },
      house: { x0: -26, z0: -26, w: 34, d: 27, stories: 1, roof: 'gable', front: [{ k: 'win', at: .17, w: 7 }, { k: 'door', at: .40 }, { k: 'win', at: .62, w: 6 }, { k: 'win', at: .87, w: 3.2 }], entry: 'stoop', sideDoor: 'right', planter: true },
      garage: { type: 'separate', cars: 2, side: 'right', x0: 5, z0: -57, w: 24, d: 24 },
      drive: { x0: 10, w: 18, pad: true },
      fence: [[31, -40, 31, -121], [31, -121, -31, -121], [-31, -121, -31, -60], [-31, -60, -26, -55], [8, -50, 5, -57]],
      trees: [[-14, -92, 'oak', 30], [-6, -64, 'pine', 24], [-22, -8, 'none', 0]],
      colors: { siding: '#ebeae5', trim: '#4b5763', roof: '#7c7f81' }
    },
    facts: [
      ['Built', '1975', 'record'], ['Bedrooms / baths', '2 · 1.5', 'record'], ['Lot', '7,492 sq ft', 'record'],
      ['House outline', '≈ 34 × 27 ft · one story', 'est'], ['Roof', 'Gable, low pitch — check this', 'est'],
      ['Garage', 'Separate, 2-car, behind on the right', 'you'], ['Driveway', 'Along the right side to the garage, with a pad', 'you'],
      ['Fence', 'White vinyl, right side and back yard', 'you'], ['Front', 'Stoop with railings, planter bed', 'you']
    ]
  })
};
function siteFromBox(w, d, gar) {
  const garage = gar.type === 'none' ? { type: 'none' } : gar.type === 'attached' ? { type: 'attached', cars: gar.cars, side: gar.side } : { type: 'separate', cars: gar.cars, side: gar.side };
  const s = { lot: { w: 62, d: 121 }, house: { x0: gar.side === 'left' ? -w / 2 + 6 : -w / 2 - 6, z0: -25, w, d, stories: 1, roof: 'gable', entry: 'stoop', planter: false }, garage, drive: { w: 16, pad: garage.type === 'separate' }, fence: [], trees: [[-24, -90, 'oak', 28], [22, -100, 'pine', 24]], colors: { siding: '#d9cbb0', trim: '#f4f1ea', roof: '#6b5a4c' } };
  layoutGarage(s); return s;
}
function layoutGarage(s) {
  const h = s.house, g = s.garage; if (!g || g.type === 'none') { s.drive.x0 = h.x0 + h.w + 2; return; }
  g.w = g.cars === 1 ? 13 : 24; g.d = 24;
  if (g.type === 'attached') { g.x0 = g.side === 'left' ? h.x0 - g.w : h.x0 + h.w; g.z0 = h.z0; }
  else { g.x0 = g.side === 'left' ? -s.lot.w / 2 + 3 : s.lot.w / 2 - g.w - 2; g.z0 = h.z0 - h.d - 4; }
  s.drive.w = g.cars === 1 ? 11 : 18; s.drive.x0 = g.x0 + (g.w - s.drive.w) / 2; s.drive.pad = g.type === 'separate';
}
function lookupAddress(q) {
  const k = q.toLowerCase().replace(/\b(drive|dr\.?)\b/g, '').replace(/[^\w ]/g, ' ').replace(/\s+/g, ' ').trim();
  const hit = Object.keys(LOOKUP).find(x => k.startsWith(x) || k.includes(x)); if (hit) return LOOKUP[hit]();
  return { address: q.trim(), found: false, records: null, site: null, facts: [] };
}

/* ---- 3D from the site plan ---- */
function sidingMat(base, w, h) { const m = base.clone(); m.map = base.map.clone(); m.map.needsUpdate = true; m.map.repeat.set(Math.max(1, w / 2.2), Math.max(1, h / 1.1)); return m; }
function buildCustom(h) {
  const H0 = HOMES[h]; const s = H0.site; const col = H0.colors || s.colors;
  if (CUST[h]) { const old = CUST[h]; Object.entries(old.P).forEach(([k, l]) => { PARTS[k] = (PARTS[k] || []).filter(m => !l.includes(m)); }); scene.remove(old.g); old.g.traverse(o => { if (o.geometry) o.geometry.dispose(); }); }
  const g = new THREE.Group(); scene.add(g); g.visible = false; const prev = ROOT; ROOT = g;
  const ho = s.house, ga = s.garage || { type: 'none' };
  const ox = -(ho.x0 + ho.w / 2), oz = -(ho.z0 - ho.d / 2);
  const X = x => (x + ox) * FT, Z = z => (z + oz) * FT;
  const M = { siding: [], trim: [], roof: [] }; const P = {};
  const pk = (k, o) => { (P[k] = P[k] || []).push(o); (PARTS[k] = PARTS[k] || []).push(o); return o; };
  const sid = mats.siding.clone(); sid.transparent = false;
  const sm = (w, hh) => { const m = sidingMat(sid, w, hh); m.color.set(col.siding); M.siding.push(m); return m; };
  const trimD = new THREE.MeshStandardMaterial({ color: col.trim, roughness: .6 }); M.trim.push(trimD);
  const white = new THREE.MeshStandardMaterial({ color: 0xf6f5f0, roughness: .6 });
  const roofM = mats.roof.clone(); roofM.map = mats.roof.map.clone(); roofM.map.needsUpdate = true; roofM.map.repeat.set(6, 4); roofM.color.set(col.roof); roofM.transparent = false; M.roof.push(roofM);
  const vinyl = new THREE.MeshStandardMaterial({ color: 0xf7f7f4, roughness: .5 });
  const block = new THREE.MeshStandardMaterial({ color: 0x9a6a55, roughness: .9 });
  // lot, neighbours' lawns, sidewalk, street
  const L = s.lot; const lx = X(0), lz = Z(-L.d / 2);
  box((L.w + 60) * FT, .2, (L.d + 10) * FT, mats.grass, lx, -.1, lz);
  box((L.w + 60) * FT, .12, 5 * FT, mats.conc, lx, .01, Z(2.5)); box((L.w + 60) * FT, .1, 28 * FT, mats.asphalt, lx, -.04, Z(19));
  for (let i = -40; i < 41; i += 10) box(1.4, .02, .14, new THREE.MeshBasicMaterial({ color: 0xf5f0d8 }), lx + i * FT * 1.2, .02, Z(19));
  const lotLine = new THREE.LineSegments(new THREE.EdgesGeometry(new THREE.BoxGeometry(L.w * FT, .02, L.d * FT)), new THREE.LineDashedMaterial({ color: 0x9fbf86, dashSize: .5, gapSize: .35 })); lotLine.position.set(lx, .06, lz); lotLine.computeLineDistances(); g.add(lotLine);
  // driveway + pad
  const dr = s.drive; const dEnd = ga.type === 'none' ? ho.z0 - 6 : ga.z0;
  const drv = box(dr.w * FT, .12, (-dEnd) * FT, mats.conc, X(dr.x0 + dr.w / 2), .02, Z(dEnd / 2)); pk('drive', drv);
  if (ga.type === 'separate' && dr.pad) { const pz0 = ho.z0 - ho.d + 6, pz1 = ga.z0; const pad = box((ga.w + 2) * FT, .125, (pz0 - pz1) * FT, mats.conc, X(ga.x0 + ga.w / 2), .025, Z((pz0 + pz1) / 2)); pk('drive', pad); }
  // house
  const W = ho.w * FT, D = ho.d * FT, FYc = .4, wallH = ho.stories === 2 ? 5.0 : ho.stories === 1.5 ? 3.5 : 2.6;
  bbox(W + .2, FYc, D + .2, mats.found, 0, 0, 0);
  const wall = (w, x, z, ry, key) => { const m = box(w, wallH, .2, sm(w, wallH), x, FYc + wallH / 2, z); m.rotation.y = ry; pk(key, m); };
  wall(W, 0, D / 2, 0, 'sFront'); wall(W, 0, -D / 2, 0, 'sBack'); wall(D, -W / 2, 0, Math.PI / 2, 'sLeft'); wall(D, W / 2, 0, Math.PI / 2, 'sRight');
  [[-W / 2, D / 2], [W / 2, D / 2], [-W / 2, -D / 2], [W / 2, -D / 2]].forEach(([x, z]) => box(.18, wallH, .18, white, x, FYc + wallH / 2, z));
  const win = (x, y, z, ry, w, hh) => { const fr = box(w + .14, hh + .14, .07, trimD, x, y, z); fr.rotation.y = ry; const gl = box(w - .04, hh - .04, .09, mats.glass, x, y, z); gl.rotation.y = ry; const sl = box(w + .2, .06, .12, white, x, y - hh / 2 - .06, z); sl.rotation.y = ry; };
  const rows = ho.stories === 2 ? [FYc + 1.45, FYc + 3.95] : ho.stories === 1.5 ? [FYc + .8, FYc + 2.4] : [FYc + 1.45];
  const front = ho.front || [{ k: 'win', at: .2, w: 6 }, { k: 'door', at: .42 }, { k: 'win', at: .65, w: 6 }, { k: 'win', at: .86, w: 4 }];
  let doorX = 0;
  front.forEach(f => { const x = -W / 2 + f.at * W; if (f.k === 'door') { doorX = x; const d0 = box(.95, 2.05, .09, white, x, FYc + 1.03, D / 2 + .1); pk('entry', d0); box(.36, .45, .1, mats.glass, x, FYc + 1.6, D / 2 + .15); box(1.1, 2.18, .06, trimD, x, FYc + 1.08, D / 2 + .07); } else rows.forEach(y => win(x, y, D / 2 + .07, 0, f.w * FT, f.w > 5 ? 1.25 : 1.0)); });
  rows.forEach(y => { [-.3, .3].forEach(f => win(f * W, y, -D / 2 - .07, 0, 1.1, .9)); win(-W / 2 - .07, y, -D * .15, Math.PI / 2, 1.0, .9); win(W / 2 + .07, y, D * .2, Math.PI / 2, 1.0, .9); });
  // stoop with railings, planter bed, side door steps
  if (ho.entry === 'stoop' || ho.entry === 'porch') { for (let i = 0; i < 3; i++) box(1.7 - i * .0, .14, 1.2 - i * .3, mats.conc, doorX, .07 + i * .13, D / 2 + .7 - i * .15); [-.82, .82].forEach(dx => { box(.04, .9, .04, white, doorX + dx, FYc + .45, D / 2 + .9); box(.04, .04, 1.2, white, doorX + dx, FYc + .9, D / 2 + .7); }); if (ho.entry === 'porch') { [-1.1, 1.1].forEach(dx => box(.14, 2.5, .14, white, doorX + dx, FYc + 1.25, D / 2 + 1.5)); const pr = box(3.0, .12, 1.9, roofM, doorX, FYc + 2.6, D / 2 + .9); pr.rotation.x = .16; } }
  if (ho.planter) { const pw = Math.max(1, W / 2 - doorX - 1.4); const bx = doorX + .9 + pw / 2; box(pw, .45, .12, block, bx, .22, D / 2 + 1.25); box(.12, .45, 1.0, block, bx + pw / 2, .22, D / 2 + .75); box(pw, .38, .95, mats.soil, bx, .19, D / 2 + .75); }
  if (ho.sideDoor) { const sx = ho.sideDoor === 'right' ? W / 2 : -W / 2, sg = Math.sign(sx); for (let i = 0; i < 3; i++) box(.9 - i * .25, .14, 1.4, mats.conc, sx + sg * (.55 - i * .12), .07 + i * .13, -D * .28); box(.09, 2.0, .9, white, sx + sg * .1, FYc + 1.0, -D * .28); }
  // front walk from driveway to the stoop
  const walkEnd = X(dr.x0) - 0;
  box(Math.max(.5, walkEnd - doorX), .12, 1.0, mats.conc, (walkEnd + doorX) / 2, .02, D / 2 + 1.75);
  // roof
  const top = FYc + wallH, o = .5, pitch = ho.roof === 'flat' ? 0 : .3, rise = (D / 2 + o) * pitch;
  if (ho.roof === 'flat') pk('front', box(W + o * 2, .25, D + o * 2, roofM, 0, top + .12, 0));
  else if (ho.roof === 'hip') {
    const a = [-W / 2 - o, top, D / 2 + o], b = [W / 2 + o, top, D / 2 + o], c = [W / 2 + o, top, -D / 2 - o], d = [-W / 2 - o, top, -D / 2 - o]; const rx = Math.max(.2, W / 2 - D / 2), r1 = [-rx, top + rise, 0], r2 = [rx, top + rise, 0];
    const face = (pts, key) => { const pos = []; for (let i = 1; i < pts.length - 1; i++) pos.push(...pts[0], ...pts[i], ...pts[i + 1]); const geo = new THREE.BufferGeometry(); geo.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3)); const uvA = []; for (let i = 0; i < pos.length; i += 3) uvA.push(pos[i] / 1.6, pos[i + 2] / 1.6 + pos[i + 1] / 1.6); geo.setAttribute('uv', new THREE.Float32BufferAttribute(uvA, 2)); geo.computeVertexNormals(); const m = new THREE.Mesh(geo, roofM); m.castShadow = m.receiveShadow = true; roofM.side = THREE.DoubleSide; g.add(m); pk(key, m); };
    face([a, b, r2, r1], 'front'); face([c, d, r1, r2], 'back'); face([d, a, r1], 'front'); face([b, c, r2], 'back');
  } else {
    const SL = Math.hypot(D / 2 + o, rise), ang = Math.atan2(rise, D / 2 + o);
    const rF = box(W + o * 2, .14, SL, roofM, 0, top + rise / 2, (D / 2 + o) / 2); rF.rotation.x = ang; pk('front', rF);
    const rB = box(W + o * 2, .14, SL, roofM, 0, top + rise / 2, -(D / 2 + o) / 2); rB.rotation.x = -ang; pk('back', rB);
    const sh = new THREE.Shape(); sh.moveTo(-D / 2, 0); sh.lineTo(D / 2, 0); sh.lineTo(0, rise * D / 2 / (D / 2 + o)); sh.closePath();
    [-W / 2, W / 2].forEach(x => { const m = new THREE.Mesh(new THREE.ExtrudeGeometry(sh, { depth: .2, bevelEnabled: false }), sm(D, rise)); m.rotation.y = Math.PI / 2; m.position.set(x - .1, top, 0); m.castShadow = true; g.add(m); pk(x < 0 ? 'sLeft' : 'sRight', m); });
    box(W + o * 2 + .1, .1, .14, roofM, 0, top + rise + .02, 0);
  }
  [D / 2 + o, -D / 2 - o].forEach(z => box(W + o * 2, .2, .06, white, 0, top - .03, z));
  // garage
  let gx = 0, gz = 0;
  if (ga.type !== 'none') {
    const GW = ga.w * FT, GD = ga.d * FT, gh = 2.6; gx = X(ga.x0 + ga.w / 2); gz = Z(ga.z0 - ga.d / 2);
    bbox(GW, .15, GD, mats.conc, gx, 0, gz);
    const gw = (w, x, z, ry) => { const m = box(w, gh, .2, sm(w, gh), x, .15 + gh / 2, z); m.rotation.y = ry; pk('sGarage', m); };
    const fz = gz + GD / 2; gw(GW, gx, fz, 0); gw(GW, gx, gz - GD / 2, 0); gw(GD, gx - GW / 2, gz, Math.PI / 2); gw(GD, gx + GW / 2, gz, Math.PI / 2);
    const dw = ga.cars === 1 ? 2.75 : 4.9; const gd0 = box(dw, 2.15, .08, mats.gdoor, gx, .15 + 1.08, fz + .11); pk('garage', gd0); box(dw + .2, 2.28, .05, white, gx, .15 + 1.12, fz + .1);
    if (ga.type === 'separate') { box(.85, 2.0, .08, white, gx - GW / 2 + .7, 1.15, fz + .11); }
    // gable faces the street
    const gr = (GW / 2 + .35) * .42, gSL = Math.hypot(GW / 2 + .35, gr), gang = Math.atan2(gr, GW / 2 + .35);
    const r1 = box(gSL, .13, GD + .6, roofM, gx - (GW / 2 + .35) / 2, .15 + gh + gr / 2, gz); r1.rotation.z = gang; pk('garageRoof', r1);
    const r2 = box(gSL, .13, GD + .6, roofM, gx + (GW / 2 + .35) / 2, .15 + gh + gr / 2, gz); r2.rotation.z = -gang; pk('garageRoof', r2);
    const tri = new THREE.Shape(); tri.moveTo(-GW / 2, 0); tri.lineTo(GW / 2, 0); tri.lineTo(0, gr * GW / 2 / (GW / 2 + .35)); tri.closePath();
    [fz - .2, gz - GD / 2 - .0].forEach(z => { const m = new THREE.Mesh(new THREE.ExtrudeGeometry(tri, { depth: .2, bevelEnabled: false }), sm(GW, gr)); m.position.set(gx, .15 + gh, z); m.castShadow = true; g.add(m); pk('sGarage', m); });
  }
  // fence (white vinyl panels)
  (s.fence || []).forEach(([x1, z1, x2, z2]) => { const len = Math.hypot(x2 - x1, z2 - z1) * FT; const m = box(len, 1.8, .06, vinyl, X((x1 + x2) / 2), .9, Z((z1 + z2) / 2)); m.rotation.y = -Math.atan2((z2 - z1), (x2 - x1)); pk('fence', m); const n = Math.max(1, Math.round(len / 1.8)); for (let i = 0; i <= n; i++) { const t = i / n; box(.12, 1.95, .12, vinyl, X(x1 + (x2 - x1) * t), .97, Z(z1 + (z2 - z1) * t)); } });
  // trees (simple, always available)
  (s.trees || []).forEach(([x, z, kind, hf]) => { if (kind === 'none') return; const H1 = hf * FT; box(.35, H1 * .4, .35, new THREE.MeshStandardMaterial({ color: 0x6b4f36 }), X(x), H1 * .2, Z(z)); const leaf = new THREE.MeshStandardMaterial({ color: kind === 'pine' ? 0x2f5e3a : 0x4f7d3c, roughness: .9 }); if (kind === 'pine') mesh(new THREE.ConeGeometry(H1 * .22, H1 * .75, 8), leaf, X(x), H1 * .6, Z(z), g); else { mesh(new THREE.IcosahedronGeometry(H1 * .3, 1), leaf, X(x), H1 * .62, Z(z), g); mesh(new THREE.IcosahedronGeometry(H1 * .2, 1), leaf, X(x) + H1 * .18, H1 * .55, Z(z) + H1 * .1, g); } });
  ROOT = prev;
  // tags + cameras
  const T = top + rise; const tag = { sFront: [0, 1.9, D / 2 + .5], sBack: [0, 1.9, -D / 2 - .5], sLeft: [-W / 2 - .5, 1.9, 0], sRight: [W / 2 + .5, 1.9, 0], front: [0, T - rise * .3, D / 4], back: [0, T - rise * .3, -D / 4], entry: [doorX, 2.9, D / 2 + .8], living: [-W / 4, 1.3, D / 6], kitchen: [W / 5, 1.3, -D / 5], bath: [W / 3, 1.3, 0], bed: [-W / 3, 1.3, -D / 5], lawn: [X(-10), .5, Z(-95)], fyard: [X(-12), .5, Z(-12)], drive: [X(dr.x0 + dr.w / 2), .5, Z(-15)], fence: [X(31), 2.2, Z(-85)] };
  if (ga.type !== 'none') { tag.sGarage = [gx, 1.6, gz + ga.d * FT / 2 + .5]; tag.garage = [gx, 3.2, gz + ga.d * FT / 2 + .5]; tag.garageRoof = [gx, 4.2, gz]; }
  const pts = [[-W / 2, D / 2], [W / 2, -D / 2]]; if (ga.type !== 'none') pts.push([gx - ga.w * FT / 2, gz - ga.d * FT / 2], [gx + ga.w * FT / 2, gz + ga.d * FT / 2]); pts.push([X(dr.x0), Z(0)]);
  const xs = pts.map(p => p[0]), zs = pts.map(p => p[1]); const cx = (Math.min(...xs) + Math.max(...xs)) / 2, cz = (Math.min(...zs) + Math.max(...zs)) / 2; const span = Math.max(Math.max(...xs) - Math.min(...xs), Math.max(...zs) - Math.min(...zs));
  const cam = { home: [[cx + span * .55, span * .62 + 4, cz + span * .95 + 4], [cx, 1, cz]] };
  cam.sFront = [[0, 3, D / 2 + 16], [0, 1.6, D / 2]]; cam.sBack = [[0, 4, -D / 2 - 16], [0, 1.6, -D / 2]]; cam.sLeft = [[-W / 2 - 16, 3.5, 2], [-W / 2, 1.6, 0]]; cam.sRight = [[W / 2 + 16, 3.5, 2], [W / 2, 1.6, 0]];
  cam.front = [[-3, T + 9, D / 2 + 13], [0, T - 1, 0]]; cam.back = [[-3, T + 9, -D / 2 - 13], [0, T - 1, 0]]; cam.entry = [[doorX + 2, 3, D / 2 + 10], [doorX, 1.4, D / 2]];
  if (ga.type !== 'none') { cam.sGarage = [[gx + 4, 3.5, gz + 16], [gx, 1.4, gz]]; cam.garage = cam.sGarage; cam.garageRoof = [[gx + 7, 9, gz + 12], [gx, 2.8, gz]]; }
  cam.drive = [[X(dr.x0) + 12, 8, Z(-5) + 12], [X(dr.x0 + dr.w / 2), 0, Z(-30)]]; cam.lawn = [[cx + 10, 10, Z(-121) - 12], [X(-10), 0, Z(-90)]]; cam.fyard = [[X(-12) + 6, 7, Z(0) + 14], [X(-12), 0, Z(-12)]]; cam.fence = [[X(31) + 14, 6, Z(-80) + 8], [X(31), 1, Z(-85)]];
  CUST[h] = { g, M, tag, cam, P };
  return CUST[h];
}
function decorateCustom() {}
function setCustomColors(h) { const c = CUST[h]; if (!c) return; const col = HOMES[h].colors || {}; if (col.siding) c.M.siding.forEach(m => m.color.set(col.siding)); if (col.trim) c.M.trim.forEach(m => m.color.set(col.trim)); if (col.roof) c.M.roof.forEach(m => m.color.set(col.roof)); }

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
      $('#faManual').addEventListener('click', () => { res = { address: 'My house', found: false, records: null, site: null, facts: [] }; st = 'result'; redraw(); });
      $('#faOld').addEventListener('click', addHome);
    } else if (st === 'search') {
      const steps = ['Matching the address', 'Lot lines', 'Building outlines', 'Public record', 'Building your 3D model'];
      B.innerHTML = `<div class="card"><div class="k v">LOOKING UP</div><h3>${esc(q)}</h3><div class="lsteps">${steps.map((s, i) => `<div data-ls="${i}"><i></i>${s}</div>`).join('')}</div></div>`;
      res = lookupAddress(q); let i = 0; const tick = () => { const el = B.querySelector(`[data-ls="${i}"]`); if (!el) return; el.classList.add('done'); i++; if (i < steps.length) setTimeout(tick, 330); else setTimeout(() => { st = 'result'; if (fullStack.length && fullStack[fullStack.length - 1].title === 'Add a home') redraw(); }, 380); }; setTimeout(tick, 300);
    } else {
      const known = !!res.site;
      if (!known && gar.type) res.site = siteFromBox(40, 28, gar);
      const s = res.site;
      B.innerHTML = `<div class="card found"><div class="k ${res.found ? 'g' : 'v'}">${res.found ? '✓ MATCHED YOUR ADDRESS' : 'NO RECORD FOUND'}</div><h3>${esc(res.address)}</h3>${res.area ? `<p>${esc(res.area)}</p>` : ''}${s ? sitePlanSVG(s) : ''}${s ? `<div class="legend2"><span><i style="background:#d9cfe9;border-color:#6b4bd1"></i>House</span><span><i style="background:#ece6f6;border-color:#6b4bd1"></i>Garage</span><span><i style="background:#e4e2dc"></i>Concrete</span><span><i style="background:#fff;border-color:#9a9a9a"></i>Fence</span></div>` : ''}</div>
        ${res.facts.length ? `<div class="card"><div class="k v">WHAT ATLAS FOUND</div>${res.facts.map(([k, v, c]) => `<div class="fact"><span class="fk2">${esc(k)}</span><b>${esc(v)}</b><em class="sb ${c}">${SRC[c]}</em></div>`).join('')}<p class="src2">${WOULD}</p></div>` : ''}
        ${!known ? `<div class="card"><div class="k v">${res.found ? '' : 'NOTHING ON FILE — '}TWO QUICK QUESTIONS</div><div class="srow"><span>Garage</span><div class="chipset">${[['none', 'None'], ['attached', 'Attached'], ['separate', 'Separate, behind']].map(([k, l]) => `<button class="${gar.type === k ? 'on' : ''}" data-gt="${k}">${l}</button>`).join('')}</div></div>${gar.type && gar.type !== 'none' ? `<div class="srow"><span>Which side</span><div class="chipset">${['left', 'right'].map(k => `<button class="${gar.side === k ? 'on' : ''}" data-gs="${k}">${k[0].toUpperCase() + k.slice(1)}</button>`).join('')}</div></div><div class="srow"><span>Size</span><div class="chipset">${[1, 2].map(k => `<button class="${gar.cars === k ? 'on' : ''}" data-gc="${k}">${k}-car</button>`).join('')}</div></div>` : ''}<p class="mini">Atlas asks instead of guessing. You can change everything on the model next.</p></div>` : ''}
        ${s ? `<div class="card"><div class="k v">NAME IT</div><input class="inp" id="faN" value="${esc(res.found ? res.address.split(',')[0] : 'My house')}" style="margin-top:6px"></div>
        <div class="row"><button class="btn" id="faBuild" style="flex:1">Build my house</button><button class="btn g" id="faBack">Wrong house</button></div>` : `<div class="row"><button class="btn g" id="faBack">Back</button></div>`}`;
      B.querySelectorAll('[data-gt]').forEach(b => b.addEventListener('click', () => { gar.type = b.dataset.gt; res.site = null; redraw(); }));
      B.querySelectorAll('[data-gs]').forEach(b => b.addEventListener('click', () => { gar.side = b.dataset.gs; res.site = null; redraw(); }));
      B.querySelectorAll('[data-gc]').forEach(b => b.addEventListener('click', () => { gar.cars = +b.dataset.gc; res.site = null; redraw(); }));
      $('#faBack').addEventListener('click', () => { st = 'ask'; gar.type = null; redraw(); });
      const fb = $('#faBuild'); if (fb) fb.addEventListener('click', () => { const r = res.records; const id = 'h' + Date.now();
        HOMES[id] = { name: $('#faN').value.trim() || 'My house', icon: '🏡', kind: r ? `House · ${r.beds} bed · built ${r.built}` : 'House', model: 'custom', custom: true, address: res.address, records: r, facts: res.facts, site: JSON.parse(JSON.stringify(s)), colors: { ...s.colors } };
        if (r) THINGS['t' + id] = { home: id, name: 'House', place: 'Outside', hist: [{ y: r.built, t: 'House built', s: 'Public record' }] };
        buildCustom(id); save(); S.shaping = id; goHome(id); toast('Built from the site plan — check it below'); });
    }
  });
}
function sitePlanSVG(s) {
  const L = s.lot, sc = 2.1, pad = 8, W = L.w * sc, D = L.d * sc; const px = x => pad + (x + L.w / 2) * sc, pz = z => pad + 26 * sc * .3 + (-z) * sc;
  const r = (x0, z0, w, d, fill, stroke, dash) => `<rect x="${px(x0)}" y="${pz(z0)}" width="${w * sc}" height="${d * sc}" fill="${fill}" stroke="${stroke}" stroke-width="1.4" ${dash ? 'stroke-dasharray="3 2"' : ''}/>`;
  const h = s.house, g = s.garage, dr = s.drive; let o = '';
  o += `<rect x="${pad}" y="${pz(0)}" width="${W}" height="${D}" fill="#eef5e6" stroke="#9fbf86" stroke-dasharray="4 3"/>`;
  o += `<rect x="0" y="${pz(0) - 12}" width="${W + pad * 2}" height="9" fill="#d6d3cd"/><text x="${pad + 4}" y="${pz(0) - 16}" font-size="10" fill="#5d5672">Street · Maywood Dr</text>`;
  const dEnd = !g || g.type === 'none' ? h.z0 - 6 : g.z0; o += r(dr.x0, 0, dr.w, -dEnd, '#e4e2dc', 'none');
  if (g && g.type === 'separate' && dr.pad) { const pz0 = h.z0 - h.d + 6; o += r(g.x0 - 1, pz0, g.w + 2, pz0 - g.z0, '#e4e2dc', 'none'); }
  o += r(h.x0, h.z0, h.w, h.d, '#d9cfe9', '#6b4bd1');
  if (g && g.type !== 'none') o += r(g.x0, g.z0, g.w, g.d, '#ece6f6', '#6b4bd1', g.type === 'separate' ? 0 : 1);
  (s.fence || []).forEach(([x1, z1, x2, z2]) => o += `<line x1="${px(x1)}" y1="${pz(z1)}" x2="${px(x2)}" y2="${pz(z2)}" stroke="#9a9a9a" stroke-width="2.5" stroke-linecap="round"/><line x1="${px(x1)}" y1="${pz(z1)}" x2="${px(x2)}" y2="${pz(z2)}" stroke="#fff" stroke-width="1.2"/>`);
  (s.trees || []).forEach(([x, z, k, hf]) => { if (k !== 'none') o += `<circle cx="${px(x)}" cy="${pz(z)}" r="${hf * sc * .28}" fill="${k === 'pine' ? '#5f8c62' : '#7fae6a'}" opacity=".8"/>`; });
  o += `<text x="${px(h.x0 + h.w / 2)}" y="${pz(h.z0 - h.d / 2) + 4}" font-size="10" text-anchor="middle" fill="#3b2f6b">House</text>`;
  if (g && g.type !== 'none') o += `<text x="${px(g.x0 + g.w / 2)}" y="${pz(g.z0 - g.d / 2) + 4}" font-size="10" text-anchor="middle" fill="#3b2f6b">Garage</text>`;
  return `<svg class="fp" viewBox="0 0 ${W + pad * 2} ${D + pz(0) + pad}" role="img" aria-label="Site plan: lot, house, garage, driveway and fence">${o}</svg>`;
}

/* ---- adjust the model (lives in the bottom card) ---- */
function shapeHTML(h) {
  const s = HOMES[h].site, ho = s.house, g = s.garage || { type: 'none' };
  const c = (on, attr, l) => `<button class="${on ? 'on' : ''}" ${attr}>${l}</button>`;
  return `<div class="shape"><div class="srow"><span>Stories</span><div class="chipset">${[[1, 'One'], [1.5, 'Split-level'], [2, 'Two']].map(([v, l]) => c(ho.stories === v, `data-hk="stories" data-hv="${v}"`, l)).join('')}</div></div>
    <div class="srow"><span>Roof</span><div class="chipset">${[['gable', 'Gable'], ['hip', 'Hip'], ['flat', 'Flat']].map(([v, l]) => c(ho.roof === v, `data-hk="roof" data-hv="${v}"`, l)).join('')}</div></div>
    <div class="srow"><span>Front entry</span><div class="chipset">${[['stoop', 'Steps'], ['porch', 'Covered porch'], ['none', 'None']].map(([v, l]) => c(ho.entry === v, `data-hk="entry" data-hv="${v}"`, l)).join('')}</div></div>
    <div class="srow"><span>Garage</span><div class="chipset">${[['none', 'None'], ['attached', 'Attached'], ['separate', 'Separate']].map(([v, l]) => c(g.type === v, `data-gk="type" data-gv="${v}"`, l)).join('')}</div></div>
    ${g.type !== 'none' ? `<div class="srow"><span>Garage side</span><div class="chipset">${['left', 'right'].map(v => c(g.side === v, `data-gk="side" data-gv="${v}"`, v[0].toUpperCase() + v.slice(1))).join('')}</div></div><div class="srow"><span>Garage size</span><div class="chipset">${[1, 2].map(v => c(g.cars === v, `data-gk="cars" data-gv="${v}"`, v + '-car')).join('')}</div></div>` : ''}
    <div class="srow"><span>House width</span><input type="range" min="24" max="70" value="${ho.w}" data-sr="w" aria-label="House width in feet"><b>${ho.w} ft</b></div>
    <div class="srow"><span>House depth</span><input type="range" min="20" max="50" value="${ho.d}" data-sr="d" aria-label="House depth in feet"><b>${ho.d} ft</b></div>
    <div class="row" style="margin-top:12px"><button class="btn" id="shOk" style="flex:1">Looks like my house</button></div>
    <p class="mini">Close enough is fine — it’s a map, not a blueprint. Change it any time with “Edit shape” on the home card.</p></div>`;
}
function bindShape(B, h) {
  const s = HOMES[h].site; let t; const val = v => v === 'true' ? true : v === 'false' ? false : isNaN(+v) ? v : +v;
  const mark = k => { const f = (HOMES[h].facts || []).find(x => x[0] === k); if (f) f[2] = 'you'; };
  const rebuild = () => { buildCustom(h); setCustomColors(h); applyView(); buildTags(); };
  B.querySelectorAll('[data-hk]').forEach(b => b.addEventListener('click', () => { s.house[b.dataset.hk] = val(b.dataset.hv); if (b.dataset.hk === 'roof') mark('Roof'); rebuild(); save(); render(); buzz(6); }));
  B.querySelectorAll('[data-gk]').forEach(b => b.addEventListener('click', () => { s.garage = s.garage && s.garage.type ? s.garage : { type: 'none', cars: 2, side: 'right' }; s.garage[b.dataset.gk] = val(b.dataset.gv); if (!s.garage.cars) s.garage.cars = 2; if (!s.garage.side) s.garage.side = 'right'; layoutGarage(s); mark('Garage'); rebuild(); save(); flyTo(camOf(), .8); render(); buzz(6); }));
  B.querySelectorAll('[data-sr]').forEach(r => { r.addEventListener('input', () => { const k = r.dataset.sr, old = s.house[k]; s.house[k] = +r.value; if (k === 'w' && s.house.x0 != null) s.house.x0 -= (s.house.w - old) / 2; if (s.garage && s.garage.type === 'attached') layoutGarage(s); r.nextElementSibling.textContent = r.value + ' ft'; mark('House outline'); clearTimeout(t); t = setTimeout(rebuild, 60); }); r.addEventListener('change', () => save()); });
  $('#shOk').addEventListener('click', () => { S.shaping = null; save(); render(); photoPrompt(h); });
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
