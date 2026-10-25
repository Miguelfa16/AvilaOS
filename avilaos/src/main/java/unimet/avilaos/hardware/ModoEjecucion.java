package unimet.avilaos.hardware;

public enum ModoEjecucion {
    USUARIO("Programa de usuario"),
    SISTEMA_OPERATIVO("Sistema operativo");

    private final String etiqueta;

    ModoEjecucion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
