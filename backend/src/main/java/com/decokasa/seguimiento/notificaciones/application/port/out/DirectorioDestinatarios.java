package com.decokasa.seguimiento.notificaciones.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.util.Set;

/** Sin política final no se inventa un receptor: vacío se conserva como pendiente de resolución. */
public interface DirectorioDestinatarios {
    Set<String> administradores(CodigoPais pais);
}
