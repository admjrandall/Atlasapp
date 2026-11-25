
/* =================== start =================== */
load(); PROJ.forEach(p => { p.notes = p.notes || []; p.people = p.people || []; p.waiting = p.waiting || []; p.care = p.care || []; p.lines = p.lines || []; });
renderTen(); route(); previewAll(); applyView(); buildTags(); render(); setSheet('half', true);
camera.position.set(26, 24, 32); flyTo(CAM[modelOf(S.home)], 1.6);
loadAll().then(furnish).catch(e => console.error(e));
frame();
}
