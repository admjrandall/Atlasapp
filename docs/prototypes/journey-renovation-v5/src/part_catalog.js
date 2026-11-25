/* catalog → new renovation form */
function openCatalog(mode) {
  let cat = 'All';
  full(mode === 'done' ? 'Log a past renovation' : 'New renovation', () => {
    $('#fBody').innerHTML = `<input class="search" id="cQ" placeholder="${mode === 'done' ? 'What was done? e.g. new furnace' : 'What are you working on? e.g. new gutters'}" aria-label="Search kinds of renovation"><div class="seg">${['All', ...Object.keys(CATALOG)].map(g => `<button class="${g === cat ? 'on' : ''}" data-g="${g}">${g}</button>`).join('')}</div><div id="cL"></div>`;
    const draw = () => { const q = $('#cQ').value.trim().toLowerCase(); const groups = Object.entries(CATALOG).filter(([g]) => cat === 'All' || g === cat);
      let h = groups.map(([g, list]) => { const l = list.filter(x => !q || x.toLowerCase().includes(q) || g.toLowerCase().includes(q)); return l.length ? `<div class="gh">${g.toUpperCase()}</div><div class="chipset">${l.map(x => `<button data-c="${esc(x)}" data-g="${esc(g)}">${esc(x)}</button>`).join('')}</div>` : ''; }).join('');
      if (q) h = `<button class="proj" id="cOwn"><span class="ic">＋</span><span><b>“${esc($('#cQ').value.trim())}”</b><small>Use your own words — Atlas sets basic steps you can edit</small></span></button>` + h;
      else h = `<p class="mini">${Object.values(CATALOG).flat().length} kinds to start from, or type anything.</p>` + h;
      $('#cL').innerHTML = h; $('#cL').querySelectorAll('[data-c]').forEach(b => b.addEventListener('click', () => newForm(b.dataset.c, b.dataset.g, mode))); const o = $('#cOwn'); if (o) o.addEventListener('click', () => newForm($('#cQ').value.trim(), guessGroup($('#cQ').value), mode)); };
    $('#cQ').addEventListener('input', draw); $('#fBody').querySelectorAll('.seg [data-g]').forEach(b => b.addEventListener('click', () => { cat = b.dataset.g; $('#fBody').querySelectorAll('.seg [data-g]').forEach(x => x.classList.toggle('on', x === b)); draw(); })); draw(); setTimeout(() => $('#cQ')?.focus(), 80);
  });
}
function guessGroup(t) { t = t.toLowerCase(); for (const [g, l] of Object.entries(CATALOG)) if (l.some(x => x.toLowerCase().split(/[ /&]+/).some(w => w.length > 3 && t.includes(w)))) return g; if (/yard|garden|lawn|tree|fence/.test(t)) return 'Yard'; if (/roof|gutter|window|door|deck|siding/.test(t)) return 'Outside'; if (/heat|furnace|pipe|wire|plumb|electric|water/.test(t)) return 'Systems'; return 'Rooms'; }
const AREAS_BY = { main: { Inside: { kitchen: 'Kitchen', living: 'Living room', bed: 'Bedroom', bath: 'Bathroom', entry: 'Hallway' }, Outside: { front: 'Roof · front', back: 'Roof · back', sFront: 'Front', sLeft: 'Left side', sBack: 'Back', sGarage: 'Garage' }, Yard: { patio: 'Patio', beds: 'Garden beds', drain: 'Fence line', lawn: 'Back lawn' }, Systems: { wh: 'Garage / utility' } }, cabin: { Inside: { deck: 'Cabin' }, Outside: { deck: 'Deck', stairs: 'Stairs', stove: 'Roof' }, Yard: { deck: 'Lot' }, Systems: { stove: 'Stove' } }, condo: { Inside: { unit: 'Unit' }, Outside: { unit: 'Balcony' }, Yard: { unit: 'Unit' }, Systems: { unit: 'Unit' } } };
const VIEW_BY = { main: { Inside: 'rooms', Outside: 'siding', Yard: 'yard', Systems: 'item' }, cabin: { Inside: 'deck', Outside: 'deck', Yard: 'deck', Systems: 'deck' }, condo: { Inside: 'condo', Outside: 'condo', Yard: 'condo', Systems: 'condo' } };
function newForm(name, group, mode) {
  let home = S.home, place = PLACE_OF[group] || 'Inside', when = mode === 'done' ? 'done' : 'planned', picked = new Set();
  let steps = (STEPS_OF[group] || ['Plan', 'Do it', 'Done']).slice();
  const draw = () => {
    const areas = AREAS_BY[modelOf(home)][place];
    $('#fBody').innerHTML = `<div class="lbl">Name</div><input class="inp" id="nN" value="${esc(name)}"><div class="lbl">Home</div><div class="chipset" id="nH">${Object.entries(HOMES).map(([k, h]) => `<button data-h="${k}" class="${k === home ? 'on' : ''}">${h.icon} ${esc(h.name)}</button>`).join('')}</div>
      <div class="lbl">Where</div><div class="chipset" id="nP">${['Inside', 'Outside', 'Yard', 'Systems'].map(x => `<button data-p="${x}" class="${x === place ? 'on' : ''}">${x}</button>`).join('')}</div><div class="chipset" id="nA">${Object.entries(areas).map(([k, l]) => `<button data-a="${k}" class="${picked.has(k) ? 'on' : ''}">${esc(l)}</button>`).join('')}</div><p class="mini" style="margin-top:0">Pick the parts it touches — they light up on the map. Optional.</p>
      <div class="lbl">When</div><div class="chipset" id="nW">${[['active', 'Starting now'], ['planned', 'Planned'], ['done', 'Already done']].map(([k, l]) => `<button data-w="${k}" class="${k === when ? 'on' : ''}">${l}</button>`).join('')}</div>${when === 'done' ? `<div class="lbl">Year</div><input class="inp" id="nY" inputmode="numeric" value="2024"><div class="lbl">What it cost (optional)</div><input class="inp" id="nC" inputmode="decimal" placeholder="$0">` : `<div class="lbl">Budget (optional)</div><input class="inp" id="nB" inputmode="decimal" placeholder="$0">`}
      <div class="lbl">Steps · edit any time</div><div class="card" style="margin:0">${steps.map((s, i) => `<div class="stepl"><input value="${esc(s)}" data-si="${i}" aria-label="Step ${i + 1}"><button data-sd="${i}" aria-label="Remove step">✕</button></div>`).join('')}<div class="row"><button class="btn g" id="nSA">＋ Add a step</button></div></div>
      <div class="row" style="margin-top:14px"><button class="btn" id="nGo" style="flex:1">${when === 'done' ? 'Save to history' : 'Create'}</button></div>`;
    const keep = () => { name = $('#nN').value; steps = [...$('#fBody').querySelectorAll('[data-si]')].map(x => x.value); };
    $('#nH').querySelectorAll('button').forEach(b => b.addEventListener('click', () => { keep(); home = b.dataset.h; picked.clear(); draw(); }));
    $('#nP').querySelectorAll('button').forEach(b => b.addEventListener('click', () => { keep(); place = b.dataset.p; picked.clear(); draw(); }));
    $('#nA').querySelectorAll('button').forEach(b => b.addEventListener('click', () => { const k = b.dataset.a; picked.has(k) ? picked.delete(k) : picked.add(k); b.classList.toggle('on'); }));
    $('#nW').querySelectorAll('button').forEach(b => b.addEventListener('click', () => { keep(); when = b.dataset.w; draw(); }));
    $('#fBody').querySelectorAll('[data-sd]').forEach(b => b.addEventListener('click', () => { keep(); if (steps.length > 1) steps.splice(+b.dataset.sd, 1); draw(); }));
    $('#nSA').addEventListener('click', () => { keep(); steps.push('New step'); draw(); });
    $('#nGo').addEventListener('click', () => { keep(); if (!name.trim()) { $('#nN')?.focus(); return; } create({ name: name.trim()[0].toUpperCase() + name.trim().slice(1), group, home, place, when, areaKeys: [...picked], steps: steps.filter(Boolean), year: parseInt($('#nY')?.value) || 2026, cost: parseFloat(($('#nC')?.value || '').replace(/[$,]/g, '')) || 0, budget: parseFloat(($('#nB')?.value || '').replace(/[$,]/g, '')) || 0 }); });
  };
  full(mode === 'done' ? 'Log a past renovation' : 'New renovation', draw);
}
function create(o) {
  const id = 'n' + Date.now(); const mo = modelOf(o.home); const all = AREAS_BY[mo][o.place]; const areas = {}; (o.areaKeys.length ? o.areaKeys : Object.keys(all).slice(0, 3)).forEach(k => areas[k] = all[k]);
  const thingKey = 't' + id; THINGS[thingKey] = { home: o.home, name: o.name, place: o.place, hist: [] };
  const done = o.when === 'done';
  PROJ.push({ id, home: o.home, thing: thingKey, name: o.name, icon: { Outside: '🏠', Inside: '🛋', Yard: '🌿', Systems: '⚙️' }[o.place] || '🛠', status: o.when, year: done ? o.year : 2026, place: o.place, view: VIEW_BY[mo][o.place], record: { Outside: 'Hidden layers', Inside: 'Walls', Yard: 'Underground', Systems: 'Label' }[o.place], stops: o.steps.length ? o.steps : ['Plan', 'Do it', 'Done'], here: done ? o.steps.length : 0, where: done ? 'logged from memory' : `${o.steps[0] || 'Plan'} · just started`, areas,
    details: [F('What exactly', '', 'text'), F('Material / type', '', 'text'), F('Colour', '', 'colour', { pal: o.place === 'Outside' ? 'siding' : 'paint' }), F('Brand & model', '', 'text'), F('Who did it', '', 'text'), F('Warranty', '', 'text')],
    needs: done ? [] : [{ id: 'first', area: null, kind: 'task', k: 'FIRST', title: 'Add a photo or two of what’s there now', p: 'Before photos help with quotes, claims and resale.' }], waiting: [], budget: o.budget, lines: done && o.cost ? [{ l: 'Total paid', v: o.cost, kind: 'paid', who: '' }] : [], watch: done ? ['Logged from memory', 'Add receipts or photos if you find them.'] : ['Keep every quote', 'Add quotes, payments and receipts as they come — type them or snap them.'], agreed: [], people: [], photos: [], notes: [], care: [], later: [], coming: done ? ['CARE', 'Add a care reminder', 'Atlas can remind you when this needs upkeep.'] : ['WHEN WORK STARTS', 'Photos of anything that gets covered up', 'Atlas reminds you at the right step.'] });
  buzz([10, 20]); save(); toast(done ? `${o.name} saved to ${HOMES[o.home].name}’s history` : `${o.name} created`); go(id);
}

