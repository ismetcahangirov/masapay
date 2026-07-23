import type { ReactNode } from 'react'
import { ArrowRight, QrCode, SplitSquareHorizontal, Star } from 'lucide-react'
import { SiteHeader } from '@/components/layout/site-header'
import { Reveal } from '@/components/motion/reveal'
import { Button } from '@/components/ui/button'

const FEATURES = [
  {
    icon: <QrCode className="size-6" aria-hidden />,
    title: 'QR ilə ani giriş',
    body: 'Tətbiq yükləmədən, masadakı QR kodla canlı adisyona çıxış.',
  },
  {
    icon: <SplitSquareHorizontal className="size-6" aria-hidden />,
    title: 'Ağıllı hesab bölmə',
    body: 'Tam, öz yediyini və ya bərabər böl — hamısı bir toxunuşla.',
  },
  {
    icon: <Star className="size-6" aria-hidden />,
    title: 'Daha çox Google rəyi',
    body: 'Uğurlu ödənişdən sonra 5 ulduzlu rəyə yönləndirmə.',
  },
]

// Marketing landing page (shell for #69). Alternating greige / near-black
// section rhythm, the scroll-aware header (#66) and scroll-reveal animation
// (#67). Parallax (#68) and the full content pass build on this scaffold.
export function LandingPage() {
  return (
    <div id="top" className="min-h-svh bg-background">
      <SiteHeader />

      {/* Hero — greige, oversized light heading */}
      <section className="mx-auto flex min-h-svh max-w-6xl flex-col justify-center px-6 pb-24 pt-32">
        <Reveal>
          <p className="mb-6 text-sm font-medium uppercase tracking-widest text-muted-foreground">
            Restoranlar üçün QR ödəniş
          </p>
          <h1 className="max-w-4xl text-[clamp(2.5rem,6vw,4.5rem)] font-normal leading-[1.05] tracking-tight">
            Vaxt qazandıran və 5 dəfə çox Google rəyi gətirən ödəniş təcrübəsi.
          </h1>
          <p className="mt-8 max-w-xl text-lg text-muted-foreground">
            Müştəri masadakı QR kodu skan edir, hesabı görür, bölür, bəxşiş
            əlavə edir və saniyələr içində ödəyir.
          </p>
          <div className="mt-10 flex flex-wrap items-center gap-4">
            <Button variant="success" size="lg" asChild>
              <a href="#demo">
                Demo istə
                <ArrowRight />
              </a>
            </Button>
            <Button variant="outline" size="lg" asChild>
              <a href="#how">Necə işləyir</a>
            </Button>
          </div>
        </Reveal>
      </section>

      {/* Product — near-black panel, white text */}
      <section id="product" className="bg-brand-near-black text-brand-white">
        <div className="mx-auto max-w-6xl px-6 py-28">
          <Reveal>
            <h2 className="max-w-3xl text-[clamp(2rem,4vw,3rem)] font-normal leading-tight tracking-tight">
              Skan et, böl, ödə.
            </h2>
          </Reveal>
          <div className="mt-16 grid gap-12 sm:grid-cols-3">
            {FEATURES.map((feature, index) => (
              <Reveal key={feature.title} delay={index * 0.08}>
                <Feature
                  icon={feature.icon}
                  title={feature.title}
                  body={feature.body}
                />
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* How — greige */}
      <section id="how" className="bg-background">
        <div className="mx-auto max-w-6xl px-6 py-28">
          <Reveal>
            <h2 className="max-w-3xl text-[clamp(2rem,4vw,3rem)] font-normal leading-tight tracking-tight">
              Personala yox, təcrübəyə vaxt.
            </h2>
            <p className="mt-6 max-w-xl text-lg text-muted-foreground">
              Masa dövriyyəsi sürətlənir, ödəniş növbəsi yox olur, bəxşiş və rəy
              artır.
            </p>
          </Reveal>
        </div>
      </section>

      {/* Pricing / CTA — near-black */}
      <section id="pricing" className="bg-brand-near-black text-brand-white">
        <div className="mx-auto flex max-w-6xl flex-col items-start gap-8 px-6 py-28">
          <Reveal>
            <h2 className="max-w-3xl text-[clamp(2rem,4vw,3rem)] font-normal leading-tight tracking-tight">
              Restoranınız üçün canlı demo.
            </h2>
          </Reveal>
          <Button variant="success" size="lg" asChild>
            <a id="demo" href="#top">
              Demo istə
              <ArrowRight />
            </a>
          </Button>
        </div>
      </section>

      <footer className="bg-background">
        <div className="mx-auto max-w-6xl px-6 py-10 text-sm text-muted-foreground">
          masapay
        </div>
      </footer>
    </div>
  )
}

function Feature({
  icon,
  title,
  body,
}: {
  icon: ReactNode
  title: string
  body: string
}) {
  return (
    <div>
      <div className="mb-5 text-brand-green">{icon}</div>
      <h3 className="text-lg font-medium">{title}</h3>
      <p className="mt-2 text-white/60">{body}</p>
    </div>
  )
}
