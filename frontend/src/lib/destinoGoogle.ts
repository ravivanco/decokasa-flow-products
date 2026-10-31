/**
 * Destinos aceptados para «Conectar Google Drive». El navegador solo sale de la aplicación hacia:
 * - el consentimiento de Google (`https://accounts.google.com/o/oauth2/…`), con el retorno local; o
 * - la salida de un solo uso del backend (`https://…/api/v1/integraciones/google/oauth2/salida?t=…`), con el retorno
 *   HTTPS de Render (ADR-0034): fija la cookie de vínculo en el dominio del callback y redirige a Google.
 * Cualquier otra URL se rechaza aunque venga del servidor.
 */
export function destinoGoogleValido(valor: string): boolean {
  let url: URL;
  try {
    url = new URL(valor);
  } catch {
    return false;
  }
  if (url.protocol !== 'https:' || url.username || url.password || url.hash) return false;
  if (url.hostname === 'accounts.google.com') return url.pathname.startsWith('/o/oauth2/');
  const ticket = url.searchParams.get('t');
  return (
    url.pathname === '/api/v1/integraciones/google/oauth2/salida' &&
    [...url.searchParams.keys()].length === 1 &&
    ticket !== null &&
    /^[A-Za-z0-9_-]{43}$/.test(ticket)
  );
}
