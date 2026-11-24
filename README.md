# Atlas

A private, on-device knowledge and productivity app for Android, designed around the Samsung Galaxy Z Fold 6 (inner and cover screens). Kotlin and Jetpack Compose.

Tell or ask Atlas anything in one box: a name finds things, a sentence files them ("Call Priya Friday", "add to Garden"), a question about your things gets an instant answer, and a general question goes to an optional on-phone model. Spaces, people, tasks, files, ideas and notes live on boards that show how things connect.

## Principles

- **Local first.** Data stays on the phone, encrypted. No accounts, ads, analytics or telemetry. `allowBackup=false`.
- **AI is optional and on-device.** One `Brain` interface; today Gemma via LiteRT-LM on the GPU. The model is downloaded once or imported from a file, never bundled in the APK. No cloud fallback.
- **Code before model.** Routing, search, rule answers, briefings and grouping are plain code; the model only refines or answers what code can't.
- **Imported content is untrusted data.**

## Build

Requirements: JDK 21 (Robolectric SDK 36 needs it), Android SDK (compileSdk 37), minSdk 26.

```bash
./gradlew testDebugUnitTest      # Robolectric UI + screenshot tests
./gradlew assembleDebug
```

Release builds are signed only when a `keystore.properties` (with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`) is present in the project root. The keystore and its properties are git-ignored and never committed.

## Docs

- [Architecture](docs/ARCHITECTURE.md) · [Data model](docs/DATA-MODEL.md) · [Design tokens](docs/DESIGN-TOKENS.md)
- [Threat model](docs/THREAT-MODEL.md) · [Test plan](docs/TEST-PLAN.md)
- [Build ledger](docs/LEDGER.md): what was asked, what changed, and which test covers it, per release

Current version: 0.7.2.
