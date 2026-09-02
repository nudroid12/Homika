# Homika Property and Booking Flow

Phase 3 turns the local data foundation into the first real owner workflow.

## Property setup

`More -> Properties -> Add property`

Required:

- Property name

Optional:

- Address
- Default nightly rate
- Notes

Existing properties can be edited and marked inactive.

Inactive properties:

- Remain visible in history.
- Cannot be selected for a new booking.
- Remain selectable when editing an old booking that already belongs to them.

## New booking

Primary entry:

`Global + -> New booking`

Fields:

1. Property
2. Guest name
3. Phone
4. Check-in
5. Check-out
6. Booking source
7. Total booking value
8. Notes

Booking data saves to Room through:

`Compose UI -> BookingViewModel -> BookingManager -> Repository -> Room DAO`

## Validation

A booking cannot save when:

- No property is selected.
- Property no longer exists.
- Guest name is empty.
- Check-out is not after check-in.
- Amount is invalid or negative.
- The stay overlaps another non-cancelled booking.
- The stay overlaps a blocked date.

## Date overlap rule

Stay ranges are half-open:

`[check-in, check-out)`

Example:

- Booking A: 10 Sep to 12 Sep
- Booking B: 12 Sep to 14 Sep

This is valid because the second guest checks in on the first guest's check-out day.

The overlap formula is:

`existingStart < newEnd && existingEnd > newStart`

## Booking list

Bookings tab reads directly from local Room data.

Filters:

- All
- Upcoming
- Completed
- Cancelled

Each card shows:

- Guest
- Property
- Date range
- Total
- Booking source
- Booking status

## Booking details

Booking details show the operational record and allow:

- Edit
- Cancel

Cancellation changes status to `CANCELLED`.

It does not delete the booking, preserving history and future reporting accuracy.

Cancelled bookings do not block calendar availability.

## Booking sources

Supported:

- WhatsApp
- Airbnb
- Booking.com
- Facebook
- TikTok
- Repeat guest
- Walk-in
- Other

Source is persisted in the booking record for later analytics.

## Not in Phase 3

These remain intentionally deferred:

- Payment entry
- Deposit handling
- Expense entry
- Calendar live booking rendering
- Block date UI
- Dashboard calculations
- Cloud backup
- Sync
