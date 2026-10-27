package com.decokasa.seguimiento.archivos.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoError;
import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;

/** Mensajes cerrados: sin rutas, contenido del archivo, errores SQL o causas de infraestructura. */
public final class ProblemaCarga extends ErrorDeDominio {
    private static final long serialVersionUID = 1L;
    private ProblemaCarga(CodigoError codigo, String mensaje) { super(codigo,mensaje); }
    public static ProblemaCarga conflicto() {
        return new ProblemaCarga(CodigoError.CARGA_CONFLICTO,"La carga o el fragmento no coincide con lo ya confirmado. Consulta su progreso antes de continuar.");
    }
    public static ProblemaCarga incompleta() {
        return new ProblemaCarga(CodigoError.CARGA_INCOMPLETA,"Todavía faltan bytes del archivo. Continúa la carga antes de completarla.");
    }
    public static ProblemaCarga ocupada() {
        return new ProblemaCarga(CodigoError.CARGA_OCUPADA,"Esta carga está siendo atendida. Consulta el progreso y vuelve a intentarlo.");
    }
    public static ProblemaCarga almacenamiento() {
        return new ProblemaCarga(CodigoError.CARGA_ALMACENAMIENTO,"No se pudo conservar o recuperar la carga. No se ha confirmado un adjunto nuevo. Avisa al administrador si persiste.");
    }
}
