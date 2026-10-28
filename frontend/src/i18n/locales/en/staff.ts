import type { Messages } from '@/i18n/messages'
import type az from '../az/staff'

const en: Messages<typeof az> = {
  roles: {
    WAITER: 'Waiter',
    CHEF: 'Chef',
  },
  status: {
    INVITED: 'Invited, not signed in yet',
    PENDING: 'Awaiting approval',
    ACTIVE: 'Active',
    DEACTIVATED: 'Deactivated',
  },
  page: {
    title: 'Staff',
    description:
      'Invite waiters and chefs by the email of their Google account. Approve them here once they have signed in with Google.',
    inviteForm: 'Invite a staff member',
    email: 'Google email',
    emailPlaceholder: 'name.surname@gmail.com',
    role: 'Role',
    invite: 'Invite',
    emailTaken:
      'This email already belongs to a staff member or another restaurant.',
    inviteFailed: 'Could not send the invitation. Check the email.',
    updateFailed: 'Could not save the change.',
    loading: 'Loading',
    loadFailed: 'Could not load the staff.',
    columns: {
      member: 'Staff member',
      role: 'Role',
      status: 'Status',
      actions: 'Actions',
    },
    roleAria: 'Role of {{email}}',
    approveAria: 'Approve {{email}}',
    approve: 'Approve',
    activate: 'Activate',
    deactivateAria: 'Deactivate {{email}}',
    deactivate: 'Deactivate',
    empty: 'No staff yet. Invite your first waiter or chef above.',
  },
}

export default en
