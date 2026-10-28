import type { Messages } from '@/i18n/messages'
import type az from '../az/menu'

const en: Messages<typeof az> = {
  actions: {
    cancel: 'Cancel',
    save: 'Save',
    saving: 'Saving...',
    saveFailed: 'Could not save.',
  },
  page: {
    title: 'Menu',
    subtitle:
      'Categories and dishes. The customer menu is built from this list.',
    importMenu: 'Import menu from a file',
    addCategory: 'Add category',
    loading: 'Loading',
    loadFailed: 'Could not load the menu.',
    empty: 'The menu is empty so far.',
    emptyHint:
      ' Add the first category, then dishes to it, or import the whole menu from a PDF/Word file.',
    addDish: 'Dish',
    addCombo: 'Combo',
    editCategory: 'Edit category {{name}}',
    deleteCategory: 'Delete category {{name}}',
    noDishes: 'There are no dishes in this category.',
    comboBadge: 'Combo',
    askBadge: 'Asked before confirming',
    ingredients: 'Ingredients:',
    suggestions: 'Suggested:',
    availability: '{{name}} availability',
    extras: '{{name}} extra suggestions',
    ask: 'Ask about {{name}} before confirming',
    askTitle: 'Ask every customer before confirming (sauce, water)',
    editDish: 'Edit {{name}}',
    deleteDish: 'Delete {{name}}',
    deleteFailed: 'Could not delete.',
    categoryHasDishes:
      'This category has dishes. Delete them or move them to another category first.',
    dishInCombo:
      'This dish is part of a combo. Remove it from the combo first.',
    deleteCategoryTitle: 'Delete category',
    deleteDishTitle: 'Delete dish',
    deleteCategoryText:
      'The category "{{name}}" will be deleted. Are you sure?',
    deleteDishText: '"{{name}}" will be removed from the menu. Are you sure?',
  },
  availability: {
    off: 'Switched off',
    offToday: 'Not available today',
    comboPartOff: 'Contains a dish not available today',
  },
  availabilityDialog: {
    title: 'Daily menu',
    description: '{{name}}: when customers can see and order it.',
    mode: 'Availability',
    modes: {
      ALWAYS: 'Always on the menu',
      OFF_TODAY: 'Switch off for today only',
      DATES: 'Only on these dates',
      OFF: 'Switch off (until switched back on)',
    },
    offTodayHint: 'It comes back on the menu by itself tomorrow.',
    from: 'Start',
    until: 'Last day',
    datesHint:
      'Both days are included. If one is left empty, there is no limit on that side.',
    saveFailed: 'Could not save. Check the dates.',
  },
  categoryDialog: {
    editTitle: 'Edit category',
    newTitle: 'New category',
    name: 'Name',
    namePlaceholder: 'For example, Salads',
    order: 'Order',
    orderHint: 'A smaller number is shown higher on the menu.',
    duplicate: 'A category with this name already exists.',
  },
  deleteDialog: {
    deleting: 'Deleting...',
    delete: 'Delete',
  },
  dishDialog: {
    editCombo: 'Edit combo',
    newCombo: 'New combo',
    editDish: 'Edit dish',
    newDish: 'New dish',
    category: 'Category',
    categoryPlaceholder: 'Choose a category',
    photo: 'Photo (optional)',
    photoFallbackLabel: 'Dish',
    name: 'Name',
    comboPlaceholder: 'For example, Business lunch',
    dishPlaceholder: 'For example, Caesar salad',
    contents: 'Contents',
    noChoices: 'Add regular dishes to the menu first.',
    decrease: 'Decrease {{name}}',
    count: '{{name}} quantity',
    increase: 'Increase {{name}}',
    comboHint:
      'Choose at least one dish for the combo. You set its price yourself below.',
    ingredients: 'Main ingredients (optional)',
    ingredientsPlaceholder: 'chicken, rice, saffron',
    ingredientsHint:
      'Separate with commas. Customers see them under the dish on the menu.',
    description: 'Description (optional)',
    descriptionPlaceholder: 'Portion, cooking method, etc.',
    price: 'Price (AZN)',
    priceInvalid:
      'Enter the price as a number, at most 2 decimals: for example 8.50',
    photoFailed:
      'The dish was saved, but the photo was not. Choose a JPEG, PNG or WebP photo and try again.',
  },
  photoField: {
    change: 'Change the photo of {{name}}',
    choose: 'Choose a photo for {{name}}',
    changeButton: 'Change photo',
    chooseButton: 'Choose photo',
    remove: 'Remove the photo of {{name}}',
  },
  suggestionsDialog: {
    title: 'Extra suggestions',
    description:
      'Customers who choose {{name}} are offered these before confirming. At most {{max}}, in the order you pick them.',
    picked: 'Picked suggestions',
    selected: 'Picked',
    offer: 'Suggest',
  },
  importDialog: {
    title: 'Import menu from a file',
    done: 'The menu has been updated.',
    reviewHint:
      'Check what was found: fix names, prices and photos, remove what you do not need, then confirm. Nothing is saved until you confirm.',
    intro:
      'We read the text in a PDF or Word (DOCX, DOC) file or in a photo of the menu (JPG, PNG, scan). You can then check and fix every line.',
    result: '{{created}}, {{updated}}, {{categories}}.',
    dishesCreated_one: '{{count}} dish added',
    dishesCreated_other: '{{count}} dishes added',
    dishesUpdated_one: '{{count}} dish updated',
    dishesUpdated_other: '{{count}} dishes updated',
    categoriesCreated_one: '{{count}} new category',
    categoriesCreated_other: '{{count}} new categories',
    close: 'Close',
    reading: 'Reading the file...',
    chooseFile: 'Choose a file or photo',
    tips: 'Best results: each dish on one line with its name and price (for example "Caesar salad ... 9 AZN"), category names on their own lines. For scans and photos the text should be clear and straight; reading them can take up to a minute. At most 10 MB.',
    ocrNotice:
      'The text was recognised from an image (OCR). Letters and numbers may be misread: compare the prices with the file in particular.',
    skippedImages_one:
      '{{count}} photo in the file could not be read (not JPEG, PNG or WebP). You can add it yourself below.',
    skippedImages_other:
      '{{count}} photos in the file could not be read (not JPEG, PNG or WebP). You can add them yourself below.',
    categorySection: 'Category {{name}}',
    categoryName: 'Category name',
    existingCategory: 'Existing category',
    removeCategory: 'Remove category',
    dishFallback: 'Dish',
    dishName: 'Dish name',
    onMenu: 'Already on the menu, will be updated',
    removeDish: 'Remove {{name}}',
    ingredients: 'Main ingredients',
    description: 'Description',
    price: 'Price (AZN)',
    addDish: 'Add dish',
    newCategory: 'New category',
    addCategory: 'Add category',
    otherFile: 'Another file',
    confirm_one: 'Confirm ({{count}} dish)',
    confirm_other: 'Confirm ({{count}} dishes)',
    rowProblems: {
      noName: 'Enter a name',
      badPrice: 'Enter the price as a number',
      duplicate: 'This name appears twice',
    },
    applyErrors: {
      duplicateDish: 'Two dishes have the same name. Change one of them.',
      invalidImage:
        'One of the photos could not be read. Remove it or choose another photo.',
    },
    previewErrors: {
      tooLarge: 'The file is larger than 10 MB.',
      unsupported:
        'This file type cannot be read. Choose a PDF, DOCX or DOC file, or a JPG or PNG image.',
      busy: 'The server is reading other scans right now. Try again in a moment.',
      noText:
        'No readable text was found in the file. If it is a scan or photo, upload a clearer, straighter and better lit copy.',
      ocrUnavailable:
        'Reading scanned files and photos is not set up on this server. Choose a PDF with text or a Word file.',
      ocrTimeout:
        'Reading the scan took too long. Upload a file with fewer pages or a smaller file.',
      noDishes:
        'No dishes with prices were found in the file. Choose another file or add dishes one by one.',
      protected:
        'The file is password protected. Upload a copy without a password.',
      tooManyPages:
        'The file is too long: a PDF with text can have at most 60 pages, a scan at most 15.',
      failed: 'Could not read the file.',
    },
  },
}

export default en
