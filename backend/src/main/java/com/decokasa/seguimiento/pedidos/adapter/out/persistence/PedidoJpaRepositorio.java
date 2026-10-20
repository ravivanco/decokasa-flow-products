package com.decokasa.seguimiento.pedidos.adapter.out.persistence;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.EtapaCambio;
import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.pedidos.application.port.out.ConsultaDePedidos;
import com.decokasa.seguimiento.pedidos.application.port.out.PedidoRepositorio;
import com.decokasa.seguimiento.pedidos.domain.DemoraAduana;
import com.decokasa.seguimiento.pedidos.domain.EstadoEtapa;
import com.decokasa.seguimiento.pedidos.domain.EtapaDePedido;
import com.decokasa.seguimiento.pedidos.domain.NumeroPedido;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import com.decokasa.seguimiento.pedidos.domain.UrgenciaActiva;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;

/**
 * Adaptador JPA del puerto {@link PedidoRepositorio}. Se usa dentro de la transacción del caso de uso.
 *
 * <p>Índice etapa_pedido_una_actual (una sola ACTUAL por pedido, no diferible): al guardar, primero se escriben las
 * etapas que dejan de ser ACTUAL y después la que pasa a serlo. Si no, al volver atrás (A4 → A2) la base vería dos
 * etapas ACTUAL a la vez.
 */
@Component
public class PedidoJpaRepositorio implements PedidoRepositorio, ConsultaDePedidos {

    private static final String VIOLACION_DE_UNICIDAD = "23505";
    private static final String UNA_ACTUAL = "etapa_pedido_una_actual";
    private static final String CAMBIO_SIMULTANEO = "El pedido cambió mientras se guardaba. Vuelve a cargarlo.";
    // SQLSTATE propio del trigger pedido_bodega_activa (V12): la bodega no existe o se desactivó a la vez (F4-D2).
    private static final String BODEGA_INACTIVA = "UB001";

    private final EntityManager entityManager;

    public PedidoJpaRepositorio(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Primero bloquea la fila y solo después lee el pedido y sus etapas (P-11; 2.B2-D02).
     *
     * <p>Antes se usaba {@code find} y {@code refresh(…, PESSIMISTIC_WRITE)}: el orden entre leer y bloquear quedaba
     * en manos de Hibernate, y la segunda de dos acciones simultáneas podía armar el agregado con la fila del pedido
     * leída antes de obtener el bloqueo y con las etapas cargadas después, ya con el avance confirmado (etapa actual 3
     * con A4 ACTUAL). El agregado lo rechazaba con IllegalStateException en lugar de responder ETAPA_CAMBIO.
     *
     * <p>Ahora el bloqueo ({@code FOR NO KEY UPDATE}, el mismo que usa Hibernate para PESSIMISTIC_WRITE en
     * PostgreSQL) se obtiene con una sentencia propia que no carga nada. Cuando termina de esperar, la otra acción ya
     * confirmó: el {@code refresh} sin bloqueo relee pedido y etapas (en cascada) con lo último confirmado, aunque el
     * pedido ya estuviera en esta sesión. Así el caso de uso compara {@code etapaEsperada} con el estado vigente y la
     * segunda acción recibe ETAPA_CAMBIO sin cambios.
     */
    @Override
    public Optional<Pedido> buscarParaActualizar(UUID pedidoId) {
        boolean existe = !entityManager.createNativeQuery("select id from pedido where id = :id for no key update")
                .setParameter("id", pedidoId)
                .getResultList()
                .isEmpty();
        if (!existe) {
            return Optional.empty();
        }
        PedidoEntity entidad = entityManager.find(PedidoEntity.class, pedidoId);
        entityManager.refresh(entidad);
        return Optional.of(aDominio(entidad));
    }

    @Override
    public void insertar(Pedido pedido) {
        PedidoEntity entidad = new PedidoEntity(pedido.id(), pedido.numero().valor(), pedido.pais().name(),
                pedido.tipoFlujo().name(), pedido.flujoId(), pedido.incluyeProductosNuevos(), pedido.creadoPor(),
                pedido.creadoEn());
        copiarPedido(pedido, entidad);
        List<EntradaEtapaEntity> entradas = new ArrayList<>();
        for (EtapaDePedido etapa : pedido.etapas()) {
            EtapaPedidoEntity fila = new EtapaPedidoEntity(etapa.id(), entidad, (short) etapa.numero());
            entradaNueva(pedido, etapa, fila).ifPresent(entradas::add);
            copiarEtapa(etapa, fila);
            entidad.agregarEtapa(fila);
        }
        entityManager.persist(entidad);
        // Después del pedido (FK de entrada_etapa a pedido); la FK de etapa_pedido a la entrada es diferible (V3).
        entradas.forEach(entityManager::persist);
        vaciar();
    }

    @Override
    public void actualizar(Pedido pedido) {
        PedidoEntity entidad = entityManager.find(PedidoEntity.class, pedido.id());
        if (entidad == null) {
            throw new IllegalStateException("Solo se actualiza un pedido leído en esta transacción.");
        }
        Map<Integer, EtapaPedidoEntity> filas = entidad.getEtapas().stream()
                .collect(Collectors.toMap(fila -> (int) fila.getNumero(), Function.identity()));
        boolean salieron = false;
        for (EtapaDePedido etapa : pedido.etapas()) {
            EtapaPedidoEntity fila = filas.get(etapa.numero());
            if (EstadoEtapa.ACTUAL.name().equals(fila.getEstado()) && etapa.estado() != EstadoEtapa.ACTUAL) {
                copiarEtapa(etapa, fila);
                salieron = true;
            }
        }
        if (salieron) {
            vaciar();
        }
        for (EtapaDePedido etapa : pedido.etapas()) {
            EtapaPedidoEntity fila = filas.get(etapa.numero());
            entradaNueva(pedido, etapa, fila).ifPresent(entityManager::persist);
            copiarEtapa(etapa, fila);
        }
        copiarPedido(pedido, entidad);
        vaciar();
    }

    /** Una entrada (o reentrada) nueva de la etapa se registra en entrada_etapa, de solo inserción (P-18). */
    private static Optional<EntradaEtapaEntity> entradaNueva(Pedido pedido, EtapaDePedido etapa,
            EtapaPedidoEntity fila) {
        if (etapa.entradaId() == null || etapa.entradaId().equals(fila.getEntradaId())) {
            return Optional.empty();
        }
        return Optional.of(new EntradaEtapaEntity(etapa.entradaId(), pedido.id(), (short) etapa.numero(),
                etapa.entradaEn()));
    }

    // --- Consultas sin bloqueo (ConsultaDePedidos) ---

    @Override
    public Optional<Pedido> porId(UUID pedidoId) {
        return Optional.ofNullable(entityManager.find(PedidoEntity.class, pedidoId))
                .map(PedidoJpaRepositorio::aDominio);
    }

    @Override
    public Optional<Pedido> porNumero(String numero) {
        return entityManager.createQuery("select p from PedidoEntity p where p.numero = :numero", PedidoEntity.class)
                .setParameter("numero", numero)
                .getResultStream()
                .findFirst()
                .map(PedidoJpaRepositorio::aDominio);
    }

    @Override
    public Pagina listar(TipoFlujo tipoFlujo, Integer etapa, Boolean vencido, String texto, Instant ahora, int pagina,
            int tamano) {
        StringBuilder condiciones = new StringBuilder(" where 1 = 1");
        if (tipoFlujo != null) {
            condiciones.append(" and p.tipoFlujo = :tipoFlujo");
        }
        if (etapa != null) {
            condiciones.append(" and p.etapaActual = :etapa");
        }
        String vencida = "exists (select e.id from EtapaPedidoEntity e where e.pedido = p and e.estado = 'ACTUAL'"
                + " and e.venceEn is not null and e.venceEn < :ahora)";
        if (Boolean.TRUE.equals(vencido)) {
            condiciones.append(" and p.finalizado = false and ").append(vencida);
        } else if (Boolean.FALSE.equals(vencido)) {
            condiciones.append(" and not (p.finalizado = false and ").append(vencida).append(")");
        }
        if (texto != null) {
            condiciones.append(" and (lower(p.numero) like :texto escape '!'")
                    .append(" or lower(p.nombre) like :texto escape '!')");
        }
        TypedQuery<UUID> ids = entityManager.createQuery("select p.id from PedidoEntity p" + condiciones
                + " order by p.creadoEn desc, p.id", UUID.class);
        TypedQuery<Long> total = entityManager.createQuery("select count(p) from PedidoEntity p" + condiciones,
                Long.class);
        for (TypedQuery<?> consulta : List.of(ids, total)) {
            if (tipoFlujo != null) {
                consulta.setParameter("tipoFlujo", tipoFlujo.name());
            }
            if (etapa != null) {
                consulta.setParameter("etapa", etapa.shortValue());
            }
            if (vencido != null) {
                consulta.setParameter("ahora", ahora);
            }
            if (texto != null) {
                consulta.setParameter("texto", "%" + escaparComodines(texto.toLowerCase(Locale.ROOT)) + "%");
            }
        }
        List<UUID> deLaPagina = ids.setFirstResult(pagina * tamano).setMaxResults(tamano).getResultList();
        return new Pagina(cargarEnOrden(deLaPagina), total.getSingleResult());
    }

    @Override
    public Pagina bandeja(UUID usuarioId, Set<CodigoRol> roles, boolean admin, int pagina, int tamano) {
        // definicion_etapa da el rol responsable de la etapa actual en la versión del flujo de cada pedido.
        String desde = " from pedido p"
                + " join etapa_pedido e on e.pedido_id = p.id and e.estado = 'ACTUAL'"
                + " join definicion_etapa d on d.flujo_id = p.flujo_id and d.numero = e.numero"
                + " where not p.finalizado and (cast(:admin as boolean) or (d.rol_codigo in (:roles)"
                + " and (e.asignado_a is null or e.asignado_a = :usuario)))";
        List<String> codigos = roles.stream().map(CodigoRol::valor).toList();
        @SuppressWarnings("unchecked")
        List<Object> filas = entityManager.createNativeQuery("select cast(p.id as text)" + desde
                        + " order by p.creado_en desc, p.id limit :tamano offset :desde")
                .setParameter("admin", admin)
                .setParameter("roles", codigos)
                .setParameter("usuario", usuarioId)
                .setParameter("tamano", tamano)
                .setParameter("desde", (long) pagina * tamano)
                .getResultList();
        Number total = (Number) entityManager.createNativeQuery("select count(*)" + desde)
                .setParameter("admin", admin)
                .setParameter("roles", codigos)
                .setParameter("usuario", usuarioId)
                .getSingleResult();
        List<UUID> ids = filas.stream().map(fila -> UUID.fromString((String) fila)).toList();
        return new Pagina(cargarEnOrden(ids), total.longValue());
    }

    /** Carga los pedidos de una página con sus etapas en una consulta (sin N+1) y conserva el orden de la página. */
    private List<Pedido> cargarEnOrden(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, PedidoEntity> porId = entityManager.createQuery(
                        "select distinct p from PedidoEntity p left join fetch p.etapas where p.id in :ids",
                        PedidoEntity.class)
                .setParameter("ids", ids)
                // Materializa la colección completa antes de procesar las raíces del fetch join.
                .getResultList().stream()
                .collect(Collectors.toMap(PedidoEntity::getId, Function.identity()));
        return ids.stream().map(porId::get).map(PedidoJpaRepositorio::aDominio).toList();
    }

    private static String escaparComodines(String texto) {
        return texto.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    // Vacía la sesión aquí para traducir los conflictos de la base antes de publicar eventos.
    private void vaciar() {
        try {
            entityManager.flush();
        } catch (OptimisticLockException conflicto) {
            throw new EtapaCambio(CAMBIO_SIMULTANEO);
        } catch (PersistenceException error) {
            if (esViolacionDe(error, UNA_ACTUAL)) {
                throw new EtapaCambio(CAMBIO_SIMULTANEO);
            }
            if (tieneEstadoSql(error, BODEGA_INACTIVA)) {
                // Solo este rechazo: la bodega se desactivó entre la comprobación y el guardado. Nada se guarda.
                throw new DatoInvalido("bodegaId", "La bodega no existe o está inactiva; elige una bodega activa.");
            }
            throw error;
        }
    }

    private static boolean tieneEstadoSql(Throwable error, String estado) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sql && estado.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static boolean esViolacionDe(Throwable error, String restriccion) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacion) {
                return VIOLACION_DE_UNICIDAD.equals(violacion.getSQLState())
                        && restriccion.equals(violacion.getConstraintName());
            }
        }
        return false;
    }

    private static void copiarPedido(Pedido pedido, PedidoEntity entidad) {
        UrgenciaActiva urgencia = pedido.urgencia().orElse(null);
        entidad.cambiarEstado(pedido.nombre(), pedido.descripcion(), (short) pedido.etapaActual(), pedido.finalizado(),
                pedido.finalizadoEn(), urgencia == null ? null : urgencia.nivel().name(),
                urgencia == null ? null : urgencia.activadaEn(), urgencia == null ? null : urgencia.venceEn(),
                pedido.actualizadoEn());
        entidad.asignarBodega(pedido.bodegaId().orElse(null));
        DemoraAduana demora = pedido.demora().orElse(null);
        entidad.cambiarLlegadaYDemora(pedido.llegadaAduanaEn().orElse(null),
                demora == null ? null : demora.motivo(), demora == null ? null : demora.nuevaFechaEstimadaLlegada(),
                demora == null ? null : demora.reportadaPor(), demora == null ? null : demora.reportadaEn());
    }

    private static void copiarEtapa(EtapaDePedido etapa, EtapaPedidoEntity fila) {
        fila.cambiar(etapa.estado().name(), etapa.asignadoA(), etapa.entradaEn(), etapa.salidaEn(), etapa.venceEn(),
                etapa.reentradas(), etapa.entradaId());
    }

    private static Pedido aDominio(PedidoEntity entidad) {
        UrgenciaActiva urgencia = entidad.getUrgenciaNivel() == null ? null
                : new UrgenciaActiva(NivelUrgencia.valueOf(entidad.getUrgenciaNivel()),
                        entidad.getUrgenciaActivadaEn(), entidad.getUrgenciaVenceEn());
        DemoraAduana demora = entidad.getDemoraMotivo() == null ? null
                : new DemoraAduana(entidad.getDemoraMotivo(), entidad.getDemoraNuevaFechaLlegada(),
                        entidad.getDemoraReportadaPor(), entidad.getDemoraReportadaEn());
        return new Pedido(entidad.getId(), new NumeroPedido(entidad.getNumero()),
                CodigoPais.valueOf(entidad.getPaisCodigo()), TipoFlujo.valueOf(entidad.getTipoFlujo()),
                entidad.getFlujoId(), entidad.getNombre(), entidad.getDescripcion(),
                entidad.isIncluyeProductosNuevos(), entidad.getCreadoPor(), entidad.getCreadoEn(),
                entidad.getEtapas().stream().map(PedidoJpaRepositorio::etapaADominio).toList(),
                entidad.getEtapaActual(), entidad.isFinalizado(), entidad.getFinalizadoEn(), urgencia,
                entidad.getLlegadaAduanaEn(), demora, entidad.getBodegaId(), entidad.getActualizadoEn(),
                entidad.getVersion());
    }

    private static EtapaDePedido etapaADominio(EtapaPedidoEntity fila) {
        return new EtapaDePedido(fila.getId(), fila.getNumero(), EstadoEtapa.valueOf(fila.getEstado()),
                fila.getAsignadoA(), fila.getEntradaEn(), fila.getSalidaEn(), fila.getVenceEn(), fila.getReentradas(),
                fila.getEntradaId());
    }
}
