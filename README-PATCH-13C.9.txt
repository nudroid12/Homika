HOMIKA PRO PATCH 13C.9
Worker Configuration Persistence / Deployment Hardening

Purpose
-------
Prevent future Worker deployments from silently losing dashboard variables,
and prevent deployment of a Worker when critical production secrets are absent.

How it works
------------
1. backend/scripts/prepare-worker-deploy-config.mjs reads the EXISTING
   backend/wrangler.jsonc at build time.
2. It generates a temporary deployment config that preserves the existing
   D1 database ID, D1 binding, R2 binding and all other Worker configuration.
3. The generated config forces keep_vars=true.
4. It declares critical Homika secrets as required. Wrangler must see those
   secrets on the Worker before a production deploy can succeed.
5. .wrangler/deploy/config.json redirects normal `wrangler deploy` to the
   generated hardened config, including Cloudflare Workers Builds that invoke
   Wrangler directly after dependency installation.
6. `npm run deploy` also explicitly uses --keep-vars.

Critical secrets guarded
------------------------
- LICENSE_SIGNING_PRIVATE_KEY
- CLOUD_MASTER_KEY
- HOMIKA_ADMIN_SECRET
- HOMIKA_ADMIN_TELEGRAM_BOT_TOKEN
- HOMIKA_ADMIN_TELEGRAM_CHAT_ID
- BREVO_API_KEY
- HOMIKA_PURCHASE_PIN_PEPPER

Non-secret dashboard variables
------------------------------
keep_vars=true preserves dashboard variables such as HOMIKA_EMAIL_FROM.
HOMIKA_STORE_URL already has a compiled production fallback from Patch 13C.2.

One-time action after installing this patch
-------------------------------------------
If the next Cloudflare Worker deploy reports a missing required secret, add
that secret ONCE in app-license-api -> Settings -> Variables and Secrets,
then redeploy. Do not put secret values in GitHub or in this patch.

No D1 migration.
No Android changes.
No secret values included.
