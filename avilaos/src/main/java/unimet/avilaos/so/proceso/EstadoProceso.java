package unimet.avilaos.so.proceso;

/** Los cinco estados de un proceso y que cambios se permiten entre ellos. */
public enum EstadoProceso {
    NUEVO("Nuevo"),
    LISTO("Listo"),
    EJECUCION("Ejecucion"),
    BLOQUEADO("Bloqueado"),
    TERMINADO("Terminado");

    private final String etiqueta;

    EstadoProceso(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean puedePasarA(EstadoProceso destino) {
        return switch (this) {
            case NUEVO -> destino == LISTO || destino == TERMINADO;
            case LISTO -> destino == EJECUCION || destino == TERMINADO;
            case EJECUCION -> destino == LISTO || destino == BLOQUEADO || destino == TERMINADO;
            case BLOQUEADO -> destino == LISTO || destino == TERMINADO;
            case TERMINADO -> false;
        };
    }
}
