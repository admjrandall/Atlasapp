
/* ======================================================================
   details — everything edits in place
   ====================================================================== */
const COMPARE = { 'Fiber cement': ['30–50 yr life', 'Paint every 10–15 yrs', 'Heavier; pro install'], Vinyl: ['20–40 yr life', 'No paint', 'Can crack in cold'], 'Engineered wood': ['20–30 yr life', 'Paint every 7–10 yrs', 'Lighter, easier cuts'], Cedar: ['20–40 yr life with care', 'Stain or paint often', 'Natural look'], Metal: ['40+ yr life', 'Little upkeep', 'Can dent'], 'Architectural asphalt': ['~25–30 yr life', 'Most common', 'Lowest cost'], 'Standing-seam metal': ['40–70 yr life', 'Sheds snow', 'Higher cost'], 'Composite shake': ['30–50 yr life', 'Shake look', 'Mid–high cost'], Tile: ['50+ yr life', 'Very heavy', 'Needs strong framing'], 'Gas tank': ['10–12 yr life', 'Lowest upfront', 'Runs out of hot water'], 'Tankless gas': ['20 yr life', 'Endless hot water', 'May need a bigger gas line'], 'Heat-pump': ['13–15 yr life', 'Lowest running cost', 'Needs space & cool air'], 'Electric tank': ['10–15 yr life', 'Simple install', 'Higher running cost'], Composite: ['25–30 yr life', 'No sealing', 'Costs more up front'], 'Pressure-treated': ['10–15 yr life', 'Cheapest', 'Seal yearly'] };
const MORE_COL = [{ n: 'Tricorn Black', code: 'SW 6258', m: 'Sherwin-Williams', hex: '#2f2f30' }, { n: 'Repose Gray', code: 'SW 7015', m: 'Sherwin-Williams', hex: '#c2bdb4' }, { n: 'Revere Pewter', code: 'HC-172', m: 'Benjamin Moore', hex: '#ccc4b6' }];
function openDetails(focus) {
  const p = cur(); if (!p) return; let cmp = null, more = false;
  const setV = (f, v) => { const was = f.v; if (v === was) return; f.v = v; save(); applyLook(); render(); draw(); buzz(8); toast(`${f.k}: ${v || 'cleared'}`, () => { f.v = was; save(); applyLook(); render(); redrawDetails(); }); };
  const draw = () => {
    const ae = document.activeElement; const fk = ae && ae.dataset && ae.dataset.fk && $('#fBody').contains(ae) ? ae.dataset.fk : null;
    $('#fBody').innerHTML = `<div class="card"><div class="k v">NAME</div><input class="flat big" data-fk="pn" id="pName" value="${esc(p.name)}" aria-label="Project name"><div class="mini" style="margin:2px 6px 0">${esc(HOMES[p.home].name)} · ${esc(THINGS[p.thing].place)} · ${esc(THINGS[p.thing].name)}</div></div>
      <div class="card">${lh('DETAILS · tap any value to change it', 'field')}${p.details.map((f, i) => { const col = f.type === 'colour' ? hexOf(f.v) : null; return `<div class="frow" data-row="${i}"><input class="flat fk" data-fk="dk${i}" data-dk="${i}" value="${esc(f.k)}" aria-label="Field name">${f.type === 'colour' ? `<span class="sw2" style="background:${col || 'repeating-linear-gradient(45deg,#eee 0 4px,#fff 4px 8px)'}"></span>` : ''}<input class="flat" data-fk="dv${i}" data-dv="${i}" value="${esc(f.v)}" placeholder="${f.type === 'choice' ? 'Pick or type' : f.type === 'colour' ? 'Pick or type a colour' : 'Add'}" aria-label="${esc(f.k)}"><button class="xb" data-dx="${i}" aria-label="Remove ${esc(f.k)}">✕</button></div>${cmp === i ? cmpHTML(f) : ''}`; }).join('')}</div>
      <div class="card" id="stepsCard">${lh('STEPS · match how this job really goes', 'step')}${p.stops.map((s, i) => `<div class="frow"><span class="sn ${p.status === 'done' || i < p.here ? 'd' : i === p.here ? 'h' : ''}">${i + 1}</span><input class="flat" data-fk="st${i}" data-st="${i}" value="${esc(s)}" aria-label="Step ${i + 1}"><button class="xb" data-up="${i}" aria-label="Move up" ${i ? '' : 'disabled style="opacity:.25"'}>↑</button><button class="xb" data-del="${i}" aria-label="Remove step">✕</button></div>`).join('')}</div>
      <div class="card">${lh(`PEOPLE · ${p.people.length}`, 'person')}${p.people.map(([n, r], i) => `<div class="frow"><span class="av">${esc((n || '?')[0])}</span><div class="grow"><input class="flat b" data-fk="pn${i}" data-pnm="${i}" value="${esc(n)}" aria-label="Name"><input class="flat who" data-fk="pr${i}" data-prl="${i}" value="${esc(r)}" placeholder="Role" aria-label="Role"></div><button class="xb" data-prm="${i}" aria-label="Remove ${esc(n)}">✕</button></div>`).join('') || '<p class="mini" style="margin:2px 0 0">No one yet.</p>'}<div class="row"><button class="btn g" id="findPro">✦ Find a local pro</button></div></div>
      <div class="card"><div class="k v">${esc(THINGS[p.thing].name.toUpperCase())} · HISTORY</div>${histHTML(p.thing)}</div>
      <div class="row"><button class="btn g danger" id="delP">Delete this project</button></div>`;
    const B = $('#fBody'); flatKeys(B);
    $('#pName').addEventListener('change', () => { const v = $('#pName').value.trim(); const was = p.name; if (!v || v === was) { $('#pName').value = was; return; } p.name = v; save(); render(); buildTags(); toast('Name saved', () => { p.name = was; save(); render(); buildTags(); redrawDetails(); }); });
    B.querySelectorAll('[data-dk]').forEach(inp => inp.addEventListener('change', () => { const f = p.details[+inp.dataset.dk]; const v = inp.value.trim(); const was = f.k; if (!v || v === was) { inp.value = was; return; } f.k = v; save(); toast('Label saved', () => { f.k = was; save(); redrawDetails(); }); }));
    B.querySelectorAll('[data-dv]').forEach(inp => { const i = +inp.dataset.dv, f = p.details[i];
      inp.addEventListener('change', () => setV(f, inp.value.trim()));
      if (f.type === 'choice' || f.type === 'colour') { inp.addEventListener('focus', () => showOpts(inp, f, i)); inp.addEventListener('blur', () => setTimeout(() => { const o = B.querySelector('.fopts'); if (o && !o.contains(document.activeElement) && document.activeElement !== inp) o.remove(); }, 160)); }
    });
    B.querySelectorAll('[data-dx]').forEach(b => b.addEventListener('click', () => { const i = +b.dataset.dx; const f = p.details.splice(i, 1)[0]; save(); draw(); toast(`Removed ${f.k}`, () => { p.details.splice(i, 0, f); save(); redrawDetails(); }); }));
    B.querySelectorAll('[data-add]').forEach(b => b.addEventListener('click', () => {
      const t = b.dataset.add;
      if (t === 'field') inlineAdd(b, 'Name: value — e.g. Warranty: 30 years', v => { const [k, ...r] = v.split(':'); p.details.push(F(k.trim()[0].toUpperCase() + k.trim().slice(1), r.join(':').trim())); save(); draw(); toast(`Added ${k.trim()}`); const ins = B.querySelectorAll('[data-dv]'); if (!r.length) ins[ins.length - 1].focus(); });
      if (t === 'step') inlineAdd(b, 'New step — e.g. Inspection', v => { p.stops.push(v[0].toUpperCase() + v.slice(1)); save(); draw(); render(); toast(`Step added · ${v}`, () => { p.stops.pop(); save(); render(); redrawDetails(); }); });
      if (t === 'person') inlineAdd(b, 'Name, role — e.g. Dana, tile shop', v => { const [n, ...r] = v.split(','); p.people.push([n.trim(), r.join(',').trim() || 'Added by you']); save(); draw(); toast(`${n.trim()} added`); });
    }));
    B.querySelectorAll('[data-st]').forEach(inp => inp.addEventListener('change', () => { const i = +inp.dataset.st; const v = inp.value.trim(); const was = p.stops[i]; if (!v || v === was) { inp.value = was; return; } p.stops[i] = v; save(); render(); toast('Step renamed', () => { p.stops[i] = was; save(); render(); redrawDetails(); }); }));
    B.querySelectorAll('[data-up]').forEach(b => b.addEventListener('click', () => { const i = +b.dataset.up; if (!i) return; [p.stops[i - 1], p.stops[i]] = [p.stops[i], p.stops[i - 1]]; save(); draw(); render(); }));
    B.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', () => { if (p.stops.length < 2) { toast('A project needs at least one step'); return; } const i = +b.dataset.del; const was = { s: p.stops.slice(), h: p.here }; p.stops.splice(i, 1); p.here = Math.min(p.here, p.status === 'done' ? p.stops.length : p.stops.length - 1); save(); draw(); render(); toast(`Removed ${was.s[i]}`, () => { p.stops = was.s; p.here = was.h; save(); render(); redrawDetails(); }); }));
    B.querySelectorAll('[data-pnm]').forEach(inp => inp.addEventListener('change', () => { const i = +inp.dataset.pnm; const v = inp.value.trim(); if (!v) { inp.value = p.people[i][0]; return; } p.people[i][0] = v; save(); toast('Saved'); }));
    B.querySelectorAll('[data-prl]').forEach(inp => inp.addEventListener('change', () => { p.people[+inp.dataset.prl][1] = inp.value.trim(); save(); toast('Saved'); }));
    B.querySelectorAll('[data-prm]').forEach(b => b.addEventListener('click', () => { const i = +b.dataset.prm; const x = p.people.splice(i, 1)[0]; save(); draw(); toast(`Removed ${x[0]}`, () => { p.people.splice(i, 0, x); save(); redrawDetails(); }); }));
    $('#findPro').addEventListener('click', () => findPro(p, draw));
    B.querySelectorAll('[data-p]').forEach(b => b.addEventListener('click', () => go(b.dataset.p)));
    B.querySelectorAll('[data-cmpx]').forEach(b => b.addEventListener('click', () => { cmp = null; draw(); }));
    B.querySelectorAll('[data-cmpp]').forEach(b => b.addEventListener('click', () => { const f = p.details[cmp]; cmp = null; setV(f, b.dataset.cmpp); }));
    $('#delP').addEventListener('click', () => modal(`<h3>Delete “${esc(p.name)}”?</h3><p class="mini" style="margin:0">Its costs, photos and notes go too. You can undo right after.</p><div class="row"><button class="btn danger" id="dY">Delete</button><button class="btn g" id="cx">Cancel</button></div>`, () => { $('#dY').addEventListener('click', () => { const i = PROJ.indexOf(p); PROJ.splice(i, 1); closeModal(); go(null); changed(`Deleted ${p.name}`, () => { PROJ.splice(i, 0, p); }); }); $('#cx').addEventListener('click', closeModal); }));
    if (fk) { const el = B.querySelector(`[data-fk="${fk}"]`); if (el && fk.startsWith('dv') === false) el.focus(); }
    if (focus === 'steps') { $('#stepsCard').scrollIntoView({ block: 'start' }); focus = null; }
  };
  const showOpts = (inp, f, i) => {
    const B = $('#fBody'); B.querySelectorAll('.fopts').forEach(o => o.remove());
    const row = inp.closest('.frow'); const o = document.createElement('div'); o.className = 'fopts';
    if (f.type === 'choice') o.innerHTML = f.opts.map(x => `<button class="${x === f.v ? 'on' : ''}" data-o="${esc(x)}">${esc(x)}</button>`).join('') + `<button data-cmpo="1">✦ Compare</button>`;
    else { const pal = (COLOURS[f.pal] || COLOURS.paint).concat(more ? MORE_COL : []); o.innerHTML = pal.map(c => { const v = c.n + (c.code ? ' · ' + c.code : ''); return `<button class="${f.v && f.v.startsWith(c.n) ? 'on' : ''}" data-o="${esc(v)}" title="${esc(c.m)}"><span class="dot2" style="background:${c.hex}"></span>${esc(v)}</button>`; }).join('') + (more ? '' : `<button data-more="1">✦ More colours</button>`); }
    row.after(o);
    o.querySelectorAll('button').forEach(b => { b.addEventListener('pointerdown', e => e.preventDefault()); b.addEventListener('click', () => {
      if (b.dataset.o != null) { inp.value = b.dataset.o; inp.blur(); setV(f, b.dataset.o); }
      else if (b.dataset.cmpo) { cmp = i; inp.blur(); draw(); }
      else if (b.dataset.more) { more = true; showOpts(inp, f, i); toast('Looked up from the makers’ current colour lines'); }
    }); });
    if (f.type === 'colour') o.insertAdjacentHTML('beforeend', '');
  };
  const cmpHTML = f => `<div class="cmpcard"><div class="lh"><div class="k v">✦ QUICK COMPARE · general guide</div><button class="xb" data-cmpx="1" aria-label="Close compare">✕</button></div>${f.opts.map(o => `<div class="cr"><div class="grow"><b>${esc(o)}</b><div class="mini" style="margin:0">${esc((COMPARE[o] || ['Ask Atlas for specifics']).join(' · '))}</div></div><button class="btn ${o === f.v ? '' : 'g'}" data-cmpp="${esc(o)}">${o === f.v ? 'Chosen' : 'Pick'}</button></div>`).join('')}<p class="src2">In the app, Atlas cites the sources it used. Prices depend on your area.</p></div>`;
  full('Details', draw);
}
