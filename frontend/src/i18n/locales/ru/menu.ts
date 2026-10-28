import type { Messages } from '@/i18n/messages'
import type az from '../az/menu'

const ru: Messages<typeof az> = {
  actions: {
    cancel: 'Отмена',
    save: 'Сохранить',
    saving: 'Сохранение...',
    saveFailed: 'Не удалось сохранить.',
  },
  page: {
    title: 'Меню',
    subtitle: 'Категории и блюда. Меню для гостей строится из этого списка.',
    importMenu: 'Загрузить меню из файла',
    addCategory: 'Добавить категорию',
    loading: 'Загрузка',
    loadFailed: 'Не удалось загрузить меню.',
    empty: 'Меню пока пусто.',
    emptyHint:
      ' Добавьте первую категорию, затем блюда в неё, или загрузите всё меню из файла PDF/Word.',
    addDish: 'Блюдо',
    addCombo: 'Комбо',
    editCategory: 'Изменить категорию {{name}}',
    deleteCategory: 'Удалить категорию {{name}}',
    noDishes: 'В этой категории нет блюд.',
    comboBadge: 'Комбо',
    askBadge: 'Спрашивается перед подтверждением',
    ingredients: 'Состав:',
    suggestions: 'Предложение:',
    availability: 'Доступность: {{name}}',
    extras: 'Дополнительные предложения: {{name}}',
    ask: 'Спрашивать про {{name}} перед подтверждением',
    askTitle: 'Спрашивать каждого гостя перед подтверждением (соус, вода)',
    editDish: 'Изменить {{name}}',
    deleteDish: 'Удалить {{name}}',
    deleteFailed: 'Не удалось удалить.',
    categoryHasDishes:
      'В этой категории есть блюда. Сначала удалите их или перенесите в другую категорию.',
    dishInCombo: 'Это блюдо входит в комбо. Сначала уберите его из комбо.',
    deleteCategoryTitle: 'Удалить категорию',
    deleteDishTitle: 'Удалить блюдо',
    deleteCategoryText: 'Категория «{{name}}» будет удалена. Вы уверены?',
    deleteDishText: '«{{name}}» будет удалено из меню. Вы уверены?',
  },
  availability: {
    off: 'Выключено',
    offToday: 'Сегодня нет',
    comboPartOff: 'В составе есть блюдо, которого сегодня нет',
  },
  availabilityDialog: {
    title: 'Меню на день',
    description: '{{name}}: когда гости могут видеть и заказывать его.',
    mode: 'Доступность',
    modes: {
      ALWAYS: 'Всегда в меню',
      OFF_TODAY: 'Выключить только на сегодня',
      DATES: 'Только в эти даты',
      OFF: 'Выключить (пока не включите снова)',
    },
    offTodayHint: 'Завтра блюдо само вернётся в меню.',
    from: 'Начало',
    until: 'Последний день',
    datesHint:
      'Оба дня включены. Если одно поле пустое, с этой стороны ограничения нет.',
    saveFailed: 'Не удалось сохранить. Проверьте даты.',
  },
  categoryDialog: {
    editTitle: 'Изменить категорию',
    newTitle: 'Новая категория',
    name: 'Название',
    namePlaceholder: 'Например, Салаты',
    order: 'Порядок',
    orderHint: 'Меньшее число показывается выше в меню.',
    duplicate: 'Категория с таким названием уже есть.',
  },
  deleteDialog: {
    deleting: 'Удаление...',
    delete: 'Удалить',
  },
  dishDialog: {
    editCombo: 'Изменить комбо',
    newCombo: 'Новое комбо',
    editDish: 'Изменить блюдо',
    newDish: 'Новое блюдо',
    category: 'Категория',
    categoryPlaceholder: 'Выберите категорию',
    photo: 'Фото (необязательно)',
    photoFallbackLabel: 'Блюдо',
    name: 'Название',
    comboPlaceholder: 'Например, Бизнес-ланч',
    dishPlaceholder: 'Например, Салат Цезарь',
    contents: 'Состав',
    noChoices: 'Сначала добавьте в меню обычные блюда.',
    decrease: 'Уменьшить: {{name}}',
    count: 'Количество: {{name}}',
    increase: 'Увеличить: {{name}}',
    comboHint:
      'Выберите для комбо хотя бы одно блюдо. Цену вы задаёте сами ниже.',
    ingredients: 'Основной состав (необязательно)',
    ingredientsPlaceholder: 'курица, рис, шафран',
    ingredientsHint: 'Разделяйте запятыми. Гость видит это в меню под блюдом.',
    description: 'Описание (необязательно)',
    descriptionPlaceholder: 'Порция, способ приготовления и т. д.',
    price: 'Цена (AZN)',
    priceInvalid:
      'Укажите цену числом, не более 2 знаков после запятой: например 8.50',
    photoFailed:
      'Блюдо сохранено, но фото не загрузилось. Выберите фото JPEG, PNG или WebP и попробуйте снова.',
  },
  photoField: {
    change: 'Заменить фото: {{name}}',
    choose: 'Выбрать фото: {{name}}',
    changeButton: 'Заменить фото',
    chooseButton: 'Выбрать фото',
    remove: 'Удалить фото: {{name}}',
  },
  suggestionsDialog: {
    title: 'Дополнительные предложения',
    description:
      'Гостю, выбравшему {{name}}, перед подтверждением предлагается это. Не более {{max}}, в выбранном вами порядке.',
    picked: 'Выбранные предложения',
    selected: 'Выбрано',
    offer: 'Предложить',
  },
  importDialog: {
    title: 'Загрузить меню из файла',
    done: 'Меню обновлено.',
    reviewHint:
      'Проверьте найденное: исправьте названия, цены и фото, удалите лишнее, затем подтвердите. До подтверждения ничего не сохраняется.',
    intro:
      'Мы читаем текст из файла PDF, Word (DOCX, DOC) или с фото меню (JPG, PNG, скан). Затем вы сможете проверить и исправить каждую строку.',
    result: '{{created}}, {{updated}}, {{categories}}.',
    dishesCreated_one: '{{count}} блюдо добавлено',
    dishesCreated_few: '{{count}} блюда добавлено',
    dishesCreated_many: '{{count}} блюд добавлено',
    dishesCreated_other: '{{count}} блюда добавлено',
    dishesUpdated_one: '{{count}} блюдо обновлено',
    dishesUpdated_few: '{{count}} блюда обновлено',
    dishesUpdated_many: '{{count}} блюд обновлено',
    dishesUpdated_other: '{{count}} блюда обновлено',
    categoriesCreated_one: '{{count}} новая категория',
    categoriesCreated_few: '{{count}} новые категории',
    categoriesCreated_many: '{{count}} новых категорий',
    categoriesCreated_other: '{{count}} новой категории',
    close: 'Закрыть',
    reading: 'Чтение файла...',
    chooseFile: 'Выберите файл или фото',
    tips: 'Лучший результат: каждое блюдо на одной строке с названием и ценой (например «Салат Цезарь ... 9 AZN»), названия категорий на отдельных строках. На сканах и фото текст должен быть чётким и ровным; их чтение может занять до минуты. Не более 10 МБ.',
    ocrNotice:
      'Текст распознан с изображения (OCR). Буквы и цифры могут быть прочитаны с ошибками: особенно сверьте цены с файлом.',
    skippedImages_one:
      '{{count}} изображение в файле не прочитано (не JPEG, PNG или WebP). Вы можете добавить фото ниже сами.',
    skippedImages_few:
      '{{count}} изображения в файле не прочитаны (не JPEG, PNG или WebP). Вы можете добавить фото ниже сами.',
    skippedImages_many:
      '{{count}} изображений в файле не прочитаны (не JPEG, PNG или WebP). Вы можете добавить фото ниже сами.',
    skippedImages_other:
      '{{count}} изображения в файле не прочитаны (не JPEG, PNG или WebP). Вы можете добавить фото ниже сами.',
    categorySection: 'Категория {{name}}',
    categoryName: 'Название категории',
    existingCategory: 'Существующая категория',
    removeCategory: 'Убрать категорию',
    dishFallback: 'Блюдо',
    dishName: 'Название блюда',
    onMenu: 'Уже в меню, будет обновлено',
    removeDish: 'Убрать {{name}}',
    ingredients: 'Основной состав',
    description: 'Описание',
    price: 'Цена (AZN)',
    addDish: 'Добавить блюдо',
    newCategory: 'Новая категория',
    addCategory: 'Добавить категорию',
    otherFile: 'Другой файл',
    confirm_one: 'Подтвердить ({{count}} блюдо)',
    confirm_few: 'Подтвердить ({{count}} блюда)',
    confirm_many: 'Подтвердить ({{count}} блюд)',
    confirm_other: 'Подтвердить ({{count}} блюда)',
    rowProblems: {
      noName: 'Введите название',
      badPrice: 'Укажите цену числом',
      duplicate: 'Это название встречается дважды',
    },
    applyErrors: {
      duplicateDish:
        'Есть два блюда с одинаковым названием. Измените одно из них.',
      invalidImage:
        'Одно из фото не удалось прочитать. Удалите его или выберите другое.',
    },
    previewErrors: {
      tooLarge: 'Файл больше 10 МБ.',
      unsupported:
        'Этот тип файла не читается. Выберите файл PDF, DOCX, DOC или изображение JPG, PNG.',
      busy: 'Сервер сейчас читает другие сканы. Попробуйте чуть позже.',
      noText:
        'В файле не найден читаемый текст. Если это скан или фото, загрузите более чёткую, ровную и светлую копию.',
      ocrUnavailable:
        'Чтение сканов и фото на этом сервере не настроено. Выберите PDF с текстом или файл Word.',
      ocrTimeout:
        'Чтение скана заняло слишком много времени. Загрузите файл с меньшим числом страниц или меньшего размера.',
      noDishes:
        'В файле не найдено блюд с ценами. Выберите другой файл или добавьте блюда по одному.',
      protected: 'Файл защищён паролем. Загрузите копию без пароля.',
      tooManyPages:
        'Файл слишком длинный: PDF с текстом может содержать не более 60 страниц, скан не более 15.',
      failed: 'Не удалось прочитать файл.',
    },
  },
}

export default ru
