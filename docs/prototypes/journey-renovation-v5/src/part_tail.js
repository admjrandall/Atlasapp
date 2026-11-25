/* ======================================================================
   modal · toast · router · loop
   ====================================================================== */
function modal(html, fn) { $('#mcard').innerHTML = html; if (!modalOpen) { modalOpen = true; arm(); } $('#modal').classList.add('show'); fn && fn(); }
function closeModal() { modalOpen = false; $('#modal').classList.remove('show'); }
$('#modal').addEventListener('click', e => { if (e.target.id === 'modal') closeModal(); });
let tt; function toast(msg, undo) { const t = $('#toast'); const over = $('#full').classList.contains('show') || modalOpen || sheetState === 'full' || !$('#reno').classList.contains('on'); t.style.setProperty('bottom', over ? '20px' : `calc(var(--shpx) + 14px)`, 'important'); t.innerHTML = `<span>${esc(msg)}</span>${undo ? '<button id="un">Undo</button>' : ''}`; t.classList.add('show'); if (undo) $('#un').addEventListener('click', () => { undo(); t.classList.remove('show'); }); clearTimeout(tt); tt = setTimeout(() => t.classList.remove('show'), 4500); }
function route() { const h = location.hash === '#journeys' ? 'ten' : 'reno'; $('#reno').classList.toggle('on', h === 'reno'); $('#ten').classList.toggle('on', h === 'ten'); $('#t1').classList.toggle('on', h === 'reno'); $('#t2').classList.toggle('on', h === 'ten'); if (h === 'reno') { setSheet(sheetState, true); resize(); } }
addEventListener('hashchange', route);
function frame() {
  if (fly) { const k = ease(clamp((performance.now() - fly.s) / fly.d)); camera.position.lerpVectors(fly.p0, fly.p1, k); controls.target.lerpVectors(fly.t0, fly.t1, k); if (k >= 1) fly = null; }
  controls.update(); glowMat.opacity = .55 + Math.sin(performance.now() / 300) * .35;
  placeAll(); renderer.render(scene, camera); requestAnimationFrame(frame);
}

