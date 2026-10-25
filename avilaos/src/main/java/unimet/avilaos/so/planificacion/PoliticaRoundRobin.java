package unimet.avilaos.so.planificacion;

import unimet.avilaos.estructuras.Comparador;
import unimet.avilaos.so.proceso.PCB;
public class PoliticaRoundRobin implements PoliticaPlanificacion {

    private static final Comparador<PCB> ORDEN = (a, b) -> 0;

    private volatile int quantum;

    public PoliticaRoundRobin(int quantum) {
        setQuantum(quantum);
    }

    public int getQuantum() {
        return quantum;
    }

    public final void setQuantum(int quantum) {
        if (quantum < 1) {
            throw new IllegalArgumentException("El quantum debe ser al menos 1");
        }
        this.quantum = quantum;
    }

    @Override
    public TipoPolitica getTipo() {
        return TipoPolitica.ROUND_ROBIN;
    }

    @Override
    public Comparador<PCB> getComparador() {
        return ORDEN;
    }

    @Override
    public boolean debeExpropiar(PCB enEjecucion, PCB candidato, int ciclosEnCpu) {
        return ciclosEnCpu >= quantum;
    }

    @Override
    public String getNombre() {
        return TipoPolitica.ROUND_ROBIN.getEtiqueta() + " (q=" + quantum + ")";
    }
}
