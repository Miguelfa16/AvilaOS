package unimet.avilaos.hardware;

import java.util.concurrent.Semaphore;

/**
 * Reloj global del sistema distribuido. Corre en su propio hilo.
 * En cada ciclo libera a todos los computadores y espera a que todos
 * terminen (barrera con semaforos), asi cada uno avanza exactamente un ciclo.
 */
public class RelojGlobal implements Runnable {

    /** Algo que se sincroniza con el reloj (cada Computador). */
    public interface Participante {
        Semaphore inicioCiclo();
    }

    private final Participante[] participantes;
    private final Semaphore finCiclo = new Semaphore(0);
    private final Semaphore mutex = new Semaphore(1);
    private final Semaphore pausa = new Semaphore(0);

    private volatile long ciclo = 0;
    private volatile int duracionCiclo;
    private volatile boolean corriendo = false;
    private volatile boolean vivo = true;
    private Runnable trasCiclo = () -> { };

    public RelojGlobal(Participante[] participantes, int duracionCiclo) {
        this.participantes = participantes;
        this.duracionCiclo = duracionCiclo;
    }

    public Semaphore finCiclo() { return finCiclo; }
    public long getCiclo() { return ciclo; }
    public int getDuracionCiclo() { return duracionCiclo; }
    public boolean estaCorriendo() { return corriendo; }

    public void setDuracionCiclo(int ms) {
        this.duracionCiclo = Math.max(1, ms);
    }

    public void setTrasCiclo(Runnable r) {
        this.trasCiclo = r;
    }

    public void iniciar() {
        if (!corriendo) {
            corriendo = true;
            pausa.release();
        }
    }

    public void pausar() {
        corriendo = false;
    }

    /** Avanza un solo ciclo estando en pausa. */
    public void paso() {
        if (!corriendo) {
            tic();
        }
    }

    public void detener() {
        vivo = false;
        corriendo = false;
        pausa.release();
    }

    @Override
    public void run() {
        while (vivo) {
            if (!corriendo) {
                pausa.acquireUninterruptibly();
                pausa.drainPermits();
                continue;
            }
            tic();
            try {
                Thread.sleep(duracionCiclo);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void tic() {
        mutex.acquireUninterruptibly();
        try {
            ciclo++;
            for (Participante p : participantes) {
                p.inicioCiclo().release();
            }
            finCiclo.acquireUninterruptibly(participantes.length);
            trasCiclo.run();
        } finally {
            mutex.release();
        }
    }
}
