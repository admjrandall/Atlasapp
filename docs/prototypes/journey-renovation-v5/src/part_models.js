/* ======================================================================
   x-ray extras · models
   ====================================================================== */
const studs = grp(house, 0, FY, 0); studs.visible = false;
const studM = new THREE.MeshBasicMaterial({ color: 0xd9b77e });
for (let x = -3.85; x <= 3.9; x += .4) box(.05, H, .08, studM, x, H / 2, -3.0, studs);
box(5, .035, .05, new THREE.MeshBasicMaterial({ color: 0xd94a3d }), 1.4, 1.0, -2.98, studs); box(.035, .8, .05, new THREE.MeshBasicMaterial({ color: 0xd94a3d }), 3.0, .6, -2.98, studs); box(3.6, .035, .05, new THREE.MeshBasicMaterial({ color: 0x2f6fd9 }), 2.0, 1.25, -2.98, studs); box(.07, H, .07, new THREE.MeshBasicMaterial({ color: 0x9aa3ad }), 3.4, H / 2, -2.96, studs);
const sheath = grp(house, 0, FY, 0); sheath.visible = false;
[[8.0, .06, 0, 2.86], [8.0, .06, 0, -2.86], [.06, 5.8, -3.86, 0]].forEach(([w, d, x, z]) => bbox(w, H, d, mats.osb, x, 0, z, sheath));
const footings = grp(cabinRoot); footings.visible = false; for (let x = -4.2; x <= 4.2; x += 2.1) for (const z of [2.4, 6.0]) mesh(new THREE.CylinderGeometry(.25, .3, 1.2, 10), mats.conc, x, -.5, z, footings);

const GLB = {}; const loader = new GLTFLoader();
function b64(s) { const bin = atob(s); const u = new Uint8Array(bin.length); for (let i = 0; i < bin.length; i++) u[i] = bin.charCodeAt(i); return u.buffer; }
async function loadAll() { await Promise.all(Object.keys(MODELS).map(k => new Promise((res, rej) => loader.parse(b64(MODELS[k]), './', g => { GLB[k] = g; res(); }, rej)))); }
function inst(name, size, by = 'h', parent) { const o = GLB[name].scene.clone(true); o.updateMatrixWorld(true); const b = new THREE.Box3().setFromObject(o); const d = b.getSize(new THREE.Vector3()); o.scale.multiplyScalar(size / (by === 'h' ? d.y : d.x)); o.updateMatrixWorld(true); const b2 = new THREE.Box3().setFromObject(o); const c = b2.getCenter(new THREE.Vector3()); o.position.set(-c.x, -b2.min.y, -c.z); o.traverse(m => { if (m.isMesh) m.castShadow = m.receiveShadow = true; }); const w = new THREE.Group(); w.add(o); (parent || mainRoot).add(w); return w; }
function put(n, s, x, y, z, ry = 0, by = 'h', parent) { const o = inst(n, s, by, parent); o.position.set(x, y, z); o.rotation.y = ry; return o; }
const FENCE = [];
function furnish() {
  const Fu = grp(house, 0, FY + .04, 0);
  put('rug_rectangle_stripes_A', 2.2, -2.0, 0, 1.3, 0, 'w', Fu); put('couch_pillows', .8, -2.0, 0, .35, 0, 'h', Fu); put('table_low', .34, -2.0, 0, 1.3, 0, 'h', Fu); put('armchair', .78, -3.2, 0, 2.2, 2.3, 'h', Fu); put('lamp_standing', 1.4, -3.5, 0, .3, 0, 'h', Fu); put('shelf_B_large_decorated', .5, -.9, 0, 2.6, Math.PI, 'h', Fu);
  put('bed_double_A', .7, -2.0, 0, -1.7, 0, 'h', Fu); put('cabinet_small', .6, -.6, 0, -2.6, 0, 'h', Fu); put('cactus_medium_A', .45, .8, 0, 2.3, 0, 'h', Fu);
  [[1.5, -.3], [2.3, -.3]].forEach(([x, z]) => put('chair_A_wood', .85, x, 0, z, Math.PI, 'h', Fu));
  put('car_stationwagon', 1.5, 5.8, 0, 5.2, Math.PI, 'h', mainRoot);
  [[-8.5, 6.5, 'tree_oak', 3.6], [9.2, 6, 'tree_fat', 3.0], [-9, -8.6, 'tree_fat', 3.4], [8.8, -9.5, 'tree_oak', 3.8], [-6.5, -10.3, 'tree_oak', 3.2]].forEach(([x, z, n, h]) => put(n, h, x, 0, z));
  [[-3.4, 3.7], [-1.8, 3.7], [2.6, 3.6], [3.4, 3.6], [-4.6, 2.0], [-4.6, 0]].forEach(([x, z]) => put('bush', .6, x, 0, z));
  beds.forEach(b => { for (let i = -1; i <= 1; i++) put('bush', .55, b.position.x + i * 1.0, .2, b.position.z); });
  for (let x = -10.5; x <= 10.5; x += 1.5) FENCE.push(put('fence', 1.5, x, 0, -10.6, 0, 'w'));
  for (let z = -9.8; z <= -3.8; z += 1.5) { FENCE.push(put('fence', 1.5, -11.2, 0, z, Math.PI / 2, 'w')); FENCE.push(put('fence', 1.5, 11.2, 0, z, Math.PI / 2, 'w')); }
  put('bench', .6, 6.5, 0, -6.2, -Math.PI / 2); applyLook();
  put('bench', .6, -2.5, 1.02, 5.4, 0, 'h', cabinRoot); put('armchair', .7, 2.4, 1.02, 4.0, -.4, 'h', cabinRoot);
  put('couch_pillows', .7, -1, .1, 2.6, Math.PI, 'h', unitG); put('rug_rectangle_stripes_A', 2, -1, .1, 1.2, 0, 'w', unitG); put('bed_double_A', .62, 1.4, .1, -3.2, 0, 'h', unitG); put('table_medium', .7, -1.6, .1, -2.6, 0, 'h', unitG); put('lamp_standing', 1.3, -2.4, .1, 3.8, 0, 'h', unitG);
}

