# Test plan

Automated (Robolectric, JVM): run `./gradlew :app:testDebugUnitTest`.

| Area | Tests |
|---|---|
| Schema & integrity | FK cascade, unique relationships, trash/restore, shared document linked to 3 projects stays 1 row |
| Capture pipeline | Original persisted before extraction; Jamie scenario proposals (person, project via graph evidence, follow-up task, expected estimate, milestone); accept/reject transactionality; no duplicate person on accept |
| Search | FTS exact, prefix, extracted text, graph neighbours |
| Insights | Every insight has ≥1 source; no numeric scores |
| Tokens | No prohibited hues; key text pairs ≥ 4.5:1 |
| UI | Compact and expanded shells; every destination reachable; refocus + back on map; inspector tabs; state kept across width change |
| Visual | Screenshots: phone 411dp, Fold cover ~344dp, Fold inner ~840dp, tablet 1280dp |

Not covered by automation here: real-device performance, TalkBack walkthrough on hardware, biometric, camera scanner (needs Play services). These are listed in `LEDGER.md` as unverified.
