package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.AutorizarCuentaGoogle;
import com.decokasa.seguimiento.archivos.application.port.out.ConexionOAuthGoogle;
import com.decokasa.seguimiento.archivos.application.port.out.ConexionOAuthGoogle.Pendiente;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.util.UUID;

/** Mismo criterio de autorización vigente que pedidos/archivos; no confía en roles antiguos del JWT. */
public final class AutorizarCuentaGoogleCasoDeUso implements AutorizarCuentaGoogle {
    private static final CodigoRol ADMIN = new CodigoRol("admin");
    private final ConsultarResponsables usuarios;
    private final ConexionOAuthGoogle conexion;

    public AutorizarCuentaGoogleCasoDeUso(ConsultarResponsables usuarios, ConexionOAuthGoogle conexion) {
        this.usuarios = usuarios;
        this.conexion = conexion;
    }

    @Override
    public Inicio iniciar(Solicitante solicitante, String vinculoNavegador) {
        exigirAdmin(solicitante.usuarioId());
        return conexion.iniciar(solicitante.usuarioId(), vinculoNavegador);
    }

    @Override
    public void completar(String estado, String vinculoNavegador, String codigo, boolean denegado) {
        // Consume una sola vez, antes de intercambiar códigos, y revalida al admin que inició la conexión.
        Pendiente pendiente = conexion.consumir(estado, vinculoNavegador);
        exigirAdmin(pendiente.adminId());
        if (denegado || codigo == null || codigo.isBlank() || codigo.length() > 4096) {
            throw new DatoInvalido("No se completó el consentimiento de Google. Inicia la conexión de nuevo.");
        }
        conexion.conectar(pendiente, codigo);
        // Si se retiró el rol durante la llamada externa no se debe conservar una autorización nueva.
        // La persistencia hace su comprobación final mediante el mismo puerto (configuración).
    }

    private void exigirAdmin(UUID usuarioId) {
        boolean autorizado = usuarios.estado(usuarioId)
                .filter(estado -> estado.activo() && estado.roles().contains(ADMIN)).isPresent();
        if (!autorizado) {
            throw new SinPermiso("Solo un admin vigente puede conectar la cuenta Google de la empresa.");
        }
    }
}
