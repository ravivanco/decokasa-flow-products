package com.decokasa.ensayob3;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Solo guardas puras y archivos temporales; no Spring, Docker, OAuth, Drive ni Gmail. */
class GuardasB3Test {
    @TempDir Path temporal;
    @Test void soloAceptaCuentaExactaAprobada() {
        assertThatCode(() -> GuardasB3.receptor(GuardasB3.CUENTA)).doesNotThrowAnyException();
        assertThatThrownBy(() -> GuardasB3.receptor("otro@ejemplo.test")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> GuardasB3.receptor(null)).isInstanceOf(IllegalStateException.class);
    }
    @Test void confirmacionNoSeDeduceDeUnSiGenerico() {
        assertThat(GuardasB3.confirmar("si", "AUTORIZAR GOOGLE SOLO 3.B3")).isFalse();
        assertThat(GuardasB3.confirmar(null, "AUTORIZAR GOOGLE SOLO 3.B3")).isFalse();
        assertThat(GuardasB3.confirmar("AUTORIZAR GOOGLE SOLO 3.B3", "AUTORIZAR GOOGLE SOLO 3.B3")).isTrue();
    }
    @Test void exigeUnArchivoUnPedidoY100MiBExactos() {
        GuardasB3.contenido(1, 1, 104857600L, "ficticio-3b3-100MiB.bin");
        assertThatThrownBy(() -> GuardasB3.contenido(1, 1, 100000000L, "ficticio-3b3-100MiB.bin")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> GuardasB3.contenido(1, 2, GuardasB3.BYTES, "ficticio-3b3-100MiB.bin")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> GuardasB3.contenido(2, 1, GuardasB3.BYTES, "ficticio-3b3-100MiB.bin")).isInstanceOf(IllegalStateException.class);
    }
    @Test void noAdmiteFalloIncertidumbreONuevoIntentoDeCorreo() {
        GuardasB3.correo(1, GuardasB3.CUENTA, "PENDIENTE", 0, false);
        assertThatThrownBy(() -> GuardasB3.correo(1, GuardasB3.CUENTA, "FALLIDA", 1, true)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> GuardasB3.correo(2, GuardasB3.CUENTA, "PENDIENTE", 0, false)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> GuardasB3.correo(1, GuardasB3.CUENTA, "PENDIENTE", 1, false)).isInstanceOf(IllegalStateException.class);
    }
    @Test void rutaDelRepoYRelativaQuedanBloqueadas() {
        assertThatThrownBy(() -> GuardasB3.externo(Path.of("relativa"), temporal)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> GuardasB3.externo(temporal, temporal)).isInstanceOf(IllegalStateException.class);
    }
    @Test void marcaConsumidaNoPuedeSobrescribirse() throws Exception {
        Path marca = temporal.resolve("correo-invocado");
        // Archivo no sensible del fixture: se ejercita CREATE_NEW, sin exigir la ACL de un directorio real.
        Files.writeString(marca, "ya consumido", StandardOpenOption.CREATE_NEW);
        assertThatThrownBy(() -> GuardasB3.consumirCorreo(marca)).isInstanceOf(FileAlreadyExistsException.class);
        assertThat(Files.readString(marca)).isEqualTo("ya consumido");
    }
    @Test void ningunModoHabilitaSchedulersNiDosAdaptadoresALaVez() {
        for (boolean[] modo : new boolean[][]{{false,false,false},{true,false,false},{true,true,false},{true,false,true}}) {
            var valores = GuardasB3.modo(modo[0],modo[1],modo[2]);
            assertThat(valores).containsEntry("decokasa.google.drive.worker.automatico",false)
                    .containsEntry("decokasa.google.gmail.worker.automatico",false);
        }
        assertThatThrownBy(() -> GuardasB3.modo(true,true,true)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> GuardasB3.modo(false,true,false)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> GuardasB3.modo(false,false,true)).isInstanceOf(IllegalStateException.class);
    }
    @Test void mainSinConsolaOArgumentosNoIniciaElEntorno() {
        assertThatCode(() -> ArnesGoogleB3.main(new String[0])).doesNotThrowAnyException();
    }
}
