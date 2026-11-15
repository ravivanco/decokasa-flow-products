package com.decokasa.seguimiento.embarques.config;

import com.decokasa.seguimiento.archivos.application.port.in.DescargarArchivo;
import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.embarques.application.port.in.EnviosDeEmbarque;
import com.decokasa.seguimiento.embarques.application.port.in.GestionarEmbarques;
import com.decokasa.seguimiento.embarques.application.port.out.EmbarquesRepositorio;
import com.decokasa.seguimiento.embarques.application.port.out.PedidosParaEmbarques;
import com.decokasa.seguimiento.embarques.application.usecase.EnviosDeEmbarqueCasoDeUso;
import com.decokasa.seguimiento.embarques.application.usecase.GestionarEmbarquesCasoDeUso;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Raíz de composición de embarques. {@link EnviosDeEmbarque} corre dentro de la transacción de la acción del pedido, por
 * eso no abre una propia. Dirección de dependencias (R9): pedidos → embarques → archivos.
 */
@Configuration(proxyBeanMethods = false)
public class EmbarquesConfig {

    @Bean
    EnviosDeEmbarque enviosDeEmbarque(EmbarquesRepositorio repositorio, PublicadorEventos publicador) {
        return new EnviosDeEmbarqueCasoDeUso(repositorio, publicador);
    }

    @Bean
    GestionarEmbarques gestionarEmbarques(PlatformTransactionManager gestor, EmbarquesRepositorio repositorio,
            PedidosParaEmbarques pedidos, PublicadorEventos publicador, DescargarArchivo descargas, Reloj reloj,
            com.decokasa.seguimiento.archivos.application.port.in.AccesoConfidencial confidencial) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        TransactionTemplate lectura = new TransactionTemplate(gestor);
        lectura.setReadOnly(true);
        GestionarEmbarques caso = new GestionarEmbarquesCasoDeUso(repositorio, pedidos, publicador, descargas, reloj);
        return new GestionarEmbarques() {
            @Override public Vista consultar(Solicitante s, UUID pedidoId) {
                return lectura.execute(e -> caso.consultar(s, pedidoId));
            }

            @Override public Vista crear(Solicitante s, UUID pedidoId, NuevoBl nuevo) {
                return tx.execute(e -> caso.crear(s, pedidoId, nuevo));
            }

            @Override public Vista actualizar(Solicitante s, UUID pedidoId, UUID blId, CambioBl cambio) {
                return tx.execute(e -> caso.actualizar(s, pedidoId, blId, cambio));
            }

            @Override public Vista registrarPackingList(Solicitante s, UUID pedidoId, UUID blId, UUID adjuntoId) {
                return tx.execute(e -> caso.registrarPackingList(s, pedidoId, blId, adjuntoId));
            }

            @Override public Vista vincularPorCodigo(Solicitante s, UUID pedidoId, String codigo) {
                return tx.execute(e -> caso.vincularPorCodigo(s, pedidoId, codigo));
            }

            @Override public Vista anadirPedido(Solicitante s, UUID pedidoId, UUID blId, String numero) {
                return tx.execute(e -> caso.anadirPedido(s, pedidoId, blId, numero));
            }

            @Override public Vista desvincular(Solicitante s, UUID pedidoId, UUID blId) {
                return tx.execute(e -> caso.desvincular(s, pedidoId, blId));
            }

            @Override public Vista marcarRevisada(Solicitante s, UUID pedidoId, UUID blId) {
                return tx.execute(e -> caso.marcarRevisada(s, pedidoId, blId));
            }

            @Override public DescargarArchivo.Autorizada packingList(Solicitante s, UUID pedidoId, UUID blId, UUID id) {
                return lectura.execute(e -> caso.packingList(s, pedidoId, blId, id));
            }

            @Override public Checklist checklist(Solicitante s, UUID pedidoId) {
                Checklist lista = lectura.execute(e -> caso.checklist(s, pedidoId));
                // V37: sin acceso a los confidenciales se ve qué ítem está cumplido, no el archivo ni quién lo subió.
                if (lista == null || !lista.aplica() || confidencial.puedeVer(pedidoId, s.usuarioId())) { return lista; }
                return new Checklist(lista.aplica(), lista.pendienteDefinicion(), lista.total(), lista.confirmados(),
                        lista.requisitos().stream().map(r -> new Requisito(r.tipoDocumento(), r.adjuntoId(), null, null,
                                null)).toList(), lista.entradaId(), lista.puedeSubir());
            }

            @Override public List<AlertaSimulada> alertasSimuladas(Solicitante s, UUID pedidoId) {
                return lectura.execute(e -> caso.alertasSimuladas(s, pedidoId));
            }
        };
    }
}
