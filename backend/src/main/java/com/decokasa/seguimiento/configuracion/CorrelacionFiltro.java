package com.decokasa.seguimiento.configuracion;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Identificador de correlación por petición (MDC {@code corr}, cabecera de respuesta {@code X-Correlacion}) y una línea
 * de resumen al terminar: método, ruta SIN query, estado HTTP, código del contrato y duración. Nunca registra cabeceras,
 * cuerpos, tokens, la query (puede llevar state, code o el ticket OAuth) ni el contenido de documentos.
 *
 * <p>INFO: escrituras y errores (lo que hay que poder rastrear en producción). DEBUG: lecturas y fragmentos de carga
 * correctos, que en INFO saturarían el log (25 por documento de 100 MiB).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelacionFiltro extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger("decokasa.http");
    private static final Pattern ENTRANTE = Pattern.compile("[A-Za-z0-9-]{8,64}");
    private final SecureRandom azar = new SecureRandom();

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String entrante = peticion.getHeader("X-Request-Id");
        String corr = entrante != null && ENTRANTE.matcher(entrante).matches() ? entrante : nuevo();
        MDC.put("corr", corr);
        respuesta.setHeader("X-Correlacion", corr);
        long inicio = System.nanoTime();
        try {
            cadena.doFilter(peticion, respuesta);
        } finally {
            resumir(peticion, respuesta, (System.nanoTime() - inicio) / 1_000_000);
            MDC.remove("codigo");
            MDC.remove("corr");
        }
    }

    private static void resumir(HttpServletRequest peticion, HttpServletResponse respuesta, long ms) {
        String ruta = peticion.getRequestURI();
        if (!ruta.startsWith("/api/") || ruta.equals("/api/v1/salud")) { return; }
        String metodo = peticion.getMethod();
        int estado = respuesta.getStatus();
        String codigo = MDC.get("codigo");
        boolean fragmento = ruta.endsWith("/fragmentos");
        boolean info = estado >= 400 || ms > 10_000 || (!"GET".equals(metodo) && !fragmento);
        if (info) {
            LOG.info("http {} {} -> {} codigo={} ms={}", metodo, ruta, estado, codigo == null ? "-" : codigo, ms);
        } else {
            LOG.debug("http {} {} -> {} ms={}", metodo, ruta, estado, ms);
        }
    }

    private String nuevo() {
        byte[] bytes = new byte[6];
        azar.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
