/** Same name the shadcn sidebar writes when it is opened or collapsed (#184). */
const SIDEBAR_COOKIE = 'sidebar_state'

/** Whether the sidebar was left open; open the first time. */
export function sidebarWasOpen(cookie: string = document.cookie): boolean {
  const match = cookie
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith(`${SIDEBAR_COOKIE}=`))
  return match ? match.slice(SIDEBAR_COOKIE.length + 1) !== 'false' : true
}
