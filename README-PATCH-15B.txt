HOMIKA PRO PATCH 15B - FINAL PRODUCTION AUDIT + RELEASE HARDENING

Purpose:
- Final pre-v1.0 static audit and low-risk production hardening only.
- No new product feature.

Changes:
- Worker v22 health marker.
- HTTPS-only Store/renewal checkout URLs.
- Constant-time admin-secret comparison.
- Complete CORS declaration for existing PUT cloud snapshot route.
- Store no-referrer + CSP security headers.
- Ignore and safety-remove LOCAL-ONLY-DO-NOT-UPLOAD from repository tree.
- Add final signed-APK acceptance checklist.

No D1 migration.
No signing-key regeneration.
No change to pricing, licensing rules, PIN hashing, trial ledger, device limits,
cloud crypto/sync merger, Money, Calendar, Booking, Reports or updater cert.

After GitHub Actions is green, run docs/FINAL-PRODUCTION-AUDIT.md on the signed APK.
