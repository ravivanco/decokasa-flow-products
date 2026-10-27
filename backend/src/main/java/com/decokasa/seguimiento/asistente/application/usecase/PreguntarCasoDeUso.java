package com.decokasa.seguimiento.asistente.application.usecase;

import com.decokasa.seguimiento.asistente.application.port.in.Preguntar;
import com.decokasa.seguimiento.asistente.application.port.out.AsistenteIA;
import com.decokasa.seguimiento.asistente.application.port.out.ConversacionesIA;
import com.decokasa.seguimiento.asistente.application.port.out.CuentasParaAsistente;
import com.decokasa.seguimiento.asistente.application.port.out.DocumentosParaAsistente;
import com.decokasa.seguimiento.asistente.application.port.out.DocumentosParaAsistente.DocumentoPedido;
import com.decokasa.seguimiento.asistente.application.port.out.ManifiestoPiloto;
import com.decokasa.seguimiento.asistente.application.port.out.PedidosParaAsistente;
import com.decokasa.seguimiento.asistente.application.port.out.PedidosParaAsistente.DatosPedido;
import com.decokasa.seguimiento.asistente.application.port.out.PedidosParaAsistente.EtapaPedido;
import com.decokasa.seguimiento.asistente.application.port.out.PresupuestoIA;
import com.decokasa.seguimiento.asistente.application.port.out.RegistroConsultasIA;
import com.decokasa.seguimiento.asistente.application.port.out.RegistroConsultasIA.ConsultaRegistrada;
import com.decokasa.seguimiento.asistente.domain.Autor;
import com.decokasa.seguimiento.asistente.domain.ContextoExcedido;
import com.decokasa.seguimiento.asistente.domain.ConversacionIA;
import com.decokasa.seguimiento.asistente.domain.DocumentoExcluido;
import com.decokasa.seguimiento.asistente.domain.FalloProveedorIA;
import com.decokasa.seguimiento.asistente.domain.FuenteEnviada;
import com.decokasa.seguimiento.asistente.domain.IaNoDisponible;
import com.decokasa.seguimiento.asistente.domain.LimiteConsumo;
import com.decokasa.seguimiento.asistente.domain.LimitePreguntas;
import com.decokasa.seguimiento.asistente.domain.MensajeIA;
import com.decokasa.seguimiento.asistente.domain.Referencia;
import com.decokasa.seguimiento.asistente.domain.VerificadorReferencias;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.ArchivoNoDisponible;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Una pregunta sobre el pedido de la conversación (contrato 0.19.0; ADR-0020 a ADR-0023). Orden:
 * <ol>
 * <li>permisos vigentes (conversación propia, cuenta, rol F7-D4, país, pedido);</li>
 * <li>controles completos ({@link ControlesAsistente}); sin ellos no se llama a nada del proveedor;</li>
 * <li>historial sin mensajes retenidos y documentos elegibles (P-17, PDF, manifiesto, huella);</li>
 * <li>count_tokens de todo lo que se enviaría (instrucciones, historial, datos, PDF y pregunta) contra el límite de
 * entrada; los PDF que no caben se excluyen;</li>
 * <li>reserva atómica del peor caso;</li>
 * <li>una llamada, fuera de cualquier transacción;</li>
 * <li>cierre de la reserva, verificación de referencias, registro sin texto y guardado del turno.</li>
 * </ol>
 */
public final class PreguntarCasoDeUso implements Preguntar {

    static final int MAX_PREGUNTA = 2_000;
    static final String TIPO_PDF = "application/pdf";
    static final String AVISO_SIN_EVIDENCIA = "No encontré evidencia verificable en los datos ni en los documentos "
            + "de este pedido para responder con seguridad.";
    static final String RESPUESTA_RECHAZADA = "No puedo responder esa pregunta.";
    static final String AVISO_INCOMPLETA = "(La respuesta quedó incompleta.)";
    static final String MOTIVO_LIMITE_GASTO = "El proveedor informó el límite de gasto del workspace o de la "
            + "organización. Revisa la Console antes de reactivar.";

    private final AccesoAsistente acceso;
    private final ControlesAsistente controles;
    private final PedidosParaAsistente pedidos;
    private final DocumentosParaAsistente documentos;
    private final ConversacionesIA conversaciones;
    private final RegistroConsultasIA registro;
    private final ManifiestoPiloto manifiesto;
    private final AsistenteIA asistente;
    private final PresupuestoIA presupuesto;
    private final Reloj reloj;
    private final ConfiguracionAsistente configuracion;

    public PreguntarCasoDeUso(CuentasParaAsistente cuentas, PedidosParaAsistente pedidos,
            DocumentosParaAsistente documentos, ConversacionesIA conversaciones, RegistroConsultasIA registro,
            ManifiestoPiloto manifiesto, AsistenteIA asistente, PresupuestoIA presupuesto, Reloj reloj,
            ConfiguracionAsistente configuracion) {
        this.acceso = new AccesoAsistente(cuentas, conversaciones);
        this.controles = new ControlesAsistente(configuracion, presupuesto);
        this.pedidos = pedidos;
        this.documentos = documentos;
        this.conversaciones = conversaciones;
        this.registro = registro;
        this.manifiesto = manifiesto;
        this.asistente = asistente;
        this.presupuesto = presupuesto;
        this.reloj = reloj;
        this.configuracion = configuracion;
    }

    @Override
    public Preguntar.Resultado preguntar(Solicitante solicitante, UUID conversacionId, String pregunta) {
        String texto = validar(pregunta);
        Instant inicio = reloj.ahora();
        ConversacionIA conversacion = acceso.vigente(solicitante, conversacionId, inicio).conversacion();
        DatosPedido pedido = pedidos.leer(solicitante, conversacion.pedidoId());
        if (conversacion.preguntas() >= ConversacionIA.LIMITE_PREGUNTAS) {
            throw new LimitePreguntas();
        }
        LocalDate dia = dia(inicio);
        ControlesAsistente.Vigentes vigentes = controles.exigir(dia);
        presupuesto.recuperarAbandonadas(inicio);
        // Antes de count_tokens: si la reserva ya no podría concederse, no se llama a nada del proveedor.
        try {
            presupuesto.comprobarDisponibilidad(solicitante.usuarioId(), dia);
        } catch (LimiteConsumo limite) {
            registrarLimite(conversacion, solicitante);
            throw limite;
        }

        Map<UUID, Boolean> descargables = new HashMap<>();
        Predicate<UUID> puedeDescargar = archivoId -> descargables.computeIfAbsent(archivoId,
                id -> documentos.puedeDescargar(solicitante, id));
        List<AsistenteIA.Turno> historial = historialAutorizado(conversaciones.mensajes(conversacion.id()),
                puedeDescargar);
        int maxTokens = Math.min(configuracion.maxTokensRespuesta(), vigentes.limites().tokensSalidaMax());
        Seleccion seleccion = ajustar(historial, texto, maxTokens, vigentes.limites().tokensEntradaMax(),
                elegibles(solicitante, pedido));

        long maximo = vigentes.tarifa().costoMaximo(seleccion.tokensEntrada(), maxTokens);
        PresupuestoIA.Reserva reserva;
        try {
            reserva = presupuesto.reservar(solicitante.usuarioId(), maximo, dia, reloj.ahora());
        } catch (LimiteConsumo limite) {
            registrarLimite(conversacion, solicitante);
            throw limite;
        }

        AsistenteIA.Respuesta respuesta;
        try {
            respuesta = asistente.responder(seleccion.consulta());
        } catch (IaNoDisponible desactivado) {
            presupuesto.liberar(reserva, reloj.ahora());
            throw desactivado;
        } catch (FalloProveedorIA fallo) {
            try {
                cerrarTrasFallo(reserva, fallo);
            } finally {
                // Lo dado por consumido se registra con el costo reservado, para que el consumo cuadre con el presupuesto.
                registrar(conversacion, solicitante, null, AsistenteIA.Consumo.NINGUNO,
                        fallo.posibleCobro() ? maximo : 0,
                        fallo.posibleCobro() ? RegistroConsultasIA.Resultado.ERROR_INCIERTO
                                : RegistroConsultasIA.Resultado.ERROR_SIN_COBRO,
                        seleccion.enviados());
            }
            throw new IaNoDisponible();
        } catch (RuntimeException inesperado) {
            try {
                presupuesto.darPorConsumida(reserva, reloj.ahora());
            } finally {
                registrar(conversacion, solicitante, null, AsistenteIA.Consumo.NINGUNO, maximo,
                        RegistroConsultasIA.Resultado.ERROR_INCIERTO, seleccion.enviados());
            }
            throw new IaNoDisponible();
        }

        // Ya se gastó: pase lo que pase después, la reserva se cierra y el consumo se registra.
        AsistenteIA.Consumo consumo = respuesta.consumo();
        long costo = maximo;
        try {
            costo = vigentes.tarifa().costoReal(consumo.entrada(), consumo.salida(), consumo.escrituraCache(),
                    consumo.lecturaCache());
            presupuesto.liquidar(reserva, costo, reloj.ahora());
        } catch (RuntimeException liquidacion) {
            presupuesto.darPorConsumida(reserva, reloj.ahora());
        }
        // Ya pagada: si la verificación falla, la respuesta se entrega como "sin evidencia", nunca como un error 500.
        List<Referencia> referencias;
        try {
            referencias = respuesta.fin() == AsistenteIA.Fin.RECHAZADA ? List.of()
                    : VerificadorReferencias.verificar(pedido.numero(), seleccion.fuentes(), respuesta.citas());
        } catch (RuntimeException verificacion) {
            referencias = List.of();
        }
        registrar(conversacion, solicitante, respuesta.modelo(), consumo, costo,
                resultado(respuesta.fin(), !referencias.isEmpty()), seleccion.enviados());
        boolean evidencia = !referencias.isEmpty();
        String textoFinal = textoFinal(respuesta, evidencia);

        int previas = conversacion.preguntas();
        Instant ahora = reloj.ahora();
        MensajeIA mensajePregunta = MensajeIA.pregunta(2 * previas + 1, texto, ahora);
        MensajeIA mensajeRespuesta = MensajeIA.respuesta(2 * previas + 2, textoFinal, referencias,
                seleccion.enviados(), seleccion.excluidos(), evidencia, ahora);
        if (!conversaciones.agregarTurno(conversacion.id(), previas, mensajePregunta, mensajeRespuesta, ahora)) {
            // Otro turno de la misma conversación terminó antes o la conversación venció. La reserva de 1 consulta en
            // curso por usuario lo hace improbable para una conversación propia; el consumo ya quedó registrado.
            throw new IaNoDisponible();
        }
        return new Preguntar.Resultado(textoFinal, evidencia, referencias, seleccion.excluidos(),
                ConversacionIA.LIMITE_PREGUNTAS - (previas + 1));
    }

    private static LocalDate dia(Instant instante) {
        return LocalDate.ofInstant(instante, Reloj.ZONA_ECUADOR);
    }

    /** El apagado por límite de gasto va primero: aunque el cierre de la reserva fallara, la IA queda desactivada. */
    private void cerrarTrasFallo(PresupuestoIA.Reserva reserva, FalloProveedorIA fallo) {
        try {
            if (fallo.causa() == FalloProveedorIA.Causa.LIMITE_GASTO) {
                presupuesto.desactivar(MOTIVO_LIMITE_GASTO, reloj.ahora());
            }
        } finally {
            if (fallo.posibleCobro()) {
                presupuesto.darPorConsumida(reserva, reloj.ahora());
            } else {
                presupuesto.liberar(reserva, reloj.ahora());
            }
        }
    }

    /** Un fallo al registrar el intento no convierte el 429 en un 500. */
    private void registrarLimite(ConversacionIA conversacion, Solicitante solicitante) {
        try {
            registrar(conversacion, solicitante, null, AsistenteIA.Consumo.NINGUNO, 0,
                    RegistroConsultasIA.Resultado.LIMITE, Set.of());
        } catch (RuntimeException registro) {
            // El límite ya se aplicó; el registro de un intento rechazado no cambia el presupuesto.
        }
    }

    private static String validar(String pregunta) {
        String texto = pregunta == null ? "" : pregunta.strip();
        if (texto.isEmpty() || texto.length() > MAX_PREGUNTA) {
            throw new DatoInvalido("Escribe una pregunta de 1 a " + MAX_PREGUNTA + " caracteres.");
        }
        return texto;
    }

    /**
     * Precisión 3 de Richard: solo se reenvían los pares pregunta-respuesta cuyas fuentes siguen autorizadas. Un par
     * incompleto o fuera de orden no se reenvía.
     */
    static List<AsistenteIA.Turno> historialAutorizado(List<MensajeIA> mensajes, Predicate<UUID> puedeDescargar) {
        List<AsistenteIA.Turno> turnos = new ArrayList<>();
        for (int i = 0; i + 1 < mensajes.size(); i += 2) {
            MensajeIA pregunta = mensajes.get(i);
            MensajeIA respuesta = mensajes.get(i + 1);
            if (pregunta.autor() != Autor.USUARIO || respuesta.autor() != Autor.ASISTENTE
                    || pregunta.retenidoPara(puedeDescargar) || respuesta.retenidoPara(puedeDescargar)) {
                continue;
            }
            turnos.add(new AsistenteIA.Turno(Autor.USUARIO, pregunta.texto()));
            turnos.add(new AsistenteIA.Turno(Autor.ASISTENTE, respuesta.texto()));
        }
        return turnos;
    }

    /** Un PDF que pasó P-17, el tipo, el manifiesto y la huella, listo para medir. */
    private record Pdf(DocumentoPedido documento, byte[] contenido, int paginas) {
    }

    private record Elegibles(DatosPedido pedido, List<Pdf> pdfs, List<DocumentoExcluido> excluidos) {
    }

    /**
     * Por etapa, solo los archivos que la persona puede descargar hoy (P-17); de los demás no se dice nada. Fuera del
     * piloto ficticio no se envía ningún documento (F7-D6 pendiente).
     */
    private Elegibles elegibles(Solicitante solicitante, DatosPedido pedido) {
        List<Pdf> pdfs = new ArrayList<>();
        List<DocumentoExcluido> excluidos = new ArrayList<>();
        long bytes = 0;
        for (EtapaPedido etapa : pedido.etapas()) {
            if (!pedidos.puedeDescargarEtapa(solicitante, pedido.id(), etapa.numero())) {
                continue;
            }
            for (DocumentoPedido documento : documentos.deEtapa(solicitante, pedido.id(), etapa.numero())) {
                DocumentoExcluido.Motivo motivo = null;
                ManifiestoPiloto.Entrada entrada = null;
                if (!configuracion.pilotoFicticio()) {
                    motivo = DocumentoExcluido.Motivo.NO_AUTORIZADO_PILOTO;
                } else if (!TIPO_PDF.equals(documento.tipoDetectado())) {
                    motivo = DocumentoExcluido.Motivo.FORMATO_NO_ADMITIDO;
                } else {
                    entrada = manifiesto.porSha256(documento.sha256()).orElse(null);
                    if (entrada == null) {
                        motivo = DocumentoExcluido.Motivo.NO_AUTORIZADO_PILOTO;
                    } else if (documento.tamanoBytes() > configuracion.limiteBytesDocumento()
                            || bytes + documento.tamanoBytes() > ConfiguracionAsistente.LIMITE_BYTES_MAXIMO) {
                        motivo = DocumentoExcluido.Motivo.CONTEXTO_EXCEDIDO;
                    }
                }
                if (motivo != null) {
                    excluidos.add(new DocumentoExcluido(documento.id(), documento.nombre(), motivo));
                    continue;
                }
                byte[] contenido;
                try {
                    contenido = documentos.leer(solicitante, documento.id(), configuracion.limiteBytesDocumento());
                } catch (SinPermiso | NoEncontrado | ArchivoNoDisponible noLegible) {
                    // Permiso perdido entre la lista y la lectura, o archivo dañado: no se envía ni se nombra.
                    continue;
                }
                // F7-D3: se compara la huella de lo que de verdad se enviaría con la del manifiesto revisado.
                if (!entrada.sha256().equals(sha256(contenido))) {
                    excluidos.add(new DocumentoExcluido(documento.id(), documento.nombre(),
                            DocumentoExcluido.Motivo.NO_AUTORIZADO_PILOTO));
                    continue;
                }
                bytes += contenido.length;
                pdfs.add(new Pdf(documento, contenido, entrada.paginas()));
            }
        }
        return new Elegibles(pedido, pdfs, excluidos);
    }

    private record Seleccion(AsistenteIA.Consulta consulta, long tokensEntrada, List<FuenteEnviada> fuentes,
            Set<UUID> enviados, List<DocumentoExcluido> excluidos) {
    }

    /**
     * Mide con count_tokens todo el contexto (F7-D2: 60 000 tokens de entrada). Primero la base (instrucciones,
     * historial, datos del pedido y pregunta); si no cabe, 400 CONTEXTO_EXCEDIDO. Después cada PDF por separado, en el
     * orden de las etapas, y se incluyen los que caben. Al final se cuenta la consulta exacta que se enviará y, si el
     * conteo conjunto supera el límite, se quitan PDF desde el último. count_tokens es gratuito pero es una estimación
     * (documentación oficial): la liquidación usa el {@code usage} real.
     */
    private Seleccion ajustar(List<AsistenteIA.Turno> historial, String pregunta, int maxTokens, int limite,
            Elegibles elegibles) {
        ContextoPedido contexto = ContextoPedido.de(elegibles.pedido());
        AsistenteIA.Documento datos = AsistenteIA.Documento.texto("Datos del pedido " + elegibles.pedido().numero(),
                contexto.texto());
        long base = contar(consulta(historial, datos, List.of(), pregunta, maxTokens));
        if (base > limite) {
            throw new ContextoExcedido();
        }
        List<DocumentoExcluido> excluidos = new ArrayList<>(elegibles.excluidos());
        List<Pdf> incluidos = new ArrayList<>();
        long estimado = base;
        for (Pdf pdf : elegibles.pdfs()) {
            long conPdf = contar(consulta(historial, datos, List.of(pdf), pregunta, maxTokens));
            long aporte = Math.max(0, conPdf - base);
            if (estimado + aporte <= limite) {
                incluidos.add(pdf);
                estimado += aporte;
            } else {
                excluidos.add(excluidoPorContexto(pdf));
            }
        }
        AsistenteIA.Consulta consulta = consulta(historial, datos, incluidos, pregunta, maxTokens);
        long tokens = incluidos.isEmpty() ? base : contar(consulta);
        while (tokens > limite && !incluidos.isEmpty()) {
            excluidos.add(excluidoPorContexto(incluidos.remove(incluidos.size() - 1)));
            consulta = consulta(historial, datos, incluidos, pregunta, maxTokens);
            tokens = incluidos.isEmpty() ? base : contar(consulta);
        }
        List<FuenteEnviada> fuentes = new ArrayList<>();
        fuentes.add(FuenteEnviada.datosPedido(contexto.texto(), contexto.tramos()));
        Set<UUID> enviados = new LinkedHashSet<>();
        for (Pdf pdf : incluidos) {
            fuentes.add(FuenteEnviada.pdf(pdf.documento().id(), pdf.documento().nombre(), pdf.documento().etapa(),
                    pdf.paginas()));
            enviados.add(pdf.documento().id());
        }
        return new Seleccion(consulta, tokens, fuentes, enviados, excluidos);
    }

    private static DocumentoExcluido excluidoPorContexto(Pdf pdf) {
        return new DocumentoExcluido(pdf.documento().id(), pdf.documento().nombre(),
                DocumentoExcluido.Motivo.CONTEXTO_EXCEDIDO);
    }

    /** Los datos del pedido van siempre primero (índice 0 de las citas) y después los PDF en el mismo orden. */
    private static AsistenteIA.Consulta consulta(List<AsistenteIA.Turno> historial, AsistenteIA.Documento datos,
            List<Pdf> pdfs, String pregunta, int maxTokens) {
        List<AsistenteIA.Documento> documentos = new ArrayList<>();
        documentos.add(datos);
        pdfs.forEach(pdf -> documentos.add(AsistenteIA.Documento.pdf(pdf.documento().nombre(), pdf.contenido())));
        return new AsistenteIA.Consulta(InstruccionesAsistente.SISTEMA, historial, documentos, pregunta, maxTokens);
    }

    /** count_tokens es previo a la reserva: un fallo no cuesta nada y se responde 503. */
    private long contar(AsistenteIA.Consulta consulta) {
        try {
            return asistente.contarTokens(consulta);
        } catch (FalloProveedorIA fallo) {
            if (fallo.causa() == FalloProveedorIA.Causa.LIMITE_GASTO) {
                presupuesto.desactivar(MOTIVO_LIMITE_GASTO, reloj.ahora());
            }
            throw new IaNoDisponible();
        }
    }

    private static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("SHA-256 no disponible", imposible);
        }
    }

    private void registrar(ConversacionIA conversacion, Solicitante solicitante, String modelo,
            AsistenteIA.Consumo consumo, long costo, RegistroConsultasIA.Resultado resultado, Set<UUID> enviados) {
        registro.registrar(new ConsultaRegistrada(UUID.randomUUID(), conversacion.id(), solicitante.usuarioId(),
                conversacion.pedidoId(), conversacion.pais(), modelo, consumo, costo, resultado, enviados,
                reloj.ahora()));
    }

    private static RegistroConsultasIA.Resultado resultado(AsistenteIA.Fin fin, boolean evidencia) {
        return switch (fin) {
            case RECHAZADA -> RegistroConsultasIA.Resultado.RECHAZADA;
            case TRUNCADA, OTRA -> RegistroConsultasIA.Resultado.INCOMPLETA;
            case COMPLETA -> evidencia ? RegistroConsultasIA.Resultado.RESPONDIDA
                    : RegistroConsultasIA.Resultado.SIN_EVIDENCIA;
        };
    }

    /** Sin referencias verificadas, la respuesta lo dice primero: nunca se presenta como respaldada. */
    private static String textoFinal(AsistenteIA.Respuesta respuesta, boolean evidencia) {
        if (respuesta.fin() == AsistenteIA.Fin.RECHAZADA) {
            return RESPUESTA_RECHAZADA;
        }
        String texto = respuesta.texto().strip();
        if (!evidencia) {
            texto = texto.isEmpty() ? AVISO_SIN_EVIDENCIA : AVISO_SIN_EVIDENCIA + "\n\n" + texto;
        }
        if (respuesta.fin() != AsistenteIA.Fin.COMPLETA) {
            texto = texto + "\n\n" + AVISO_INCOMPLETA;
        }
        return texto;
    }
}
