package com.decokasa.seguimiento.asistente.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Lectura de consulta_ia y presupuesto_ia para el admin. Sin textos: consulta_ia no los tiene. */
public interface ConsultaConsumoIA {

    /** Agrupado por día de Ecuador y usuario, solo de esos países. Los intentos rechazados por límite no cuentan. */
    List<FilaConsumo> porDiaYUsuario(Set<CodigoPais> paises, Instant desde, Instant hasta);

    Optional<EstadoPresupuesto> presupuesto(LocalDate periodo);

    record FilaConsumo(LocalDate fecha, UUID usuarioId, long consultas, long tokensEntrada, long tokensSalida,
            long costoMicroUsd) {
    }

    record EstadoPresupuesto(long limiteMicroUsd, long consumidoMicroUsd, long reservadoMicroUsd) {
    }
}
