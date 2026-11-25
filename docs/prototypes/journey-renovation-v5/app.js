import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { MODELS } from './models.js';

{
const $ = s => document.querySelector(s);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = n => (n < 0 ? '−$' : '$') + Math.abs(Math.round(n)).toLocaleString('en-US');
const clamp = (x, a = 0, b = 1) => Math.min(b, Math.max(a, x));
const ease = t => t < .5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
const buzz = p => { try { navigator.vibrate && navigator.vibrate(p); } catch (e) {} };

/* ======================================================================
   HOMES · THINGS · PROJECTS  (example data; anything marked sample is made up)
   ====================================================================== */
const HOMES = {
  main: { name: 'Main house', icon: '🏡', kind: 'House · 3 bed · built 1962 (sample)', model: 'main' },
  cabin: { name: 'Lake cabin', icon: '🌲', kind: 'A-frame · weekends (sample)', model: 'cabin' },
  condo: { name: 'Rental condo', icon: '🏢', kind: 'Unit 4B · rented out (sample)', model: 'condo' }
};
const THINGS = {
  kitchen: { home: 'main', name: 'Kitchen', place: 'Inside', hist: [{ y: 1962, t: 'Original kitchen', s: 'Built with the house' }] },
  roof: { home: 'main', name: 'Roof', place: 'Outside', hist: [{ y: 1962, t: 'Original cedar-brown shingles', s: 'Built with the house' }, { y: 2009, t: 'Asphalt 3-tab, grey', s: 'Previous owner · from your inspection report' }] },
  siding: { home: 'main', name: 'Siding', place: 'Outside', hist: [{ y: 1988, t: 'Wood lap siding, painted', s: 'Inspection report' }] },
  yard: { home: 'main', name: 'Back yard', place: 'Yard', hist: [{ y: 2019, t: 'Cedar fence, back yard', s: 'Your receipt · $3,900 (sample)' }] },
  wh: { home: 'main', name: 'Water heater', place: 'Systems', hist: [{ y: 2011, t: '40-gal gas tank', s: 'Read from the label' }] },
  bath: { home: 'main', name: 'Bathroom', place: 'Inside', hist: [{ y: 2022, t: 'Vanity, toilet, tile floor', s: 'Bathroom refresh · $9,400 (sample)' }] },
  deck: { home: 'cabin', name: 'Deck', place: 'Outside', hist: [{ y: 1998, t: 'Pressure-treated pine', s: 'Came with the cabin' }] }, cabin: { home: 'cabin', name: 'Cabin', place: 'Inside', hist: [{ y: 1974, t: 'A-frame built', s: 'County records (sample)' }] },
  stove: { home: 'cabin', name: 'Wood stove', place: 'Systems', hist: [{ y: 2024, t: 'Chimney swept', s: 'Receipt · $240 (sample)' }] },
  unit: { home: 'condo', name: 'Unit interior', place: 'Inside', hist: [{ y: 2025, t: 'Move-out inspection', s: '84 photos, room by room' }] }
};
const COLOURS = {
  siding: [{ n: 'Mountain Sage', m: 'James Hardie ColorPlus', hex: '#8f9a7f' }, { n: 'Arctic White', m: 'James Hardie ColorPlus', hex: '#eeece6' }, { n: 'Iron Gray', m: 'James Hardie ColorPlus', hex: '#5b5e60' }, { n: 'Evening Blue', m: 'James Hardie ColorPlus', hex: '#3d4a5c' }, { n: 'Aged Pewter', m: 'James Hardie ColorPlus', hex: '#8c877c' }],
  paint: [{ n: 'Agreeable Gray', code: 'SW 7029', m: 'Sherwin-Williams', hex: '#d1cbc1' }, { n: 'Alabaster', code: 'SW 7008', m: 'Sherwin-Williams', hex: '#edeae0' }, { n: 'Evergreen Fog', code: 'SW 9130', m: 'Sherwin-Williams', hex: '#95978a' }, { n: 'White Dove', code: 'OC-17', m: 'Benjamin Moore', hex: '#f0eee6' }, { n: 'Hale Navy', code: 'HC-154', m: 'Benjamin Moore', hex: '#434b56' }],
  shingle: [{ n: 'Charcoal', m: 'Architectural asphalt', hex: '#45494f' }, { n: 'Weathered Wood', m: 'Architectural asphalt', hex: '#76685a' }, { n: 'Pewter Gray', m: 'Architectural asphalt', hex: '#7b7d7d' }, { n: 'Hickory', m: 'Architectural asphalt', hex: '#6b5443' }]
};
const F = (k, v, type = 'text', extra = {}) => ({ k, v, type, ...extra });
const PROJ = [
  { id: 'kitchen', home: 'main', thing: 'kitchen', name: 'Kitchen remodel', icon: '🍳', status: 'active', year: 2026, place: 'Inside', view: 'rooms', record: 'Walls',
    stops: ['Plan', 'Quotes', 'Demo', 'Build', 'Finish'], here: 1, where: 'Quotes · demolition starts Dec 15',
    areas: { kitchen: 'Kitchen', living: 'Living room', entry: 'Hallway' },
    details: [F('Cabinets', 'Shaker, painted', 'choice', { opts: ['Shaker, painted', 'Flat slab', 'Keep and reface'] }), F('Cabinet colour', 'Evergreen Fog · SW 9130', 'colour', { pal: 'paint' }), F('Counters', 'White quartz', 'choice', { opts: ['White quartz', 'Butcher block', 'Granite'] }), F('Backsplash', 'Deciding', 'text'), F('Flooring', 'Keep the original oak', 'text')],
    needs: [
      { id: 'quotes', area: 'kitchen', kind: 'compare', k: 'DUE TODAY', title: 'Compare contractor quotes', p: 'Like for like — Carter leaves out electrical, so Marco’s number is added.', table: [['', 'Carter + Marco', 'Ridge'], ['Estimate', '$48,200', '$55,400'], ['Electrical', '$6,800', 'incl.'], ['Haul-away', 'not in writing', 'incl.'], ['Total', '$55,000', '$55,400']], picks: ['Carter + Marco', 'Ridge'], note: 'Ridge and Marco numbers are samples.' },
      { id: 'tile', area: 'kitchen', kind: 'choose', k: 'PICK BY DEC 5', title: 'Backsplash tile', field: 'Backsplash', p: 'Tile takes about 3 weeks to arrive, so this sets the install date (sample).', opts: [{ n: 'Matte white zellige', s: '$14 / sq ft · sample', c: 'linear-gradient(135deg,#f3eee6,#e2d9cb)', vote: 'You like this' }, { n: 'Sage subway', s: '$9 / sq ft · sample', c: 'linear-gradient(135deg,#b9c5a9,#94a585)', vote: 'Your partner likes this' }] },
      { id: 'sky', area: 'entry', kind: 'choose', k: 'BEFORE FRAMING', title: 'Skylight size', p: 'The hallway is the darkest part of the house. You measured it 3 days ago.', cost: [1150, 780], opts: [{ n: '2 × 4 ft', s: '+$1,150 change order · sample', c: 'linear-gradient(#cfe6ff,#9cc3e8)' }, { n: '2 × 2 ft', s: '+$780 change order · sample', c: 'linear-gradient(#e2efff,#b9d4ee)' }] }
    ],
    waiting: [{ who: 'Jamie Carter', what: 'Updated estimate', when: 'Friday' }],
    budget: 60000, lines: [{ l: 'Carter Build estimate', v: 48200, kind: 'quote', who: 'Jamie' }, { l: 'Electrical (Marco)', v: 6800, kind: 'quote', who: 'Marco', sample: true }],
    watch: ['Electrical isn’t in Jamie’s estimate', 'Marco’s number is counted (sample). If he quotes higher, the cushion shrinks.'],
    agreed: [{ area: 'living', ok: true, t: 'Keep the original oak floors', s: 'Your decision · Plan' }, { area: 'kitchen', ok: true, t: 'Wall between kitchen and living room is not load-bearing', s: 'Jamie · site visit notes' }, { area: 'kitchen', ok: true, t: 'Island moves 40 cm toward the window', s: 'Priya · Floor plan v3 (PDF)' }, { area: 'kitchen', ok: true, t: 'Cabinets ship in three weeks', s: 'Jamie · your voice note today' }, { area: null, ok: false, id: 'haul', t: 'Demolition haul-away is included', s: 'Only said out loud', draft: 'Hi Jamie — confirming what we talked about: haul-away of all demolition debris is included in your $48,200 estimate (EST-2026-0118). Could you reply to confirm? Thanks!', to: 'Jamie' }],
    people: [['Jamie Carter', 'General contractor · Carter Build Co.'], ['Marco Ruiz', 'Electrician'], ['Priya Shah', 'Architect'], ['Lena Brooks', 'Interior designer']],
    photos: [{ area: 'kitchen', t: 'Kitchen back wall · before', d: 'Nov 20 · site visit (sample photo)', pos: [2.2, 1.3, -2.85], draw: 'studs' }, { area: 'entry', t: 'Hallway ceiling · measured for skylight', d: '3 days ago (sample photo)', pos: [.7, 2.5, 1.6], draw: 'sky' }],
    coming: ['ABOUT JAN 12 (SAMPLE)', 'Before drywall: walk the walls', 'Atlas reminds you the day before drywall goes up, with a checklist: each wall wide, then close on outlets, pipes and wiring.'] },
  { id: 'roof', home: 'main', thing: 'roof', name: 'New roof', icon: '🏠', status: 'active', year: 2026, place: 'Outside', view: 'roof', record: 'Roof deck',
    stops: ['Inspect', 'Claim & quotes', 'Tear-off', 'Install', 'Final check'], here: 1, where: 'Claim & quotes · adjuster visit pending',
    areas: { front: 'Front slope', back: 'Back slope', garageRoof: 'Garage roof', chimney: 'Chimney flashing' },
    details: [F('Material', 'Architectural asphalt', 'choice', { opts: ['Architectural asphalt', 'Standing-seam metal', 'Composite shake', 'Tile'] }), F('Colour', 'Charcoal', 'colour', { pal: 'shingle' }), F('Underlayment', 'Synthetic', 'choice', { opts: ['Synthetic', 'Felt'] }), F('Warranty', 'Ask each roofer', 'text'), F('Claim number', '', 'text')],
    needs: [
      { id: 'adj', area: 'front', kind: 'task', k: 'DUE TODAY', title: 'Call insurance about the roof claim', p: 'From your tasks. Ask when the adjuster can come, and whether they want your hail photos first.' },
      { id: 'flash', area: 'chimney', kind: 'choose', k: 'WHILE THE ROOF IS OPEN', title: 'Redo the chimney flashing too?', p: 'A common leak spot, and cheapest to fix while the shingles are off (sample prices).', cost: [650, 0], opts: [{ n: 'Yes, redo it', s: '+$650 · sample', c: 'linear-gradient(#b5654a,#8f4b36)' }, { n: 'Leave as is', s: '$0', c: 'linear-gradient(#d8d2c8,#bdb6aa)' }] }
    ],
    waiting: [{ who: 'Insurance', what: 'Adjuster appointment', when: 'after your call' }],
    budget: 18000, lines: [{ l: 'Roofer’s quote', v: 16800, kind: 'quote', who: 'Summit Roofing', sample: true }, { l: 'Insurance estimate (pending)', v: -13500, kind: 'insurance', who: 'Insurer', sample: true }],
    watch: ['Decking is the usual surprise', 'Rotted sheets found during tear-off are often billed per sheet. Get that price in writing first.'],
    agreed: [{ area: 'front', ok: true, t: 'Hail damage on the front slope', s: 'Your photos · 12 pictures' }, { area: null, ok: false, id: 'tear', t: 'Full tear-off down to the deck — no layover', s: 'Said by the roofer', draft: 'Hi — confirming from our call: the job is a full tear-off down to the roof deck, not a layover. Could you add that line to the quote? Thanks!', to: 'the roofer' }, { area: null, ok: false, id: 'deck', t: 'Price per sheet for any rotted decking', s: 'Not discussed yet', draft: 'Hi — before we sign, could you put the price per sheet for replacing any rotted decking into the quote? Thanks!', to: 'the roofer' }],
    people: [['Summit Roofing', 'Roofer (sample)'], ['Your insurer', 'Claim · adjuster pending']],
    photos: [{ area: 'front', t: 'Front slope · hail hits', d: 'Your photo · Nov 2 (sample)', pos: [-1.5, 4.0, 1.6], draw: 'hail' }],
    coming: ['DURING TEAR-OFF', 'Photograph the bare deck', 'Atlas reminds you that morning, then reminds you to register the shingle warranty after install.'] },
  { id: 'heater', home: 'main', thing: 'wh', name: 'Water heater', icon: '🔥', status: 'active', year: 2026, place: 'Systems', view: 'item', record: 'Label',
    stops: ['Choose', 'Buy', 'Install', 'Done'], here: 0, where: 'Choose · the old tank is from 2011', areas: { wh: 'Water heater' },
    details: [F('Type', 'Deciding', 'choice', { opts: ['Gas tank', 'Electric tank', 'Heat-pump', 'Tankless gas'] }), F('Size', '50 gal', 'text'), F('Brand & model', '', 'text'), F('Installer', '', 'text')],
    needs: [{ id: 'type', area: 'wh', kind: 'choose', k: 'CHOOSE', title: 'Tank or tankless', field: 'Type', p: 'Tankless may need a bigger gas line or vent — ask before you compare price (sample prices).', opts: [{ n: 'Gas tank', s: '≈ $1,900 installed · sample', c: 'linear-gradient(#f2f2f2,#d8d8d8)' }, { n: 'Tankless gas', s: '≈ $3,800 installed · sample', c: 'linear-gradient(#e8e4dc,#c8c2b6)' }] }, { id: 'permit', area: 'wh', kind: 'task', k: 'BEFORE INSTALL', title: 'Check whether your city needs a permit', p: 'Many cities do for water heaters. Ask Atlas and it looks up your city’s page.' }],
    waiting: [], budget: 2500, lines: [], watch: ['Haul-away and fittings', 'Ask that removing the old tank and new fittings are in the price.'],
    agreed: [{ area: 'wh', ok: false, id: 'haulwh', t: 'Old tank hauled away', s: 'Not in writing', draft: 'Hi — could you confirm the price includes removing and hauling away the old water heater? Thanks!', to: 'the plumber' }],
    people: [], photos: [{ area: 'wh', t: 'Old tank label', d: 'Read by Atlas: 2011, 40 gal (sample)', pos: [6.6, 1.3, -2.3], draw: 'plate' }],
    coming: ['AFTER INSTALL', 'Snap the new label', 'Atlas reads the model, serial and warranty, and starts a yearly “flush the tank” reminder as its own Orbit journey.'] },
  { id: 'siding', home: 'main', thing: 'siding', name: 'New siding', icon: '🧱', status: 'planned', year: 2027, place: 'Outside', view: 'siding', record: 'Under siding',
    stops: ['Pick', 'Quotes', 'Remove', 'Wrap & install', 'Trim & paint'], here: 0, where: 'Pick · material and colour', areas: { sFront: 'Front', sLeft: 'Left side', sBack: 'Back', sGarage: 'Garage' },
    details: [F('Material', 'Fiber cement', 'choice', { opts: ['Fiber cement', 'Vinyl', 'Engineered wood', 'Cedar', 'Metal'] }), F('Profile', 'Lap, 7″ exposure', 'choice', { opts: ['Lap, 7″ exposure', 'Board & batten', 'Shingle', 'Panel'] }), F('Colour', 'Mountain Sage', 'colour', { pal: 'siding' }), F('Trim colour', 'Arctic White', 'colour', { pal: 'siding' }), F('House built', '1962 (sample)', 'text')],
    needs: [{ id: 'lead', area: null, kind: 'task', k: 'BEFORE ANY QUOTE', title: 'Lead-safe crew', p: 'Built before 1978, so the old paint may contain lead — hire a crew that is lead-safe certified.' }],
    waiting: [], budget: 25000, lines: [], watch: ['No quotes yet', 'When quotes arrive, Atlas lines them up like for like: wrap, trim, flashing and haul-away.'],
    agreed: [{ area: null, ok: false, id: 'wrap', t: 'House wrap and window flashing included', s: 'Ask every bidder', draft: 'Hi — could your quote list house wrap and flashing at every window and door as included items? Thanks!', to: 'each bidder' }],
    people: [], photos: [], coming: ['WHEN THE OLD SIDING COMES OFF', 'Photograph the sheathing on every side', 'It’s the only time you’ll see rot or gaps. Atlas pins each photo to its side of the house.'] },
  { id: 'yard', home: 'main', thing: 'yard', name: 'Garden redesign', icon: '🌿', status: 'planned', year: 2027, place: 'Yard', view: 'yard', record: 'Underground',
    stops: ['Design', 'Quotes', 'Call 811 & dig', 'Build', 'Plant'], here: 0, where: 'Design · native planting, a small patio, drainage', areas: { patio: 'Patio', beds: 'Garden beds', drain: 'Fence drainage', lawn: 'Back lawn' },
    details: [F('Patio', 'Pavers', 'choice', { opts: ['Pavers', 'Concrete', 'Flagstone', 'Gravel'] }), F('Patio size', '16 × 10 ft', 'text'), F('Drainage', 'French drain', 'choice', { opts: ['French drain', 'Regrade', 'Dry creek bed'] }), F('Plants', 'Native, low water', 'text')],
    needs: [{ id: '811', area: 'lawn', kind: 'task', k: 'BEFORE ANY DIGGING', title: 'Call 811 to mark buried lines', p: 'It’s free. Give them a few days before the dig date — Atlas sets the reminder.' }],
    waiting: [], budget: 8000, lines: [], watch: ['Plants are the easy part to trim', 'If patio and drainage run over, planting can move to spring.'],
    agreed: [{ area: null, ok: true, t: 'Native planting, a small patio and better drainage along the fence', s: 'Your project notes' }], people: [],
    photos: [{ area: 'lawn', t: 'Sprinkler line · back lawn', d: 'Dug up last spring (sample)', pos: [3.2, .1, -7.5], draw: 'sprinkler' }], coming: ['AFTER YOU CALL 811', 'Photograph the locate marks', 'The paint and flags fade in days. Atlas pins your photos to the yard for good.'] },
  { id: 'bath22', home: 'main', thing: 'bath', name: 'Bathroom refresh', icon: '🛁', status: 'done', year: 2022, place: 'Inside', view: 'rooms', record: 'Walls', stops: ['Plan', 'Buy', 'Install', 'Done'], here: 4, where: 'vanity, tile floor, fan',
    areas: { bath: 'Bathroom' }, details: [F('Vanity', '36″ white oak, quartz top', 'text'), F('Floor tile', 'Hex marble, 2″', 'text'), F('Paint', 'Alabaster · SW 7008', 'colour', { pal: 'paint' }), F('Fan', 'Panasonic WhisperCeiling (sample)', 'text')],
    needs: [], waiting: [], budget: 10000, lines: [{ l: 'Total paid', v: 9400, kind: 'paid', who: 'Various', sample: true }], watch: ['Finished', 'Under budget by $600 (sample).'], agreed: [{ area: 'bath', ok: true, t: 'Vanity warranty 5 years', s: 'Receipt' }], people: [], care: [{ t: 'Re-seal the grout', every: 'Every 2 years', next: 'May 2026' }], photos: [{ area: 'bath', t: 'Bathroom · before', d: 'Mar 2022 (sample)', pos: [2.2, 1.3, 1.8], draw: 'bathBefore' }, { area: 'bath', t: 'Bathroom · after', d: 'May 2022 (sample)', pos: [3.0, 1.3, 1.8], draw: 'bathAfter' }], coming: ['CARE', 'Re-seal the grout', 'Every 2 years — next in May 2026 (sample).'] },
  { id: 'fence19', home: 'main', thing: 'yard', name: 'Back fence', icon: '🪵', status: 'done', year: 2019, place: 'Yard', view: 'yard', record: 'Underground', stops: ['Quote', 'Build', 'Done'], here: 3, where: 'cedar, 96 ft',
    areas: { lawn: 'Back yard' }, details: [F('Material', 'Cedar, 6 ft', 'text'), F('Length', '96 ft', 'text'), F('Installer', 'Front Range Fence (sample)', 'text')], needs: [], waiting: [], budget: 4000, lines: [{ l: 'Total paid', v: 3900, kind: 'paid', who: 'Front Range Fence', sample: true }], watch: ['Finished', 'Stain every 3–4 years.'], agreed: [], people: [], care: [{ t: 'Stain the fence', every: 'Every 3–4 years', next: 'Summer 2026' }], photos: [], coming: ['CARE', 'Stain the fence', 'Next due 2026 (sample).'] },
  { id: 'deck', home: 'cabin', thing: 'deck', name: 'Deck rebuild', icon: '🪚', status: 'active', year: 2026, place: 'Outside', view: 'deck', record: 'Footings',
    stops: ['Measure', 'Materials', 'Build', 'Seal'], here: 1, where: 'Materials · before the spring thaw', areas: { deck: 'Deck', stairs: 'Stairs' },
    details: [F('Decking', 'Deciding', 'choice', { opts: ['Cedar', 'Composite', 'Pressure-treated'] }), F('Size', '12 × 20 ft', 'text'), F('Railing', 'Black metal balusters', 'text')],
    needs: [{ id: 'decking', area: 'deck', kind: 'choose', k: 'ORDER BY MARCH', title: 'Decking', field: 'Decking', p: 'Composite costs more up front and skips yearly sealing (sample prices).', opts: [{ n: 'Cedar', s: '≈ $5,200 · sample', c: 'repeating-linear-gradient(90deg,#c48a5a 0 18px,#a87148 18px 20px)' }, { n: 'Composite', s: '≈ $7,900 · sample', c: 'repeating-linear-gradient(90deg,#8a7c6c 0 18px,#73675a 18px 20px)' }] }],
    waiting: [], budget: 9000, lines: [{ l: 'Footing inspection', v: 150, kind: 'paid', who: 'County', sample: true }], watch: ['Lumber prices move', 'Lock the price when you order.'], agreed: [], people: [], photos: [], coming: ['BEFORE DECKING GOES DOWN', 'Photograph the footings and joists', 'Proof for the inspector and the next owner.'] },
  { id: 'stove', home: 'cabin', thing: 'stove', name: 'Wood stove check', icon: '🔥', status: 'planned', year: 2026, place: 'Systems', view: 'deck', record: 'Records', stops: ['Book', 'Sweep', 'Done'], here: 0, where: 'Before first fire of the season', areas: { stove: 'Stove pipe' },
    details: [F('Stove', 'Jøtul F 3 (sample)', 'text'), F('Last swept', 'Oct 2024', 'text')], needs: [{ id: 'sweep', area: 'stove', kind: 'task', k: 'BEFORE WINTER', title: 'Book the chimney sweep', p: 'Last time was Oct 2024 — $240 (sample).' }], waiting: [], budget: 300, lines: [], watch: ['Yearly', 'This becomes an Orbit once it repeats.'], agreed: [], people: [], photos: [], coming: ['AFTER THE SWEEP', 'Snap the receipt', 'Atlas files it and sets next year’s reminder.'] },
  { id: 'paint', home: 'condo', thing: 'unit', name: 'Repaint the unit', icon: '🎨', status: 'planned', year: 2027, place: 'Inside', view: 'condo', record: 'Condition', stops: ['Pick', 'Notice', 'Paint', 'Photos'], here: 0, where: 'Between tenants · lease ends Mar 31 (sample)', areas: { unit: 'Unit 4B' },
    details: [F('Walls', 'Agreeable Gray · SW 7029', 'colour', { pal: 'paint' }), F('Trim', 'Alabaster · SW 7008', 'colour', { pal: 'paint' }), F('Finish', 'Eggshell', 'choice', { opts: ['Flat', 'Eggshell', 'Satin'] })],
    needs: [{ id: 'hoa', area: 'unit', kind: 'task', k: 'CHECK FIRST', title: 'HOA rules for contractors', p: 'Your HOA may require a certificate of insurance from the painter before work.' }], waiting: [], budget: 3500, lines: [], watch: ['Between tenants', 'Paint in the gap between leases to avoid lost rent.'], agreed: [], people: [], photos: [], coming: ['AFTER PAINT', 'Condition photos, room by room', 'Your record for the next deposit return.'] },
  { id: 'moveout25', home: 'condo', thing: 'unit', name: 'Move-out inspection', icon: '📋', status: 'done', year: 2025, place: 'Inside', view: 'condo', record: 'Condition', stops: ['Walkthrough', 'Photos', 'Deposit'], here: 3, where: 'Deposit returned Apr 2025 (sample)', areas: { unit: 'Unit 4B' },
    details: [F('Photos', '84, room by room', 'text'), F('Deductions', 'Carpet clean $180 (sample)', 'text')], needs: [], waiting: [], budget: 0, lines: [], watch: ['Done', 'Photos used for the deposit decision.'], agreed: [], people: [], photos: [], coming: ['NEXT TENANT', 'Move-in photos', 'Same rooms, same angles.'] }
];
const CATALOG = {
  Outside: ['Roof replacement', 'Roof repair', 'Gutters & downspouts', 'Siding', 'Exterior paint', 'Trim & fascia', 'Windows', 'Front door', 'Garage door', 'Porch', 'Deck', 'Balcony', 'Chimney repair', 'Foundation repair', 'Waterproofing', 'Attic insulation', 'Solar panels', 'Skylight'],
  Rooms: ['Kitchen remodel', 'Bathroom remodel', 'Primary suite', 'Basement finish', 'Attic conversion', 'Laundry room', 'Closets', 'Home office', 'Garage conversion', 'Room addition', 'ADU / in-law unit', 'Mudroom', 'Nursery', 'Home gym'],
  Surfaces: ['Flooring', 'Interior paint', 'Wallpaper', 'Tile', 'Drywall repair', 'Ceilings', 'Trim & moulding', 'Stairs & railings', 'Fireplace surround'],
  Systems: ['Furnace', 'AC / heat pump', 'Ductwork', 'Water heater', 'Repiping', 'Electrical panel', 'Rewiring', 'EV charger', 'Generator', 'Well', 'Septic', 'Sump pump', 'Water softener', 'Radon mitigation', 'Smart home', 'Security system'],
  Fixtures: ['Cabinets', 'Countertops', 'Sink & faucet', 'Toilet', 'Shower', 'Bathtub', 'Lighting', 'Ceiling fans', 'Range / oven', 'Refrigerator', 'Dishwasher', 'Washer & dryer'],
  Yard: ['Landscaping', 'Patio', 'Pavers', 'Fence', 'Driveway', 'Retaining wall', 'Drainage', 'Irrigation', 'Lawn', 'Tree work', 'Garden beds', 'Shed', 'Pergola', 'Pool', 'Hot tub', 'Outdoor kitchen', 'Outdoor lighting'],
  Repairs: ['Water damage', 'Mold remediation', 'Storm / hail damage', 'Fire / smoke damage', 'Pest damage', 'Foundation cracks', 'Insurance claim'],
  'Aging in place': ['Grab bars', 'Ramp', 'Curbless shower', 'Wider doors', 'Stair lift', 'Lever handles'],
  Renters: ['Move-in photos', 'Move-out photos', 'Landlord approval', 'Removable upgrades', 'Repair request'],
  Condo: ['HOA approval', 'Unit flooring (sound rules)', 'Balcony (HOA)', 'Shared-wall work']
};
const PLACE_OF = { Outside: 'Outside', Rooms: 'Inside', Surfaces: 'Inside', Systems: 'Systems', Fixtures: 'Inside', Yard: 'Yard', Repairs: 'Inside', 'Aging in place': 'Inside', Renters: 'Inside', Condo: 'Inside' };
const STEPS_OF = { Outside: ['Inspect', 'Quotes', 'Prep', 'Install', 'Final check'], Rooms: ['Plan', 'Quotes', 'Demo', 'Build', 'Finish'], Surfaces: ['Pick', 'Prep', 'Do it', 'Done'], Systems: ['Choose', 'Quote', 'Install', 'Inspect'], Fixtures: ['Choose', 'Buy', 'Install', 'Done'], Yard: ['Design', 'Quotes', 'Call 811 & dig', 'Build', 'Plant'], Repairs: ['Document', 'Claim', 'Repair', 'Close out'], 'Aging in place': ['Assess', 'Choose', 'Install'], Renters: ['Ask', 'Do it', 'Photos'], Condo: ['HOA', 'Quotes', 'Do it', 'Done'] };

const STEP_INFO = {
  Plan: 'Decide what you want: layout, must-haves, rough budget. Floor plans and inspiration live here.', Quotes: 'Get at least two quotes and compare them like for like — same scope, same exclusions.', Demo: 'Old work comes out. Photograph anything that gets exposed.', Build: 'The new work goes in. Inspections usually happen here — before anything gets covered.', Finish: 'Paint, fixtures, trim and the punch list. Hold the last payment until the list is done.',
  Inspect: 'Find out what’s really wrong. Photos and an inspection report make every later step easier.', 'Claim & quotes': 'Open the insurance claim and line up roofer quotes against the adjuster’s estimate.', 'Tear-off': 'The old roof comes off. This is the one chance to see the deck.', Install: 'New material goes on. Keep the product wrappers and lot numbers for the warranty.', 'Final check': 'Walk it with the contractor. Register the warranty and file the final invoice.',
  Pick: 'Choose material, style and colour. Samples beat screens.', Remove: 'The old layer comes off. Photograph what’s under it.', 'Wrap & install': 'Weather barrier first, then the new layer.', 'Trim & paint': 'Details, caulk and paint — then the final walk-through.',
  Design: 'Sketch what goes where, and how water drains.', 'Call 811 & dig': 'Buried lines get marked — free — before anyone digs.', Plant: 'Plants, mulch and watering. Note what went where.',
  Choose: 'Pick the type and size that fits how you live.', Buy: 'Order it, or have the installer supply it. Keep the receipt.', Done: 'Finished. Photos, receipts and the warranty stay with this thing.', Measure: 'Measure twice and note it here.', Materials: 'Order materials — lock the price when you order.', Seal: 'Seal or stain to protect the new work.', Book: 'Book the appointment.', Sweep: 'The sweep happens; keep the receipt.', Notice: 'Give the tenant written notice before entry.', Paint: 'The painting itself.', Photos: 'Condition photos, room by room.', Walkthrough: 'Walk the unit with the tenant.', Deposit: 'Decide and document deductions.', Quote: 'Get a written quote.', Prep: 'Get the site ready.', 'Do it': 'The work itself.', Document: 'Photograph the damage before anything is moved.', Claim: 'File the claim with photos and receipts.', Repair: 'The repair itself.', 'Close out': 'Final invoice, final payment, close the claim.', Assess: 'What needs to change, room by room.', Ask: 'Ask for permission in writing.', HOA: 'Get HOA approval in writing first.'
};
const S = { home: 'main', p: null, area: null, tab: 'needs', xray: false, picked: {}, people: [{ n: 'You', r: 'Owner', s: 'Everything' }, { n: 'Your partner', r: 'Can edit', s: 'All homes' }] };
const P = id => PROJ.find(x => x.id === id);
const cur = () => S.p ? P(S.p) : null;
const projOf = h => PROJ.filter(x => x.home === h);
PROJ.forEach(p => { p.notes = p.notes || []; p.care = p.care || []; });
const modelOf = h => (HOMES[h] && HOMES[h].model) || 'main';

/* ======================================================================
   3D — the whole property: house, garage, yard, street
   ====================================================================== */
const canvas = $('#c');
const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
renderer.setPixelRatio(Math.min(devicePixelRatio, 2)); renderer.shadowMap.enabled = true; renderer.shadowMap.type = THREE.PCFSoftShadowMap;
renderer.toneMapping = THREE.ACESFilmicToneMapping; renderer.toneMappingExposure = 1.05;
const scene = new THREE.Scene(); scene.fog = new THREE.Fog(0xf3efe8, 38, 70);
const mainRoot = new THREE.Group(); scene.add(mainRoot); const cabinRoot = new THREE.Group(); scene.add(cabinRoot); const condoRoot = new THREE.Group(); scene.add(condoRoot); let ROOT = mainRoot;
const camera = new THREE.PerspectiveCamera(40, 1, .1, 200);
const controls = new OrbitControls(camera, canvas);
Object.assign(controls, { enableDamping: true, dampingFactor: .08, enablePan: false, minDistance: 4, maxDistance: 42, minPolarAngle: .25, maxPolarAngle: 1.25, rotateSpeed: .6 });
scene.add(new THREE.HemisphereLight(0xfdf6ec, 0x8e9a78, 1.35));
const sun = new THREE.DirectionalLight(0xfff0da, 2.6); sun.position.set(-9, 18, 12); sun.castShadow = true; sun.shadow.mapSize.set(2048, 2048);
Object.assign(sun.shadow.camera, { left: -14, right: 14, top: 14, bottom: -14, near: 1, far: 60 }); sun.shadow.bias = -.0004; sun.shadow.normalBias = .04; scene.add(sun);
const fill = new THREE.DirectionalLight(0xdfe8ff, .5); fill.position.set(10, 8, -8); scene.add(fill);

function tex(w, h, draw, rx = 1, ry = 1) { const c = document.createElement('canvas'); c.width = w; c.height = h; draw(c.getContext('2d'), w, h); const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace; t.wrapS = t.wrapT = THREE.RepeatWrapping; t.repeat.set(rx, ry); t.anisotropy = 8; return t; }
const rnd = (() => { let s = 7; return () => (s = (s * 16807) % 2147483647) / 2147483647; })();
const T = {
  siding: () => tex(256, 256, (g, w, h) => { g.fillStyle = '#fff'; g.fillRect(0, 0, w, h); for (let y = 0; y < h; y += 32) { const gr = g.createLinearGradient(0, y, 0, y + 32); gr.addColorStop(0, 'rgba(0,0,0,.20)'); gr.addColorStop(.12, 'rgba(0,0,0,.02)'); gr.addColorStop(1, 'rgba(0,0,0,.08)'); g.fillStyle = gr; g.fillRect(0, y, w, 32); } }),
  shingle: () => tex(256, 256, (g, w, h) => { for (let r = 0; r < 8; r++) for (let c = 0; c < 9; c++) { const v = 200 + rnd() * 55 | 0; g.fillStyle = `rgb(${v},${v},${v})`; const off = r % 2 ? 14 : 0; g.fillRect(c * 30 - off, r * 32, 29, 31); } g.fillStyle = 'rgba(0,0,0,.25)'; for (let r = 0; r < 8; r++) g.fillRect(0, r * 32 + 29, w, 3); }),
  grass: () => tex(256, 256, (g, w, h) => { g.fillStyle = '#8fbf6a'; g.fillRect(0, 0, w, h); for (let i = 0; i < 2600; i++) { g.fillStyle = `rgba(${60 + rnd() * 60 | 0},${120 + rnd() * 60 | 0},${40 + rnd() * 40 | 0},.35)`; g.fillRect(rnd() * w, rnd() * h, 2, 4); } }),
  planks: () => tex(256, 256, (g, w, h) => { for (let r = 0; r < 8; r++) { const v = rnd() * 25 | 0; g.fillStyle = `rgb(${196 - v},${146 - v},${92 - v})`; g.fillRect(0, r * 32, w, 32); g.fillStyle = 'rgba(0,0,0,.18)'; g.fillRect(0, r * 32, w, 2); g.fillRect((r * 97) % w, r * 32, 2, 32); } }),
  tile: () => tex(256, 256, (g, w, h) => { g.fillStyle = '#eceae4'; g.fillRect(0, 0, w, h); g.fillStyle = '#cfcac0'; for (let i = 0; i <= 4; i++) { g.fillRect(i * 64 - 1, 0, 2, h); g.fillRect(0, i * 64 - 1, w, 2); } }),
  brick: () => tex(256, 256, (g, w, h) => { g.fillStyle = '#d8cfc4'; g.fillRect(0, 0, w, h); for (let r = 0; r < 8; r++) for (let c = 0; c < 5; c++) { const v = rnd() * 30 | 0; g.fillStyle = `rgb(${170 - v},${90 - v},${68 - v})`; g.fillRect(c * 56 - (r % 2 ? 28 : 0) + 2, r * 32 + 2, 52, 28); } }),
  concrete: () => tex(256, 256, (g, w, h) => { g.fillStyle = '#d6d3cd'; g.fillRect(0, 0, w, h); for (let i = 0; i < 1500; i++) { g.fillStyle = `rgba(0,0,0,${rnd() * .06})`; g.fillRect(rnd() * w, rnd() * h, 3, 3); } g.fillStyle = 'rgba(0,0,0,.12)'; g.fillRect(0, h - 2, w, 2); }),
  asphalt: () => tex(256, 256, (g, w, h) => { g.fillStyle = '#5d6066'; g.fillRect(0, 0, w, h); for (let i = 0; i < 2500; i++) { g.fillStyle = `rgba(255,255,255,${rnd() * .06})`; g.fillRect(rnd() * w, rnd() * h, 2, 2); } }),
  garage: () => tex(256, 256, (g, w, h) => { g.fillStyle = '#f4f1ea'; g.fillRect(0, 0, w, h); for (let r = 0; r < 4; r++) { g.fillStyle = 'rgba(0,0,0,.12)'; g.fillRect(0, r * 64, w, 3); for (let c = 0; c < 4; c++) { g.strokeStyle = 'rgba(0,0,0,.08)'; g.strokeRect(c * 64 + 8, r * 64 + 10, 48, 44); } } }),
  osb: () => tex(256, 256, (g, w, h) => { g.fillStyle = '#c9a66b'; g.fillRect(0, 0, w, h); for (let i = 0; i < 500; i++) { g.fillStyle = `rgba(${120 + rnd() * 60 | 0},${80 + rnd() * 40 | 0},30,.35)`; g.save(); g.translate(rnd() * w, rnd() * h); g.rotate(rnd() * 3); g.fillRect(0, 0, 18, 5); g.restore(); } })
};
const mats = {
  siding: new THREE.MeshStandardMaterial({ color: 0xc9d1c3, map: T.siding(), roughness: .85, transparent: true }),
  paint: new THREE.MeshStandardMaterial({ color: 0xf2ede4, roughness: .9 }),
  trim: new THREE.MeshStandardMaterial({ color: 0xffffff, roughness: .6 }),
  roof: new THREE.MeshStandardMaterial({ color: 0x5a5f69, map: T.shingle(), roughness: .9, transparent: true }),
  glass: new THREE.MeshStandardMaterial({ color: 0x9ec4e6, roughness: .08, metalness: .35, envMapIntensity: 1 }),
  door: new THREE.MeshStandardMaterial({ color: 0x8a5a36, roughness: .55 }),
  found: new THREE.MeshStandardMaterial({ color: 0xb8b3aa, map: T.concrete(), roughness: .95 }),
  oak: new THREE.MeshStandardMaterial({ map: T.planks(), roughness: .7 }),
  tile: new THREE.MeshStandardMaterial({ map: T.tile(), roughness: .5 }),
  brick: new THREE.MeshStandardMaterial({ map: T.brick(), roughness: .9 }),
  grass: new THREE.MeshStandardMaterial({ map: T.grass(), roughness: 1, transparent: true }),
  conc: new THREE.MeshStandardMaterial({ map: T.concrete(), roughness: .95 }),
  asphalt: new THREE.MeshStandardMaterial({ map: T.asphalt(), roughness: 1 }),
  gdoor: new THREE.MeshStandardMaterial({ map: T.garage(), roughness: .6 }),
  cab: new THREE.MeshStandardMaterial({ color: 0x5f7a6a, roughness: .6 }), counter: new THREE.MeshStandardMaterial({ color: 0xefece6, roughness: .3 }),
  steel: new THREE.MeshStandardMaterial({ color: 0xd9dcdf, roughness: .3, metalness: .6 }), gutter: new THREE.MeshStandardMaterial({ color: 0xf0f0f0, roughness: .4 }),
  ghost: new THREE.MeshBasicMaterial({ color: 0x86b4ff, transparent: true, opacity: .28, depthWrite: false }),
  osb: new THREE.MeshStandardMaterial({ map: T.osb(), roughness: .9 }), soil: new THREE.MeshStandardMaterial({ color: 0x6e5038, roughness: 1 })
};
mats.grass.map.repeat.set(10, 9); mats.conc.map.repeat.set(2, 3); mats.asphalt.map.repeat.set(8, 1); mats.oak.map.repeat.set(2, 2); mats.tile.map.repeat.set(2, 2);
function mesh(g, m, x = 0, y = 0, z = 0, p) { const o = new THREE.Mesh(g, m); o.position.set(x, y, z); o.castShadow = o.receiveShadow = true; (p || ROOT).add(o); return o; }
function box(w, h, d, m, x, y, z, p) { return mesh(new THREE.BoxGeometry(w, h, d), m, x, y, z, p); }
function bbox(w, h, d, m, x, y, z, p) { const g = new THREE.BoxGeometry(w, h, d); g.translate(0, h / 2, 0); return mesh(g, m, x, y, z, p); }
function grp(p, x = 0, y = 0, z = 0) { const g = new THREE.Group(); g.position.set(x, y, z); (p || ROOT).add(g); return g; }
const CLONES = new Map();
function uv(m, rx, ry) { const t = m.material.map; if (!t) return; const base = m.material; m.material = m.material.clone(); if (!CLONES.has(base)) CLONES.set(base, []); CLONES.get(base).push(m.material); m.material.map = t.clone(); m.material.map.needsUpdate = true; m.material.map.repeat.set(rx, ry); }

const PARTS = {}; const TAGPOS = {};
function part(key, o, pos) { (PARTS[key] = PARTS[key] || []).push(o); if (pos) TAGPOS[key] = pos; return o; }

/* ---- lot, street, paths ---- */
const ground = box(24, .2, 21, mats.grass, 0, -.1, -1); ground.receiveShadow = true;
box(24, .1, 3, mats.asphalt, 0, -.04, 11); box(24, .12, 1.1, mats.conc, 0, .01, 9.0);
box(2.6, .12, 6.2, mats.conc, 5.8, .02, 5.0); // driveway
box(1.2, .12, 4.4, mats.conc, .7, .02, 6.6); // front walk
for (let i = -11; i < 12; i += 3) box(1.4, .02, .14, new THREE.MeshBasicMaterial({ color: 0xf5f0d8 }), i, .02, 11);

/* ---- house ---- */
const FY = .35, H = 2.6, WT = .2;
const house = grp();
bbox(8.2, FY, 6.2, mats.found, 0, 0, 0, house);
const extWalls = grp(house, 0, FY, 0); const inWalls = grp(house, 0, FY, 0);
function face(arr, out) { return [0, 1, 2, 3, 4, 5].map(i => i === out ? mats.siding : (i === 2 ? mats.trim : mats.paint)); }
function xwall(w, d, x, z, out, key) { const g = new THREE.BoxGeometry(w, H, d); g.translate(0, H / 2, 0); const m = new THREE.Mesh(g, face(null, out)); m.position.set(x, 0, z); m.castShadow = m.receiveShadow = true; extWalls.add(m); const sx = (out < 2 ? d : w) / 2, sy = H / 2; const t = mats.siding.map; return part(key, m); }
xwall(8.2, WT, 0, 3.0, 4, 'sFront'); xwall(8.2, WT, 0, -3.0, 5, 'sBack'); xwall(WT, 6.2, -4.0, 0, 1, 'sLeft'); xwall(WT, 6.2, 4.0, 0, 0, 'sRight');
// windows + door
const wins = grp(house, 0, FY, 0);
function win(x, y, z, w, h, ry) { const g = grp(wins, x, y, z); g.rotation.y = ry; box(w + .2, h + .2, .12, mats.trim, 0, 0, 0, g); box(w, h, .1, mats.glass, 0, 0, .03, g); box(.06, h, .14, mats.trim, 0, 0, .04, g); box(w, .06, .14, mats.trim, 0, 0, .04, g); box(w + .35, .08, .25, mats.trim, 0, -h / 2 - .12, .08, g); return g; }
win(-2.2, 1.55, 3.1, 1.6, 1.2, 0); win(2.8, 1.75, 3.1, .8, .8, 0); win(-2.2, 1.55, -3.1, 1.2, 1.1, Math.PI); win(2.2, 1.6, -3.1, 1.8, 1.0, Math.PI); win(-4.1, 1.55, 1.4, 1.2, 1.1, -Math.PI / 2); win(-4.1, 1.55, -1.6, 1.0, 1.0, -Math.PI / 2);
const door = grp(wins, .7, 0, 3.1); box(1.0, 2.1, .1, mats.door, 0, 1.05, 0, door); box(1.25, .12, .16, mats.trim, 0, 2.16, 0, door); box(.12, 2.2, .16, mats.trim, -.56, 1.1, 0, door); box(.12, 2.2, .16, mats.trim, .56, 1.1, 0, door); box(.06, .06, .08, mats.steel, .35, 1.0, .08, door);
// porch
const porch = grp(house, .7, 0, 3.0); box(2.4, FY, 1.3, mats.found, 0, FY / 2, .75, porch); box(2.4, .12, .4, mats.conc, 0, .06, 1.6, porch);
[-1.05, 1.05].forEach(x => box(.12, 2.4, .12, mats.trim, x, FY + 1.2, 1.3, porch)); const pr = box(2.8, .12, 1.6, mats.roof, 0, FY + 2.5, .75, porch); pr.rotation.x = .18;
// floors + interior walls (rooms)
const floorParts = grp(house, 0, FY, 0);
function rfloor(key, x0, x1, z0, z1, m, rx = 2, ry = 2) { const f = box(x1 - x0, .04, z1 - z0, m, (x0 + x1) / 2, .02, (z0 + z1) / 2, floorParts); uv(f, (x1 - x0) / 2.5 * rx / 2, (z1 - z0) / 2.5 * ry / 2); f.userData.area = key; return part(key, f, [(x0 + x1) / 2, 1.6, (z0 + z1) / 2]); }
rfloor('living', -3.9, 0, -.2, 2.9, mats.oak); rfloor('bed', -3.9, 0, -2.9, -.2, mats.oak); rfloor('entry', 0, 1.4, .6, 2.9, mats.oak); rfloor('bath', 1.4, 3.9, .6, 2.9, mats.tile); rfloor('kitchen', 0, 3.9, -2.9, .6, mats.tile);
const iw = (w, d, x, z) => bbox(w, H, d, mats.paint, x, 0, z, inWalls);
iw(3.9, .12, -1.95, -.2); iw(.12, 2.3, 1.4, 1.75); iw(2.5, .12, 2.65, .6); iw(.12, 1.0, 0, 2.4);
// kitchen
const kit = grp(house, 0, FY, 0);
bbox(3.6, .9, .62, mats.cab, 2.05, 0, -2.58, kit); box(3.7, .05, .68, mats.counter, 2.05, .93, -2.58, kit);
bbox(.62, .9, 2.4, mats.cab, 3.58, 0, -1.4, kit); box(.68, .05, 2.5, mats.counter, 3.58, .93, -1.4, kit);
bbox(.8, 1.9, .7, mats.steel, .5, 0, -2.55, kit); bbox(1.8, .9, .9, mats.cab, 1.9, 0, -.9, kit); box(1.95, .05, 1.05, mats.counter, 1.9, .93, -.9, kit);
// bath
bbox(1.6, .55, .8, mats.trim, 3.05, 0, 2.45, kit); bbox(.9, .8, .5, mats.cab, 1.95, 0, 2.6, kit); box(.95, .04, .55, mats.counter, 1.95, .82, 2.6, kit); bbox(.4, .45, .6, mats.trim, 2.75, 0, 1.0, kit);
// gables + roof
const roof = grp(house, 0, FY + H, 0);
const RISE = 1.9, HALF = 3.55, ang = Math.atan2(RISE, HALF), SL = Math.hypot(HALF, RISE);
function gable(x, key) { const s = new THREE.Shape(); s.moveTo(-3.1, 0); s.lineTo(3.1, 0); s.lineTo(0, RISE * 3.1 / HALF); s.closePath(); const g = new THREE.ExtrudeGeometry(s, { depth: WT, bevelEnabled: false }); g.rotateY(Math.PI / 2); const m = mesh(g, mats.siding, x - WT / 2, 0, 0, roof); uv(m, 1, 1); return part(key, m); }
gable(-4.0, 'sLeft'); gable(4.0, 'sRight');
const rB = box(8.9, .16, SL, mats.roof, 0, RISE / 2, -HALF / 2, roof); rB.rotation.x = -ang; uv(rB, 4, 2); part('back', rB, [-.5, FY + H + 2.6, -1.6]);
const rF = box(8.9, .16, SL, mats.roof, 0, RISE / 2, HALF / 2, roof); rF.rotation.x = ang; uv(rF, 4, 2); part('front', rF, [-.5, FY + H + 2.6, 1.6]);
box(9.0, .14, .22, mats.trim, 0, RISE + .04, 0, roof);
const chim = grp(roof, -2.4, 0, -1.6); const ch = box(.7, 2.6, .7, mats.brick, 0, 1.3, 0, chim); box(.85, .12, .85, mats.found, 0, 2.62, 0, chim); part('chimney', ch, [-2.4, FY + H + 3.1, -1.6]);
const flash = box(.9, .06, .9, mats.steel, 0, .55 * 1.6, 0, chim);
[3.62, -3.62].forEach(z => { box(8.9, .14, .14, mats.gutter, 0, -.05, z, roof); });
[[-4.35, 3.62], [4.35, -3.62]].forEach(([x, z]) => box(.1, H + FY, .1, mats.gutter, x, -(H + FY) / 2, z, roof));
// roof deck (x-ray for the roof)
const deck = grp(roof); deck.visible = false; const rotM = new THREE.MeshStandardMaterial({ color: 0x4f3a29, roughness: 1 });
[[rB, -1], [rF, 1]].forEach(([slab, sg]) => { const g = grp(deck); g.position.copy(slab.position); g.rotation.copy(slab.rotation); for (let i = 0; i < 4; i++) for (let j = 0; j < 2; j++) { const bad = sg > 0 && i === 1 && j === 0; box(2.15, .05, SL / 2 - .06, bad ? rotM : mats.osb, -3.3 + i * 2.2, -.1, (j ? 1 : -1) * SL / 4, g); } });
const rotPos = [-1.1, FY + H + 1.1, 2.3];

/* ---- garage ---- */
const gar = grp(ROOT, 0, 0, 0);
bbox(3.6, .15, 5.0, mats.found, 5.8, 0, -.55, gar);
const gW = grp(gar, 0, .15, 0); const GH = 2.4;
function gwall(w, d, x, z, out) { const g = new THREE.BoxGeometry(w, GH, d); g.translate(0, GH / 2, 0); const m = new THREE.Mesh(g, face(null, out)); m.position.set(x, 0, z); m.castShadow = m.receiveShadow = true; gW.add(m); return part('sGarage', m); }
const gFront = gwall(3.6, .18, 5.8, 1.9, 4); gwall(3.6, .18, 5.8, -3.0, 5); gwall(.18, 5.0, 7.55, -.55, 0);
const gd = box(2.6, 2.0, .06, mats.gdoor, 5.8, 1.15, 2.0, gar); const gdTrim = box(2.85, .1, .1, mats.trim, 5.8, 2.2, 2.0, gar);
const groof = grp(gar, 5.8, .15 + GH, -.55); const ga = Math.atan2(1.1, 2.1), gsl = Math.hypot(2.1, 1.1);
const g1 = box(gsl, .14, 5.6, mats.roof, -1.05, .55, 0, groof); g1.rotation.z = ga; uv(g1, 1.5, 3); const g2 = box(gsl, .14, 5.6, mats.roof, 1.05, .55, 0, groof); g2.rotation.z = -ga; uv(g2, 1.5, 3);
part('garageRoof', g1, [5.8, 4.0, -.5]); part('garageRoof', g2);
const gg = new THREE.Shape(); gg.moveTo(-1.8, 0); gg.lineTo(1.8, 0); gg.lineTo(0, 1.0); gg.closePath(); const gge = new THREE.ExtrudeGeometry(gg, { depth: .18, bevelEnabled: false }); const gF = mesh(gge, mats.siding, 0, 0, 2.38, groof); gF.position.z = 2.36 - 2.36 + 2.36; gF.position.set(0, 0, 2.27); const gB = mesh(gge, mats.siding, 0, 0, -2.45, groof);
// water heater (one-item renovation)
const wh = grp(gar, 6.75, .15, -2.35); const tank = mesh(new THREE.CylinderGeometry(.32, .32, 1.5, 24), new THREE.MeshStandardMaterial({ color: 0xf1f1ee, roughness: .45 }), 0, .75, 0, wh); mesh(new THREE.CylinderGeometry(.33, .33, .06, 24), mats.steel, 0, 1.5, 0, wh); [-.12, .12].forEach(x => mesh(new THREE.CylinderGeometry(.03, .03, .9, 8), new THREE.MeshStandardMaterial({ color: 0xb87333, metalness: .7, roughness: .3 }), x, 1.95, 0, wh)); box(.2, .26, .02, new THREE.MeshStandardMaterial({ color: 0xffd34d }), 0, 1.0, .33, wh);
part('wh', tank, [6.75, 2.4, -2.35]);
/* ---- yard ---- */
const yard = grp();
const patio = box(5, .08, 3, mats.ghost, 0, .05, -5.6, yard); part('patio', patio, [0, .8, -5.6]);
const patioEdge = new THREE.LineSegments(new THREE.EdgesGeometry(new THREE.BoxGeometry(5, .6, 3)), new THREE.LineDashedMaterial({ color: 0x5b8ddc, dashSize: .2, gapSize: .14 })); patioEdge.position.set(0, .3, -5.6); patioEdge.computeLineDistances(); yard.add(patioEdge);
const beds = []; [[-5, -9.6], [0, -9.8], [5, -9.6]].forEach(([x, z]) => { const b = box(3.4, .22, 1.0, mats.soil, x, .1, z, yard); beds.push(b); part('beds', b, [0, .9, -9.6]); });
const drain = box(.8, .03, 8, new THREE.MeshStandardMaterial({ color: 0x7a8f63, roughness: 1, transparent: true, opacity: .9 }), -9.9, .02, -6, yard); part('drain', drain, [-9.6, .7, -6]);
const lawn = box(7, .02, 3.2, new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: 0, depthWrite: false }), 4.5, .02, -7.2, yard); part('lawn', lawn, [4.5, .6, -7.2]);
// underground lines (x-ray for the yard)
const under = grp(); under.visible = false;
const line = (pts, c, r = .05) => { const cu = new THREE.CatmullRomCurve3(pts.map(p => new THREE.Vector3(p[0], -.12, p[1]))); mesh(new THREE.TubeGeometry(cu, 40, r, 6, false), new THREE.MeshBasicMaterial({ color: c }), 0, 0, 0, under); };
line([[-4.1, -2.0], [-7, -3], [-11, -3.4]], 0xffd23d, .07); line([[3.9, -2.8], [6, -6], [9, -10]], 0xd94a3d); line([[1, -3.2], [2.5, -6.5], [6, -8], [8, -8.4]], 0x3a8ad9, .04); line([[-1, -3.2], [-3.5, -6.5], [-7, -8]], 0x3a8ad9, .04);

/* ---- lake cabin (A-frame) ---- */
ROOT = cabinRoot;
box(26, .2, 24, mats.grass, 0, -.1, -1);
const lake = box(26, .1, 9, new THREE.MeshStandardMaterial({ color: 0x5d97bf, roughness: .15, metalness: .2 }), 0, -.02, 10);
const pineM = new THREE.MeshStandardMaterial({ color: 0x3f6b4a, roughness: .9, flatShading: true }), trunkM = new THREE.MeshStandardMaterial({ color: 0x6b4a33, roughness: 1 });
function pine(x, z, h) { const g = grp(cabinRoot, x, 0, z); mesh(new THREE.CylinderGeometry(.12, .16, h * .25, 6), trunkM, 0, h * .12, 0, g); for (let i = 0; i < 3; i++) mesh(new THREE.ConeGeometry(h * (.28 - i * .06), h * .42, 7), pineM, 0, h * (.32 + i * .2), 0, g); }
[[-9, -6, 7], [-6, -9, 8], [7, -8, 7.5], [10, -3, 6], [-11, 1, 6.5], [11, 3, 5.5], [-3, -10, 7], [3, -11, 8], [-8, 5, 5]].forEach(a => pine(...a));
const cab = grp(cabinRoot); const AH = 6.4, AW = 3.3, AD = 6.4, aL = Math.hypot(AW, AH), aA = Math.atan2(AH, AW);
const cRoofM = mats.roof.clone(); cRoofM.color = new THREE.Color(0x3d5446); cRoofM.map = mats.roof.map.clone(); cRoofM.map.needsUpdate = true; cRoofM.map.repeat.set(3, 4);
const sL = box(aL, .22, AD, cRoofM, -AW / 2, AH / 2 + .3, -1.2, cab); sL.rotation.z = aA; const sR = box(aL, .22, AD, cRoofM, AW / 2, AH / 2 + .3, -1.2, cab); sR.rotation.z = -aA;
const tri = new THREE.Shape(); tri.moveTo(-AW + .2, 0); tri.lineTo(AW - .2, 0); tri.lineTo(0, AH - .4); tri.closePath();
const woodM = new THREE.MeshStandardMaterial({ color: 0xb98652, map: T.siding(), roughness: .8 }); woodM.map.repeat.set(1, 3);
const back = mesh(new THREE.ExtrudeGeometry(tri, { depth: .15, bevelEnabled: false }), woodM, 0, .3, -4.4, cab);
const front = mesh(new THREE.ExtrudeGeometry(tri, { depth: .08, bevelEnabled: false }), mats.glass, 0, .3, 1.9, cab);
for (let i = -2; i <= 2; i++) box(.08, AH * (1 - Math.abs(i) / 3.2), .14, mats.trim, i * 1.1, .3 + AH * (1 - Math.abs(i) / 3.2) / 2, 2.0, cab);
[1.6, 3.2].forEach(y => box((AW - .2) * 2 * (1 - (y) / (AH - .4)), .08, .14, mats.trim, 0, .3 + y, 2.0, cab));
box(1.0, 2.0, .1, mats.door, 1.2, 1.3, 2.02, cab);
bbox(6.8, .3, 6.6, mats.found, 0, 0, -1.2, cab);
const deckG = grp(cabinRoot, 0, 0, 0); const deckM = mats.oak.clone(); deckM.map = mats.oak.map.clone(); deckM.map.needsUpdate = true; deckM.map.repeat.set(3, 1.5);
const deckTop = box(9, .14, 4, deckM, 0, .95, 4.2, deckG); part('deck', deckTop, [0, 2.1, 4.4]);
for (let x = -4.2; x <= 4.2; x += 2.1) for (const z of [2.4, 6.0]) box(.18, .9, .18, trunkM, x, .45, z, deckG);
for (let x = -4.4; x <= 4.4; x += .35) box(.04, .8, .04, new THREE.MeshStandardMaterial({ color: 0x222222 }), x, 1.4, 6.15, deckG);
box(9, .08, .1, new THREE.MeshStandardMaterial({ color: 0x222222 }), 0, 1.82, 6.15, deckG);
const stairs = grp(cabinRoot, 5.2, 0, 4.6); for (let i = 0; i < 4; i++) box(1.2, .14, .45, deckM, 0, .9 - i * .24, i * .45 - .2, stairs); part('stairs', stairs.children[0], [5.4, 1.6, 4.9]); stairs.children.forEach(c => part('stairs', c));
const pipe = mesh(new THREE.CylinderGeometry(.14, .14, 1.6, 12), new THREE.MeshStandardMaterial({ color: 0x1f1f1f, roughness: .5 }), 1.3, 5.3, -2.4, cab); part('stove', pipe, [1.3, 6.6, -2.4]);
const dock = box(1.6, .12, 5, deckM, -6, .15, 9, cabinRoot);

/* ---- rental condo (unit 4B) ---- */
ROOT = condoRoot;
box(30, .2, 26, mats.conc, 0, -.1, 0);
const facade = tex(256, 256, (g, w, h) => { g.fillStyle = '#d9cfc2'; g.fillRect(0, 0, w, h); g.fillStyle = '#8fb0cc'; for (let r = 0; r < 2; r++) for (let c = 0; c < 2; c++) g.fillRect(c * 128 + 24, r * 128 + 26, 80, 70); g.fillStyle = 'rgba(0,0,0,.12)'; g.fillRect(0, 126, w, 4); }, 7, 2);
const facM = new THREE.MeshStandardMaterial({ map: facade, roughness: .85 });
const FL = 2.6; bbox(14, FL * 3, 10, facM, 0, 0, 0, condoRoot);
const topLeft = bbox(8, FL, 10, facM.clone(), -3, FL * 3, 0, condoRoot); topLeft.material.map = facade.clone(); topLeft.material.map.repeat.set(4, 1); topLeft.material.map.needsUpdate = true;
const unitG = grp(condoRoot, 4, FL * 3, 0);
const uFloor = box(5.8, .1, 9.8, mats.oak.clone(), 0, .05, 0, unitG); part('unit', uFloor, [4, FL * 3 + 1.8, 0]);
const uw = M => M; const uWall = new THREE.MeshStandardMaterial({ color: 0xe9e4da, roughness: .9 });
[[5.8, .16, 0, 4.9], [5.8, .16, 0, -4.9], [.16, 9.8, 2.9, 0]].forEach(([w, d, x, z]) => bbox(w, 1.1, d, uWall, x, .1, z, unitG));
bbox(.12, 1.1, 4, uWall, -.6, .1, -2.9, unitG); bbox(2.4, 1.1, .12, uWall, 1.7, .1, -1, unitG);
const balc = box(5.8, .15, 1.6, mats.conc, 4, FL * 3, 5.8, condoRoot); for (let x = 1.4; x <= 6.8; x += .3) box(.04, .9, .04, new THREE.MeshStandardMaterial({ color: 0x333333 }), x, FL * 3 + .5, 6.55, condoRoot);
box(14.4, .3, 10.4, mats.found, 0, FL * 4 + .15, 0, condoRoot).visible = false;
[[-11, 9], [11, 9], [-11, -9], [12, -6]].forEach(([x, z]) => { const g = grp(condoRoot, x, 0, z); mesh(new THREE.CylinderGeometry(.15, .2, 1.4, 6), trunkM, 0, .7, 0, g); mesh(new THREE.IcosahedronGeometry(1.6, 0), new THREE.MeshStandardMaterial({ color: 0x7fae5c, flatShading: true }), 0, 2.3, 0, g); });
for (let x = -12; x <= 12; x += 3) box(2.2, .02, 4.6, new THREE.MeshBasicMaterial({ color: 0xffffff }), x, .02, 10.5).scale.set(.06, 1, 1);
ROOT = mainRoot;
/* ======================================================================
   x-ray extras · models
   ====================================================================== */
const studs = grp(house, 0, FY, 0); studs.visible = false;
const studM = new THREE.MeshBasicMaterial({ color: 0xd9b77e });
for (let x = -3.85; x <= 3.9; x += .4) box(.05, H, .08, studM, x, H / 2, -3.0, studs);
box(5, .035, .05, new THREE.MeshBasicMaterial({ color: 0xd94a3d }), 1.4, 1.0, -2.98, studs); box(.035, .8, .05, new THREE.MeshBasicMaterial({ color: 0xd94a3d }), 3.0, .6, -2.98, studs); box(3.6, .035, .05, new THREE.MeshBasicMaterial({ color: 0x2f6fd9 }), 2.0, 1.25, -2.98, studs); box(.07, H, .07, new THREE.MeshBasicMaterial({ color: 0x9aa3ad }), 3.4, H / 2, -2.96, studs);
const sheath = grp(house, 0, FY, 0); sheath.visible = false;
[[8.0, .06, 0, 2.86], [8.0, .06, 0, -2.86], [.06, 5.8, -3.86, 0]].forEach(([w, d, x, z]) => bbox(w, H, d, mats.osb, x, 0, z, sheath));
const footings = grp(cabinRoot); footings.visible = false; for (let x = -4.2; x <= 4.2; x += 2.1) for (const z of [2.4, 6.0]) mesh(new THREE.CylinderGeometry(.25, .3, 1.2, 10), mats.conc, x, -.5, z, footings);

const GLB = {}; const loader = new GLTFLoader();
function b64(s) { const bin = atob(s); const u = new Uint8Array(bin.length); for (let i = 0; i < bin.length; i++) u[i] = bin.charCodeAt(i); return u.buffer; }
async function loadAll() { await Promise.all(Object.keys(MODELS).map(k => new Promise((res, rej) => loader.parse(b64(MODELS[k]), './', g => { GLB[k] = g; res(); }, rej)))); }
function inst(name, size, by = 'h', parent) { const o = GLB[name].scene.clone(true); o.updateMatrixWorld(true); const b = new THREE.Box3().setFromObject(o); const d = b.getSize(new THREE.Vector3()); o.scale.multiplyScalar(size / (by === 'h' ? d.y : d.x)); o.updateMatrixWorld(true); const b2 = new THREE.Box3().setFromObject(o); const c = b2.getCenter(new THREE.Vector3()); o.position.set(-c.x, -b2.min.y, -c.z); o.traverse(m => { if (m.isMesh) m.castShadow = m.receiveShadow = true; }); const w = new THREE.Group(); w.add(o); (parent || mainRoot).add(w); return w; }
function put(n, s, x, y, z, ry = 0, by = 'h', parent) { const o = inst(n, s, by, parent); o.position.set(x, y, z); o.rotation.y = ry; return o; }
const FENCE = [];
function furnish() {
  const Fu = grp(house, 0, FY + .04, 0);
  put('rug_rectangle_stripes_A', 2.2, -2.0, 0, 1.3, 0, 'w', Fu); put('couch_pillows', .8, -2.0, 0, .35, 0, 'h', Fu); put('table_low', .34, -2.0, 0, 1.3, 0, 'h', Fu); put('armchair', .78, -3.2, 0, 2.2, 2.3, 'h', Fu); put('lamp_standing', 1.4, -3.5, 0, .3, 0, 'h', Fu); put('shelf_B_large_decorated', .5, -.9, 0, 2.6, Math.PI, 'h', Fu);
  put('bed_double_A', .7, -2.0, 0, -1.7, 0, 'h', Fu); put('cabinet_small', .6, -.6, 0, -2.6, 0, 'h', Fu); put('cactus_medium_A', .45, .8, 0, 2.3, 0, 'h', Fu);
  [[1.5, -.3], [2.3, -.3]].forEach(([x, z]) => put('chair_A_wood', .85, x, 0, z, Math.PI, 'h', Fu));
  put('car_stationwagon', 1.5, 5.8, 0, 5.2, Math.PI, 'h', mainRoot);
  [[-8.5, 6.5, 'tree_oak', 3.6], [9.2, 6, 'tree_fat', 3.0], [-9, -8.6, 'tree_fat', 3.4], [8.8, -9.5, 'tree_oak', 3.8], [-6.5, -10.3, 'tree_oak', 3.2]].forEach(([x, z, n, h]) => put(n, h, x, 0, z));
  [[-3.4, 3.7], [-1.8, 3.7], [2.6, 3.6], [3.4, 3.6], [-4.6, 2.0], [-4.6, 0]].forEach(([x, z]) => put('bush', .6, x, 0, z));
  beds.forEach(b => { for (let i = -1; i <= 1; i++) put('bush', .55, b.position.x + i * 1.0, .2, b.position.z); });
  for (let x = -10.5; x <= 10.5; x += 1.5) FENCE.push(put('fence', 1.5, x, 0, -10.6, 0, 'w'));
  for (let z = -9.8; z <= -3.8; z += 1.5) { FENCE.push(put('fence', 1.5, -11.2, 0, z, Math.PI / 2, 'w')); FENCE.push(put('fence', 1.5, 11.2, 0, z, Math.PI / 2, 'w')); }
  put('bench', .6, 6.5, 0, -6.2, -Math.PI / 2); applyLook();
  put('bench', .6, -2.5, 1.02, 5.4, 0, 'h', cabinRoot); put('armchair', .7, 2.4, 1.02, 4.0, -.4, 'h', cabinRoot);
  put('couch_pillows', .7, -1, .1, 2.6, Math.PI, 'h', unitG); put('rug_rectangle_stripes_A', 2, -1, .1, 1.2, 0, 'w', unitG); put('bed_double_A', .62, 1.4, .1, -3.2, 0, 'h', unitG); put('table_medium', .7, -1.6, .1, -2.6, 0, 'h', unitG); put('lamp_standing', 1.3, -2.4, .1, 3.8, 0, 'h', unitG);
}

/* ======================================================================
   persistence (this browser only)
   ====================================================================== */
const KEY = 'atlas-reno-v5';
let saveT;
function save() { clearTimeout(saveT); saveT = setTimeout(() => { try { localStorage.setItem(KEY, JSON.stringify({ PROJ, THINGS, HOMES, people: S.people, home: S.home })); } catch (e) { toast('This demo couldn’t keep that photo — your browser’s storage is full'); } }, 250); }
function load() { try { const d = JSON.parse(localStorage.getItem(KEY) || 'null'); if (!d || !d.PROJ) return; PROJ.length = 0; d.PROJ.forEach(p => PROJ.push(p)); Object.keys(THINGS).forEach(k => delete THINGS[k]); Object.assign(THINGS, d.THINGS); Object.assign(HOMES, d.HOMES); S.people = d.people || S.people; if (HOMES[d.home]) S.home = d.home; } catch (e) {} }
function resetDemo() { try { localStorage.removeItem(KEY); } catch (e) {} location.reload(); }


/* ======================================================================
   tags · pins · popover · highlight
   ====================================================================== */
const AREA_TAG = { sFront: [0, 2.6, 3.4], sLeft: [-4.5, 2.6, 0], sBack: [0, 2.6, -3.4], sGarage: [7.9, 2.2, -.5], bath: [2.6, 1.6, 1.8] };
const PROJ_TAG = { kitchen: [2.6, FY + H + 2.3, -.6], roof: [-1.2, FY + H + 2.4, .6], siding: [-4.5, 1.8, 1.8], yard: [0, 1.1, -6.6], heater: [6.75, 3.3, -2.35], deck: [0, 2.4, 4.4], stove: [1.3, 7.0, -2.4], paint: [4, FL * 3 + 2.4, 0] };
const areaPos = k => AREA_TAG[k] || TAGPOS[k] || [0, 2, 0];
function projPos(q) { if (PROJ_TAG[q.id]) return PROJ_TAG[q.id]; const k = Object.keys(q.areas)[0]; const p = areaPos(k).slice(); p[1] += .6; return p; }
const tagsEl = $('#tags'); let tagEls = [], pinEls = [];
function mk(cls, html, fn, label) { const el = document.createElement('button'); el.className = cls; el.innerHTML = html; if (label) el.setAttribute('aria-label', label); el.addEventListener('click', e => { e.stopPropagation(); fn(); }); tagsEl.appendChild(el); return el; }
function buildTags() {
  tagEls.forEach(t => t.el.remove()); pinEls.forEach(t => t.el.remove()); tagEls = []; pinEls = [];
  const p = cur();
  if (!p) {
    if (S.time && (S.year ?? TODAY) < TODAY) return;
    const seen = {};
    projOf(S.home).filter(q => q.status !== 'done').forEach(q => { const pos = projPos(q).slice(); const key = pos.map(v => Math.round(v)).join(','); seen[key] = (seen[key] || 0) + 1; pos[1] += (seen[key] - 1) * .9; const n = q.needs.length; tagEls.push({ el: mk('tag' + (POP && POP.id === q.id ? ' sel' : ''), `<span class="n ${n ? '' : 'z'}">${n || '✓'}</span><span>${q.icon} ${esc(q.name)}</span>`, () => POP && POP.id === q.id ? closePop() : openPop({ kind: 'proj', id: q.id, pos })), pos }); });
    return;
  }
  Object.entries(p.areas).forEach(([k, name]) => { const n = p.needs.filter(x => x.area === k).length; tagEls.push({ el: mk('tag' + (S.area === k ? ' sel' : ''), `<span class="n ${n ? '' : 'z'}">${n || '✓'}</span><span>${esc(name)}</span>`, () => focusArea(k)), pos: areaPos(k) }); });
  p.photos.forEach(ph => pinEls.push({ el: mk('pin', ph.src ? `<img src="${ph.src}" alt="" style="width:100%;height:100%;object-fit:cover;border-radius:17px">` : '📷', () => openPhoto(p, ph), ph.t), pos: ph.pos }));
  if (p.id === 'roof') pinEls.push({ el: mk('pin', '!', () => toast('Sample: the roofer flagged a soft spot here — that’s what the per-sheet price covers'), 'Soft spot'), pos: rotPos });
}
const pv = new THREE.Vector3();
function proj2d(pos) { pv.set(...pos).project(camera); const r = canvas.getBoundingClientRect(); return { x: (pv.x + 1) / 2 * r.width, y: (1 - pv.y) / 2 * r.height, z: pv.z, w: r.width, h: r.height }; }
function place(el, pos) { const q = proj2d(pos); if (q.z > 1 || q.x < -40 || q.x > q.w + 40 || q.y < $('.hd').offsetHeight - 4 || q.y > q.h - sheetCover() - 8) { el.style.display = 'none'; return; } el.style.display = ''; el.style.left = q.x + 'px'; el.style.top = q.y + 'px'; }
function sheetCover() { return $('#sheet').offsetHeight; }
function placeAll() { tagEls.forEach(t => S.xray ? (t.el.style.display = 'none') : place(t.el, t.pos)); pinEls.forEach(t => S.xray ? place(t.el, t.pos) : (t.el.style.display = 'none')); declutter(); placePop(); }
function declutter() { const vis = tagEls.filter(t => t.el.style.display !== 'none').map(t => ({ el: t.el, x: parseFloat(t.el.style.left), y: parseFloat(t.el.style.top), w: t.el.offsetWidth + 6, h: t.el.offsetHeight + 4 })).sort((a, b) => a.y - b.y); for (let i = 0; i < vis.length; i++) for (let j = 0; j < i; j++) { const A = vis[j], Bq = vis[i]; if (Math.abs(A.x - Bq.x) < (A.w + Bq.w) / 2 && Math.abs(A.y - Bq.y) < (A.h + Bq.h) / 2) { Bq.y = A.y + (A.h + Bq.h) / 2; } } vis.forEach(v => v.el.style.top = v.y + 'px'); }
const glow = new THREE.Group(); scene.add(glow); const glowMat = new THREE.LineBasicMaterial({ color: 0xff9a3c, transparent: true, opacity: .9, depthTest: false });
function highlight() { glow.clear(); const p = cur(); const keys = p ? Object.keys(p.areas).filter(k => !S.area || S.area === k) : (POP && POP.kind === 'proj' ? Object.keys(P(POP.id).areas) : []); keys.forEach(k => (PARTS[k] || []).forEach(m => { if (!isShown(m)) return; m.updateMatrixWorld(true); const e = new THREE.LineSegments(new THREE.EdgesGeometry(m.geometry, 30), glowMat); e.matrixAutoUpdate = false; e.matrix.copy(m.matrixWorld); e.renderOrder = 5; glow.add(e); })); }
function isShown(o) { while (o) { if (!o.visible) return false; o = o.parent; } return true; }

/* popover: a small card where you tapped */
let POP = null; const popEl = $('#pop');
function openPop(o) { POP = o; drawPop(); buildTags(); highlight(); buzz(6); arm(); }
function closePop() { if (!POP) return; POP = null; popEl.classList.remove('on'); buildTags(); highlight(); render(); }
function lastDone(th) { const d = PROJ.filter(q => q.thing === th && q.status === 'done').sort((a, b) => b.year - a.year)[0]; if (d) return `${d.name} · ${d.year}`; const h = (THINGS[th] || { hist: [] }).hist.slice(-1)[0]; return h ? `${h.t} · ${h.y}` : ''; }
function drawPop() {
  if (!POP) { popEl.classList.remove('on'); return; }
  if (POP.kind === 'proj') {
    const q = P(POP.id); if (!q) { POP = null; popEl.classList.remove('on'); return; } const m = moneyOf(q), n = q.needs.length;
    const meta = q.status === 'done' ? `Finished ${q.year}${m.cost ? ' · ' + money(m.cost) : ''}` : q.status === 'planned' ? `Planned ${q.year} · ${q.stops[0]}` : `Step ${Math.min(q.here + 1, q.stops.length)} of ${q.stops.length} · ${q.stops[Math.min(q.here, q.stops.length - 1)]}`;
    const last = lastDone(q.thing);
    popEl.innerHTML = `<b>${q.icon} ${esc(q.name)}</b><div class="m">${esc(meta)}</div>${n ? `<div class="pn"><span class="n">${n}</span>${esc(q.needs[0].title)}${n > 1 ? ` <small>+${n - 1} more</small>` : ''}</div>` : ''}${q.status !== 'done' && q.budget ? `<div class="pn">💵 ${money(Math.max(0, m.left))} of ${money(q.budget)} left</div>` : ''}${last ? `<div class="pn">🕓 Last done: ${esc(last)}</div>` : ''}<div class="row"><button class="btn" data-po="open">Open</button><button class="btn g" data-po="x">Close</button></div>`;
  } else {
    const p = cur(); const k = POP.k; if (!p || !p.areas[k]) { POP = null; popEl.classList.remove('on'); return; }
    const n = p.needs.filter(x => x.area === k).length, ph = p.photos.filter(x => x.area === k).length, ag = p.agreed.filter(x => x.area === k).length;
    popEl.innerHTML = `<b>📍 ${esc(p.areas[k])}</b><div class="m">${n} open · ${ag} agreed · ${ph} photo${ph === 1 ? '' : 's'} · the card below shows only this</div><div class="row"><button class="btn" data-po="add">＋ Add here</button><button class="btn g" data-po="all">Show all</button></div>`;
  }
  popEl.querySelectorAll('[data-po]').forEach(b => b.addEventListener('click', e => { e.stopPropagation(); const a = b.dataset.po; const o = POP;
    if (a === 'open') { POP = null; popEl.classList.remove('on'); go(o.id); }
    if (a === 'x') closePop();
    if (a === 'add') { openAdd({ proj: S.p, area: o.k }); }
    if (a === 'all') { POP = null; popEl.classList.remove('on'); focusArea(S.area); } }));
  popEl.classList.add('on'); placePop();
}
function placePop() {
  if (!POP) return; const q = proj2d(POP.pos); const top = $('.hd').offsetHeight, bot = q.h - sheetCover();
  if (q.z > 1 || q.y < top - 10 || q.y > bot + 10) { popEl.style.visibility = 'hidden'; return; } popEl.style.visibility = '';
  const ph = popEl.offsetHeight || 140; const below = q.y - ph - 18 < top; popEl.classList.toggle('below', below);
  const x = clamp(q.x, 133, q.w - 133); popEl.style.left = x + 'px'; popEl.style.top = Math.min(q.y, bot - (below ? ph + 18 : 0)) + 'px'; popEl.style.setProperty('--ax', (q.x - x) + 'px');
}

/* ======================================================================
   the house through time
   ====================================================================== */
const TODAY = 2026;
function setM(base, o) { [base, ...(CLONES.get(base) || [])].forEach(m => { for (const k in o) { if (k === 'color') m.color.set(o[k]); else m[k] = o[k]; } m.needsUpdate = true; }); }
const LOOK0 = { roof: '#5a5f69', oldRoof: '#a0724f', siding: '#d6c49c', trim: '#f2ede2' };
const hexOf = v => { const c = Object.values(COLOURS).flat().find(x => v && v.startsWith(x.n)); return c && c.hex; };
const fieldOf = (q, k) => q && (q.details.find(f => f.k === k) || {}).v;
const effYear = q => q.status === 'done' ? q.year : q.status === 'active' ? TODAY + 1 : q.year;
function applyLook() {
  const y = S.year ?? TODAY, p = cur(), timeOn = S.year != null;
  const mainH = S.home === 'main'; const roofP = mainH && P('roof'), sidP = mainH && P('siding'), yardP = mainH && P('yard'); const pid = p && p.id;
  let rc = mainH && y < 2009 ? LOOK0.oldRoof : LOOK0.roof;
  if (roofP && (pid === 'roof' || (timeOn && y >= effYear(roofP)))) rc = hexOf(fieldOf(roofP, 'Colour')) || rc;
  setM(mats.roof, { color: rc });
  let sc = LOOK0.siding, tc = LOOK0.trim;
  if (sidP && (pid === 'siding' || (timeOn && y >= effYear(sidP)))) { sc = hexOf(fieldOf(sidP, 'Colour')) || sc; tc = hexOf(fieldOf(sidP, 'Trim colour')) || tc; }
  setM(mats.siding, { color: sc }); setM(mats.trim, { color: tc });
  FENCE.forEach(f => f.visible = !mainH || y >= 2019);
  const pat = pid === 'yard' || (timeOn && yardP && y >= effYear(yardP)); patio.visible = patioEdge.visible = !!pat;
  const gn = $('#ghostnote'); let note = '';
  const XT = { rooms: 'inside the walls — wiring, pipes, studs', roof: 'the roof deck under the shingles', siding: 'the sheathing under the siding', yard: 'buried lines under the yard', deck: 'the footings', item: 'what’s behind it', condo: 'inside the walls' };
  if (S.xray && p) note = `X-ray: ${XT[p.view] || 'what’s hidden'} · tap to turn off`;
  else if (timeOn && y > TODAY) note = `Planned — not real yet · ${y}`;
  else if (p && p.status !== 'done' && ['roof', 'siding', 'yard'].includes(pid)) note = 'Showing your choices — not built yet';
  gn.textContent = note; gn.classList.toggle('on', !!note); gn.classList.toggle('xr', !!(S.xray && p));
}
const previewAll = applyLook;
function events(h) {
  const ev = [];
  Object.entries(THINGS).filter(([, t]) => t.home === h).forEach(([k, t]) => t.hist.forEach(r => ev.push({ y: r.y, t: r.t, s: r.s, thing: k, tn: t.name })));
  projOf(h).forEach(q => { if (q.status === 'done' && (THINGS[q.thing] || { hist: [] }).hist.some(r => r.y === q.year)) return; ev.push({ y: effYear(q), t: q.name + (q.status === 'done' ? '' : q.status === 'active' ? ' · finishing' : ' · planned'), s: q.where, id: q.id, thing: q.thing, tn: (THINGS[q.thing] || {}).name, fut: q.status !== 'done' }); });
  return ev.sort((a, b) => a.y - b.y);
}
function yearLabel(y) { const ev = events(S.home).filter(e => e.y === y); if (ev.length) return ev.map(e => e.t).join(' · '); if (y === TODAY) return 'Today'; return y > TODAY ? 'nothing else planned yet' : 'No changes recorded'; }
function buildTime() {
  const T = $('#timebar'); T.classList.toggle('on', S.time); $('#timeB').classList.toggle('on', S.time); $('#timeB').setAttribute('aria-pressed', S.time);
  if (!S.time) { T.innerHTML = ''; return; }
  const ev = events(S.home); const y0 = Math.min(TODAY, ...ev.map(e => e.y)), y1 = TODAY + 2, y = S.year ?? TODAY;
  const yrs = [...new Set(ev.map(e => e.y).concat(TODAY))];
  T.innerHTML = `<div class="yr"><b id="tmY">${y}</b><span id="tmL"></span><button class="xb" id="tmX" aria-label="Close the time view">✕</button></div><input type="range" id="tmR" min="${y0}" max="${y1}" step="1" value="${y}" aria-label="Year"><div class="ticks">${yrs.map(v => `<i class="${v > TODAY ? 'fut' : ''}${v === TODAY ? ' now' : ''}" data-y="${v}" title="${v}" style="left:calc(9px + (100% - 18px) * ${(v - y0) / (y1 - y0)})"></i>`).join('')}</div>`;
  const upd = () => { const v = +$('#tmR').value; S.year = v; $('#tmY').textContent = v; const lb = yearLabel(v); $('#tmL').textContent = v === TODAY ? (lb === 'Today' ? 'Today' : 'Today · ' + lb) : (v > TODAY ? 'Planned · ' : '') + lb; applyLook(); buildTags(); };
  $('#tmR').addEventListener('input', () => { const v = +$('#tmR').value; const near = yrs.find(e => Math.abs(e - v) <= 1 && e !== v); if (near && !$('#tmR').dataset.free) { /* gentle snap */ } upd(); });
  $('#tmR').addEventListener('change', () => { buzz(6); render(); });
  T.querySelectorAll('[data-y]').forEach(i => i.addEventListener('click', () => { $('#tmR').value = i.dataset.y; upd(); render(); }));
  $('#tmX').addEventListener('click', () => setTime(false));
  upd();
}
function setTime(on) { if (on && S.p) go(null); S.time = on; S.year = on ? TODAY : null; closePop(); buildTime(); applyLook(); buildTags(); render(); resize(); if (on) arm(); }
function jumpYear(y) { if (!S.time) setTime(true); $('#tmR').value = y; $('#tmR').dispatchEvent(new Event('input')); render(); }

/* ======================================================================
   views & camera
   ====================================================================== */
const CAM = { main: [[16, 15, 21], [1, .5, -1.5]], cabin: [[13, 9, 17], [0, 2.4, 0]], condo: [[19, 15, 21], [1.5, 6, 0]], rooms: [[7.5, 14, 11], [.6, 0, .2]], roof: [[-10, 11, 13], [0, 3.2, 0]], siding: [[-12, 5.5, 12.5], [.5, 1.8, 0]], yard: [[11, 11, -18], [0, 0, -6.5]], item: [[6.4, 3.4, 7.5], [6.75, 1.1, -2.3]], deck: [[9, 6, 14], [0, 1.5, 3]], condoUnit: [[11, 15, 11], [4, 8, 0]] };
const AREACAM = { kitchen: [[6, 9, 7], [2, 0, -1.2]], living: [[-6, 9, 9], [-2, 0, 1.2]], entry: [[3, 8, 9], [.7, 0, 1.7]], bath: [[5, 8, 8], [2.6, 0, 1.8]], front: [[-4, 9, 14], [-.5, 3.4, 1.5]], back: [[-4, 10, -13], [-.5, 3.4, -1.5]], garageRoof: [[12, 9, 6], [5.8, 3, -.5]], chimney: [[-8, 9, -9], [-2.4, 4.4, -1.6]], sFront: [[-3, 3.5, 12], [0, 1.6, 3]], sLeft: [[-12, 3.5, 2], [-4, 1.6, 0]], sBack: [[-3, 4, -12], [0, 1.6, -3]], sGarage: [[13, 3.5, 3], [7.5, 1.4, -.5]], patio: [[6, 7, -15], [0, 0, -5.6]], beds: [[4, 6, -3], [0, 0, -9.6]], drain: [[-3, 6, -14], [-9.8, 0, -6]], lawn: [[11, 6, -11], [4.5, 0, -7.2]], wh: [[6.5, 2.9, 5.2], [6.75, 1.1, -2.35]], deck: [[7, 5, 12], [0, 1, 4]], stairs: [[9, 4, 9], [5.2, .6, 4.6]], stove: [[6, 9, 4], [1.3, 5.2, -2.4]], unit: [[10, 14, 9], [4, 8, 0]] };
function viewOf(p) { if (!p) return modelOf(S.home); return p.view === 'condo' ? 'condoUnit' : p.view; }
function applyView() {
  const mo = modelOf(S.home); mainRoot.visible = mo === 'main'; cabinRoot.visible = mo === 'cabin'; condoRoot.visible = mo === 'condo';
  const p = cur(); const v = p ? p.view : 'home'; const cut = v === 'rooms';
  extWalls.scale.y = inWalls.scale.y = cut ? 1.15 / H : 1;
  roof.visible = !cut; wins.visible = !cut; porch.children[porch.children.length - 1].visible = !cut;
  const it = v === 'item'; groof.visible = !it; gd.visible = !it; gFront.visible = !it; gdTrim.visible = !it; gFront.scale.y = 1;
  const x = S.xray;
  const xs = x && (v === 'siding' || v === 'rooms'), xp = x && v === 'rooms', xr = x && v === 'roof', xg = x && (v === 'yard' || v === 'deck');
  setM(mats.siding, { opacity: xs ? .16 : 1, depthWrite: !xs });
  setM(mats.paint, { transparent: xp, opacity: xp ? .2 : 1, depthWrite: !xp });
  setM(mats.roof, { opacity: xr ? .18 : 1, depthWrite: !xr });
  setM(mats.grass, { opacity: xg ? .45 : 1, depthWrite: !xg });
  studs.visible = x && v === 'rooms'; sheath.visible = x && v === 'siding'; deck.visible = x && v === 'roof'; under.visible = x && v === 'yard'; footings.visible = x && v === 'deck';
  $('#xray').classList.toggle('on', x); $('#xray').setAttribute('aria-pressed', x);
  applyLook(); highlight();
}
let fly = null;
function flyTo(c, d = 1.1) { const t1 = new THREE.Vector3(...c[1]); const off = new THREE.Vector3(...c[0]).sub(t1); off.multiplyScalar(camera.aspect > 1.2 ? 1.22 : clamp(.72 / Math.max(.3, camera.aspect), 1, 1.75)); fly = { p0: camera.position.clone(), t0: controls.target.clone(), p1: t1.clone().add(off), t1, s: performance.now(), d: d * 1000 }; }
const VO = { cur: null, tgt: 0 };
function viewOffTarget() { const R = $('#map').getBoundingClientRect().height; const top = $('.hd').offsetHeight - 6; const sh = sheetState === 'peek' ? sheetH('peek') : Math.min(sheetH('half'), R * .62); return (sh - top) / 2; }
function applyVO(v) { const r = $('#map').getBoundingClientRect(); camera.setViewOffset(r.width, r.height, 0, v, r.width, r.height); camera.updateProjectionMatrix(); }
function resize() {
  const r = $('#map').getBoundingClientRect(); if (!r.width) return; renderer.setSize(r.width, r.height, false); camera.aspect = r.width / r.height; camera.fov = r.width < r.height ? 46 : 36;
  VO.tgt = viewOffTarget(); if (VO.cur == null) VO.cur = VO.tgt; applyVO(VO.cur);
  document.documentElement.style.setProperty('--shpx', $('#sheet').offsetHeight + 'px');
}
addEventListener('resize', () => { setSheet(sheetState, true); resize(); }); new ResizeObserver(resize).observe($('#sheet')); new ResizeObserver(resize).observe($('.hd'));

/* ======================================================================
   bottom card: peek · half · full (drag or tap the handle) — same on every screen
   ====================================================================== */
let sheetState = 'half';
function sheetH(st) { const R = $('#reno').getBoundingClientRect().height; const peek = $('#grabzone').offsetHeight + $('#shead').offsetHeight + $('#stepper').offsetHeight + ($('#areachip').classList.contains('on') ? $('#areachip').offsetHeight : 0) + ($('#four').style.display === 'none' ? 0 : $('#four').offsetHeight) + 6; return st === 'peek' ? peek : st === 'full' ? R : Math.round(R * (R > 760 ? .46 : .5)); }
function setSheet(st, quiet) { sheetState = st; $('#sheet').style.setProperty('--sh', sheetH(st) + 'px'); $('#sheet').dataset.st = st; $('#ghostnote').style.visibility = st === 'full' ? 'hidden' : ''; $('#grabzone').setAttribute('aria-label', st === 'full' ? 'Show the map' : 'Make the card bigger'); VO.tgt = viewOffTarget(); if (st === 'full') arm(); if (!quiet) buzz(6); }
(() => { const sh = $('#sheet'); let st = null;
  const begin = (e, lazy) => { if (e.button > 0) return; if (!lazy && e.target.closest('button')) return; if (lazy && (sheetState === 'full' || e.target.closest('input,select,textarea,.ba'))) return; st = { y0: e.clientY, h0: sh.offsetHeight, t0: performance.now(), moved: false, lazy, id: e.pointerId, el: e.currentTarget, cap: false }; if (!lazy) { st.cap = true; sh.classList.add('drag'); e.currentTarget.setPointerCapture(e.pointerId); } };
  const move = e => { if (!st) return; const dy = e.clientY - st.y0; if (Math.abs(dy) > 6) st.moved = true; if (!st.moved) return; if (!st.cap) { st.cap = true; sh.classList.add('drag'); try { st.el.setPointerCapture(st.id); } catch (x) {} } sh.style.setProperty('--sh', clamp(st.h0 - dy, sheetH('peek'), sheetH('full')) + 'px'); };
  const end = e => { if (!st) return; const s = st; st = null; sh.classList.remove('drag');
    if (!s.moved) { if (s.lazy) return; if (s.el.id === 'grabzone') setSheet(sheetState === 'peek' ? 'half' : sheetState === 'half' ? 'full' : 'half'); else if (sheetState === 'peek') setSheet('half'); return; }
    if (s.lazy) { const kill = ev => { ev.stopPropagation(); ev.preventDefault(); }; addEventListener('click', kill, { capture: true, once: true }); setTimeout(() => removeEventListener('click', kill, { capture: true }), 60); }
    const h = sh.offsetHeight, v = (e.clientY - s.y0) / Math.max(1, performance.now() - s.t0); let best = ['peek', 'half', 'full'].map(x => [x, Math.abs(sheetH(x) - h)]).sort((a, b) => a[1] - b[1])[0][0]; if (v > .5) best = sheetState === 'full' ? 'half' : 'peek'; if (v < -.5) best = sheetState === 'peek' ? 'half' : 'full'; setSheet(best); };
  [['grabzone', 0], ['shead', 0], ['stepper', 0], ['four', 1], ['areachip', 1], ['sbody', 1]].forEach(([id, lazy]) => { const el = $('#' + id); el.addEventListener('pointerdown', e => begin(e, !!lazy)); el.addEventListener('pointermove', move); el.addEventListener('pointerup', end); el.addEventListener('pointercancel', end); });
  $('#sbody').addEventListener('wheel', e => { if (sheetState !== 'full' && e.deltaY > 4) { e.preventDefault(); setSheet('full'); } }, { passive: false });
  $('#mapB').addEventListener('click', () => setSheet('half'));
})();

/* ======================================================================
   navigation + back (header ←, Escape, phone back)
   ====================================================================== */
let modalOpen = false; const fullStack = []; let armed = false;
function depth() { return (modalOpen ? 1 : 0) + fullStack.length + (POP ? 1 : 0) + (sheetState === 'full' ? 1 : 0) + (S.time ? 1 : 0) + (S.area ? 1 : 0) + (S.p ? 1 : 0); }
function arm() { try { if (depth() > 0 && !armed) { history.pushState({ atlas: 1 }, ''); armed = true; } } catch (e) {} }
function navBack() { if (modalOpen) closeModal(); else if (fullStack.length) popFull(); else if (POP) closePop(); else if (sheetState === 'full') setSheet('half'); else if (S.time) setTime(false); else if (S.area) focusArea(S.area); else if (S.p) go(null); }
addEventListener('popstate', () => { armed = false; navBack(); arm(); });
addEventListener('keydown', e => { if (e.key === 'Escape' && !(e.target.matches && e.target.matches('input,textarea'))) navBack(); });
$('#backB').addEventListener('click', () => { if (S.p) go(null); else navBack(); });

/* ======================================================================
   tapping the 3D
   ====================================================================== */
const ray = new THREE.Raycaster(), ndc = new THREE.Vector2(); let down = null;
canvas.addEventListener('pointerdown', e => { down = [e.clientX, e.clientY]; fly = null; });
canvas.addEventListener('pointerup', e => {
  if (!down || Math.hypot(e.clientX - down[0], e.clientY - down[1]) > 8) return; const r = canvas.getBoundingClientRect(); ndc.set((e.clientX - r.left) / r.width * 2 - 1, -((e.clientY - r.top) / r.height) * 2 + 1); ray.setFromCamera(ndc, camera);
  const p = cur(); const objs = [];
  if (!p) {
    const L = projOf(S.home).sort((a, b) => (a.status === 'done') - (b.status === 'done')); const owner = new Map();
    L.forEach(q => Object.keys(q.areas).forEach(k => (PARTS[k] || []).forEach(m => { if (isShown(m) && !owner.has(m)) { owner.set(m, q.id); objs.push(m); } })));
    const h = ray.intersectObjects(objs); if (h.length) { const id = owner.get(h[0].object); if (POP && POP.id === id) closePop(); else openPop({ kind: 'proj', id, pos: h[0].point.toArray() }); } else closePop(); return;
  }
  Object.keys(p.areas).forEach(k => (PARTS[k] || []).forEach(m => { if (isShown(m)) { m.userData.ak = k; objs.push(m); } })); const h = ray.intersectObjects(objs);
  if (h.length) focusArea(h[0].object.userData.ak);
});
function focusArea(k) {
  const p = cur(); if (!p) return; POP = null; popEl.classList.remove('on');
  S.area = S.area === k ? null : k;
  buzz(8); flyTo(S.area ? (AREACAM[S.area] || CAM[viewOf(p)]) : CAM[viewOf(p)]); buildTags(); highlight(); render(); if (S.area) arm();
}
function go(id, tab = 'needs') {
  const q = id ? P(id) : null; if (q && q.home !== S.home) S.home = q.home; if (q && q.status === 'done' && tab === 'needs') tab = 'record';
  POP = null; popEl.classList.remove('on'); if (id && S.time) { S.time = false; S.year = null; buildTime(); }
  S.p = id; S.area = null; S.tab = tab; S.xray = tab === 'record'; buzz(10); applyView(); flyTo(CAM[viewOf(cur())], 1.3); buildTags(); while (fullStack.length) popFull(true); render(); if (sheetState === 'full' || sheetState === 'peek') setSheet('half', true); else setSheet('half', true); arm();
}
function goHome(h) { S.home = h; save(); if (S.time) { buildTime(); } go(null); }
$('#xray').addEventListener('click', () => { if (!cur()) { toast('Open a project first — x-ray shows what’s hidden for that job'); return; } S.xray = !S.xray; S.tab = S.xray ? 'record' : (S.tab === 'record' ? 'needs' : S.tab); applyView(); render(); buzz(10); });
$('#timeB').addEventListener('click', () => setTime(!S.time));
$('#ghostnote').addEventListener('click', () => { if (S.xray && cur()) $('#xray').click(); });
$('#titleSw').addEventListener('click', () => openSwitcher());
$('#detB').addEventListener('click', () => openDetails());
/* ======================================================================
   money
   ====================================================================== */
function moneyOf(p) { const cost = p.lines.filter(l => l.v > 0).reduce((s, l) => s + l.v, 0); const covered = -p.lines.filter(l => l.v < 0).reduce((s, l) => s + l.v, 0); return { cost, covered, you: cost - covered, left: p.budget - cost, budget: p.budget }; }
const KIND = { quote: 'QUOTE', paid: 'PAID', change: 'CHANGE', insurance: 'COVERED', cost: 'COST' };
function parseCost(t) { const m = t.match(/\$\s*([\d,]+(?:\.\d+)?)\s*(k\b)?|([\d,]{3,}(?:\.\d+)?)\s*(dollars|bucks)?/i); if (!m) return null; const num = m[1] || m[3]; let v = parseFloat(num.replace(/,/g, '')) * (m[2] ? 1000 : 1); if (!v) return null; let kind = /change/i.test(t) ? 'change' : /paid|deposit|payment|receipt/i.test(t) ? 'paid' : /quote|estimate|bid/i.test(t) ? 'quote' : /rebate|insurance|refund|credit/i.test(t) ? 'insurance' : 'cost'; if (kind === 'insurance') v = -v; let l = t.replace(m[0], ' ').replace(/\b(change order|change|paid|deposit|payment|receipt|quote|estimate|bid|rebate|refund|credit|for)\b/ig, ' ').replace(/\s+/g, ' ').trim(); const wm = l.match(/(?:,|\bto|\bfrom|\bby|\s-)\s*([A-Z][\w’']+)\s*$/); const who = wm ? wm[1] : ''; if (wm) l = l.slice(0, wm.index); l = l.replace(/[,\-–:]+\s*$/, '').trim(); return { l: l ? l[0].toUpperCase() + l.slice(1) : 'Cost', v, kind, who }; }


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
    if (S.time) S.htab = 'years'; const ht = S.htab || 'week'; const ev = events(S.home);
    $('#four').innerHTML = [['week', 'Needs you', open ? `${open} open` : 'All clear', open], ['all', 'Projects', `${L.length} total`, 0], ['years', 'Over the years', `${ev.length} events`, 0]].map(([k, l, v, c]) => `<button class="${ht === k ? 'on' : ''}" data-hk="${k}" aria-pressed="${ht === k}"><b>${l}</b><span class="${c ? 'am' : ''}">${v}</span></button>`).join('');
    $('#four').querySelectorAll('[data-hk]').forEach(x => x.addEventListener('click', () => { S.htab = x.dataset.hk; if (S.time && S.htab !== 'years') setTime(false); else render(); $('#sbody').scrollTop = 0; if (sheetState === 'peek') setSheet('half'); }));
    const ask = `<div class="ask2"><button id="askB">✦ Ask your house — “what colour is the trim?”</button></div>`;
    if (!L.length) B.innerHTML = ask + `<div class="empty"><b>Nothing here yet</b>Start a renovation, or log one you’ve already done.<div class="row" style="justify-content:center"><button class="btn" data-new="planned">＋ New renovation</button><button class="btn g" data-new="done">Log a past one</button></div></div>`;
    else if (ht === 'week') B.innerHTML = ask + weekHTML(L);
    else if (ht === 'all') B.innerHTML = ask + `<div class="lh"><div class="k">${L.length} PROJECT${L.length === 1 ? '' : 'S'} AT ${esc(H0.name.toUpperCase())}</div><button class="addb" data-newm="1">＋ New</button></div>${newMenuHTML()}` + groupsHTML(L);
    else B.innerHTML = (S.time ? '' : `<div class="row" style="margin:0 0 10px"><button class="btn" id="tmOn" style="flex:1">🕓 Show it on the house</button></div>`) + timeListHTML();
    bindList(B); bindNewMenu(B); const ab = $('#askB'); if (ab) ab.addEventListener('click', () => openSwitcher(true));
    const to = $('#tmOn'); if (to) to.addEventListener('click', () => setTime(true));
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
  const ro = $('#reopen'); if (ro) ro.addEventListener('click', () => { const was = { s: p.status, h: p.here }; p.status = 'active'; p.here = Math.max(0, p.stops.length - 1); p.where = `${p.stops[p.here]} · reopened`; changed(`${p.name} reopened`, () => { p.status = was.s; p.here = was.h; }); });
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
    const adv = () => { const was = { h: p.here, s: p.status, w: p.where, y: p.year }; p.here = i + 1; if (p.here >= p.stops.length) { p.status = 'done'; p.year = TODAY; p.where = 'finished just now'; } else { p.status = 'active'; p.where = `${p.stops[p.here]} · started today`; } closeModal(); buzz([20, 30, 50]); changed(p.status === 'done' ? `${p.name} finished · kept as a record` : `On to ${p.stops[p.here]}`, () => { p.here = was.h; p.status = was.s; p.where = was.w; p.year = was.y; }); setTimeout(() => afterStep(p), 260); };
    const d = $('#stDone'); if (d) d.addEventListener('click', () => { if (open) { $('#mcard').querySelector('.row').innerHTML = `<p style="margin:0 0 8px;font-size:14px;width:100%">${open} thing${open > 1 ? 's' : ''} still need${open > 1 ? '' : 's'} you. Move on anyway? They stay on the list.</p><button class="btn" id="stYes">Move on</button><button class="btn g" id="cx2">Not yet</button>`; $('#stYes').addEventListener('click', adv); $('#cx2').addEventListener('click', closeModal); } else adv(); });
    const bk = $('#stBack'); if (bk) bk.addEventListener('click', () => { const was = { h: p.here, w: p.where }; p.here = i; p.where = `${s} · back to this step`; closeModal(); changed(`Back to ${s}`, () => { p.here = was.h; p.where = was.w; }); });
    const j = $('#stJump'); if (j) j.addEventListener('click', () => { const was = { h: p.here, s: p.status, w: p.where }; p.here = i; p.status = 'active'; p.where = `${s} · skipped ahead`; closeModal(); changed(`Skipped ahead to ${s}`, () => { p.here = was.h; p.status = was.s; p.where = was.w; }); setTimeout(() => afterStep(p), 260); });
    $('#stRen').addEventListener('click', () => { closeModal(); openDetails('steps'); }); $('#cx').addEventListener('click', closeModal); });
}
function afterStep(p) {
  if (p.status === 'done') { careSetup(p); return; }
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
    $('#swHL').querySelector('[data-addh]').addEventListener('click', addHome);
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
function findPro(p, redraw) {
  const kind = /roof/i.test(p.name) ? 'roofer' : p.place === 'Yard' ? 'landscaper' : /heater|plumb/i.test(p.name) ? 'plumber' : /siding/i.test(p.name) ? 'siding installer' : 'contractor';
  const res = [[`Summit ${kind === 'roofer' ? 'Roofing' : 'Home Pros'}`, 4.8, 212, true], ['Front Range Exteriors', 4.6, 98, true], ['Peak Home Pros', 4.2, 41, false]];
  modal(`<h3>Find a local ${kind}</h3><p class="mini" style="margin:0">Atlas searches only when you ask, and shows where each result came from. These are sample results.</p><div class="card" style="margin-top:10px">${res.map(([n, r, c, ok], i) => `<div class="person"><span class="av">${n[0]}</span><div style="flex:1"><b>${n}</b><small>${r} ★ · ${c} reviews · ${ok ? 'license active on the state lookup' : 'license expired — skip'}</small></div>${ok ? `<button class="btn g" data-add="${i}" style="height:36px">Add</button>` : ''}</div>`).join('')}<p class="src2">Sources: review sites and your state’s contractor license lookup.</p></div><div class="row"><button class="btn g" id="cx">Close</button></div>`, () => {
    $('#mcard').querySelectorAll('[data-add]').forEach(b => b.addEventListener('click', () => { const [n] = res[+b.dataset.add]; if (!p.people.some(x => x[0] === n)) p.people.push([n, `${kind[0].toUpperCase() + kind.slice(1)} · to get a quote from`]); const nd = { id: 'q' + Date.now(), area: null, kind: 'task', k: 'QUOTE', title: `Ask ${n} for a quote`, p: '' }; p.needs.push(nd); closeModal(); save(); redraw(); render(); buildTags(); toast(`${n} added · to-do: ask for a quote`); }));
    $('#cx').addEventListener('click', closeModal); });
}
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

/* ======================================================================
   photos
   ====================================================================== */
function drawPhoto(d) {
  const c = document.createElement('canvas'); c.width = 640; c.height = 400; const g = c.getContext('2d');
  if (d === 'studs') { g.fillStyle = '#e9dfcf'; g.fillRect(0, 0, 640, 400); g.fillStyle = '#d9b77e'; for (let x = 20; x < 640; x += 90) g.fillRect(x, 0, 22, 400); g.strokeStyle = '#d94a3d'; g.lineWidth = 6; g.beginPath(); g.moveTo(0, 230); g.lineTo(480, 230); g.lineTo(480, 320); g.stroke(); g.strokeStyle = '#2f6fd9'; g.beginPath(); g.moveTo(80, 150); g.lineTo(640, 150); g.stroke(); g.fillStyle = '#9aa3ad'; g.fillRect(560, 120, 18, 280); }
  else if (d === 'sky') { g.fillStyle = '#efe9df'; g.fillRect(0, 0, 640, 400); g.fillStyle = '#cfe6ff'; g.fillRect(200, 100, 240, 180); g.setLineDash([10, 8]); g.strokeStyle = '#1d1830'; g.lineWidth = 3; g.strokeRect(200, 100, 240, 180); g.setLineDash([]); g.fillStyle = '#1d1830'; g.font = 'bold 26px Roboto,sans-serif'; g.fillText('48″ × 24″ between rafters', 170, 330); }
  else if (d === 'hail') { g.fillStyle = '#4b5059'; g.fillRect(0, 0, 640, 400); for (let r = 0; r < 12; r++) for (let k = 0; k < 12; k++) { g.fillStyle = (r + k) % 2 ? '#454a52' : '#50555e'; g.fillRect(k * 56 - (r % 2) * 28, r * 34, 54, 32); } g.fillStyle = 'rgba(210,200,180,.85)'; [[120, 90], [300, 160], [420, 70], [210, 260], [520, 300], [90, 330]].forEach(([x, y]) => { g.beginPath(); g.arc(x, y, 9, 0, 7); g.fill(); }); }
  else if (d === 'bathBefore') { g.fillStyle = '#e8d9a8'; g.fillRect(0, 0, 640, 400); g.fillStyle = '#d9c88f'; for (let x = 0; x < 640; x += 40) for (let y = 0; y < 250; y += 40) g.fillRect(x + 2, y + 2, 36, 36); g.fillStyle = '#c9a3a8'; g.fillRect(70, 230, 260, 140); g.fillStyle = '#b48e93'; g.fillRect(70, 230, 260, 14); g.fillStyle = '#f4f1ea'; g.beginPath(); g.ellipse(200, 236, 70, 16, 0, 0, 7); g.fill(); g.fillStyle = '#9e8f7a'; g.fillRect(0, 370, 640, 30); g.fillStyle = '#d7d2c6'; g.fillRect(420, 150, 150, 220); g.fillStyle = '#eceae2'; g.fillRect(430, 160, 130, 200); g.fillStyle = '#ffffffaa'; g.fillRect(110, 40, 180, 150); g.strokeStyle = '#bba86e'; g.lineWidth = 6; g.strokeRect(110, 40, 180, 150); }
  else if (d === 'bathAfter') { g.fillStyle = '#f2efe8'; g.fillRect(0, 0, 640, 400); g.fillStyle = '#ffffff'; for (let x = 0; x < 640; x += 64) for (let y = 120; y < 260; y += 22) g.fillRect(x + ((y / 22) % 2) * 32 + 1, y + 1, 62, 20); g.fillStyle = '#a77b52'; g.fillRect(60, 230, 280, 130); g.fillStyle = '#8f6741'; [100, 200, 300].forEach(x => g.fillRect(x - 1, 240, 2, 110)); g.fillStyle = '#e9e6df'; g.fillRect(52, 220, 296, 14); g.fillStyle = '#fff'; g.beginPath(); g.ellipse(200, 226, 60, 10, 0, 0, 7); g.fill(); g.fillStyle = '#2b2b2b'; g.fillRect(196, 186, 8, 34); g.fillStyle = '#dedad1'; for (let x = 0; x < 640; x += 26) for (let y = 360; y < 400; y += 15) { g.beginPath(); for (let k = 0; k < 6; k++) { const a = k * Math.PI / 3; g.lineTo(x + ((y / 15) % 2) * 13 + 7 * Math.cos(a), y + 7 * Math.sin(a)); } g.fill(); } g.fillStyle = '#cfd8dc'; g.beginPath(); g.ellipse(200, 100, 70, 80, 0, 0, 7); g.fill(); g.strokeStyle = '#2b2b2b'; g.lineWidth = 4; g.stroke(); g.fillStyle = '#5b7a4a'; g.beginPath(); g.arc(470, 300, 34, 0, 7); g.fill(); g.fillStyle = '#c9b8a0'; g.fillRect(450, 320, 40, 40); }
  else if (d === 'sprinkler') { g.fillStyle = '#6e5038'; g.fillRect(0, 0, 640, 400); g.fillStyle = '#8fbf6a'; g.fillRect(0, 0, 640, 80); g.strokeStyle = '#2b2b2b'; g.lineWidth = 14; g.beginPath(); g.moveTo(0, 240); g.lineTo(640, 210); g.stroke(); g.fillStyle = '#3a8ad9'; g.fillRect(300, 190, 40, 50); }
  else { g.fillStyle = '#f1f1ee'; g.fillRect(0, 0, 640, 400); g.fillStyle = '#ffd34d'; g.fillRect(150, 60, 340, 280); g.fillStyle = '#1d1830'; g.font = 'bold 28px Roboto,sans-serif'; g.fillText('WATER HEATER', 200, 110); g.font = '22px monospace'; ['MODEL  XG40T06', 'SERIAL 1103A0042', 'MFG    MAR 2011', 'CAP    40 GAL', 'GAS    NATURAL'].forEach((t, i) => g.fillText(t, 190, 160 + i * 34)); }
  return c.toDataURL('image/jpeg', .85);
}
function openPhoto(p, ph) {
  modal(`<h3>${esc(ph.t)}</h3><div class="mini" style="margin:0">${esc(ph.d)}${p.areas[ph.area] ? ' · ' + esc(p.areas[ph.area]) : ''}</div><img class="photo" alt="${esc(ph.t)}" src="${photoSrc(ph)}"><div class="lbl">Caption</div><input class="inp" id="phT" value="${esc(ph.t)}"><div class="row"><button class="btn" id="phOk">Save</button><button class="btn g danger" id="phDel">Delete</button><button class="btn g" id="cx">Close</button></div>`, () => {
    $('#phOk').addEventListener('click', () => { ph.t = $('#phT').value.trim() || ph.t; closeModal(); changed('Caption saved'); });
    $('#phDel').addEventListener('click', () => { const i = p.photos.indexOf(ph); p.photos.splice(i, 1); closeModal(); changed('Photo deleted', () => p.photos.splice(i, 0, ph)); });
    $('#cx').addEventListener('click', closeModal); });
}
let photoCb = null;
function pickPhoto(cb) { photoCb = cb; $('#fileIn').value = ''; $('#fileIn').click(); }
$('#fileIn').addEventListener('change', e => { const f = e.target.files[0]; if (!f || !photoCb) return; const url = URL.createObjectURL(f); const img = new Image(); img.onload = () => { const s = Math.min(1, 900 / Math.max(img.width, img.height)); const c = document.createElement('canvas'); c.width = img.width * s; c.height = img.height * s; c.getContext('2d').drawImage(img, 0, 0, c.width, c.height); URL.revokeObjectURL(url); const cb = photoCb; photoCb = null; cb(c.toDataURL('image/jpeg', .8)); }; img.onerror = () => toast('That file couldn’t be opened as a photo'); img.src = url; });

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

/* ======================================================================
   modal · toast · router · loop
   ====================================================================== */
function modal(html, fn) { $('#mcard').innerHTML = html; if (!modalOpen) { modalOpen = true; arm(); } $('#modal').classList.add('show'); fn && fn(); }
function closeModal() { modalOpen = false; $('#modal').classList.remove('show'); }
$('#modal').addEventListener('click', e => { if (e.target.id === 'modal') closeModal(); });
let tt; function toast(msg, undo) { const t = $('#toast'); const over = $('#full').classList.contains('show') || modalOpen || sheetState === 'full' || !$('#reno').classList.contains('on'); t.style.setProperty('bottom', over ? '20px' : `calc(var(--shpx) + 14px)`, 'important'); t.innerHTML = `<span>${esc(msg)}</span>${undo ? '<button id="un">Undo</button>' : ''}`; t.classList.add('show'); $('#ghostnote').style.opacity = 0; if (undo) $('#un').addEventListener('click', () => { undo(); t.classList.remove('show'); $('#ghostnote').style.opacity = ''; }); clearTimeout(tt); tt = setTimeout(() => { t.classList.remove('show'); $('#ghostnote').style.opacity = ''; }, 4500); }
function route() { const h = location.hash === '#journeys' ? 'ten' : 'reno'; $('#reno').classList.toggle('on', h === 'reno'); $('#ten').classList.toggle('on', h === 'ten'); $('#t1').classList.toggle('on', h === 'reno'); $('#t2').classList.toggle('on', h === 'ten'); if (h === 'reno') { setSheet(sheetState, true); resize(); } }
addEventListener('hashchange', route);
function frame() {
  if (fly) { const k = ease(clamp((performance.now() - fly.s) / fly.d)); camera.position.lerpVectors(fly.p0, fly.p1, k); controls.target.lerpVectors(fly.t0, fly.t1, k); if (k >= 1) fly = null; }
  if (VO.cur != null && Math.abs(VO.cur - VO.tgt) > .5) { VO.cur += (VO.tgt - VO.cur) * .14; applyVO(VO.cur); }
  controls.update(); glowMat.opacity = .55 + Math.sin(performance.now() / 300) * .35;
  placeAll(); renderer.render(scene, camera); requestAnimationFrame(frame);
}


const ICONS = {
  trail: '<path d="M8 44 C 20 40, 14 28, 26 26 S 40 14, 46 8" fill="none" stroke="#6b4bd1" stroke-width="3" stroke-dasharray="2 5" stroke-linecap="round"/><circle cx="8" cy="44" r="4" fill="#2f9a66"/><circle cx="26" cy="26" r="5" fill="#6b4bd1"/><path d="M44 4v10l6-3z" fill="#d9822b"/>',
  tree: '<path d="M26 48V24" stroke="#7b5136" stroke-width="4"/><circle cx="26" cy="18" r="12" fill="#9fc48b"/><circle cx="15" cy="26" r="7" fill="#b5d3a3"/><circle cx="37" cy="26" r="7" fill="#b5d3a3"/><circle cx="20" cy="16" r="2.5" fill="#d9822b"/><circle cx="32" cy="22" r="2.5" fill="#d9822b"/>',
  remember: '<path d="M10 44h32" stroke="#c9b8a0" stroke-width="3"/><path d="M26 44V22" stroke="#7b9e6c" stroke-width="3"/><path d="M26 24c-8-2-10-10-6-14 5 1 7 7 6 14zM26 28c7-4 13-2 14 3-5 3-11 1-14-3z" fill="#a7c79a"/><circle cx="26" cy="14" r="4" fill="#e8a6b4"/>',
  crossroads: '<path d="M6 10 C 18 14, 22 24, 26 30 M46 10 C 34 14, 30 24, 26 30 M26 4 V 30" stroke="#b9b0d0" stroke-width="3" fill="none"/><path d="M26 30V48" stroke="#6b4bd1" stroke-width="4"/><rect x="18" y="28" width="16" height="6" rx="2" fill="#6b4bd1"/>',
  river: '<path d="M4 14 C 16 8, 22 22, 34 16 S 48 12, 50 18" stroke="#9cc3e8" stroke-width="4" fill="none"/><path d="M2 28 C 16 22, 22 38, 36 30 S 50 26, 52 32" stroke="#5b8ddc" stroke-width="6" fill="none"/><path d="M4 44 C 18 38, 24 50, 38 44" stroke="#2f5fb0" stroke-width="8" fill="none"/>',
  orbit: '<circle cx="26" cy="26" r="6" fill="#6b4bd1"/><circle cx="26" cy="26" r="13" fill="none" stroke="#c9bdf0" stroke-width="2"/><circle cx="26" cy="26" r="21" fill="none" stroke="#e1d9f6" stroke-width="2"/><circle cx="39" cy="26" r="3.5" fill="#d9822b"/><circle cx="12" cy="13" r="3" fill="#b9b0d0"/>',
  shelf: '<path d="M6 20h40M6 36h40" stroke="#b98a5a" stroke-width="3"/><rect x="10" y="6" width="6" height="14" fill="#6b4bd1"/><rect x="18" y="9" width="5" height="11" fill="#d9822b"/><rect x="25" y="7" width="7" height="13" fill="#9fae8f"/><rect x="12" y="25" width="10" height="11" rx="2" fill="#e8a6b4"/><rect x="26" y="23" width="6" height="13" fill="#5b8ddc"/>',
  voyage: '<path d="M4 40 C 14 36, 24 44, 34 38 S 48 36, 50 38" stroke="#9cc3e8" stroke-width="3" fill="none"/><ellipse cx="12" cy="30" rx="7" ry="3" fill="#cdb98f"/><ellipse cx="38" cy="22" rx="8" ry="3.5" fill="#cdb98f"/><path d="M12 30 C 20 18, 30 30, 38 22" stroke="#6b4bd1" stroke-width="2" stroke-dasharray="3 3" fill="none"/>',
  climb: '<path d="M4 46 L 22 16 L 30 28 L 38 14 L 50 46 Z" fill="#c9c2d9"/><path d="M38 14 l -4 7 h 8 z" fill="#fff"/><path d="M8 44 L 18 34 L 26 36 L 34 22" stroke="#6b4bd1" stroke-width="2.5" fill="none" stroke-dasharray="3 3"/><path d="M38 6v8l5-3z" fill="#d9822b"/>',
  countdown: '<circle cx="26" cy="26" r="20" fill="none" stroke="#efe9ff" stroke-width="6"/><path d="M26 6 A 20 20 0 1 1 9 36" fill="none" stroke="#6b4bd1" stroke-width="6" stroke-linecap="round"/><text x="26" y="31" text-anchor="middle" font-size="13" font-weight="700" fill="#1d1830" font-family="Roboto,sans-serif">12</text>'
};
const TEN = [
  { k: 'trail', n: 'Trail', f: 'Projects with an end · renovation, a move, a job search', job: 'Keep a big project on budget and on time, and remember what everyone agreed.', a: ['Which stop you’re at, what’s next and when', 'Decisions with deadlines, ranked by what they hold up', 'Money left, with change orders landing as they happen', 'What was promised, in writing, with where it came from'], cap: 'Photos, shared texts and emails, PDF quotes, voice notes', one: 'The decision queue, and “make it official” for anything only said out loud', last: 'A record of the house: x-ray walls, paint colours, warranties' },
  { k: 'tree', n: 'Living Tree', f: 'A person or life · grandchild, child, pet', job: 'Know someone well as they grow — and never lose the photo, the size or the idea.', a: ['Their age and season of life', 'Birthdays coming up, and gift ideas you saved', 'Nothing to lose here — just time', 'Sizes, favourites, allergies, doctors, school'], cap: 'Photos of them from your gallery (on device), messages that mention them', one: '“Ask the tree”: what size shoes is she in now? What did she love last year?', last: 'A year in their life, made into a book' },
  { k: 'remember', n: 'Remember', f: 'Someone who died', job: 'Keep the stories before they fade, and get through the hard dates together.', a: ['Gentle — no progress at all', 'An anniversary coming, with a quiet heads-up', 'Stories only you or family still hold', 'What’s been gathered: photos, words, recordings'], cap: 'The obituary (fetched only when asked), your photos and old messages, voice recordings of family stories', one: 'Ask family for one memory each; their answers plant into the room, garden or letter', last: 'A keepsake the family can open on any day' },
  { k: 'crossroads', n: 'Crossroads', f: 'A decision · a purchase, a job offer, a contractor', job: 'Choose well, compare like with like, and remember why you chose.', a: ['How many options are still in', 'Missing facts to decide (a price, a review, a spec)', 'Price and fit against what matters to you', 'What you ruled out, and why'], cap: 'Links, screenshots, quotes and reviews you share; the AI fetches only what you ask for', one: 'Side by side on your own criteria, with gaps evened out — like adding the missing electrical', last: 'The verdict and the reasons, for when you wonder later' },
  { k: 'river', n: 'River', f: 'Learning · a skill, an instrument, a language', job: 'Keep going through the plateau — the point where most people quit.', a: ['How deep you are, and recent practice', 'The next small thing to try', 'Nothing lost but time — no guilt streaks', 'What you’ve learned and where from'], cap: 'Quick recordings or videos of you practising, links and lessons you save', one: 'Then vs now: play your first recording beside this week’s, so a plateau doesn’t feel like failure', last: 'A record of how you got good' },
  { k: 'orbit', n: 'Orbit', f: 'Recurring life admin · taxes, car registration, physicals', job: 'Never pay a late fee for something you do every year.', a: ['Days until it’s due', 'What to gather, with last year’s files ready', 'The fee or fine if it slips', 'What you paid and filed last time'], cap: 'Renewal emails and letters (photo), receipts — dates and amounts read automatically', one: 'Last time vs this time, with a reminder weeks ahead and the documents already attached', last: 'Every year’s paperwork, one tap away' },
  { k: 'shelf', n: 'Shelf', f: 'Things to keep · recipes, books, a wishlist, gift ideas', job: 'Actually use what you save, instead of building a graveyard of saves.', a: ['What’s on the shelf, newest first', 'The right item at the right time — a gift idea before the birthday', 'Nothing urgent', 'What you tried, rated and kept'], cap: 'Share from any app; the recipe or article itself is kept, not just a link that can vanish', one: 'Resurfacing: Atlas brings an item back when it fits — and “make this” turns it into a plan', last: 'Your own cookbook, reading list, gift list' },
  { k: 'voyage', n: 'Voyage', f: 'A trip · before, during, after', job: 'Have every booking in one place, today’s plan in hand, and an album after.', a: ['Before, during or after — and which day', 'The next check-in, booking or document', 'Dates and what it all costs', 'Confirmations, saved offline'], cap: 'Booking emails you share, tickets, photos (placed on the right day and place)', one: 'The “today” card on the trip: where, when, the ticket — without opening five apps', last: 'An album by day and place, built from your photos' },
  { k: 'climb', n: 'Climb', f: 'A measured goal · savings, fitness, recovery', job: 'See if you’re on pace, and what to change to get there.', a: ['Altitude: where you are vs the target', 'Off pace? By how much, and since when', 'When you’ll arrive at the current pace', 'What helped on the way up'], cap: 'Quick numbers, screenshots from your bank or fitness app read on the phone', one: 'Drag the summit date and see the pace it needs — a real what-if', last: 'Proof of how you did it' },
  { k: 'countdown', n: 'Countdown', f: 'A fixed day · wedding, party, a service', job: 'Get to the day with the budget, the guests and the vendors handled.', a: ['Days left, and what each lane still needs', 'Decisions and deadlines by lane — guests, vendors, run of show', 'Budget and RSVPs', 'Booked vendors and their contracts'], cap: 'Vendor emails and contracts, RSVP replies, the venue’s PDF', one: 'Day-of mode: the run of show, who does what and when, shareable with helpers', last: 'An album of the day' }
];
function renderTen() {
  $('#tenwrap').innerHTML = `
  <h2>Every journey answers four questions at a glance</h2>
  <p class="lede">People don’t keep using apps that feel like a second job, or games about fake work. So each journey has one real job, fills itself from what you already have, and gives the same four answers — in its own shape.</p>
  <div class="answers"><div><b>Where am I?</b><span>The shape shows it</span></div><div><b>What needs me?</b><span>Decisions and dates, ranked</span></div><div><b>What’s at stake?</b><span>Money, time, or what could be lost</span></div><div><b>What’s settled?</b><span>Agreed, chosen, kept — with its source</span></div></div>
  <div class="rules"><h3>Rules every journey follows</h3><ol><li>No typing to keep it alive: photos, shares, voice and email fill it.</li><li>Every tap is real work: decide, confirm, send, or look something up.</li><li>Nothing is sent for you. Atlas drafts; you send.</li><li>It stays useful after it ends — a record, an album, an answer.</li><li>No streaks, badges or guilt.</li></ol></div>
  ${TEN.map(j => `<article class="j"><div class="jh"><svg viewBox="0 0 52 52" aria-hidden="true">${ICONS[j.k]}</svg><div><b>${esc(j.n)}</b><small>${esc(j.f)}</small></div></div>
    <p class="job"><strong>Real job:</strong> ${esc(j.job)}</p>
    <div class="g4"><div><i>WHERE AM I</i>${esc(j.a[0])}</div><div><i>WHAT NEEDS ME</i>${esc(j.a[1])}</div><div><i>WHAT’S AT STAKE</i>${esc(j.a[2])}</div><div><i>WHAT’S SETTLED</i>${esc(j.a[3])}</div></div>
    <dl class="jl"><dt>Fills itself from</dt><dd>${esc(j.cap)}</dd><dt>The one interaction</dt><dd>${esc(j.one)}</dd><dt>Afterwards</dt><dd>${esc(j.last)}</dd></dl></article>`).join('')}
  <div class="shared"><h3>Build one engine, ten shapes</h3><p>Most of this is shared. Build it once and each shape only adds its picture and its one interaction.</p><div class="chips"><span>Capture with no typing</span><span>Auto-file to place and stage</span><span>“Needs you” queue</span><span>Agreed, with sources</span><span>Money &amp; dates</span><span>Resurfacing at the right time</span><span>Share with family</span><span>Record afterwards</span></div></div>
  <p class="src">Research behind this: Houzz &amp; Home Study 2026 (budgets, overruns), Houzz homeowner survey (timelines, communication), Houzz couples survey (selections), change-order dispute guidance, before-drywall photo practice, app abandonment and gamification research, Zola First Look Report (wedding pain points), forgotten-renewal surveys, save-for-later research, learning-plateau research. Samples on the renovation tab are marked.</p>`;
}


/* =================== start =================== */
load(); PROJ.forEach(p => { p.notes = p.notes || []; p.people = p.people || []; p.waiting = p.waiting || []; p.care = p.care || []; p.lines = p.lines || []; });
renderTen(); route(); previewAll(); applyView(); buildTags(); render(); setSheet('half', true);
camera.position.set(26, 24, 32); flyTo(CAM[modelOf(S.home)], 1.6);
loadAll().then(furnish).catch(e => console.error(e));
frame();
}
