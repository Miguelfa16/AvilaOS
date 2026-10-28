package unimet.avilaos.sincronizacion;

import unimet.avilaos.so.proceso.PCB;

/**
 * Buffer acotado del productor-consumidor. Vive en la RAM de su anfitrion.
 * Usa 3 semaforos de AvilaOS (vacios, llenos, mutex) y un Mutex de Java
 * porque lo tocan hilos de varios computadores.
 */
public class BufferAcotado {

    private final String nombre;
    private final int capacidad;
    private final int anfitrion;
    private final int memoria;

    private final long[] huecos;
    private int entrada;
    private int salida;
    private int cantidad;

    private final SemaforoConteo vacios;
    private final SemaforoConteo llenos;
    private final SemaforoBinario mutex;

    private final Mutex candado = new Mutex();
    private EnrutadorDespertar enrutador;

    private long producidos;
    private long consumidos;

    public BufferAcotado(String nombre, int capacidad, int anfitrion, int memoria) {
        if (capacidad < 1) {
            throw new IllegalArgumentException("La capacidad debe ser al menos 1");
        }
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.anfitrion = anfitrion;
        this.memoria = memoria;
        this.huecos = new long[capacidad];
        this.vacios = new SemaforoConteo("vacios", capacidad);
        this.llenos = new SemaforoConteo("llenos", 0);
        this.mutex = new SemaforoBinario("mutex");
    }

    public void setEnrutador(EnrutadorDespertar e) { this.enrutador = e; }

    public void cerrar() { candado.cerrar(); }
    public void abrir() { candado.abrir(); }

    // ---- operaciones (quien llama debe tener cerrar()) ----

    public void insertar(PCB productor) {
        huecos[entrada] = productor.getId();
        entrada = (entrada + 1) % capacidad;
        cantidad++;
        producidos++;
    }

    public void extraer() {
        huecos[salida] = 0;
        salida = (salida + 1) % capacidad;
        cantidad--;
        consumidos++;
    }

    /** semSignal y manda a despertar al proceso en su computador. */
    public void senalar(SemaforoSO s) {
        PCB despierta = s.senalar();
        if (despierta != null && enrutador != null) {
            enrutador.despertar(despierta, "signal(" + s.getNombre() + ") en " + nombre);
        }
    }

    public SemaforoConteo getVacios() { return vacios; }
    public SemaforoConteo getLlenos() { return llenos; }
    public SemaforoBinario getMutex() { return mutex; }

    public String getNombre() { return nombre; }
    public int getCapacidad() { return capacidad; }
    public int getAnfitrion() { return anfitrion; }
    public int getMemoria() { return memoria; }

    public static final String[] COLUMNAS = {
        "Buffer", "Anfitrion", "Elementos", "vacios", "llenos", "mutex",
        "Esperan vacios", "Esperan llenos", "Esperan mutex", "Producidos", "Consumidos"
    };

    public Object[] fila() {
        cerrar();
        try {
            return new Object[] {
                nombre, "C" + anfitrion, cantidad + "/" + capacidad,
                vacios.getValor(), llenos.getValor(), mutex.getValor(),
                vacios.colaComoTexto(), llenos.colaComoTexto(), mutex.colaComoTexto(),
                producidos, consumidos
            };
        } finally {
            abrir();
        }
    }

    public long getProducidos() {
        cerrar();
        try { return producidos; } finally { abrir(); }
    }

    public long getConsumidos() {
        cerrar();
        try { return consumidos; } finally { abrir(); }
    }

    @Override
    public String toString() {
        return nombre + " (C" + anfitrion + ", cap " + capacidad + ")";
    }
}
