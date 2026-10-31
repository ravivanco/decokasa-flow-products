package com.decokasa.seguimiento.archivos.adapter.out.google;

import com.decokasa.seguimiento.archivos.adapter.out.google.AlmacenTokensGoogle.Tokens;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.function.UnaryOperator;

/** Dónde viven los tokens OAuth cifrados: archivo local o PostgreSQL (ADR-0034). Nunca salen por HTTP. */
interface BovedaTokensGoogle {
    /** Comprueba la llave y el destino antes de iniciar un consentimiento. Sin red. */
    void preparar() throws IOException;

    /** Guarda el resultado de un consentimiento con las reglas de {@link ReglasTokensGoogle#combinar}. */
    void guardar(Tokens nuevos, Instant ahora) throws IOException, GeneralSecurityException;

    /** Tokens con acceso vigente; si el acceso caducó, renueva una sola vez y guarda el resultado. */
    Tokens vigente(Instant ahora, UnaryOperator<Tokens> renovar) throws IOException, GeneralSecurityException;
}
