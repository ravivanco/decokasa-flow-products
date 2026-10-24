package com.decokasa.seguimiento.asistente.application.usecase;

import com.decokasa.seguimiento.asistente.application.port.out.PedidosParaAsistente.DatosPedido;
import com.decokasa.seguimiento.asistente.application.port.out.PedidosParaAsistente.EtapaPedido;
import com.decokasa.seguimiento.asistente.domain.FuenteEnviada.TramoEtapa;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Texto de los datos del pedido que se envía como documento citable, con el tramo de cada etapa para atribuir sus
 * citas. Sin personas ni observaciones.
 */
record ContextoPedido(String texto, List<TramoEtapa> tramos) {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    ContextoPedido {
        tramos = List.copyOf(tramos);
    }

    static ContextoPedido de(DatosPedido pedido) {
        ZoneId zona = ZoneId.of(pedido.pais() == CodigoPais.CO ? "America/Bogota" : "America/Guayaquil");
        StringBuilder texto = new StringBuilder();
        texto.append("Pedido ").append(pedido.numero()).append('\n');
        texto.append("País: ").append(pedido.pais().name()).append('\n');
        texto.append("Flujo: ").append(pedido.flujo()).append('\n');
        texto.append("Estado: ").append(pedido.finalizado() ? "finalizado" : "en curso").append('\n');
        texto.append("Etapa actual: ").append(pedido.etapaActual()).append(" - ")
                .append(pedido.nombreEtapaActual()).append('\n');
        if (pedido.descripcion() != null && !pedido.descripcion().isBlank()) {
            texto.append("Descripción: ").append(pedido.descripcion().strip()).append('\n');
        }
        texto.append("Fechas en hora local de ").append(zona.getId()).append('\n');
        List<TramoEtapa> tramos = new ArrayList<>();
        for (EtapaPedido etapa : pedido.etapas()) {
            int inicio = texto.length();
            texto.append("Etapa ").append(etapa.numero()).append(" - ").append(etapa.nombre()).append(": ")
                    .append(etapa.estado());
            agregarFecha(texto, "entrada", etapa.entrada(), zona);
            agregarFecha(texto, "salida", etapa.salida(), zona);
            agregarFecha(texto, "vence", etapa.venceEn(), zona);
            texto.append('\n');
            tramos.add(new TramoEtapa(inicio, texto.length(), etapa.numero()));
        }
        return new ContextoPedido(texto.toString(), tramos);
    }

    private static void agregarFecha(StringBuilder texto, String nombre, Instant fecha, ZoneId zona) {
        if (fecha != null) {
            texto.append("; ").append(nombre).append(' ').append(FECHA.format(fecha.atZone(zona)));
        }
    }

    @Override
    public String toString() {
        return "ContextoPedido[tramos=" + tramos.size() + "]";
    }
}
