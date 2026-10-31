package com.decokasa.seguimiento.configuracion;

import java.net.URI;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Resumen de la configuración efectiva al arrancar, para diagnosticar un despliegue sin entrar a la consola: versión,
 * modo de almacenamiento, flags de Google y trabajadores, versión del esquema y si hay una autorización OAuth guardada.
 * Nunca registra valores secretos: de claves, llaves, clientes y cuentas solo dice si están configurados; del retorno
 * OAuth solo el host. Avisa (WARN) de las combinaciones que dejan documentos pendientes o sin carpeta.
 */
@Component
public class DiagnosticoArranque {
    private static final Logger LOG = LoggerFactory.getLogger("decokasa.arranque");
    private final Environment entorno;
    private final DataSource fuente;

    public DiagnosticoArranque(Environment entorno, DataSource fuente) {
        this.entorno = entorno;
        this.fuente = fuente;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void informar() {
        String commit = valor("RENDER_GIT_COMMIT", "");
        boolean enRender = !valor("RENDER", "").isBlank() || !commit.isBlank();
        String destino = valor("decokasa.archivos.destino", "local").strip().toLowerCase(java.util.Locale.ROOT);
        boolean oauth = activo("decokasa.google.oauth.habilitada");
        boolean drive = activo("decokasa.google.drive.habilitada");
        boolean trabajador = activo("decokasa.google.drive.worker.automatico");
        String almacen = valor("decokasa.google.oauth.almacen", "archivo");

        LOG.info("arranque version={} rama={} render={} perfiles={}", commit.isBlank() ? "local"
                : commit.substring(0, Math.min(7, commit.length())), valor("RENDER_GIT_BRANCH", "-"), enRender,
                String.join(",", entorno.getActiveProfiles()));
        LOG.info("arranque esquema={} autorizacion_google_guardada={}", esquema(), autorizacionGuardada(almacen));
        LOG.info("arranque archivos destino={} tamano_maximo={} directorio_temporal={}", destino,
                valor("decokasa.archivos.tamano-maximo", "-"), valor("decokasa.archivos.directorio", "-"));
        LOG.info("arranque google oauth={} almacen={} retorno_host={} publicacion={} cuenta={} cliente={} llave={}",
                oauth, almacen, host(valor("decokasa.google.oauth.redirect-uri", "")),
                valor("decokasa.google.oauth.publicacion", "-"), presente("decokasa.google.oauth.cuenta-email"),
                "base".equals(almacen) ? presente("decokasa.google.oauth.cliente-id")
                        : presente("decokasa.google.oauth.cliente-ruta"),
                "base".equals(almacen) ? presente("decokasa.google.oauth.llave")
                        : presente("decokasa.google.oauth.llave-ruta"));
        LOG.info("arranque google drive={} trabajador={} intervalo_ms={} copiar_desde={}", drive, trabajador,
                valor("decokasa.google.drive.worker.intervalo-ms", "-"),
                valor("decokasa.google.drive.worker.copiar-desde", "").isBlank() ? "-"
                        : valor("decokasa.google.drive.worker.copiar-desde", ""));
        LOG.info("arranque apagados gmail={} correo_automatico={} recuperacion_envio={} ia={}",
                activo("decokasa.google.gmail.habilitada"), activo("decokasa.google.gmail.worker.automatico"),
                activo("decokasa.recuperacion.envio-automatico"), activo("decokasa.ia.habilitada"));

        if ("local".equals(destino) && enRender) {
            LOG.warn("arranque ARCHIVOS_DESTINO=local en Render: los documentos se guardarían en un disco efímero y "
                    + "quedarían «Copia a Drive pendiente». Configura ARCHIVOS_DESTINO=drive.");
        }
        if ("drive".equals(destino) && (!oauth || !drive)) {
            LOG.warn("arranque ARCHIVOS_DESTINO=drive con GOOGLE_OAUTH_HABILITADA={} y GOOGLE_DRIVE_HABILITADA={}: "
                    + "las cargas responderán 503 hasta activar ambos.", oauth, drive);
        }
        if (oauth && drive && !trabajador) {
            LOG.warn("arranque COPIA_DRIVE_AUTOMATICA=false: la carpeta de cada pedido no se crea al darlo de alta; "
                    + "se crea con su primera carga.");
        }
        if (oauth && "base".equals(almacen) && (!presente("decokasa.google.oauth.cliente-id")
                || !presente("decokasa.google.oauth.cliente-secreto") || !presente("decokasa.google.oauth.llave"))) {
            LOG.warn("arranque GOOGLE_OAUTH_ALMACEN=base sin cliente, secreto o llave: Google no podrá usarse.");
        }
    }

    private String esquema() {
        try {
            List<String> filas = new JdbcTemplate(fuente).queryForList(
                    "select version from flyway_schema_history where success and version is not null "
                            + "order by installed_rank desc limit 1", String.class);
            return filas.isEmpty() ? "-" : "v" + filas.getFirst();
        } catch (RuntimeException error) {
            return "desconocido";
        }
    }

    /** Solo si existe la fila (sin leer ni descifrar el contenido). */
    private String autorizacionGuardada(String almacen) {
        if (!"base".equals(almacen)) { return "no-aplica(archivo)"; }
        try {
            Integer filas = new JdbcTemplate(fuente).queryForObject("select count(*) from credencial_google",
                    Integer.class);
            return filas != null && filas > 0 ? "si" : "no";
        } catch (RuntimeException error) {
            return "desconocido";
        }
    }

    private String valor(String clave, String porDefecto) {
        String v = entorno.getProperty(clave);
        return v == null ? porDefecto : v;
    }

    private boolean activo(String clave) {
        return Boolean.parseBoolean(valor(clave, "false").strip());
    }

    private boolean presente(String clave) {
        return !valor(clave, "").isBlank();
    }

    private static String host(String uri) {
        try { return uri.isBlank() ? "-" : URI.create(uri).getHost(); }
        catch (IllegalArgumentException error) { return "invalido"; }
    }
}
