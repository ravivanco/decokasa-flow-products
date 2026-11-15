package com.decokasa.seguimiento.archivos.application.port.out;

import com.decokasa.seguimiento.archivos.application.port.in.AccesoConfidencial;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Lecturas y escrituras de la confidencialidad (V37). */
public interface ConfidencialesRepositorio {

    Set<UUID> confidenciales(UUID pedidoId);

    List<AccesoConfidencial.PorEtapa> porEtapa(UUID pedidoId);

    boolean accesoPorRol(UUID usuarioId);

    boolean accesoOtorgado(UUID pedidoId, UUID usuarioId);

    boolean esAdminVigente(UUID usuarioId);

    boolean usuarioActivo(UUID usuarioId);

    List<AccesoConfidencial.ConAcceso> conAcceso(UUID pedidoId);

    void otorgar(UUID id, UUID pedidoId, UUID usuarioId, UUID porAdmin, Instant ahora);

    void revocar(UUID pedidoId, UUID usuarioId, UUID porAdmin, Instant ahora);
}
