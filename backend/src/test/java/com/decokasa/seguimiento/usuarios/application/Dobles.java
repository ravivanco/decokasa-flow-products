package com.decokasa.seguimiento.usuarios.application;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.out.CodificadorContrasena;
import com.decokasa.seguimiento.usuarios.application.port.out.EmisorTokens;
import com.decokasa.seguimiento.usuarios.application.port.out.GeneradorTokenRenovacion;
import com.decokasa.seguimiento.usuarios.application.port.out.RolRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.TokenRenovacionRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.Rol;
import com.decokasa.seguimiento.usuarios.domain.SesionRenovable;
import com.decokasa.seguimiento.usuarios.domain.Telefono;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Dobles de prueba de los puertos de salida de {@code usuarios} (TEST_STRUCTURE del plan): repositorios en memoria y
 * adaptadores falsos que registran lo que reciben. Solo datos ficticios.
 */
final class Dobles {

    static final Instant AHORA = Instant.parse("2026-10-12T14:00:00Z");
    static final String CLAVE = "Clave-Prueba-1";

    private static final AtomicInteger SIGUIENTE = new AtomicInteger();

    private Dobles() {
    }

    static CodigoRol rol(String codigo) {
        return new CodigoRol(codigo);
    }

    static Set<CodigoRol> roles(String... codigos) {
        return Arrays.stream(codigos).map(CodigoRol::new).collect(Collectors.toUnmodifiableSet());
    }

    /** Admin activo guardado en el repositorio, con el solicitante que armaría el adaptador web con su token. */
    static Solicitante adminEn(UsuarioRepositorioEnMemoria usuarios) {
        return solicitanteEn(usuarios, "admin");
    }

    /**
     * Guarda un usuario ficticio activo con esos roles y devuelve su solicitante. La autorización lee el estado
     * actual del usuario en el repositorio, no solo los roles del token (hallazgo A1): el solicitante debe existir.
     */
    static Solicitante solicitanteEn(UsuarioRepositorioEnMemoria usuarios, String... codigos) {
        int n = SIGUIENTE.incrementAndGet();
        Usuario usuario = new Usuario(UUID.randomUUID(), "Solicitante Prueba", "solicitante" + n + "@ejemplo.test",
                "solicitante" + n, new Telefono("+5939920%05d".formatted(n)), CodificadorFalso.PREFIJO + CLAVE,
                roles(codigos), Set.of(CodigoPais.EC), true, 0, null, 0);
        usuarios.guardar(usuario);
        return new Solicitante(usuario.id(), usuario.roles());
    }

    /** Otro admin desactiva la cuenta del solicitante; su token (el {@link Solicitante}) sigue vigente. */
    static void desactivarEnLaBase(UsuarioRepositorioEnMemoria usuarios, Solicitante solicitante) {
        Usuario usuario = usuarios.buscarPorId(solicitante.usuarioId()).orElseThrow();
        usuario.desactivar();
        usuarios.guardar(usuario);
    }

    /** Otro admin le deja solo el rol marketing; su token sigue diciendo admin. */
    static void quitarAdminEnLaBase(UsuarioRepositorioEnMemoria usuarios, Solicitante solicitante) {
        Usuario usuario = usuarios.buscarPorId(solicitante.usuarioId()).orElseThrow();
        usuario.asignarRoles(roles("marketing"));
        usuarios.guardar(usuario);
    }

    /** Usuario ficticio guardado, con la contraseña {@link #CLAVE} codificada por {@link CodificadorFalso}. */
    static Usuario ana(Set<CodigoRol> roles, boolean activo) {
        return conClave(roles, activo, CLAVE);
    }

    static Usuario conClave(Set<CodigoRol> roles, boolean activo, String clave) {
        return new Usuario(UUID.randomUUID(), "Ana Prueba", "ana@ejemplo.test", "anaprueba",
                new Telefono("+593990000001"), CodificadorFalso.PREFIJO + clave, roles, Set.of(CodigoPais.EC),
                activo, 0, null, 0);
    }

    static final class UsuarioRepositorioEnMemoria implements UsuarioRepositorio {

        private final Map<UUID, Usuario> usuarios = new LinkedHashMap<>();
        private Runnable antesDeBloquear = () -> { };

        @Override
        public Optional<Usuario> buscarPorId(UUID id) {
            return Optional.ofNullable(usuarios.get(id));
        }

        @Override
        public Optional<Usuario> buscarParaActualizar(UUID id) {
            Runnable accion = antesDeBloquear;
            antesDeBloquear = () -> { };
            accion.run();
            return buscarPorId(id);
        }

        /**
         * Lo que otra solicitud confirma mientras esta comprueba la contraseña: se aplica justo antes de que esta
         * obtenga la fila bloqueada (carrera A4).
         */
        void otraSolicitudConfirmaAntesDeBloquear(Runnable accion) {
            antesDeBloquear = accion;
        }

        @Override
        public Optional<Usuario> buscarPorCorreo(String correo) {
            return usuarios.values().stream().filter(u -> u.correo().equalsIgnoreCase(correo)).findFirst();
        }

        @Override
        public Optional<Usuario> buscarPorNombreDeUsuario(String usuario) {
            return usuarios.values().stream().filter(u -> u.usuario().equalsIgnoreCase(usuario)).findFirst();
        }

        @Override
        public Optional<Usuario> buscarPorTelefono(Telefono telefono) {
            return usuarios.values().stream().filter(u -> u.telefono().equals(telefono)).findFirst();
        }

        @Override
        public List<Usuario> listar() {
            return List.copyOf(usuarios.values());
        }

        /**
         * Como JPA con {@code @Version}: un usuario nuevo queda con la versión con la que llega (0); al guardar uno
         * existente, se guarda otra instancia con la versión siguiente (D10).
         */
        @Override
        public void guardar(Usuario usuario) {
            if (!usuarios.containsKey(usuario.id())) {
                usuarios.put(usuario.id(), usuario);
                return;
            }
            usuarios.put(usuario.id(), new Usuario(usuario.id(), usuario.nombre(), usuario.correo(), usuario.usuario(),
                    usuario.telefono(), usuario.contrasenaHash(), usuario.roles(), usuario.paises(), usuario.activo(),
                    usuario.intentosFallidos(), usuario.bloqueadoHasta(), usuarios.get(usuario.id()).version() + 1));
        }

        /** Solo el contador y el bloqueo: la versión guardada no cambia (D10). */
        @Override
        public void guardarIntentos(Usuario usuario) {
            Usuario guardado = usuarios.get(usuario.id());
            usuarios.put(usuario.id(), new Usuario(guardado.id(), guardado.nombre(), guardado.correo(),
                    guardado.usuario(), guardado.telefono(), guardado.contrasenaHash(), guardado.roles(),
                    guardado.paises(), guardado.activo(), usuario.intentosFallidos(), usuario.bloqueadoHasta(),
                    guardado.version()));
        }

        int cantidad() {
            return usuarios.size();
        }
    }

    static final class RolRepositorioEnMemoria implements RolRepositorio {

        private final Map<CodigoRol, Rol> roles = new LinkedHashMap<>();

        /** Con los 7 roles que siembra la migración V1. */
        RolRepositorioEnMemoria() {
            for (String codigo : List.of("admin", "asistente_compras", "marketing", "compras_china", "logistica",
                    "asistente_china", "consulta")) {
                guardar(new Rol(UUID.randomUUID(), new CodigoRol(codigo), codigo, Set.of()));
            }
        }

        @Override
        public Optional<Rol> buscarPorId(UUID id) {
            return roles.values().stream().filter(r -> r.id().equals(id)).findFirst();
        }

        @Override
        public Optional<Rol> buscarPorCodigo(CodigoRol codigo) {
            return Optional.ofNullable(roles.get(codigo));
        }

        @Override
        public List<Rol> listar() {
            return List.copyOf(roles.values());
        }

        @Override
        public void guardar(Rol rol) {
            // Un rol que cambia de código deja de estar bajo el código anterior.
            roles.values().removeIf(r -> r.id().equals(rol.id()));
            roles.put(rol.codigo(), rol);
        }

        int cantidad() {
            return roles.size();
        }
    }

    static final class TokenRenovacionRepositorioEnMemoria implements TokenRenovacionRepositorio {

        private final Map<UUID, SesionRenovable> sesiones = new LinkedHashMap<>();
        private boolean otraRenovacionSeAdelanta;

        /** Devuelve una copia, como una lectura de la base: cambiarla no cambia lo guardado. */
        @Override
        public Optional<SesionRenovable> buscarPorHash(String tokenHash) {
            Optional<SesionRenovable> leida = sesiones.values().stream()
                    .filter(s -> s.tokenHash().equals(tokenHash))
                    .findFirst()
                    .map(s -> new SesionRenovable(s.id(), s.usuarioId(), s.familiaId(), s.tokenHash(), s.creadoEn(),
                            s.expiraEn(), s.revocadoEn()));
            if (otraRenovacionSeAdelanta && leida.isPresent()) {
                // Carrera A2: otra solicitud consume el token justo después de esta lectura.
                otraRenovacionSeAdelanta = false;
                consumir(leida.get().id(), AHORA);
            }
            return leida;
        }

        @Override
        public void guardar(SesionRenovable sesion) {
            sesiones.put(sesion.id(), sesion);
        }

        @Override
        public boolean consumir(UUID id, Instant ahora) {
            SesionRenovable guardada = sesiones.get(id);
            if (guardada == null || guardada.estaRevocada()) {
                return false;
            }
            guardada.revocar(ahora);
            return true;
        }

        /** La próxima búsqueda ve el token vigente, pero otra renovación lo consume antes que esta solicitud. */
        void otraRenovacionSeAdelantaraAlBuscar() {
            otraRenovacionSeAdelanta = true;
        }

        @Override
        public void revocarFamilia(UUID familiaId, Instant ahora) {
            sesiones.values().stream().filter(s -> s.familiaId().equals(familiaId)).forEach(s -> s.revocar(ahora));
        }

        @Override
        public void revocarDeUsuario(UUID usuarioId, Instant ahora) {
            sesiones.values().stream().filter(s -> s.usuarioId().equals(usuarioId)).forEach(s -> s.revocar(ahora));
        }

        List<SesionRenovable> todas() {
            return List.copyOf(sesiones.values());
        }

        List<SesionRenovable> deFamilia(UUID familiaId) {
            return sesiones.values().stream().filter(s -> s.familiaId().equals(familiaId)).toList();
        }
    }

    /** "Hash" legible: prefijo + contraseña. Registra cada contraseña que recibe. */
    static final class CodificadorFalso implements CodificadorContrasena {

        static final String PREFIJO = "bcrypt-falso:";

        final List<String> recibidas = new ArrayList<>();

        @Override
        public String codificar(String contrasena) {
            recibidas.add(contrasena);
            return PREFIJO + contrasena;
        }

        @Override
        public boolean coincide(String contrasena, String hash) {
            recibidas.add(contrasena);
            return hash.equals(PREFIJO + contrasena);
        }
    }

    /** Registra con qué usuario, roles y duración se emitió cada token de acceso. */
    static final class EmisorTokensFalso implements EmisorTokens {

        record Emision(UUID usuarioId, Set<CodigoRol> roles, Instant emitidoEn, Duration duracion) {
        }

        final List<Emision> emisiones = new ArrayList<>();

        @Override
        public String emitirAcceso(Usuario usuario, Instant emitidoEn, Duration duracion) {
            emisiones.add(new Emision(usuario.id(), Set.copyOf(usuario.roles()), emitidoEn, duracion));
            return "acceso-" + emisiones.size();
        }

        Emision ultima() {
            return emisiones.getLast();
        }
    }

    /** Tokens "renovacion-N" y hash "sha256:" + token, para poder buscarlos en las pruebas. */
    static final class GeneradorTokenFalso implements GeneradorTokenRenovacion {

        private int contador;

        @Override
        public String nuevo() {
            contador++;
            return "renovacion-" + contador;
        }

        @Override
        public String hash(String token) {
            return "sha256:" + token;
        }
    }
}
