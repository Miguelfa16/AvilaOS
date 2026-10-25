package unimet.avilaos.estructuras;


public class ListaEnlazada<T> {

    private Nodo<T> primero;
    private Nodo<T> ultimo;
    private int tamano;

    public int tamano() {
        return tamano;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    public void agregarAlInicio(T dato) {
        insertarAntesDe(primero, dato);
    }

    public void agregarAlFinal(T dato) {
        insertarAntesDe(null, dato);
    }

    public void insertarEn(int indice, T dato) {
        if (indice < 0 || indice > tamano) {
            throw new IndexOutOfBoundsException("Indice " + indice + ", tamano " + tamano);
        }
        if (indice == tamano) {
            agregarAlFinal(dato);
        } else {
            insertarAntesDe(nodoEn(indice), dato);
        }
    }

    public T obtener(int indice) {
        return nodoEn(indice).dato;
    }

    public T obtenerPrimero() {
        if (primero == null) {
            throw new IllegalStateException("La lista esta vacia");
        }
        return primero.dato;
    }

    public T obtenerUltimo() {
        if (ultimo == null) {
            throw new IllegalStateException("La lista esta vacia");
        }
        return ultimo.dato;
    }

    public boolean contiene(T dato) {
        return indiceDe(dato) >= 0;
    }

    public int indiceDe(T dato) {
        int i = 0;
        for (Nodo<T> n = primero; n != null; n = n.siguiente) {
            if (iguales(n.dato, dato)) {
                return i;
            }
            i++;
        }
        return -1;
    }


    public T eliminarPrimero() {
        if (primero == null) {
            throw new IllegalStateException("La lista esta vacia");
        }
        Nodo<T> n = primero;
        desenlazar(n);
        return n.dato;
    }

    public T eliminarUltimo() {
        if (ultimo == null) {
            throw new IllegalStateException("La lista esta vacia");
        }
        Nodo<T> n = ultimo;
        desenlazar(n);
        return n.dato;
    }

    public T eliminarEn(int indice) {
        Nodo<T> n = nodoEn(indice);
        desenlazar(n);
        return n.dato;
    }

    public boolean eliminar(T dato) {
        for (Nodo<T> n = primero; n != null; n = n.siguiente) {
            if (iguales(n.dato, dato)) {
                desenlazar(n);
                return true;
            }
        }
        return false;
    }

    public void limpiar() {
        primero = null;
        ultimo = null;
        tamano = 0;
    }

    public Recorrido<T> recorrido() {
        return new RecorridoLista();
    }

    private class RecorridoLista implements Recorrido<T> {

        private Nodo<T> proximo = primero;

        @Override
        public boolean haySiguiente() {
            return proximo != null;
        }

        @Override
        public T siguiente() {
            if (proximo == null) {
                throw new IllegalStateException("No hay mas elementos");
            }
            T dato = proximo.dato;
            proximo = proximo.siguiente;
            return dato;
        }
    }


    Nodo<T> primerNodo() {
        return primero;
    }

    void insertarAntesDe(Nodo<T> referencia, T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (referencia == null) {
            nuevo.anterior = ultimo;
            if (ultimo != null) {
                ultimo.siguiente = nuevo;
            } else {
                primero = nuevo;
            }
            ultimo = nuevo;
        } else {
            nuevo.siguiente = referencia;
            nuevo.anterior = referencia.anterior;
            if (referencia.anterior != null) {
                referencia.anterior.siguiente = nuevo;
            } else {
                primero = nuevo;
            }
            referencia.anterior = nuevo;
        }
        tamano++;
    }

    private Nodo<T> nodoEn(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException("Indice " + indice + ", tamano " + tamano);
        }
        Nodo<T> n;
        if (indice < tamano / 2) {
            n = primero;
            for (int i = 0; i < indice; i++) {
                n = n.siguiente;
            }
        } else {
            n = ultimo;
            for (int i = tamano - 1; i > indice; i--) {
                n = n.anterior;
            }
        }
        return n;
    }

    private void desenlazar(Nodo<T> n) {
        if (n.anterior != null) {
            n.anterior.siguiente = n.siguiente;
        } else {
            primero = n.siguiente;
        }
        if (n.siguiente != null) {
            n.siguiente.anterior = n.anterior;
        } else {
            ultimo = n.anterior;
        }
        tamano--;
    }

    private static boolean iguales(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}
