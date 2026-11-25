
/* ======================================================================
   tags · pins · popover · highlight
   ====================================================================== */
const AREA_TAG = { sFront: [0, 2.6, 3.4], sLeft: [-4.5, 2.6, 0], sBack: [0, 2.6, -3.4], sGarage: [7.9, 2.2, -.5], bath: [2.6, 1.6, 1.8] };
const PROJ_TAG = { kitchen: [2.6, FY + H + 2.3, -.6], roof: [-1.2, FY + H + 2.4, .6], siding: [-4.5, 1.8, 1.8], yard: [0, 1.1, -6.6], heater: [6.75, 3.3, -2.35], deck: [0, 2.4, 4.4], stove: [1.3, 7.0, -2.4], paint: [4, FL * 3 + 2.4, 0] };
const areaPos = k => AREA_TAG[k] || TAGPOS[k] || [0, 2, 0];
function projPos(q) { if (PROJ_TAG[q.id]) return PROJ_TAG[q.id]; const k = Object.keys(q.areas)[0]; const p = areaPos(k).slice(); p[1] += .6; return p; }
const tagsEl = $('#tags'); let tagEls = [], pinEls = [];
function mk(cls, html, fn, label) { const el = document.createElement('button'); el.className = cls; el.innerHTML = html; if (label) el.setAttribute('aria-label', label); el.addEventListener('click', e => { e.stopPropagation(); fn(); }); tagsEl.appendChild(el); return el; }
function buildTags() {
  tagEls.forEach(t => t.el.remove()); pinEls.forEach(t => t.el.remove()); tagEls = []; pinEls = [];
  const p = cur();
  if (!p) {
    if (S.time && (S.year ?? TODAY) < TODAY) return;
    const seen = {};
    projOf(S.home).filter(q => q.status !== 'done').forEach(q => { const pos = projPos(q).slice(); const key = pos.map(v => Math.round(v)).join(','); seen[key] = (seen[key] || 0) + 1; pos[1] += (seen[key] - 1) * .9; const n = q.needs.length; tagEls.push({ el: mk('tag' + (POP && POP.id === q.id ? ' sel' : ''), `<span class="n ${n ? '' : 'z'}">${n || '✓'}</span><span>${q.icon} ${esc(q.name)}</span>`, () => POP && POP.id === q.id ? closePop() : openPop({ kind: 'proj', id: q.id, pos })), pos }); });
    return;
  }
  Object.entries(p.areas).forEach(([k, name]) => { const n = p.needs.filter(x => x.area === k).length; tagEls.push({ el: mk('tag' + (S.area === k ? ' sel' : ''), `<span class="n ${n ? '' : 'z'}">${n || '✓'}</span><span>${esc(name)}</span>`, () => focusArea(k, true)), pos: areaPos(k) }); });
  p.photos.forEach(ph => pinEls.push({ el: mk('pin', ph.src ? `<img src="${ph.src}" alt="" style="width:100%;height:100%;object-fit:cover;border-radius:17px">` : '📷', () => openPhoto(p, ph), ph.t), pos: ph.pos }));
  if (p.id === 'roof') pinEls.push({ el: mk('pin', '!', () => toast('Sample: the roofer flagged a soft spot here — that’s what the per-sheet price covers'), 'Soft spot'), pos: rotPos });
}
const pv = new THREE.Vector3();
function proj2d(pos) { pv.set(...pos).project(camera); const r = canvas.getBoundingClientRect(); return { x: (pv.x + 1) / 2 * r.width, y: (1 - pv.y) / 2 * r.height, z: pv.z, w: r.width, h: r.height }; }
function place(el, pos) { const q = proj2d(pos); if (q.z > 1 || q.x < -40 || q.x > q.w + 40 || q.y < $('.hd').offsetHeight - 4 || q.y > q.h - sheetCover() - 8) { el.style.display = 'none'; return; } el.style.display = ''; el.style.left = q.x + 'px'; el.style.top = q.y + 'px'; }
function sheetCover() { return $('#sheet').offsetHeight; }
function placeAll() { tagEls.forEach(t => S.xray ? (t.el.style.display = 'none') : place(t.el, t.pos)); pinEls.forEach(t => S.xray ? place(t.el, t.pos) : (t.el.style.display = 'none')); declutter(); placePop(); }
function declutter() { const vis = tagEls.filter(t => t.el.style.display !== 'none').map(t => ({ el: t.el, x: parseFloat(t.el.style.left), y: parseFloat(t.el.style.top), w: t.el.offsetWidth + 6, h: t.el.offsetHeight + 4 })).sort((a, b) => a.y - b.y); for (let i = 0; i < vis.length; i++) for (let j = 0; j < i; j++) { const A = vis[j], Bq = vis[i]; if (Math.abs(A.x - Bq.x) < (A.w + Bq.w) / 2 && Math.abs(A.y - Bq.y) < (A.h + Bq.h) / 2) { Bq.y = A.y + (A.h + Bq.h) / 2; } } vis.forEach(v => v.el.style.top = v.y + 'px'); }
const glow = new THREE.Group(); scene.add(glow); const glowMat = new THREE.LineBasicMaterial({ color: 0xff9a3c, transparent: true, opacity: .9, depthTest: false });
function highlight() { glow.clear(); const p = cur(); const keys = p ? Object.keys(p.areas).filter(k => !S.area || S.area === k) : (POP && POP.kind === 'proj' ? Object.keys(P(POP.id).areas) : []); keys.forEach(k => (PARTS[k] || []).forEach(m => { if (!isShown(m)) return; m.updateMatrixWorld(true); const e = new THREE.LineSegments(new THREE.EdgesGeometry(m.geometry, 30), glowMat); e.matrixAutoUpdate = false; e.matrix.copy(m.matrixWorld); e.renderOrder = 5; glow.add(e); })); }
function isShown(o) { while (o) { if (!o.visible) return false; o = o.parent; } return true; }

/* popover: a small card where you tapped */
let POP = null; const popEl = $('#pop');
function openPop(o) { POP = o; drawPop(); buildTags(); highlight(); buzz(6); arm(); }
function closePop() { if (!POP) return; POP = null; popEl.classList.remove('on'); buildTags(); highlight(); render(); }
function lastDone(th) { const d = PROJ.filter(q => q.thing === th && q.status === 'done').sort((a, b) => b.year - a.year)[0]; if (d) return `${d.name} · ${d.year}`; const h = (THINGS[th] || { hist: [] }).hist.slice(-1)[0]; return h ? `${h.t} · ${h.y}` : ''; }
function drawPop() {
  if (!POP) { popEl.classList.remove('on'); return; }
  if (POP.kind === 'proj') {
    const q = P(POP.id); if (!q) { POP = null; popEl.classList.remove('on'); return; } const m = moneyOf(q), n = q.needs.length;
    const meta = q.status === 'done' ? `Finished ${q.year}${m.cost ? ' · ' + money(m.cost) : ''}` : q.status === 'planned' ? `Planned ${q.year} · ${q.stops[0]}` : `Step ${Math.min(q.here + 1, q.stops.length)} of ${q.stops.length} · ${q.stops[Math.min(q.here, q.stops.length - 1)]}`;
    const last = lastDone(q.thing);
    popEl.innerHTML = `<b>${q.icon} ${esc(q.name)}</b><div class="m">${esc(meta)}</div>${n ? `<div class="pn"><span class="n">${n}</span>${esc(q.needs[0].title)}${n > 1 ? ` <small>+${n - 1} more</small>` : ''}</div>` : ''}${q.status !== 'done' && q.budget ? `<div class="pn">💵 ${money(Math.max(0, m.left))} of ${money(q.budget)} left</div>` : ''}${last ? `<div class="pn">🕓 Last done: ${esc(last)}</div>` : ''}<div class="row"><button class="btn" data-po="open">Open</button><button class="btn g" data-po="x">Close</button></div>`;
  } else {
    const p = cur(); const k = POP.k; if (!p || !p.areas[k]) { POP = null; popEl.classList.remove('on'); return; }
    const n = p.needs.filter(x => x.area === k).length, ph = p.photos.filter(x => x.area === k).length, ag = p.agreed.filter(x => x.area === k).length;
    popEl.innerHTML = `<b>📍 ${esc(p.areas[k])}</b><div class="m">${n} open · ${ag} agreed · ${ph} photo${ph === 1 ? '' : 's'} · the card below shows only this</div><div class="row"><button class="btn" data-po="add">＋ Add here</button><button class="btn g" data-po="all">Show all</button></div>`;
  }
  popEl.querySelectorAll('[data-po]').forEach(b => b.addEventListener('click', e => { e.stopPropagation(); const a = b.dataset.po; const o = POP;
    if (a === 'open') { POP = null; popEl.classList.remove('on'); go(o.id); }
    if (a === 'x') closePop();
    if (a === 'add') { openAdd({ proj: S.p, area: o.k }); }
    if (a === 'all') { POP = null; popEl.classList.remove('on'); focusArea(S.area); } }));
  popEl.classList.add('on'); placePop();
}
function placePop() {
  if (!POP) return; const q = proj2d(POP.pos); const top = $('.hd').offsetHeight, bot = q.h - sheetCover();
  if (q.z > 1 || q.y < top - 10 || q.y > bot + 10) { popEl.style.visibility = 'hidden'; return; } popEl.style.visibility = '';
  const ph = popEl.offsetHeight || 140; const below = q.y - ph - 18 < top; popEl.classList.toggle('below', below);
  const x = clamp(q.x, 133, q.w - 133); popEl.style.left = x + 'px'; popEl.style.top = Math.min(q.y, bot - (below ? ph + 18 : 0)) + 'px'; popEl.style.setProperty('--ax', (q.x - x) + 'px');
}

/* ======================================================================
   the house through time
   ====================================================================== */
const TODAY = 2026;
const LOOK0 = { roof: '#5a5f69', oldRoof: '#a0724f', siding: '#d6c49c', trim: '#f2ede2' };
const hexOf = v => { const c = Object.values(COLOURS).flat().find(x => v && v.startsWith(x.n)); return c && c.hex; };
const fieldOf = (q, k) => q && (q.details.find(f => f.k === k) || {}).v;
const effYear = q => q.status === 'done' ? q.year : q.status === 'active' ? TODAY + 1 : q.year;
function applyLook() {
  const y = S.year ?? TODAY, p = cur(), timeOn = S.year != null;
  const mainH = S.home === 'main'; const roofP = mainH && P('roof'), sidP = mainH && P('siding'), yardP = mainH && P('yard'); const pid = p && p.id;
  let rc = mainH && y < 2009 ? LOOK0.oldRoof : LOOK0.roof;
  if (roofP && (pid === 'roof' || (timeOn && y >= effYear(roofP)))) rc = hexOf(fieldOf(roofP, 'Colour')) || rc;
  mats.roof.color.set(rc);
  let sc = LOOK0.siding, tc = LOOK0.trim;
  if (sidP && (pid === 'siding' || (timeOn && y >= effYear(sidP)))) { sc = hexOf(fieldOf(sidP, 'Colour')) || sc; tc = hexOf(fieldOf(sidP, 'Trim colour')) || tc; }
  mats.siding.color.set(sc); mats.trim.color.set(tc);
  FENCE.forEach(f => f.visible = !mainH || y >= 2019);
  const pat = pid === 'yard' || (timeOn && yardP && y >= effYear(yardP)); patio.visible = patioEdge.visible = !!pat;
  const gn = $('#ghostnote'); let note = '';
  if (timeOn && y > TODAY) note = `Planned — not real yet · ${y}`;
  else if (p && p.status !== 'done' && ['roof', 'siding', 'yard'].includes(pid)) note = 'Showing your choices — not built yet';
  gn.textContent = note; gn.classList.toggle('on', !!note);
}
const previewAll = applyLook;
function events(h) {
  const ev = [];
  Object.entries(THINGS).filter(([, t]) => t.home === h).forEach(([k, t]) => t.hist.forEach(r => ev.push({ y: r.y, t: r.t, s: r.s, thing: k, tn: t.name })));
  projOf(h).forEach(q => { if (q.status === 'done' && (THINGS[q.thing] || { hist: [] }).hist.some(r => r.y === q.year)) return; ev.push({ y: effYear(q), t: q.name + (q.status === 'done' ? '' : q.status === 'active' ? ' · finishing' : ' · planned'), s: q.where, id: q.id, thing: q.thing, tn: (THINGS[q.thing] || {}).name, fut: q.status !== 'done' }); });
  return ev.sort((a, b) => a.y - b.y);
}
function yearLabel(y) { const ev = events(S.home).filter(e => e.y === y); if (ev.length) return ev.map(e => e.t).join(' · '); if (y === TODAY) return 'Today'; return y > TODAY ? 'nothing else planned yet' : 'No changes recorded'; }
function buildTime() {
  const T = $('#timebar'); T.classList.toggle('on', S.time); $('#timeB').classList.toggle('on', S.time); $('#timeB').setAttribute('aria-pressed', S.time);
  if (!S.time) { T.innerHTML = ''; return; }
  const ev = events(S.home); const y0 = Math.min(TODAY, ...ev.map(e => e.y)), y1 = TODAY + 2, y = S.year ?? TODAY;
  const yrs = [...new Set(ev.map(e => e.y).concat(TODAY))];
  T.innerHTML = `<div class="yr"><b id="tmY">${y}</b><span id="tmL"></span><button class="xb" id="tmX" aria-label="Close the time view">✕</button></div><input type="range" id="tmR" min="${y0}" max="${y1}" step="1" value="${y}" aria-label="Year"><div class="ticks">${yrs.map(v => `<i class="${v > TODAY ? 'fut' : ''}${v === TODAY ? ' now' : ''}" data-y="${v}" title="${v}" style="left:calc(9px + (100% - 18px) * ${(v - y0) / (y1 - y0)})"></i>`).join('')}</div>`;
  const upd = () => { const v = +$('#tmR').value; S.year = v; $('#tmY').textContent = v; const lb = yearLabel(v); $('#tmL').textContent = v === TODAY ? (lb === 'Today' ? 'Today' : 'Today · ' + lb) : (v > TODAY ? 'Planned · ' : '') + lb; applyLook(); buildTags(); };
  $('#tmR').addEventListener('input', () => { const v = +$('#tmR').value; const near = yrs.find(e => Math.abs(e - v) <= 1 && e !== v); if (near && !$('#tmR').dataset.free) { /* gentle snap */ } upd(); });
  $('#tmR').addEventListener('change', () => { buzz(6); render(); });
  T.querySelectorAll('[data-y]').forEach(i => i.addEventListener('click', () => { $('#tmR').value = i.dataset.y; upd(); render(); }));
  $('#tmX').addEventListener('click', () => setTime(false));
  upd();
}
function setTime(on) { if (on && S.p) go(null); S.time = on; S.year = on ? TODAY : null; closePop(); buildTime(); applyLook(); buildTags(); render(); resize(); if (on) arm(); }
function jumpYear(y) { if (!S.time) setTime(true); $('#tmR').value = y; $('#tmR').dispatchEvent(new Event('input')); render(); }

/* ======================================================================
   views & camera
   ====================================================================== */
const CAM = { main: [[16, 15, 21], [1, .5, -1.5]], cabin: [[13, 9, 17], [0, 2.4, 0]], condo: [[19, 15, 21], [1.5, 6, 0]], rooms: [[7.5, 14, 11], [.6, 0, .2]], roof: [[-10, 11, 13], [0, 3.2, 0]], siding: [[-12, 5.5, 12.5], [.5, 1.8, 0]], yard: [[11, 11, -18], [0, 0, -6.5]], item: [[6.4, 3.4, 7.5], [6.75, 1.1, -2.3]], deck: [[9, 6, 14], [0, 1.5, 3]], condoUnit: [[11, 15, 11], [4, 8, 0]] };
const AREACAM = { kitchen: [[6, 9, 7], [2, 0, -1.2]], living: [[-6, 9, 9], [-2, 0, 1.2]], entry: [[3, 8, 9], [.7, 0, 1.7]], bath: [[5, 8, 8], [2.6, 0, 1.8]], front: [[-4, 9, 14], [-.5, 3.4, 1.5]], back: [[-4, 10, -13], [-.5, 3.4, -1.5]], garageRoof: [[12, 9, 6], [5.8, 3, -.5]], chimney: [[-8, 9, -9], [-2.4, 4.4, -1.6]], sFront: [[-3, 3.5, 12], [0, 1.6, 3]], sLeft: [[-12, 3.5, 2], [-4, 1.6, 0]], sBack: [[-3, 4, -12], [0, 1.6, -3]], sGarage: [[13, 3.5, 3], [7.5, 1.4, -.5]], patio: [[6, 7, -15], [0, 0, -5.6]], beds: [[4, 6, -3], [0, 0, -9.6]], drain: [[-3, 6, -14], [-9.8, 0, -6]], lawn: [[11, 6, -11], [4.5, 0, -7.2]], wh: [[6.5, 2.9, 5.2], [6.75, 1.1, -2.35]], deck: [[7, 5, 12], [0, 1, 4]], stairs: [[9, 4, 9], [5.2, .6, 4.6]], stove: [[6, 9, 4], [1.3, 5.2, -2.4]], unit: [[10, 14, 9], [4, 8, 0]] };
function viewOf(p) { if (!p) return modelOf(S.home); return p.view === 'condo' ? 'condoUnit' : p.view; }
function applyView() {
  const mo = modelOf(S.home); mainRoot.visible = mo === 'main'; cabinRoot.visible = mo === 'cabin'; condoRoot.visible = mo === 'condo';
  const p = cur(); const v = p ? p.view : 'home'; const cut = v === 'rooms';
  extWalls.scale.y = inWalls.scale.y = cut ? 1.15 / H : 1;
  roof.visible = !cut; wins.visible = !cut; porch.children[porch.children.length - 1].visible = !cut;
  const it = v === 'item'; groof.visible = !it; gd.visible = !it; gFront.visible = !it; gdTrim.visible = !it; gFront.scale.y = 1;
  const x = S.xray;
  mats.siding.opacity = x && (v === 'siding' || v === 'rooms') ? .16 : 1; mats.siding.depthWrite = !(x && (v === 'siding' || v === 'rooms'));
  mats.paint.transparent = x && v === 'rooms'; mats.paint.opacity = x && v === 'rooms' ? .2 : 1; mats.paint.depthWrite = !(x && v === 'rooms'); mats.paint.needsUpdate = true;
  mats.roof.opacity = x && v === 'roof' ? .18 : 1; mats.roof.depthWrite = !(x && v === 'roof');
  mats.grass.opacity = x && (v === 'yard' || v === 'deck') ? .45 : 1; mats.grass.depthWrite = !(x && (v === 'yard' || v === 'deck'));
  studs.visible = x && v === 'rooms'; sheath.visible = x && v === 'siding'; deck.visible = x && v === 'roof'; under.visible = x && v === 'yard'; footings.visible = x && v === 'deck';
  $('#xray').classList.toggle('on', x); $('#xray').setAttribute('aria-pressed', x);
  applyLook(); highlight();
}
let fly = null;
function flyTo(c, d = 1.1) { const t1 = new THREE.Vector3(...c[1]); const off = new THREE.Vector3(...c[0]).sub(t1); off.multiplyScalar(camera.aspect > 1.2 ? 1.22 : clamp(.72 / Math.max(.3, camera.aspect), 1, 1.75)); fly = { p0: camera.position.clone(), t0: controls.target.clone(), p1: t1.clone().add(off), t1, s: performance.now(), d: d * 1000 }; }
const VO = { cur: null, tgt: 0 };
function viewOffTarget() { const R = $('#map').getBoundingClientRect().height; const top = $('.hd').offsetHeight - 6; const sh = sheetState === 'peek' ? sheetH('peek') : Math.min(sheetH('half'), R * .62); return (sh - top) / 2; }
function applyVO(v) { const r = $('#map').getBoundingClientRect(); camera.setViewOffset(r.width, r.height, 0, v, r.width, r.height); camera.updateProjectionMatrix(); }
function resize() {
  const r = $('#map').getBoundingClientRect(); if (!r.width) return; renderer.setSize(r.width, r.height, false); camera.aspect = r.width / r.height; camera.fov = r.width < r.height ? 46 : 36;
  VO.tgt = viewOffTarget(); if (VO.cur == null) VO.cur = VO.tgt; applyVO(VO.cur);
  document.documentElement.style.setProperty('--shpx', $('#sheet').offsetHeight + 'px');
}
addEventListener('resize', () => { setSheet(sheetState, true); resize(); }); new ResizeObserver(resize).observe($('#sheet')); new ResizeObserver(resize).observe($('.hd'));

/* ======================================================================
   bottom card: peek · half · full (drag or tap the handle) — same on every screen
   ====================================================================== */
let sheetState = 'half';
function sheetH(st) { const R = $('#reno').getBoundingClientRect().height; const peek = $('#grabzone').offsetHeight + $('#shead').offsetHeight + $('#stepper').offsetHeight + ($('#areachip').classList.contains('on') ? $('#areachip').offsetHeight : 0) + ($('#four').style.display === 'none' ? 0 : $('#four').offsetHeight) + 6; return st === 'peek' ? peek : st === 'full' ? R : Math.round(R * (R > 760 ? .46 : .5)); }
function setSheet(st, quiet) { sheetState = st; $('#sheet').style.setProperty('--sh', sheetH(st) + 'px'); $('#sheet').dataset.st = st; $('#ghostnote').style.visibility = st === 'full' ? 'hidden' : ''; $('#grabzone').setAttribute('aria-label', st === 'full' ? 'Show the map' : 'Make the card bigger'); VO.tgt = viewOffTarget(); if (st === 'full') arm(); if (!quiet) buzz(6); }
(() => { const sh = $('#sheet'); let st = null;
  const begin = (e, lazy) => { if (e.button > 0) return; if (!lazy && e.target.closest('button')) return; if (lazy && (sheetState === 'full' || e.target.closest('input,select,textarea,.ba'))) return; st = { y0: e.clientY, h0: sh.offsetHeight, t0: performance.now(), moved: false, lazy, id: e.pointerId, el: e.currentTarget, cap: false }; if (!lazy) { st.cap = true; sh.classList.add('drag'); e.currentTarget.setPointerCapture(e.pointerId); } };
  const move = e => { if (!st) return; const dy = e.clientY - st.y0; if (Math.abs(dy) > 6) st.moved = true; if (!st.moved) return; if (!st.cap) { st.cap = true; sh.classList.add('drag'); try { st.el.setPointerCapture(st.id); } catch (x) {} } sh.style.setProperty('--sh', clamp(st.h0 - dy, sheetH('peek'), sheetH('full')) + 'px'); };
  const end = e => { if (!st) return; const s = st; st = null; sh.classList.remove('drag');
    if (!s.moved) { if (s.lazy) return; if (s.el.id === 'grabzone') setSheet(sheetState === 'peek' ? 'half' : sheetState === 'half' ? 'full' : 'half'); else if (sheetState === 'peek') setSheet('half'); return; }
    if (s.lazy) { const kill = ev => { ev.stopPropagation(); ev.preventDefault(); }; addEventListener('click', kill, { capture: true, once: true }); setTimeout(() => removeEventListener('click', kill, { capture: true }), 60); }
    const h = sh.offsetHeight, v = (e.clientY - s.y0) / Math.max(1, performance.now() - s.t0); let best = ['peek', 'half', 'full'].map(x => [x, Math.abs(sheetH(x) - h)]).sort((a, b) => a[1] - b[1])[0][0]; if (v > .5) best = sheetState === 'full' ? 'half' : 'peek'; if (v < -.5) best = sheetState === 'peek' ? 'half' : 'full'; setSheet(best); };
  [['grabzone', 0], ['shead', 0], ['stepper', 0], ['four', 1], ['areachip', 1], ['sbody', 1]].forEach(([id, lazy]) => { const el = $('#' + id); el.addEventListener('pointerdown', e => begin(e, !!lazy)); el.addEventListener('pointermove', move); el.addEventListener('pointerup', end); el.addEventListener('pointercancel', end); });
  $('#sbody').addEventListener('wheel', e => { if (sheetState !== 'full' && e.deltaY > 4) { e.preventDefault(); setSheet('full'); } }, { passive: false });
  $('#mapB').addEventListener('click', () => setSheet('half'));
})();

/* ======================================================================
   navigation + back (header ←, Escape, phone back)
   ====================================================================== */
let modalOpen = false; const fullStack = []; let armed = false;
function depth() { return (modalOpen ? 1 : 0) + fullStack.length + (POP ? 1 : 0) + (sheetState === 'full' ? 1 : 0) + (S.time ? 1 : 0) + (S.area ? 1 : 0) + (S.p ? 1 : 0); }
function arm() { try { if (depth() > 0 && !armed) { history.pushState({ atlas: 1 }, ''); armed = true; } } catch (e) {} }
function navBack() { if (modalOpen) closeModal(); else if (fullStack.length) popFull(); else if (POP) closePop(); else if (sheetState === 'full') setSheet('half'); else if (S.time) setTime(false); else if (S.area) focusArea(S.area); else if (S.p) go(null); }
addEventListener('popstate', () => { armed = false; navBack(); arm(); });
addEventListener('keydown', e => { if (e.key === 'Escape' && !(e.target.matches && e.target.matches('input,textarea'))) navBack(); });
$('#backB').addEventListener('click', navBack);

/* ======================================================================
   tapping the 3D
   ====================================================================== */
const ray = new THREE.Raycaster(), ndc = new THREE.Vector2(); let down = null;
canvas.addEventListener('pointerdown', e => { down = [e.clientX, e.clientY]; fly = null; });
canvas.addEventListener('pointerup', e => {
  if (!down || Math.hypot(e.clientX - down[0], e.clientY - down[1]) > 8) return; const r = canvas.getBoundingClientRect(); ndc.set((e.clientX - r.left) / r.width * 2 - 1, -((e.clientY - r.top) / r.height) * 2 + 1); ray.setFromCamera(ndc, camera);
  const p = cur(); const objs = [];
  if (!p) {
    const L = projOf(S.home).sort((a, b) => (a.status === 'done') - (b.status === 'done')); const owner = new Map();
    L.forEach(q => Object.keys(q.areas).forEach(k => (PARTS[k] || []).forEach(m => { if (isShown(m) && !owner.has(m)) { owner.set(m, q.id); objs.push(m); } })));
    const h = ray.intersectObjects(objs); if (h.length) { const id = owner.get(h[0].object); if (POP && POP.id === id) closePop(); else openPop({ kind: 'proj', id, pos: h[0].point.toArray() }); } else closePop(); return;
  }
  Object.keys(p.areas).forEach(k => (PARTS[k] || []).forEach(m => { if (isShown(m)) { m.userData.ak = k; objs.push(m); } })); const h = ray.intersectObjects(objs);
  if (h.length) focusArea(h[0].object.userData.ak, true, h[0].point.toArray()); else if (S.area) focusArea(S.area); else closePop();
});
function focusArea(k, fromMap, at) {
  const p = cur(); if (!p) return;
  if (S.area === k && fromMap && POP && POP.k === k) { S.area = null; POP = null; popEl.classList.remove('on'); }
  else if (S.area === k && !fromMap) { S.area = null; POP = null; popEl.classList.remove('on'); }
  else { S.area = k; if (fromMap) { POP = { kind: 'area', k, pos: at || areaPos(k) }; } }
  buzz(8); flyTo(S.area ? (AREACAM[S.area] || CAM[viewOf(p)]) : CAM[viewOf(p)]); buildTags(); highlight(); render(); drawPop(); arm();
}
function go(id, tab = 'needs') {
  const q = id ? P(id) : null; if (q && q.home !== S.home) S.home = q.home; if (q && q.status === 'done' && tab === 'needs') tab = 'record';
  POP = null; popEl.classList.remove('on'); if (id && S.time) { S.time = false; S.year = null; buildTime(); }
  S.p = id; S.area = null; S.tab = tab; S.xray = tab === 'record'; buzz(10); applyView(); flyTo(CAM[viewOf(cur())], 1.3); buildTags(); while (fullStack.length) popFull(true); render(); if (sheetState === 'full' || sheetState === 'peek') setSheet('half', true); else setSheet('half', true); arm();
}
function goHome(h) { S.home = h; save(); if (S.time) { buildTime(); } go(null); }
$('#xray').addEventListener('click', () => { if (!cur()) { toast('Open a project first — x-ray shows what’s hidden for that job'); return; } S.xray = !S.xray; S.tab = S.xray ? 'record' : (S.tab === 'record' ? 'needs' : S.tab); applyView(); render(); buzz(10); });
$('#timeB').addEventListener('click', () => setTime(!S.time));
$('#titleSw').addEventListener('click', () => openSwitcher());
$('#detB').addEventListener('click', () => openDetails());
