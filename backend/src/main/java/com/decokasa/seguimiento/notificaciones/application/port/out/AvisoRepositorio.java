package com.decokasa.seguimiento.notificaciones.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.notificaciones.domain.AvisoEtapa;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Avisos de etapa (V27) y marca de lectura (V26). Las escrituras usan la transacción del llamador. */
public interface AvisoRepositorio {

    /** Una fila por evento y persona; false si ya existía. */
    boolean insertarSiAusente(AvisoEtapa aviso);

    List<AvisoEtapa> recientes(UUID usuarioId, Set<CodigoPais> paises, Instant desde, int limite);

    int contarDespuesDe(UUID usuarioId, Set<CodigoPais> paises, Instant despuesDe);

    /** Hasta cuándo leyó la persona; si nunca marcó, la hora de creación de su cuenta. */
    Instant leidasHasta(UUID usuarioId);

    void marcarLeidas(UUID usuarioId, Instant hasta);
}
