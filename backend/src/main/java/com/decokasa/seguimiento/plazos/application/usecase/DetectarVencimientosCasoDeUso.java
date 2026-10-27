package com.decokasa.seguimiento.plazos.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.plazos.application.port.in.DetectarVencimientos;
import com.decokasa.seguimiento.plazos.application.port.in.RegistrarVencimientosDePedido;
import com.decokasa.seguimiento.plazos.application.port.out.DiagnosticoVencimientos;
import com.decokasa.seguimiento.plazos.application.port.out.PedidosConEtapaActiva;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Una pasada de detección (paso 5.2): lee hasta {@code lote} candidatos y registra cada pedido en su propia
 * transacción. Si un pedido falla, su transacción se revierte entera (sin aviso, historial ni correo), el fallo queda
 * en el diagnóstico con la pasada, el pedido, la categoría y la hora, y el pedido vuelve a ser candidato en la pasada
 * siguiente; los demás siguen. Si falla la lectura de candidatos, se registra y la pasada termina con el error. La
 * hora sale del reloj del servidor (R5). Sin Spring (R2).
 */
public final class DetectarVencimientosCasoDeUso implements DetectarVencimientos {

    private final PedidosConEtapaActiva pedidos;
    private final RegistrarVencimientosDePedido registrar;
    private final Reloj reloj;
    private final int lote;
    private final DiagnosticoVencimientos diagnostico;

    public DetectarVencimientosCasoDeUso(PedidosConEtapaActiva pedidos, RegistrarVencimientosDePedido registrar,
            Reloj reloj, int lote, DiagnosticoVencimientos diagnostico) {
        if (lote < 1 || lote > 1000) {
            throw new IllegalArgumentException("El lote de vencimientos va de 1 a 1000.");
        }
        this.pedidos = pedidos;
        this.registrar = registrar;
        this.reloj = reloj;
        this.lote = lote;
        this.diagnostico = diagnostico;
    }

    @Override
    public Resultado detectar() {
        UUID pasadaId = UUID.randomUUID();
        Instant ahora = reloj.ahora();
        List<UUID> candidatos;
        try {
            candidatos = pedidos.candidatos(ahora, lote);
        } catch (RuntimeException error) {
            informarPasada(pasadaId, error);
            throw new PasadaInterrumpida(pasadaId, error);
        }
        int avisos = 0;
        int fallidos = 0;
        for (UUID pedidoId : candidatos) {
            try {
                avisos += registrar.registrar(pedidoId, ahora);
            } catch (RuntimeException error) {
                fallidos++;
                informarPedido(pasadaId, pedidoId, error);
            }
        }
        return new Resultado(pasadaId, candidatos.size(), avisos, fallidos);
    }

    // Un fallo del propio diagnóstico no detiene la pasada. El adaptador ya escribe un registro de último recurso si su
    // clasificación falla; este catch solo cubre que no pueda escribir nada, y el contador de fallidos lo refleja.
    private void informarPedido(UUID pasadaId, UUID pedidoId, RuntimeException error) {
        try {
            diagnostico.falloDePedido(pasadaId, pedidoId, reloj.ahora(), error);
        } catch (RuntimeException ignorada) {
            // Sin alternativa de registro aquí; la tarea informa el número de fallidos de la pasada.
        }
    }

    private void informarPasada(UUID pasadaId, RuntimeException error) {
        try {
            diagnostico.falloDePasada(pasadaId, reloj.ahora(), error);
        } catch (RuntimeException ignorada) {
            // La excepción original sigue su curso hacia la tarea.
        }
    }
}
