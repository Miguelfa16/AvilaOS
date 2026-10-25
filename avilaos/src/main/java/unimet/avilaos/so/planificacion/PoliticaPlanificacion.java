package unimet.avilaos.so.planificacion;

import unimet.avilaos.estructuras.Comparador;
import unimet.avilaos.so.proceso.PCB;

public interface PoliticaPlanificacion {
    TipoPolitica getTipo();
    Comparador<PCB> getComparador();
    boolean debeExpropiar(PCB enEjecucion, PCB candidato, int ciclosEnCpu);
    default String getNombre() {
        return getTipo().getEtiqueta();
    }
}
