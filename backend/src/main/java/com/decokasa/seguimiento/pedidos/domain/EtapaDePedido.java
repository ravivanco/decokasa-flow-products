package com.decokasa.seguimiento.pedidos.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Una etapa de un pedido en su línea de tiempo (modelo-datos.md §3.3, etapa_pedido). Solo la modifica el agregado
 * {@link Pedido}. El plazo se reinicia en cada reentrada (requerimientos 4.2, invariantes). {@code venceEn} es nulo si
 * la etapa no tiene plazo o si su plazo está pendiente del cliente. Java puro (R1).
 */
public final class EtapaDePedido {

    private final UUID id;
    private final int numero;
    private EstadoEtapa estado;
    private UUID asignadoA;
    private Instant entradaEn;
    private Instant salidaEn;
    private Instant venceEn;
    private int reentradas;
    private UUID entradaId;
    private String motivoRetraso;

    /**
     * Reconstruye una etapa guardada.
     *
     * @param entradaId identificador de la entrada actual (o la última) a la etapa; nulo si el pedido nunca entró
     */
    public EtapaDePedido(UUID id, int numero, EstadoEtapa estado, UUID asignadoA, Instant entradaEn, Instant salidaEn,
            Instant venceEn, int reentradas, UUID entradaId) {
        this(id, numero, estado, asignadoA, entradaEn, salidaEn, venceEn, reentradas, entradaId, null);
    }

    /**
     * Reconstruye una etapa guardada con el motivo del retraso vigente de su entrada actual (fase 5, paso 5.2; F5-D3).
     *
     * @param motivoRetraso nulo si no hay motivo registrado en la entrada actual
     */
    public EtapaDePedido(UUID id, int numero, EstadoEtapa estado, UUID asignadoA, Instant entradaEn, Instant salidaEn,
            Instant venceEn, int reentradas, UUID entradaId, String motivoRetraso) {
        this.id = Objects.requireNonNull(id, "id");
        this.numero = numero;
        this.estado = Objects.requireNonNull(estado, "estado");
        this.asignadoA = asignadoA;
        this.entradaEn = entradaEn;
        this.salidaEn = salidaEn;
        this.venceEn = venceEn;
        this.reentradas = reentradas;
        this.entradaId = entradaId;
        this.motivoRetraso = motivoRetraso;
    }

    /** Reconstruye una etapa guardada sin identificador de entrada (anterior a V3). */
    public EtapaDePedido(UUID id, int numero, EstadoEtapa estado, UUID asignadoA, Instant entradaEn, Instant salidaEn,
            Instant venceEn, int reentradas) {
        this(id, numero, estado, asignadoA, entradaEn, salidaEn, venceEn, reentradas, null);
    }

    static EtapaDePedido pendiente(int numero) {
        return new EtapaDePedido(UUID.randomUUID(), numero, EstadoEtapa.PENDIENTE, null, null, null, null, 0, null);
    }

    /**
     * El pedido entra en la etapa. Si ya había entrado antes, es una reentrada y el plazo normal de la etapa empieza de
     * nuevo. La asignación no cambia: al entrar no se asigna a nadie y una asignación anterior se conserva (si una
     * reentrada la borra no está definido; borrarla ampliaría permisos sin que nadie lo decida).
     *
     * <p>Cada entrada recibe un identificador nuevo (P-18): solo los archivos subidos a esa entrada cumplen los
     * requisitos de la etapa; los de entradas anteriores quedan para el historial.
     */
    void entrar(Instant ahora, Instant vence) {
        entradaId = UUID.randomUUID();
        if (entradaEn != null) {
            reentradas++;
        }
        estado = EstadoEtapa.ACTUAL;
        entradaEn = ahora;
        salidaEn = null;
        venceEn = vence;
        // Una reentrada limpia el motivo vigente (F5-D3); los anteriores quedan en el historial.
        motivoRetraso = null;
    }

    /** El pedido sale de la etapa hacia adelante (COMPLETADA) o hacia atrás (DEVUELTA). */
    void salir(Instant ahora, EstadoEtapa nuevoEstado) {
        estado = nuevoEstado;
        salidaEn = ahora;
    }

    /** Motivo del retraso vigente de la entrada actual (F5-D3). Las reglas las decide {@link Pedido}. */
    void registrarMotivo(String motivo) {
        motivoRetraso = motivo;
    }

    /** El admin reasigna la etapa; quién puede recibirla lo decide {@link PermisosEtapa}. */
    void asignar(UUID usuarioId) {
        asignadoA = usuarioId;
    }

    /** Una etapa ya completada queda DEVUELTA cuando el pedido vuelve a una etapa anterior a ella. */
    void marcarDevuelta() {
        estado = EstadoEtapa.DEVUELTA;
    }

    public UUID id() {
        return id;
    }

    public int numero() {
        return numero;
    }

    public EstadoEtapa estado() {
        return estado;
    }

    public UUID asignadoA() {
        return asignadoA;
    }

    public Instant entradaEn() {
        return entradaEn;
    }

    public Instant salidaEn() {
        return salidaEn;
    }

    public Instant venceEn() {
        return venceEn;
    }

    public int reentradas() {
        return reentradas;
    }

    /** Motivo del retraso vigente de la entrada actual; nulo si no hay (F5-D3). */
    public String motivoRetraso() {
        return motivoRetraso;
    }

    /** Identificador de la entrada actual (o la última); nulo si el pedido nunca entró en la etapa. */
    public UUID entradaId() {
        return entradaId;
    }
}
