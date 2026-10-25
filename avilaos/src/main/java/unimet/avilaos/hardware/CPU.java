package unimet.avilaos.hardware;

import unimet.avilaos.so.proceso.PCB;

/**
 * Procesador. No decide nada: el nucleo le carga que ejecutar en el ciclo
 * y la CPU lo ejecuta cuando llega el tick.
 */
public class CPU implements ComponenteDeReloj {

    private PCB actual;
    private ModoEjecucion modo = ModoEjecucion.OCIOSO;
    private String instruccion = "-";
    private boolean avanzaPc;

    private long ciclosOcupada;
    private long ciclosTotales;
    private long ciclosSO;

    public void cargar(PCB pcb, ModoEjecucion modo, String instruccion, boolean avanzaPc) {
        this.actual = pcb;
        this.modo = modo;
        this.instruccion = instruccion;
        this.avanzaPc = avanzaPc;
    }

    public void ociosa() {
        cargar(null, ModoEjecucion.OCIOSO, "-", false);
    }

    @Override
    public void alTick(long ciclo) {
        ciclosTotales++;
        if (modo != ModoEjecucion.OCIOSO) {
            ciclosOcupada++;
        }
        if (modo == ModoEjecucion.SISTEMA_OPERATIVO) {
            ciclosSO++;
        }
        if (actual != null && avanzaPc) {
            actual.ejecutarInstruccion();
        }
    }

    /** El proceso sale de la CPU (bloqueo, expulsion o fin). */
    public void liberar() {
        actual = null;
    }

    public PCB getActual() { return actual; }
    public ModoEjecucion getModo() { return modo; }
    public String getInstruccion() { return instruccion; }
    public boolean esDespacho() { return actual != null && !avanzaPc; }
    public long getCiclosOcupada() { return ciclosOcupada; }
    public long getCiclosTotales() { return ciclosTotales; }
    public long getCiclosSO() { return ciclosSO; }
}
