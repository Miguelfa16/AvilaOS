package unimet.avilaos.so.proceso;

public enum MotivoBloqueo {
    NINGUNO("-"),
    SEM_MUTEX("Semaforo mutex"),
    SEM_VACIOS("Semaforo vacios (buffer lleno)"),
    SEM_LLENOS("Semaforo llenos (buffer vacio)"),
    ACCESO_REMOTO("Acceso remoto (latencia de red)"),
    ENTRADA_SALIDA("Entrada/Salida");
    private final String etiqueta;
    MotivoBloqueo(String etiqueta) {
        this.etiqueta = etiqueta;
    }
    public String getEtiqueta() {
        return etiqueta;
    }
}
