# Homika monorepo layout

This backend lives inside the main Homika Android repository under `backend/`.

Cloudflare Workers Builds should use:
- Production branch: `main`
- Root directory: `backend/`
- Build command: `npm run check`
- Deploy command: `npx wrangler deploy`

Android sources remain under `app/`. Backend deployment is handled by Cloudflare,
not by GitHub Actions.
