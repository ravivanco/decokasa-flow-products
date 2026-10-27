package com.decokasa.seguimiento.plazos.adapter.out.diagnostico;

import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;
import com.decokasa.seguimiento.plazos.application.port.out.DiagnosticoVencimientos;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registro estructurado de los fallos del detector de vencimientos (clave=valor en una línea, nivel WARN). Clasifica
 * el error recorriendo la cadena de causas y usando solo tipos y códigos estables:
 * <ul>
 *   <li>{@code DOMINIO}: un {@link ErrorDeDominio}; código = su código del contrato.</li>
 *   <li>{@code BLOQUEO}: espera o bloqueo de fila agotado, interbloqueo o fallo de serialización (también por
 *       SQLSTATE 55P03, 40P01 y 40001).</li>
 *   <li>{@code CONCURRENCIA}: conflicto de versión optimista (otra escritura cambió la fila).</li>
 *   <li>{@code BASE_DE_DATOS}: una {@link SQLException}; código = su SQLSTATE (y la restricción si Hibernate la
 *       informa, que es un nombre del esquema, no un dato).</li>
 *   <li>{@code INESPERADO}: cualquier otro; código = la clase de la causa raíz.</li>
 * </ul>
 * Nunca registra el mensaje de la excepción ni su traza: pueden contener parámetros SQL, motivos escritos por usuarios
 * o correos. Para investigar quedan la pasada, el pedido (UUID técnico), la hora (ISO-8601 en UTC; Ecuador es UTC−5)
 * y la cadena de clases de la excepción. Si la clasificación misma fallara, se escribe un registro de último recurso
 * con la pasada, el pedido y la clase de la excepción original.
 */
@Component
public class DiagnosticoVencimientosLog implements DiagnosticoVencimientos {

    private static final Logger LOG = LoggerFactory.getLogger(DiagnosticoVencimientosLog.class);
    private static final int PROFUNDIDAD_MAXIMA = 12;
    private static final java.util.Set<String> ESTADOS_BLOQUEO = java.util.Set.of("55P03", "40P01", "40001");

    @Override
    public void falloDePedido(UUID pasadaId, UUID pedidoId, Instant ocurridoEn, RuntimeException error) {
        try {
            Clasificacion clasificacion = clasificar(error);
            LOG.warn("vencimientos_fallo_pedido pasada={} pedido={} categoria={} codigo={} excepcion={} hora={}",
                    pasadaId, pedidoId, clasificacion.categoria(), clasificacion.codigo(), clasificacion.cadena(),
                    ocurridoEn);
        } catch (RuntimeException fallaClasificacion) {
            LOG.warn("vencimientos_fallo_sin_clasificar pasada={} pedido={} excepcion={} hora={}", pasadaId, pedidoId,
                    nombre(error), ocurridoEn);
        }
    }

    @Override
    public void falloDePasada(UUID pasadaId, Instant ocurridoEn, RuntimeException error) {
        try {
            Clasificacion clasificacion = clasificar(error);
            LOG.warn("vencimientos_fallo_pasada pasada={} categoria={} codigo={} excepcion={} hora={}",
                    pasadaId, clasificacion.categoria(), clasificacion.codigo(), clasificacion.cadena(), ocurridoEn);
        } catch (RuntimeException fallaClasificacion) {
            LOG.warn("vencimientos_fallo_sin_clasificar pasada={} excepcion={} hora={}", pasadaId, nombre(error),
                    ocurridoEn);
        }
    }

    /** Categoría, código estable y cadena de clases de la excepción, sin mensajes. */
    record Clasificacion(String categoria, String codigo, String cadena) {
    }

    static Clasificacion clasificar(Throwable error) {
        List<Throwable> causas = new ArrayList<>();
        Map<Throwable, Boolean> vistas = new IdentityHashMap<>();
        for (Throwable causa = error; causa != null && causas.size() < PROFUNDIDAD_MAXIMA
                && vistas.put(causa, Boolean.TRUE) == null; causa = causa.getCause()) {
            causas.add(causa);
        }
        String cadena = String.join(">", causas.stream().map(DiagnosticoVencimientosLog::nombre).toList());

        for (Throwable causa : causas) {
            if (causa instanceof ErrorDeDominio dominio) {
                return new Clasificacion("DOMINIO", String.valueOf(dominio.codigo()), cadena);
            }
        }
        String estado = estadoSql(causas);
        for (Throwable causa : causas) {
            if (causa instanceof PessimisticLockException || causa instanceof LockTimeoutException
                    || ESTADOS_BLOQUEO.contains(estado)) {
                return new Clasificacion("BLOQUEO", estado, cadena);
            }
        }
        for (Throwable causa : causas) {
            if (causa instanceof jakarta.persistence.OptimisticLockException
                    || causa instanceof org.springframework.dao.OptimisticLockingFailureException
                    || causa instanceof org.hibernate.StaleStateException) {
                return new Clasificacion("CONCURRENCIA", nombre(causa), cadena);
            }
        }
        for (Throwable causa : causas) {
            if (causa instanceof SQLException) {
                String restriccion = restriccion(causas);
                return new Clasificacion("BASE_DE_DATOS",
                        restriccion == null ? estado : estado + ":" + restriccion, cadena);
            }
        }
        Throwable raiz = causas.isEmpty() ? error : causas.get(causas.size() - 1);
        return new Clasificacion("INESPERADO", raiz == null ? "desconocido" : nombre(raiz), cadena);
    }

    /** Nombre corto de la clase; el completo si es anónima o una lambda (sin nombre corto). */
    private static String nombre(Throwable error) {
        String corto = error.getClass().getSimpleName();
        return corto.isEmpty() ? error.getClass().getName() : corto;
    }

    private static String estadoSql(List<Throwable> causas) {
        for (Throwable causa : causas) {
            if (causa instanceof SQLException sql && sql.getSQLState() != null) {
                return sql.getSQLState();
            }
        }
        return "SIN_SQLSTATE";
    }

    private static String restriccion(List<Throwable> causas) {
        for (Throwable causa : causas) {
            if (causa instanceof org.hibernate.exception.ConstraintViolationException violacion) {
                return violacion.getConstraintName();
            }
        }
        return null;
    }
}
