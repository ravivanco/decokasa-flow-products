package com.decokasa.seguimiento.asistente.application.port.in;

import com.decokasa.seguimiento.asistente.application.port.out.TarifasIA.TarifaRegistrada;
import com.decokasa.seguimiento.asistente.domain.Tarifa;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Tarifas del asistente (solo admin). Registrar no basta: una tarifa sin validar no permite llamar al proveedor. */
public interface GestionarTarifasIA {

    List<TarifaRegistrada> listar(Solicitante solicitante);

    TarifaRegistrada registrar(Solicitante solicitante, NuevaTarifa nueva);

    /** El admin confirma que la tarifa coincide con su fuente oficial. */
    TarifaRegistrada validar(Solicitante solicitante, UUID tarifaId);

    record NuevaTarifa(String modelo, LocalDate vigenteDesde, Tarifa.Precios base, Integer umbralTokensEntrada,
            Tarifa.Precios superior, String fuente, LocalDate consultadoEn) {
    }
}
