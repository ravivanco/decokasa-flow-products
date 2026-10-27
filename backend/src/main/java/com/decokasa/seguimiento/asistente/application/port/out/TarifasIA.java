package com.decokasa.seguimiento.asistente.application.port.out;

import com.decokasa.seguimiento.asistente.domain.Tarifa;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** precio_modelo_ia (V21): tarifas versionadas por modelo y fecha. Una tarifa no se edita; se registra otra. */
public interface TarifasIA {

    List<TarifaRegistrada> listar();

    Optional<TarifaRegistrada> porId(UUID tarifaId);

    /** @throws com.decokasa.seguimiento.compartido.domain.DatoDuplicado si ya hay una del modelo para esa fecha */
    void registrar(Tarifa tarifa, UUID registradaPor, Instant ahora);

    /** @return false si no existe o ya estaba validada */
    boolean validar(UUID tarifaId, UUID validadaPor, Instant ahora);

    /** @param registradaPor nulo si la sembró una migración */
    record TarifaRegistrada(Tarifa tarifa, UUID registradaPor, Instant registradaEn, UUID validadaPor,
            Instant validadaEn) {
    }
}
