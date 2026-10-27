package com.decokasa.seguimiento.asistente.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Precisión 2 de Richard (2026-10-22): una referencia vale solo si apunta a una fuente autorizada **enviada en esa
 * consulta** y a una página que existe en ese PDF (o, en los datos del pedido, a un tramo cuyo texto coincide con lo
 * citado). Las demás se descartan y no cuentan como evidencia. Java puro: no conoce el SDK ni el proveedor.
 */
public final class VerificadorReferencias {

    static final int MAX_TEXTO_CITADO = 500;

    private VerificadorReferencias() {
    }

    public static List<Referencia> verificar(String numeroPedido, List<FuenteEnviada> fuentes,
            List<CitaPropuesta> citas) {
        Set<Referencia> validas = new LinkedHashSet<>();
        for (CitaPropuesta cita : citas) {
            if (cita.indiceDocumento() < 0 || cita.indiceDocumento() >= fuentes.size()) {
                continue;
            }
            FuenteEnviada fuente = fuentes.get((int) cita.indiceDocumento());
            Optional<Referencia> referencia = fuente.tipo() == FuenteEnviada.Tipo.PDF
                    ? dePdf(numeroPedido, fuente, cita)
                    : deDatos(numeroPedido, fuente, cita);
            referencia.ifPresent(validas::add);
        }
        return List.copyOf(validas);
    }

    /**
     * La página inicial debe existir. El fin se acepta inclusivo o exclusivo (hasta páginas + 1): la documentación de
     * citas lo define por PDF y Codex lo confirma en la parte B; nunca se acepta un inicio fuera del documento.
     */
    private static Optional<Referencia> dePdf(String numeroPedido, FuenteEnviada fuente, CitaPropuesta cita) {
        Long inicio = cita.paginaInicio();
        Long fin = cita.paginaFin();
        if (inicio == null || fin == null || inicio < 1 || inicio > fuente.paginas() || fin < inicio
                || fin > fuente.paginas() + 1L) {
            return Optional.empty();
        }
        return Optional.of(new Referencia(numeroPedido, fuente.etapa(), fuente.archivoId(), fuente.nombreArchivo(),
                Math.toIntExact(inicio), recortar(cita.textoCitado())));
    }

    private static Optional<Referencia> deDatos(String numeroPedido, FuenteEnviada fuente, CitaPropuesta cita) {
        Long inicio = cita.caracterInicio();
        Long fin = cita.caracterFin();
        String texto = fuente.texto();
        if (inicio == null || fin == null || inicio < 0 || fin <= inicio || fin > texto.length()
                || cita.textoCitado() == null) {
            return Optional.empty();
        }
        String enviado = normalizar(texto.substring(inicio.intValue(), fin.intValue()));
        if (enviado.isEmpty() || !enviado.equals(normalizar(cita.textoCitado()))) {
            return Optional.empty();
        }
        return Optional.of(new Referencia(numeroPedido, fuente.etapaEn(inicio.intValue()), null, null, null,
                recortar(cita.textoCitado())));
    }

    private static String normalizar(String texto) {
        return texto.strip().replaceAll("\\s+", " ");
    }

    private static String recortar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.strip();
        return limpio.length() <= MAX_TEXTO_CITADO ? limpio : limpio.substring(0, MAX_TEXTO_CITADO) + "…";
    }
}
