import type { Messages } from '@/i18n/messages'
import type az from '../az/customer'

const en: Messages<typeof az> = {
  orderStatus: {
    open: 'Open',
    awaiting_payment: 'Awaiting payment',
    partially_paid: 'Partially paid',
    paid: 'Paid',
    cancelled: 'Cancelled',
  },
  kitchenStatus: {
    NEW: 'Received',
    PREPARING: 'Preparing',
    READY: 'Ready',
  },
  orderItemList: {
    paid: 'Paid',
    empty: 'Nothing has been ordered yet.',
  },
  billCard: {
    equalSplitPaid: 'Equal split: {{paid}} / {{total}} shares paid',
    byItems: 'By item (crossed out)',
    equalSplitParts: 'Equal split ({{paid}} / {{total}} shares)',
    otherPayments: 'Other payments',
    paidProgress: 'Paid share',
    fullyPaid: 'The bill is fully paid. Thank you!',
    cancelled:
      'The order was cancelled by the restaurant. No payment is needed.',
    items: 'Items',
    serviceCharge: 'Service charge',
    serviceChargePercent: 'Service charge ({{percent}}%)',
    total: 'Total',
    paid: 'Paid',
    balance: 'Remaining',
  },
  billStates: {
    loading: 'Loading the bill',
    notFoundTitle: 'Table not found',
    errorTitle: 'Could not load the bill',
    notFoundDescription:
      'This QR code does not belong to any table. Please scan the QR code on your table again.',
    errorDescription: 'Check your internet connection and try again.',
    retry: 'Try again',
    noOpenBill:
      'There is no open bill at this table yet. It will appear here once an order is placed.',
  },
  confirmOrder: {
    title: 'Are you sure?',
    description: "These items will be added to the table's bill.",
    total: 'Total',
    warning: 'Once confirmed, the order cannot be removed.',
    back: 'Go back',
    sending: 'Sending...',
    confirm: 'Yes, confirm',
  },
  dish: {
    add: 'Add',
    addAria: 'Add {{name}}',
    decreaseAria: 'Decrease {{name}}',
    quantityAria: '{{name}} quantity',
    increaseAria: 'Increase {{name}}',
    aboutAria: 'About {{name}}',
    combo: 'Combo',
    ingredients: 'Main ingredients',
  },
  menuBrowser: {
    title: 'Menu',
    description: 'Pick a category, add dishes, then confirm your order.',
    loading: 'Loading',
    failed: 'Could not load the menu.',
    empty: 'There is nothing to order on the menu today.',
    categories: 'Categories',
    picked: 'Selected',
    nothingPicked: 'Nothing selected yet.',
    decreasePickedAria: 'Decrease selected {{name}}',
    increasePickedAria: 'Increase selected {{name}}',
    removeAria: 'Remove {{name}} from the basket',
    subtotal: 'Subtotal',
    confirm: 'Confirm order ({{amount}})',
  },
  menuOrder: {
    restaurantClosed:
      'The restaurant is not taking online orders right now. Please ask your waiter.',
    conflict:
      'Some items are no longer available. The menu has been updated, please check your basket.',
    failed: 'Could not send the order. Please try again.',
    title: 'Menu and order',
    description:
      "Choose, review and confirm. The order goes onto the table's bill.",
    cancelHint: 'To cancel a confirmed order, ask the restaurant manager.',
    newRound: 'Order more',
    ordered: "Your order was added to the table's bill.",
    menu: 'Menu',
    basket: 'Basket',
    removeAria: 'Remove {{name}} from the basket',
    confirm: 'Confirm order ({{amount}})',
  },
  upsell: {
    askOne: 'Would you like {{name}}?',
    askDefault: 'an extra',
    askMany: 'Would you like {{names}} or {{last}}?',
    title: 'Would you like this too?',
    askedDescription:
      'Choose a quantity if you like, it will be added to the order.',
    description: 'Goes well with what you picked.',
    addedAria: '{{name}} added',
    addAria: 'Add {{name}}',
    added: 'Added',
    add: 'Add',
    free: 'Free',
    decreaseAria: 'Decrease {{name}}',
    quantityAria: '{{name}} quantity',
    increaseAria: 'Increase {{name}}',
    continue: 'Continue',
    noThanks: 'No, thanks',
  },
  tablePage: {
    payment: 'Payment',
  },
  invalidToken: {
    missingTitle: 'QR code not recognised',
    missingDescription:
      'This link does not carry table details. Please scan the QR code on your table again.',
    malformedTitle: 'Invalid link',
    malformedDescription:
      'The QR code was not read correctly. Please scan the QR code on your table again.',
    help: 'If you need help, please contact a member of staff.',
  },
  notFound: {
    title: 'Page not found',
    description:
      'The page you are looking for does not exist. Start by scanning the QR code on your table.',
  },
}

export default en
