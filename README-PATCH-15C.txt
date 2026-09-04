Homika Pro Patch 15C — Final Release Polish / Freeze

This is intentionally a zero-runtime-change release-candidate patch.

Why:
- 15B production audit is already build-green.
- Introducing fresh Android/backend/payment/cloud code at the final gate would add regression risk without a confirmed defect.

Included:
- docs/RELEASE-CANDIDATE-15C.md
- docs/PRODUCTION-SMOKE-TEST-v1.0.md
- docs/RELEASE-NOTES-v1.0.md

Runtime changes: NONE
D1 migration: NONE
Backend migration: NONE
Cloud protocol change: NONE
Theme redesign: NONE

After the workflow is green, install the signed APK and run the short production smoke test. If no release blocker appears, proceed to v1.0 production freeze.
