package unimet.avilaos.so;

import unimet.avilaos.estructuras.ListaEnlazada;
import unimet.avilaos.sincronizacion.Mutex;

/** Log de eventos. Varios hilos escriben y la GUI lee las lineas nuevas. */
public class RegistroEventos {

    private final Mutex mutex = new Mutex();
    private ListaEnlazada<String> pendientes = new ListaEnlazada<>();
    private volatile long ciclo;

    public void setCiclo(long c) { ciclo = c; }

    public void registrar(String mensaje) {
        String linea = "[" + ciclo + "] " + mensaje;
        mutex.cerrar();
        try {
            pendientes.agregarAlFinal(linea);
        } finally {
            mutex.abrir();
        }
    }

    /** Devuelve las lineas nuevas y las saca. */
    public ListaEnlazada<String> vaciar() {
        mutex.cerrar();
        try {
            ListaEnlazada<String> out = pendientes;
            pendientes = new ListaEnlazada<>();
            return out;
        } finally {
            mutex.abrir();
        }
    }
}
