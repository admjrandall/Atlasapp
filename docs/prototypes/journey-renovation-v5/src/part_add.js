/* ======================================================================
   Add anything: type, paste, photo or voice → Atlas files it
   ====================================================================== */
const SAMPLES = ['Jamie: Cabinet delivery moved to Dec 18', '$1,150 skylight change order, Jamie', 'Remind me to send the adjuster my hail photos', 'Plumber says the new tank can go in Thursday morning', 'Pavers come in charcoal or sand — Lena likes sand', '$240 chimney sweep paid'];
const VOICE = ['Remind me to ask Marco what the panel upgrade costs', 'The roofer said they can start the week of January 5th', 'We paid the $500 deposit for the deck lumber'];
const KEYS = [[/kitchen|cabinet|counter|backsplash|island|jamie|marco|priya|skylight|lena|drywall/i, 'kitchen'], [/roof|shingle|adjuster|hail|gutter|flashing|tear.?off/i, 'roof'], [/siding|sheathing|house wrap|hardie|exterior paint/i, 'siding'], [/patio|paver|yard|garden|fence|drain|lawn|811|sprinkler|plant/i, 'yard'], [/water heater|tank|plumber|tankless/i, 'heater'], [/deck|lumber|joist|footing|stairs/i, 'deck'], [/stove|chimney sweep|sweep|firewood/i, 'stove'], [/condo|unit|tenant|lease|hoa|repaint/i, 'paint']];
function classify(t) {
  const r = { kind: 'note', title: t.trim(), amount: 0, who: '', proj: null, area: null, date: '' };
  const cost = parseCost(t); const dm = t.match(/\b(jan(uary)?|feb(ruary)?|mar(ch)?|apr(il)?|may|june?|july?|aug(ust)?|sep(tember)?|oct(ober)?|nov(ember)?|dec(ember)?)\.?\s+\d{1,2}(st|nd|rd|th)?\b|\b(mon|tues|wednes|thurs|fri|satur|sun)day\b|\bnext week\b|\btomorrow\b|\b\d{1,2}\/\d{1,2}\b/i); if (dm) r.date = dm[0];
  const sm = t.match(/^([A-Z][\w’']+)\s*:/); if (sm) r.who = sm[1];
  if (cost && /\$|\bdollars?\b/i.test(t)) { r.kind = 'cost'; r.amount = Math.abs(cost.v); r.title = cost.l; r.costKind = cost.kind; r.who = r.who || cost.who; }
  else if (/^(remind me|remember to|need to|todo|to do|call|ask|book|schedule|order|buy|pick up|check|send|email|text)\b/i.test(t.trim()) || /\bremind me\b/i.test(t)) { r.kind = 'todo'; r.title = t.replace(/^(remind me to|remember to|need to|todo:?|to do:?)\s*/i, '').trim(); r.title = r.title ? r.title[0].toUpperCase() + r.title.slice(1) : 'To-do'; }
  else if (sm || r.date || /\b(said|says|confirmed|agreed|will|can start|moved to|ships?|deliver)/i.test(t)) { r.kind = 'agreed'; r.title = t.replace(/^[A-Z][\w’']+\s*:\s*/, '').trim(); }
  const hit = KEYS.find(([re, id]) => re.test(t) && P(id)); r.proj = hit ? hit[1] : (cur() ? cur().id : null);
  if (!r.proj) { const a = projOf(S.home).find(q => q.status === 'active') || PROJ[0]; r.proj = a && a.id; }
  const q = P(r.proj); if (q) { const ak = Object.entries(q.areas).find(([k, l]) => t.toLowerCase().includes(l.toLowerCase().split(' ')[0])); r.area = ak ? ak[0] : (S.p === r.proj ? S.area : null); }
  return r;
}
function openAdd(opts = {}) {
  let text = opts.text || '', photo = null, res = null, si = 0, vi = 0, overrideProj = opts.photo ? opts.proj : null, overrideArea = opts.photo ? (opts.area || null) : null, overrideKind = null;
  const draw = () => {
    $('#fBody').innerHTML = `<textarea class="bigin" id="aT" placeholder="Type or paste anything — a text from your contractor, a cost, a reminder, a note…">${esc(text)}</textarea>
      <div class="row" style="margin-top:8px"><button class="btn g" id="aPh">📷 Photo</button><button class="btn g" id="aV">🎙 Voice</button><button class="btn g" id="aS">✦ Try an example</button></div>
      <div id="aU"></div>`;
    $('#aT').addEventListener('input', () => { text = $('#aT').value; overrideKind = null; understand(); });
    $('#aPh').addEventListener('click', () => pickPhoto(src => { photo = src; overrideKind = 'photo'; understand(); }));
    $('#aV').addEventListener('click', () => { text = VOICE[vi++ % VOICE.length]; $('#aT').value = text; overrideKind = null; toast('Voice works in the app — here’s a sample transcript you can edit'); understand(); });
    $('#aS').addEventListener('click', () => { text = SAMPLES[si++ % SAMPLES.length]; $('#aT').value = text; overrideKind = null; overrideProj = null; overrideArea = null; understand(); });
    understand(); if (!opts.photo) setTimeout(() => $('#aT')?.focus(), 80);
  };
  const understand = () => {
    const U = $('#aU'); if (!text.trim() && !photo) { U.innerHTML = `<p class="mini" style="margin-top:12px">Atlas works out what it is — a to-do, a cost, something agreed, a note or a photo — and which home, project and part it belongs to. You check it before it’s saved.</p>`; return; }
    res = classify(text || 'Photo'); if (photo) res.kind = 'photo'; if (overrideKind) res.kind = overrideKind; if (overrideProj) res.proj = overrideProj; if (overrideArea !== null) res.area = overrideArea || null;
    const q = P(res.proj); const kinds = [['todo', 'To-do'], ['cost', 'Cost'], ['agreed', 'Agreed'], ['note', 'Note']].concat(photo ? [['photo', 'Photo']] : []);
    const opt = Object.entries(HOMES).map(([h, H]) => `<optgroup label="${esc(H.name)}">${projOf(h).map(x => `<option value="${x.id}" ${x.id === res.proj ? 'selected' : ''}>${esc(x.name)}</option>`).join('')}</optgroup>`).join('');
    U.innerHTML = `<div class="understood"><div class="k" style="font-size:11px;font-weight:800">✦ ATLAS UNDERSTOOD</div>${photo ? `<img class="photo" src="${photo}" alt="" style="max-height:200px;object-fit:cover">` : ''}
      <div class="lbl">It’s a</div><div class="chipset" id="uK">${kinds.map(([k, l]) => `<button data-k="${k}" class="${k === res.kind ? 'on' : ''}">${l}</button>`).join('')}</div>
      ${res.kind === 'cost' ? `<div class="lbl">Amount</div><input class="inp" id="uA" inputmode="decimal" value="${res.amount || ''}">` : ''}
      <div class="lbl">${res.kind === 'photo' ? 'Caption' : 'Saved as'}</div><input class="inp" id="uT" value="${esc(res.kind === 'photo' ? (text.trim() || 'Photo') : res.title)}">
      ${res.who || res.date ? `<p class="mini" style="margin:6px 0 0">${res.who ? `From <b>${esc(res.who)}</b>` : ''}${res.who && res.date ? ' · ' : ''}${res.date ? `Date <b>${esc(res.date)}</b>` : ''}</p>` : ''}
      <div class="lbl">Goes to</div><select class="inp" id="uP" aria-label="Project">${opt}</select>
      ${q ? `<div class="chipset" id="uAr"><button data-a="" class="${!res.area ? 'on' : ''}">Whole project</button>${Object.entries(q.areas).map(([k, l]) => `<button data-a="${k}" class="${k === res.area ? 'on' : ''}">${esc(l)}</button>`).join('')}</div>` : ''}
      <div class="row"><button class="btn" id="uGo" style="flex:1">Save to ${esc(q ? q.name : 'project')}</button></div></div>`;
    $('#uK').querySelectorAll('button').forEach(b => b.addEventListener('click', () => { overrideKind = b.dataset.k; understand(); }));
    $('#uP').addEventListener('change', e => { overrideProj = e.target.value; overrideArea = null; understand(); });
    const ar = $('#uAr'); if (ar) ar.querySelectorAll('button').forEach(b => b.addEventListener('click', () => { overrideArea = b.dataset.a || ''; understand(); }));
    $('#uGo').addEventListener('click', commit);
  };
  const commit = () => {
    const q = P(res.proj); if (!q) return; const title = $('#uT').value.trim() || res.title || 'Note'; const area = res.area || null; let undo, tab = 'needs', msg;
    if (res.kind === 'todo') { const n = { id: 'a' + Date.now(), area, kind: 'task', k: res.date ? res.date.toUpperCase() : 'TO-DO', title, p: '' }; q.needs.push(n); undo = () => q.needs = q.needs.filter(x => x !== n); msg = `To-do added to ${q.name}`; }
    else if (res.kind === 'cost') { const v = parseFloat(($('#uA').value || '').replace(/[$,]/g, '')) || 0; const l = { l: title, v: res.costKind === 'insurance' ? -v : v, kind: res.costKind || 'cost', who: res.who }; q.lines.push(l); undo = () => q.lines = q.lines.filter(x => x !== l); tab = 'money'; msg = `${money(v)} added to ${q.name}`; }
    else if (res.kind === 'agreed') { const written = !!res.who || /text|email/i.test(text); const a = { area, ok: written, id: 'a' + Date.now(), t: title + (res.date && !title.includes(res.date) ? ` (${res.date})` : ''), s: written ? `${res.who || 'Shared message'} · in writing · just now` : 'Said, not written down yet', draft: `Hi — confirming: ${title}. Could you reply to confirm? Thanks!`, to: res.who || 'them' }; q.agreed.unshift(a); undo = () => q.agreed = q.agreed.filter(x => x !== a); tab = 'agreed'; msg = `Saved to ${q.name} · Agreed`; }
    else if (res.kind === 'photo') { const k = area || Object.keys(q.areas)[0]; const pos = areaPos(k).slice(); pos[0] += (Math.random() - .5) * 1.4; pos[1] += (Math.random() - .5) * .6; const ph = { area: k, t: title, d: 'Added just now', pos, src: photo }; q.photos.push(ph); undo = () => q.photos = q.photos.filter(x => x !== ph); tab = 'record'; msg = `Photo pinned to ${q.areas[k] || q.name}`; }
    else { const n = { t: title, s: 'You · just now' }; q.notes.push(n); undo = () => q.notes = q.notes.filter(x => x !== n); tab = 'record'; msg = `Note added to ${q.name}`; }
    buzz([10, 20]); while (fullStack.length) popFull(true); go(q.id, tab); if (area) { S.area = area; flyTo(AREACAM[area] || CAM[viewOf(q)]); buildTags(); highlight(); render(); } changed(msg, undo);
  };
  full('Add anything', draw);
  if (opts.photo) setTimeout(() => $('#aPh').click(), 120);
}
$('#fab').addEventListener('click', () => openAdd({ proj: S.p, area: S.area }));

