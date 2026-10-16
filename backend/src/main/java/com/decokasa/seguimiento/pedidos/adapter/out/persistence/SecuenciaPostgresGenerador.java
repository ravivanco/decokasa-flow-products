package com.decokasa.seguimiento.pedidos.adapter.out.persistence;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.pedidos.application.port.out.GeneradorNumeroPedido;
import com.decokasa.seguimiento.pedidos.domain.NumeroPedido;
import jakarta.persistence.EntityManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Generador de números sobre secuencia_pedido de V2 (paso 2.4; decisiones del equipo del 2026-10-15). En la conexión
 * de la transacción que crea el pedido:
 * <ol>
 *   <li>asegura la fila (país, año) con {@code INSERT … ON CONFLICT DO NOTHING};</li>
 *   <li>la incrementa con {@code UPDATE … RETURNING}, que la bloquea hasta el final de la transacción: dos altas
 *       simultáneas del mismo país y año reciben números distintos.</li>
 * </ol>
 * Si la transacción se revierte, el incremento también: ese número no perteneció a ningún pedido confirmado y puede
 * volver a entregarse (lectura A). No se garantiza ausencia de huecos. El trigger de V2 impide que la secuencia
 * retroceda o se borre. El formato lo arma {@link NumeroPedido}, que sigue provisional (P-03, ⏸).
 */
@Component
public class SecuenciaPostgresGenerador implements GeneradorNumeroPedido {

    private static final String ASEGURAR = "insert into secuencia_pedido (pais_codigo, anio, ultimo_numero, version) "
            + "values (?, ?, 0, 0) on conflict (pais_codigo, anio) do nothing";
    private static final String AVANZAR = "update secuencia_pedido set ultimo_numero = ultimo_numero + 1, "
            + "version = version + 1 where pais_codigo = ? and anio = ? returning ultimo_numero";

    private final EntityManager entityManager;

    public SecuenciaPostgresGenerador(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public NumeroPedido siguiente(CodigoPais pais, int anio) {
        int consecutivo = entityManager.unwrap(Session.class).doReturningWork(conexion -> {
            try (PreparedStatement asegurar = conexion.prepareStatement(ASEGURAR)) {
                asegurar.setString(1, pais.name());
                asegurar.setShort(2, (short) anio);
                asegurar.executeUpdate();
            }
            try (PreparedStatement avanzar = conexion.prepareStatement(AVANZAR)) {
                avanzar.setString(1, pais.name());
                avanzar.setShort(2, (short) anio);
                try (ResultSet fila = avanzar.executeQuery()) {
                    if (!fila.next()) {
                        throw new IllegalStateException("No se pudo reservar el número de pedido.");
                    }
                    return fila.getInt(1);
                }
            }
        });
        return NumeroPedido.componer(pais, anio, consecutivo);
    }
}
