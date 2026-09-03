# Homika Store Foundation

Static Cloudflare Pages site for Homika Pro Patch 13A.

Recommended Cloudflare Pages configuration:

- Repository: the Homika Pro repository
- Root directory: `store`
- Framework preset: None
- Build command: leave empty
- Build output directory: `.`

After the Pages URL is live, set Worker environment variable `HOMIKA_STORE_URL` to that HTTPS URL. The existing Android Buy/Renew links go through `/buy/homika-pro`, so installed APKs will automatically start opening this store without another app patch.

Patch 13A does not process money. Plan buttons intentionally stop at the checkout foundation. Payment gateway, signed webhook verification, automatic licence issuance and renewal are Patch 13B.
