# Homika UI/UX Rebuild

## Goal

Rebuild Homika as a production-quality private homestay operations app while preserving the proven local-first data, booking, finance, backup, Google Drive sync and app-lock engines.

## Design system

- Brand: Homika
- Mark: mint/teal geometric home + H on deep dark background
- Visual character: calm, premium, operational, modern hospitality
- Primary: deep teal
- Accent: mint
- Surfaces: warm off-white in light mode; deep green-black in dark mode
- Cards: restrained outlines rather than heavy elevation
- Radius: 10–24 dp depending on hierarchy
- Typography: compact, high legibility, no oversized demo-style headings
- Main horizontal margins: 16 dp

## Navigation

Five fixed destinations remain Home, Calendar, Bookings, Money and More. The bottom bar is custom-built so labels stay on one line on narrow phones and under larger display/font scaling. Quick Add remains globally available.

## First-run experience

1. Welcome / brand
2. Malay or English
3. Optional Google Drive connection (same-account sync)
4. Optional local PIN
5. Add first homestay or enter dashboard

The onboarding completion flag is local-only and does not change database schema.

## Rebuilt surfaces in this pass

- App-wide design tokens, typography and shapes
- Launcher/brand mark
- Main navigation
- Welcome/onboarding
- Home dashboard
- Bookings list
- Money overview
- More/settings
- Shared headers, metrics, empty states, information cards and picker fields
- Spacing normalization on remaining screens

## Compatibility

The package remains `com.homiq.app`. Room schema stays version 1. Existing backup/sync identifiers are intentionally retained where required for compatibility.

## Responsive Fix 2 — Single-page entry and safe navigation

- Replaced the multi-step first-run wizard with one professional entry screen.
- The entry screen places the approved Homika logo centrally, with an `EN | MY` language switch in the top-right.
- `Continue with Google` now completes Google Drive authorization and the first sync before entering Home.
- `Continue without account` enters the local-first app immediately.
- Optional app-lock PIN setup remains on the same page; enabling the checkbox reveals PIN and confirmation fields inline.
- First-run state now uses `complete_v2` so existing development installs see the redesigned entry experience once.
- Replaced the custom bottom navigation implementation with Material 3 `NavigationBar` / `NavigationBarItem`, which handles edge-to-edge navigation-bar insets correctly.
- Main navigation labels are constrained to a single line for narrow phones and larger display scaling.
- Calendar month grids now render only the number of week rows actually required by the month instead of forcing six rows.
- Calendar day cells are square and the Today action is compact, reducing unnecessary vertical space.
- Main-screen end padding was normalized so the global add FAB remains clear without excessive blank space.
