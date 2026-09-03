Homika Pro Patch 13C.2 - Persistent Store URL

Changes:
- Worker now has canonical fallback Store URL: https://homika-store.pages.dev/
- HOMIKA_STORE_URL remains an optional override.
- Future Wrangler deploys cannot break Store/checkout just because the dashboard text variable disappears.
- PROJECT-CONTEXT.md updated.

No Android changes.
No D1 migration.
No secrets included.
backend/wrangler.jsonc is intentionally NOT replaced.
