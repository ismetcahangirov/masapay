import type { Messages } from '@/i18n/messages'
import type az from '../az/payment'

const en: Messages<typeof az> = {
  actions: {
    full: 'Pay in full',
    equal: 'Split equally',
    items: 'My items',
    itemsLockedTitle: 'The bill is already being split equally',
    itemsTitle: 'Pay only for your own items',
    splitStartedNote:
      'The bill is being split equally, so "My items" is closed. Pay the rest with "Split equally" or "Pay in full".',
  },
  pay: {
    redirecting: 'Redirecting to Payriff...',
    payAmount: 'Pay {{amount}}',
    pay: 'Pay',
  },
  full: {
    title: 'Pay in full',
    description: 'You pay everything that is left on the bill.',
  },
  equal: {
    title: 'Split equally',
    started:
      'The bill is split into {{total}} equal shares, {{paid}} already paid.',
    ask: 'How many people do you want to split the bill between?',
    people: 'Number of people',
    partsToPay: 'How many people are you paying for',
    startedHint:
      'The split has started, so the number of people cannot change. {{remaining}} shares are left, you can pay for up to {{remaining}} people.',
    hint: 'If you are also paying for others, increase the number (up to {{remaining}}).',
  },
  items: {
    title: 'Pay for what you ate',
    description: 'Choose only the items you ordered.',
    paid: 'Paid',
    guests: 'How many of you are at the table',
    pickOne: 'Choose at least one item to pay.',
  },
  offer: {
    title: 'Would you like these too?',
    description:
      'Before you pay: if you add one, it goes on the bill and is included in the payment.',
    addedLabel: '{{name}} added',
    addLabel: 'Add {{name}}',
    added: 'Added',
    add: 'Add',
    addFailed: 'Could not add it. The item may no longer be available.',
    goToPayment: 'Go to payment',
    noPay: 'No, pay',
  },
  parts: {
    decrease: '{{label}}: decrease',
    increase: '{{label}}: increase',
    amountDue: 'Amount due',
  },
  tip: {
    title: 'Tip',
    tooBig: 'The tip cannot be more than the part being paid.',
    total: 'Total payment',
    custom: 'Other amount',
    customLabel: 'Tip amount (AZN)',
    invalid: 'Enter a valid amount.',
    tip: 'Tip:',
  },
  errors: {
    offline:
      'No internet connection. Check your connection and try again, no money was taken.',
    payriffUnavailable:
      'The payment system is not responding right now, no money was taken. Please try again a little later.',
    noOnlinePayments:
      'The restaurant is not taking online payments right now. Please ask your waiter.',
    conflict:
      'The bill changed in the meantime (someone may have already paid). Check the amount and try again.',
    badChoice: 'The selection is not valid. Please choose again.',
    generic:
      'The payment failed. Check your internet connection and try again.',
  },
  review: {
    title: 'Rate your experience',
    textNamed:
      'If you enjoyed {{name}}, a 5-star review on Google would help us a lot.',
    text: 'If you enjoyed it, a 5-star review on Google would help us a lot.',
    rate: 'Rate on Google',
    later: 'Later',
  },
  result: {
    checking: {
      title: 'Checking the payment',
      text: 'Waiting for confirmation from Payriff. This may take a few seconds, please do not close the page.',
    },
    success: {
      title: 'Payment completed',
      text: 'Thank you! Your bill has been updated.',
    },
    failed: {
      title: 'Payment did not go through',
      text: 'No money was taken from your card. You can go back to the table and try again.',
    },
    slow: {
      title: 'Confirmation is taking longer',
      text: 'Payriff has not answered yet. If the payment went through, the bill will update shortly. Check again in a few minutes.',
    },
    offline: {
      title: 'Internet connection lost',
      text: 'We could not check the payment status. Restore your connection and check again, your payment is not lost.',
    },
    unknown: {
      title: 'Payment not found',
      text: 'This link is not valid. Scan the QR code on the table again.',
    },
    amount: 'Amount',
    retry: 'Check again',
    back: 'Back to the bill',
  },
  mock: {
    badge: 'Test payment page (stands in for Payriff)',
    description: 'No real card needed. Choose the outcome.',
    approve: 'Pay (success)',
    decline: 'Decline (failure)',
    failed:
      'The answer was not sent. The backend must run with MASAPAY_PAYRIFF_MOCK=true.',
  },
}

export default en
