package com.decokasa.seguimiento.pedidos.adapter.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tabla pedido de V2. Número, país, tipo y flujo son {@code updatable = false} (y el trigger de V2 los protege). Las
 * etapas se insertan con el pedido y nunca se borran. Solo vive en el adaptador (R3).
 */
@Entity
@Table(name = "pedido")
public class PedidoEntity {

    @Id
    private UUID id;

    @Column(name = "numero", updatable = false)
    private String numero;

    @Column(name = "pais_codigo", updatable = false)
    private String paisCodigo;

    @Column(name = "tipo_flujo", updatable = false)
    private String tipoFlujo;

    @Column(name = "flujo_id", updatable = false)
    private UUID flujoId;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "incluye_productos_nuevos", updatable = false)
    private boolean incluyeProductosNuevos;

    @Column(name = "creado_por", updatable = false)
    private UUID creadoPor;

    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @Column(name = "etapa_actual")
    private short etapaActual;

    @Column(name = "finalizado")
    private boolean finalizado;

    @Column(name = "finalizado_en")
    private Instant finalizadoEn;

    @Column(name = "urgencia_nivel")
    private String urgenciaNivel;

    @Column(name = "urgencia_activada_en")
    private Instant urgenciaActivadaEn;

    @Column(name = "urgencia_vence_en")
    private Instant urgenciaVenceEn;

    // V9: check de llegada de A7.
    @Column(name = "llegada_aduana_en")
    private Instant llegadaAduanaEn;

    // V9: bodega de destino (F4-D2). El trigger de la base exige una bodega activa y, desde V12, que no cambie (UB002).
    @Column(name = "bodega_id")
    private UUID bodegaId;

    // V10: demora vigente en aduana (F4-D5); las cuatro columnas van juntas.
    @Column(name = "demora_motivo")
    private String demoraMotivo;

    @Column(name = "demora_nueva_fecha_llegada")
    private LocalDate demoraNuevaFechaLlegada;

    @Column(name = "demora_reportada_por")
    private UUID demoraReportadaPor;

    @Column(name = "demora_reportada_en")
    private Instant demoraReportadaEn;

    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    // Red de seguridad: las acciones bloquean la fila antes de leerla (P-11).
    @Version
    @Column(name = "version")
    private long version;

    @OneToMany(mappedBy = "pedido", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @OrderBy("numero")
    private List<EtapaPedidoEntity> etapas = new ArrayList<>();

    protected PedidoEntity() {
    }

    PedidoEntity(UUID id, String numero, String paisCodigo, String tipoFlujo, UUID flujoId,
            boolean incluyeProductosNuevos, UUID creadoPor, Instant creadoEn) {
        this.id = id;
        this.numero = numero;
        this.paisCodigo = paisCodigo;
        this.tipoFlujo = tipoFlujo;
        this.flujoId = flujoId;
        this.incluyeProductosNuevos = incluyeProductosNuevos;
        this.creadoPor = creadoPor;
        this.creadoEn = creadoEn;
    }

    void cambiarEstado(String nombre, String descripcion, short etapaActual, boolean finalizado, Instant finalizadoEn,
            String urgenciaNivel, Instant urgenciaActivadaEn, Instant urgenciaVenceEn, Instant actualizadoEn) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.etapaActual = etapaActual;
        this.finalizado = finalizado;
        this.finalizadoEn = finalizadoEn;
        this.urgenciaNivel = urgenciaNivel;
        this.urgenciaActivadaEn = urgenciaActivadaEn;
        this.urgenciaVenceEn = urgenciaVenceEn;
        this.actualizadoEn = actualizadoEn;
    }

    void cambiarLlegadaYDemora(Instant llegadaAduanaEn, String demoraMotivo, LocalDate demoraNuevaFechaLlegada,
            UUID demoraReportadaPor, Instant demoraReportadaEn) {
        this.llegadaAduanaEn = llegadaAduanaEn;
        this.demoraMotivo = demoraMotivo;
        this.demoraNuevaFechaLlegada = demoraNuevaFechaLlegada;
        this.demoraReportadaPor = demoraReportadaPor;
        this.demoraReportadaEn = demoraReportadaEn;
    }

    void asignarBodega(UUID bodegaId) {
        this.bodegaId = bodegaId;
    }

    void agregarEtapa(EtapaPedidoEntity etapa) {
        etapas.add(etapa);
    }

    UUID getId() {
        return id;
    }

    String getNumero() {
        return numero;
    }

    String getPaisCodigo() {
        return paisCodigo;
    }

    String getTipoFlujo() {
        return tipoFlujo;
    }

    UUID getFlujoId() {
        return flujoId;
    }

    String getNombre() {
        return nombre;
    }

    String getDescripcion() {
        return descripcion;
    }

    boolean isIncluyeProductosNuevos() {
        return incluyeProductosNuevos;
    }

    UUID getCreadoPor() {
        return creadoPor;
    }

    Instant getCreadoEn() {
        return creadoEn;
    }

    short getEtapaActual() {
        return etapaActual;
    }

    boolean isFinalizado() {
        return finalizado;
    }

    Instant getFinalizadoEn() {
        return finalizadoEn;
    }

    String getUrgenciaNivel() {
        return urgenciaNivel;
    }

    Instant getUrgenciaActivadaEn() {
        return urgenciaActivadaEn;
    }

    Instant getUrgenciaVenceEn() {
        return urgenciaVenceEn;
    }

    UUID getBodegaId() {
        return bodegaId;
    }

    Instant getLlegadaAduanaEn() {
        return llegadaAduanaEn;
    }

    String getDemoraMotivo() {
        return demoraMotivo;
    }

    LocalDate getDemoraNuevaFechaLlegada() {
        return demoraNuevaFechaLlegada;
    }

    UUID getDemoraReportadaPor() {
        return demoraReportadaPor;
    }

    Instant getDemoraReportadaEn() {
        return demoraReportadaEn;
    }

    Instant getActualizadoEn() {
        return actualizadoEn;
    }

    long getVersion() {
        return version;
    }

    List<EtapaPedidoEntity> getEtapas() {
        return etapas;
    }
}
