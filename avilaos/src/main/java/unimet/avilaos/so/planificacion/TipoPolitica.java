package unimet.avilaos.so.planificacion;

public enum TipoPolitica {
    FCFS("FCFS"),
    EDF("EDF (Earliest Deadline First)"),
    ROUND_ROBIN("Round Robin"),
    PRIORIDADES("Prioridades apropiativas");

    private final String etiqueta;

    TipoPolitica(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
