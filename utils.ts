export function delay(ms: number) {
  return new Promise<undefined>(resolve => setTimeout(resolve, ms))
}
