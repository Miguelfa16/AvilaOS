package unimet.avilaos.estructuras;


@FunctionalInterface
public interface Comparador<T> {
    int comparar(T a, T b);
}
