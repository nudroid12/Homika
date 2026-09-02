# Homika Money and Expense Flow

Phase 6 activates operating expenses and the live Money screen.

## Accounting basis

Homika V1 uses a cash-based operational view.

### Revenue

Revenue for a selected month is:

`sum(payment.amountSen where paymentDate is inside selected month)`

Revenue is based on booking payments actually received.

An unpaid booking balance is not counted as revenue yet.

### Expenses

Expenses for a selected month are:

`sum(expense.amountSen where expenseDate is inside selected month)`

### Net income

`net income = revenue - expenses`

Net income may be negative.

### Security deposit

Security deposits are excluded from:

- Revenue
- Booking paid total
- Expenses
- Net income

This remains a locked accounting rule.

## Expense entry

Entry points:

- Global quick add -> Add Expense
- Money -> Add Expense

Fields:

- Optional property
- Amount
- Date
- Category
- Description
- Notes

Property is optional.

Use General expense for a cost that cannot reasonably be assigned to one homestay.

## Expense categories

- Cleaning
- Electricity
- Water
- Internet
- Supplies
- Maintenance
- Laundry
- Platform fee
- Other

## Monthly Money screen

The owner can move between months.

Each month shows:

- Revenue
- Expenses
- Net income
- Property breakdown
- Expense history

## Property breakdown

Revenue is linked to a property through:

`Payment -> Booking -> Property`

Property expenses are linked directly through:

`Expense -> Property`

General expenses are shown in a separate General expense row.

General expenses are not artificially allocated across properties.

This keeps property figures honest and avoids inventing an allocation formula.

## Date boundaries

Month queries use half-open date ranges:

`[first day of month, first day of next month)`

This matches the date-range convention used elsewhere in Homika.

## Database impact

No schema migration is required.

Phase 6 uses the existing Phase 2 Expenses table and adds only new read queries/projections.
