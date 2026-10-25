package unimet.avilaos.hardware;

/**
 * Memoria principal. Solo lleva la cuenta de usada y libre
 * (no hace falta particionar en este proyecto).
 */
public class Memoria {

    private final int capacidad;
    private int usada;

    public Memoria(int capacidad) {
        if (capacidad < 1) {
            throw new IllegalArgumentException("La RAM debe ser al menos 1");
        }
        this.capacidad = capacidad;
    }

    public boolean puedeReservar(int cantidad) {
        return usada + cantidad <= capacidad;
    }

    public boolean reservar(int cantidad) {
        if (!puedeReservar(cantidad)) {
            return false;
        }
        usada += cantidad;
        return true;
    }

    public void liberar(int cantidad) {
        usada = Math.max(0, usada - cantidad);
    }

    public int getCapacidad() { return capacidad; }
    public int getUsada() { return usada; }
    public int getLibre() { return capacidad - usada; }
}
