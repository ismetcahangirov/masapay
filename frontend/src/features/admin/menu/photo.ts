/** The photo types the backend accepts. */
export const PHOTO_TYPES = 'image/jpeg,image/png,image/webp'

/**
 * A chosen photo as a small JPEG data: URL (at most 1024 px on the long side),
 * so an imported menu with many photos stays a small request. Falls back to
 * the file as it is where the browser cannot draw it.
 */
export async function photoDataUrl(file: Blob): Promise<string> {
  try {
    const bitmap = await createImageBitmap(file)
    const scale = Math.min(1, 1024 / Math.max(bitmap.width, bitmap.height))
    const canvas = document.createElement('canvas')
    canvas.width = Math.round(bitmap.width * scale)
    canvas.height = Math.round(bitmap.height * scale)
    const context = canvas.getContext('2d')
    if (!context) throw new Error('no canvas')
    context.fillStyle = '#FFFFFF'
    context.fillRect(0, 0, canvas.width, canvas.height)
    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
    bitmap.close()
    return canvas.toDataURL('image/jpeg', 0.85)
  } catch {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(String(reader.result))
      reader.onerror = () => reject(reader.error)
      reader.readAsDataURL(file)
    })
  }
}
