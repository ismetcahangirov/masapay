// File downloads from the API (#170): the body as a Blob plus the name the
// server chose in Content-Disposition, so every report has the same name in
// every browser.

export interface DownloadedFile {
  blob: Blob
  /** From Content-Disposition; the fallback when the server sent none. */
  filename: string
}

/**
 * Reads filename*=UTF-8''... (RFC 5987) or filename="..." from a
 * Content-Disposition header; null when neither is there.
 */
export function filenameFrom(disposition: string | null): string | null {
  if (!disposition) return null
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (encoded) {
    try {
      return decodeURIComponent(encoded[1].trim())
    } catch {
      // Malformed encoding: fall through to the plain name.
    }
  }
  const plain = /filename="?([^";]+)"?/i.exec(disposition)
  return plain ? plain[1].trim() : null
}

/** An RTK Query responseHandler that keeps the server's file name. */
export function asDownload(fallback: string) {
  return async (response: Response): Promise<DownloadedFile> => ({
    blob: await response.blob(),
    filename:
      filenameFrom(response.headers.get('Content-Disposition')) ?? fallback,
  })
}
