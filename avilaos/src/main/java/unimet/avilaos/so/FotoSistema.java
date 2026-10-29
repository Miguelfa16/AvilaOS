package unimet.avilaos.so;

import unimet.avilaos.hardware.ModoEjecucion;
import unimet.avilaos.so.planificacion.TipoPolitica;
import unimet.avilaos.so.proceso.EstadoProceso;
import unimet.avilaos.so.proceso.PCB;

/** Copia del estado del sistema operativo para que la GUI la pinte sin trancar la simulacion. */
public class FotoSistema {

    public static final String[] COLUMNAS = {
        "ID", "Nombre", "Comp.", "Estado", "Tipo", "Prior.", "Mem", "Deadline",
        "PC", "MAR", "Restante", "Motivo / fase"
    };

    public int computador;
    public TipoPolitica politica;
    public ModoEjecucion modo;
    public String instruccion;
    public Object[] enCpu;
    public int pcEnCpu;
    public int memUsada;
    public int memTotal;
    public Object[][] nuevos;
    public Object[][] listos;
    public Object[][] bloqueados;
    public Object[][] terminados;

    public static Object[] fila(PCB p) {
        String extra;
        if (p.getEstado() == EstadoProceso.BLOQUEADO) {
            extra = p.getMotivoBloqueo().getEtiqueta();
        } else if (p.getFase() != null) {
            extra = p.getFase() + " (" + p.getElementosHechos() + "/" + p.getElementosRequeridos() + ")";
        } else {
            extra = "-";
        }
        Object deadline = p.getEstado() == EstadoProceso.TERMINADO
                ? (p.isTerminadoPorDeadline() ? "VENCIDO" : "OK")
                : p.getDeadlineRestante();
        return new Object[] {
            p.getEtiqueta(), p.getNombre(), p.getIdComputador(), p.getEstado().getEtiqueta(),
            p.getTipo().getEtiqueta(), p.getPrioridad(), p.getMemoria(), deadline,
            p.getContadorPrograma(), p.getMar(), p.getTiempoRestante(), extra
        };
    }
}
