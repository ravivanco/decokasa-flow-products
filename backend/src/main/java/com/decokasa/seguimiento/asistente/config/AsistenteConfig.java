package com.decokasa.seguimiento.asistente.config;

import com.decokasa.seguimiento.archivos.application.port.in.DescargarArchivo;
import com.decokasa.seguimiento.archivos.application.port.in.ListarAdjuntosDeEtapa;
import com.decokasa.seguimiento.asistente.adapter.out.claude.AsistenteIADesactivado;
import com.decokasa.seguimiento.asistente.adapter.out.claude.ClaudeAsistenteIA;
import com.decokasa.seguimiento.asistente.adapter.out.claude.CredencialClaude;
import com.decokasa.seguimiento.asistente.adapter.out.manifiesto.ManifiestoPilotoEnArchivo;
import com.decokasa.seguimiento.asistente.adapter.out.modulos.CuentasParaAsistenteAdaptador;
import com.decokasa.seguimiento.asistente.adapter.out.modulos.DocumentosParaAsistenteAdaptador;
import com.decokasa.seguimiento.asistente.adapter.out.modulos.PedidosParaAsistenteAdaptador;
import com.decokasa.seguimiento.asistente.application.port.in.AbrirConversacion;
import com.decokasa.seguimiento.asistente.application.port.in.BorrarConversacionesVencidas;
import com.decokasa.seguimiento.asistente.application.port.in.ConsultarConsumoIA;
import com.decokasa.seguimiento.asistente.application.port.in.ConsultarConversaciones;
import com.decokasa.seguimiento.asistente.application.port.in.GestionarConfiguracionIA;
import com.decokasa.seguimiento.asistente.application.port.in.GestionarTarifasIA;
import com.decokasa.seguimiento.asistente.application.port.in.Preguntar;
import com.decokasa.seguimiento.asistente.application.port.out.ConsultaConsumoIA;
import com.decokasa.seguimiento.asistente.application.port.out.AsistenteIA;
import com.decokasa.seguimiento.asistente.application.port.out.ConversacionesIA;
import com.decokasa.seguimiento.asistente.application.port.out.CuentasParaAsistente;
import com.decokasa.seguimiento.asistente.application.port.out.DocumentosParaAsistente;
import com.decokasa.seguimiento.asistente.application.port.out.ManifiestoPiloto;
import com.decokasa.seguimiento.asistente.application.port.out.PedidosParaAsistente;
import com.decokasa.seguimiento.asistente.application.port.out.PresupuestoIA;
import com.decokasa.seguimiento.asistente.application.port.out.RegistroConsultasIA;
import com.decokasa.seguimiento.asistente.application.port.out.TarifasIA;
import com.decokasa.seguimiento.asistente.application.usecase.AbrirConversacionCasoDeUso;
import com.decokasa.seguimiento.asistente.application.usecase.BorrarConversacionesVencidasCasoDeUso;
import com.decokasa.seguimiento.asistente.application.usecase.ConfiguracionAsistente;
import com.decokasa.seguimiento.asistente.application.usecase.ConsultarConsumoIACasoDeUso;
import com.decokasa.seguimiento.asistente.application.usecase.ConsultarConversacionesCasoDeUso;
import com.decokasa.seguimiento.asistente.application.usecase.GestionarConfiguracionIACasoDeUso;
import com.decokasa.seguimiento.asistente.application.usecase.GestionarTarifasIACasoDeUso;
import com.decokasa.seguimiento.asistente.application.usecase.PreguntarCasoDeUso;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.pedidos.application.port.in.AutorizarArchivosDePedido;
import com.decokasa.seguimiento.pedidos.application.port.in.ConsultarPedidos;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Raíz de composición del módulo asistente (ADR-0020 a ADR-0023). Dirección de dependencias (R9): asistente → pedidos,
 * archivos y usuarios (puertos de entrada) y compartido; ningún módulo depende de asistente.
 *
 * <p><b>Paso 7.5:</b> {@code Preguntar} se registra porque existen todos los controles: reserva atómica
 * ({@code PresupuestoIASql}), conteo del contexto, tarifa validada y apagado automático. Cada consulta los exige antes de
 * llamar a nada del proveedor. Con IA_HABILITADA=false (valor por defecto), sin clave o sin modelo, el adaptador es
 * {@link AsistenteIADesactivado}, no se construye ningún cliente y preguntar responde 503. La clave solo llega al bean del
 * adaptador; nunca a la configuración de los casos de uso ni a un log.
 */
@Configuration(proxyBeanMethods = false)
public class AsistenteConfig {

    /** F7-D3: única marca que permite enviar documentos, y solo sin documentos reales autorizados (F7-D6). */
    static final String PILOTO_FICTICIO = "PILOTO_FICTICIO";

    @Bean
    ConfiguracionAsistente configuracionAsistente(
            @Value("${decokasa.ia.habilitada:false}") boolean habilitada,
            @Value("${decokasa.ia.claude-api-key:}") String clave,
            @Value("${decokasa.ia.modelo:}") String modelo,
            @Value("${decokasa.ia.entorno:}") String entorno,
            @Value("${decokasa.ia.documentos-reales-autorizados:false}") boolean documentosReales,
            @Value("${decokasa.ia.conservacion-dias:30}") int dias,
            @Value("${decokasa.ia.max-tokens-respuesta:4000}") int maxTokens,
            @Value("${decokasa.ia.limite-bytes-documento:10485760}") long limiteBytes,
            @Value("${decokasa.ia.espera-segundos:60}") long espera) {
        boolean activa = habilitada && !clave.isBlank() && !modelo.isBlank();
        boolean piloto = PILOTO_FICTICIO.equals(entorno.strip()) && !documentosReales;
        return new ConfiguracionAsistente(activa, piloto, Duration.ofDays(dias), maxTokens, limiteBytes, modelo,
                Duration.ofSeconds(espera));
    }

    @Bean
    AsistenteIA asistenteIA(ConfiguracionAsistente configuracion,
            @Value("${decokasa.ia.claude-api-key:}") String clave,
            @Value("${decokasa.ia.modelo:}") String modelo,
            @Value("${decokasa.ia.url-base:https://api.anthropic.com}") String urlBase,
            @Value("${decokasa.ia.espera-segundos:60}") long espera) {
        if (!configuracion.habilitada()) {
            return new AsistenteIADesactivado();
        }
        return new ClaudeAsistenteIA(new CredencialClaude(clave), modelo, urlBase, Duration.ofSeconds(espera));
    }

    @Bean
    ManifiestoPiloto manifiestoPiloto(
            @Value("${decokasa.ia.piloto.manifiesto:piloto-ia/manifiesto.json}") String ruta, JsonMapper json) {
        return new ManifiestoPilotoEnArchivo(Path.of(ruta), json);
    }

    @Bean
    CuentasParaAsistente cuentasParaAsistente(ConsultarResponsables responsables) {
        return new CuentasParaAsistenteAdaptador(responsables);
    }

    @Bean
    PedidosParaAsistente pedidosParaAsistente(ConsultarPedidos consultar, AutorizarArchivosDePedido autorizar) {
        return new PedidosParaAsistenteAdaptador(consultar, autorizar);
    }

    @Bean
    DocumentosParaAsistente documentosParaAsistente(ListarAdjuntosDeEtapa listar, DescargarArchivo descargar) {
        return new DocumentosParaAsistenteAdaptador(listar, descargar);
    }

    @Bean
    AbrirConversacion abrirConversacion(CuentasParaAsistente cuentas, PedidosParaAsistente pedidos,
            ConversacionesIA conversaciones, Reloj reloj, ConfiguracionAsistente configuracion) {
        return new AbrirConversacionCasoDeUso(cuentas, pedidos, conversaciones, reloj, configuracion);
    }

    @Bean
    ConsultarConversaciones consultarConversaciones(CuentasParaAsistente cuentas, PedidosParaAsistente pedidos,
            DocumentosParaAsistente documentos, ConversacionesIA conversaciones, Reloj reloj) {
        return new ConsultarConversacionesCasoDeUso(cuentas, pedidos, documentos, conversaciones, reloj);
    }

    @Bean
    Preguntar preguntar(CuentasParaAsistente cuentas, PedidosParaAsistente pedidos, DocumentosParaAsistente documentos,
            ConversacionesIA conversaciones, RegistroConsultasIA registro, ManifiestoPiloto manifiesto,
            AsistenteIA asistente, PresupuestoIA presupuesto, Reloj reloj, ConfiguracionAsistente configuracion) {
        return new PreguntarCasoDeUso(cuentas, pedidos, documentos, conversaciones, registro, manifiesto, asistente,
                presupuesto, reloj, configuracion);
    }

    @Bean
    ConsultarConsumoIA consultarConsumoIA(CuentasParaAsistente cuentas, ConversacionesIA conversaciones,
            ConsultaConsumoIA consumo, PresupuestoIA presupuesto, Reloj reloj) {
        return new ConsultarConsumoIACasoDeUso(cuentas, conversaciones, consumo, presupuesto, reloj);
    }

    @Bean
    GestionarTarifasIA gestionarTarifasIA(CuentasParaAsistente cuentas, ConversacionesIA conversaciones,
            TarifasIA tarifas, Reloj reloj) {
        return new GestionarTarifasIACasoDeUso(cuentas, conversaciones, tarifas, reloj);
    }

    @Bean
    GestionarConfiguracionIA gestionarConfiguracionIA(CuentasParaAsistente cuentas, ConversacionesIA conversaciones,
            PresupuestoIA presupuesto, TarifasIA tarifas, ConfiguracionAsistente configuracion, Reloj reloj) {
        return new GestionarConfiguracionIACasoDeUso(cuentas, conversaciones, presupuesto, tarifas, configuracion,
                reloj);
    }

    @Bean
    BorrarConversacionesVencidas borrarConversacionesVencidas(ConversacionesIA conversaciones, Reloj reloj) {
        return new BorrarConversacionesVencidasCasoDeUso(conversaciones, reloj);
    }
}
