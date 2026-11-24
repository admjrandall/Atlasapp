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
  main: { name: 'Main house', icon: '🏡', kind: 'House · 3 bed · built 1962 (sample)' },
  cabin: { name: 'Lake cabin', icon: '🌲', kind: 'A-frame · weekends (sample)' },
  condo: { name: 'Rental condo', icon: '🏢', kind: 'Unit 4B · rented out (sample)' }
};
const THINGS = {
  kitchen: { home: 'main', name: 'Kitchen', place: 'Inside', hist: [{ y: 1962, t: 'Original kitchen', s: 'Built with the house' }] },
  roof: { home: 'main', name: 'Roof', place: 'Outside', hist: [{ y: 2009, t: 'Asphalt 3-tab, grey', s: 'Previous owner · from your inspection report' }] },
  siding: { home: 'main', name: 'Siding', place: 'Outside', hist: [{ y: 1988, t: 'Wood lap siding, painted', s: 'Inspection report' }] },
  yard: { home: 'main', name: 'Back yard', place: 'Yard', hist: [{ y: 2019, t: 'Cedar fence, back yard', s: 'Your receipt · $3,900 (sample)' }] },
  wh: { home: 'main', name: 'Water heater', place: 'Systems', hist: [{ y: 2011, t: '40-gal gas tank', s: 'Read from the label' }] },
  bath: { home: 'main', name: 'Bathroom', place: 'Inside', hist: [{ y: 2022, t: 'Vanity, toilet, tile floor', s: 'Bathroom refresh · $9,400 (sample)' }] },
  deck: { home: 'cabin', name: 'Deck', place: 'Outside', hist: [{ y: 1998, t: 'Pressure-treated pine', s: 'Came with the cabin' }] },
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
  { id: 'bath22', home: 'main', thing: 'bath', name: 'Bathroom refresh', icon: '🛁', status: 'done', year: 2022, place: 'Inside', view: 'rooms', record: 'Walls', stops: ['Plan', 'Buy', 'Install', 'Done'], here: 4, where: 'Finished May 2022',
    areas: { bath: 'Bathroom' }, details: [F('Vanity', '36″ white oak, quartz top', 'text'), F('Floor tile', 'Hex marble, 2″', 'text'), F('Paint', 'Alabaster · SW 7008', 'colour', { pal: 'paint' }), F('Fan', 'Panasonic WhisperCeiling (sample)', 'text')],
    needs: [], waiting: [], budget: 10000, lines: [{ l: 'Total paid', v: 9400, kind: 'paid', who: 'Various', sample: true }], watch: ['Finished', 'Under budget by $600 (sample).'], agreed: [{ area: 'bath', ok: true, t: 'Vanity warranty 5 years', s: 'Receipt' }], people: [], photos: [], coming: ['CARE', 'Re-seal the grout', 'Every 2 years — next in May 2026 (sample).'] },
  { id: 'fence19', home: 'main', thing: 'yard', name: 'Back fence', icon: '🪵', status: 'done', year: 2019, place: 'Yard', view: 'yard', record: 'Underground', stops: ['Quote', 'Build', 'Done'], here: 3, where: 'Finished Aug 2019',
    areas: { lawn: 'Back yard' }, details: [F('Material', 'Cedar, 6 ft', 'text'), F('Length', '96 ft', 'text'), F('Installer', 'Front Range Fence (sample)', 'text')], needs: [], waiting: [], budget: 4000, lines: [{ l: 'Total paid', v: 3900, kind: 'paid', who: 'Front Range Fence', sample: true }], watch: ['Finished', 'Stain every 3–4 years.'], agreed: [], people: [], photos: [], coming: ['CARE', 'Stain the fence', 'Next due 2026 (sample).'] },
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

const S = { home: 'main', p: null, area: null, tab: 'needs', xray: false, picked: {}, people: [{ n: 'You', r: 'Owner', s: 'Everything' }, { n: 'Your partner', r: 'Can edit', s: 'All homes' }] };
const P = id => PROJ.find(x => x.id === id);
const cur = () => S.p ? P(S.p) : null;
const projOf = h => PROJ.filter(x => x.home === h);

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
function uv(m, rx, ry) { const t = m.material.map; if (!t) return; m.material = m.material.clone(); m.material.map = t.clone(); m.material.map.needsUpdate = true; m.material.map.repeat.set(rx, ry); }

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
const gd = box(2.6, 2.0, .06, mats.gdoor, 5.8, 1.15, 2.0, gar); box(2.85, .1, .1, mats.trim, 5.8, 2.2, 2.0, gar);
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

/* ---- x-ray extras for rooms and siding ---- */
const studs = grp(house, 0, FY, 0); studs.visible = false;
const studM = new THREE.MeshBasicMaterial({ color: 0xd9b77e });
for (let x = -3.85; x <= 3.9; x += .4) box(.05, H, .08, studM, x, H / 2, -3.0, studs);
box(5, .035, .05, new THREE.MeshBasicMaterial({ color: 0xd94a3d }), 1.4, 1.0, -2.98, studs); box(.035, .8, .05, new THREE.MeshBasicMaterial({ color: 0xd94a3d }), 3.0, .6, -2.98, studs); box(3.6, .035, .05, new THREE.MeshBasicMaterial({ color: 0x2f6fd9 }), 2.0, 1.25, -2.98, studs); box(.07, H, .07, new THREE.MeshBasicMaterial({ color: 0x9aa3ad }), 3.4, H / 2, -2.96, studs);
const sheath = grp(house, 0, FY, 0); sheath.visible = false;
[[8.0, .06, 0, 2.86], [8.0, .06, 0, -2.86], [.06, 5.8, -3.86, 0]].forEach(([w, d, x, z]) => bbox(w, H, d, mats.osb, x, 0, z, sheath));
const footings = grp(cabinRoot); footings.visible = false; for (let x = -4.2; x <= 4.2; x += 2.1) for (const z of [2.4, 6.0]) mesh(new THREE.CylinderGeometry(.25, .3, 1.2, 10), mats.conc, x, -.5, z, footings);

/* ---- models ---- */
const GLB = {}; const loader = new GLTFLoader();
function b64(s) { const bin = atob(s); const u = new Uint8Array(bin.length); for (let i = 0; i < bin.length; i++) u[i] = bin.charCodeAt(i); return u.buffer; }
async function loadAll() { await Promise.all(Object.keys(MODELS).map(k => new Promise((res, rej) => loader.parse(b64(MODELS[k]), './', g => { GLB[k] = g; res(); }, rej)))); }
function inst(name, size, by = 'h', parent) { const o = GLB[name].scene.clone(true); o.updateMatrixWorld(true); const b = new THREE.Box3().setFromObject(o); const d = b.getSize(new THREE.Vector3()); o.scale.multiplyScalar(size / (by === 'h' ? d.y : d.x)); o.updateMatrixWorld(true); const b2 = new THREE.Box3().setFromObject(o); const c = b2.getCenter(new THREE.Vector3()); o.position.set(-c.x, -b2.min.y, -c.z); o.traverse(m => { if (m.isMesh) m.castShadow = m.receiveShadow = true; }); const w = new THREE.Group(); w.add(o); (parent || mainRoot).add(w); return w; }
function put(n, s, x, y, z, ry = 0, by = 'h', parent) { const o = inst(n, s, by, parent); o.position.set(x, y, z); o.rotation.y = ry; return o; }
function furnish() {
  const Fu = grp(house, 0, FY + .04, 0);
  put('rug_rectangle_stripes_A', 2.2, -2.0, 0, 1.3, 0, 'w', Fu); put('couch_pillows', .8, -2.0, 0, .35, 0, 'h', Fu); put('table_low', .34, -2.0, 0, 1.3, 0, 'h', Fu); put('armchair', .78, -3.2, 0, 2.2, 2.3, 'h', Fu); put('lamp_standing', 1.4, -3.5, 0, .3, 0, 'h', Fu); put('shelf_B_large_decorated', .5, -.9, 0, 2.6, Math.PI, 'h', Fu);
  put('bed_double_A', .7, -2.0, 0, -1.7, 0, 'h', Fu); put('cabinet_small', .6, -.6, 0, -2.6, 0, 'h', Fu); put('cactus_medium_A', .45, .8, 0, 2.3, 0, 'h', Fu);
  [[1.5, -.3], [2.3, -.3]].forEach(([x, z]) => put('chair_A_wood', .85, x, 0, z, Math.PI, 'h', Fu));
  put('car_stationwagon', 1.5, 5.8, 0, 5.2, Math.PI, 'h', mainRoot);
  [[-8.5, 6.5, 'tree_oak', 3.6], [9.2, 6, 'tree_fat', 3.0], [-9, -8.6, 'tree_fat', 3.4], [8.8, -9.5, 'tree_oak', 3.8], [-6.5, -10.3, 'tree_oak', 3.2]].forEach(([x, z, n, h]) => put(n, h, x, 0, z));
  [[-3.4, 3.7], [-1.8, 3.7], [2.6, 3.6], [3.4, 3.6], [-4.6, 2.0], [-4.6, 0]].forEach(([x, z]) => put('bush', .6, x, 0, z));
  beds.forEach(b => { for (let i = -1; i <= 1; i++) put('bush', .55, b.position.x + i * 1.0, .2, b.position.z); });
  for (let x = -10.5; x <= 10.5; x += 1.5) put('fence', 1.5, x, 0, -10.6, 0, 'w');
  for (let z = -9.8; z <= -3.8; z += 1.5) { put('fence', 1.5, -11.2, 0, z, Math.PI / 2, 'w'); put('fence', 1.5, 11.2, 0, z, Math.PI / 2, 'w'); }
  put('bench', .6, 6.5, 0, -6.2, -Math.PI / 2);
  put('bench', .6, -2.5, 1.02, 5.4, 0, 'h', cabinRoot); put('armchair', .7, 2.4, 1.02, 4.0, -.4, 'h', cabinRoot);
  put('couch_pillows', .7, -1, .1, 2.6, Math.PI, 'h', unitG); put('rug_rectangle_stripes_A', 2, -1, .1, 1.2, 0, 'w', unitG); put('bed_double_A', .62, 1.4, .1, -3.2, 0, 'h', unitG); put('table_medium', .7, -1.6, .1, -2.6, 0, 'h', unitG); put('lamp_standing', 1.3, -2.4, .1, 3.8, 0, 'h', unitG);
}

/* ---- tags & pins ---- */
const AREA_TAG = { sFront: [0, 2.6, 3.4], sLeft: [-4.5, 2.6, 0], sBack: [0, 2.6, -3.4], sGarage: [7.9, 2.2, -.5], bath: [2.6, 1.6, 1.8] };
const PROJ_TAG = { kitchen: [2.6, FY + H + 2.3, -.6], roof: [-1.2, FY + H + 2.4, .6], siding: [-4.5, 1.8, 1.8], yard: [0, 1.1, -6.6], heater: [6.75, 3.3, -2.35], deck: [0, 2.4, 4.4], stove: [1.3, 7.0, -2.4], paint: [4, FL * 3 + 2.4, 0] };
const tagsEl = $('#tags'); let tagEls = []; let pinEls = [];
function mk(cls, html, fn) { const el = document.createElement('button'); el.className = cls; el.innerHTML = html; el.addEventListener('click', fn); tagsEl.appendChild(el); return el; }
function buildTags() {
  tagEls.forEach(t => t.el.remove()); pinEls.forEach(t => t.el.remove()); tagEls = []; pinEls = [];
  const p = cur();
  if (!p) { projOf(S.home).filter(q => q.status !== 'done' && PROJ_TAG[q.id]).forEach(q => { const n = q.needs.length; tagEls.push({ el: mk('tag', `<span class="n ${n ? '' : 'z'}">${n || '✓'}</span><span>${q.icon} ${esc(q.name)}</span>`, () => go(q.id)), pos: PROJ_TAG[q.id] }); }); return; }
  Object.entries(p.areas).forEach(([k, name]) => { const n = p.needs.filter(x => x.area === k).length; tagEls.push({ el: mk('tag' + (S.area === k ? ' sel' : ''), `<span class="n ${n ? '' : 'z'}">${n || '✓'}</span><span>${esc(name)}</span>`, () => focusArea(k)), pos: AREA_TAG[k] || TAGPOS[k] || [0, 2, 0] }); });
  p.photos.forEach(ph => pinEls.push({ el: mk('pin', '📷', () => openPhoto(ph)), pos: ph.pos }));
  if (p.id === 'roof') pinEls.push({ el: mk('pin', '!', () => toast('Sample: the roofer flagged a soft spot here — that’s what the per-sheet price covers')), pos: rotPos });
}
const pv = new THREE.Vector3();
function place(el, pos) { pv.set(...pos).project(camera); if (pv.z > 1) { el.style.display = 'none'; return; } const r = canvas.getBoundingClientRect(); el.style.display = ''; el.style.left = ((pv.x + 1) / 2 * r.width) + 'px'; el.style.top = ((1 - pv.y) / 2 * r.height) + 'px'; }
function placeAll() { tagEls.forEach(t => S.xray ? (t.el.style.display = 'none') : place(t.el, t.pos)); pinEls.forEach(t => S.xray ? place(t.el, t.pos) : (t.el.style.display = 'none')); }

/* ---- highlight ---- */
const glow = new THREE.Group(); scene.add(glow); const glowMat = new THREE.LineBasicMaterial({ color: 0xff9a3c, transparent: true, opacity: .9, depthTest: false });
function highlight() { glow.clear(); const p = cur(); if (!p) return; Object.keys(p.areas).forEach(k => { if (S.area && S.area !== k) return; (PARTS[k] || []).forEach(m => { m.updateMatrixWorld(true); const e = new THREE.LineSegments(new THREE.EdgesGeometry(m.geometry, 30), glowMat); e.matrixAutoUpdate = false; e.matrix.copy(m.matrixWorld); e.renderOrder = 5; glow.add(e); }); }); }

/* ---- views ---- */
const CAM = { main: [[16, 15, 21], [1, .5, -1.5]], cabin: [[13, 9, 17], [0, 2.4, 0]], condo: [[19, 15, 21], [1.5, 6, 0]], rooms: [[7.5, 14, 11], [.6, 0, .2]], roof: [[-10, 11, 13], [0, 3.2, 0]], siding: [[-12, 5.5, 12.5], [.5, 1.8, 0]], yard: [[11, 11, -18], [0, 0, -6.5]], item: [[6.2, 3.6, 6.5], [6.6, 1.0, -2.0]], deck: [[9, 6, 14], [0, 1.5, 3]], condoUnit: [[11, 15, 11], [4, 8, 0]] };
const AREACAM = { kitchen: [[6, 9, 7], [2, 0, -1.2]], living: [[-6, 9, 9], [-2, 0, 1.2]], entry: [[3, 8, 9], [.7, 0, 1.7]], bath: [[5, 8, 8], [2.6, 0, 1.8]], front: [[-4, 9, 14], [-.5, 3.4, 1.5]], back: [[-4, 10, -13], [-.5, 3.4, -1.5]], garageRoof: [[12, 9, 6], [5.8, 3, -.5]], chimney: [[-8, 9, -9], [-2.4, 4.4, -1.6]], sFront: [[-3, 3.5, 12], [0, 1.6, 3]], sLeft: [[-12, 3.5, 2], [-4, 1.6, 0]], sBack: [[-3, 4, -12], [0, 1.6, -3]], sGarage: [[13, 3.5, 3], [7.5, 1.4, -.5]], patio: [[6, 7, -15], [0, 0, -5.6]], beds: [[4, 6, -3], [0, 0, -9.6]], drain: [[-3, 6, -14], [-9.8, 0, -6]], lawn: [[11, 6, -11], [4.5, 0, -7.2]], wh: [[6.4, 2.8, 4.5], [6.75, 1.0, -2.35]], deck: [[7, 5, 12], [0, 1, 4]], stairs: [[9, 4, 9], [5.2, .6, 4.6]], stove: [[6, 9, 4], [1.3, 5.2, -2.4]], unit: [[10, 14, 9], [4, 8, 0]] };
function viewOf(p) { if (!p) return S.home; return p.view === 'condo' ? 'condoUnit' : p.view; }
function applyView() {
  mainRoot.visible = S.home === 'main'; cabinRoot.visible = S.home === 'cabin'; condoRoot.visible = S.home === 'condo';
  const p = cur(); const v = p ? p.view : 'home'; const cut = v === 'rooms';
  extWalls.scale.y = inWalls.scale.y = cut ? 1.15 / H : 1;
  roof.visible = !cut; wins.visible = !cut; porch.children[porch.children.length - 1].visible = !cut;
  groof.visible = v !== 'item'; gd.visible = v !== 'item'; gFront.scale.y = v === 'item' ? .35 : 1;
  const x = S.xray;
  mats.siding.opacity = x && (v === 'siding' || v === 'rooms') ? .22 : 1; mats.siding.depthWrite = !(x && (v === 'siding' || v === 'rooms'));
  mats.paint.transparent = x && v === 'rooms'; mats.paint.opacity = x && v === 'rooms' ? .22 : 1; mats.paint.depthWrite = !(x && v === 'rooms'); mats.paint.needsUpdate = true;
  mats.roof.opacity = x && v === 'roof' ? .18 : 1; mats.roof.depthWrite = !(x && v === 'roof');
  mats.grass.opacity = x && (v === 'yard' || v === 'deck') ? .45 : 1; mats.grass.depthWrite = !(x && (v === 'yard' || v === 'deck'));
  studs.visible = x && v === 'rooms'; sheath.visible = x && v === 'siding'; deck.visible = x && v === 'roof'; under.visible = x && v === 'yard'; footings.visible = x && v === 'deck';
  highlight();
}

/* ---- camera ---- */
let fly = null;
function flyTo(c, d = 1.1) { const t1 = new THREE.Vector3(...c[1]); const off = new THREE.Vector3(...c[0]).sub(t1); off.multiplyScalar(clamp(.72 / Math.max(.3, camera.aspect), 1, 1.75)); fly = { p0: camera.position.clone(), t0: controls.target.clone(), p1: t1.clone().add(off), t1, s: performance.now(), d: d * 1000 }; }
function resize() { const r = $('#map').getBoundingClientRect(); if (!r.width) return; renderer.setSize(r.width, r.height, false); camera.aspect = r.width / r.height; camera.fov = r.width < r.height ? 46 : 34; const sh = $('#sheet').offsetHeight, top = $('.hd').offsetHeight - 10; camera.setViewOffset(r.width, r.height, 0, (sh - top) / 2, r.width, r.height); camera.updateProjectionMatrix(); document.documentElement.style.setProperty('--sheetH', sh + 'px'); }
addEventListener('resize', resize); new ResizeObserver(resize).observe($('#sheet')); new ResizeObserver(resize).observe($('.hd'));

/* ---- tapping the 3D ---- */
const ray = new THREE.Raycaster(), ndc = new THREE.Vector2(); let down = null;
canvas.addEventListener('pointerdown', e => { down = [e.clientX, e.clientY]; fly = null; });
canvas.addEventListener('pointerup', e => {
  if (!down || Math.hypot(e.clientX - down[0], e.clientY - down[1]) > 8) return; const r = canvas.getBoundingClientRect(); ndc.set((e.clientX - r.left) / r.width * 2 - 1, -((e.clientY - r.top) / r.height) * 2 + 1); ray.setFromCamera(ndc, camera);
  const p = cur();
  if (!p) { const objs = []; projOf(S.home).filter(q => q.status !== 'done').forEach(q => Object.keys(q.areas).forEach(k => (PARTS[k] || []).forEach(m => { m.userData.proj = q.id; objs.push(m); }))); const h = ray.intersectObjects(objs); if (h.length) go(h[0].object.userData.proj); return; }
  const objs = []; Object.keys(p.areas).forEach(k => (PARTS[k] || []).forEach(m => { m.userData.ak = k; objs.push(m); })); const h = ray.intersectObjects(objs); if (h.length) focusArea(h[0].object.userData.ak); else if (S.area) focusArea(S.area);
});
function focusArea(k) { S.area = S.area === k ? null : k; buzz(8); flyTo(S.area ? (AREACAM[S.area] || CAM[viewOf(cur())]) : CAM[viewOf(cur())]); buildTags(); highlight(); render(); }
const recent = [];
function go(id) { if (id && id !== S.p) { const i = recent.indexOf(id); if (i >= 0) recent.splice(i, 1); recent.unshift(id); } S.p = id; S.area = null; S.tab = 'needs'; S.xray = false; $('#xray').classList.remove('on'); buzz(10); applyView(); flyTo(CAM[viewOf(cur())], 1.3); buildTags(); render(); closeFull(); }
function goHome(h) { S.home = h; recent.length = 0; go(null); }
$('#xray').addEventListener('click', () => { if (!cur()) { toast('Pick a project — x-ray shows what’s hidden for that job'); return; } S.xray = !S.xray; $('#xray').classList.toggle('on', S.xray); S.tab = S.xray ? 'record' : (S.tab === 'record' ? 'needs' : S.tab); applyView(); render(); buzz(10); });

/* ---- money ---- */
function moneyOf(p) { const lines = p.lines; const cost = lines.filter(l => l.v > 0).reduce((s, l) => s + l.v, 0); const covered = -lines.filter(l => l.v < 0).reduce((s, l) => s + l.v, 0); return { cost, covered, you: cost - covered, left: p.budget - cost, budget: p.budget }; }
const KIND = { quote: 'QUOTE', paid: 'PAID', change: 'CHANGE', insurance: 'COVERED', cost: 'COST' };
function parseCost(t) { const m = t.match(/\$?\s*([\d,]+(?:\.\d+)?)\s*(k)?/i); if (!m) return null; let v = parseFloat(m[1].replace(/,/g, '')) * (m[2] ? 1000 : 1); let kind = /change/i.test(t) ? 'change' : /paid|deposit|payment|receipt/i.test(t) ? 'paid' : /quote|estimate|bid/i.test(t) ? 'quote' : /rebate|insurance|refund|credit/i.test(t) ? 'insurance' : 'cost'; if (kind === 'insurance') v = -v; let l = t.replace(m[0], '').replace(/\b(change order|change|paid|deposit|payment|receipt|quote|estimate|bid|rebate|refund|credit)\b/ig, '').replace(/\s+/g, ' ').trim(); const who = (l.match(/\b(?:to|from|by)\s+([A-Z][\w’']+)/) || l.match(/,\s*([A-Z][\w’']+)$/) || [])[1]; l = l.replace(/,?\s*(?:to|from|by)?\s*[A-Z][\w’']+$/, '').replace(/[,\-–]+$/, '').trim(); return { l: l ? l[0].toUpperCase() + l.slice(1) : 'Cost', v, kind, who: who || '' }; }

/* ---- render ---- */
function render() {
  const p = cur(); const H0 = HOMES[S.home];
  $('#homeIc').textContent = H0.icon; $('#homeName').textContent = H0.name;
  const rc = recent.filter(id => id !== S.p && P(id) && P(id).home === S.home).slice(0, 2);
  $('#recent').innerHTML = (p ? `<button data-r="">← Everything</button>` : '') + rc.map(id => `<button data-r="${id}">${P(id).icon} ${esc(P(id).name)}</button>`).join('');
  $('#recent').querySelectorAll('button').forEach(b => b.addEventListener('click', () => go(b.dataset.r || null)));
  if (!p) {
    $('#ptitle').textContent = 'Everything'; const L = projOf(S.home); const nd = L.reduce((s, q) => s + q.needs.length, 0);
    $('#where').textContent = `${L.filter(q => q.status === 'active').length} in progress · ${L.filter(q => q.status === 'planned').length} planned · ${L.filter(q => q.status === 'done').length} done`;
    $('#trail').innerHTML = ''; $('#traill').innerHTML = '';
    $('#scope').textContent = H0.name; $('#scopeSub').textContent = H0.kind; $('#detB').style.display = 'none'; $('#four').style.display = 'none';
    $('#sbody').innerHTML = groupsHTML(L, true) + `<button class="btn" id="newR" style="width:100%;margin-top:6px">＋ New renovation</button>`;
    bindList($('#sbody')); $('#newR').addEventListener('click', openCatalog); return;
  }
  $('#detB').style.display = ''; $('#four').style.display = '';
  $('#ptitle').textContent = p.name; $('#where').textContent = p.status === 'done' ? `${p.year} · ${p.where}` : `Stop ${Math.min(p.here + 1, p.stops.length)} of ${p.stops.length} · ${p.where}`;
  $('#trail').innerHTML = p.stops.map((s, i) => `${i ? `<b class="${i <= p.here ? 'done' : ''}"></b>` : ''}<i class="${i < p.here ? 'done' : i === p.here ? 'here' : ''}"></i>`).join('');
  $('#traill').innerHTML = p.stops.map(s => `<span>${esc(s)}</span>`).join('');
  const m = moneyOf(p); const inA = x => !S.area || x.area === S.area || x.area === null; const needs = p.needs.filter(inA), ag = p.agreed.filter(inA), ph = p.photos.filter(inA);
  $('#scope').textContent = S.area ? p.areas[S.area] : p.name; $('#scopeSub').textContent = S.area ? `${needs.length} need you · tap it again for all of it` : `${THINGS[p.thing].place} · ${THINGS[p.thing].name}`;
  $('#four').innerHTML = [['needs', needs.length, 'Needs you', 'needs'], ['money', money(m.left), 'Left', ''], ['agreed', `${ag.filter(a => a.ok).length}✓ ${ag.filter(a => !a.ok).length}?`, 'Agreed', ''], ['record', ph.length, p.record, '']].map(([k, v, l, c]) => `<button class="${c} ${S.tab === k ? 'on' : ''}" data-k="${k}"><b>${esc(v)}</b><span>${esc(l)}</span></button>`).join('');
  $('#four').querySelectorAll('button').forEach(b => b.addEventListener('click', () => { const k = b.dataset.k; if ((k === 'record') !== S.xray) { S.xray = k === 'record'; $('#xray').classList.toggle('on', S.xray); applyView(); } S.tab = k; render(); }));
  const B = $('#sbody');
  const doneBanner = p.status === 'done' ? `<div class="card"><div class="k g">FINISHED · ${p.year}</div><h3>Kept as a record</h3><p>Everything from this job — details, receipts, photos and care reminders — stays with the ${esc(THINGS[p.thing].name.toLowerCase())}.</p><div class="row"><button class="btn g" id="reopen">Reopen</button></div></div>` : '';
  if (S.tab === 'needs') B.innerHTML = doneBanner + (p.status === 'done' ? '' : needsHTML(p, needs));
  if (S.tab === 'money') B.innerHTML = moneyHTML(p, m);
  if (S.tab === 'agreed') B.innerHTML = `<div class="card">${ag.map(a => `<div class="agree"><span class="ic ${a.ok ? 'ok' : 'q'}">${a.ok ? '✓' : '?'}</span><div style="flex:1"><b>${esc(a.t)}</b><small>${esc(a.s)}</small>${!a.ok && a.draft ? `<div class="row"><button class="btn g" data-off="${a.id}">Make it official</button></div>` : ''}</div></div>`).join('') || '<p class="mini">Nothing yet. Shared texts and emails land here.</p>'}</div><div class="addc"><input id="agIn" placeholder="Type something that was agreed…"><button id="agAdd">Add</button></div><p class="mini">✓ in writing, with its source · ? said but not written down</p>`;
  if (S.tab === 'record') B.innerHTML = `<div class="card"><div class="k v">${esc(p.record.toUpperCase())}</div><h3>${ph.length ? `${ph.length} photo${ph.length > 1 ? 's' : ''} pinned where taken` : 'Nothing hidden recorded yet'}</h3><p>Tap a 📷 on the map.</p></div><div class="card"><div class="k">COMING · ${esc(p.coming[0])}</div><h3>${esc(p.coming[1])}</h3><p>${esc(p.coming[2])}</p></div><div class="card"><div class="k v">${esc(THINGS[p.thing].name.toUpperCase())} · HISTORY</div>${histHTML(p.thing)}</div>`;
  bind(B, p);
}
function histHTML(th) { const T0 = THINGS[th]; const rows = T0.hist.slice(); PROJ.filter(q => q.thing === th).forEach(q => { if (!rows.some(r => r.y === q.year && r.t === q.name)) rows.push({ y: q.year, t: q.name + (q.status === 'done' ? '' : q.status === 'active' ? ' · in progress' : ' · planned'), s: q.where, id: q.id }); }); rows.sort((a, b) => a.y - b.y); return `<div class="hist">${rows.map(r => `<div><b>${r.y} · ${esc(r.t)}</b><small>${esc(r.s)}</small></div>`).join('')}</div>`; }
function groupsHTML(L, compact) {
  const g = [['Needs you', L.filter(q => q.status === 'active' && q.needs.length)], ['In progress', L.filter(q => q.status === 'active' && !q.needs.length)], ['Planned', L.filter(q => q.status === 'planned')]];
  const done = L.filter(q => q.status === 'done').sort((a, b) => b.year - a.year);
  const row = q => { const n = q.needs.length; return `<button class="proj" data-p="${q.id}"><span class="ic">${q.icon}</span><span><b>${esc(q.name)}</b><small>${esc(HOMES[q.home].name)} · ${esc(q.place)} · ${q.status === 'done' ? q.year : esc(q.where)}</small></span>${q.status === 'done' ? '' : `<span class="c ${n ? '' : 'z'}">${n || '✓'}</span>`}</button>`; };
  let h = g.filter(x => x[1].length).map(([t, l]) => `<div class="gh">${t.toUpperCase()}</div>${l.map(row).join('')}`).join('');
  if (done.length) h += `<div class="gh">DONE</div>` + done.map(row).join('');
  return h || '<p class="mini">No projects match.</p>';
}
function bindList(B) { B.querySelectorAll('[data-p]').forEach(b => b.addEventListener('click', () => { const q = P(b.dataset.p); if (q.home !== S.home) { S.home = q.home; } go(q.id); })); }
function needsHTML(p, list) {
  let h = list.map(n => {
    if (n.kind === 'choose') return `<div class="card"><div class="k">${esc(n.k)}${n.area ? ' · ' + esc(p.areas[n.area]) : ''}</div><h3>${esc(n.title)}</h3><p>${esc(n.p)}</p><div class="opts">${n.opts.map((o, i) => `<button class="opt" data-ch="${n.id}" data-i="${i}"><div class="sw" style="background:${o.c}"></div><b>${esc(o.n)}</b><small>${esc(o.s)}</small>${o.vote ? `<div class="vote">${esc(o.vote)}</div>` : ''}</button>`).join('')}</div><div class="mini">None of these? <button class="ed" style="color:var(--v);font-weight:700" data-own="${n.id}">Type your own</button></div></div>`;
    if (n.kind === 'compare') return `<div class="card"><div class="k">${esc(n.k)} · ${esc(p.areas[n.area])}</div><h3>${esc(n.title)}</h3><p>${esc(n.p)}</p><table class="cmp">${n.table.map((r, i) => `<tr>${r.map(c => i === 0 || i === n.table.length - 1 ? `<th>${esc(c)}</th>` : `<td>${esc(c)}</td>`).join('')}</tr>`).join('')}</table><div class="mini">${esc(n.note)}</div><div class="row">${n.picks.map((x, i) => `<button class="btn ${i ? 'g' : ''}" data-cmp="${n.id}" data-i="${i}">Go with ${esc(x)}</button>`).join('')}</div></div>`;
    return `<div class="card"><div class="k">${esc(n.k)}</div><h3>${esc(n.title)}</h3><p>${esc(n.p)}</p><div class="row"><button class="btn g" data-done="${n.id}">Done</button></div></div>`;
  }).join('');
  if (!list.length) h = `<div class="card"><div class="k g">ALL CLEAR</div><h3>Nothing needs you here</h3><p>New things show up when they need a decision or a date.</p></div>`;
  if (!S.area && p.waiting.length) h += `<div class="card"><div class="k v">WAITING ON OTHERS</div>${p.waiting.map(w => `<h3>${esc(w.what)}</h3><p>${esc(w.who)} · ${esc(w.when)}. Atlas nudges you if it doesn’t arrive.</p>`).join('')}</div>`;
  return h;
}
function moneyHTML(p, m) {
  const seg = (v, c) => v > 0 ? `<div style="width:${Math.min(100, v / Math.max(1, m.budget) * 100)}%;background:${c}"></div>` : '';
  const q = p.lines.filter(l => l.kind === 'quote' && l.v > 0).reduce((s, l) => s + l.v, 0), pd = p.lines.filter(l => l.kind === 'paid').reduce((s, l) => s + l.v, 0), ch = p.lines.filter(l => l.kind === 'change' || l.kind === 'cost').reduce((s, l) => s + l.v, 0);
  return `<div class="card"><div class="k v">BUDGET <button class="ed" id="budE" style="color:var(--v);font-weight:700">${money(m.budget)} ✎</button></div><h3>${money(m.left)} ${m.cost ? 'left for surprises' : 'to spend — nothing committed yet'}</h3><div class="mbar">${seg(pd, '#2f9a66')}${seg(q, '#6b4bd1')}${seg(ch, '#d9822b')}${seg(Math.max(0, m.left), '#e9e4f3')}</div>
  <div class="legend"><span><i class="dot" style="background:#2f9a66"></i>Paid</span><span>${money(pd)}</span><span><i class="dot" style="background:#6b4bd1"></i>Quoted / contracted</span><span>${money(q)}</span><span><i class="dot" style="background:#d9822b"></i>Change orders &amp; extras</span><span>${money(ch)}</span>${m.covered ? `<span><i class="dot" style="background:#2f6fb0"></i>Covered (insurance, rebates)</span><span>−${money(m.covered).replace('$', '$')}</span><span><b>You pay</b></span><span>${money(m.you)}</span>` : ''}</div></div>
  <div class="card"><div class="k v">EVERY COST</div>${p.lines.map((l, i) => `<div class="line"><span class="kd ${l.kind}">${KIND[l.kind]}</span><span class="l">${esc(l.l)}${l.who ? ` <small style="color:var(--sub)">· ${esc(l.who)}</small>` : ''}${l.sample ? ' <small style="color:var(--sub)">(sample)</small>' : ''}</span><span class="v">${money(l.v)}</span><button aria-label="Remove" data-rm="${i}" style="color:var(--sub)">✕</button></div>`).join('') || '<p class="mini">No costs yet.</p>'}
  <div class="addc"><input id="costIn" placeholder="e.g. $1,150 skylight change order, Jamie"><button id="costAdd">Add</button></div><div class="row"><button class="btn g" id="rcpt">📷 Scan a receipt</button></div></div>
  <div class="card"><div class="k">WATCH</div><h3>${esc(p.watch[0])}</h3><p>${esc(p.watch[1])}</p></div>`;
}
function setField(p, k, v) { const f = p.details.find(x => x.k === k); if (f) f.v = v; else p.details.push(F(k, v)); preview(p, k, v); }
function preview(p, k, v) { const all = Object.values(COLOURS).flat(); const c = all.find(x => v.startsWith(x.n)); if (!c) return; if (p.id === 'siding' && k === 'Colour') mats.siding.color.set(c.hex); if (p.id === 'siding' && k === 'Trim colour') mats.trim.color.set(c.hex); if (p.id === 'roof' && k === 'Colour') mats.roof.color.set(c.hex); }
function bind(B, p) {
  const re = () => { buildTags(); render(); };
  const ro = $('#reopen'); if (ro) ro.addEventListener('click', () => { p.status = 'active'; p.here = Math.max(0, p.stops.length - 1); toast(`${p.name} reopened`); re(); });
  B.querySelectorAll('[data-ch]').forEach(b => b.addEventListener('click', () => { const n = p.needs.find(x => x.id === b.dataset.ch), i = +b.dataset.i; const o = n.opts[i]; p.needs = p.needs.filter(x => x !== n); const before = p.details.map(f => ({ ...f })); if (n.field) setField(p, n.field, o.n); let line = null; if (n.cost && n.cost[i]) { line = { l: `${n.title}: ${o.n}`, v: n.cost[i], kind: 'change', who: '', sample: true }; p.lines.push(line); } p.agreed.unshift({ area: n.area, ok: true, t: `${n.title}: ${o.n}`, s: 'Your decision · just now' }); buzz([12, 20, 30]); toast(`${n.title}: ${o.n}${line ? ' · added to money' : ''}`, () => { p.needs.push(n); p.agreed.shift(); p.details = before; if (line) p.lines = p.lines.filter(x => x !== line); re(); }); re(); }));
  B.querySelectorAll('[data-own]').forEach(b => b.addEventListener('click', () => { const n = p.needs.find(x => x.id === b.dataset.own); modal(`<h3>${esc(n.title)}</h3><p class="mini" style="margin:0">Type exactly what you want — Atlas keeps your words.</p><div class="editor"><input id="ownIn" placeholder="e.g. Fireclay 3×12, Sea Glass"></div><div class="row"><button class="btn" id="ownOk">Save choice</button><button class="btn g" id="cx">Cancel</button></div>`, () => { $('#ownIn').focus(); $('#ownOk').addEventListener('click', () => { const v = $('#ownIn').value.trim(); if (!v) return; p.needs = p.needs.filter(x => x !== n); if (n.field) setField(p, n.field, v); p.agreed.unshift({ area: n.area, ok: true, t: `${n.title}: ${v}`, s: 'Your decision · typed' }); closeModal(); toast(`${n.title}: ${v}`); re(); }); $('#cx').addEventListener('click', closeModal); }); }));
  B.querySelectorAll('[data-cmp]').forEach(b => b.addEventListener('click', () => { const n = p.needs.find(x => x.id === b.dataset.cmp), i = +b.dataset.i; p.needs = p.needs.filter(x => x !== n); p.agreed.unshift({ area: n.area, ok: true, t: `Going with ${n.picks[i]}`, s: 'Your decision · compared like for like' }); buzz([15, 25, 40]); toast('Quotes compared · decision recorded', () => { p.needs.unshift(n); p.agreed.shift(); re(); }); re(); }));
  B.querySelectorAll('[data-done]').forEach(b => b.addEventListener('click', () => { const n = p.needs.find(x => x.id === b.dataset.done); p.needs = p.needs.filter(x => x !== n); buzz(12); toast(`${n.title} · done`, () => { p.needs.push(n); re(); }); re(); }));
  B.querySelectorAll('[data-off]').forEach(b => b.addEventListener('click', () => { const a = p.agreed.find(x => x.id === b.dataset.off); modal(`<h3>Make it official</h3><p class="mini" style="margin:0">Drafted from your notes — edit anything. You send it; Atlas never sends for you.</p><textarea id="drTxt" style="width:100%;min-height:120px;margin-top:8px;border-radius:14px;border:1px solid var(--line);padding:12px;font:inherit;font-size:15px">${esc(a.draft)}</textarea><div class="row"><button class="btn" id="cp">Copy &amp; mark as asked</button><button class="btn g" id="cx">Cancel</button></div>`, () => { $('#cp').addEventListener('click', () => { const t = $('#drTxt').value; try { navigator.clipboard.writeText(t); } catch (e) {} a.s = 'Asked in writing just now · waiting for the reply'; p.waiting.push({ who: a.to[0].toUpperCase() + a.to.slice(1), what: 'Confirm: ' + a.t.toLowerCase(), when: 'this week' }); closeModal(); toast(`Copied · paste it to ${a.to}`); re(); }); $('#cx').addEventListener('click', closeModal); }); }));
  const ca = $('#costAdd'); if (ca) { const add = () => { const r = parseCost($('#costIn').value); if (!r) { toast('Add an amount, like $250'); return; } p.lines.push(r); buzz(10); toast(`${KIND[r.kind]} · ${r.l} · ${money(r.v)}`, () => { p.lines = p.lines.filter(x => x !== r); re(); }); re(); }; ca.addEventListener('click', add); $('#costIn').addEventListener('keydown', e => { if (e.key === 'Enter') add(); }); }
  B.querySelectorAll('[data-rm]').forEach(b => b.addEventListener('click', () => { const l = p.lines[+b.dataset.rm]; p.lines = p.lines.filter(x => x !== l); toast(`Removed ${l.l}`, () => { p.lines.push(l); re(); }); re(); }));
  const rc = $('#rcpt'); if (rc) rc.addEventListener('click', () => { const r = { l: 'Home Depot · fittings, sealant', v: 86.42, kind: 'paid', who: 'Read from receipt', sample: true }; p.lines.push(r); buzz(10); toast('Receipt read · $86.42 paid (sample)', () => { p.lines = p.lines.filter(x => x !== r); re(); }); re(); });
  const be = $('#budE'); if (be) be.addEventListener('click', () => modal(`<h3>Budget</h3><div class="editor"><input id="bIn" inputmode="decimal" value="${p.budget}"></div><div class="row"><button class="btn" id="bOk">Save</button><button class="btn g" id="cx">Cancel</button></div>`, () => { $('#bOk').addEventListener('click', () => { const v = parseFloat($('#bIn').value.replace(/[$,]/g, '')); if (!isNaN(v)) p.budget = v; closeModal(); re(); }); $('#cx').addEventListener('click', closeModal); }));
  const aa = $('#agAdd'); if (aa) aa.addEventListener('click', () => { const v = $('#agIn').value.trim(); if (!v) return; const a = { area: S.area, ok: false, id: 'u' + Date.now(), t: v, s: 'You typed it · not in writing yet', draft: `Hi — confirming what we agreed: ${v}. Could you reply to confirm? Thanks!`, to: 'them' }; p.agreed.push(a); toast('Added · tap “Make it official” to get it in writing'); re(); });
}

/* ---- full-screen sheets ---- */
function full(title, html, fn) { $('#fTitle').textContent = title; $('#fBody').innerHTML = html; $('#full').classList.add('show'); fn && fn(); }
function closeFull() { $('#full').classList.remove('show'); }
$('#fClose').addEventListener('click', closeFull);

/* switcher: one tap on the title, search everything */
let swSeg = 'This home';
function openSwitcher() {
  const draw = () => { const q = ($('#swQ')?.value || '').toLowerCase(); let L = swSeg === 'All homes' ? PROJ : projOf(S.home); L = L.filter(x => !q || (x.name + ' ' + x.place + ' ' + THINGS[x.thing].name + ' ' + x.year).toLowerCase().includes(q));
    $('#swL').innerHTML = groupsHTML(L); bindList($('#swL')); };
  full('Projects', `<input class="search" id="swQ" placeholder="Search projects, things, years…"><div class="seg">${['This home', 'All homes'].map(s => `<button class="${s === swSeg ? 'on' : ''}" data-s="${s}">${s}</button>`).join('')}<button data-new="1">＋ New renovation</button></div><button class="proj" id="swAll"><span class="ic">${HOMES[S.home].icon}</span><span><b>Everything at ${esc(HOMES[S.home].name)}</b><small>The whole map</small></span></button><div id="swL"></div>`, () => {
    $('#swQ').addEventListener('input', draw); $('#swAll').addEventListener('click', () => go(null));
    $('#fBody').querySelectorAll('[data-s]').forEach(b => b.addEventListener('click', () => { swSeg = b.dataset.s; $('#fBody').querySelectorAll('[data-s]').forEach(x => x.classList.toggle('on', x === b)); draw(); }));
    $('#fBody').querySelector('[data-new]').addEventListener('click', openCatalog); draw(); });
}
$('#titleSw').addEventListener('click', openSwitcher);

/* homes */
$('#homeSw').addEventListener('click', () => modal(`<h3>Your homes</h3>${Object.entries(HOMES).map(([k, h]) => { const L = projOf(k); const n = L.reduce((s, q) => s + q.needs.length, 0); return `<button class="proj" data-h="${k}"><span class="ic">${h.icon}</span><span><b>${esc(h.name)}</b><small>${esc(h.kind)} · ${L.length} projects</small></span><span class="c ${n ? '' : 'z'}">${n || '✓'}</span></button>`; }).join('')}<button class="btn g" id="addHome" style="width:100%">＋ Add a home</button>`, () => {
  $('#mcard').querySelectorAll('[data-h]').forEach(b => b.addEventListener('click', () => { closeModal(); goHome(b.dataset.h); }));
  $('#addHome').addEventListener('click', () => { $('#mcard').innerHTML = `<h3>Add a home</h3><div class="editor"><input id="hN" placeholder="Name — e.g. Mom’s house"></div><div class="chipset" id="hK">${['House', 'Apartment', 'Condo', 'Cabin', 'Townhouse', 'Mobile home', 'RV'].map((k, i) => `<button class="${i ? '' : 'on'}">${k}</button>`).join('')}</div><p class="mini">Atlas starts with a simple model. Later: sketch the floor plan or scan rooms with your phone so the map looks like your place.</p><div class="row"><button class="btn" id="hOk">Add</button><button class="btn g" id="cx">Cancel</button></div>`; $('#hK').querySelectorAll('button').forEach(b => b.addEventListener('click', () => $('#hK').querySelectorAll('button').forEach(x => x.classList.toggle('on', x === b)))); $('#cx').addEventListener('click', closeModal); $('#hOk').addEventListener('click', () => { const n = $('#hN').value.trim() || 'New home'; closeModal(); toast(`${n} added (mockup — uses the house model for now)`); }); });
}));

/* catalog: hundreds of kinds, or your own */
function openCatalog() {
  let cat = 'All';
  const draw = () => { const q = ($('#cQ')?.value || '').trim().toLowerCase(); const groups = Object.entries(CATALOG).filter(([g]) => cat === 'All' || g === cat); let h = groups.map(([g, list]) => { const l = list.filter(x => !q || x.toLowerCase().includes(q)); return l.length ? `<div class="gh">${g.toUpperCase()}</div><div class="chipset">${l.map(x => `<button data-c="${esc(x)}" data-g="${esc(g)}">${esc(x)}</button>`).join('')}</div>` : ''; }).join('');
    if (q) h = `<button class="proj" id="cOwn"><span class="ic">＋</span><span><b>Start “${esc($('#cQ').value.trim())}”</b><small>Your own kind of renovation — Atlas sets basic steps you can edit</small></span></button>` + h;
    $('#cL').innerHTML = h; $('#cL').querySelectorAll('[data-c]').forEach(b => b.addEventListener('click', () => create(b.dataset.c, b.dataset.g))); const o = $('#cOwn'); if (o) o.addEventListener('click', () => create($('#cQ').value.trim(), 'Custom')); };
  full('New renovation', `<input class="search" id="cQ" placeholder="What are you working on? e.g. new gutters"><div class="seg">${['All', ...Object.keys(CATALOG)].map(g => `<button class="${g === cat ? 'on' : ''}" data-g="${g}">${g}</button>`).join('')}</div><p class="mini">Or just say it — “the downstairs toilet keeps running” — and Atlas picks the kind and the size of the job.</p><div id="cL"></div>`, () => { $('#cQ').addEventListener('input', draw); $('#fBody').querySelectorAll('.seg [data-g]').forEach(b => b.addEventListener('click', () => { cat = b.dataset.g; $('#fBody').querySelectorAll('.seg [data-g]').forEach(x => x.classList.toggle('on', x === b)); draw(); })); draw(); });
}
function create(name, group) {
  const id = 'n' + Date.now(); const place = PLACE_OF[group] || 'Inside'; const steps = (STEPS_OF[group] || ['Plan', 'Do it', 'Done']).slice();
  const thingKey = 't' + id; THINGS[thingKey] = { home: S.home, name, place, hist: [] };
  const view = S.home === 'main' ? (place === 'Yard' ? 'yard' : place === 'Outside' ? 'siding' : place === 'Systems' ? 'item' : 'rooms') : S.home === 'cabin' ? 'deck' : 'condo';
  const areas = view === 'yard' ? { lawn: 'Yard' } : view === 'siding' ? { sFront: 'Front', sLeft: 'Left side', sBack: 'Back' } : view === 'item' ? { wh: 'Garage / utility' } : view === 'deck' ? { deck: 'Cabin' } : view === 'condo' ? { unit: 'Unit 4B' } : { kitchen: 'Kitchen', living: 'Living room', bath: 'Bathroom' };
  PROJ.push({ id, home: S.home, thing: thingKey, name, icon: '🛠', status: 'planned', year: 2026, place, view, record: 'Records', stops: steps, here: 0, where: `${steps[0]} · just started`, areas, details: [F('What exactly', '', 'text'), F('Material / type', '', 'text'), F('Colour', '', 'colour', { pal: place === 'Outside' ? 'siding' : 'paint' }), F('Size', '', 'text')], needs: [{ id: 'first', area: null, kind: 'task', k: 'FIRST', title: 'Add a photo or two of what’s there now', p: 'Before photos help with quotes, claims and later.' }], waiting: [], budget: 0, lines: [], watch: ['No budget yet', 'Tap the budget to set one, or add costs as they come.'], agreed: [], people: [], photos: [], coming: ['WHEN WORK STARTS', 'Photos of anything that gets covered up', 'Atlas reminds you at the right step.'] });
  buzz([10, 20]); toast(`${name} added to ${HOMES[S.home].name} · ${place}`); go(id);
}

/* details: every field editable, with AI help */
function openDetails() {
  const p = cur(); let open = null;
  const draw = () => {
    $('#fBody').innerHTML = `<div class="card">${p.details.map((f, i) => { const col = f.type === 'colour' ? Object.values(COLOURS).flat().find(c => f.v && f.v.startsWith(c.n)) : null; return `<div class="field"><button class="fr" data-f="${i}"><span class="fk">${esc(f.k)}</span><span class="fv">${col ? `<span class="sw2" style="background:${col.hex}"></span>` : ''}${f.v ? esc(f.v) : '<i>Add</i>'}</span><span class="ed">✎</span></button>${open === i ? editorHTML(f) : ''}</div>`; }).join('')}
      <div class="field"><button class="fr" id="addF"><span class="fk">＋ Add a field</span><span class="fv"><i>Warranty, model number, anything</i></span></button>${open === 'new' ? `<div class="editor"><input id="nfK" placeholder="Name — e.g. Warranty"><input id="nfV" placeholder="Value — e.g. 30 years, registered" style="margin-top:6px"><div class="row"><button class="btn" id="nfOk">Add</button></div></div>` : ''}</div></div>
      <div class="card"><div class="k v">STEPS · edit to match how this job really goes</div>${p.stops.map((s, i) => `<div class="stepl"><input value="${esc(s)}" data-st="${i}" aria-label="Step ${i + 1}"><button data-up="${i}" aria-label="Move up">↑</button><button data-del="${i}" aria-label="Remove">✕</button></div>`).join('')}<div class="row"><button class="btn g" id="addSt">＋ Add a step</button></div></div>
      <div class="card"><div class="k v">PEOPLE</div>${(p.people.length ? p.people : []).map(([n, r]) => `<div class="person"><span class="av">${esc(n[0])}</span><div><b>${esc(n)}</b><small>${esc(r)}</small></div></div>`).join('') || '<p class="mini">No one yet.</p>'}<div class="row"><button class="btn g" id="findPro">Find a local ${p.place === 'Outside' && /roof/i.test(p.name) ? 'roofer' : p.place === 'Yard' ? 'landscaper' : p.place === 'Systems' ? 'plumber' : 'contractor'}</button></div></div>
      <div class="card"><div class="k v">${esc(THINGS[p.thing].name.toUpperCase())} · HISTORY</div>${histHTML(p.thing)}</div>`;
    $('#fBody').querySelectorAll('[data-f]').forEach(b => b.addEventListener('click', () => { open = open === +b.dataset.f ? null : +b.dataset.f; draw(); }));
    $('#addF').addEventListener('click', () => { open = open === 'new' ? null : 'new'; draw(); });
    const nf = $('#nfOk'); if (nf) nf.addEventListener('click', () => { const k = $('#nfK').value.trim(), v = $('#nfV').value.trim(); if (!k) return; p.details.push(F(k, v)); open = null; draw(); toast(`Added ${k}`); });
    if (typeof open === 'number') bindEditor(p, p.details[open], () => { open = null; draw(); render(); });
    $('#fBody').querySelectorAll('[data-st]').forEach(inp => inp.addEventListener('change', () => { p.stops[+inp.dataset.st] = inp.value; render(); }));
    $('#fBody').querySelectorAll('[data-up]').forEach(b => b.addEventListener('click', () => { const i = +b.dataset.up; if (!i) return; [p.stops[i - 1], p.stops[i]] = [p.stops[i], p.stops[i - 1]]; draw(); render(); }));
    $('#fBody').querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', () => { if (p.stops.length < 2) return; p.stops.splice(+b.dataset.del, 1); p.here = Math.min(p.here, p.stops.length - 1); draw(); render(); }));
    $('#addSt').addEventListener('click', () => { p.stops.push('New step'); draw(); render(); });
    $('#findPro').addEventListener('click', () => modal(`<h3>Find a local pro</h3><p class="mini" style="margin:0">Atlas searches only when you ask, and shows where each result came from.</p><div class="card" style="margin-top:10px"><div class="k v">EXAMPLE RESULTS · SAMPLE</div>${[['Summit Roofing', '4.8 ★ · 212 reviews · license active on the state lookup'], ['Front Range Exteriors', '4.6 ★ · 98 reviews · license active'], ['Peak Home Pros', '4.2 ★ · 41 reviews · license expired — skip']].map(([n, r]) => `<div class="person"><span class="av">${n[0]}</span><div><b>${n}</b><small>${r}</small></div></div>`).join('')}<p class="src2">Sources: review sites and your state’s license lookup (in the app, links appear here).</p></div><div class="row"><button class="btn g" id="cx">Close</button></div>`, () => $('#cx').addEventListener('click', closeModal)));
  };
  full(`${p.name} · details`, '', draw);
}
function editorHTML(f) {
  if (f.type === 'choice') return `<div class="editor"><div class="chipset">${f.opts.map(o => `<button class="${o === f.v ? 'on' : ''}" data-o="${esc(o)}">${esc(o)}</button>`).join('')}</div><input id="eIn" placeholder="Or type your own" value=""><div class="row"><button class="btn" id="eOk">Save</button><button class="btn g" id="eAi">✦ What are my options?</button></div></div>`;
  if (f.type === 'colour') { const pal = COLOURS[f.pal] || COLOURS.paint; return `<div class="editor">${pal.map(c => `<button class="colrow" data-col="${esc(c.n + (c.code ? ' · ' + c.code : ''))}"><span class="big" style="background:${c.hex}"></span><span><b>${esc(c.n)}${c.code ? ' · ' + esc(c.code) : ''}</b><small>${esc(c.m)}</small></span></button>`).join('')}<input id="eIn" placeholder="Type a colour name or code — e.g. SW 7015" style="margin-top:6px"><div class="row"><button class="btn" id="eOk">Save</button><button class="btn g" id="eAi">✦ Look up current colours</button></div><p class="src2">Colour names from the makers’ current lines · verify with a sample before ordering. Swatches are approximate.</p></div>`; }
  return `<div class="editor"><input id="eIn" value="${esc(f.v)}" placeholder="Type…"><div class="row"><button class="btn" id="eOk">Save</button></div></div>`;
}
function bindEditor(p, f, done) {
  const B = $('#fBody'); const save = v => { if (v == null) return; f.v = v; preview(p, f.k, v); buzz(8); done(); };
  B.querySelectorAll('[data-o]').forEach(b => b.addEventListener('click', () => save(b.dataset.o)));
  B.querySelectorAll('[data-col]').forEach(b => b.addEventListener('click', () => save(b.dataset.col)));
  const ok = $('#eOk'); if (ok) ok.addEventListener('click', () => { const v = $('#eIn').value.trim(); if (v) save(v); else done(); });
  const ai = $('#eAi'); if (ai) ai.addEventListener('click', () => { if (f.type === 'choice') toast(`Atlas compared ${f.opts.length} options for ${f.k.toLowerCase()} — cost, life span, upkeep (in the app, with sources)`); else toast('Looked up the maker’s current colour line · 5 matches shown (sample)'); });
}
$('#detB').addEventListener('click', openDetails);

/* share */
$('#shareB').addEventListener('click', () => { const p = cur(); const scope = p ? p.name : `Everything at ${HOMES[S.home].name}`;
  const draw = () => $('#fBody').innerHTML = `<div class="card"><div class="k v">WHO HAS ACCESS</div>${S.people.map(x => `<div class="person"><span class="av">${esc(x.n[0])}</span><div style="flex:1"><b>${esc(x.n)}</b><small>${esc(x.r)} · ${esc(x.s)}</small></div></div>`).join('')}</div>
    <div class="card"><div class="k v">INVITE</div><div class="editor" style="margin-top:0;background:none;padding:0"><input id="shN" placeholder="Name, email or phone"></div><div class="gh">THEY CAN</div><div class="chipset" id="shR"><button class="on">View</button><button>Comment</button><button>Edit</button></div><div class="gh">WHAT THEY SEE</div><div class="chipset" id="shS"><button class="on">${esc(scope)}</button><button>${esc(HOMES[S.home].name)}</button><button>All homes</button></div><div class="tog">Hide money and payments<input type="checkbox" id="shM" checked></div><div class="tog">Hide private notes<input type="checkbox" checked></div><div class="row"><button class="btn" id="shGo">Send invite</button><button class="btn g" id="shLink">Copy view-only link</button></div><p class="mini">For a contractor: view only, this project, money hidden. For your partner: edit, everything.</p></div>`;
  full('Share', '', () => { draw(); const rebind = () => { ['#shR', '#shS'].forEach(id => $(id).querySelectorAll('button').forEach(b => b.addEventListener('click', () => $(id).querySelectorAll('button').forEach(x => x.classList.toggle('on', x === b))))); $('#shGo').addEventListener('click', () => { const n = $('#shN').value.trim(); if (!n) { toast('Add a name, email or phone'); return; } S.people.push({ n, r: 'Can ' + $('#shR .on').textContent.toLowerCase(), s: $('#shS .on').textContent + ($('#shM').checked ? ' · money hidden' : '') }); toast(`Invite ready for ${n} — you send it`); draw(); rebind(); }); $('#shLink').addEventListener('click', () => toast('View-only link copied (mockup)')); }; rebind(); }); });

/* export */
$('#exportB').addEventListener('click', () => { const p = cur(); let scope = p ? 'project' : 'home', preset = 'Custom';
  const scopes = [p && ['project', `This project · ${p.name}`], p && ['thing', `${THINGS[p.thing].name} · full history`], ['home', `${HOMES[S.home].name} · everything`], ['all', 'All homes']].filter(Boolean);
  const draw = () => { const L = scope === 'project' ? [p] : scope === 'thing' ? PROJ.filter(q => q.thing === p.thing) : scope === 'home' ? projOf(S.home) : PROJ; const tot = L.reduce((s, q) => s + moneyOf(q).cost, 0);
    $('#fBody').innerHTML = `<div class="gh">WHAT</div><div class="chipset" id="exS">${scopes.map(([k, l]) => `<button class="${k === scope ? 'on' : ''}" data-x="${k}">${esc(l)}</button>`).join('')}</div>
    <div class="gh">FOR</div><div class="chipset" id="exP">${['Custom', 'Insurance claim', 'Selling the home', 'Taxes'].map(x => `<button class="${x === preset ? 'on' : ''}" data-pr="${x}">${x}</button>`).join('')}</div>
    <div class="card checks"><label><input type="checkbox" checked><span><b>PDF binder</b><small>Readable: summary, details, timeline, photos, receipts</small></span></label><label><input type="checkbox" checked><span><b>Spreadsheet</b><small>Every cost — Google Sheets or CSV</small></span></label><label><input type="checkbox" ${preset === 'Insurance claim' ? 'checked' : ''}><span><b>Photos &amp; files (ZIP)</b><small>Originals with dates</small></span></label><label><input type="checkbox"><span><b>Full backup</b><small>Re-import into Atlas on another phone</small></span></label></div>
    <div class="gh">PREVIEW · PAGE 1</div><div class="prev"><h4>${esc(scope === 'all' ? 'All homes' : HOMES[S.home].name)} — ${esc(scope === 'project' ? p.name : scope === 'thing' ? THINGS[p.thing].name + ' history' : 'Home record')}</h4><div class="sm">${preset === 'Custom' ? 'Prepared Nov 24, 2026' : preset + ' · prepared Nov 24, 2026'} · ${L.length} project${L.length > 1 ? 's' : ''}</div><table>${L.slice(0, 7).map(q => `<tr><td>${q.year} · ${esc(q.name)}${preset === 'Selling the home' && q.status !== 'done' ? ' (in progress)' : ''}</td><td>${moneyOf(q).cost ? money(moneyOf(q).cost) : '—'}</td></tr>`).join('')}<tr><td><b>Total recorded</b></td><td>${money(tot)}</td></tr></table>${preset === 'Taxes' ? '<div class="sm" style="margin-top:6px">Improvement costs with receipts — a starting point for your tax preparer.</div>' : ''}${preset === 'Insurance claim' ? '<div class="sm" style="margin-top:6px">Dated before/after photos and receipts come first.</div>' : ''}</div>
    <div class="row"><button class="btn" id="exGo">Create export</button></div><p class="mini">Saves to your phone or Google Drive, or shares straight to someone. Numbers here include samples.</p>`;
    $('#exS').querySelectorAll('button').forEach(b => b.addEventListener('click', () => { scope = b.dataset.x; draw(); })); $('#exP').querySelectorAll('button').forEach(b => b.addEventListener('click', () => { preset = b.dataset.pr; draw(); })); $('#exGo').addEventListener('click', () => { buzz([10, 20]); toast('Export ready (mockup) — in the app it saves or shares'); }); };
  full('Export', '', draw); });

/* ---- photos ---- */
function openPhoto(ph) {
  const c = document.createElement('canvas'); c.width = 640; c.height = 400; const g = c.getContext('2d'); const d = ph.draw;
  if (d === 'studs') { g.fillStyle = '#e9dfcf'; g.fillRect(0, 0, 640, 400); g.fillStyle = '#d9b77e'; for (let x = 20; x < 640; x += 90) g.fillRect(x, 0, 22, 400); g.strokeStyle = '#d94a3d'; g.lineWidth = 6; g.beginPath(); g.moveTo(0, 230); g.lineTo(480, 230); g.lineTo(480, 320); g.stroke(); g.strokeStyle = '#2f6fd9'; g.beginPath(); g.moveTo(80, 150); g.lineTo(640, 150); g.stroke(); g.fillStyle = '#9aa3ad'; g.fillRect(560, 120, 18, 280); }
  if (d === 'sky') { g.fillStyle = '#efe9df'; g.fillRect(0, 0, 640, 400); g.fillStyle = '#cfe6ff'; g.fillRect(200, 100, 240, 180); g.setLineDash([10, 8]); g.strokeStyle = '#1d1830'; g.lineWidth = 3; g.strokeRect(200, 100, 240, 180); g.setLineDash([]); g.fillStyle = '#1d1830'; g.font = 'bold 26px Roboto,sans-serif'; g.fillText('48″ × 24″ between rafters', 170, 330); }
  if (d === 'hail') { g.fillStyle = '#4b5059'; g.fillRect(0, 0, 640, 400); for (let r = 0; r < 12; r++) for (let k = 0; k < 12; k++) { g.fillStyle = (r + k) % 2 ? '#454a52' : '#50555e'; g.fillRect(k * 56 - (r % 2) * 28, r * 34, 54, 32); } g.fillStyle = 'rgba(210,200,180,.85)'; [[120, 90], [300, 160], [420, 70], [210, 260], [520, 300], [90, 330]].forEach(([x, y]) => { g.beginPath(); g.arc(x, y, 9, 0, 7); g.fill(); }); }
  if (d === 'sprinkler') { g.fillStyle = '#6e5038'; g.fillRect(0, 0, 640, 400); g.fillStyle = '#8fbf6a'; g.fillRect(0, 0, 640, 80); g.strokeStyle = '#2b2b2b'; g.lineWidth = 14; g.beginPath(); g.moveTo(0, 240); g.lineTo(640, 210); g.stroke(); g.fillStyle = '#3a8ad9'; g.fillRect(300, 190, 40, 50); }
  if (d === 'plate') { g.fillStyle = '#f1f1ee'; g.fillRect(0, 0, 640, 400); g.fillStyle = '#ffd34d'; g.fillRect(150, 60, 340, 280); g.fillStyle = '#1d1830'; g.font = 'bold 28px Roboto,sans-serif'; g.fillText('WATER HEATER', 200, 110); g.font = '22px monospace'; ['MODEL  XG40T06', 'SERIAL 1103A0042', 'MFG    MAR 2011', 'CAP    40 GAL', 'GAS    NATURAL'].forEach((t, i) => g.fillText(t, 190, 160 + i * 34)); }
  modal(`<h3>${esc(ph.t)}</h3><div class="mini" style="margin:0">${esc(ph.d)}</div><img class="photo" alt="${esc(ph.t)}" src="${c.toDataURL()}"><div class="row"><button class="btn g" id="cx">Close</button></div>`, () => $('#cx').addEventListener('click', closeModal));
}

/* ---- capture ---- */
$('#fab').addEventListener('click', () => modal(`<h3>Add anything</h3><p class="mini" style="margin:0">Snap, say, share — or type. Atlas files it to the right home, project and part, and you can move it.</p><div class="cap"><button id="cS"><span>💬</span>Shared text</button><button id="cP"><span>📷</span>Photo</button><button id="cV"><span>🎙</span>Voice</button></div><div class="addc" style="margin-top:12px"><input id="cT" placeholder="Or type: “roofer starts Jan 5”"><button id="cTOk">Add</button></div>`, () => {
  const target = () => cur() || P(S.home === 'main' ? 'kitchen' : S.home === 'cabin' ? 'deck' : 'paint');
  $('#cS').addEventListener('click', () => { closeModal(); const q = target(); const a = { area: Object.keys(q.areas)[0], ok: true, t: q.id === 'kitchen' ? 'Cabinet delivery moved to Dec 18' : 'Can start the week of Jan 5', s: 'Text you shared just now' }; q.agreed.unshift(a); buzz(12); toast(`Filed to ${q.name} · Agreed`, () => { q.agreed.shift(); render(); }); render(); });
  $('#cV').addEventListener('click', () => { closeModal(); const q = target(); const n = { id: 'v' + Date.now(), area: null, kind: 'task', k: 'FROM YOUR VOICE NOTE', title: 'Get that in writing', p: '“Remind me to get the start date in writing.”' }; q.needs.push(n); buzz(12); toast(`Voice note → a to-do in ${q.name}`, () => { q.needs = q.needs.filter(x => x !== n); buildTags(); render(); }); buildTags(); render(); });
  $('#cP').addEventListener('click', () => { closeModal(); const q = target(); if (!q.photos.length && !cur()) { toast(`Photo filed to ${q.name}`); return; } const area = Object.keys(q.areas)[0]; const pos = (AREA_TAG[area] || TAGPOS[area] || [0, 2, 0]).slice(); pos[0] += .8; const ph = { area, t: `${q.areas[area]} · new photo`, d: 'Just now (sample)', pos, draw: { rooms: 'studs', roof: 'hail', siding: 'studs', yard: 'sprinkler', item: 'plate' }[q.view] || 'studs' }; q.photos.push(ph); buildTags(); buzz(12); toast(`Photo pinned to ${q.areas[area]}`, () => { q.photos = q.photos.filter(x => x !== ph); buildTags(); render(); }); if (cur() && !S.xray) $('#xray').click(); else render(); });
  $('#cTOk').addEventListener('click', () => { const v = $('#cT').value.trim(); if (!v) return; closeModal(); const q = target(); const r = parseCost(v); if (r && /\$|\d{3,}/.test(v)) { q.lines.push(r); toast(`Cost added to ${q.name} · ${money(r.v)}`); } else { q.agreed.unshift({ area: null, ok: false, id: 't' + Date.now(), t: v, s: 'You typed it', draft: `Hi — confirming: ${v}. Could you reply to confirm? Thanks!`, to: 'them' }); toast(`Added to ${q.name}`); } render(); });
}));

/* ---- modal, toast ---- */
function modal(html, fn) { $('#mcard').innerHTML = html; $('#modal').classList.add('show'); fn && fn(); }
function closeModal() { $('#modal').classList.remove('show'); }
$('#modal').addEventListener('click', e => { if (e.target.id === 'modal') closeModal(); });
let tt; function toast(msg, undo) { const t = $('#toast'); t.innerHTML = `<span>${esc(msg)}</span>${undo ? '<button id="un">Undo</button>' : ''}`; t.classList.add('show'); if (undo) $('#un').addEventListener('click', () => { undo(); t.classList.remove('show'); }); clearTimeout(tt); tt = setTimeout(() => t.classList.remove('show'), 4200); }
$('#grab').addEventListener('click', () => $('#sheet').classList.toggle('tall'));

/* ---- router + loop ---- */
function route() { const h = location.hash === '#journeys' ? 'ten' : 'reno'; $('#reno').classList.toggle('on', h === 'reno'); $('#ten').classList.toggle('on', h === 'ten'); $('#t1').classList.toggle('on', h === 'reno'); $('#t2').classList.toggle('on', h === 'ten'); if (h === 'reno') resize(); }
addEventListener('hashchange', route);
function frame() {
  if (fly) { const k = ease(clamp((performance.now() - fly.s) / fly.d)); camera.position.lerpVectors(fly.p0, fly.p1, k); controls.target.lerpVectors(fly.t0, fly.t1, k); if (k >= 1) fly = null; }
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
renderTen(); route(); applyView(); buildTags(); render();
camera.position.set(26, 24, 32); flyTo(CAM.main, 1.6);
loadAll().then(furnish).catch(e => console.error(e));
frame();
}
