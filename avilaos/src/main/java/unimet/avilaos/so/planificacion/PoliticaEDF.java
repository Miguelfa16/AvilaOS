package unimet.avilaos.so.planificacion;

import unimet.avilaos.estructuras.Comparador;
import unimet.avilaos.so.proceso.PCB;


/** Earliest Deadline First: primero el que tenga el deadline mas cerca. */
public class PoliticaEDF implements PoliticaPlanificacion {

    private static final Comparador<PCB> ORDEN =
            (a, b) -> Integer.compare(a.getDeadlineRestante(), b.getDeadlineRestante());

    @Override
    public TipoPolitica getTipo() {
        return TipoPolitica.EDF;
    }

    @Override
    public Comparador<PCB> getComparador() {
        return ORDEN;
    }

    @Override
    public boolean debeExpropiar(PCB enEjecucion, PCB candidato, int ciclosEnCpu) {
        return candidato.getDeadlineRestante() < enEjecucion.getDeadlineRestante();
    }
}
