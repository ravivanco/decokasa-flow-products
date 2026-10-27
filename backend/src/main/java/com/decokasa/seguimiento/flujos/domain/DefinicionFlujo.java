package com.decokasa.seguimiento.flujos.domain;

import com.decokasa.seguimiento.compartido.domain.AccionEtapa;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.compartido.domain.TransicionNoPermitida;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeFlujo;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Una versión de un flujo con sus etapas y transiciones (ADR-0004, aceptado por el equipo el 2026-10-15). Las reglas
 * de cada etapa salen de estos datos, no de condiciones por número de etapa en el código; así se evitan los errores
 * 1 y 2 del demo (auditoria-demo.md §6). Un pedido sigue la versión con la que se creó. Java puro (R1).
 *
 * <p>Implementa {@link ContratoDeFlujo}, lo único que otros módulos pueden leer de ella (R4).
 */
public final class DefinicionFlujo implements ContratoDeFlujo {

    private final UUID id;
    private final CodigoPais pais;
    private final TipoFlujo tipo;
    private final int version;
    private final boolean vigente;
    private final List<DefinicionEtapa> etapas;
    private final List<TransicionPermitida> transiciones;

    public DefinicionFlujo(UUID id, CodigoPais pais, TipoFlujo tipo, int version, boolean vigente,
            List<DefinicionEtapa> etapas, List<TransicionPermitida> transiciones) {
        this.id = Objects.requireNonNull(id, "id");
        this.pais = Objects.requireNonNull(pais, "pais");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        if (version < 1) {
            throw new DatoInvalido("La versión de un flujo empieza en 1.");
        }
        this.version = version;
        this.vigente = vigente;
        this.etapas = etapas.stream().sorted(Comparator.comparingInt(DefinicionEtapa::numero)).toList();
        this.transiciones = List.copyOf(transiciones);
        if (this.etapas.isEmpty()) {
            throw new DatoInvalido("Un flujo necesita al menos una etapa.");
        }
        if (this.etapas.stream().map(DefinicionEtapa::numero).distinct().count() != this.etapas.size()) {
            throw new DatoInvalido("Dos etapas del flujo tienen el mismo número.");
        }
        for (DefinicionEtapa etapa : this.etapas) {
            if (etapa.revisaEtapa() != null) {
                exigirEtapa(etapa.revisaEtapa());
            }
        }
        if (this.etapas.stream().filter(DefinicionEtapa::registraLlegada).count() > 1) {
            throw new DatoInvalido("Un flujo tiene como máximo una etapa de llegada (ADR-0018).");
        }
        for (TransicionPermitida transicion : this.transiciones) {
            exigirEtapa(transicion.etapaOrigen());
            if (transicion.etapaDestino() != null) {
                exigirEtapa(transicion.etapaDestino());
            }
            // Fase 6, paso 6.4: ninguna etapa automática (Fin) admite acciones manuales, en ningún flujo.
            if (etapa(transicion.etapaOrigen()).automatica()) {
                throw new DatoInvalido("La etapa " + transicion.etapaOrigen()
                        + " es automática: no admite acciones manuales.");
            }
        }
    }

    /**
     * La transición de la acción en la etapa de origen.
     *
     * @throws TransicionNoPermitida si la acción no existe en esa etapa
     */
    public TransicionPermitida resolver(int etapaOrigen, AccionEtapa accion) {
        return transiciones.stream()
                .filter(transicion -> transicion.etapaOrigen() == etapaOrigen && transicion.accion() == accion)
                .findFirst()
                .orElseThrow(() -> new TransicionNoPermitida("La acción no está permitida en esta etapa."));
    }

    @Override
    public DefinicionEtapa etapa(int numero) {
        return etapas.stream()
                .filter(etapa -> etapa.numero() == numero)
                .findFirst()
                .orElseThrow(() -> new DatoInvalido("La etapa " + numero + " no existe en este flujo."));
    }

    /** La primera etapa del flujo (A1 / B1), donde nace un pedido. */
    @Override
    public DefinicionEtapa etapaInicial() {
        return etapas.get(0);
    }

    /**
     * Una etapa desde la que se devuelve por producto nuevo (A4 / B5): al volver a ella termina la urgencia
     * (requerimientos 4.2 #11). Se deduce de los datos, no del número de etapa.
     */
    public boolean esRetornoDeUrgencia(int numeroEtapa) {
        return transiciones.stream().anyMatch(transicion -> transicion.etapaOrigen() == numeroEtapa
                && transicion.accion() == AccionEtapa.DEVOLVER_POR_PRODUCTO_NUEVO);
    }

    private void exigirEtapa(int numero) {
        if (etapas.stream().noneMatch(etapa -> etapa.numero() == numero)) {
            throw new DatoInvalido("Una transición apunta a la etapa " + numero + ", que no existe en el flujo.");
        }
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public CodigoPais pais() {
        return pais;
    }

    @Override
    public TipoFlujo tipo() {
        return tipo;
    }

    @Override
    public int version() {
        return version;
    }

    public boolean vigente() {
        return vigente;
    }

    @Override
    public List<DefinicionEtapa> etapas() {
        return etapas;
    }

    public List<TransicionPermitida> transiciones() {
        return transiciones;
    }
}
