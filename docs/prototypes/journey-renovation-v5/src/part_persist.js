/* ======================================================================
   persistence (this browser only)
   ====================================================================== */
const KEY = 'atlas-reno-v5';
let saveT;
function save() { clearTimeout(saveT); saveT = setTimeout(() => { try { localStorage.setItem(KEY, JSON.stringify({ PROJ, THINGS, HOMES, people: S.people, home: S.home })); } catch (e) { toast('This demo couldn’t keep that photo — your browser’s storage is full'); } }, 250); }
function load() { try { const d = JSON.parse(localStorage.getItem(KEY) || 'null'); if (!d || !d.PROJ) return; PROJ.length = 0; d.PROJ.forEach(p => PROJ.push(p)); Object.keys(THINGS).forEach(k => delete THINGS[k]); Object.assign(THINGS, d.THINGS); Object.assign(HOMES, d.HOMES); S.people = d.people || S.people; if (HOMES[d.home]) S.home = d.home; } catch (e) {} }
function resetDemo() { try { localStorage.removeItem(KEY); } catch (e) {} location.reload(); }

