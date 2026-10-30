package unimet.avilaos.so.planificacion;

import unimet.avilaos.estructuras.Comparador;
import unimet.avilaos.so.proceso.PCB;

/** First Come First Served: atiende en orden de llegada y nunca expulsa. */
public class PoliticaFCFS implements PoliticaPlanificacion {

    private static final Comparador<PCB> ORDEN = (a, b) -> 0;

    @Override
    public TipoPolitica getTipo() {
        return TipoPolitica.FCFS;
    }

    @Override
    public Comparador<PCB> getComparador() {
        return ORDEN;
    }

    @Override
    public boolean debeExpropiar(PCB enEjecucion, PCB candidato, int ciclosEnCpu) {
        return false;
    }
}
