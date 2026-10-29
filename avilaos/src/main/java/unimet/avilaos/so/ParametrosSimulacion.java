package unimet.avilaos.so;

/** Parametros globales que se pueden cambiar mientras corre la simulacion. */
public class ParametrosSimulacion {

    private volatile int quantum;
    private volatile int latenciaRed;

    public ParametrosSimulacion(int quantum, int latenciaRed) {
        setQuantum(quantum);
        setLatenciaRed(latenciaRed);
    }

    public int getQuantum() { return quantum; }
    public int getLatenciaRed() { return latenciaRed; }

    public void setQuantum(int q) { quantum = Math.max(1, q); }
    public void setLatenciaRed(int l) { latenciaRed = Math.max(0, l); }
}
