package com.decokasa.seguimiento.usuarios.config;

import java.util.function.Predicate;
import java.util.function.Supplier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Abre la transacción alrededor de cada caso de uso, fuera del núcleo (ADR-0012): los casos de uso no llevan
 * {@code @Transactional} (R2).
 */
public final class Transacciones {

    private final TransactionTemplate plantilla;

    public Transacciones(PlatformTransactionManager gestor) {
        this.plantilla = new TransactionTemplate(gestor);
    }

    public <T> T ejecutar(Supplier<T> accion) {
        return plantilla.execute(estado -> accion.get());
    }

    /**
     * Confirma lo hecho aunque el caso de uso termine con uno de los errores indicados, y después lo relanza. Sirve
     * para que un intento fallido o la revocación de una sesión robada queden guardados aunque la respuesta sea 401.
     */
    public <T> T ejecutarConfirmandoAnte(Supplier<T> accion, Predicate<RuntimeException> confirmar) {
        RuntimeException[] pendiente = new RuntimeException[1];
        T resultado = plantilla.execute(estado -> {
            try {
                return accion.get();
            } catch (RuntimeException error) {
                if (!confirmar.test(error)) {
                    throw error;
                }
                pendiente[0] = error;
                return null;
            }
        });
        if (pendiente[0] != null) {
            throw pendiente[0];
        }
        return resultado;
    }
}
