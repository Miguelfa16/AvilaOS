package unimet.avilaos.so.proceso;

import java.util.concurrent.Semaphore;

public class GeneradorId {

    private final Semaphore mutex = new Semaphore(1);
    private long ultimo = 0;

    /** @return el siguiente ID, empezando en 1 */
    public long siguiente() {
        mutex.acquireUninterruptibly();
        try {
            return ++ultimo;
        } finally {
            mutex.release();
        }
    }
}
