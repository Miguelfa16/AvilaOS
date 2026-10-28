package unimet.avilaos.sincronizacion;

import java.util.concurrent.Semaphore;

/**
 * Exclusion mutua entre HILOS de Java (no es el mutex del productor-consumidor,
 * ese es un semaforo simulado de AvilaOS).
 */
public final class Mutex {

    private final Semaphore semaforo = new Semaphore(1, true);

    public void cerrar() {
        semaforo.acquireUninterruptibly();
    }

    public void abrir() {
        semaforo.release();
    }
}
