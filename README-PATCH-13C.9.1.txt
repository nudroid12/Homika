Homika Pro Patch 13C.9.1 - Deploy Config Path Fix

Fix:
- Rebase Worker `main` path when generating .homika-wrangler/wrangler.deploy.jsonc.
- Expected generated entry point: ../src/index.js
- Keeps 13C.9 keep_vars and required-secret hardening intact.

No D1 migration.
No Android changes.
