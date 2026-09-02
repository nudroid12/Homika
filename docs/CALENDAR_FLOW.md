# Homika Calendar Flow

Phase 4 connects the calendar to real Room data.

## Main calendar

The Calendar tab now reads:

- Properties
- Active non-cancelled bookings in the visible month
- Blocked date ranges in the visible month

Data path:

`CalendarScreen -> CalendarViewModel -> Repositories -> Room`

## Property filter

The owner can view:

- All properties
- One specific property

The month grid and selected-day agenda both respect the filter.

## Month cells

A date can visually indicate:

- Booking
- Blocked period
- Today
- Current selected date

Cancelled bookings do not appear as occupied because the booking range query excludes them.

## Selected date agenda

Tapping a date selects it.

The area below the calendar shows all bookings and blocked periods covering that date.

Tapping a booking opens Booking Details.

## Create booking from calendar

The Book action uses the selected date as the initial check-in.

When a specific property is selected, that property is also preselected in the New Booking form.

The BookingManager still performs the final overlap validation before save.

## Block date

Block Date is now a real flow.

Entry points:

- Global quick add -> Block date
- Calendar selected date -> Block

Fields:

- Property
- First blocked date
- Last blocked date
- Optional reason

The UI presents the final date inclusively to the owner. Storage converts it to an exclusive end date to preserve the same half-open range rule used by bookings.

Examples:

- Block 10 Sep only -> stored `[10 Sep, 11 Sep)`
- Block 10-12 Sep -> stored `[10 Sep, 13 Sep)`

A block is rejected if it overlaps:

- An active booking
- Another blocked range

## Multi-property availability

All Properties provides a portfolio overview.

For exact booking creation, the booking form still requires one property. This avoids guessing which homestay the owner intended to book.

## Phase boundary

Phase 4 does not implement:

- Payments
- Deposits
- Expenses
- Revenue calculations
- Occupancy percentage
- Cloud backup
- Sync

Those remain in their planned phases.
