package com.decokasa.seguimiento.embarques.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.RequisitoFaltante;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.TransicionNoPermitida;
import com.decokasa.seguimiento.compartido.domain.eventos.DatosDelPedido;
import com.decokasa.seguimiento.compartido.domain.eventos.EventoEmbarque;
import com.decokasa.seguimiento.compartido.domain.eventos.Interviniente;
import com.decokasa.seguimiento.embarques.application.port.in.EnviosDeEmbarque;
import com.decokasa.seguimiento.embarques.application.port.in.GestionarEmbarques;
import com.decokasa.seguimiento.embarques.application.port.out.EmbarquesRepositorio;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Envío a aprobación y decisión. Corre en la transacción de la acción del pedido. Regla provisional, pendiente de
 * confirmar con el cliente: basta una aprobación autorizada y quien envió no aprueba su propio envío. Sin Spring (R2).
 */
public final class EnviosDeEmbarqueCasoDeUso implements EnviosDeEmbarque {

    private final EmbarquesRepositorio repositorio;
    private final PublicadorEventos publicador;

    public EnviosDeEmbarqueCasoDeUso(EmbarquesRepositorio repositorio, PublicadorEventos publicador) {
        this.repositorio = repositorio;
        this.publicador = publicador;
    }

    @Override
    public List<String> faltantes(UUID pedidoId) {
        List<GestionarEmbarques.Bl> bls = repositorio.blsDelPedido(pedidoId, false);
        if (bls.isEmpty()) {
            return List.of("vincula al menos un BL");
        }
        List<String> faltantes = new ArrayList<>();
        for (GestionarEmbarques.Bl bl : bls) {
            if (bl.packingList() == null) {
                faltantes.add(bl.codigo() + ": Packing List confirmado");
            }
            if (bl.fechaSalida() == null) {
                faltantes.add(bl.codigo() + ": fecha de salida");
            }
            if (bl.importadora() == null) {
                faltantes.add(bl.codigo() + ": importadora");
            }
        }
        return List.copyOf(faltantes);
    }

    @Override
    public List<String> faltantesParaAprobar(UUID pedidoId, UUID actorId) {
        var envio = repositorio.envioPendiente(pedidoId);
        if (envio.isEmpty()) {
            return List.of("no hay un envío pendiente");
        }
        List<String> faltantes = new ArrayList<>();
        if (envio.get().enviadoPor().equals(actorId)) {
            faltantes.add("quien envió no aprueba su propio envío");
        }
        repositorio.revisionesPendientes(envio.get().id()).forEach(pendiente -> faltantes.add(
                "revisar la versión v" + pendiente.versionVigente() + " del Packing List de " + pendiente.codigo()));
        return List.copyOf(faltantes);
    }

    @Override
    public void exigirAprobable(UUID pedidoId, UUID actorId) {
        EmbarquesRepositorio.EnvioPendiente envio = pendiente(pedidoId);
        if (envio.enviadoPor().equals(actorId)) {
            throw new SinPermiso("Quien envió no aprueba su propio envío.");
        }
        List<String> revisiones = repositorio.revisionesPendientes(envio.id()).stream()
                .map(pendiente -> "revisar la versión v" + pendiente.versionVigente() + " del Packing List de "
                        + pendiente.codigo())
                .toList();
        if (!revisiones.isEmpty()) {
            throw new RequisitoFaltante(revisiones);
        }
    }

    @Override
    public void registrarEnvio(DatosDelPedido pedido, int etapa, UUID entradaId, Interviniente actor, Instant ahora) {
        int numero = repositorio.registrarEnvio(pedido.pedidoId(), entradaId, actor.usuarioId(), ahora);
        GestionarEmbarques.Envio envio = repositorio.enviosDelPedido(pedido.pedidoId()).stream()
                .filter(fila -> fila.numero() == numero)
                .findFirst()
                .orElseThrow();
        String detalle = envio.filas().stream()
                .map(fila -> fila.codigo() + " con Packing List v" + fila.version() + " para "
                        + fila.pedidos().stream().map(vinculo -> vinculo.numero() + " (vinculó "
                                + vinculo.vinculadoPor() + ")").collect(Collectors.joining(", ")))
                .collect(Collectors.joining("; "));
        publicar(pedido, etapa, actor, ahora, "Envío " + numero + " a aprobación: " + detalle + ".");
    }

    @Override
    public void aprobar(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ahora) {
        EmbarquesRepositorio.EnvioPendiente envio = pendiente(pedido.pedidoId());
        repositorio.registrarDecision(envio.id(), "APROBADA", actor.usuarioId(), null, List.of(), ahora);
        repositorio.bloquearBls(envio.id(), ahora);
        publicar(pedido, etapa, actor, ahora, "Envío " + envio.numero() + " aprobado.");
    }

    @Override
    public void rechazar(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ahora, String motivo,
            List<UUID> blSenalados) {
        EmbarquesRepositorio.EnvioPendiente envio = pendiente(pedido.pedidoId());
        List<String> codigos = repositorio.codigosDelEnvio(envio.id(), blSenalados);
        if (codigos.size() != blSenalados.size()) {
            throw new DatoInvalido("blSenalados", "Solo se señalan BL de este envío.");
        }
        repositorio.registrarDecision(envio.id(), "RECHAZADA", actor.usuarioId(), motivo, blSenalados, ahora);
        publicar(pedido, etapa, actor, ahora, "Envío " + envio.numero() + " rechazado"
                + (codigos.isEmpty() ? "" : ". BL señalados: " + String.join(", ", codigos)) + ". Motivo: " + motivo);
    }

    private EmbarquesRepositorio.EnvioPendiente pendiente(UUID pedidoId) {
        return repositorio.envioPendiente(pedidoId)
                .orElseThrow(() -> new TransicionNoPermitida("Este pedido no tiene un envío pendiente."));
    }

    private void publicar(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ahora, String texto) {
        publicador.publicar(new EventoEmbarque(pedido, etapa, actor, ahora, texto, false));
    }
}
