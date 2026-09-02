# Production secrets

Do NOT commit secret values to this repository.

The existing Cloudflare Worker `app-license-api` must keep these secrets in
Cloudflare Dashboard -> Worker -> Settings -> Variables and Secrets:

- `LICENSE_SIGNING_PRIVATE_KEY`
- `CLOUD_MASTER_KEY`

Bindings declared in `wrangler.jsonc`:

- D1: `DB` -> `app-license-prod`
- R2: `BACKUPS` -> `homika-cloud-prod`

The Worker source contains only the RSA PUBLIC verification key. The private
signing key and cloud master key must remain only in Cloudflare Secrets.
