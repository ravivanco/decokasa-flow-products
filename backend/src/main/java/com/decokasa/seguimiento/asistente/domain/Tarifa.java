package com.decokasa.seguimiento.asistente.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Tarifa de un modelo desde una fecha (ADR-0022), en micro-USD por millón de tokens. Java puro: el costo se calcula aquí
 * y no en el adaptador. Si hay umbral y el prompt de una petición (entrada más escritura y lectura de caché) lo supera,
 * toda la petición paga los precios superiores (documentación oficial de precios, "Long context pricing").
 *
 * @param umbralTokensEntrada nulo si el modelo no tiene tramo superior
 * @param validada un admin la comparó con la fuente oficial; sin validar, la IA no llama al proveedor
 */
public record Tarifa(UUID id, String modelo, LocalDate vigenteDesde, Precios base, Integer umbralTokensEntrada,
        Precios superior, String fuente, LocalDate consultadoEn, boolean validada) {

    private static final long POR_MILLON = 1_000_000L;

    /** Micro-USD por millón de tokens. */
    public record Precios(long entrada, long salida, long escrituraCache, long lecturaCache) {

        public Precios {
            if (entrada < 1 || salida < 1 || escrituraCache < 1 || lecturaCache < 1) {
                throw new IllegalArgumentException("Los precios deben ser positivos");
            }
        }
    }

    public Tarifa {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(modelo, "modelo");
        Objects.requireNonNull(vigenteDesde, "vigenteDesde");
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(fuente, "fuente");
        Objects.requireNonNull(consultadoEn, "consultadoEn");
        if ((umbralTokensEntrada == null) != (superior == null)
                || (umbralTokensEntrada != null && umbralTokensEntrada < 1)) {
            throw new IllegalArgumentException("Tramo superior incompleto");
        }
        if (!fuente.startsWith("https://")) {
            throw new IllegalArgumentException("La fuente debe ser una URL https");
        }
    }

    /**
     * Peor caso de una consulta: los tokens de entrada contados más {@code max_tokens} de salida. La entrada se valora
     * al mayor de los precios de entrada y de escritura de caché, por si el proveedor escribiera en caché.
     */
    public long costoMaximo(long tokensEntrada, long maxTokensSalida) {
        Precios precios = preciosPara(tokensEntrada);
        long entrada = Math.max(precios.entrada(), precios.escrituraCache());
        long micro = Math.addExact(Math.multiplyExact(tokensEntrada, entrada),
                Math.multiplyExact(maxTokensSalida, precios.salida()));
        return Math.ceilDiv(micro, POR_MILLON);
    }

    /** Costo con el {@code usage} real, redondeado hacia arriba al micro-USD. */
    public long costoReal(long entrada, long salida, long escrituraCache, long lecturaCache) {
        long prompt = Math.addExact(Math.addExact(entrada, escrituraCache), lecturaCache);
        Precios precios = preciosPara(prompt);
        long micro = Math.addExact(Math.addExact(Math.multiplyExact(entrada, precios.entrada()),
                Math.multiplyExact(salida, precios.salida())),
                Math.addExact(Math.multiplyExact(escrituraCache, precios.escrituraCache()),
                        Math.multiplyExact(lecturaCache, precios.lecturaCache())));
        return Math.ceilDiv(micro, POR_MILLON);
    }

    /** El límite de entrada debe quedar bajo el umbral: si no, una consulta podría saltar al tramo caro sin aviso. */
    public boolean admiteLimiteDeEntrada(int tokensEntradaMax) {
        return umbralTokensEntrada == null || tokensEntradaMax < umbralTokensEntrada;
    }

    private Precios preciosPara(long tokensPrompt) {
        return umbralTokensEntrada != null && tokensPrompt > umbralTokensEntrada ? superior : base;
    }
}
