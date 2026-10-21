// Google OAuth client id for the admin login (#17), from frontend/.env.local:
//   VITE_GOOGLE_CLIENT_ID=<id>.apps.googleusercontent.com
// It must be the same id the backend checks (MASAPAY_GOOGLE_CLIENT_ID).
export const googleClientId: string =
  import.meta.env.VITE_GOOGLE_CLIENT_ID ?? ''
