package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.AdjuntoVista;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import java.util.Set;
import java.util.UUID;

/** Traduce el resumen de usuarios a la persona que se muestra en un adjunto. */
final class PersonasDeArchivos {

    private PersonasDeArchivos() {
    }

    static AdjuntoVista.Persona persona(UUID id, ResumirUsuarios.Resumen resumen) {
        return resumen == null ? new AdjuntoVista.Persona(id, "Usuario no disponible", Set.of())
                : new AdjuntoVista.Persona(resumen.id(), resumen.nombre(), resumen.roles());
    }
}
