import * as React from 'react'
import { Slot } from '@radix-ui/react-slot'
import { cva, type VariantProps } from 'class-variance-authority'

import { cn } from '@/lib/utils'

// Left-to-right fill on hover: a half-and-half gradient sized to 200% is
// parked showing its transparent half (revealing the base colour), then slid so
// the coloured half sweeps in from the left. Works on a plain element with no
// extra DOM, so it composes with asChild/Slot.
const sweepBase =
  '[background-size:200%_100%] [background-position:100%_0] hover:[background-position:0_0]'
const sweepGreen = `[background-image:linear-gradient(to_right,#08cb00_50%,transparent_50%)] ${sweepBase}`
const sweepNearBlack = `[background-image:linear-gradient(to_right,#0a0a0a_50%,transparent_50%)] ${sweepBase}`

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-sm font-medium transition-[color,background-color,border-color,background-position] duration-300 ease-out focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0',
  {
    variants: {
      variant: {
        default: `bg-primary text-primary-foreground hover:text-black ${sweepGreen}`,
        success: 'bg-success text-success-foreground hover:bg-success/90',
        destructive:
          'bg-destructive text-destructive-foreground hover:bg-destructive/90',
        outline: `border border-input bg-background text-foreground hover:text-white ${sweepNearBlack}`,
        secondary:
          'bg-secondary text-secondary-foreground hover:bg-secondary/80',
        ghost: 'hover:bg-accent hover:text-accent-foreground',
        link: 'text-primary underline-offset-4 hover:underline',
      },
      size: {
        default: 'h-10 px-5 py-2',
        sm: 'h-9 px-4',
        lg: 'h-12 px-7 text-base',
        icon: 'h-10 w-10',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
)

export interface ButtonProps
  extends
    React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : 'button'
    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      />
    )
  },
)
Button.displayName = 'Button'

export { Button, buttonVariants }
