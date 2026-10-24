package com.decokasa.seguimiento.asistente.application.port.in;

import com.decokasa.seguimiento.asistente.domain.Autor;
import com.decokasa.seguimiento.asistente.domain.ConversacionIA;
import com.decokasa.seguimiento.asistente.domain.DocumentoExcluido;
import com.decokasa.seguimiento.asistente.domain.MensajeIA;
import com.decokasa.seguimiento.asistente.domain.Referencia;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** GET /asistente/conversaciones y GET /asistente/conversaciones/{id}. Solo las propias. */
public interface ConsultarConversaciones {

    /**
     * @param pais país seleccionado en la pantalla (paso 7.3); nulo = todos los asignados hoy
     * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si el país no está asignado hoy
     */
    Pagina listar(Solicitante solicitante, UUID pedidoId, CodigoPais pais, int pagina, int tamano);

    /**
     * @throws com.decokasa.seguimiento.compartido.domain.NoEncontrado ajena, vencida, inexistente, o sin acceso hoy al
     *         pedido o a su país
     * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso cuenta inactiva o rol sin acceso
     */
    Detalle obtener(Solicitante solicitante, UUID conversacionId);

    record Pagina(List<ConversacionIA> elementos, int pagina, int tamano, long total) {
    }

    record Detalle(ConversacionIA conversacion, List<VistaMensaje> mensajes) {
    }

    /** Un mensaje retenido no lleva texto, evidencia, referencias ni documentos excluidos. */
    record VistaMensaje(int orden, Autor autor, Instant creadoEn, boolean retenido, String texto,
            Boolean evidenciaSuficiente, List<Referencia> referencias, List<DocumentoExcluido> documentosExcluidos) {

        public static VistaMensaje de(MensajeIA mensaje, boolean retenido) {
            if (retenido) {
                return new VistaMensaje(mensaje.orden(), mensaje.autor(), mensaje.creadoEn(), true, null, null,
                        List.of(), List.of());
            }
            return new VistaMensaje(mensaje.orden(), mensaje.autor(), mensaje.creadoEn(), false, mensaje.texto(),
                    mensaje.evidenciaSuficiente(), mensaje.referencias(), mensaje.documentosExcluidos());
        }

        @Override
        public String toString() {
            return "VistaMensaje[orden=" + orden + ", retenido=" + retenido + "]";
        }
    }
}
