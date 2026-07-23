import type { ReactNode } from 'react'
import { ArrowRight, QrCode, SplitSquareHorizontal, Star } from 'lucide-react'
import { SiteHeader } from '@/components/layout/site-header'
import { Reveal } from '@/components/motion/reveal'
import { Parallax } from '@/components/motion/parallax'
import { HeroScene } from '@/components/three/hero-scene'
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

const STATS = [
  { value: '5×', label: 'daha çox Google rəyi' },
  { value: '40%', label: 'daha sürətli masa dövriyyəsi' },
  { value: '3 san', label: 'orta ödəniş vaxtı' },
]

const STEPS = [
  { n: '01', title: 'Skan et', body: 'Müştəri masadakı QR kodu skan edir.' },
  { n: '02', title: 'Böl', body: 'Hesabı görür, bölür və bəxşiş əlavə edir.' },
  { n: '03', title: 'Ödə', body: 'Payriff ilə saniyələr içində ödəyir.' },
]

// Marketing landing page (#69). Whitespace-driven, content-centric, built on
// the design-system-v2 rhythm: alternating greige (light) and near-black (dark)
// sections, oversized light Helvetica Neue headings, the scroll-aware header
// (#66), scroll reveal (#67) and parallax (#68).
export function LandingPage() {
  return (
    <div id="top" className="min-h-svh bg-background">
      <SiteHeader />

      {/* Hero — light */}
      <section className="mx-auto flex min-h-svh max-w-6xl items-center px-6 pb-24 pt-32">
        <div className="grid w-full items-center gap-12 lg:grid-cols-[1.15fr_0.85fr]">
          <Reveal>
            <p className="mb-6 text-sm font-medium uppercase tracking-widest text-muted-foreground">
              Restoranlar üçün QR ödəniş
            </p>
            <h1 className="max-w-4xl text-[clamp(2.5rem,6vw,4.5rem)] font-normal leading-[1.05] tracking-tight">
              Vaxt qazandıran və 5 dəfə çox Google rəyi gətirən ödəniş
              təcrübəsi.
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

          <HeroScene className="hidden h-[440px] w-full lg:block" />
        </div>
      </section>

      {/* Product — dark, feature grid with green icons */}
      <section id="product" className="bg-brand-near-black text-brand-white">
        <div className="mx-auto max-w-6xl px-6 py-28">
          <Parallax speed={0.25}>
            <Reveal>
              <h2 className="max-w-3xl text-[clamp(2rem,4vw,3rem)] font-normal leading-tight tracking-tight">
                Skan et, böl, ödə.
              </h2>
            </Reveal>
          </Parallax>
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

      {/* Stats / trust — light */}
      <section className="bg-background">
        <div className="mx-auto grid max-w-6xl gap-12 px-6 py-28 sm:grid-cols-3">
          {STATS.map((stat, index) => (
            <Reveal key={stat.label} delay={index * 0.08}>
              <div>
                <div className="text-[clamp(2.5rem,5vw,3.5rem)] font-normal leading-none tracking-tight">
                  {stat.value}
                </div>
                <p className="mt-3 text-muted-foreground">{stat.label}</p>
              </div>
            </Reveal>
          ))}
        </div>
      </section>

      {/* How — dark, numbered steps */}
      <section id="how" className="bg-brand-near-black text-brand-white">
        <div className="mx-auto max-w-6xl px-6 py-28">
          <Reveal>
            <h2 className="max-w-3xl text-[clamp(2rem,4vw,3rem)] font-normal leading-tight tracking-tight">
              Personala yox, təcrübəyə vaxt.
            </h2>
          </Reveal>
          <div className="mt-16 grid gap-12 sm:grid-cols-3">
            {STEPS.map((step, index) => (
              <Reveal key={step.n} delay={index * 0.08}>
                <div>
                  <div className="text-sm font-medium text-brand-green">
                    {step.n}
                  </div>
                  <h3 className="mt-4 text-lg font-medium">{step.title}</h3>
                  <p className="mt-2 text-white/60">{step.body}</p>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* CTA — light */}
      <section id="pricing" className="bg-background">
        <div className="mx-auto flex max-w-6xl flex-col items-start gap-8 px-6 py-28">
          <Parallax speed={0.2}>
            <Reveal>
              <h2 className="max-w-3xl text-[clamp(2rem,4vw,3rem)] font-normal leading-tight tracking-tight">
                Restoranınız üçün canlı demo.
              </h2>
            </Reveal>
          </Parallax>
          <Reveal delay={0.08}>
            <p className="max-w-xl text-lg text-muted-foreground">
              15 dəqiqəlik demoda masapay-ın restoranınıza necə uyğunlaşdığını
              göstərək.
            </p>
          </Reveal>
          <Button variant="success" size="lg" asChild>
            <a id="demo" href="#top">
              Demo istə
              <ArrowRight />
            </a>
          </Button>
        </div>
      </section>

      {/* Footer — dark */}
      <footer className="bg-brand-near-black text-brand-white">
        <div className="mx-auto flex max-w-6xl flex-col gap-6 px-6 py-16 sm:flex-row sm:items-center sm:justify-between">
          <span className="text-lg font-medium tracking-tight">masapay</span>
          <nav className="flex flex-wrap gap-8 text-sm text-white/60">
            <a
              href="#product"
              className="transition-colors hover:text-brand-white"
            >
              Məhsul
            </a>
            <a href="#how" className="transition-colors hover:text-brand-white">
              Necə işləyir
            </a>
            <a
              href="#demo"
              className="transition-colors hover:text-brand-white"
            >
              Demo istə
            </a>
          </nav>
          <span className="text-sm text-white/40">&copy; 2026 masapay</span>
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
