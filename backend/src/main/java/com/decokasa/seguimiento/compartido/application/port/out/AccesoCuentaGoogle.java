package com.decokasa.seguimiento.compartido.application.port.out;

/** Credencial interna del backend para adaptadores; no es un puerto HTTP ni una respuesta serializable de negocio. */
public interface AccesoCuentaGoogle {
    Credencial credencial();

    record Credencial(String correo, String tokenAcceso) {
        @Override public String toString() { return "CredencialGoogle[PROTEGIDO]"; }
    }
}
