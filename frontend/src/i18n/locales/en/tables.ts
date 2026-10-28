import type { Messages } from '@/i18n/messages'
import type az from '../az/tables'

const en: Messages<typeof az> = {
  page: {
    title: 'Tables',
    subtitle:
      'Every table has its own QR code. Customers scan it to see the bill and pay.',
    preparing: 'Preparing...',
    downloadAll: 'Download all (ZIP)',
    zipFileName: '{{name}}-qr-codes.zip',
    zipFallbackName: 'tables',
    newTablePlaceholder: 'New table name, e.g. Table 7',
    newTableLabel: 'New table name',
    add: 'Add',
    duplicate: 'A table with this name already exists.',
    createFailed: 'Could not add the table.',
    zipFailed: 'Could not download the ZIP.',
    loading: 'Loading',
    loadFailed: 'Could not load the tables.',
    empty: 'No tables yet.',
    emptyHint: ' Add your first table above.',
  },
  qrCard: {
    qrAlt: '{{label}} QR code',
    open: 'Open',
    serviceCharge: 'Service charge: {{value}}',
    editLabel: '{{label}} service charge',
    edit: 'Change',
  },
  serviceCharge: {
    none: 'None',
    restaurant: '{{percent}} (restaurant)',
    thisTable: '{{percent}} (this table)',
  },
  serviceChargeDialog: {
    title: 'Service charge',
    description:
      '{{label}}: how much service charge the bills at this table get. Applies when a new line is added.',
    restaurant: "Restaurant's",
    own: 'Custom percentage',
    none: 'None',
    percentLabel: 'Percentage (%)',
    percentInvalid:
      'Enter a number above 0 and up to 100, for example 15 or 12.5',
    saveFailed: 'Could not save.',
    cancel: 'Cancel',
    saving: 'Saving...',
    save: 'Save',
  },
}

export default en
