package com.decokasa.seguimiento.asistente.application.usecase;

import com.decokasa.seguimiento.asistente.application.port.in.ConsultarConversaciones;
import com.decokasa.seguimiento.asistente.application.port.out.ConversacionesIA;
import com.decokasa.seguimiento.asistente.application.port.out.CuentasParaAsistente;
import com.decokasa.seguimiento.asistente.application.port.out.CuentasParaAsistente.EstadoCuenta;
import com.decokasa.seguimiento.asistente.application.port.out.DocumentosParaAsistente;
import com.decokasa.seguimiento.asistente.application.port.out.PedidosParaAsistente;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Lista y lee las conversaciones propias. Antes de devolver una conversación se vuelven a comprobar cuenta, rol, país y
 * pedido; los mensajes con una fuente que la persona ya no puede descargar se devuelven retenidos (ADR-0021).
 */
public final class ConsultarConversacionesCasoDeUso implements ConsultarConversaciones {

    private final AccesoAsistente acceso;
    private final PedidosParaAsistente pedidos;
    private final DocumentosParaAsistente documentos;
    private final ConversacionesIA conversaciones;
    private final Reloj reloj;

    public ConsultarConversacionesCasoDeUso(CuentasParaAsistente cuentas, PedidosParaAsistente pedidos,
            DocumentosParaAsistente documentos, ConversacionesIA conversaciones, Reloj reloj) {
        this.acceso = new AccesoAsistente(cuentas, conversaciones);
        this.pedidos = pedidos;
        this.documentos = documentos;
        this.conversaciones = conversaciones;
        this.reloj = reloj;
    }

    @Override
    public Pagina listar(Solicitante solicitante, UUID pedidoId, CodigoPais pais, int pagina, int tamano) {
        EstadoCuenta cuenta = acceso.habilitado(solicitante);
        Set<CodigoPais> paises;
        if (pais == null) {
            paises = cuenta.paises();
        } else if (cuenta.paises().contains(pais)) {
            paises = Set.of(pais);
        } else {
            throw new SinPermiso("No tienes asignado ese país.");
        }
        if (pagina < 0 || tamano < 1 || tamano > 100) {
            throw new DatoInvalido("La página o su tamaño no son válidos.");
        }
        ConversacionesIA.Pagina resultado = conversaciones.deUsuario(solicitante.usuarioId(), pedidoId, paises,
                reloj.ahora(), pagina, tamano);
        return new Pagina(resultado.elementos(), pagina, tamano, resultado.total());
    }

    @Override
    public Detalle obtener(Solicitante solicitante, UUID conversacionId) {
        AccesoAsistente.Vigente vigente = acceso.vigente(solicitante, conversacionId, reloj.ahora());
        // P-13 y país del pedido hoy: si ya no puede leerlo, 404 sin contenido.
        pedidos.leer(solicitante, vigente.conversacion().pedidoId());
        Map<UUID, Boolean> descargables = new HashMap<>();
        Predicate<UUID> puedeDescargar = archivoId -> descargables.computeIfAbsent(archivoId,
                id -> documentos.puedeDescargar(solicitante, id));
        List<VistaMensaje> mensajes = conversaciones.mensajes(conversacionId).stream()
                .map(mensaje -> VistaMensaje.de(mensaje, mensaje.retenidoPara(puedeDescargar)))
                .toList();
        return new Detalle(vigente.conversacion(), mensajes);
    }
}
