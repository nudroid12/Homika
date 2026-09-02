# HOMIQ

HOMIQ is a private, owner-only homestay management app.

It is designed for homestay owners who receive bookings manually through WhatsApp, Airbnb, Booking.com, Facebook, TikTok, repeat guests, walk-ins, or other channels, then need one simple place to record bookings, payments, deposits, expenses, occupancy, and profit.

## Product principles

- Owner only. This is not a guest booking marketplace.
- Zero recurring operating cost is a core requirement.
- Local first. Daily use must continue even without internet.
- Bahasa Melayu and English.
- Optional account sign-in.
- Local backup and Google Drive backup.
- Optional multi-device sync for the same account.
- Simple enough for one homestay, structured enough for multiple properties.
- Financial records must separate revenue, outstanding balances, deposits, refunds, and expenses.

## Initial Android stack

- Kotlin
- Jetpack Compose
- Material 3
- Android Gradle Plugin 9.3.2
- Gradle 9.5.0
- compileSdk 36
- targetSdk 36
- minSdk 26
- Compose BOM 2026.06.00
- JDK 17

## Repository source of truth

Read `PROJECT_CONTEXT.md` before making product or architecture changes.

The complete end-to-end roadmap and flowcharts are in `docs/FLOWCHART.md`.

## Current stage

Phase 0: Foundation.

The initial project only proves that the Android toolchain and bilingual resource structure build successfully. Business features are intentionally added phase by phase.
