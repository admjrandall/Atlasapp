/* share */
$('#shareB').addEventListener('click', () => {
  const p = cur(); let role = 'View', scope = p ? 'project' : 'home', hideMoney = true;
  full('Share', () => {
    const scopes = [p && ['project', p.name], ['home', HOMES[S.home].name], ['all', 'All homes']].filter(Boolean);
    $('#fBody').innerHTML = `<div class="card"><div class="k v">WHO HAS ACCESS</div>${S.people.map((x, i) => `<div class="pm"><span class="av">${esc(x.n[0])}</span><div class="grow"><b>${esc(x.n)}</b><div class="mini" style="margin:0">${esc(x.r)} · ${esc(x.s)}</div></div>${i ? `<button class="rm" data-sr="${i}">Remove</button>` : ''}</div>`).join('')}</div>
      <div class="card"><div class="k v">INVITE SOMEONE</div><input class="inp" id="shN" placeholder="Name, email or phone" style="margin-top:6px"><div class="lbl">They can</div><div class="chipset" id="shR">${['View', 'Comment', 'Edit'].map(x => `<button class="${x === role ? 'on' : ''}">${x}</button>`).join('')}</div><div class="lbl">What they see</div><div class="chipset" id="shS">${scopes.map(([k, l]) => `<button data-s="${k}" class="${k === scope ? 'on' : ''}">${esc(l)}</button>`).join('')}</div><div class="tog">Hide money and payments<input type="checkbox" id="shM" ${hideMoney ? 'checked' : ''}></div><div class="row"><button class="btn" id="shGo">Copy invite</button></div><p class="mini">Typical: a contractor gets <b>View · this project · money hidden</b>; your partner gets <b>Edit · all homes</b>.</p></div>`;
    $('#shR').querySelectorAll('button').forEach(b => b.addEventListener('click', () => { role = b.textContent; $('#shR').querySelectorAll('button').forEach(x => x.classList.toggle('on', x === b)); }));
    $('#shS').querySelectorAll('button').forEach(b => b.addEventListener('click', () => { scope = b.dataset.s; $('#shS').querySelectorAll('button').forEach(x => x.classList.toggle('on', x === b)); }));
    $('#shM').addEventListener('change', e => hideMoney = e.target.checked);
    $('#fBody').querySelectorAll('[data-sr]').forEach(b => b.addEventListener('click', () => { const x = S.people.splice(+b.dataset.sr, 1)[0]; save(); fullStack[fullStack.length - 1].draw(); toast(`${x.n} no longer has access`); }));
    $('#shGo').addEventListener('click', () => { const n = $('#shN').value.trim(); if (!n) { $('#shN')?.focus(); return; } const label = scopes.find(s => s[0] === scope)[1]; S.people.push({ n, r: 'Can ' + role.toLowerCase(), s: label + (hideMoney ? ' · money hidden' : '') }); copyText(`I’m sharing “${label}” with you on Atlas — you can ${role.toLowerCase()} it${hideMoney ? ' (costs hidden)' : ''}. Open the invite link to see it.`); save(); fullStack[fullStack.length - 1].draw(); toast(`Invite for ${n} copied — paste it into a text or email`); });
  });
});

