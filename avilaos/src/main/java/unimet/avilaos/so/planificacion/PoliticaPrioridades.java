package unimet.avilaos.so.planificacion;

import unimet.avilaos.estructuras.Comparador;
import unimet.avilaos.so.proceso.PCB;
/** Prioridades apropiativas: si llega uno mas importante, le quita la CPU al que esta. */
public class PoliticaPrioridades implements PoliticaPlanificacion {
    private static final Comparador<PCB> ORDEN =
            (a, b) -> Integer.compare(a.getPrioridad(), b.getPrioridad());
    @Override
    public TipoPolitica getTipo() {
        return TipoPolitica.PRIORIDADES;
    }
    @Override
    public Comparador<PCB> getComparador() {
        return ORDEN;
    }
    @Override
    public boolean debeExpropiar(PCB enEjecucion, PCB candidato, int ciclosEnCpu) {
        return candidato.getPrioridad() < enEjecucion.getPrioridad();
    }
}
