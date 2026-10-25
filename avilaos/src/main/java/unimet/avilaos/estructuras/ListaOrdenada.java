package unimet.avilaos.estructuras;


public class ListaOrdenada<T> {

    private ListaEnlazada<T> elementos = new ListaEnlazada<>();
    private Comparador<T> comparador;

    public ListaOrdenada(Comparador<T> comparador) {
        if (comparador == null) {
            throw new IllegalArgumentException("El comparador no puede ser null");
        }
        this.comparador = comparador;
    }

    public void insertar(T dato) {
        insertarOrdenado(elementos, comparador, dato);
    }

    public T verPrimero() {
        return elementos.obtenerPrimero();
    }

    public T extraerPrimero() {
        return elementos.eliminarPrimero();
    }

    public boolean eliminar(T dato) {
        return elementos.eliminar(dato);
    }

    public boolean contiene(T dato) {
        return elementos.contiene(dato);
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

    public void cambiarComparador(Comparador<T> nuevo) {
        if (nuevo == null) {
            throw new IllegalArgumentException("El comparador no puede ser null");
        }
        this.comparador = nuevo;
        reordenar();
    }

    public void reordenar() {
        ListaEnlazada<T> nueva = new ListaEnlazada<>();
        Recorrido<T> r = elementos.recorrido();
        while (r.haySiguiente()) {
            insertarOrdenado(nueva, comparador, r.siguiente());
        }
        elementos = nueva;
    }

    public Recorrido<T> recorrido() {
        return elementos.recorrido();
    }

    private static <E> void insertarOrdenado(ListaEnlazada<E> lista, Comparador<E> c, E dato) {
        Nodo<E> actual = lista.primerNodo();
        while (actual != null && c.comparar(actual.dato, dato) <= 0) {
            actual = actual.siguiente;
        }
        lista.insertarAntesDe(actual, dato);
    }
}
