// Every text of the app, per language and namespace (one namespace per area).
import az_common from './locales/az/common'
import az_customer from './locales/az/customer'
import az_payment from './locales/az/payment'
import az_admin from './locales/az/admin'
import az_auth from './locales/az/auth'
import az_dashboard from './locales/az/dashboard'
import az_orders from './locales/az/orders'
import az_landing from './locales/az/landing'
import az_menu from './locales/az/menu'
import az_platform from './locales/az/platform'
import az_staff from './locales/az/staff'
import az_settings from './locales/az/settings'
import az_tables from './locales/az/tables'
import az_tips from './locales/az/tips'
import az_transactions from './locales/az/transactions'
import en_common from './locales/en/common'
import en_customer from './locales/en/customer'
import en_payment from './locales/en/payment'
import en_admin from './locales/en/admin'
import en_auth from './locales/en/auth'
import en_dashboard from './locales/en/dashboard'
import en_orders from './locales/en/orders'
import en_landing from './locales/en/landing'
import en_menu from './locales/en/menu'
import en_platform from './locales/en/platform'
import en_staff from './locales/en/staff'
import en_settings from './locales/en/settings'
import en_tables from './locales/en/tables'
import en_tips from './locales/en/tips'
import en_transactions from './locales/en/transactions'
import ru_common from './locales/ru/common'
import ru_customer from './locales/ru/customer'
import ru_payment from './locales/ru/payment'
import ru_admin from './locales/ru/admin'
import ru_auth from './locales/ru/auth'
import ru_dashboard from './locales/ru/dashboard'
import ru_orders from './locales/ru/orders'
import ru_landing from './locales/ru/landing'
import ru_menu from './locales/ru/menu'
import ru_platform from './locales/ru/platform'
import ru_staff from './locales/ru/staff'
import ru_settings from './locales/ru/settings'
import ru_tables from './locales/ru/tables'
import ru_tips from './locales/ru/tips'
import ru_transactions from './locales/ru/transactions'

const az = {
  common: az_common,
  customer: az_customer,
  payment: az_payment,
  admin: az_admin,
  auth: az_auth,
  dashboard: az_dashboard,
  orders: az_orders,
  landing: az_landing,
  menu: az_menu,
  platform: az_platform,
  staff: az_staff,
  settings: az_settings,
  tables: az_tables,
  tips: az_tips,
  transactions: az_transactions,
}

const en = {
  common: en_common,
  customer: en_customer,
  payment: en_payment,
  admin: en_admin,
  auth: en_auth,
  dashboard: en_dashboard,
  orders: en_orders,
  landing: en_landing,
  menu: en_menu,
  platform: en_platform,
  staff: en_staff,
  settings: en_settings,
  tables: en_tables,
  tips: en_tips,
  transactions: en_transactions,
}

const ru = {
  common: ru_common,
  customer: ru_customer,
  payment: ru_payment,
  admin: ru_admin,
  auth: ru_auth,
  dashboard: ru_dashboard,
  orders: ru_orders,
  landing: ru_landing,
  menu: ru_menu,
  platform: ru_platform,
  staff: ru_staff,
  settings: ru_settings,
  tables: ru_tables,
  tips: ru_tips,
  transactions: ru_transactions,
}

export const resources = { az, en, ru }
