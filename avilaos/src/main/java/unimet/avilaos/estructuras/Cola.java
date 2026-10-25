package unimet.avilaos.estructuras;

public class Cola<T> {

    private final ListaEnlazada<T> elementos = new ListaEnlazada<>();

    public void encolar(T dato) {
        elementos.agregarAlFinal(dato);
    }

    public T desencolar() {
        return elementos.eliminarPrimero();
    }


    public T verFrente() {
        return elementos.obtenerPrimero();
    }

    public boolean estaVacia() {
        return elementos.estaVacia();
    }

    public int tamano() {
        return elementos.tamano();
    }

    public boolean contiene(T dato) {
        return elementos.contiene(dato);
    }

    public boolean eliminar(T dato) {
        return elementos.eliminar(dato);
    }

    public void limpiar() {
        elementos.limpiar();
    }

    public Recorrido<T> recorrido() {
        return elementos.recorrido();
    }
}
