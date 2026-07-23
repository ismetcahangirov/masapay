import { useState } from 'react'
import { CreditCard, Info, QrCode } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'

function App() {
  const [splitMode, setSplitMode] = useState('full')

  return (
    <main className="flex min-h-svh items-center justify-center p-6">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>masapay</CardTitle>
            <Badge variant="success">
              <QrCode className="mr-1" />
              shadcn/ui
            </Badge>
          </div>
          <CardDescription>
            TailwindCSS və shadcn/ui dizayn sistemi uğurla inteqrasiya olundu.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <ToggleGroup
            type="single"
            value={splitMode}
            onValueChange={(value) => value && setSplitMode(value)}
            variant="outline"
          >
            <ToggleGroupItem value="full">Tam hesab</ToggleGroupItem>
            <ToggleGroupItem value="items">Öz yediyin</ToggleGroupItem>
            <ToggleGroupItem value="equal">Bərabər böl</ToggleGroupItem>
          </ToggleGroup>
        </CardContent>
        <CardFooter className="gap-2">
          <Button variant="success">
            <CreditCard />
            Hesabı ödə
          </Button>
          <Dialog>
            <DialogTrigger asChild>
              <Button variant="outline">
                <Info />
                Ətraflı
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Dizayn sistemi hazırdır</DialogTitle>
                <DialogDescription>
                  Button, Card, Dialog, Badge və ToggleGroup komponentləri
                  layihəyə əlavə olundu. Vizual işarələr üçün yalnız
                  lucide-react ikonlarından istifadə olunur.
                </DialogDescription>
              </DialogHeader>
            </DialogContent>
          </Dialog>
        </CardFooter>
      </Card>
    </main>
  )
}

export default App
