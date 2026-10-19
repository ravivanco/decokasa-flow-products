package com.decokasa.seguimiento.notificaciones.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.notificaciones.application.port.in.EnviarNotificacionesPendientes;
import com.decokasa.seguimiento.notificaciones.application.port.out.DirectorioDestinatarios;
import com.decokasa.seguimiento.notificaciones.application.port.out.Notificador;
import com.decokasa.seguimiento.notificaciones.application.port.out.TrabajoCorreo;
import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

/** La reclamación deja incertidumbre durable ANTES de cualquier POST. No reenvía filas inciertas. */
public final class EnviarNotificacionesCasoDeUso implements EnviarNotificacionesPendientes {
    private final TrabajoCorreo trabajo;
    private final Notificador gmail;
    private final DirectorioDestinatarios directorio;
    private final Reloj reloj;
    private final boolean habilitada;
    private final int lote;
    private final Duration lease;
    public EnviarNotificacionesCasoDeUso(TrabajoCorreo trabajo, Notificador gmail,
            DirectorioDestinatarios directorio, Reloj reloj, boolean habilitada, int lote, Duration lease) {
        if (lote < 1 || lote > 25 || lease.compareTo(Duration.ofMinutes(1)) < 0
                || lease.compareTo(Duration.ofMinutes(30)) > 0) {
            throw new IllegalArgumentException("Configuración de procesamiento de correo inválida");
        }
        this.trabajo=trabajo; this.gmail=gmail; this.directorio=directorio; this.reloj=reloj;
        this.habilitada=habilitada; this.lote=lote; this.lease=lease;
    }
    @Override public int procesar() {
        if (!habilitada) { return 0; }
        // F3-D8 pendiente: sin receptores aprobados no reclamar ni consumir intentos.
        if (Arrays.stream(CodigoPais.values()).allMatch(p -> directorio.administradores(p).isEmpty())) { return 0; }
        int tratados=0;
        for (int i=0; i<lote; i++) {
            if (Thread.currentThread().isInterrupted()) { break; }
            UUID propietario=UUID.randomUUID();
            var ahora=reloj.ahora();
            var siguiente=trabajo.reclamar(ahora, ahora.plus(lease), propietario);
            if (siguiente.isEmpty()) { break; }
            Notificacion n=siguiente.orElseThrow();
            // Una fila por vez: no dejar caducar un lote mientras se hacen otras llamadas.
            Notificador.Correo correo;
            try {
                var pais=CodigoPais.valueOf(n.datos().get("pais"));
                if (!directorio.administradores(pais).contains(n.destinatario())) {
                    trabajo.fallo(n.id(),propietario,Causa.CONFIGURACION,reloj.ahora());
                    tratados++; continue;
                }
                correo=new Notificador.Correo(n.id(),n.destinatario(),n.asunto(),texto(n),n.messageId());
            } catch (IllegalArgumentException error) {
                trabajo.fallo(n.id(),propietario,Causa.CONFIGURACION,reloj.ahora());
                tratados++; continue;
            }
            try {
                if (!trabajo.vigente(n.id(),propietario,reloj.ahora())) { continue; }
                var entrega=gmail.enviar(correo); // FUERA de toda transacción de negocio/persistencia.
                // Si perdió lease o falla el commit posterior, conserva incertidumbre; jamás otro POST.
                if (!trabajo.enviada(n.id(),propietario,entrega.proveedorId(),reloj.ahora())) { break; }
            } catch (FalloIntegracionGoogle error) {
                var fin=reloj.ahora();
                if (error.rechazoConfirmado() && n.intentos()>=1 && n.intentos()<3
                        && (error.causa()==Causa.CUOTA || error.causa()==Causa.TRANSITORIA)) {
                    Duration base=n.intentos()==1 ? Duration.ofMinutes(1) : Duration.ofMinutes(5);
                    Duration espera=error.esperaMinima().compareTo(base)>0 ? error.esperaMinima() : base;
                    java.time.Instant vencimiento;
                    try { vencimiento=fin.plus(espera); }
                    catch (java.time.DateTimeException | ArithmeticException fueraDeRango) {
                        // An unrepresentable Retry-After must never cause an earlier retry.
                        trabajo.fallo(n.id(),propietario,error.causa(),fin);
                        tratados++; continue;
                    }
                    trabajo.reintentar(n.id(),propietario,error.causa(),vencimiento,fin);
                } else {
                    trabajo.fallo(n.id(),propietario,error.causa(),fin);
                }
            }
            // Otras excepciones (por ejemplo DB después del POST) salen y mantienen la marca incierta.
            tratados++;
        }
        return tratados;
    }
    private static String texto(Notificacion n) {
        return "Pedido: "+n.datos().getOrDefault("pedido",n.pedidoId().toString())
            +"\nPaís: "+n.datos().getOrDefault("pais","")
            +"\nEvento: "+n.datos().getOrDefault("tipoEvento","")
            +"\nEtapa de origen: "+n.datos().getOrDefault("etapaOrigen","")
            +"\nEtapa de destino: "+n.datos().getOrDefault("etapaDestino","")
            +"\nFecha: "+n.datos().getOrDefault("ocurridoEn","")
            +"\nConsulta el pedido en la aplicación.";
    }
}
