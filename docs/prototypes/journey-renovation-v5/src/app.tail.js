
/* =================== start =================== */
load(); PROJ.forEach(p => { p.notes = p.notes || []; p.people = p.people || []; p.waiting = p.waiting || []; p.care = p.care || []; p.lines = p.lines || []; });
renderTen(); route(); previewAll(); applyView(); buildTags(); render(); setSheet('half', true);
camera.position.set(26, 24, 32); flyTo(camOf(modelOf(S.home)), 1.6);
loadAll().then(() => { furnish(); Object.keys(CUST).forEach(decorateCustom); }).catch(e => console.error(e));
frame();
window.__t = { S, CUST, O, HOMES, PROJ, at: (h, id) => { const c = CUST[h]; const p = c.tag[id]; const q = proj2d(p); const r = canvas.getBoundingClientRect(); return [r.left + q.x, r.top + q.y]; }, atC: (h, id) => { const b = new THREE.Box3().setFromObject(CUST[h].OG[id]); const c = b.getCenter(new THREE.Vector3()); const q = proj2d(c.toArray()); const r = canvas.getBoundingClientRect(); return [r.left + q.x, r.top + q.y]; } };
}
