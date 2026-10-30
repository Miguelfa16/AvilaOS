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

    /**
     * El sistema operativo le dice a la CPU que va a hacer en este ciclo.
     * @param avanzaPc false cuando el ciclo es un despacho (el PC no se mueve)
     */
    public void cargar(PCB pcb, ModoEjecucion modo, String instruccion, boolean avanzaPc) {
        this.actual = pcb;
        this.modo = modo;
        this.instruccion = instruccion;
        this.avanzaPc = avanzaPc;
    }

    public void ociosa() {
        cargar(null, ModoEjecucion.OCIOSO, "-", false);
    }

    /** Ejecuta el ciclo: cuenta el uso y, si toca, sube el PC y el MAR del proceso. */
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
