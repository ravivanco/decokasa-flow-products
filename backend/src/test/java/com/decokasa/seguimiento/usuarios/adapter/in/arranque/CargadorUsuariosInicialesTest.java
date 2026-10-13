package com.decokasa.seguimiento.usuarios.adapter.in.arranque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.usuarios.application.port.in.CrearUsuario;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

// Carga de usuarios iniciales fuera de Git (ADR-0015, plan tarea 10): solo con la base vacía, con las mismas reglas
// que el alta del admin y con una contraseña temporal que no está en ningún archivo. Datos ficticios.
class CargadorUsuariosInicialesTest {

    private static final String CLAVE_TEMPORAL = "Temporal-Prueba-1";

    @TempDir
    Path carpeta;

    private final List<CrearUsuario.Comando> creados = new ArrayList<>();
    private final CrearUsuario crearUsuario = (solicitante, comando) -> {
        assertThat(solicitante.roles()).contains(new CodigoRol("admin"));
        creados.add(comando);
        return null;
    };

    @Test
    void conLaBaseVaciaCreaCadaUsuarioDelArchivoConSusRoles() throws IOException {
        Path archivo = archivo("""
                [
                  {"nombre": "Admin Prueba", "correo": "admin@ejemplo.test", "usuario": "adminprueba",
                   "telefono": "+593990000010", "roles": ["admin"]},
                  {"nombre": "Ana Prueba", "correo": "ana@ejemplo.test", "usuario": "anaprueba",
                   "telefono": "+593 99 000 0001", "roles": ["compras_china", "logistica"]}
                ]""");

        cargador(archivo, false).cargar();

        assertThat(creados).hasSize(2);
        assertThat(creados.get(1).roles()).containsExactlyInAnyOrder(new CodigoRol("compras_china"),
                new CodigoRol("logistica"));
        assertThat(creados.get(1).paises()).containsExactly(CodigoPais.EC);
        assertThat(creados).allSatisfy(c -> assertThat(c.contrasenaInicial()).isEqualTo(CLAVE_TEMPORAL));
    }

    @Test
    void siYaHayUsuariosNoCargaNada() throws IOException {
        Path archivo = archivo("""
                [{"nombre": "Admin Prueba", "correo": "admin@ejemplo.test", "usuario": "adminprueba",
                  "telefono": "+593990000010", "roles": ["admin"]}]""");

        cargador(archivo, true).cargar();

        assertThat(creados).isEmpty();
    }

    @Test
    void sinContrasenaTemporalNoArranca() throws IOException {
        Path archivo = archivo("[]");

        assertThatThrownBy(() -> new CargadorUsuariosIniciales(crearUsuario, () -> false, JsonMapper.builder().build(),
                archivo, " ").cargar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SEMILLA_CONTRASENA_TEMPORAL");
    }

    @Test
    void unArchivoQueNoExisteDetieneElArranqueConUnMensajeClaro() {
        assertThatThrownBy(() -> cargador(carpeta.resolve("no-existe.json"), false).cargar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SEMILLA_ARCHIVO");
    }

    private CargadorUsuariosIniciales cargador(Path archivo, boolean hayUsuarios) {
        return new CargadorUsuariosIniciales(crearUsuario, () -> hayUsuarios, JsonMapper.builder().build(), archivo,
                CLAVE_TEMPORAL);
    }

    private Path archivo(String contenido) throws IOException {
        Path archivo = carpeta.resolve("usuarios.local.json");
        Files.writeString(archivo, contenido);
        return archivo;
    }
}
