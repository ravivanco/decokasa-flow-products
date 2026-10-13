package com.decokasa.seguimiento.usuarios.application;

import static com.decokasa.seguimiento.usuarios.application.Dobles.AHORA;
import static com.decokasa.seguimiento.usuarios.application.Dobles.CLAVE;
import static com.decokasa.seguimiento.usuarios.application.Dobles.roles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.decokasa.seguimiento.compartido.adapter.out.RelojFijo;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.usuarios.application.Dobles.CodificadorFalso;
import com.decokasa.seguimiento.usuarios.application.Dobles.EmisorTokensFalso;
import com.decokasa.seguimiento.usuarios.application.Dobles.GeneradorTokenFalso;
import com.decokasa.seguimiento.usuarios.application.Dobles.TokenRenovacionRepositorioEnMemoria;
import com.decokasa.seguimiento.usuarios.application.Dobles.UsuarioRepositorioEnMemoria;
import com.decokasa.seguimiento.usuarios.application.port.in.IniciarSesion;
import com.decokasa.seguimiento.usuarios.application.port.in.SesionIniciada;
import com.decokasa.seguimiento.usuarios.application.usecase.IniciarSesionCasoDeUso;
import com.decokasa.seguimiento.usuarios.domain.CredencialesInvalidas;
import com.decokasa.seguimiento.usuarios.domain.CuentaBloqueada;
import com.decokasa.seguimiento.usuarios.domain.PoliticaBloqueo;
import com.decokasa.seguimiento.usuarios.domain.PoliticaSesion;
import com.decokasa.seguimiento.usuarios.domain.Telefono;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Inicio de sesión (requisito 4.2 #1; ADR-0002: D2 bloqueo, D3 acceso de 15 min y renovación de 8 h; D4 roles).
class IniciarSesionTest {

    private static final UUID ID_ANA = UUID.randomUUID();

    private final UsuarioRepositorioEnMemoria usuarios = new UsuarioRepositorioEnMemoria();
    private final TokenRenovacionRepositorioEnMemoria tokens = new TokenRenovacionRepositorioEnMemoria();
    private final CodificadorFalso codificador = new CodificadorFalso();
    private final EmisorTokensFalso emisor = new EmisorTokensFalso();
    private final GeneradorTokenFalso generador = new GeneradorTokenFalso();
    private final RelojFijo reloj = new RelojFijo(AHORA);

    private IniciarSesion iniciarSesion;

    @BeforeEach
    void preparar() {
        iniciarSesion = new IniciarSesionCasoDeUso(usuarios, tokens, codificador, emisor, generador, reloj,
                PoliticaBloqueo.ACORDADA, PoliticaSesion.ACORDADA);
        guardarAna(roles("compras_china"), true, CLAVE);
    }

    // --- Identificadores ---

    @Test
    void entraConElCorreoSinDistinguirMayusculas() {
        assertThat(iniciarSesion.iniciar("ANA@Ejemplo.test", CLAVE).usuario().id()).isEqualTo(ID_ANA);
    }

    @Test
    void entraConElNombreDeUsuarioSinDistinguirMayusculas() {
        assertThat(iniciarSesion.iniciar("AnaPrueba", CLAVE).usuario().id()).isEqualTo(ID_ANA);
    }

    @Test
    void entraConElTelefonoEscritoConEspacios() {
        assertThat(iniciarSesion.iniciar("+593 99 000 0001", CLAVE).usuario().id()).isEqualTo(ID_ANA);
    }

    // --- Tokens (D3 y D4) ---

    @Test
    void emiteUnAccesoDeQuinceMinutos() {
        SesionIniciada sesion = iniciarSesion.iniciar("ana@ejemplo.test", CLAVE);

        assertThat(sesion.expiraEnSegundos()).isEqualTo(900);
        assertThat(sesion.tokenAcceso()).isEqualTo("acceso-1");
        assertThat(emisor.ultima().duracion()).isEqualTo(Duration.ofMinutes(15));
        assertThat(emisor.ultima().emitidoEn()).isEqualTo(AHORA);
    }

    @Test
    void elTokenDeAccesoLlevaTodosLosRolesDeUnUsuarioConVariosRoles() {
        guardarAna(roles("compras_china", "logistica"), true, CLAVE);

        SesionIniciada sesion = iniciarSesion.iniciar("ana@ejemplo.test", CLAVE);

        assertThat(emisor.ultima().usuarioId()).isEqualTo(ID_ANA);
        assertThat(emisor.ultima().roles()).isEqualTo(roles("compras_china", "logistica"));
        assertThat(sesion.usuario().roles()).isEqualTo(roles("compras_china", "logistica"));
    }

    @Test
    void creaUnaSesionRenovableDeOchoHorasGuardandoSoloElHash() {
        SesionIniciada sesion = iniciarSesion.iniciar("ana@ejemplo.test", CLAVE);

        assertThat(sesion.renovableHasta()).isEqualTo(AHORA.plus(Duration.ofHours(8)));
        assertThat(tokens.todas()).singleElement().satisfies(guardada -> {
            assertThat(guardada.usuarioId()).isEqualTo(ID_ANA);
            assertThat(guardada.tokenHash()).isEqualTo(generador.hash(sesion.tokenRenovacion()));
            assertThat(guardada.tokenHash()).isNotEqualTo(sesion.tokenRenovacion());
            assertThat(guardada.creadoEn()).isEqualTo(AHORA);
            assertThat(guardada.expiraEn()).isEqualTo(AHORA.plus(Duration.ofHours(8)));
        });
    }

    // --- Rechazos: siempre el mismo error ---

    @Test
    void unaContrasenaIncorrectaSumaUnIntentoFallido() {
        assertThatThrownBy(() -> iniciarSesion.iniciar("ana@ejemplo.test", "Clave-Mala-1"))
                .isInstanceOf(CredencialesInvalidas.class);

        assertThat(ana().intentosFallidos()).isEqualTo(1);
        assertThat(tokens.todas()).isEmpty();
        assertThat(emisor.emisiones).isEmpty();
    }

    @Test
    void unUsuarioInexistenteRecibeElMismoErrorQueUnaContrasenaIncorrecta() {
        Throwable inexistente = catchThrowable(() -> iniciarSesion.iniciar("nadie@ejemplo.test", CLAVE));
        Throwable claveMala = catchThrowable(() -> iniciarSesion.iniciar("ana@ejemplo.test", "Clave-Mala-1"));

        assertThat(inexistente).isInstanceOf(CredencialesInvalidas.class);
        assertThat(claveMala).isInstanceOf(CredencialesInvalidas.class);
        assertThat(inexistente).hasMessage(claveMala.getMessage());
    }

    @Test
    void unUsuarioDesactivadoNoEntraNiConLaContrasenaCorrecta() {
        usuarios.guardar(new Usuario(UUID.randomUUID(), "Beto Prueba", "beto@ejemplo.test", "betoprueba",
                new Telefono("+593990000002"), CodificadorFalso.PREFIJO + CLAVE, roles("logistica"),
                Set.of(CodigoPais.EC), false, 0, null, 0));

        assertThatThrownBy(() -> iniciarSesion.iniciar("beto@ejemplo.test", CLAVE))
                .isInstanceOf(CredencialesInvalidas.class);
        assertThat(tokens.todas()).isEmpty();
        assertThat(emisor.emisiones).isEmpty();
    }

    @Test
    void unUsuarioSinRolesNoEntraNiConLaContrasenaCorrecta() {
        guardarAna(roles(), true, CLAVE);

        assertThatThrownBy(() -> iniciarSesion.iniciar("ana@ejemplo.test", CLAVE))
                .isInstanceOf(CredencialesInvalidas.class);
        assertThat(tokens.todas()).isEmpty();
        assertThat(emisor.emisiones).isEmpty();
    }

    // --- Bloqueo (D2) ---

    @Test
    void trasCincoFallosLaCuentaQuedaBloqueadaAunqueLaContrasenaSeaCorrecta() {
        fallar(5);

        assertThatThrownBy(() -> iniciarSesion.iniciar("ana@ejemplo.test", CLAVE))
                .isInstanceOf(CuentaBloqueada.class)
                .satisfies(error -> assertThat(((CuentaBloqueada) error).hasta())
                        .isEqualTo(AHORA.plus(Duration.ofMinutes(15))));
        assertThat(tokens.todas()).isEmpty();
    }

    @Test
    void cuatroFallosTodaviaPermitenEntrarYElAciertoReiniciaElContador() {
        fallar(4);

        assertThat(iniciarSesion.iniciar("ana@ejemplo.test", CLAVE).usuario().id()).isEqualTo(ID_ANA);
        assertThat(ana().intentosFallidos()).isZero();
    }

    @Test
    void pasadosQuinceMinutosVuelveAEntrar() {
        fallar(5);

        reloj.adelantar(Duration.ofMinutes(15));

        assertThat(iniciarSesion.iniciar("ana@ejemplo.test", CLAVE).usuario().id()).isEqualTo(ID_ANA);
    }

    @Test
    void unAciertoAntesDelLimiteEvitaElBloqueo() {
        fallar(4);
        iniciarSesion.iniciar("ana@ejemplo.test", CLAVE);
        fallar(4);

        assertThat(iniciarSesion.iniciar("ana@ejemplo.test", CLAVE).usuario().id()).isEqualTo(ID_ANA);
    }

    // --- Intentos simultáneos (hallazgo A4): se cuenta sobre lo confirmado y se respeta el bloqueo ---

    @Test
    void unFalloSeSumaALoQueGuardoOtraSolicitudSimultanea() {
        fallar(1);
        usuarios.otraSolicitudConfirmaAntesDeBloquear(() -> ana().registrarFallo(AHORA, PoliticaBloqueo.ACORDADA));

        assertThatThrownBy(() -> iniciarSesion.iniciar("ana@ejemplo.test", "Clave-Mala-1"))
                .isInstanceOf(CredencialesInvalidas.class);

        assertThat(ana().intentosFallidos()).isEqualTo(3);
    }

    @Test
    void siOtraSolicitudBloqueoLaCuentaEntretantoUnFalloRespondeBloqueadaSinContarDeMas() {
        fallar(4);
        usuarios.otraSolicitudConfirmaAntesDeBloquear(() -> ana().registrarFallo(AHORA, PoliticaBloqueo.ACORDADA));

        assertThatThrownBy(() -> iniciarSesion.iniciar("ana@ejemplo.test", "Clave-Mala-1"))
                .isInstanceOf(CuentaBloqueada.class);

        assertThat(ana().intentosFallidos()).isEqualTo(5);
        assertThat(ana().bloqueadoHasta()).isEqualTo(AHORA.plus(Duration.ofMinutes(15)));
    }

    @Test
    void siOtraSolicitudBloqueoLaCuentaEntretantoNiLaContrasenaCorrectaEntra() {
        fallar(4);
        usuarios.otraSolicitudConfirmaAntesDeBloquear(() -> ana().registrarFallo(AHORA, PoliticaBloqueo.ACORDADA));

        assertThatThrownBy(() -> iniciarSesion.iniciar("ana@ejemplo.test", CLAVE)).isInstanceOf(CuentaBloqueada.class);

        assertThat(ana().intentosFallidos()).isEqualTo(5);
        assertThat(tokens.todas()).isEmpty();
        assertThat(emisor.emisiones).isEmpty();
    }

    // --- D10: el contador de intentos no forma parte de la versión administrativa ---

    @Test
    void niLosFallosNiElAciertoCambianLaVersionDelUsuario() {
        fallar(2);
        iniciarSesion.iniciar("ana@ejemplo.test", CLAVE);
        fallar(1);

        assertThat(ana().version()).isZero();
        assertThat(ana().intentosFallidos()).isEqualTo(1);
    }

    // --- Revalidación sobre el estado releído (tras A4): lo que cambie entre la lectura y el bloqueo cuenta ---

    @Test
    void unaCuentaDesactivadaEntreLaLecturaYLaRelecturaNoEntra() {
        otraSolicitudGuardaAna(roles("compras_china"), false, CLAVE);

        assertThatThrownBy(() -> iniciarSesion.iniciar("ana@ejemplo.test", CLAVE))
                .isInstanceOf(CredencialesInvalidas.class);

        assertThat(tokens.todas()).isEmpty();
        assertThat(emisor.emisiones).isEmpty();
    }

    @Test
    void unaCuentaQuePierdeTodosSusRolesEntreLaLecturaYLaRelecturaNoEntra() {
        otraSolicitudGuardaAna(roles(), true, CLAVE);

        assertThatThrownBy(() -> iniciarSesion.iniciar("ana@ejemplo.test", CLAVE))
                .isInstanceOf(CredencialesInvalidas.class);

        assertThat(tokens.todas()).isEmpty();
        assertThat(emisor.emisiones).isEmpty();
    }

    @Test
    void elTokenLlevaLosRolesDelEstadoReleido() {
        otraSolicitudGuardaAna(roles("logistica", "marketing"), true, CLAVE);

        SesionIniciada sesion = iniciarSesion.iniciar("ana@ejemplo.test", CLAVE);

        assertThat(emisor.ultima().roles()).isEqualTo(roles("logistica", "marketing"));
        assertThat(sesion.usuario().roles()).isEqualTo(roles("logistica", "marketing"));
    }

    @Test
    void siElHashCambioEntretantoLaContrasenaAnteriorYaNoEntraYCuentaComoFallo() {
        otraSolicitudGuardaAna(roles("compras_china"), true, "Clave-Nueva-9");

        assertThatThrownBy(() -> iniciarSesion.iniciar("ana@ejemplo.test", CLAVE))
                .isInstanceOf(CredencialesInvalidas.class);

        assertThat(ana().intentosFallidos()).isEqualTo(1);
        assertThat(tokens.todas()).isEmpty();
        assertThat(emisor.emisiones).isEmpty();
    }

    @Test
    void siElHashCambioEntretantoSeVerificaContraElNuevo() {
        otraSolicitudGuardaAna(roles("compras_china"), true, "Clave-Nueva-9");

        SesionIniciada sesion = iniciarSesion.iniciar("ana@ejemplo.test", "Clave-Nueva-9");

        assertThat(sesion.usuario().id()).isEqualTo(ID_ANA);
        assertThat(ana().intentosFallidos()).isZero();
    }

    // --- 72 bytes en UTF-8, sin recorte silencioso ---

    @Test
    void entraConUnaContrasenaDeSetentaYDosBytes() {
        String clave72 = "ñ".repeat(36);
        guardarAna(roles("logistica"), true, clave72);

        assertThat(iniciarSesion.iniciar("ana@ejemplo.test", clave72).usuario().id()).isEqualTo(ID_ANA);
    }

    @Test
    void unaContrasenaDeMasDeSetentaYDosBytesNoEntraNiSeRecorta() {
        // Si se recortara a 72 bytes, "ñ"×36 + "x" coincidiría con la guardada y entraría.
        String clave72 = "ñ".repeat(36);
        guardarAna(roles("logistica"), true, clave72);

        assertThatThrownBy(() -> iniciarSesion.iniciar("ana@ejemplo.test", clave72 + "x"))
                .isInstanceOf(CredencialesInvalidas.class);
        assertThat(ana().intentosFallidos()).isEqualTo(1);
        assertThat(codificador.recibidas)
                .allSatisfy(recibida -> assertThat(recibida.getBytes(StandardCharsets.UTF_8).length)
                        .isLessThanOrEqualTo(72));
    }

    private void guardarAna(Set<CodigoRol> roles, boolean activo, String clave) {
        usuarios.guardar(new Usuario(ID_ANA, "Ana Prueba", "ana@ejemplo.test", "anaprueba",
                new Telefono("+593990000001"), CodificadorFalso.PREFIJO + clave, roles, Set.of(CodigoPais.EC),
                activo, 0, null, 0));
    }

    /** Otra solicitud guarda una Ana nueva (otra instancia, como una relectura de la base) antes del bloqueo de fila. */
    private void otraSolicitudGuardaAna(Set<CodigoRol> roles, boolean activo, String clave) {
        usuarios.otraSolicitudConfirmaAntesDeBloquear(() -> guardarAna(roles, activo, clave));
    }

    private Usuario ana() {
        return usuarios.buscarPorId(ID_ANA).orElseThrow();
    }

    private void fallar(int veces) {
        for (int i = 0; i < veces; i++) {
            assertThatThrownBy(() -> iniciarSesion.iniciar("ana@ejemplo.test", "Clave-Mala-1"))
                    .isInstanceOf(CredencialesInvalidas.class);
        }
    }
}
