HOMIKA PRO PATCH 13C.4 - APPROVAL COMPLETION UX

Purpose
- Make Admin Approve/Reject visibly complete with a confirmation popup.
- Make Licence Key delivery obvious for fresh purchases.
- Keep upgrade/renewal on the same existing licence.
- Keep approved/rejected history accessible in Admin Dashboard.

Files
- backend/src/index.js
- store/admin.html
- store/admin.css
- store/admin.js
- store/app.js
- store/styles.css
- PROJECT-CONTEXT.md

No D1 migration.
No Android changes.
No secret values included.

After deploy
1. Approve a fresh-purchase submission.
2. Confirm Admin success popup shows Licence Key + customer link.
3. Confirm customer status page shows the same Licence Key.
4. Test Trial upgrade/renewal and confirm it keeps the same licence and asks customer to Verify Now.
