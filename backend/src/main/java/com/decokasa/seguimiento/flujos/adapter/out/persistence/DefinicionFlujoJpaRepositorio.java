package com.decokasa.seguimiento.flujos.adapter.out.persistence;

import com.decokasa.seguimiento.compartido.domain.AccionEtapa;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.compartido.domain.TipoPlazo;
import com.decokasa.seguimiento.flujos.application.port.out.DefinicionFlujoRepositorio;
import com.decokasa.seguimiento.flujos.domain.DefinicionEtapa;
import com.decokasa.seguimiento.flujos.domain.DefinicionFlujo;
import com.decokasa.seguimiento.flujos.domain.RequisitosDocumentales;
import com.decokasa.seguimiento.flujos.domain.TransicionPermitida;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador JPA del puerto {@link DefinicionFlujoRepositorio}. Traduce las filas de V2 al dominio sin completar nada:
 * un plazo PENDIENTE sigue pendiente y unos documentos PENDIENTES siguen pendientes (S-2).
 */
@Component
public class DefinicionFlujoJpaRepositorio implements DefinicionFlujoRepositorio {

    private final DefinicionFlujoDatos datos;

    public DefinicionFlujoJpaRepositorio(DefinicionFlujoDatos datos) {
        this.datos = datos;
    }

    @Override
    public Optional<DefinicionFlujo> buscarPorId(UUID flujoId) {
        return datos.findById(flujoId).map(this::aDominio);
    }

    @Override
    public Optional<DefinicionFlujo> buscarVigente(CodigoPais pais, TipoFlujo tipo) {
        return datos.findByPaisCodigoAndTipoFlujoAndVigenteTrue(pais.name(), tipo.name()).map(this::aDominio);
    }

    @Override
    public Optional<DefinicionFlujo> buscarVersion(CodigoPais pais, TipoFlujo tipo, int version) {
        return datos.findByPaisCodigoAndTipoFlujoAndVersion(pais.name(), tipo.name(), version).map(this::aDominio);
    }

    private DefinicionFlujo aDominio(DefinicionFlujoEntity flujo) {
        List<DefinicionEtapa> etapas = datos.etapasDe(flujo.getId()).stream()
                .map(DefinicionFlujoJpaRepositorio::etapa)
                .toList();
        List<TransicionPermitida> transiciones = datos.transicionesDe(flujo.getId()).stream()
                .map(DefinicionFlujoJpaRepositorio::transicion)
                .toList();
        return new DefinicionFlujo(flujo.getId(), CodigoPais.valueOf(flujo.getPaisCodigo()),
                TipoFlujo.valueOf(flujo.getTipoFlujo()), flujo.getVersion(), flujo.isVigente(), etapas, transiciones);
    }

    private static DefinicionEtapa etapa(DefinicionEtapaEntity fila) {
        PlazoDeEtapa plazo = switch (fila.getPlazoEstado()) {
            case "DEFINIDO" ->
                    new PlazoDeEtapa.Definido(fila.getPlazoCantidad(), TipoPlazo.valueOf(fila.getPlazoTipo()));
            case "SIN_PLAZO" -> new PlazoDeEtapa.SinPlazo();
            case "PENDIENTE" -> new PlazoDeEtapa.Pendiente();
            default -> throw new IllegalStateException("Estado de plazo desconocido: " + fila.getPlazoEstado());
        };
        return new DefinicionEtapa(fila.getNumero(), fila.getNombre(),
                fila.getRolCodigo() == null ? null : new CodigoRol(fila.getRolCodigo()), plazo, fila.isAutomatica(),
                fila.getRevisaEtapa() == null ? null : fila.getRevisaEtapa().intValue());
    }

    private static TransicionPermitida transicion(TransicionPermitidaEntity fila) {
        return new TransicionPermitida(fila.getEtapaOrigen(), AccionEtapa.valueOf(fila.getAccion()),
                fila.getEtapaDestino() == null ? null : fila.getEtapaDestino().intValue(),
                fila.isRequiereObservacion(), fila.isRequiereUrgencia(), fila.isRequiereBodega(),
                RequisitosDocumentales.valueOf(fila.getRequisitosDocumentales()), fila.getDocumentos());
    }
}
