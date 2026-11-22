# Threat model (Phase 0, single-user local app)

| Asset | Threat | Control in this build | Later |
|---|---|---|---|
| Attachments (PDFs, photos) | Device theft / file extraction | AES-256-GCM with non-exportable Keystore key; app-private dir | Biometric-gated key (Phase 7) |
| Knowledge database | Device theft; cloud backup leakage | App-private storage + Android FBE; `allowBackup=false`, data-extraction rules exclude all | SQLCipher (Phase 7) |
| Imported content | Malicious PDFs/HTML; prompt injection | Content treated as data only; no scripts/macros executed; PDF rendered to bitmap; no AI actions in this build | Phase 4: imported text never authorizes actions; schema-validated, allowlisted AI outputs |
| Temp decrypted files (for "open original") | Leftover plaintext | Written to cache/`open/`, deleted on next launch and after 10 min | — |
| Logs | Secret leakage | No content logged | — |
| Network | Exfiltration | No network permission in this build | Explicit consent per external provider |
