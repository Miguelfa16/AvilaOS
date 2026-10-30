package unimet.avilaos.so.planificacion;

import unimet.avilaos.estructuras.Comparador;
import unimet.avilaos.so.proceso.PCB;

/**
 * Lo que tiene que saber hacer cualquier politica de planificacion.
 * Para agregar una nueva basta con otra clase que implemente esto.
 */
public interface PoliticaPlanificacion {
    TipoPolitica getTipo();
    /** Como se ordena la cola de listos con esta politica. */
    Comparador<PCB> getComparador();
    /**
     * Dice si hay que sacar al proceso de la CPU para meter al candidato.
     * @param enEjecucion el que esta en la CPU
     * @param candidato el primero de la cola de listos
     * @param ciclosEnCpu cuantos ciclos seguidos lleva el que esta en la CPU
     * @return true si hay que cambiarlos
     */
    boolean debeExpropiar(PCB enEjecucion, PCB candidato, int ciclosEnCpu);
    default String getNombre() {
        return getTipo().getEtiqueta();
    }
}
