# Journey prototype: 3D Home Renovation (approved direction, Nov 24 2026)

Live page: https://claude.ai/artifact/3szfsJcbcyoJ9xsVuWJNja

Open `index.html` from a local web server (`python3 -m http.server`) — it uses ES modules.

- three.js r170 (vendored in `lib/`), models embedded as base64 GLB in `models.js`.
- Models: KayKit Furniture Bits, KayKit City Builder Bits, Kenney, Quaternius — all CC0.
- House is procedural; stages: plan → quotes → demolition → build → finish.
- Time dial: past solid, future as blue "not real yet" ghost. Look inside = roof lifts, front wall drops.
- Port target for the app: SceneView (Filament) in Compose.
