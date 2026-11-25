/* export */
$('#exportB').addEventListener('click', () => {
  const p = cur(); let scope = p ? 'project' : 'home', preset = 'Custom'; const fmt = { pdf: true, sheet: true, zip: false, backup: false };
  const listFor = () => scope === 'project' ? [p] : scope === 'thing' ? PROJ.filter(q => q.thing === p.thing) : scope === 'home' ? projOf(S.home) : PROJ.slice();
  const titleFor = () => scope === 'project' ? p.name : scope === 'thing' ? `${THINGS[p.thing].name} — full history` : scope === 'home' ? `${HOMES[S.home].name} — home record` : 'All homes';
  full('Export', () => {
    const scopes = [p && ['project', `This project`], p && ['thing', `${THINGS[p.thing].name} history`], ['home', HOMES[S.home].name], ['all', 'All homes']].filter(Boolean); const L = listFor();
    $('#fBody').innerHTML = `<div class="lbl" style="margin-top:0">What</div><div class="chipset" id="exS">${scopes.map(([k, l]) => `<button class="${k === scope ? 'on' : ''}" data-x="${k}">${esc(l)}</button>`).join('')}</div><div class="lbl">For</div><div class="chipset" id="exP">${['Custom', 'Insurance claim', 'Selling the home', 'Taxes'].map(x => `<button class="${x === preset ? 'on' : ''}" data-pr="${x}">${x}</button>`).join('')}</div>
      <div class="card checks">${[['pdf', 'PDF binder', 'Readable: summary, details, timeline, photos, receipts'], ['sheet', 'Spreadsheet', 'Every cost — Google Sheets or CSV'], ['zip', 'Photos & files', 'Originals with dates'], ['backup', 'Full backup', 'Re-import into Atlas on another phone']].map(([k, t, d]) => `<label><input type="checkbox" data-fm="${k}" ${fmt[k] ? 'checked' : ''}><span><b>${t}</b><small>${d}</small></span></label>`).join('')}</div>
      <p class="mini">${L.length} project${L.length === 1 ? '' : 's'} · ${money(L.reduce((s, q) => s + moneyOf(q).cost, 0))} recorded · ${L.reduce((s, q) => s + q.photos.length, 0)} photos</p>`;
    $('#exS').querySelectorAll('button').forEach(b => b.addEventListener('click', () => { scope = b.dataset.x; fullStack[fullStack.length - 1].draw(); }));
    $('#exP').querySelectorAll('button').forEach(b => b.addEventListener('click', () => { preset = b.dataset.pr; if (preset === 'Insurance claim') fmt.zip = true; fullStack[fullStack.length - 1].draw(); }));
    $('#fBody').querySelectorAll('[data-fm]').forEach(c => c.addEventListener('change', () => fmt[c.dataset.fm] = c.checked));
  }, ['Create', () => { if (!Object.values(fmt).some(Boolean)) { toast('Pick at least one format'); return; } openExportResult(listFor(), titleFor(), preset, { ...fmt }); }]);
});
function csvOf(L) { const rows = [['Home', 'Project', 'Year', 'Status', 'Item', 'Kind', 'Who', 'Amount']]; L.forEach(q => q.lines.forEach(l => rows.push([HOMES[q.home].name, q.name, q.year, q.status, l.l, KIND[l.kind], l.who || '', l.v]))); return rows.map(r => r.map(c => /[",\n]/.test(String(c)) ? `"${String(c).replace(/"/g, '""')}"` : c).join(',')).join('\n'); }
function binderHTML(L, title, preset) {
  const tot = L.reduce((s, q) => s + moneyOf(q).cost, 0); const order = preset === 'Selling the home' ? L.slice().sort((a, b) => b.year - a.year) : L;
  return `<div class="binder"><h2>${esc(title)}</h2><div class="muted">${preset === 'Custom' ? '' : esc(preset) + ' · '}Prepared Nov 24, 2026 · ${L.length} project${L.length === 1 ? '' : 's'} · ${money(tot)} recorded</div>
    <h3>Summary</h3><table>${order.map(q => `<tr><td>${q.year}</td><td>${esc(q.name)}<div class="muted">${esc(HOMES[q.home].name)} · ${esc(q.place)} · ${q.status === 'done' ? 'finished' : q.status === 'active' ? 'in progress' : 'planned'}</div></td><td class="n">${moneyOf(q).cost ? money(moneyOf(q).cost) : '—'}</td></tr>`).join('')}</table>
    ${preset === 'Taxes' ? '<p class="muted">Improvement costs with receipts, by year — a starting point for your tax preparer.</p>' : ''}${preset === 'Insurance claim' ? '<p class="muted">Dated photos and receipts come first in each section.</p>' : ''}</div>` +
    order.map(q => `<div class="binder"><h2 style="font-size:18px">${q.icon} ${esc(q.name)}</h2><div class="muted">${esc(HOMES[q.home].name)} · ${q.year} · ${esc(q.where)}</div>
      ${q.photos.length ? `<h3>Photos</h3><div class="thumbs">${q.photos.map(ph => `<button style="cursor:default"><img src="${photoSrc(ph)}" alt=""></button>`).join('')}</div>` : ''}
      <h3>Details</h3><table>${q.details.filter(f => f.v).map(f => `<tr><td>${esc(f.k)}</td><td>${esc(f.v)}</td></tr>`).join('') || '<tr><td class="muted">None recorded</td></tr>'}</table>
      <h3>Costs</h3><table>${q.lines.map(l => `<tr><td>${esc(l.l)}${l.who ? ` · ${esc(l.who)}` : ''}</td><td>${KIND[l.kind]}</td><td class="n">${money(l.v)}</td></tr>`).join('') || '<tr><td class="muted">None recorded</td></tr>'}<tr><td><b>Total</b></td><td></td><td class="n"><b>${money(moneyOf(q).cost)}</b></td></tr></table>
      ${q.agreed.length ? `<h3>Agreed</h3><table>${q.agreed.map(a => `<tr><td>${a.ok ? '✓' : '?'}</td><td>${esc(a.t)}<div class="muted">${esc(a.s)}</div></td></tr>`).join('')}</table>` : ''}
      ${q.people.length ? `<h3>People</h3><table>${q.people.map(([n, r]) => `<tr><td>${esc(n)}</td><td>${esc(r)}</td></tr>`).join('')}</table>` : ''}
      <h3>History of the ${esc(THINGS[q.thing].name.toLowerCase())}</h3>${histHTML(q.thing)}</div>`).join('');
}
function openExportResult(L, title, preset, fmt) {
  let tab = fmt.pdf ? 'pdf' : fmt.sheet ? 'sheet' : fmt.zip ? 'zip' : 'backup';
  full('Your export', () => {
    const tabs = [['pdf', 'PDF binder'], ['sheet', 'Spreadsheet'], ['zip', 'Photos'], ['backup', 'Backup']].filter(([k]) => fmt[k]);
    const csv = csvOf(L);
    const body = tab === 'pdf' ? binderHTML(L, title, preset) : tab === 'sheet' ? `<div class="card" style="overflow:auto"><table class="cmp" style="min-width:520px"><tr>${csv.split('\n')[0].split(',').map(h => `<th>${esc(h)}</th>`).join('')}</tr>${L.flatMap(q => q.lines.map(l => `<tr><td>${esc(HOMES[q.home].name)}</td><td>${esc(q.name)}</td><td>${q.year}</td><td>${q.status}</td><td>${esc(l.l)}</td><td>${KIND[l.kind]}</td><td>${esc(l.who || '')}</td><td>${money(l.v)}</td></tr>`)).join('') || '<tr><td colspan="8">No costs recorded</td></tr>'}</table></div>` : tab === 'zip' ? `<div class="card"><div class="k v">${L.reduce((s, q) => s + q.photos.length, 0)} PHOTOS</div><div class="thumbs">${L.flatMap(q => q.photos.map(ph => `<button style="cursor:default" aria-label="${esc(ph.t)}"><img src="${photoSrc(ph)}" alt=""></button>`)).join('') || '<p class="mini">No photos yet.</p>'}</div></div>` : `<div class="card"><div class="k v">FULL BACKUP</div><p>Everything in this export as one file Atlas can import on another phone.</p><p class="mini">${(JSON.stringify(L).length / 1024).toFixed(0)} KB</p></div>`;
    $('#fBody').innerHTML = `<div class="seg">${tabs.map(([k, l]) => `<button class="${k === tab ? 'on' : ''}" data-t="${k}">${l}</button>`).join('')}</div>${body}<div class="row"><button class="btn" id="exCopy">${tab === 'sheet' ? 'Copy as CSV' : tab === 'backup' ? 'Copy backup' : tab === 'zip' ? 'Copy photo list' : 'Copy as text'}</button></div><p class="mini">In the Atlas app this saves real files — PDF, Sheets/CSV, ZIP — to your phone or Google Drive, or shares them straight to someone. This mockup can only copy.</p>`;
    $('#fBody').querySelectorAll('[data-t]').forEach(b => b.addEventListener('click', () => { tab = b.dataset.t; fullStack[fullStack.length - 1].draw(); }));
    $('#exCopy').addEventListener('click', () => { const t = tab === 'sheet' ? csv : tab === 'backup' ? JSON.stringify({ exported: '2026-11-24', homes: HOMES, projects: L.map(q => ({ ...q, photos: q.photos.map(p => ({ ...p, src: p.src ? '(image)' : undefined, _d: undefined })) })) }, null, 1) : tab === 'zip' ? L.flatMap(q => q.photos.map(ph => `${q.name} — ${ph.t} — ${ph.d}`)).join('\n') : [...$('#fBody').querySelectorAll('.binder')].map(b => b.innerText).join('\n\n'); copyText(t); toast('Copied'); });
  });
}

