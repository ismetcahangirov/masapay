import type { ReactNode } from 'react'
import { ArrowRight, QrCode, SplitSquareHorizontal, Star } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { BrandMark } from '@/components/layout/brand-mark'
import { SiteHeader } from '@/components/layout/site-header'
import { Reveal } from '@/components/motion/reveal'
import { Parallax } from '@/components/motion/parallax'
import { HeroScene } from '@/components/three/hero-scene'
import { Button } from '@/components/ui/button'

// Shared heading style: oversized, weight 400, tight leading and very tight
// tracking, matching the reference design language.
const HEADING = 'font-normal leading-[0.98] tracking-[-0.045em]'

// Texts live in the "landing" namespace; these hold their keys.
const FEATURES = [
  { icon: <QrCode className="size-10" aria-hidden />, key: 'qr' },
  {
    icon: <SplitSquareHorizontal className="size-10" aria-hidden />,
    key: 'split',
  },
  { icon: <Star className="size-10" aria-hidden />, key: 'reviews' },
] as const

const STATS = ['reviews', 'turnover', 'payment'] as const

const STEPS = [
  { n: '01', key: 'scan' },
  { n: '02', key: 'split' },
  { n: '03', key: 'pay' },
] as const

// Marketing landing page. Whitespace-driven, content-centric: alternating
// greige (light) / near-black (dark) sections, oversized weight-400 headings
// with tight tracking, black pill CTAs and the brand green used only as a rare
// accent (logo and feature icons). Built on the scroll-aware header (#66),
// reveal (#67), parallax (#68) and the 3D hero accent (#70).
export function LandingPage() {
  const { t } = useTranslation('landing')
  return (
    <div id="top" className="min-h-svh bg-background">
      <SiteHeader />

      {/* Hero — light */}
      <section
        data-section-tone="light"
        className="mx-auto flex min-h-svh max-w-6xl items-center px-4 sm:px-6 pb-24 pt-32"
      >
        <div className="grid w-full items-center gap-12 lg:grid-cols-[1.15fr_0.85fr]">
          <Reveal>
            <p className="mb-6 text-sm font-medium uppercase tracking-widest text-muted-foreground">
              {t('hero.eyebrow')}
            </p>
            <h1
              className={`max-w-4xl text-[clamp(2.25rem,4.6vw,3.25rem)] ${HEADING}`}
            >
              {t('hero.title')}
            </h1>
            <p className="mt-8 max-w-lg text-base text-muted-foreground">
              {t('hero.body')}
            </p>
            <div className="mt-10 flex flex-wrap items-center gap-3">
              <Button size="lg" asChild>
                <a href="#demo">
                  {t('requestDemo')}
                  <ArrowRight />
                </a>
              </Button>
              <Button variant="outline" size="lg" asChild>
                <a href="#how">{t('nav.how')}</a>
              </Button>
            </div>
          </Reveal>

          <HeroScene className="hidden h-[440px] w-full lg:block" />
        </div>
      </section>

      {/* Product — dark, feature grid with green accent icons */}
      <section
        id="product"
        data-section-tone="dark"
        className="bg-brand-near-black text-brand-white"
      >
        <div className="mx-auto max-w-6xl px-4 sm:px-6 py-28">
          <Parallax speed={0.25}>
            <Reveal>
              <h2
                className={`max-w-3xl text-[clamp(2rem,4vw,3rem)] ${HEADING}`}
              >
                {t('product.title')}
              </h2>
            </Reveal>
          </Parallax>
          <div className="mt-16 grid gap-12 sm:grid-cols-3">
            {FEATURES.map((feature, index) => (
              <Reveal key={feature.key} delay={index * 0.08}>
                <Feature
                  icon={feature.icon}
                  title={t(`features.${feature.key}.title`)}
                  body={t(`features.${feature.key}.body`)}
                />
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* Stats / trust — light */}
      <section data-section-tone="light" className="bg-background">
        <div className="mx-auto grid max-w-6xl gap-12 px-4 sm:px-6 py-28 sm:grid-cols-3">
          {STATS.map((stat, index) => (
            <Reveal key={stat} delay={index * 0.08}>
              <div>
                <div
                  className={`text-[clamp(2.5rem,5vw,3.5rem)] leading-none ${HEADING}`}
                >
                  {t(`stats.${stat}.value`)}
                </div>
                <p className="mt-3 text-base text-muted-foreground">
                  {t(`stats.${stat}.label`)}
                </p>
              </div>
            </Reveal>
          ))}
        </div>
      </section>

      {/* How — dark, numbered steps */}
      <section
        id="how"
        data-section-tone="dark"
        className="bg-brand-near-black text-brand-white"
      >
        <div className="mx-auto max-w-6xl px-4 sm:px-6 py-28">
          <Reveal>
            <h2 className={`max-w-3xl text-[clamp(2rem,4vw,3rem)] ${HEADING}`}>
              {t('how.title')}
            </h2>
          </Reveal>
          <div className="mt-16 grid gap-12 sm:grid-cols-3">
            {STEPS.map((step, index) => (
              <Reveal key={step.n} delay={index * 0.08}>
                <div>
                  <div className="text-5xl font-normal leading-none tracking-tight text-white/40 proportional-nums">
                    {step.n}
                  </div>
                  <h3 className="mt-6 text-2xl font-normal tracking-tight">
                    {t(`steps.${step.key}.title`)}
                  </h3>
                  <p className="mt-2 text-white/60">
                    {t(`steps.${step.key}.body`)}
                  </p>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* CTA — light */}
      <section id="pricing" data-section-tone="light" className="bg-background">
        <div className="mx-auto flex max-w-6xl flex-col items-start gap-8 px-4 sm:px-6 py-28">
          <Parallax speed={0.2}>
            <Reveal>
              <h2
                className={`max-w-3xl text-[clamp(2rem,4vw,3rem)] ${HEADING}`}
              >
                {t('demo.title')}
              </h2>
            </Reveal>
          </Parallax>
          <Reveal delay={0.08}>
            <p className="max-w-lg text-base text-muted-foreground">
              {t('demo.body')}
            </p>
          </Reveal>
          <Button size="lg" asChild>
            <a id="demo" href="#top">
              {t('requestDemo')}
              <ArrowRight />
            </a>
          </Button>
        </div>
      </section>

      {/* Footer — dark */}
      <footer
        data-section-tone="dark"
        className="bg-brand-near-black text-brand-white"
      >
        <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 sm:px-6 py-16 sm:flex-row sm:items-center sm:justify-between">
          <span className="flex items-center gap-2 text-xl font-normal tracking-tight lg:text-2xl">
            <BrandMark className="h-8 lg:h-9" />
            masapay
          </span>
          <nav className="flex flex-wrap gap-8 text-base text-white/60">
            <a
              href="#product"
              className="transition-colors hover:text-brand-white"
            >
              {t('nav.product')}
            </a>
            <a href="#how" className="transition-colors hover:text-brand-white">
              {t('nav.how')}
            </a>
            <a
              href="#demo"
              className="transition-colors hover:text-brand-white"
            >
              {t('requestDemo')}
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
      <div className="mb-6 text-brand-green">{icon}</div>
      <h3 className="text-2xl font-normal tracking-tight">{title}</h3>
      <p className="mt-2 text-white/60">{body}</p>
    </div>
  )
}
