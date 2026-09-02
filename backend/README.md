# Homika Backend

Backend for Homika Pro licensing and encrypted Homika Cloud backup.

## Production Worker

- Worker name: `app-license-api`
- D1 binding: `DB` -> `app-license-prod`
- R2 binding: `BACKUPS` -> `homika-cloud-prod`
- Secrets stored only in Cloudflare:
  - `LICENSE_SIGNING_PRIVATE_KEY`
  - `CLOUD_MASTER_KEY`

## Before first Git deployment

Open `wrangler.jsonc` and replace:

`REPLACE_WITH_D1_DATABASE_ID`

with the UUID shown for the existing Cloudflare D1 database `app-license-prod`.

Do not create a second production database.

## Cloudflare Git deployment

Connect the existing Homika repository to the EXISTING Worker `app-license-api` using
Cloudflare Workers Builds, with the Root directory set to `backend/`.

Suggested settings:
- Production branch: `main`
- Root directory: `backend/`
- Build command: `npm run check`
- Deploy command: `npx wrangler deploy`

After the connection is saved, any new push to `main` can deploy the Worker
through Cloudflare's own build system. No GitHub Actions workflow is included.

## Important

The Worker name in `wrangler.jsonc` intentionally matches the existing
Cloudflare Worker name: `app-license-api`.

Never commit `.dev.vars`, private keys, cloud master keys, or other secrets.
