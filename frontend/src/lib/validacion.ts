// Ayudas de validación compartidas por los formularios.

/** Bytes en UTF-8: BCrypt solo usa los primeros 72 bytes, así que el límite de la contraseña se mide en bytes. */
export function bytesUtf8(texto: string): number {
  return new TextEncoder().encode(texto).length;
}
