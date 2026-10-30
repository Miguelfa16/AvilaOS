package unimet.avilaos.estructuras;

/** Pila LIFO sobre ListaEnlazada. */
public class Pila<T> {

    private final ListaEnlazada<T> elementos = new ListaEnlazada<>();

    public void apilar(T dato) {
        elementos.agregarAlInicio(dato);
    }

    public T desapilar() {
        return elementos.eliminarPrimero();
    }

    public T verTope() {
        return elementos.obtenerPrimero();
    }

    public boolean estaVacia() {
        return elementos.estaVacia();
    }

    public int tamano() {
        return elementos.tamano();
    }

    public void limpiar() {
        elementos.limpiar();
    }

    public Recorrido<T> recorrido() {
        return elementos.recorrido();
    }
}
