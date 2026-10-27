/** Navegación de página completa a otro origen (por ejemplo, el consentimiento de Google). Aislada para poder probarla. */
export function irA(url: string) {
  window.location.assign(url);
}
