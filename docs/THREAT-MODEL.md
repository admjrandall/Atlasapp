# Threat model (Phase 0, single-user local app)

| Asset | Threat | Control in this build | Later |
|---|---|---|---|
| Attachments (PDFs, photos) | Device theft / file extraction | AES-256-GCM with non-exportable Keystore key; app-private dir | Biometric-gated key (Phase 7) |
| Knowledge database | Device theft; cloud backup leakage | App-private storage + Android FBE; `allowBackup=false`, data-extraction rules exclude all | SQLCipher (Phase 7) |
| Imported content | Malicious PDFs/HTML; prompt injection | Content treated as data only; no scripts/macros executed; PDF rendered to bitmap; no AI actions in this build | Phase 4: imported text never authorizes actions; schema-validated, allowlisted AI outputs |
| Temp decrypted files (for "open original") | Leftover plaintext | Written to cache/`open/`, deleted on next launch and after 10 min | — |
| Logs | Secret leakage | No content logged | — |
| Network | Exfiltration | No network permission in this build | Explicit consent per external provider |

## Phase 1–3 verification (Nov 22 2026)
- ML Kit libraries merge `INTERNET` and `ACCESS_NETWORK_STATE` into the manifest. `INTERNET` is removed with `tools:node="remove"`; verified with `aapt2 dump permissions` on the signed release APK and by `DataTest.appCannotUseTheNetwork`. ML Kit's own usage-log uploads therefore cannot leave the app.
- The document scanner runs inside Google Play services (its own process and permissions), not in Atlas. Scanned pages come back as a PDF that Atlas encrypts before reading.
- Dictation uses the system speech recognizer with `EXTRA_PREFER_OFFLINE`. Whether audio leaves the phone is up to that recognizer app, not Atlas. Stated in the ledger.
- Opening an original decrypts a temporary copy in `cache/open`, shared through a FileProvider limited to that folder; the folder is emptied on every launch.


## 0.5 changes (Nov 22 2026)
- **Network now present.** INTERNET is used only for (1) fetching a page you saved and its preview image, (2) downloading the AI model once. Cleartext disabled by network_security_config. No upload path exists in code. Residual risk: a malicious page can't execute (stored as plain text, never rendered as HTML), but its text may try prompt injection → model output is schema-constrained, validated (`Ai.parse`) and only becomes reviewable proposals.
- **SSRF to your LAN:** hosts resolving to private, loopback, link-local, CGNAT (incl. Tailscale 100.64/10) or ULA addresses are refused before and on every redirect. Residual: DNS rebinding between check and connect (low value for an attacker; accepted).
- **Model supply chain:** URL pinned to a Hugging Face commit; file size and SHA-256 must match before use; otherwise deleted.
- **Database at rest:** SQLCipher AES-256; passphrase never stored in plaintext (Keystore-wrapped, noBackupFilesDir). App lock is a UI gate on top (biometric or device credential), not a second key.
- **Contacts:** written back only when you enable two-way sync; contact data stays on the phone. Play's Oct 28 2026 Contacts policy applies to Play distribution only; Atlas is side-loaded.
- **Notifications:** content shows on the lock screen and watch per your system settings; reminders contain task titles.
