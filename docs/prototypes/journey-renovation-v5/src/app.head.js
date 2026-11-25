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
