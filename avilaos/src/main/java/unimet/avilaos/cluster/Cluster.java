package unimet.avilaos.cluster;

import unimet.avilaos.estructuras.ListaEnlazada;
import unimet.avilaos.estructuras.Recorrido;
import unimet.avilaos.hardware.Computador;
import unimet.avilaos.hardware.RelojGlobal;
import unimet.avilaos.metricas.Estadisticas;
import unimet.avilaos.sincronizacion.BufferAcotado;
import unimet.avilaos.sincronizacion.EnrutadorDespertar;
import unimet.avilaos.sincronizacion.Mutex;
import unimet.avilaos.so.ParametrosSimulacion;
import unimet.avilaos.so.RegistroEventos;
import unimet.avilaos.so.planificacion.FabricaPoliticas;
import unimet.avilaos.so.planificacion.TipoPolitica;
import unimet.avilaos.so.proceso.GeneradorId;
import unimet.avilaos.so.proceso.PCB;
import unimet.avilaos.so.proceso.TipoProceso;

/**
 * El sistema distribuido: computadores, buffers, reloj, IDs unicos y log.
 * Es lo unico que usa la GUI.
 */
public class Cluster implements EnrutadorDespertar {

    private final Computador[] computadores;
    private final ListaEnlazada<BufferAcotado> buffers = new ListaEnlazada<>();
    private final ListaEnlazada<EspecificacionProceso> procesosCreados = new ListaEnlazada<>();
    private final ListaEnlazada<EspecificacionBuffer> buffersCreados = new ListaEnlazada<>();
    private final Mutex candado = new Mutex();
    private final GeneradorId ids = new GeneradorId();

    private final ParametrosSimulacion parametros;
    private final RegistroEventos log = new RegistroEventos();
    private final RelojGlobal reloj;

    public Cluster(int[] rams, TipoPolitica[] politicas, int duracionCiclo, int quantum, int latencia) {
        if (rams.length < 2) {
            throw new IllegalArgumentException("Hacen falta al menos 2 computadores");
        }
        parametros = new ParametrosSimulacion(quantum, latencia);
        computadores = new Computador[rams.length];
        for (int i = 0; i < rams.length; i++) {
            computadores[i] = new Computador(i + 1, rams[i], FabricaPoliticas.crear(politicas[i], quantum), parametros, log);
        }
        reloj = new RelojGlobal(computadores, duracionCiclo);
        reloj.setTrasCiclo(() -> log.setCiclo(reloj.getCiclo() + 1));
        for (Computador c : computadores) {
            c.conectar(reloj);
        }
        Thread hilo = new Thread(reloj, "RelojGlobal");
        hilo.setDaemon(true);
        hilo.start();
        log.registrar("AvilaOS arranca con " + computadores.length + " computadores");
    }

    // ------------------------------------------------------------- procesos

    /** Crea un proceso. Lanza IllegalArgumentException si un dato no sirve. */
    public PCB crearProceso(EspecificacionProceso e) {
        BufferAcotado buffer = null;
        if (e.tipo().usaBuffer()) {
            buffer = buscarBuffer(e.buffer());
            if (buffer == null) {
                throw new IllegalArgumentException("No existe el buffer " + e.buffer());
            }
        }
        if (e.computador() < 0 || e.computador() > computadores.length) {
            throw new IllegalArgumentException("Computador invalido: " + e.computador());
        }
        Computador destino = e.computador() == 0 ? menosCargado() : computadores[e.computador() - 1];
        PCB pcb = new PCB(ids.siguiente(), e.nombre(), destino.getId(), e.tipo(), e.prioridad(),
                e.memoria(), e.instrucciones(), e.deadline(), reloj.getCiclo());
        if (e.tipo() == TipoProceso.IO_BOUND) {
            pcb.configurarES(e.esCada(), e.duracionES());
        }
        if (buffer != null) {
            pcb.configurarBuffer(buffer, e.cadaCuantos(), e.elementos());
        }
        if (e.computador() == 0) {
            log.registrar("AvilaOS asigna " + pcb.getEtiqueta() + " al Computador " + destino.getId() + " (menos carga)");
        }
        candado.cerrar();
        try {
            procesosCreados.agregarAlFinal(e);
        } finally {
            candado.abrir();
        }
        destino.getOs().admitirProceso(pcb);
        return pcb;
    }

    /**
     * Balanceo simple: el computador con menos procesos vivos; si empatan,
     * el de mas RAM libre.
     */
    private Computador menosCargado() {
        Computador mejor = computadores[0];
        for (Computador c : computadores) {
            int cc = c.getOs().getCarga();
            int cm = mejor.getOs().getCarga();
            if (cc < cm || (cc == cm && c.getOs().getRamLibre() > mejor.getOs().getRamLibre())) {
                mejor = c;
            }
        }
        return mejor;
    }

    // ------------------------------------------------------------- buffers

    /** Crea un buffer. Lanza IllegalArgumentException si no se puede. */
    public BufferAcotado crearBuffer(EspecificacionBuffer e) {
        if (e.anfitrion() < 1 || e.anfitrion() > computadores.length) {
            throw new IllegalArgumentException("Anfitrion invalido: " + e.anfitrion());
        }
        if (buscarBuffer(e.nombre()) != null) {
            throw new IllegalArgumentException("Ya existe un buffer llamado " + e.nombre());
        }
        BufferAcotado b = new BufferAcotado(e.nombre(), e.capacidad(), e.anfitrion(), e.memoria());
        b.setEnrutador(this);
        if (!computadores[e.anfitrion() - 1].getOs().reservarBuffer(b)) {
            throw new IllegalArgumentException("No hay RAM en el Computador " + e.anfitrion());
        }
        candado.cerrar();
        try {
            buffers.agregarAlFinal(b);
            buffersCreados.agregarAlFinal(e);
        } finally {
            candado.abrir();
        }
        log.registrar("Buffer " + e.nombre() + " creado en el Computador " + e.anfitrion()
                + " (capacidad " + e.capacidad() + ")");
        return b;
    }

    public BufferAcotado buscarBuffer(String nombre) {
        candado.cerrar();
        try {
            Recorrido<BufferAcotado> r = buffers.recorrido();
            while (r.haySiguiente()) {
                BufferAcotado b = r.siguiente();
                if (b.getNombre().equalsIgnoreCase(nombre)) {
                    return b;
                }
            }
            return null;
        } finally {
            candado.abrir();
        }
    }

    /** Copia de la lista de buffers para recorrerla sin candado. */
    public ListaEnlazada<BufferAcotado> getBuffers() {
        return copiar(buffers);
    }

    public ListaEnlazada<EspecificacionProceso> getProcesosCreados() {
        return copiar(procesosCreados);
    }

    public ListaEnlazada<EspecificacionBuffer> getBuffersCreados() {
        return copiar(buffersCreados);
    }

    private <T> ListaEnlazada<T> copiar(ListaEnlazada<T> origen) {
        candado.cerrar();
        try {
            ListaEnlazada<T> c = new ListaEnlazada<>();
            Recorrido<T> r = origen.recorrido();
            while (r.haySiguiente()) {
                c.agregarAlFinal(r.siguiente());
            }
            return c;
        } finally {
            candado.abrir();
        }
    }

    /** Un signal desperto a un proceso: va al sistema operativo de su computador. */
    @Override
    public void despertar(PCB pcb, String motivo) {
        computadores[pcb.getIdComputador() - 1].getOs().pedirDespertar(pcb, motivo);
    }

    // ------------------------------------------------------------- metricas globales

    public double throughputGlobal() {
        long c = reloj.getCiclo();
        long hechos = 0;
        for (Computador comp : computadores) {
            hechos += comp.getOs().getEstadisticas().getCompletados();
        }
        return c == 0 ? 0 : (double) hechos / c;
    }

    public double utilizacionGlobal() {
        long ocupada = 0;
        long total = 0;
        for (Computador comp : computadores) {
            ocupada += comp.getCpu().getCiclosOcupada();
            total += comp.getCpu().getCiclosTotales();
        }
        return total == 0 ? 0 : (double) ocupada / total;
    }

    public double respuestaPromedioGlobal() {
        long suma = 0;
        long n = 0;
        for (Computador comp : computadores) {
            Estadisticas e = comp.getOs().getEstadisticas();
            suma += e.getSumaRespuesta();
            n += e.getConRespuesta();
        }
        return n == 0 ? 0 : (double) suma / n;
    }

    public double cumplimientoGlobal() {
        long ok = 0;
        long t = 0;
        for (Computador comp : computadores) {
            Estadisticas e = comp.getOs().getEstadisticas();
            ok += e.getCompletados();
            t += e.getTerminados();
        }
        return t == 0 ? 0 : (double) ok / t;
    }

    // ------------------------------------------------------------- getters

    public Computador[] getComputadores() { return computadores; }
    public RelojGlobal getReloj() { return reloj; }
    public RegistroEventos getLog() { return log; }
    public ParametrosSimulacion getParametros() { return parametros; }
}
