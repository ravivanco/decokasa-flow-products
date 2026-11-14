package com.decokasa.seguimiento.embarques;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.RequisitoFaltante;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.embarques.application.port.in.GestionarEmbarques;
import com.decokasa.seguimiento.embarques.application.port.out.EmbarquesRepositorio;
import com.decokasa.seguimiento.embarques.application.usecase.EnviosDeEmbarqueCasoDeUso;
import com.decokasa.seguimiento.embarques.domain.CodigoBl;
import com.decokasa.seguimiento.embarques.domain.LlegadaEstimada;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Reglas de BL y envíos, sin base de datos: normalización, llegada, requisitos y quién puede aprobar. */
class EmbarquesReglasTest {

    private final EmbarquesRepositorio repositorio = mock(EmbarquesRepositorio.class);
    private final EnviosDeEmbarqueCasoDeUso envios = new EnviosDeEmbarqueCasoDeUso(repositorio, mock(PublicadorEventos.class));
    private final UUID pedido = UUID.randomUUID();
    private final UUID actor = UUID.randomUUID();

    private GestionarEmbarques.Bl bl(String codigo, LocalDate salida, boolean conArchivo) {
        var archivo = conArchivo ? new GestionarEmbarques.PackingList(UUID.randomUUID(), 1, UUID.randomUUID(), "pl.pdf",
                "David", Instant.EPOCH) : null;
        return new GestionarEmbarques.Bl(UUID.randomUUID(), codigo, salida, null, "SIN_DATOS", null, 0, false, "LISTO",
                archivo, List.of(), List.of(), List.of(), false, null, "DECOKASA");
    }

    @Test
    void elCodigoSeComparaEnMayusculasYSinEspaciosConservandoLosGuiones() {
        assertThat(CodigoBl.de(" bl - 001 ").normalizado()).isEqualTo("BL-001");
        assertThat(CodigoBl.de(" bl - 001 ").texto()).isEqualTo("bl - 001");
        assertThatThrownBy(() -> CodigoBl.de("x")).isInstanceOf(DatoInvalido.class);
    }

    @Test
    void laLlegadaSeCalculaSeConservaManualYQuedaPorDefinirSinDatos() {
        LocalDate salida = LocalDate.of(2026, 11, 18);
        assertThat(LlegadaEstimada.resolver(salida, null, null, 45).fecha()).isEqualTo(LocalDate.of(2027, 1, 2));
        LocalDate manual = LocalDate.of(2027, 2, 1);
        assertThat(LlegadaEstimada.resolver(salida, manual, null, 45).origen()).isEqualTo(LlegadaEstimada.Origen.MANUAL);
        assertThat(LlegadaEstimada.resolver(salida, manual, LlegadaEstimada.Origen.AUTOMATICA, 45).origen())
                .isEqualTo(LlegadaEstimada.Origen.AUTOMATICA);
        assertThat(LlegadaEstimada.resolver(null, null, null, 45).origen()).isEqualTo(LlegadaEstimada.Origen.SIN_DATOS);
        assertThat(LlegadaEstimada.resolver(salida, null, null, null).fecha()).isNull();
        assertThatThrownBy(() -> LlegadaEstimada.resolver(salida, salida.minusDays(1), null, 45))
                .isInstanceOf(DatoInvalido.class);
    }

    @Test
    void paraEnviarFaltanBlYDatosRequeridos() {
        when(repositorio.blsDelPedido(pedido, false)).thenReturn(List.of());
        assertThat(envios.faltantes(pedido)).containsExactly("vincula al menos un BL");

        when(repositorio.blsDelPedido(pedido, false)).thenReturn(List.of(bl("BL-001", null, false)));
        assertThat(envios.faltantes(pedido)).containsExactly("BL-001: Packing List confirmado", "BL-001: fecha de salida");

        when(repositorio.blsDelPedido(pedido, false)).thenReturn(List.of(bl("BL-001", LocalDate.now(), true)));
        assertThat(envios.faltantes(pedido)).isEmpty();
    }

    @Test
    void quienEnvioNoApruebaSuPropioEnvio() {
        when(repositorio.envioPendiente(pedido)).thenReturn(Optional.of(
                new EmbarquesRepositorio.EnvioPendiente(UUID.randomUUID(), 1, actor)));
        assertThatThrownBy(() -> envios.exigirAprobable(pedido, actor)).isInstanceOf(SinPermiso.class);
    }

    @Test
    void unaVersionNuevaSinRevisarImpideAprobar() {
        UUID envio = UUID.randomUUID();
        when(repositorio.envioPendiente(pedido)).thenReturn(Optional.of(
                new EmbarquesRepositorio.EnvioPendiente(envio, 1, UUID.randomUUID())));
        when(repositorio.revisionesPendientes(envio)).thenReturn(List.of(
                new EmbarquesRepositorio.RevisionPendiente(UUID.randomUUID(), "BL-003", UUID.randomUUID(), 2)));
        assertThatThrownBy(() -> envios.exigirAprobable(pedido, actor)).isInstanceOf(RequisitoFaltante.class);
        assertThat(envios.faltantesParaAprobar(pedido, actor)).singleElement().asString().contains("v2", "BL-003");
        verify(repositorio, never()).registrarDecision(eq(envio), any(), any(), any(), any(), any());
    }
}
