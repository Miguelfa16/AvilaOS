package unimet.avilaos.estructuras;



final class Nodo<T> {

    T dato;
    Nodo<T> siguiente;
    Nodo<T> anterior;

    Nodo(T dato) {
        this.dato = dato;
    }
}
