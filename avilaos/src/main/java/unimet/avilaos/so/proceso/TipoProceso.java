package unimet.avilaos.so.proceso;

public enum TipoProceso {
    CPU_BOUND("CPU bound", false),
    IO_BOUND("I/O bound", false),
    PRODUCTOR("Productor", true),
    CONSUMIDOR("Consumidor", true);

    private final String etiqueta;
    private final boolean usaBuffer;

    TipoProceso(String etiqueta, boolean usaBuffer) {
        this.etiqueta = etiqueta;
        this.usaBuffer = usaBuffer;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean usaBuffer() {
        return usaBuffer;
    }
}
