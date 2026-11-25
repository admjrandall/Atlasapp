
/* ======================================================================
   render: header, stepper, tabs, card body
   ====================================================================== */
const TABS = ['needs', 'money', 'agreed', 'record'];
function render() {
  const p = cur(); const H0 = HOMES[S.home];
  const ae = document.activeElement; const fk = ae && ae.dataset && ae.dataset.fk && $('#sbody').contains(ae) ? ae.dataset.fk : null; const selS = fk ? ae.selectionStart : null;
  $('#homeIc').textContent = H0.icon; $('#homeName').textContent = H0.name; $('#homeName').classList.toggle('p', !!p); $('#titleSw').classList.toggle('inp', !!p);
  $('#crSep').style.display = p ? '' : 'none'; $('#ptitle').style.display = p ? '' : 'none'; $('#ptitle').textContent = p ? p.name : '';
  $('#backB').style.display = S.p ? '' : 'none'; $('#backB').setAttribute('aria-label', `Back to ${H0.name}`); $('#backT').textContent = H0.name;
  $('#xray').style.display = p ? '' : 'none'; $('#timeB').style.display = p ? 'none' : '';
  $('#areachip').classList.toggle('on', !!(p && S.area)); $('#areachip').innerHTML = p && S.area ? `<span>📍 Showing only <b>${esc(p.areas[S.area])}</b></span><button id="clrA">Show everything</button>` : '';
  if ($('#clrA')) $('#clrA').addEventListener('click', () => focusArea(S.area));
  const B = $('#sbody');
  if (!p) {
    const L = projOf(S.home); const open = L.reduce((s, q) => s + (q.status === 'done' ? 0 : q.needs.length), 0);
    $('#where').textContent = H0.kind;
    $('#scope').textContent = H0.name;
    $('#scopeSub').textContent = L.length ? `${L.filter(q => q.status === 'active').length} in progress · ${L.filter(q => q.status === 'planned').length} planned · ${L.filter(q => q.status === 'done').length} done` : 'No projects yet';
    $('#detB').style.display = 'none'; $('#stepper').innerHTML = ''; $('#four').style.display = L.length ? '' : 'none'; $('#four').classList.add('three');
    const cst = H0.model === 'custom';
    if (cst && S.obj && O(S.home, S.obj)) { const o = O(S.home, S.obj); $('#four').style.display = 'none'; $('#scope').textContent = objName(o); $('#scopeSub').textContent = `${(OT[o.type] || OT.photo).n} · ${sizeText(o)}`; $('#where').textContent = S.plan ? 'Plan view · drag to move · pull the handle to resize' : `${H0.name} · tap anything else to switch`;
      B.innerHTML = (S.obj === 'house' && H0.onboard === 'check' ? `<div class="week"><div class="k v">DOES THIS LOOK LIKE YOUR HOUSE?</div><p style="margin:4px 0 8px;font-size:14px">Fix the size, stories, roof and entry below — or tap the garage, a window or a tree on the model to fix those.</p><div class="row"><button class="btn" id="obOk">Looks right</button></div></div>` : '') + `<div class="obhead"><button class="btn g" id="obX">✕ Close</button></div>` + objCardHTML(S.home, S.obj);
      bindObjCard(B, S.home, S.obj); $('#obX').addEventListener('click', closeObj); const ok = $('#obOk'); if (ok) ok.addEventListener('click', () => { H0.onboard = 'photo'; S.htab = 'house'; save(); closeObj(); photoPrompt(S.home); }); return; }
    if (S.pick) { $('#four').style.display = 'none'; $('#scope').textContent = 'Tap the thing'; $('#scopeSub').textContent = 'The project starts from whatever you tap'; B.innerHTML = `<div class="week"><div class="k v">NEW PROJECT</div><p style="margin:4px 0 8px;font-size:14px">Tap the roof, a window, a tree, the fence, the driveway… on the model above.</p><div class="row"><button class="btn g" id="pkX">Cancel</button></div></div>`; $('#pkX').addEventListener('click', () => { S.pick = false; render(); }); return; }
    if (cst && !S.htab) S.htab = L.length ? 'week' : 'house';
    if (S.time) S.htab = 'years'; const ht = S.htab || 'week'; const ev = events(S.home); const nObj = cst ? (H0.objs || []).filter(o => o.type !== 'lot' && o.status !== 'removed').length : 0;
    if (cst) { $('#four').style.display = ''; $('#four').classList.remove('three'); }
    $('#four').innerHTML = (cst ? [['house', 'Your house', `${nObj} things`, 0]] : []).concat([['week', 'Needs you', open ? `${open} open` : 'All clear', open], ['all', 'Projects', `${L.length} total`, 0], ['years', cst ? 'History' : 'Over the years', `${ev.length} event${ev.length === 1 ? '' : 's'}`, 0]]).map(([k, l, v, c]) => `<button class="${ht === k ? 'on' : ''}" data-hk="${k}" aria-pressed="${ht === k}"><b>${l}</b><span class="${c ? 'am' : ''}">${v}</span></button>`).join('');
    $('#four').querySelectorAll('[data-hk]').forEach(x => x.addEventListener('click', () => { S.htab = x.dataset.hk; if (S.time && S.htab !== 'years') setTime(false); else render(); $('#sbody').scrollTop = 0; if (sheetState === 'peek') setSheet('half'); }));
    const ask = `<div class="ask2"><button id="askB">✦ Ask your house — “what colour is the trim?”</button></div><div class="hrow"><button id="rvB">${H0.photo ? `<img src="${H0.photo}" alt="">` : '<span class="ph">📷</span>'}<span><b>Real view</b><small>${H0.photo ? 'Try colours on your photo' : 'Add a photo of your house'}</small></span></button>${cst ? `<button id="shB"><span class="ph">🏠</span><span><b>Edit the house</b><small>Size, stories, roof</small></span></button>` : `<button id="fhB" class="hi"><span class="ph">🔎</span><span><b>Find my house</b><small>Type your address — build yours</small></span></button>`}</div>`;
    if (ht === 'house' && cst) B.innerHTML = ask + thingsHTML(S.home);
    else if (!L.length) B.innerHTML = ask + `<div class="empty"><b>Nothing here yet</b>Start a renovation, or log one you’ve already done.<div class="row" style="justify-content:center"><button class="btn" id="npB">＋ New project</button><button class="btn g" data-new="done">Log a past one</button></div></div>`;
    else if (ht === 'week') B.innerHTML = ask + weekHTML(L);
    else if (ht === 'all') B.innerHTML = ask + `<div class="lh"><div class="k">${L.length} PROJECT${L.length === 1 ? '' : 'S'} AT ${esc(H0.name.toUpperCase())}</div><button class="addb" ${cst ? 'id="npB2"' : 'data-newm="1"'}>＋ New</button></div>${cst ? '' : newMenuHTML()}` + groupsHTML(L);
    else B.innerHTML = (S.time ? '' : `<div class="row" style="margin:0 0 10px"><button class="btn" id="tmOn" style="flex:1">🕓 Show it on the house</button></div>`) + timeListHTML();
    bindList(B); bindNewMenu(B); const rv = $('#rvB'); if (rv) rv.addEventListener('click', () => { if (!H0.photo) { photoPrompt(S.home); return; } openRealView(S.home, H0.photo && Object.values(H0.marks || {}).some(l => l.length) ? 'colour' : 'mark'); }); const fh = $('#fhB'); if (fh) fh.addEventListener('click', openFindHome); const sb = $('#shB'); if (sb) sb.addEventListener('click', () => selectObj('house')); if (ht === 'house' && cst) bindThings(B, S.home); const ab = $('#askB'); if (ab) ab.addEventListener('click', () => openSwitcher(true));
    const np2 = $('#npB2'); if (np2) np2.addEventListener('click', openNewProject); const npb = $('#npB'); if (npb) npb.addEventListener('click', openNewProject); const to = $('#tmOn'); if (to) to.addEventListener('click', () => setTime(true));
    bindWeek(B); B.querySelectorAll('[data-yr]').forEach(x => x.addEventListener('click', () => jumpYear(+x.dataset.yr)));
    return;
  }
  ['care', 'notes', 'people', 'waiting', 'lines', 'photos', 'agreed', 'needs'].forEach(k => p[k] = p[k] || []);
  $('#detB').style.display = ''; $('#four').style.display = ''; $('#four').classList.remove('three');
  const stepI = Math.min(p.here, p.stops.length - 1);
  $('#where').textContent = p.status === 'done' ? `Finished ${p.year} · ${p.where}` : p.status === 'planned' ? `Planned ${p.year} · ${p.where}` : `Step ${stepI + 1} of ${p.stops.length} · ${p.where}`;
  $('#scope').textContent = p.name; $('#scopeSub').textContent = `${THINGS[p.thing].place} · ${THINGS[p.thing].name}${p.status === 'done' ? ' · finished ' + p.year : ''}`;
  const n = p.stops.length; const fillTo = p.status === 'done' ? 1 : n > 1 ? Math.min(p.here, n - 1) / (n - 1) : 0;
  $('#stepper').innerHTML = `<div class="stp"><div class="fl" style="width:calc((100% - 10px) * ${fillTo})"></div>${p.stops.map((s, i) => { const st = p.status === 'done' || i < p.here ? 'd' : i === p.here && p.status === 'active' ? 'h' : i === p.here ? 'h' : ''; return `<button class="${st}" data-st="${i}" aria-label="Step ${i + 1}: ${esc(s)}${st === 'h' ? ' (you are here)' : st === 'd' ? ' (done)' : ''}"><i></i><span>${esc(s)}</span></button>`; }).join('')}</div>`;
  $('#stepper').querySelectorAll('[data-st]').forEach(b => b.addEventListener('click', () => openStep(p, +b.dataset.st)));
  const m = moneyOf(p); const inA = x => !S.area || x.area === S.area || x.area == null; const needs = p.needs.filter(inA), ag = p.agreed.filter(inA), ph = p.photos.filter(inA);
  const unconf = ag.filter(a => !a.ok).length; const recN = ph.length + p.notes.length + p.care.length;
  const moneyV = (!p.budget || p.status === 'done') ? `${money(m.cost)} spent` : m.left < 0 ? `${money(-m.left)} over` : `${money(m.left)} left`;
  $('#four').innerHTML = [['needs', 'Needs you', needs.length ? `${needs.length} open` : 'All clear', needs.length], ['money', 'Money', moneyV, 0], ['agreed', 'Agreed', unconf ? `${unconf} to confirm` : `${ag.length} in writing`, 0], ['record', 'Record', `${recN} item${recN === 1 ? '' : 's'}`, 0]].map(([k, l, v, c]) => `<button class="${S.tab === k ? 'on' : ''}" data-k="${k}" aria-pressed="${S.tab === k}"><b>${esc(l)}</b><span class="${c ? 'am' : ''}">${esc(v)}</span></button>`).join('');
  $('#four').querySelectorAll('button').forEach(b => b.addEventListener('click', () => { const k = b.dataset.k; if ((k === 'record') !== S.xray) { S.xray = k === 'record'; applyView(); } S.tab = k; render(); $('#sbody').scrollTop = 0; if (sheetState === 'peek') setSheet('half'); }));
  const doneBanner = p.status === 'done' ? `<div class="card"><div class="k g">FINISHED · ${p.year}</div><h3>Kept as a record</h3><p>Details, receipts, photos and care reminders stay with the ${esc(THINGS[p.thing].name.toLowerCase())}.</p><div class="row"><button class="btn g" id="reopen">Reopen</button><button class="btn g" id="toRec">See the record</button></div></div>` : '';
  const plannedBanner = p.status === 'planned' ? `<div class="card"><div class="k v">PLANNED · ${p.year}</div><h3>Not started yet</h3><p>Everything you add now is ready when you are.</p><div class="row"><button class="btn" id="startP">Start it now</button></div></div>` : '';
  if (S.tab === 'needs') B.innerHTML = doneBanner + plannedBanner + (p.status === 'done' ? '' : needsHTML(p, needs));
  if (S.tab === 'money') B.innerHTML = moneyHTML(p, m);
  if (S.tab === 'agreed') B.innerHTML = agreedHTML(p, ag);
  if (S.tab === 'record') B.innerHTML = recordHTML(p, ph);
  bind(B, p);
  if (fk) { const el = B.querySelector(`[data-fk="${fk}"]`); if (el) { el.focus(); try { if (selS != null) el.setSelectionRange(selS, selS); } catch (e) {} } }
  if (reAdd) { const b = B.querySelector(`[data-add="${reAdd}"]`); reAdd = null; if (b) b.click(); }
}
let reAdd = null;
function timeListHTML() { const ev = events(S.home); const y = S.year ?? TODAY; return `<div class="card"><div class="k v">${esc(HOMES[S.home].name.toUpperCase())} · OVER THE YEARS</div><div class="hist">${ev.map(e => `<div class="${e.y === y ? 'cur' : ''}"><button data-yr="${e.y}" style="text-align:left;padding:0"><b>${e.y} · ${esc(e.t)}</b></button><small>${esc(e.tn ? e.tn + ' · ' : '')}${esc(e.s)}</small></div>`).join('')}</div><p class="mini">Drag the year above, or tap a line — the house changes to match.</p></div>`; }
function weekItems(L) { const pr = n => /TODAY/.test(n.k) ? 0 : /DUE|BY |BEFORE/.test(n.k) ? 1 : 2; const it = []; L.filter(q => q.status !== 'done').forEach(q => q.needs.forEach(n => it.push({ q, n, r: pr(n) + (q.status === 'planned' ? 2 : 0) }))); return it.sort((a, b) => a.r - b.r); }
function weekHTML(L) {
  const it = weekItems(L); if (!it.length) return `<div class="empty"><b>Nothing needs you</b>Decisions and dates show up here when they come in.</div>`;
  const row = ({ q, n }) => `<div class="it"><span class="ic">${q.icon}</span><div class="grow"><b>${esc(n.title)}</b><small>${esc(q.name)} · ${esc(n.k.toLowerCase())}</small></div>${n.kind === 'task' ? `<button class="btn g" data-wd="${q.id}|${n.id}">Done</button>` : `<button class="btn" data-wo="${q.id}">Decide</button>`}</div>`;
  return `<div class="week"><div class="k v">THIS WEEK</div>${it.slice(0, 3).map(row).join('')}</div>${it.length > 3 ? `<div class="week later"><div class="k">AFTER THAT · ${it.length - 3}</div>${it.slice(3).map(row).join('')}</div>` : ''}`;
}
function bindWeek(B) {
  B.querySelectorAll('[data-wd]').forEach(b => b.addEventListener('click', () => { const [pid, nid] = b.dataset.wd.split('|'); const q = P(pid); const k = q.needs.findIndex(x => x.id === nid); const n = q.needs.splice(k, 1)[0]; buzz(12); changed(`${n.title} · done`, () => q.needs.splice(k, 0, n)); }));
  B.querySelectorAll('[data-wo]').forEach(b => b.addEventListener('click', () => go(b.dataset.wo)));
}
function histHTML(th) { const T0 = THINGS[th]; if (!T0) return ''; const rows = T0.hist.map(r => ({ ...r })); PROJ.filter(q => q.thing === th).forEach(q => { if (!rows.some(r => r.y === q.year)) rows.push({ y: q.year, t: q.name + (q.status === 'done' ? '' : q.status === 'active' ? ' · in progress' : ' · planned'), s: q.where, id: q.id }); }); rows.sort((a, b) => a.y - b.y); return rows.length ? `<div class="hist">${rows.map(r => `<div>${r.id && r.id !== S.p ? `<button data-p="${r.id}" style="text-align:left;padding:0"><b>${r.y} · ${esc(r.t)} ›</b></button>` : `<b>${r.y} · ${esc(r.t)}</b>`}<small>${esc(r.s)}</small></div>`).join('')}</div>` : '<p class="mini">No history yet.</p>'; }
const COL = { done: true };
function projRow(q) { const n = q.needs.length; return `<button class="proj" data-p="${q.id}"><span class="ic">${q.icon}</span><span style="flex:1;min-width:0"><b>${esc(q.name)}</b><small>${q.home !== S.home ? esc(HOMES[q.home].name) + ' · ' : ''}${q.status === 'done' ? q.year + ' · ' + esc(q.where) : esc(q.where)}</small></span>${q.status === 'done' ? '<span class="c z">✓</span>' : `<span class="c ${n ? '' : 'z'}">${n || '✓'}</span>`}</button>`; }
function groupsHTML(L, flat) {
  if (flat) return L.map(projRow).join('');
  const G = [['needs', 'Needs you', L.filter(q => q.status === 'active' && q.needs.length)], ['prog', 'In progress', L.filter(q => q.status === 'active' && !q.needs.length)], ['plan', 'Planned', L.filter(q => q.status === 'planned')], ['done', 'Done', L.filter(q => q.status === 'done').sort((a, b) => b.year - a.year)]];
  const h = G.filter(g => g[2].length).map(([k, t, l]) => `<div class="grp ${COL[k] ? 'shut' : ''}"><button class="gh2" data-gk="${k}" aria-expanded="${!COL[k]}"><span class="chev">▾</span><span class="t">${t}</span><em>${l.length}</em></button><div class="gl">${l.map(projRow).join('')}</div></div>`).join('');
  return h || '<div class="empty"><b>No matches</b>Try another word.</div>';
}
function bindGroups(B) { B.querySelectorAll('[data-gk]').forEach(x => x.addEventListener('click', () => { const k = x.dataset.gk; COL[k] = !COL[k]; B.querySelectorAll(`[data-gk="${k}"]`).forEach(y => { y.closest('.grp').classList.toggle('shut', COL[k]); y.setAttribute('aria-expanded', !COL[k]); }); buzz(5); })); }
function newMenuHTML() { return `<div class="newmenu" hidden><button data-new="planned"><span class="ic">🛠</span><span><b>Start a renovation</b><small>Plan it, track decisions, costs and photos</small></span></button><button data-new="done"><span class="ic">🕓</span><span><b>Log one already done</b><small>Add it to the house’s history — year, cost, receipts</small></span></button></div>`; }
function bindNewMenu(B) { B.querySelectorAll('[data-newm]').forEach(x => x.addEventListener('click', () => { const m = B.querySelector('.newmenu'); m.hidden = !m.hidden; x.textContent = m.hidden ? '＋ New' : '✕ Close'; })); B.querySelectorAll('[data-new]').forEach(x => x.addEventListener('click', () => openCatalog(x.dataset.new))); }
function bindList(B) { B.querySelectorAll('[data-p]').forEach(b => b.addEventListener('click', () => go(b.dataset.p))); bindGroups(B); }
const lh = (label, add, cls = 'v') => `<div class="lh"><div class="k ${cls}">${label}</div>${add ? `<button class="addb" data-add="${add}">＋ Add</button>` : ''}</div><div class="inlslot" data-slot="${add}"></div>`;
const moneyIn = v => v ? money(v) : '';
const numOf = s => { const v = parseFloat(String(s).replace(/[^\d.\-]/g, '')); return isNaN(v) ? null : v; };
function needsHTML(p, list) {
  let h = `<div class="sec">${lh(`TO-DOS & DECISIONS · ${list.length}`, 'todo')}${list.length ? '' : `<div class="empty"><b>Nothing needs you${S.area ? ' here' : ''}</b>Decisions and dates show up here when they come in.</div>`}</div>`;
  h += list.map(n => {
    if (n.kind === 'choose') return `<div class="card" data-nc="${n.id}"><div class="k">${esc(n.k)}${n.area ? ' · ' + esc(p.areas[n.area] || '') : ''}</div><h3>${esc(n.title)}</h3><p>${esc(n.p)}</p><div class="opts">${n.opts.map((o, i) => `<button class="opt" data-ch="${n.id}" data-i="${i}"><div class="sw" style="background:${o.c}"></div><b>${esc(o.n)}</b><small>${esc(o.s)}</small>${o.vote ? `<div class="vote">${esc(o.vote)}</div>` : ''}</button>`).join('')}</div><div class="row ownrow"><button class="btn g" data-own="${n.id}">Type your own</button><button class="btn g" data-snz="${n.id}">Not now</button></div></div>`;
    if (n.kind === 'compare') return `<div class="card"><div class="k">${esc(n.k)} · ${esc(p.areas[n.area] || '')}</div><h3>${esc(n.title)}</h3><p>${esc(n.p)}</p><table class="cmp">${n.table.map((r, i) => `<tr>${r.map(c => i === 0 || i === n.table.length - 1 ? `<th>${esc(c)}</th>` : `<td>${esc(c)}</td>`).join('')}</tr>`).join('')}</table><div class="mini">${esc(n.note)}</div><div class="row">${n.picks.map((x, i) => `<button class="btn ${i ? 'g' : ''}" data-cmp="${n.id}" data-i="${i}">Go with ${esc(x)}</button>`).join('')}</div></div>`;
    return `<div class="card"><div class="k">${esc(n.k)}${n.area && p.areas[n.area] ? ' · ' + esc(p.areas[n.area]) : ''}</div><input class="flat h3" data-fk="nt${n.id}" data-nt="${n.id}" value="${esc(n.title)}" aria-label="To-do">${n.p ? `<p>${esc(n.p)}</p>` : ''}<div class="row"><button class="btn" data-done="${n.id}">Done</button><button class="btn g" data-snz="${n.id}">Not now</button></div></div>`;
  }).join('');
  if (!S.area && p.waiting.length) h += `<div class="card"><div class="k v">WAITING ON OTHERS</div>${p.waiting.map((w, i) => `<div class="pm"><div class="grow"><b>${esc(w.what)}</b><div class="mini" style="margin:0">${esc(w.who)} · ${esc(w.when)} · Atlas nudges you if it doesn’t arrive</div></div><button class="btn g" data-got="${i}" style="height:36px">Got it</button></div>`).join('')}</div>`;
  if (!S.area && (p.later || []).length) h += `<div class="card"><div class="k">LATER · ${p.later.length}</div>${p.later.map((n, i) => `<div class="pm"><div class="grow">${esc(n.title)}</div><button class="btn g" data-back="${i}" style="height:34px">Bring back</button></div>`).join('')}</div>`;
  return h;
}
function moneyHTML(p, m) {
  const seg = (v, c) => v > 0 ? `<div style="width:${Math.min(100, v / Math.max(1, m.budget || m.cost) * 100)}%;background:${c}"></div>` : '';
  const sum = k => p.lines.filter(l => k.includes(l.kind) && l.v > 0).reduce((s, l) => s + l.v, 0);
  const q = sum(['quote']), pd = sum(['paid']), ch = sum(['change', 'cost']);
  return `<div class="card"><div class="frow" style="border:0;padding:0"><div class="k v" style="flex:1">BUDGET</div><input class="flat num" data-fk="bud" id="budIn" inputmode="decimal" value="${moneyIn(m.budget)}" placeholder="Set one" aria-label="Budget"></div><h3>${m.budget ? (m.left >= 0 ? `${money(m.left)} ${m.cost ? 'left for surprises' : 'to spend'}` : `${money(-m.left)} over budget`) : m.cost ? `${money(m.cost)} spent or committed` : 'No costs yet'}</h3><div class="mbar">${seg(pd, '#2f9a66')}${seg(q, '#6b4bd1')}${seg(ch, '#d9822b')}${seg(Math.max(0, m.left), '#e9e4f3')}</div>
  <div class="legend"><span><i class="dot" style="background:#2f9a66"></i>Paid</span><span>${money(pd)}</span><span><i class="dot" style="background:#6b4bd1"></i>Quoted / contracted</span><span>${money(q)}</span><span><i class="dot" style="background:#d9822b"></i>Change orders &amp; extras</span><span>${money(ch)}</span>${m.covered ? `<span><i class="dot" style="background:#2f6fb0"></i>Covered (insurance, rebates)</span><span>−${money(m.covered)}</span><span><b>You pay</b></span><span>${money(m.you)}</span>` : ''}</div></div>
  <div class="card">${lh(`EVERY COST · ${p.lines.length}`, 'cost')}${p.lines.map((l, i) => `<div class="frow cost">${l.receipt ? `<img class="rthumb" src="${l.receipt}" alt="Receipt">` : ''}<div class="grow"><input class="flat b" data-fk="ll${i}" data-ll="${i}" value="${esc(l.l)}" aria-label="What"><div class="subr"><select class="kchip ${l.kind}" data-fk="lk${i}" data-lk="${i}" aria-label="Kind">${Object.entries(KIND).map(([k, t]) => `<option value="${k}" ${k === l.kind ? 'selected' : ''}>${t}</option>`).join('')}</select><input class="flat who" data-fk="lw${i}" data-lw="${i}" value="${esc(l.who || '')}" placeholder="Who" aria-label="Who"></div></div><div class="rc"><input class="flat num" data-fk="lv${i}" data-lv="${i}" inputmode="decimal" value="${l.v ? money(l.v) : ''}" placeholder="$0" aria-label="Amount">${l.sample ? '<small class="smp">sample</small>' : ''}</div><button class="xb" data-lx="${i}" aria-label="Delete ${esc(l.l)}">✕</button></div>`).join('') || '<p class="mini" style="margin:2px 0 0">No costs yet. ＋ Add one — type it like you’d say it, or scan a receipt.</p>'}</div>
  <div class="card"><div class="k">WATCH</div><h3>${esc(p.watch[0])}</h3><p>${esc(p.watch[1])}</p></div>`;
}
function agreedHTML(p, ag) {
  return `<div class="card">${lh(`AGREED · ${ag.length}`, 'agree')}${ag.map(a => { const i = p.agreed.indexOf(a); return `<div class="agree"><span class="ic ${a.ok ? 'ok' : 'q'}">${a.ok ? '✓' : '?'}</span><div style="flex:1;min-width:0"><input class="flat b" data-fk="at${i}" data-at="${i}" value="${esc(a.t)}" aria-label="What was agreed"><small>${esc(a.s)}</small>${!a.ok ? `<div class="row"><button class="btn g" data-off="${i}">Make it official</button><button class="btn g" data-conf="${i}">They confirmed</button></div>` : ''}</div><button class="xb" data-ax="${i}" aria-label="Delete">✕</button></div>`; }).join('') || '<p class="mini" style="margin:2px 0 0">Nothing agreed yet. Shared texts and emails land here.</p>'}<p class="mini">✓ in writing, with its source · ? said but not written down yet</p></div>`;
}
function photoSrc(ph) { if (ph.src) return ph.src; if (!ph._d) ph._d = drawPhoto(ph.draw); return ph._d; }
function baPair(ph) { const b = ph.find(x => /before/i.test(x.t)); const a = ph.find(x => /after/i.test(x.t) && x !== b); return a && b ? [b, a] : null; }
function recordHTML(p, ph) {
  const ba = baPair(ph);
  return `${ba ? `<div class="card"><div class="k v">BEFORE / AFTER · drag</div><div class="ba" id="ba"><img src="${photoSrc(ba[0])}" alt="${esc(ba[0].t)}"><img class="after" src="${photoSrc(ba[1])}" alt="${esc(ba[1].t)}"><div class="hand"></div><span class="lb" style="left:8px">${esc(ba[0].d.split('·')[0].trim() || 'Before')}</span><span class="lb" style="right:8px">${esc(ba[1].d.split('·')[0].trim() || 'After')}</span></div></div>` : ''}
  <div class="card">${lh(`${esc(p.record.toUpperCase())} · PHOTOS · ${ph.length}`, 'photo')}${ph.length ? `<div class="pgrid">${ph.map(x => { const i = p.photos.indexOf(x); return `<div class="pc"><button data-ph="${i}" aria-label="Open ${esc(x.t)}"><img src="${photoSrc(x)}" alt=""></button><input class="flat cap" data-fk="pc${i}" data-pc="${i}" value="${esc(x.t)}" aria-label="Caption"></div>`; }).join('')}</div>` : '<p class="mini" style="margin:2px 0 0">Nothing recorded yet. Photos you add are pinned to the map where they belong.</p>'}</div>
  <div class="card">${lh(`CARE · ${p.care.length}`, 'care')}${p.care.map((c, i) => `<div class="care"><input class="flat" data-fk="ct${i}" data-ct="${i}" value="${esc(c.t)}" aria-label="Care task"><input class="flat when" data-fk="ce${i}" data-ce="${i}" value="${esc(c.every)}" aria-label="How often"><button class="xb" data-cx="${i}" aria-label="Delete">✕</button></div><div class="mini" style="margin:-2px 0 4px 6px">Next: ${esc(c.next || nextDue(c.every))} · repeats as its own Orbit journey</div>`).join('') || `<p class="mini" style="margin:2px 0 0">${p.status === 'done' ? 'No upkeep reminders yet.' : 'When this is finished, Atlas suggests the upkeep that keeps it working.'}</p>`}${p.status === 'done' && !p.care.length ? `<div class="row"><button class="btn g" id="sugCare">✦ Suggest care for the ${esc(THINGS[p.thing].name.toLowerCase())}</button></div>` : ''}</div>
  <div class="card">${lh(`NOTES · ${p.notes.length}`, 'note')}${p.notes.map((n, i) => `<div class="pm"><div class="grow"><input class="flat" data-fk="no${i}" data-no="${i}" value="${esc(n.t)}" aria-label="Note"><div class="mini" style="margin:0 6px">${esc(n.s)}</div></div><button class="xb" data-nrm="${i}" aria-label="Delete note">✕</button></div>`).join('') || '<p class="mini" style="margin:2px 0 0">No notes.</p>'}</div>
  ${p.status !== 'done' ? `<div class="card"><div class="k">COMING · ${esc(p.coming[0])}</div><h3>${esc(p.coming[1])}</h3><p>${esc(p.coming[2])}</p></div>` : ''}
  <div class="card"><div class="k v">${esc(THINGS[p.thing].name.toUpperCase())} · HISTORY</div>${histHTML(p.thing)}</div>`;
}
function nextDue(every) { const mo = { 'Every year': 12, 'Every 2 years': 24, 'Every 3–4 years': 36, 'Every 3 months': 3, 'Every 6 months': 6 }[every]; const M = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']; if (mo) { const t = 10 + mo; return `${M[t % 12]} ${TODAY + Math.floor(t / 12)}`; } if (/fall/i.test(every)) return `Oct ${TODAY + 1}`; if (/spring/i.test(every)) return `Apr ${TODAY + 1}`; return 'set a date'; }

/* edits */
function changed(msg, undo) { save(); applyLook(); buildTags(); render(); drawPop(); if (msg) toast(msg, undo ? () => { undo(); save(); applyLook(); buildTags(); render(); redrawDetails(); } : null); }
function inlineAdd(btn, ph, onAdd, extra) {
  const slot = btn.closest('.card, .sec').querySelector('.inlslot'); if (slot.innerHTML) { slot.querySelector('input').focus(); return; }
  slot.innerHTML = `<div class="inl"><input placeholder="${esc(ph)}" aria-label="${esc(ph)}">${extra ? extra[0] : ''}<button class="go">Add</button></div>`;
  const i = slot.querySelector('input'); const add = () => { const v = i.value.trim(); if (!v) { i.focus(); return; } onAdd(v); };
  slot.querySelector('.go').addEventListener('click', add); if (extra) extra[1](slot);
  i.addEventListener('keydown', e => { if (e.key === 'Enter') add(); if (e.key === 'Escape') slot.innerHTML = ''; });
  i.addEventListener('blur', () => setTimeout(() => { if (slot.contains(document.activeElement) || i.value.trim()) return; slot.innerHTML = ''; }, 180));
  btn.textContent = '＋ Add'; i.focus();
}
function flatKeys(root) { root.querySelectorAll('input.flat').forEach(i => i.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); i.blur(); } if (e.key === 'Escape') { i.value = i.defaultValue; i.blur(); } })); }
function editText(root, attr, get, set, label) { root.querySelectorAll(`[data-${attr}]`).forEach(inp => inp.addEventListener('change', () => { const k = inp.dataset[attr]; const v = inp.value.trim(); const was = get(k); if (!v || v === was) { inp.value = was; return; } set(k, v); save(); inp.defaultValue = v; buildTags(); toast(`${label} saved`, () => { set(k, was); save(); render(); redrawDetails(); }); })); }
function bind(B, p) {
  flatKeys(B);
  B.querySelectorAll('[data-p]').forEach(b => b.addEventListener('click', () => go(b.dataset.p)));
  const ro = $('#reopen'); if (ro) ro.addEventListener('click', () => { const was = { s: p.status, h: p.here }; applyEffect(p, false); p.status = 'active'; p.here = Math.max(0, p.stops.length - 1); p.where = `${p.stops[p.here]} · reopened`; changed(`${p.name} reopened`, () => { p.status = was.s; p.here = was.h; if (p.status === 'done') applyEffect(p, true); }); });
  const tr = $('#toRec'); if (tr) tr.addEventListener('click', () => { S.tab = 'record'; S.xray = true; applyView(); render(); });
  const sp = $('#startP'); if (sp) sp.addEventListener('click', () => { p.status = 'active'; p.where = `${p.stops[p.here]} · started today`; changed(`${p.name} started`, () => { p.status = 'planned'; }); });
  B.querySelectorAll('[data-ch]').forEach(b => b.addEventListener('click', () => { const n = p.needs.find(x => x.id === b.dataset.ch), i = +b.dataset.i; decide(p, n, n.opts[i].n, n.cost ? n.cost[i] : 0); }));
  B.querySelectorAll('[data-own]').forEach(b => b.addEventListener('click', () => { const n = p.needs.find(x => x.id === b.dataset.own); const row = b.closest('.ownrow'); row.outerHTML = `<div class="inl own"><input id="ownIn" placeholder="Exactly what you want — e.g. Fireclay 3×12, Sea Glass" aria-label="Your choice"><input id="ownC" class="c" inputmode="decimal" placeholder="$ extra" aria-label="Extra cost"><button class="go" id="ownOk">Save</button></div>`; const i = $('#ownIn'); i.focus(); const ok = () => { const v = i.value.trim(); if (!v) { i.focus(); return; } decide(p, n, v, numOf($('#ownC').value) || 0); }; $('#ownOk').addEventListener('click', ok); [i, $('#ownC')].forEach(x => x.addEventListener('keydown', e => { if (e.key === 'Enter') ok(); if (e.key === 'Escape') render(); })); }));
  B.querySelectorAll('[data-snz]').forEach(b => b.addEventListener('click', () => { const n = p.needs.find(x => x.id === b.dataset.snz); const i = p.needs.indexOf(n); p.needs.splice(i, 1); p.later = p.later || []; p.later.push(n); changed(`Moved “${n.title}” to later`, () => { p.later = p.later.filter(x => x !== n); p.needs.splice(i, 0, n); }); }));
  B.querySelectorAll('[data-back]').forEach(b => b.addEventListener('click', () => { const n = p.later.splice(+b.dataset.back, 1)[0]; p.needs.push(n); changed(`${n.title} · back on the list`); }));
  B.querySelectorAll('[data-cmp]').forEach(b => b.addEventListener('click', () => { const n = p.needs.find(x => x.id === b.dataset.cmp), i = +b.dataset.i; const k = p.needs.indexOf(n); p.needs.splice(k, 1); const a = { area: n.area, ok: true, t: `Going with ${n.picks[i]}`, s: 'Your decision · compared like for like' }; p.agreed.unshift(a); buzz([15, 25, 40]); changed(`Going with ${n.picks[i]} · saved to Agreed`, () => { p.needs.splice(k, 0, n); p.agreed = p.agreed.filter(x => x !== a); }); }));
  B.querySelectorAll('[data-done]').forEach(b => b.addEventListener('click', () => { const n = p.needs.find(x => x.id === b.dataset.done); const k = p.needs.indexOf(n); p.needs.splice(k, 1); buzz(12); changed(`${n.title} · done`, () => p.needs.splice(k, 0, n)); }));
  editText(B, 'nt', k => p.needs.find(x => x.id === k).title, (k, v) => p.needs.find(x => x.id === k).title = v, 'To-do');
  B.querySelectorAll('[data-got]').forEach(b => b.addEventListener('click', () => { const w = p.waiting.splice(+b.dataset.got, 1)[0]; const a = { area: null, ok: true, t: w.what.replace(/^Confirm: /, ''), s: `${w.who} · received just now` }; p.agreed.unshift(a); changed(`${w.what} · received`, () => { p.waiting.push(w); p.agreed = p.agreed.filter(x => x !== a); }); }));
  // add buttons at the top of each list
  B.querySelectorAll('[data-add]').forEach(b => b.addEventListener('click', () => {
    const t = b.dataset.add;
    if (t === 'todo') inlineAdd(b, 'Add a to-do or decision…', v => { const n = { id: 'u' + Date.now(), area: S.area, kind: 'task', k: 'TO-DO', title: v[0].toUpperCase() + v.slice(1), p: '' }; p.needs.unshift(n); reAdd = 'todo'; changed(`Added “${n.title}”`, () => { p.needs = p.needs.filter(x => x !== n); }); });
    if (t === 'cost') inlineAdd(b, 'e.g. $1,150 skylight change order, Jamie', v => { const r = parseCost(v); if (!r) { toast('Include an amount, like $250'); return; } r.area = S.area; p.lines.unshift(r); buzz(10); changed(`${KIND[r.kind]} · ${r.l} · ${money(r.v)}`, () => { p.lines = p.lines.filter(x => x !== r); }); }, [`<button class="cam" aria-label="Scan a receipt">📷</button>`, slot => slot.querySelector('.cam').addEventListener('click', () => pickPhoto(src => { const r = { l: 'Receipt', v: 0, kind: 'paid', who: '', receipt: src }; p.lines.unshift(r); render(); toast('Receipt added — type the amount'); setTimeout(() => { const el = $('#sbody').querySelector(`[data-lv="0"]`); if (el) el.focus(); }, 60); }))]);
    if (t === 'agree') inlineAdd(b, 'Something that was agreed…', v => { const a = { area: S.area, ok: false, id: 'u' + Date.now(), t: v, s: 'You typed it · not in writing yet', draft: `Hi — confirming what we agreed: ${v}. Could you reply to confirm? Thanks!`, to: 'them' }; p.agreed.unshift(a); changed('Added · “Make it official” gets it in writing', () => { p.agreed = p.agreed.filter(x => x !== a); }); });
    if (t === 'note') inlineAdd(b, 'Add a note…', v => { const n = { t: v, s: 'You · just now' }; p.notes.unshift(n); reAdd = 'note'; changed('Note added', () => { p.notes = p.notes.filter(x => x !== n); }); });
    if (t === 'care') inlineAdd(b, 'e.g. Flush the tank every year', v => { const m = v.match(/\b(every .+|each .+|yearly|monthly)$/i); const c = { t: (m ? v.slice(0, m.index) : v).trim() || v, every: m ? m[1][0].toUpperCase() + m[1].slice(1) : 'Every year' }; c.next = nextDue(c.every); p.care.unshift(c); changed(`Care added · ${c.t}`, () => { p.care = p.care.filter(x => x !== c); }); });
    if (t === 'photo') addPhotoTo(p);
  }));
  // costs inline
  B.querySelectorAll('[data-lk]').forEach(s => s.addEventListener('change', () => { const l = p.lines[+s.dataset.lk]; const was = { kind: l.kind, v: l.v }; l.kind = s.value; l.v = l.kind === 'insurance' ? -Math.abs(l.v) : Math.abs(l.v); delete l.sample; changed(`Now ${KIND[l.kind].toLowerCase()}`, () => Object.assign(l, was)); }));
  editText(B, 'll', i => p.lines[i].l, (i, v) => p.lines[i].l = v, 'Cost');
  B.querySelectorAll('[data-lw]').forEach(inp => inp.addEventListener('change', () => { const l = p.lines[+inp.dataset.lw]; const was = l.who; l.who = inp.value.trim(); save(); toast('Saved', () => { l.who = was; save(); render(); }); }));
  B.querySelectorAll('[data-lv]').forEach(inp => inp.addEventListener('change', () => { const l = p.lines[+inp.dataset.lv]; const v = numOf(inp.value); if (v == null) { inp.value = l.v ? money(l.v) : ''; return; } const was = l.v; l.v = l.kind === 'insurance' ? -Math.abs(v) : Math.abs(v); delete l.sample; changed(`${l.l} · ${money(l.v)}`, () => l.v = was); }));
  B.querySelectorAll('[data-lx]').forEach(b => b.addEventListener('click', () => { const i = +b.dataset.lx; const l = p.lines.splice(i, 1)[0]; changed(`Deleted ${l.l}`, () => p.lines.splice(i, 0, l)); }));
  const bi = $('#budIn'); if (bi) bi.addEventListener('change', () => { const v = numOf(bi.value); const was = p.budget; p.budget = v == null ? 0 : Math.abs(v); changed(p.budget ? `Budget ${money(p.budget)}` : 'Budget cleared', () => p.budget = was); });
  // agreed
  editText(B, 'at', i => p.agreed[i].t, (i, v) => p.agreed[i].t = v, 'Agreement');
  B.querySelectorAll('[data-off]').forEach(b => b.addEventListener('click', () => officialFor(p, p.agreed[+b.dataset.off])));
  B.querySelectorAll('[data-conf]').forEach(b => b.addEventListener('click', () => { const a = p.agreed[+b.dataset.conf]; const was = { ok: a.ok, s: a.s }; a.ok = true; a.s = 'Confirmed in writing · just now'; p.waiting = p.waiting.filter(w => !w.what.toLowerCase().includes(a.t.toLowerCase().slice(0, 18))); buzz([10, 20]); changed('Marked as confirmed in writing', () => { a.ok = was.ok; a.s = was.s; }); }));
  B.querySelectorAll('[data-ax]').forEach(b => b.addEventListener('click', () => { const i = +b.dataset.ax; const a = p.agreed.splice(i, 1)[0]; changed('Deleted', () => p.agreed.splice(i, 0, a)); }));
  // record
  B.querySelectorAll('[data-ph]').forEach(b => b.addEventListener('click', () => openPhoto(p, p.photos[+b.dataset.ph])));
  editText(B, 'pc', i => p.photos[i].t, (i, v) => p.photos[i].t = v, 'Caption');
  editText(B, 'no', i => p.notes[i].t, (i, v) => p.notes[i].t = v, 'Note');
  B.querySelectorAll('[data-nrm]').forEach(b => b.addEventListener('click', () => { const i = +b.dataset.nrm; const n = p.notes.splice(i, 1)[0]; changed('Note deleted', () => p.notes.splice(i, 0, n)); }));
  editText(B, 'ct', i => p.care[i].t, (i, v) => p.care[i].t = v, 'Care');
  B.querySelectorAll('[data-ce]').forEach(inp => inp.addEventListener('change', () => { const c = p.care[+inp.dataset.ce]; const v = inp.value.trim(); if (!v) { inp.value = c.every; return; } const was = { every: c.every, next: c.next }; c.every = v; c.next = nextDue(v); changed('How often · saved', () => Object.assign(c, was)); }));
  B.querySelectorAll('[data-cx]').forEach(b => b.addEventListener('click', () => { const i = +b.dataset.cx; const c = p.care.splice(i, 1)[0]; changed(`Removed ${c.t}`, () => p.care.splice(i, 0, c)); }));
  const sc = $('#sugCare'); if (sc) sc.addEventListener('click', () => careSetup(p));
  const ba = $('#ba'); if (ba) { const set = e => { const r = ba.getBoundingClientRect(); const x = clamp((e.clientX - r.left) / r.width, .02, .98); ba.querySelector('.after').style.clipPath = `inset(0 0 0 ${x * 100}%)`; ba.querySelector('.hand').style.left = x * 100 + '%'; }; let on = false; ba.addEventListener('pointerdown', e => { on = true; ba.setPointerCapture(e.pointerId); set(e); }); ba.addEventListener('pointermove', e => on && set(e)); ba.addEventListener('pointerup', () => on = false); ba.addEventListener('pointercancel', () => on = false); }
}
function addPhotoTo(p, area, caption) { pickPhoto(src => { const k = area || S.area || Object.keys(p.areas)[0]; const pos = areaPos(k).slice(); pos[0] += (Math.random() - .5) * 1.4; pos[1] += (Math.random() - .5) * .6; const ph = { area: k, t: caption || 'Photo', d: 'Added just now', pos, src }; p.photos.push(ph); if (S.p === p.id && S.tab !== 'record') { S.tab = 'record'; S.xray = true; applyView(); } changed(`Photo pinned to ${p.areas[k] || p.name} — type a caption`, () => { p.photos = p.photos.filter(x => x !== ph); }); setTimeout(() => { const el = $('#sbody').querySelector(`[data-pc="${p.photos.indexOf(ph)}"]`); if (el) { el.focus(); el.select(); } }, 80); }); }
function decide(p, n, val, cost) {
  const k = p.needs.indexOf(n); p.needs.splice(k, 1); const before = p.details.map(f => ({ ...f }));
  if (n.field) { const f = p.details.find(x => x.k === n.field); if (f) f.v = val; else p.details.push(F(n.field, val)); }
  let line = null; if (cost) { line = { l: `${n.title}: ${val}`, v: cost, kind: 'change', who: '' }; p.lines.push(line); }
  const a = { area: n.area, ok: true, t: `${n.title}: ${val}`, s: 'Your decision · just now' }; p.agreed.unshift(a); buzz([12, 20, 30]);
  changed(`${n.title}: ${val}${line ? ` · ${money(cost)} added to money` : ''}`, () => { p.needs.splice(k, 0, n); p.agreed = p.agreed.filter(x => x !== a); p.details = before; if (line) p.lines = p.lines.filter(x => x !== line); });
}
function officialFor(p, a) {
  modal(`<h3>Make it official</h3><p class="mini" style="margin:0">Drafted from your notes — edit anything. You send it; Atlas never sends for you.</p><textarea class="bigin" id="drTxt" style="margin-top:10px;min-height:130px">${esc(a.draft || `Hi — confirming what we agreed: ${a.t}. Could you reply to confirm? Thanks!`)}</textarea><div class="row"><button class="btn" id="cp">Copy &amp; mark as asked</button><button class="btn g" id="cx">Cancel</button></div>`, () => {
    $('#cp').addEventListener('click', () => { const t = $('#drTxt').value; copyText(t); const was = a.s; const to = a.to || 'them'; a.s = 'Asked in writing just now · waiting for the reply'; const w = { who: to[0].toUpperCase() + to.slice(1), what: 'Confirm: ' + a.t.toLowerCase(), when: 'this week' }; p.waiting.push(w); closeModal(); changed(`Copied — paste it into your message to ${to}`, () => { a.s = was; p.waiting = p.waiting.filter(x => x !== w); }); });
    $('#cx').addEventListener('click', closeModal); });
}
function copyText(t) { try { navigator.clipboard.writeText(t).catch(() => manualCopy(t)); } catch (e) { manualCopy(t); } }
function manualCopy(t) { setTimeout(() => modal(`<h3>Copy this</h3><textarea class="bigin" id="mc" style="min-height:200px">${esc(t)}</textarea><div class="row"><button class="btn g" id="cx">Done</button></div>`, () => { $('#mc')?.select(); $('#cx').addEventListener('click', closeModal); }), 60); }

/* ======================================================================
   steps · the reminder that comes with the next step · care when it’s done
   ====================================================================== */
const REMIND = {
  Demo: ['Photograph what’s exposed', 'Old work comes out now — the one chance to see inside these walls. Wide shots first, then close-ups.'],
  Build: ['Walk the walls before drywall', 'Each wall wide, then close on outlets, pipes and wiring. Future-you will know where every pipe runs.'],
  'Tear-off': ['Photograph the bare roof deck', 'Rot and gaps only show while the shingles are off — and it backs up the per-sheet charge.'],
  Remove: ['Photograph the sheathing on every side', 'It’s the only time you’ll see it. Atlas pins each photo to its side of the house.'],
  'Call 811 & dig': ['Photograph the locate marks', 'The paint and flags fade in days.'],
  Install: ['Snap the label and serial number', 'Atlas reads model, serial and warranty from the photo.'],
  'Wrap & install': ['Photograph the wrap and window flashing', 'Proof it was done right, before it’s covered.'],
  Photos: ['Condition photos, room by room', 'Same rooms, same angles as move-in.']
};
const CARE_SUG = { wh: [['Flush the tank', 'Every year'], ['Test the pressure-relief valve', 'Every year']], yard: [['Stain the fence', 'Every 3–4 years'], ['Winterize the irrigation', 'Every fall']], roof: [['Look over the roof', 'Every year'], ['Clean the gutters', 'Spring & fall']], bath: [['Re-seal the grout', 'Every 2 years'], ['Clean the fan cover', 'Every 6 months']], siding: [['Wash the siding', 'Every year'], ['Check caulk at windows', 'Every year']], kitchen: [['Clean the range-hood filter', 'Every 3 months'], ['Re-seal the counters', 'Every year']], deck: [['Seal the deck', 'Every 2 years']], stove: [['Chimney sweep', 'Every fall']] };
const CARE_BY = { Outside: [['Look it over', 'Every year']], Systems: [['Service it', 'Every year']], Inside: [['Touch up', 'Every 2 years']], Yard: [['Seasonal clean-up', 'Spring & fall']] };
function openStep(p, i) {
  const s = p.stops[i]; const st = p.status === 'done' || i < p.here ? 'done' : i === p.here ? 'here' : 'later'; const open = p.needs.length; const rm = REMIND[s];
  modal(`<div style="font-size:11px;font-weight:800;color:${st === 'done' ? 'var(--ok)' : st === 'here' ? 'var(--v)' : 'var(--sub)'}">STEP ${i + 1} OF ${p.stops.length} · ${st === 'done' ? 'DONE' : st === 'here' ? (p.status === 'planned' ? 'FIRST STEP' : 'YOU ARE HERE') : 'COMING UP'}</div><h3 style="margin-top:2px">${esc(s)}</h3><p style="margin:0;color:var(--sub)">${esc(STEP_INFO[s] || 'A step in this job. Rename it in Details to match how yours really goes.')}</p>
    ${rm ? `<div class="card" style="margin-top:10px"><div class="k v">📷 AT THIS STEP</div><b style="font-size:14px">${esc(rm[0])}</b></div>` : ''}
    ${st === 'here' && open ? `<div class="card" style="margin-top:10px"><div class="k">STILL OPEN</div>${p.needs.slice(0, 3).map(n => `<div style="font-size:14px;padding:3px 0">• ${esc(n.title)}</div>`).join('')}</div>` : ''}
    <div class="row">${st === 'here' && p.status !== 'done' ? `<button class="btn" id="stDone">Mark “${esc(s)}” done</button>` : ''}${st === 'done' && p.status !== 'done' ? `<button class="btn g" id="stBack">Go back to this step</button>` : ''}${st === 'later' ? `<button class="btn g" id="stJump">Skip ahead to here</button>` : ''}<button class="btn g" id="stRen">Edit steps</button><button class="btn g" id="cx">Close</button></div>`, () => {
    const adv = () => { const was = { h: p.here, s: p.status, w: p.where, y: p.year }; p.here = i + 1; if (p.here >= p.stops.length) { p.status = 'done'; p.year = TODAY; p.where = 'finished just now'; applyEffect(p, true); } else { p.status = 'active'; p.where = `${p.stops[p.here]} · started today`; } closeModal(); buzz([20, 30, 50]); changed(p.status === 'done' ? `${p.name} finished · kept as a record` : `On to ${p.stops[p.here]}`, () => { if (p.status === 'done' && was.s !== 'done') applyEffect(p, false); p.here = was.h; p.status = was.s; p.where = was.w; p.year = was.y; }); setTimeout(() => afterStep(p), 260); };
    const d = $('#stDone'); if (d) d.addEventListener('click', () => { if (open) { $('#mcard').querySelector('.row').innerHTML = `<p style="margin:0 0 8px;font-size:14px;width:100%">${open} thing${open > 1 ? 's' : ''} still need${open > 1 ? '' : 's'} you. Move on anyway? They stay on the list.</p><button class="btn" id="stYes">Move on</button><button class="btn g" id="cx2">Not yet</button>`; $('#stYes').addEventListener('click', adv); $('#cx2').addEventListener('click', closeModal); } else adv(); });
    const bk = $('#stBack'); if (bk) bk.addEventListener('click', () => { const was = { h: p.here, w: p.where }; p.here = i; p.where = `${s} · back to this step`; closeModal(); changed(`Back to ${s}`, () => { p.here = was.h; p.where = was.w; }); });
    const j = $('#stJump'); if (j) j.addEventListener('click', () => { const was = { h: p.here, s: p.status, w: p.where }; p.here = i; p.status = 'active'; p.where = `${s} · skipped ahead`; closeModal(); changed(`Skipped ahead to ${s}`, () => { p.here = was.h; p.status = was.s; p.where = was.w; }); setTimeout(() => afterStep(p), 260); });
    $('#stRen').addEventListener('click', () => { closeModal(); openDetails('steps'); }); $('#cx').addEventListener('click', closeModal); });
}
function afterStep(p) {
  if (p.status === 'done') { if (!p.effect || p.effect.kind !== 'remove') careSetup(p); return; }
  const s = p.stops[p.here]; const rm = REMIND[s]; if (!rm) return;
  modal(`<div style="font-size:11px;font-weight:800;color:var(--v)">NOW ON · ${esc(s.toUpperCase())}</div><h3 style="margin-top:2px">📷 ${esc(rm[0])}</h3><p style="margin:0;color:var(--sub)">${esc(rm[1])}</p><div class="row"><button class="btn" id="rmNow">Take it now</button><button class="btn g" id="rmLater">Remind me</button><button class="btn g" id="cx">Skip</button></div>`, () => {
    $('#rmNow').addEventListener('click', () => { closeModal(); addPhotoTo(p, null, rm[0].replace(/^(Photograph|Snap) /, '').replace(/^the /, '')[0].toUpperCase() + rm[0].replace(/^(Photograph|Snap) /, '').replace(/^the /, '').slice(1)); });
    $('#rmLater').addEventListener('click', () => { const n = { id: 'r' + Date.now(), area: null, kind: 'task', k: s.toUpperCase(), title: rm[0], p: rm[1] }; p.needs.unshift(n); closeModal(); changed(`Reminder added · ${rm[0]}`, () => { p.needs = p.needs.filter(x => x !== n); }); });
    $('#cx').addEventListener('click', closeModal); });
}
function careSetup(p) {
  const sug = (CARE_SUG[p.thing] || CARE_BY[p.place] || CARE_BY.Inside).filter(([t]) => !p.care.some(c => c.t === t)); if (!sug.length) return; const on = new Set(sug.map((_, i) => i));
  modal(`<div style="font-size:11px;font-weight:800;color:var(--ok)">✓ ${esc(p.name.toUpperCase())} · FINISHED</div><h3 style="margin-top:2px">Keep it working</h3><p style="margin:0;color:var(--sub)">Upkeep for the ${esc(THINGS[p.thing].name.toLowerCase())} — each repeats as its own Orbit journey. Untick what you don’t want.</p><div class="checks card" style="margin-top:10px">${sug.map(([t, e], i) => `<label><input type="checkbox" data-cs="${i}" checked><span><b>${esc(t)}</b><small>${esc(e)} · next ${esc(nextDue(e))}</small></span></label>`).join('')}</div><div class="row"><button class="btn" id="csOk">Add care reminders</button><button class="btn g" id="cx">Not now</button></div>`, () => {
    $('#mcard').querySelectorAll('[data-cs]').forEach(c => c.addEventListener('change', () => c.checked ? on.add(+c.dataset.cs) : on.delete(+c.dataset.cs)));
    $('#csOk').addEventListener('click', () => { const add = sug.filter((_, i) => on.has(i)).map(([t, e]) => ({ t, every: e, next: nextDue(e) })); closeModal(); if (!add.length) return; p.care.push(...add); S.tab = 'record'; S.xray = true; applyView(); changed(`${add.length} care reminder${add.length > 1 ? 's' : ''} added`, () => { p.care = p.care.filter(c => !add.includes(c)); }); });
    $('#cx').addEventListener('click', closeModal); });
}

/* ======================================================================
   full-screen sheets (stacked)
   ====================================================================== */
function full(title, draw, act) { fullStack.push({ title, draw, act }); showTop(); arm(); }
function showTop() { const t = fullStack[fullStack.length - 1]; if (!t) { $('#full').classList.remove('show'); return; } $('#fTitle').textContent = t.title; $('#fAct').innerHTML = ''; $('#fBody').innerHTML = ''; $('#fBody').scrollTop = 0; $('#full').classList.add('show'); t.draw(); if (t.act) { $('#fAct').innerHTML = `<button id="fActB">${esc(t.act[0])}</button>`; $('#fActB').addEventListener('click', t.act[1]); } }
function popFull(quiet) { fullStack.pop(); showTop(); if (!quiet) render(); }
function redrawDetails() { const t = fullStack[fullStack.length - 1]; if (t && t.title === 'Details') t.draw(); }
$('#fBack').addEventListener('click', navBack);

/* ======================================================================
   switcher: ask or search · homes · new renovation
   ====================================================================== */
const STOP = new Set('what whats what’s when where who whom which how is are was were be been the a an of in on for to my our we i did do does it its at by and or with much many any about tell me show find there this that get got have has had'.split(' '));
function corpus() {
  const out = [];
  PROJ.forEach(q => { const base = { id: q.id, proj: q.name, icon: q.icon }; const src = s => `${q.name} · ${s}`;
    q.details.forEach(f => f.v && out.push({ ...base, a: f.v, k: f.k, src: src('Details'), txt: `${f.k} ${f.v}`, w: 1.2 }));
    q.agreed.forEach(a => out.push({ ...base, a: a.t, k: a.ok ? 'Agreed in writing' : 'Said, not in writing yet', src: src(a.s), txt: `${a.t} ${a.s} agreed`, w: 1 }));
    q.notes.forEach(n => out.push({ ...base, a: n.t, k: 'Note', src: src(n.s), txt: n.t, w: 1 }));
    (q.people || []).forEach(([n, r]) => out.push({ ...base, a: n, k: r, src: src('People'), txt: `${n} ${r} who contractor person phone`, w: 1 }));
    q.lines.forEach(l => out.push({ ...base, a: money(l.v), k: l.l, src: src(KIND[l.kind].toLowerCase() + (l.who ? ' · ' + l.who : '')), txt: `${l.l} ${l.who || ''} cost price paid spent money ${KIND[l.kind]}`, w: 1 }));
    q.care.forEach(c => out.push({ ...base, a: c.t, k: `${c.every} · next ${c.next || nextDue(c.every)}`, src: src('Care'), txt: `${c.t} ${c.every} care upkeep maintain`, w: 1 }));
    q.photos.forEach(ph => out.push({ ...base, a: ph.t, k: 'Photo', src: src(ph.d), txt: `${ph.t} photo picture`, w: .8 }));
    out.push({ ...base, a: q.status === 'done' ? `Finished in ${q.year}` : q.status === 'planned' ? `Planned for ${q.year}` : `In progress · ${q.stops[Math.min(q.here, q.stops.length - 1)]}`, k: q.name, src: src('Status'), txt: `${q.name} status when year done finished ${q.year} ${q.where}`, w: .9, yr: q.year });
  });
  Object.entries(THINGS).forEach(([k, t]) => t.hist.forEach(r => { const pq = PROJ.find(q => q.thing === k); out.push({ id: pq ? pq.id : null, proj: t.name, icon: '🕓', a: `${r.y} · ${r.t}`, k: '', src: `${t.name} history · ${r.s}`, txt: `${t.name} ${r.t} ${r.s}`, w: 1, yr: r.y }); }));
  return out;
}
function ask(q) {
  const stem = w => w.replace(/(ing|ed|es|s)$/, '');
  const words = q.toLowerCase().replace(/[^\w\s’'-]/g, ' ').split(/\s+/).filter(w => w && !STOP.has(w)).map(stem).filter(w => w.length > 1); if (!words.length) return [];
  const isWhen = /\bwhen\b|\bwhat year\b|\bhow old\b/i.test(q), isCol = /\bcolou?r\b|\bpaint\b/i.test(q), isCost = /\bcost|\bpaid|\bspen[dt]|\bprice|how much/i.test(q), isWho = /\bwho\b/i.test(q);
  return corpus().map(e => { const t = `${e.txt} ${e.proj} ${e.k}`.toLowerCase(); let s = 0, hit = 0; words.forEach(w => { if (['colour', 'color', 'year', 'cost'].includes(w)) return; if (t.includes(w)) { hit++; s += e.proj.toLowerCase().includes(w) || (e.k || '').toLowerCase().includes(w) ? 2 : 1.5; } }); if (!hit) return { ...e, s: 0 };
    if (isWhen && e.yr) s += 1.5 + (e.yr - 1950) / 200; if (isCol && /colou?r|paint|walls|trim/i.test(e.k || '')) s += 2.5; if (isCost && /quote|paid|cost|change|covered/.test(e.src)) s += 2; if (isWho && /People/.test(e.src)) s += 2;
    return { ...e, s: s * e.w }; }).filter(e => e.s > 0).sort((a, b) => b.s - a.s);
}
function openSwitcher(focusAsk) {
  full('Homes & projects', () => {
    const needN = k => projOf(k).reduce((s, q) => s + (q.status === 'done' ? 0 : q.needs.length), 0);
    const H0 = HOMES[S.home];
    $('#fBody').innerHTML = `<input class="search" id="swQ" placeholder="Ask or search — “when was the roof done?”" aria-label="Ask or search" enterkeyhint="search">
      <div id="swTop"><button class="homepick" id="swHome" aria-expanded="false"><span class="ic">${H0.icon}</span><span class="grow"><b>${esc(H0.name)}</b><small>${esc(H0.kind)}</small></span><span class="chg">${Object.keys(HOMES).length > 1 ? 'Switch home' : 'Homes'} <i>▾</i></span></button>
      <div class="homelist" id="swHL" hidden>${Object.entries(HOMES).map(([k, h]) => { const n = needN(k); return `<button data-h="${k}" class="${k === S.home ? 'on' : ''}"><span class="ic">${h.icon}</span><span class="grow"><b>${esc(h.name)}</b><small>${projOf(k).length} project${projOf(k).length === 1 ? '' : 's'}${n ? ` · ${n} need you` : ''}</small></span>${k === S.home ? '<span class="here">Viewing</span>' : '<span class="go">›</span>'}</button>`; }).join('')}<button data-addh="1"><span class="ic">＋</span><span class="grow"><b>Add a home</b><small>House, apartment, cabin, rental…</small></span></button></div>
      <div class="lh" style="margin-top:14px"><div class="k">PROJECTS</div><button class="addb" data-newm="1">＋ New</button></div>${newMenuHTML()}</div>
      <div id="swA"></div><div id="swL"></div><div class="row" style="margin-top:18px"><button class="btn g" id="rst">Reset demo</button></div>`;
    const draw = () => { const q = $('#swQ').value.trim(); const A = $('#swA'), L = $('#swL'); $('#swTop').style.display = q ? 'none' : '';
      if (!q) { A.innerHTML = ''; L.innerHTML = groupsHTML(projOf(S.home)); bindList(L); return; }
      const r = ask(q); const top = r[0];
      A.innerHTML = top ? `<div class="answer"><div class="k">✦ FROM YOUR RECORDS</div><h3>${esc(top.a)}</h3><p>${esc(top.k && top.k !== top.a ? top.k + ' · ' : '')}${esc(top.src)}</p>${top.id ? `<div class="row"><button class="btn" data-p="${top.id}">Open ${esc(P(top.id).name)}</button></div>` : ''}</div>${r.length > 1 ? `<div class="gh">ALSO MATCHES</div>${r.slice(1, 6).map(e => `<button class="proj" ${e.id ? `data-p="${e.id}"` : ''}><span class="ic">${e.icon}</span><span style="flex:1;min-width:0"><b>${esc(e.a)}</b><small>${esc(e.k && e.k !== e.a ? e.k + ' · ' : '')}${esc(e.src)}</small></span></button>`).join('')}` : ''}` : `<div class="empty"><b>Nothing in your records yet</b>Try other words — or ask Atlas in the app to look it up online.</div>`;
      const ql = q.toLowerCase(); const PL = PROJ.filter(x => [x.name, x.place, THINGS[x.thing]?.name, x.year, HOMES[x.home].name, ...(x.people || []).map(p => p[0]), ...x.details.map(f => f.v)].join(' ').toLowerCase().includes(ql));
      L.innerHTML = PL.length ? `<div class="gh">PROJECTS</div>` + groupsHTML(PL, true) : ''; bindList(A); bindList(L); };
    $('#swQ').addEventListener('input', draw); $('#swQ').addEventListener('keydown', e => { if (e.key === 'Enter') $('#swQ').blur(); });
    $('#swHome').addEventListener('click', () => { const hl = $('#swHL'); hl.hidden = !hl.hidden; $('#swHome').setAttribute('aria-expanded', !hl.hidden); $('#swHome').classList.toggle('open', !hl.hidden); });
    $('#swHL').querySelectorAll('[data-h]').forEach(x => x.addEventListener('click', () => goHome(x.dataset.h)));
    $('#swHL').querySelector('[data-addh]').addEventListener('click', openFindHome);
    bindNewMenu($('#swTop'));
    $('#rst').addEventListener('click', () => modal(`<h3>Reset the demo?</h3><p class="mini" style="margin:0">Everything you added or changed in this mockup goes back to the example data.</p><div class="row"><button class="btn danger" id="rY">Reset</button><button class="btn g" id="cx">Cancel</button></div>`, () => { $('#rY').addEventListener('click', resetDemo); $('#cx').addEventListener('click', closeModal); }));
    draw(); if (focusAsk) setTimeout(() => $('#swQ')?.focus(), 80);
  });
}
const HOME_ICON = { House: '🏡', Apartment: '🏬', Condo: '🏢', Cabin: '🌲', Townhouse: '🏘', 'Mobile home': '🚐', RV: '🚌' };
function addHome() {
  let kind = 'House';
  modal(`<h3>Add a home</h3><div class="lbl">Name</div><input class="inp" id="hN" placeholder="e.g. Mom’s house"><div class="lbl">Kind</div><div class="chipset" id="hK">${Object.keys(HOME_ICON).map(k => `<button class="${k === kind ? 'on' : ''}" data-k="${k}">${HOME_ICON[k]} ${k}</button>`).join('')}</div><div class="lbl">Notes (optional)</div><input class="inp" id="hD" placeholder="e.g. 2 bed · built 1978"><p class="mini">Starts with a ready-made model. Later you can sketch the floor plan or scan rooms with your phone so the map looks like your place.</p><div class="row"><button class="btn" id="hOk">Add home</button><button class="btn g" id="cx">Cancel</button></div>`, () => {
    setTimeout(() => $('#hN')?.focus(), 50); $('#hK').querySelectorAll('button').forEach(b => b.addEventListener('click', () => { kind = b.dataset.k; $('#hK').querySelectorAll('button').forEach(x => x.classList.toggle('on', x === b)); }));
    $('#cx').addEventListener('click', closeModal);
    const ok = () => { const n = $('#hN').value.trim(); if (!n) { $('#hN')?.focus(); return; } const id = 'h' + Date.now(); HOMES[id] = { name: n, icon: HOME_ICON[kind], kind: kind + ($('#hD').value.trim() ? ' · ' + $('#hD').value.trim() : ''), model: kind === 'Cabin' ? 'cabin' : (kind === 'Apartment' || kind === 'Condo') ? 'condo' : 'main', custom: true }; closeModal(); goHome(id); toast(`${n} added`); };
    $('#hOk').addEventListener('click', ok); $('#hN').addEventListener('keydown', e => { if (e.key === 'Enter') ok(); });
  });
}
