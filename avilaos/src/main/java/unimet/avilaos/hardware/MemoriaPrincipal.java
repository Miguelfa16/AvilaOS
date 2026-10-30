package unimet.avilaos.hardware;

/** RAM de un computador. Solo lleva la cuenta de cuanto esta usado y cuanto queda. */
public class MemoriaPrincipal {

    private final int capacidad;
    private int enUso;

    public MemoriaPrincipal(int capacidad) {
        if (capacidad < 1) {
            throw new IllegalArgumentException();
        }
        this.capacidad = capacidad;
        this.enUso = 0;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public int getUsada() {
        return enUso;
    }

    public int getLibre() {
        return capacidad - enUso;
    }

    public boolean hayEspacio(int cantidad) {
        return getLibre() >= cantidad;
    }

    /** Aparta memoria para un proceso o buffer. Falla si no hay suficiente. */
    public void asignar(int cantidad) {
        if (!hayEspacio(cantidad)) {
            throw new IllegalStateException();
        }
        enUso += cantidad;
    }

    /** Devuelve memoria cuando un proceso termina. */
    public void liberar(int cantidad) {
        if (enUso - cantidad < 0) {
            throw new IllegalStateException();
        }
        enUso -= cantidad;
    }
}