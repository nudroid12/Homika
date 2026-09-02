# Homika Dashboard and Reports

Phase 7 activates the operational Home dashboard and business reports.

## Home dashboard

Home now reads real Room-backed data.

Current-month metrics:

- Revenue
- Expenses
- Net income
- Occupancy

Operational sections:

- Check-ins today
- Check-outs today
- Outstanding booking balances
- Upcoming bookings

Tapping an operational booking opens Booking Details.

## Revenue

Financial reporting remains cash-based.

`Revenue = payments received inside report period`

Booking balances that have not been paid are not revenue.

## Expenses

`Expenses = expenses dated inside report period`

## Net income

`Net income = Revenue - Expenses`

Security deposits remain excluded from both revenue and expenses.

## Occupancy

Occupancy is based on sellable nights.

For the report period:

`gross capacity = eligible properties × calendar nights`

`available nights = gross capacity - blocked nights`

`occupancy = booked nights / available nights × 100`

Rules:

- Cancelled bookings do not occupy nights.
- Booking nights are clipped to the report period.
- Blocked nights reduce available capacity.
- The result is clamped between 0% and 100%.
- Properties that are active, or have booking/block activity in the period, are eligible.

This avoids treating owner-blocked or maintenance dates as sellable capacity.

## Booking source analytics

Source analytics uses bookings whose check-in date is inside the selected report period.

For each source Homika shows:

- Booking count
- Booked value

Booked value is the booking total, not cash revenue.

This distinction is intentional:

- Revenue tells the owner what cash was received.
- Booked value tells the owner what demand was generated.
- Occupancy tells the owner how much sellable inventory was used.

## Monthly reports

Owner can move between months and return to the current month.

Monthly report contains:

- Revenue
- Expenses
- Net income
- Occupancy
- Booking count
- Booked value
- Average booking value
- Booked nights
- Available nights
- Booking source breakdown

## Yearly reports

Owner can move between calendar years and return to the current year.

The yearly report uses the same formulas across the entire calendar year.

## Sharing reports

Reports can be shared as a lightweight text summary using Android's system share sheet.

No third-party export or cloud service is required.

## Phase boundary

Phase 7 does not add:

- Google Drive backup
- Account sign-in
- Multi-device sync
- App lock

Those remain in later roadmap phases.
