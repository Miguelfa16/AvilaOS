package unimet.avilaos.hardware;

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

    public void asignar(int cantidad) {
        if (!hayEspacio(cantidad)) {
            throw new IllegalStateException();
        }
        enUso += cantidad;
    }

    public void liberar(int cantidad) {
        if (enUso - cantidad < 0) {
            throw new IllegalStateException();
        }
        enUso -= cantidad;
    }
}