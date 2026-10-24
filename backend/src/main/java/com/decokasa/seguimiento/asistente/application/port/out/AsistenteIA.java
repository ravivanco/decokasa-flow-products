package com.decokasa.seguimiento.asistente.application.port.out;

import com.decokasa.seguimiento.asistente.domain.Autor;
import com.decokasa.seguimiento.asistente.domain.CitaPropuesta;
import java.util.List;
import java.util.Objects;

/**
 * Puerto hacia el modelo (ADR-0020). El adaptador de Claude API vive en {@code adapter.out.claude}; el núcleo no conoce el
 * SDK. Solo lectura: la consulta no declara herramientas, así que el modelo no puede ejecutar ninguna acción.
 */
public interface AsistenteIA {

    /**
     * Una sola llamada al modelo, sin reintentos ni cambio a otro modelo.
     *
     * @throws com.decokasa.seguimiento.asistente.domain.FalloProveedorIA clasificado, sin detalles del proveedor
     * @throws com.decokasa.seguimiento.asistente.domain.IaNoDisponible si el asistente está desactivado
     */
    Respuesta responder(Consulta consulta);

    /**
     * Tokens de entrada que tendría la consulta (count_tokens). Lo usará la reserva de presupuesto del paso 7.5 antes de
     * llamar; en 7.4 nadie lo invoca.
     */
    long contarTokens(Consulta consulta);

    /**
     * @param instrucciones prompt de sistema fijo; nunca incluye datos del pedido ni de documentos
     * @param historial turnos anteriores autorizados, en pares USUARIO, ASISTENTE
     * @param documentos en orden: la posición de cada uno es el document_index de sus citas
     * @param pregunta pregunta nueva, que se envía al final y después de los documentos
     */
    record Consulta(String instrucciones, List<Turno> historial, List<Documento> documentos, String pregunta,
            int maxTokensRespuesta) {

        public Consulta {
            Objects.requireNonNull(instrucciones, "instrucciones");
            Objects.requireNonNull(pregunta, "pregunta");
            historial = List.copyOf(historial);
            documentos = List.copyOf(documentos);
            for (int i = 0; i < historial.size(); i++) {
                Autor esperado = i % 2 == 0 ? Autor.USUARIO : Autor.ASISTENTE;
                if (historial.get(i).autor() != esperado) {
                    throw new IllegalArgumentException("El historial debe alternar pregunta y respuesta");
                }
            }
            if (historial.size() % 2 != 0) {
                throw new IllegalArgumentException("El historial termina en una respuesta");
            }
            if (maxTokensRespuesta < 1) {
                throw new IllegalArgumentException("Límite de respuesta inválido");
            }
        }

        @Override
        public String toString() {
            return "Consulta[turnos=" + historial.size() + ", documentos=" + documentos.size() + "]";
        }
    }

    record Turno(Autor autor, String texto) {

        public Turno {
            Objects.requireNonNull(autor, "autor");
            Objects.requireNonNull(texto, "texto");
        }

        @Override
        public String toString() {
            return "Turno[" + autor + "]";
        }
    }

    /**
     * Un documento de la consulta. Es contenido, nunca instrucciones (ADR-0021): el adaptador lo envía como bloque
     * {@code document} con citas, separado del prompt de sistema.
     *
     * @param pdf contenido del PDF; nulo en un documento de texto
     * @param texto contenido de un documento de texto; nulo en un PDF
     */
    record Documento(Tipo tipo, String titulo, byte[] pdf, String texto) {

        public enum Tipo {
            PDF,
            TEXTO
        }

        public Documento {
            Objects.requireNonNull(tipo, "tipo");
            Objects.requireNonNull(titulo, "titulo");
            if ((tipo == Tipo.PDF) != (pdf != null) || (tipo == Tipo.TEXTO) != (texto != null)) {
                throw new IllegalArgumentException("Contenido del documento inválido");
            }
        }

        public static Documento pdf(String titulo, byte[] contenido) {
            return new Documento(Tipo.PDF, titulo, contenido, null);
        }

        public static Documento texto(String titulo, String contenido) {
            return new Documento(Tipo.TEXTO, titulo, null, contenido);
        }

        @Override
        public String toString() {
            return "Documento[" + tipo + "]";
        }
    }

    /**
     * @param fin cómo terminó la respuesta (stop_reason)
     * @param modelo modelo que respondió (el configurado; sin cambio automático)
     */
    record Respuesta(String texto, List<CitaPropuesta> citas, Fin fin, Consumo consumo, String modelo) {

        public Respuesta {
            Objects.requireNonNull(texto, "texto");
            citas = List.copyOf(citas);
            Objects.requireNonNull(fin, "fin");
            Objects.requireNonNull(consumo, "consumo");
            Objects.requireNonNull(modelo, "modelo");
        }

        @Override
        public String toString() {
            return "Respuesta[fin=" + fin + ", citas=" + citas.size() + "]";
        }
    }

    /** stop_reason: end_turn → COMPLETA; max_tokens → TRUNCADA; refusal → RECHAZADA; cualquier otro → OTRA. */
    enum Fin {
        COMPLETA,
        TRUNCADA,
        RECHAZADA,
        OTRA
    }

    /** usage de la respuesta: entrada, salida y caché. */
    record Consumo(long entrada, long salida, long escrituraCache, long lecturaCache) {

        public static final Consumo NINGUNO = new Consumo(0, 0, 0, 0);

        public Consumo {
            if (entrada < 0 || salida < 0 || escrituraCache < 0 || lecturaCache < 0) {
                throw new IllegalArgumentException("Consumo inválido");
            }
        }
    }
}
