import { describe, expect, it } from 'vitest'
import { asDownload, filenameFrom } from './download'

describe('report downloads (#170)', () => {
  it('reads the file name the server chose', () => {
    expect(
      filenameFrom(
        'attachment; filename="dolma-bexsisler.xlsx"; filename*=UTF-8\'\'dolma-bexsisler.xlsx',
      ),
    ).toBe('dolma-bexsisler.xlsx')
    expect(filenameFrom('attachment; filename="report.xlsx"')).toBe(
      'report.xlsx',
    )
    expect(
      filenameFrom("attachment; filename*=UTF-8''%C9%99m%C9%99liyyat.xlsx"),
    ).toBe('əməliyyat.xlsx')
    expect(filenameFrom(null)).toBeNull()
    expect(filenameFrom('inline')).toBeNull()
  })

  it('keeps the body and falls back to a name when the server sends none', async () => {
    const response = new Response('x', { status: 200 })

    const file = await asDownload('fallback.xlsx')(response)

    expect(file.filename).toBe('fallback.xlsx')
    expect(file.blob.size).toBe(1)
  })
})
