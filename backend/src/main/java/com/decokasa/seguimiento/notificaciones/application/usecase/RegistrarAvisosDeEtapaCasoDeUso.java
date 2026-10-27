package com.decokasa.seguimiento.notificaciones.application.usecase;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaAprobada;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaEnviada;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaReasignada;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaRechazada;
import com.decokasa.seguimiento.compartido.domain.eventos.EventoDePedido;
import com.decokasa.seguimiento.compartido.domain.eventos.EventoPedidoRegistrado;
import com.decokasa.seguimiento.compartido.domain.eventos.PedidoCreado;
import com.decokasa.seguimiento.compartido.domain.eventos.PedidoDevuelto;
import com.decokasa.seguimiento.notificaciones.application.port.in.EncolarNotificacion;
import com.decokasa.seguimiento.notificaciones.application.port.out.AvisoRepositorio;
import com.decokasa.seguimiento.notificaciones.application.port.out.EtapasDePedido;
import com.decokasa.seguimiento.notificaciones.application.port.out.NotificacionRepositorio;
import com.decokasa.seguimiento.notificaciones.domain.AvisoEtapa;
import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarContactos;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Avisos y correos de etapa (ADR-0032). En la misma transacción que la acción y su evento: un aviso por persona en
 * aviso_etapa y una fila AVISO_ETAPA por correo registrado en el outbox. El correo se envía después del commit.
 *
 * <p>Destinatarios: las administradoras del país del pedido (siempre, aunque hayan actuado) y los responsables de la
 * etapa que queda activa, menos quien actuó: la persona asignada individualmente o, si no hay, las cuentas activas con
 * el rol de la etapa y el país. Una reasignación avisa a la persona asignada. El avance a una etapa automática (Fin) es
 * un cierre: solo la admin, y PEDIDO_FINALIZADO de la misma operación no avisa otra vez. Sin números de etapa fijos:
 * todo sale de la versión del flujo del pedido. Sin Spring (R2).
 */
public final class RegistrarAvisosDeEtapaCasoDeUso implements EncolarNotificacion {

    static final String PLANTILLA = "AVISO_ETAPA";
    private static final CodigoRol ADMIN = new CodigoRol("admin");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");

    private final EtapasDePedido etapas;
    private final ConsultarContactos contactos;
    private final AvisoRepositorio avisos;
    private final NotificacionRepositorio correos;

    public RegistrarAvisosDeEtapaCasoDeUso(EtapasDePedido etapas, ConsultarContactos contactos,
            AvisoRepositorio avisos, NotificacionRepositorio correos) {
        this.etapas = etapas;
        this.contactos = contactos;
        this.avisos = avisos;
        this.correos = correos;
    }

    /** Qué operación es y entre qué etapas. */
    private record Operacion(AvisoEtapa.Tipo tipo, Integer origen, int destino, String motivo, UUID asignado) {
    }

    @Override
    public void encolar(EventoPedidoRegistrado registrado) {
        EventoDePedido evento = registrado.evento();
        Operacion operacion = switch (evento) {
            case PedidoCreado e -> new Operacion(AvisoEtapa.Tipo.CREACION, null, e.etapa(), null, null);
            case EtapaEnviada e -> new Operacion(AvisoEtapa.Tipo.LLEGADA, e.etapa(), e.etapaDestino(), e.observacion(),
                    null);
            case EtapaAprobada e -> new Operacion(AvisoEtapa.Tipo.LLEGADA, e.etapa(), e.etapaDestino(),
                    e.observacion(), null);
            case EtapaRechazada e -> new Operacion(AvisoEtapa.Tipo.DEVOLUCION, e.etapa(), e.etapaDestino(),
                    e.observacion(), null);
            case PedidoDevuelto e -> new Operacion(AvisoEtapa.Tipo.DEVOLUCION, e.etapa(), e.etapaDestino(),
                    e.observacion(), null);
            case EtapaReasignada e -> new Operacion(AvisoEtapa.Tipo.REASIGNACION, e.etapa(), e.etapa(), e.motivo(),
                    e.asignadoA());
            default -> null; // PEDIDO_FINALIZADO lo cubre el cierre; los demás eventos no cambian de responsable.
        };
        if (operacion == null) {
            return;
        }
        Optional<EtapasDePedido.Contexto> encontrado = etapas.contexto(evento.pedidoId(), operacion.origen(),
                operacion.destino());
        if (encontrado.isEmpty()) {
            return;
        }
        EtapasDePedido.Contexto contexto = encontrado.get();
        AvisoEtapa.Tipo tipo = operacion.tipo() == AvisoEtapa.Tipo.LLEGADA && contexto.destinoAutomatico()
                ? AvisoEtapa.Tipo.CIERRE : operacion.tipo();
        CodigoPais pais = evento.pais();
        UUID actorId = evento.actor() == null ? null : evento.actor().usuarioId();

        // Destinatarios sin duplicados: admin y responsable a la vez recibe un solo aviso y un solo correo.
        Map<UUID, Destino> destinos = new LinkedHashMap<>();
        for (ConsultarContactos.Contacto admin : contactos.activosConRol(ADMIN, pais)) {
            destinos.computeIfAbsent(admin.usuarioId(), id -> new Destino(admin)).admin = true;
        }
        for (ConsultarContactos.Contacto responsable : responsables(tipo, operacion, contexto, pais)) {
            if (!responsable.usuarioId().equals(actorId)) {
                destinos.computeIfAbsent(responsable.usuarioId(), id -> new Destino(responsable)).responsable = true;
            }
        }
        if (destinos.isEmpty()) {
            return;
        }
        String actorNombre = contactos.nombre(actorId).orElse(null);
        String motivo = operacion.motivo() == null || operacion.motivo().isBlank() ? null : operacion.motivo().strip();
        for (Destino destino : destinos.values()) {
            AvisoEtapa aviso = new AvisoEtapa(UUID.randomUUID(), registrado.eventoId(), evento.pedidoId(),
                    destino.contacto.usuarioId(), tipo, pais.name(), evento.tipoFlujo().name(), evento.numeroPedido(),
                    operacion.origen(), contexto.nombreOrigen(), operacion.destino(), contexto.nombreDestino(),
                    actorId, actorNombre, motivo, destino.admin, destino.responsable, evento.ocurridoEn());
            avisos.insertarSiAusente(aviso);
            String correo = correoValido(destino.contacto.correo());
            if (correo != null) {
                correos.insertarSiAusente(Notificacion.pendiente(evento.pedidoId(), registrado.eventoId(), correo,
                        datos(aviso, evento), evento.ocurridoEn(), PLANTILLA, asunto(aviso)));
            }
        }
    }

    private List<ConsultarContactos.Contacto> responsables(AvisoEtapa.Tipo tipo, Operacion operacion,
            EtapasDePedido.Contexto contexto, CodigoPais pais) {
        if (tipo == AvisoEtapa.Tipo.CIERRE) {
            return List.of();
        }
        UUID asignado = tipo == AvisoEtapa.Tipo.REASIGNACION ? operacion.asignado() : contexto.asignadoDestino();
        if (asignado != null) {
            return contactos.activoEnPais(asignado, pais).map(List::of).orElse(List.of());
        }
        if (tipo == AvisoEtapa.Tipo.REASIGNACION || contexto.rolDestino() == null) {
            return List.of();
        }
        return contactos.activosConRol(contexto.rolDestino(), pais);
    }

    private static final class Destino {
        final ConsultarContactos.Contacto contacto;
        boolean admin;
        boolean responsable;

        Destino(ConsultarContactos.Contacto contacto) {
            this.contacto = contacto;
        }
    }

    private static String correoValido(String correo) {
        if (correo == null || correo.isBlank()) {
            return null;
        }
        try {
            return Notificacion.normalizarCorreo(correo);
        } catch (IllegalArgumentException invalido) {
            return null; // No se inventa ni se corrige un destinatario: el aviso queda solo en la aplicación.
        }
    }

    static String asunto(AvisoEtapa aviso) {
        return "DECOKASA " + aviso.numeroPedido() + ": " + accion(aviso);
    }

    static String accion(AvisoEtapa aviso) {
        String destino = aviso.tipoFlujo() + aviso.etapaDestino() + " " + aviso.nombreDestino();
        return switch (aviso.tipo()) {
            case CREACION -> "pedido creado en " + destino;
            case LLEGADA -> "llegó a " + destino;
            case DEVOLUCION -> "volvió a " + destino;
            case REASIGNACION -> "reasignado en " + destino;
            case CIERRE -> "pedido finalizado";
        };
    }

    private static Map<String, String> datos(AvisoEtapa aviso, EventoDePedido evento) {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("pedido", aviso.numeroPedido());
        datos.put("pais", aviso.pais());
        datos.put("tipoEvento", evento.getClass().getSimpleName());
        datos.put("tipoAviso", aviso.tipo().name());
        datos.put("accion", accion(aviso));
        if (aviso.etapaOrigen() != null && aviso.tipo() != AvisoEtapa.Tipo.REASIGNACION) {
            datos.put("etapaOrigen",
                    aviso.tipoFlujo() + aviso.etapaOrigen() + " " + Objects.toString(aviso.nombreOrigen(), ""));
        }
        datos.put("etapaDestino", aviso.tipoFlujo() + aviso.etapaDestino() + " " + aviso.nombreDestino());
        if (aviso.actorNombre() != null) {
            datos.put("actor", aviso.actorNombre());
        }
        if (aviso.motivo() != null) {
            datos.put("motivo", aviso.motivo().length() > 1000 ? aviso.motivo().substring(0, 1000) : aviso.motivo());
        }
        datos.put("fecha", FECHA.format(aviso.ocurridoEn().atZone(ECUADOR)));
        datos.put("ocurridoEn", aviso.ocurridoEn().toString());
        List<String> como = new ArrayList<>();
        if (aviso.comoAdmin()) {
            como.add("administradora");
        }
        if (aviso.comoResponsable()) {
            como.add("responsable de la etapa");
        }
        datos.put("como", String.join(" y ", como));
        return datos;
    }
}
