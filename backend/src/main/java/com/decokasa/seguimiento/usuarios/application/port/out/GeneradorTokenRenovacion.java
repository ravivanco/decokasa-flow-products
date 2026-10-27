package com.decokasa.seguimiento.usuarios.application.port.out;

/** Crea tokens de renovación al azar y calcula su hash (SHA-256); solo el hash se guarda (ADR-0002). */
public interface GeneradorTokenRenovacion {

    String nuevo();

    String hash(String token);
}
