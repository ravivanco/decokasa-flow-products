package com.decokasa.seguimiento.embarques.adapter.out.persistence;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.DatoDuplicado;
import com.decokasa.seguimiento.embarques.application.port.in.GestionarEmbarques;
import com.decokasa.seguimiento.embarques.application.port.out.EmbarquesRepositorio;
import com.decokasa.seguimiento.embarques.domain.CodigoBl;
import com.decokasa.seguimiento.embarques.domain.LlegadaEstimada;
import java.sql.Array;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** SQL de BL y envíos. Comparte la transacción de la acción, así que el historial y la foto se confirman juntos. */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class EmbarquesSql implements EmbarquesRepositorio {

    private static final String DUPLICADO = "Ese código de BL ya existe. Puedes vincularlo a este pedido.";
    private static final String FILA_BL = """
            select b.id, b.pais_codigo, b.codigo, b.fecha_salida, b.llegada_estimada, b.llegada_origen, b.transito_dias,
              b.bloqueado_en, b.version, b.packing_list_vigente_id, pl.version as version_pl, b.importadora
            from embarque_bl b
            left join embarque_bl_packing_list pl on pl.id = b.packing_list_vigente_id
            """;

    private final NamedParameterJdbcTemplate sql;

    public EmbarquesSql(DataSource fuente) {
        sql = new NamedParameterJdbcTemplate(fuente);
    }

    @Override
    public Optional<FilaBl> blParaActualizar(UUID blId) {
        // of b: el left join no admite for update sobre el lado opcional.
        return sql.query(FILA_BL + " where b.id = :id for update of b", new MapSqlParameterSource("id", blId),
                (r, n) -> filaBl(r)).stream().findFirst();
    }

    @Override
    public Optional<FilaBl> blPorCodigo(CodigoPais pais, String codigoNormalizado) {
        return sql.query(FILA_BL + " where b.pais_codigo = :pais and b.codigo_normalizado = :codigo",
                new MapSqlParameterSource("pais", pais.name()).addValue("codigo", codigoNormalizado),
                (r, n) -> filaBl(r)).stream().findFirst();
    }

    private static FilaBl filaBl(ResultSet r) throws SQLException {
        int versionPl = r.getInt("version_pl");
        Integer version = r.wasNull() ? null : versionPl;
        return new FilaBl(r.getObject("id", UUID.class), CodigoPais.valueOf(r.getString("pais_codigo")),
                r.getString("codigo"), fecha(r, "fecha_salida"), fecha(r, "llegada_estimada"),
                r.getString("llegada_origen"), r.getTimestamp("bloqueado_en") != null, r.getLong("version"),
                r.getObject("packing_list_vigente_id", UUID.class), version, r.getString("importadora"));
    }

    @Override
    public void insertarBl(UUID id, CodigoPais pais, CodigoBl codigo, LocalDate salida, LlegadaEstimada llegada,
            String importadora, UUID autor, Instant ahora) {
        try {
            sql.update("""
                    insert into embarque_bl (id, pais_codigo, codigo, codigo_normalizado, fecha_salida, llegada_estimada,
                      llegada_origen, transito_dias, importadora, creado_por, creado_en, actualizado_en)
                    values (:id, :pais, :codigo, :normalizado, :salida, :llegada, :origen, :transito, :importadora, :autor,
                      :ahora, :ahora)
                    """, fechas(salida, llegada).addValue("id", id).addValue("pais", pais.name())
                    .addValue("codigo", codigo.texto()).addValue("normalizado", codigo.normalizado())
                    .addValue("autor", autor).addValue("ahora", Timestamp.from(ahora)).addValue("importadora", importadora));
        } catch (DuplicateKeyException repetido) {
            throw new DatoDuplicado("codigo", DUPLICADO);
        }
    }

    @Override
    public boolean actualizarBl(UUID id, long versionEsperada, LocalDate salida, LlegadaEstimada llegada,
            String importadora, Instant ahora) {
        return sql.update("""
                update embarque_bl set fecha_salida = :salida, llegada_estimada = :llegada, llegada_origen = :origen,
                  transito_dias = :transito, importadora = :importadora, actualizado_en = :ahora, version = version + 1
                where id = :id and version = :version
                """, fechas(salida, llegada).addValue("id", id).addValue("version", versionEsperada)
                .addValue("ahora", Timestamp.from(ahora)).addValue("importadora", importadora)) == 1;
    }

    private static MapSqlParameterSource fechas(LocalDate salida, LlegadaEstimada llegada) {
        return new MapSqlParameterSource()
                .addValue("salida", salida == null ? null : Date.valueOf(salida), Types.DATE)
                .addValue("llegada", llegada.fecha() == null ? null : Date.valueOf(llegada.fecha()), Types.DATE)
                .addValue("origen", llegada.origen().name())
                .addValue("transito", llegada.transitoDias(), Types.SMALLINT);
    }

    @Override
    public boolean vinculado(UUID blId, UUID pedidoId) {
        return Boolean.TRUE.equals(sql.queryForObject("""
                select exists (select 1 from embarque_bl_pedido
                  where bl_id = :bl and pedido_id = :pedido and desvinculado_en is null)
                """, new MapSqlParameterSource("bl", blId).addValue("pedido", pedidoId), Boolean.class));
    }

    @Override
    public List<UUID> pedidosDelBl(UUID blId) {
        return sql.queryForList("""
                select pedido_id from embarque_bl_pedido where bl_id = :bl and desvinculado_en is null
                order by vinculado_en, pedido_id
                """, new MapSqlParameterSource("bl", blId), UUID.class);
    }

    @Override
    public void vincular(UUID blId, UUID pedidoId, UUID autor, Instant ahora) {
        sql.update("""
                insert into embarque_bl_pedido (id, bl_id, pedido_id, orden, vinculado_por, vinculado_en)
                select :id, :bl, :pedido, coalesce(max(orden), 0) + 1, :autor, :ahora
                from embarque_bl_pedido where pedido_id = :pedido
                """, new MapSqlParameterSource("id", UUID.randomUUID()).addValue("bl", blId).addValue("pedido", pedidoId)
                .addValue("autor", autor).addValue("ahora", Timestamp.from(ahora)));
    }

    @Override
    public boolean desvincular(UUID blId, UUID pedidoId, UUID autor, Instant ahora) {
        return sql.update("""
                update embarque_bl_pedido set desvinculado_por = :autor, desvinculado_en = :ahora
                where bl_id = :bl and pedido_id = :pedido and desvinculado_en is null
                """, new MapSqlParameterSource("bl", blId).addValue("pedido", pedidoId).addValue("autor", autor)
                .addValue("ahora", Timestamp.from(ahora))) == 1;
    }

    @Override
    public Optional<AdjuntoDelPedido> adjunto(UUID adjuntoId) {
        return sql.query("""
                select id, pedido_id, etapa, entrada_id, tipo_documento, nombre from adjunto where id = :id
                """, new MapSqlParameterSource("id", adjuntoId), (r, n) -> new AdjuntoDelPedido(r.getObject("id", UUID.class),
                r.getObject("pedido_id", UUID.class), r.getInt("etapa"), r.getObject("entrada_id", UUID.class),
                r.getString("tipo_documento"), r.getString("nombre"))).stream().findFirst();
    }

    @Override
    public Optional<Integer> versionDeAdjunto(UUID blId, UUID adjuntoId) {
        return sql.queryForList("""
                select version from embarque_bl_packing_list where bl_id = :bl and adjunto_id = :adjunto
                """, new MapSqlParameterSource("bl", blId).addValue("adjunto", adjuntoId), Integer.class)
                .stream().findFirst();
    }

    @Override
    public int registrarPackingList(UUID blId, UUID adjuntoId, UUID autor, Instant ahora) {
        UUID id = UUID.randomUUID();
        var parametros = new MapSqlParameterSource("id", id).addValue("bl", blId).addValue("adjunto", adjuntoId)
                .addValue("autor", autor).addValue("ahora", Timestamp.from(ahora));
        int version = sql.queryForObject("""
                insert into embarque_bl_packing_list (id, bl_id, version, adjunto_id, subido_por, subido_en)
                select :id, :bl, coalesce(max(version), 0) + 1, :adjunto, :autor, :ahora
                from embarque_bl_packing_list where bl_id = :bl
                returning version
                """, parametros, Integer.class);
        sql.update("""
                update embarque_bl set packing_list_vigente_id = :id, actualizado_en = :ahora, version = version + 1
                where id = :bl
                """, parametros);
        return version;
    }

    @Override
    public Optional<PackingListDelBl> packingList(UUID packingListId) {
        return sql.query("""
                select pl.id, pl.bl_id, pl.version, pl.adjunto_id, a.pedido_id, a.etapa
                from embarque_bl_packing_list pl join adjunto a on a.id = pl.adjunto_id
                where pl.id = :id
                """, new MapSqlParameterSource("id", packingListId), (r, n) -> new PackingListDelBl(
                r.getObject("id", UUID.class), r.getObject("bl_id", UUID.class), r.getInt("version"),
                r.getObject("adjunto_id", UUID.class), r.getObject("pedido_id", UUID.class), r.getInt("etapa")))
                .stream().findFirst();
    }

    @Override
    public boolean relacionado(UUID blId, UUID pedidoId) {
        return Boolean.TRUE.equals(sql.queryForObject("""
                select exists (select 1 from embarque_bl_pedido
                    where bl_id = :bl and pedido_id = :pedido and desvinculado_en is null)
                  or exists (select 1 from envio_embarque_bl eb join envio_embarque e on e.id = eb.envio_id
                    where eb.bl_id = :bl and e.pedido_id = :pedido)
                """, new MapSqlParameterSource("bl", blId).addValue("pedido", pedidoId), Boolean.class));
    }

    @Override
    public List<GestionarEmbarques.Bl> blsDelPedido(UUID pedidoId, boolean conRevisionPendiente) {
        Set<UUID> pendientes = new LinkedHashSet<>();
        if (conRevisionPendiente) {
            envioPendiente(pedidoId).ifPresent(envio -> revisionesPendientes(envio.id())
                    .forEach(revision -> pendientes.add(revision.blId())));
        }
        List<FilaBl> filas = sql.query(FILA_BL + """
                join embarque_bl_pedido v on v.bl_id = b.id and v.pedido_id = :pedido and v.desvinculado_en is null
                order by v.orden, b.codigo
                """, new MapSqlParameterSource("pedido", pedidoId), (r, n) -> filaBl(r));
        List<GestionarEmbarques.Bl> bls = new ArrayList<>();
        for (FilaBl fila : filas) {
            List<GestionarEmbarques.PackingList> versiones = versiones(fila.id());
            GestionarEmbarques.PackingList vigente = versiones.stream()
                    .filter(version -> version.id().equals(fila.packingListVigente()))
                    .findFirst().orElse(null);
            String estado = vigente == null ? "SIN_PACKING_LIST" : fila.fechaSalida() == null ? "SIN_SALIDA" : "LISTO";
            bls.add(new GestionarEmbarques.Bl(fila.id(), fila.codigo(), fila.fechaSalida(), fila.llegadaEstimada(),
                    fila.llegadaOrigen(), transito(fila.id()), fila.version(), fila.bloqueado(), estado, vigente,
                    versiones, pedidosVigentes(fila.id()), enAprobacion(fila.id()), pendientes.contains(fila.id()),
                    versionAprobada(fila.id()), fila.importadora()));
        }
        return List.copyOf(bls);
    }

    private Integer transito(UUID blId) {
        return sql.queryForObject("select transito_dias::int from embarque_bl where id = :id",
                new MapSqlParameterSource("id", blId), Integer.class);
    }

    private List<GestionarEmbarques.PackingList> versiones(UUID blId) {
        return sql.query("""
                select pl.id, pl.version, pl.adjunto_id, a.nombre, u.nombre as autor, pl.subido_en
                from embarque_bl_packing_list pl
                join adjunto a on a.id = pl.adjunto_id
                join usuario u on u.id = pl.subido_por
                where pl.bl_id = :bl order by pl.version
                """, new MapSqlParameterSource("bl", blId), (r, n) -> new GestionarEmbarques.PackingList(
                r.getObject("id", UUID.class), r.getInt("version"), r.getObject("adjunto_id", UUID.class),
                r.getString("nombre"), r.getString("autor"), r.getTimestamp("subido_en").toInstant()));
    }

    private List<GestionarEmbarques.PedidoDelBl> pedidosVigentes(UUID blId) {
        return sql.query("""
                select p.id, p.numero, u.nombre, v.vinculado_en
                from embarque_bl_pedido v join pedido p on p.id = v.pedido_id join usuario u on u.id = v.vinculado_por
                where v.bl_id = :bl and v.desvinculado_en is null order by v.vinculado_en, p.numero
                """, new MapSqlParameterSource("bl", blId), (r, n) -> new GestionarEmbarques.PedidoDelBl(
                r.getObject("id", UUID.class), r.getString("numero"), r.getString("nombre"),
                r.getTimestamp("vinculado_en").toInstant()));
    }

    private List<String> enAprobacion(UUID blId) {
        return sql.queryForList("""
                select p.numero from envio_embarque e
                join envio_embarque_bl eb on eb.envio_id = e.id join pedido p on p.id = e.pedido_id
                where e.estado = 'PENDIENTE' and eb.bl_id = :bl order by p.numero
                """, new MapSqlParameterSource("bl", blId), String.class);
    }

    private Integer versionAprobada(UUID blId) {
        return sql.queryForObject("""
                select max(pl.version) from envio_embarque e
                join envio_embarque_bl eb on eb.envio_id = e.id
                join embarque_bl_packing_list pl on pl.id = coalesce(
                  (select r.packing_list_id from envio_embarque_revision r
                    where r.envio_id = e.id and r.bl_id = eb.bl_id order by r.revisado_en desc limit 1),
                  eb.packing_list_id)
                where e.estado = 'APROBADO' and eb.bl_id = :bl
                """, new MapSqlParameterSource("bl", blId), Integer.class);
    }

    @Override
    public List<GestionarEmbarques.Envio> enviosDelPedido(UUID pedidoId) {
        return sql.query("""
                select e.id, e.numero, e.estado, u.nombre, e.enviado_en, e.cerrado_en
                from envio_embarque e join usuario u on u.id = e.enviado_por
                where e.pedido_id = :pedido order by e.numero desc
                """, new MapSqlParameterSource("pedido", pedidoId), (r, n) -> {
            UUID id = r.getObject("id", UUID.class);
            Timestamp cerrado = r.getTimestamp("cerrado_en");
            return new GestionarEmbarques.Envio(id, r.getInt("numero"), r.getString("estado"), r.getString("nombre"),
                    r.getTimestamp("enviado_en").toInstant(), cerrado == null ? null : cerrado.toInstant(),
                    filasDelEnvio(id), decisiones(id), revisiones(id));
        });
    }

    private List<GestionarEmbarques.FilaEnvio> filasDelEnvio(UUID envioId) {
        return sql.query("""
                select eb.bl_id, eb.codigo, eb.fecha_salida, eb.llegada_estimada, eb.llegada_origen, eb.packing_list_id,
                  pl.version, a.nombre, eb.importadora
                from envio_embarque_bl eb
                join embarque_bl_packing_list pl on pl.id = eb.packing_list_id
                join adjunto a on a.id = pl.adjunto_id
                where eb.envio_id = :envio order by eb.codigo
                """, new MapSqlParameterSource("envio", envioId), (r, n) -> {
            UUID blId = r.getObject("bl_id", UUID.class);
            return new GestionarEmbarques.FilaEnvio(blId, r.getString("codigo"), fecha(r, "fecha_salida"),
                    fecha(r, "llegada_estimada"), r.getString("llegada_origen"),
                    r.getObject("packing_list_id", UUID.class), r.getInt("version"), r.getString("nombre"),
                    pedidosDeLaFila(envioId, blId), r.getString("importadora"));
        });
    }

    private List<GestionarEmbarques.PedidoDelBl> pedidosDeLaFila(UUID envioId, UUID blId) {
        return sql.query("""
                select p.id, p.numero, u.nombre, f.vinculado_en
                from envio_embarque_bl_pedido f join pedido p on p.id = f.pedido_id join usuario u on u.id = f.vinculado_por
                where f.envio_id = :envio and f.bl_id = :bl order by f.vinculado_en, p.numero
                """, new MapSqlParameterSource("envio", envioId).addValue("bl", blId),
                (r, n) -> new GestionarEmbarques.PedidoDelBl(r.getObject("id", UUID.class), r.getString("numero"),
                        r.getString("nombre"), r.getTimestamp("vinculado_en").toInstant()));
    }

    private List<GestionarEmbarques.Decision> decisiones(UUID envioId) {
        return sql.query("""
                select d.decision, u.nombre, d.motivo, d.bl_senalados, d.decidido_en
                from envio_embarque_decision d join usuario u on u.id = d.usuario_id
                where d.envio_id = :envio order by d.decidido_en, d.id
                """, new MapSqlParameterSource("envio", envioId), (r, n) -> {
            Array senalados = r.getArray("bl_senalados");
            List<UUID> ids = new ArrayList<>();
            for (Object id : (Object[]) senalados.getArray()) {
                ids.add((UUID) id);
            }
            return new GestionarEmbarques.Decision(r.getString("decision"), r.getString("nombre"), r.getString("motivo"),
                    codigosDelEnvio(envioId, ids), r.getTimestamp("decidido_en").toInstant());
        });
    }

    private List<GestionarEmbarques.Revision> revisiones(UUID envioId) {
        return sql.query("""
                select r.bl_id, b.codigo, pl.version, u.nombre, r.revisado_en
                from envio_embarque_revision r
                join embarque_bl b on b.id = r.bl_id
                join embarque_bl_packing_list pl on pl.id = r.packing_list_id
                join usuario u on u.id = r.revisado_por
                where r.envio_id = :envio order by r.revisado_en
                """, new MapSqlParameterSource("envio", envioId), (r, n) -> new GestionarEmbarques.Revision(
                r.getObject("bl_id", UUID.class), r.getString("codigo"), r.getInt("version"), r.getString("nombre"),
                r.getTimestamp("revisado_en").toInstant()));
    }

    @Override
    public Optional<EnvioPendiente> envioPendiente(UUID pedidoId) {
        return sql.query("""
                select id, numero, enviado_por from envio_embarque where pedido_id = :pedido and estado = 'PENDIENTE'
                """, new MapSqlParameterSource("pedido", pedidoId), (r, n) -> new EnvioPendiente(
                r.getObject("id", UUID.class), r.getInt("numero"), r.getObject("enviado_por", UUID.class)))
                .stream().findFirst();
    }

    @Override
    public List<UUID> pedidosARevisar(UUID blId, UUID packingListVigente) {
        return sql.queryForList("""
                select e.pedido_id from envio_embarque e join envio_embarque_bl eb on eb.envio_id = e.id
                where e.estado = 'PENDIENTE' and eb.bl_id = :bl and eb.packing_list_id <> :vigente
                """, new MapSqlParameterSource("bl", blId).addValue("vigente", packingListVigente), UUID.class);
    }

    @Override
    public int registrarEnvio(UUID pedidoId, UUID entradaId, UUID autor, Instant ahora) {
        UUID id = UUID.randomUUID();
        var parametros = new MapSqlParameterSource("id", id).addValue("pedido", pedidoId).addValue("entrada", entradaId)
                .addValue("autor", autor).addValue("ahora", Timestamp.from(ahora));
        int numero = sql.queryForObject("""
                insert into envio_embarque (id, pedido_id, entrada_id, numero, confirma_asignacion, enviado_por, enviado_en)
                select :id, :pedido, :entrada, coalesce(max(numero), 0) + 1, true, :autor, :ahora
                from envio_embarque where pedido_id = :pedido
                returning numero
                """, parametros, Integer.class);
        sql.update("""
                insert into envio_embarque_bl (envio_id, bl_id, codigo, fecha_salida, llegada_estimada, llegada_origen,
                  packing_list_id, importadora)
                select :id, b.id, b.codigo, b.fecha_salida, b.llegada_estimada, b.llegada_origen, b.packing_list_vigente_id,
                  b.importadora
                from embarque_bl b join embarque_bl_pedido v on v.bl_id = b.id
                where v.pedido_id = :pedido and v.desvinculado_en is null
                """, parametros);
        sql.update("""
                insert into envio_embarque_bl_pedido (envio_id, bl_id, pedido_id, vinculado_por, vinculado_en)
                select :id, eb.bl_id, v.pedido_id, v.vinculado_por, v.vinculado_en
                from envio_embarque_bl eb join embarque_bl_pedido v on v.bl_id = eb.bl_id
                where eb.envio_id = :id and v.desvinculado_en is null
                """, parametros);
        return numero;
    }

    @Override
    public List<RevisionPendiente> revisionesPendientes(UUID envioId) {
        return sql.query("""
                select eb.bl_id, b.codigo, b.packing_list_vigente_id, pl.version
                from envio_embarque_bl eb
                join embarque_bl b on b.id = eb.bl_id
                join embarque_bl_packing_list pl on pl.id = b.packing_list_vigente_id
                where eb.envio_id = :envio and b.packing_list_vigente_id <> eb.packing_list_id
                  and not exists (select 1 from envio_embarque_revision r
                    where r.envio_id = eb.envio_id and r.packing_list_id = b.packing_list_vigente_id)
                order by b.codigo
                """, new MapSqlParameterSource("envio", envioId), (r, n) -> new RevisionPendiente(
                r.getObject("bl_id", UUID.class), r.getString("codigo"),
                r.getObject("packing_list_vigente_id", UUID.class), r.getInt("version")));
    }

    @Override
    public void marcarRevisada(UUID envioId, RevisionPendiente revision, UUID autor, Instant ahora) {
        sql.update("""
                insert into envio_embarque_revision (envio_id, bl_id, packing_list_id, revisado_por, revisado_en)
                values (:envio, :bl, :version, :autor, :ahora)
                """, new MapSqlParameterSource("envio", envioId).addValue("bl", revision.blId())
                .addValue("version", revision.packingListVigente()).addValue("autor", autor)
                .addValue("ahora", Timestamp.from(ahora)));
    }

    @Override
    public void registrarDecision(UUID envioId, String decision, UUID usuario, String motivo, List<UUID> blSenalados,
            Instant ahora) {
        var parametros = new MapSqlParameterSource("id", UUID.randomUUID()).addValue("envio", envioId)
                .addValue("decision", decision).addValue("usuario", usuario).addValue("motivo", motivo)
                .addValue("bls", "{" + blSenalados.stream().map(UUID::toString)
                        .collect(java.util.stream.Collectors.joining(",")) + "}")
                .addValue("ahora", Timestamp.from(ahora))
                .addValue("estado", "APROBADA".equals(decision) ? "APROBADO" : "RECHAZADO");
        sql.update("""
                insert into envio_embarque_decision (id, envio_id, decision, usuario_id, motivo, bl_senalados, decidido_en)
                values (:id, :envio, :decision, :usuario, :motivo, cast(:bls as uuid[]), :ahora)
                """, parametros);
        sql.update("update envio_embarque set estado = :estado, cerrado_en = :ahora where id = :envio", parametros);
    }

    @Override
    public void bloquearBls(UUID envioId, Instant ahora) {
        sql.update("""
                update embarque_bl b set bloqueado_en = :ahora, actualizado_en = :ahora, version = b.version + 1
                from envio_embarque_bl eb
                where eb.envio_id = :envio and eb.bl_id = b.id and b.bloqueado_en is null
                """, new MapSqlParameterSource("envio", envioId).addValue("ahora", Timestamp.from(ahora)));
    }

    @Override
    public List<String> codigosDelEnvio(UUID envioId, List<UUID> blIds) {
        if (blIds.isEmpty()) {
            return List.of();
        }
        return sql.queryForList("""
                select codigo from envio_embarque_bl where envio_id = :envio and bl_id in (:bls) order by codigo
                """, new MapSqlParameterSource("envio", envioId).addValue("bls", blIds), String.class);
    }

    @Override
    public GestionarEmbarques.Checklist checklist(UUID flujoId, UUID pedidoId, int etapa, UUID entradaId) {
        var parametros = new MapSqlParameterSource("flujo", flujoId).addValue("pedido", pedidoId)
                .addValue("etapa", etapa).addValue("entrada", entradaId);
        String requisitos = sql.queryForObject("""
                select coalesce(max(requisitos_documentales), 'PENDIENTES') from transicion_permitida
                where flujo_id = :flujo and etapa_origen = :etapa and accion = 'APROBAR'
                """, parametros, String.class);
        List<GestionarEmbarques.Requisito> filas = sql.query("""
                select d.tipo_documento, a.id, a.nombre, coalesce(m.marcado, false) as marcado, u.nombre as marco,
                  m.creado_en as marcado_en
                from transicion_permitida t
                join documento_requerido d on d.transicion_id = t.id
                left join lateral (select x.id, x.nombre from adjunto x
                  where x.pedido_id = :pedido and x.entrada_id = :entrada and x.tipo_documento = d.tipo_documento
                  order by x.subido_en desc, x.id limit 1) a on true
                left join lateral (select c.marcado, c.usuario_id, c.creado_en from checklist_marca c
                  where c.entrada_id = :entrada and c.tipo_documento = d.tipo_documento
                  order by c.creado_en desc, c.id desc limit 1) m on true
                left join usuario u on u.id = m.usuario_id and m.marcado
                where t.flujo_id = :flujo and t.etapa_origen = :etapa and t.accion = 'APROBAR'
                order by d.orden nulls last, d.tipo_documento
                """, parametros, (r, n) -> {
                    boolean marcado = r.getBoolean("marcado");
                    Timestamp en = r.getTimestamp("marcado_en");
                    return new GestionarEmbarques.Requisito(r.getString("tipo_documento"),
                            r.getObject("id", UUID.class), r.getString("nombre"), marcado,
                            marcado ? r.getString("marco") : null, marcado && en != null ? en.toInstant() : null);
                });
        int confirmados = (int) filas.stream().filter(GestionarEmbarques.Requisito::cumplido).count();
        return new GestionarEmbarques.Checklist(true, "PENDIENTES".equals(requisitos), filas.size(), confirmados, filas,
                entradaId, false);
    }

    @Override
    public void marcarRequisito(UUID id, UUID pedidoId, UUID entradaId, String tipoDocumento, boolean marcado,
            UUID usuarioId, Instant ahora) {
        sql.update("""
                insert into checklist_marca (id, pedido_id, entrada_id, tipo_documento, marcado, usuario_id, creado_en)
                values (:id, :pedido, :entrada, :tipo, :marcado, :usuario, :ahora)
                """, new MapSqlParameterSource("id", id).addValue("pedido", pedidoId).addValue("entrada", entradaId)
                .addValue("tipo", tipoDocumento).addValue("marcado", marcado).addValue("usuario", usuarioId)
                .addValue("ahora", Timestamp.from(ahora)));
    }

    @Override
    public Set<String> tiposMarcados(UUID entradaId) {
        return new LinkedHashSet<>(sql.queryForList("""
                select tipo_documento from (select distinct on (tipo_documento) tipo_documento, marcado
                  from checklist_marca where entrada_id = :entrada
                  order by tipo_documento, creado_en desc, id desc) x where marcado
                """, new MapSqlParameterSource("entrada", entradaId), String.class));
    }

    @Override
    public List<GestionarEmbarques.AlertaSimulada> alertasSimuladas(UUID pedidoId) {
        return sql.query("""
                select w.id, u.nombre, w.texto, w.creado_en
                from notificacion_whatsapp w join usuario u on u.id = w.usuario_id
                where w.pedido_id = :pedido order by w.creado_en desc, w.id
                """, new MapSqlParameterSource("pedido", pedidoId), (r, n) -> new GestionarEmbarques.AlertaSimulada(
                r.getObject("id", UUID.class), r.getString("nombre"), r.getString("texto"),
                r.getTimestamp("creado_en").toInstant()));
    }

    private static LocalDate fecha(ResultSet r, String columna) throws SQLException {
        Date valor = r.getDate(columna);
        return valor == null ? null : valor.toLocalDate();
    }
}
