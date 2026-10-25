package unimet.avilaos.hardware;

public enum ModoEjecucion {
    USUARIO("Programa de usuario"),
    SISTEMA_OPERATIVO("Sistema operativo"),
    OCIOSO("CPU ociosa");

    private final String etiqueta;

    ModoEjecucion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
