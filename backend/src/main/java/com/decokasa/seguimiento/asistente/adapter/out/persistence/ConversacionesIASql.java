package com.decokasa.seguimiento.asistente.adapter.out.persistence;

import com.decokasa.seguimiento.asistente.application.port.out.ConversacionesIA;
import com.decokasa.seguimiento.asistente.domain.Autor;
import com.decokasa.seguimiento.asistente.domain.ConversacionIA;
import com.decokasa.seguimiento.asistente.domain.DocumentoExcluido;
import com.decokasa.seguimiento.asistente.domain.MensajeIA;
import com.decokasa.seguimiento.asistente.domain.Referencia;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * conversacion_ia y mensaje_ia (V20) con SQL parametrizado. Cada método es una transacción corta propia: la llamada al
 * proveedor nunca ocurre dentro de una (ADR-0012).
 */
@Repository
public class ConversacionesIASql implements ConversacionesIA {

    private static final String COLUMNAS = "id, usuario_id, pedido_id, numero_pedido, pais_codigo, creada_en, "
            + "expira_en, preguntas";

    private final NamedParameterJdbcTemplate sql;
    private final JsonMapper json;

    public ConversacionesIASql(DataSource fuente, JsonMapper json) {
        this.sql = new NamedParameterJdbcTemplate(fuente);
        this.json = json;
    }

    @Override
    @Transactional
    public void insertar(ConversacionIA conversacion) {
        sql.update("""
                insert into conversacion_ia (id, usuario_id, pedido_id, numero_pedido, pais_codigo, creada_en,
                                             expira_en, preguntas)
                values (:id, :usuario, :pedido, :numero, :pais, :creada, :expira, :preguntas)
                """, new MapSqlParameterSource()
                .addValue("id", conversacion.id())
                .addValue("usuario", conversacion.usuarioId())
                .addValue("pedido", conversacion.pedidoId())
                .addValue("numero", conversacion.numeroPedido())
                .addValue("pais", conversacion.pais().name())
                .addValue("creada", Timestamp.from(conversacion.creadaEn()))
                .addValue("expira", Timestamp.from(conversacion.expiraEn()))
                .addValue("preguntas", conversacion.preguntas()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConversacionIA> porId(UUID conversacionId) {
        return sql.query("select " + COLUMNAS + " from conversacion_ia where id = :id",
                Map.of("id", conversacionId), (fila, n) -> conversacion(fila)).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina deUsuario(UUID usuarioId, UUID pedidoId, Set<CodigoPais> paises, Instant ahora, int pagina,
            int tamano) {
        if (paises.isEmpty()) {
            return new Pagina(List.of(), 0);
        }
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("usuario", usuarioId)
                .addValue("ahora", Timestamp.from(ahora))
                .addValue("paises", paises.stream().map(CodigoPais::name).toList())
                .addValue("pedido", pedidoId)
                .addValue("limite", tamano)
                .addValue("inicio", Math.multiplyExact((long) pagina, tamano));
        // Fragmento fijo: el valor del pedido siempre va como parámetro.
        String condicion = " where usuario_id = :usuario and expira_en > :ahora and pais_codigo in (:paises)"
                + (pedidoId == null ? "" : " and pedido_id = :pedido");
        List<ConversacionIA> elementos = sql.query("select " + COLUMNAS + " from conversacion_ia" + condicion
                + " order by creada_en desc, id limit :limite offset :inicio", parametros,
                (fila, n) -> conversacion(fila));
        Long total = sql.queryForObject("select count(*) from conversacion_ia" + condicion, parametros, Long.class);
        return new Pagina(elementos, total == null ? 0 : total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MensajeIA> mensajes(UUID conversacionId) {
        try {
            return leerMensajes(conversacionId);
        } catch (DataAccessException | JacksonException error) {
            throw sinContenido();
        }
    }

    private List<MensajeIA> leerMensajes(UUID conversacionId) {
        return sql.query("""
                select orden, autor, texto, referencias::text as referencias, documentos_enviados,
                       documentos_excluidos::text as documentos_excluidos, evidencia_suficiente, creado_en
                  from mensaje_ia
                 where conversacion_id = :id
                 order by orden
                """, Map.of("id", conversacionId), (fila, n) -> mensaje(fila));
    }

    @Override
    @Transactional
    public boolean agregarTurno(UUID conversacionId, int preguntasEsperadas, MensajeIA pregunta,
            MensajeIA respuesta, Instant ahora) {
        int filas = sql.update("""
                update conversacion_ia
                   set preguntas = preguntas + 1
                 where id = :id and preguntas = :esperadas and preguntas < :limite and expira_en > :ahora
                """, new MapSqlParameterSource()
                .addValue("id", conversacionId)
                .addValue("esperadas", preguntasEsperadas)
                .addValue("limite", ConversacionIA.LIMITE_PREGUNTAS)
                .addValue("ahora", Timestamp.from(ahora)));
        if (filas == 0) {
            return false;
        }
        try {
            insertarMensaje(conversacionId, pregunta);
            insertarMensaje(conversacionId, respuesta);
        } catch (DataAccessException | JacksonException error) {
            throw sinContenido();
        }
        return true;
    }

    /**
     * Un error de PostgreSQL o de JSON puede repetir la fila ("Failing row contains …") y con ella la pregunta o la
     * respuesta. Se reemplaza por uno sin causa ni mensaje del original antes de que llegue al manejador de errores,
     * que registra la traza. La transacción se revierte igual (excepción no comprobada).
     */
    private static IllegalStateException sinContenido() {
        return new IllegalStateException("No se pudieron leer o guardar los mensajes del asistente");
    }

    @Override
    @Transactional
    public int borrarVencidas(Instant ahora, int lote) {
        return sql.update("""
                delete from conversacion_ia
                 where id in (select id from conversacion_ia where expira_en <= :ahora
                               order by expira_en limit :lote)
                """, new MapSqlParameterSource()
                .addValue("ahora", Timestamp.from(ahora))
                .addValue("lote", lote));
    }

    private void insertarMensaje(UUID conversacionId, MensajeIA mensaje) {
        sql.update("""
                insert into mensaje_ia (id, conversacion_id, orden, autor, texto, referencias, documentos_enviados,
                                        documentos_excluidos, evidencia_suficiente, creado_en)
                values (:id, :conversacion, :orden, :autor, :texto, cast(:referencias as jsonb),
                        cast(:enviados as uuid[]), cast(:excluidos as jsonb), :evidencia, :creado)
                """, new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("conversacion", conversacionId)
                .addValue("orden", mensaje.orden())
                .addValue("autor", mensaje.autor().name())
                .addValue("texto", mensaje.texto())
                .addValue("referencias", json.writeValueAsString(referenciasJson(mensaje.referencias())))
                .addValue("enviados", arregloUuid(mensaje.documentosEnviados()))
                .addValue("excluidos", json.writeValueAsString(excluidosJson(mensaje.documentosExcluidos())))
                .addValue("evidencia", mensaje.evidenciaSuficiente())
                .addValue("creado", Timestamp.from(mensaje.creadoEn())));
    }

    private static ConversacionIA conversacion(ResultSet fila) throws SQLException {
        return new ConversacionIA(fila.getObject("id", UUID.class), fila.getObject("usuario_id", UUID.class),
                fila.getObject("pedido_id", UUID.class), fila.getString("numero_pedido"),
                CodigoPais.valueOf(fila.getString("pais_codigo")), fila.getTimestamp("creada_en").toInstant(),
                fila.getTimestamp("expira_en").toInstant(), fila.getInt("preguntas"));
    }

    private MensajeIA mensaje(ResultSet fila) throws SQLException {
        Object evidencia = fila.getObject("evidencia_suficiente");
        return new MensajeIA(fila.getInt("orden"), Autor.valueOf(fila.getString("autor")), fila.getString("texto"),
                referencias(json.readTree(fila.getString("referencias"))),
                uuids(fila.getArray("documentos_enviados")),
                excluidos(json.readTree(fila.getString("documentos_excluidos"))),
                evidencia == null ? null : (Boolean) evidencia, fila.getTimestamp("creado_en").toInstant());
    }

    private static List<Map<String, Object>> referenciasJson(List<Referencia> referencias) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Referencia referencia : referencias) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("numeroPedido", referencia.numeroPedido());
            mapa.put("etapa", referencia.etapa());
            mapa.put("archivoId", referencia.archivoId() == null ? null : referencia.archivoId().toString());
            mapa.put("nombreArchivo", referencia.nombreArchivo());
            mapa.put("pagina", referencia.pagina());
            mapa.put("textoCitado", referencia.textoCitado());
            lista.add(mapa);
        }
        return lista;
    }

    private static List<Map<String, Object>> excluidosJson(List<DocumentoExcluido> excluidos) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (DocumentoExcluido excluido : excluidos) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("archivoId", excluido.archivoId().toString());
            mapa.put("nombreArchivo", excluido.nombreArchivo());
            mapa.put("motivo", excluido.motivo().name());
            lista.add(mapa);
        }
        return lista;
    }

    private static List<Referencia> referencias(JsonNode nodos) {
        List<Referencia> lista = new ArrayList<>();
        for (JsonNode nodo : nodos) {
            String archivo = texto(nodo, "archivoId");
            lista.add(new Referencia(texto(nodo, "numeroPedido"), entero(nodo, "etapa"),
                    archivo == null ? null : UUID.fromString(archivo), texto(nodo, "nombreArchivo"),
                    entero(nodo, "pagina"), texto(nodo, "textoCitado")));
        }
        return lista;
    }

    private static List<DocumentoExcluido> excluidos(JsonNode nodos) {
        List<DocumentoExcluido> lista = new ArrayList<>();
        for (JsonNode nodo : nodos) {
            lista.add(new DocumentoExcluido(UUID.fromString(texto(nodo, "archivoId")), texto(nodo, "nombreArchivo"),
                    DocumentoExcluido.Motivo.valueOf(texto(nodo, "motivo"))));
        }
        return lista;
    }

    private static String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor == null || valor.isNull() ? null : valor.asString();
    }

    private static Integer entero(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor == null || valor.isNull() ? null : valor.asInt();
    }

    /** Literal de arreglo de PostgreSQL ({uuid,uuid}); los UUID no llevan comas ni comillas. */
    static String arregloUuid(Set<UUID> ids) {
        return ids.stream().map(UUID::toString).collect(Collectors.joining(",", "{", "}"));
    }

    static Set<UUID> uuids(Array arreglo) throws SQLException {
        if (arreglo == null) {
            return Set.of();
        }
        Set<UUID> ids = new LinkedHashSet<>();
        for (Object valor : (Object[]) arreglo.getArray()) {
            ids.add(valor instanceof UUID id ? id : UUID.fromString(valor.toString()));
        }
        return ids;
    }
}
