Homika Pro Patch 13A.2.1

Corrected full replacement of Patch 13A.2.

Fix:
- Removes one surplus closing brace in LicenseActivationScreen.kt that caused:
  Syntax error: Expecting a top level declaration at line 322.

Includes all intended 13A.2 changes:
- Activation UI contrast/theme corrections.
- Safe status/navigation bar padding.
- Trial-specific response/error isolation.
- Worker v11 trial response contract 2.

No D1 migration.
No sync, backup, updater workflow, or signing changes.
