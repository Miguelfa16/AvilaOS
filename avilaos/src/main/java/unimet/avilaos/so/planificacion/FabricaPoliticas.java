package unimet.avilaos.so.planificacion;

/** Crea la politica que corresponde a cada TipoPolitica. */
public final class FabricaPoliticas {

    private FabricaPoliticas() {
    }
    public static PoliticaPlanificacion crear(TipoPolitica tipo, int quantum) {
        return switch (tipo) {
            case FCFS -> new PoliticaFCFS();
            case EDF -> new PoliticaEDF();
            case ROUND_ROBIN -> new PoliticaRoundRobin(quantum);
            case PRIORIDADES -> new PoliticaPrioridades();
        };
    }
}
