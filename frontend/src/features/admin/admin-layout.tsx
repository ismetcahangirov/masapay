import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { BrandMark } from '@/components/layout/brand-mark'
import { Separator } from '@/components/ui/separator'
import {
  SidebarInset,
  SidebarProvider,
  SidebarTrigger,
} from '@/components/ui/sidebar'
import { AdminSidebar } from '@/features/admin/admin-sidebar'
import { sidebarWasOpen } from '@/features/admin/sidebar-state'
import { SubscriptionBanner } from '@/features/admin/subscription-banner'

// Every admin page sits in this frame (#184): the collapsible sidebar on the
// left, a slim top bar with the toggle, the manager's subscription warning
// (#148), then the page itself.
export function AdminLayout({ children }: { children: ReactNode }) {
  const { t } = useTranslation('admin')
  return (
    <SidebarProvider defaultOpen={sidebarWasOpen()}>
      <AdminSidebar />
      <SidebarInset>
        <header className="sticky top-0 z-10 flex h-14 items-center gap-2 border-b bg-background px-4">
          <SidebarTrigger aria-label={t('layout.toggleMenu')} />
          <Separator orientation="vertical" className="h-5" />
          <span className="flex items-center gap-2 tracking-tight md:hidden">
            <BrandMark className="h-6" />
            masapay
          </span>
        </header>
        <div className="mx-auto w-full max-w-6xl px-4 pt-4 empty:hidden sm:px-6 sm:pt-6">
          <SubscriptionBanner />
        </div>
        {children}
      </SidebarInset>
    </SidebarProvider>
  )
}
