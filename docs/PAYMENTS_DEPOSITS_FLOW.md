# Homika Payments and Deposits Flow

Phase 5 activates the Payment and Deposit tables created in Phase 2.

## Payment flow

Entry points:

- Global quick add -> Record Payment
- Booking Details -> Record Payment

Global entry first shows active bookings with an outstanding balance.

Payment record fields:

- Booking
- Amount
- Payment date
- Method
- Notes

Supported methods:

- Cash
- Bank transfer
- E-wallet
- Card
- Booking platform
- Other

## Payment calculation

Only `payments` records count toward the booking paid amount.

Formula:

`paid = sum(non-deleted payment.amountSen)`

`outstanding = max(booking.totalAmountSen - paid, 0)`

A new payment is rejected when:

- Booking does not exist
- Booking is cancelled
- Amount is zero or negative
- Booking is already fully paid
- Amount exceeds outstanding balance

Multiple payment records are supported.

Example:

Booking total RM500
- Payment 1 RM100
- Payment 2 RM200

Paid = RM300
Outstanding = RM200

## Payment history

Booking Details displays all payment records for that booking.

Payment history is business history and is not merged into the booking row.

## Security deposit principle

Security deposit is never booking revenue and never counts as a booking payment.

The Deposit table is independent from Payment.

This prevents:

- Deposit receipt from falsely marking a booking paid
- Deposit receipt from inflating revenue
- Deposit returns from appearing as operating expenses

## Deposit states

Supported lifecycle:

`Not Required -> Pending -> Received -> Partially Returned -> Returned`

or:

`Received / Partially Returned -> Retained`

### Not Required

No security deposit is expected.

### Pending

A required deposit amount has been set but not yet received.

### Received

The full required deposit has been received.

### Partially Returned

Some of the deposit has been returned.

### Returned

The full deposit has been returned.

### Retained

The remaining deposit has been retained.

## Deposit calculations

`remaining = max(deposit amount - returned amount, 0)`

A return cannot exceed the remaining deposit.

Retaining the deposit preserves any amount already returned and marks the remaining balance as retained.

## Data safety

No database migration is required for Phase 5.

The existing Phase 2 Payment and Deposit tables are used unchanged.

Money remains stored in integer sen using `Long`.
