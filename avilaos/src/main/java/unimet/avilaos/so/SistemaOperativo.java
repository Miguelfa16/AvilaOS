package unimet.avilaos.so;

import unimet.avilaos.estructuras.Cola;
import unimet.avilaos.estructuras.ListaEnlazada;
import unimet.avilaos.estructuras.ListaOrdenada;
import unimet.avilaos.estructuras.Recorrido;
import unimet.avilaos.hardware.CPU;
import unimet.avilaos.hardware.ComponenteDeReloj;
import unimet.avilaos.hardware.DispositivoES;
import unimet.avilaos.hardware.ManejadorInterrupciones;
import unimet.avilaos.hardware.MemoriaPrincipal;
import unimet.avilaos.hardware.ModoEjecucion;
import unimet.avilaos.metricas.Estadisticas;
import unimet.avilaos.sincronizacion.BufferAcotado;
import unimet.avilaos.sincronizacion.Mutex;
import unimet.avilaos.sincronizacion.SemaforoSO;
import unimet.avilaos.so.planificacion.FabricaPoliticas;
import unimet.avilaos.so.planificacion.PoliticaPlanificacion;
import unimet.avilaos.so.planificacion.PoliticaRoundRobin;
import unimet.avilaos.so.planificacion.TipoPolitica;
import unimet.avilaos.so.proceso.EstadoProceso;
import unimet.avilaos.so.proceso.FaseProductorConsumidor;
import unimet.avilaos.so.proceso.MotivoBloqueo;
import unimet.avilaos.so.proceso.PCB;
import unimet.avilaos.so.proceso.TipoProceso;

/**
 * Sistema operativo de un computador: admision por memoria (largo plazo),
 * planificacion (corto plazo), bloqueos, deadlines y productor-consumidor.
 *
 * Hilos: "candado" protege las colas, la CPU y la RAM. Los despertares que
 * vienen de otro computador (un signal remoto) o del controlador de E/S entran
 * a un buzon con su propio candado y se atienden al inicio del siguiente
 * ciclo, asi nunca se toman los candados de dos computadores a la vez.
 * Orden de candados: sistema operativo -> buffer -> buzon.
 */
public class SistemaOperativo implements ComponenteDeReloj, ManejadorInterrupciones {

    private static final class Aviso {
        final PCB pcb;
        final String motivo;

        Aviso(PCB pcb, String motivo) {
            this.pcb = pcb;
            this.motivo = motivo;
        }
    }

    private Cola<PCB> nuevos = new Cola<>();
    private ListaOrdenada<PCB> listos;
    private final Cola<PCB> bloqueados = new Cola<>();
    private final Cola<PCB> terminados = new Cola<>();
    private final MemoriaPrincipal ram;
    private final CPU cpu;
    private PoliticaPlanificacion politica;

    private final int computador;
    private final DispositivoES es;
    private final ParametrosSimulacion parametros;
    private final RegistroEventos log;
    private final Estadisticas estadisticas = new Estadisticas();
    private final int[] ventanaUso = new int[20];

    private final Mutex candado = new Mutex();
    private final Mutex candadoBuzon = new Mutex();
    private Cola<Aviso> buzon = new Cola<>();

    private MotivoBloqueo bloqueoPendiente = MotivoBloqueo.NINGUNO;
    private int ciclosBloqueoPendiente;
    private long ciclo;

    public SistemaOperativo(int computador, MemoriaPrincipal ram, CPU cpu, PoliticaPlanificacion politica,
            ParametrosSimulacion parametros, RegistroEventos log) {
        this.computador = computador;
        this.ram = ram;
        this.cpu = cpu;
        this.politica = politica;
        this.listos = new ListaOrdenada<>(politica.getComparador());
        this.parametros = parametros;
        this.log = log;
        this.es = new DispositivoES(this);
    }

    // =================================================================
    //  Llamadas desde la GUI o el cluster (otros hilos)
    // =================================================================

    public void cambiarPolitica(PoliticaPlanificacion nuevaPolitica) {
        candado.cerrar();
        try {
            this.politica = nuevaPolitica;
            this.listos.cambiarComparador(nuevaPolitica.getComparador());
            log.registrar("Computador " + computador + " cambia su politica a " + nuevaPolitica.getNombre());
        } finally {
            candado.abrir();
        }
    }

    public void cambiarPolitica(TipoPolitica tipo) {
        if (getTipoPolitica() != tipo) {
            cambiarPolitica(FabricaPoliticas.crear(tipo, parametros.getQuantum()));
        }
    }

    public TipoPolitica getTipoPolitica() {
        candado.cerrar();
        try {
            return politica.getTipo();
        } finally {
            candado.abrir();
        }
    }

    /** Llega un proceso nuevo a la cola de nuevos. */
    public void admitirProceso(PCB pcb) {
        candado.cerrar();
        try {
            nuevos.encolar(pcb);
            log.registrar("Computador " + computador + ": llega " + pcb.getEtiqueta()
                    + " (" + pcb.getNombre() + ", " + pcb.getTipo().getEtiqueta() + ")");
            revisarMemoria(ciclo);
        } finally {
            candado.abrir();
        }
    }

    /** Reserva la RAM de un buffer que vive en este computador. */
    public boolean reservarBuffer(BufferAcotado b) {
        candado.cerrar();
        try {
            if (!ram.hayEspacio(b.getMemoria())) {
                return false;
            }
            ram.asignar(b.getMemoria());
            return true;
        } finally {
            candado.abrir();
        }
    }

    /** Despertar por un semSignal (puede venir de otro computador). */
    public void pedirDespertar(PCB pcb, String motivo) {
        candadoBuzon.cerrar();
        try {
            buzon.encolar(new Aviso(pcb, motivo));
        } finally {
            candadoBuzon.abrir();
        }
    }

    /** Fin de E/S o de latencia de red. */
    @Override
    public void interrumpir(PCB pcb, String motivo) {
        pedirDespertar(pcb, motivo);
    }

    /** Procesos vivos en este computador (para la asignacion automatica). */
    public int getCarga() {
        candado.cerrar();
        try {
            return nuevos.tamano() + listos.tamano() + bloqueados.tamano() + (cpu.getActual() != null ? 1 : 0);
        } finally {
            candado.abrir();
        }
    }

    public int getRamLibre() {
        candado.cerrar();
        try {
            return ram.getLibre();
        } finally {
            candado.abrir();
        }
    }

    // =================================================================
    //  Ciclo de reloj
    // =================================================================

    @Override
    public void alTick(long ciclo) {
        candado.cerrar();
        try {
            this.ciclo = ciclo;
            bloqueoPendiente = MotivoBloqueo.NINGUNO;
            actualizarQuantum();
            atenderBuzon();
            revisarDeadlines(ciclo);
            revisarMemoria(ciclo);
            planificar(ciclo);
        } finally {
            candado.abrir();
        }
    }

    /** Despues de que la CPU ejecuto el ciclo: aplica bloqueos o terminaciones. */
    public void despuesDeEjecutar() {
        candado.cerrar();
        try {
            PCB activo = cpu.getActual();
            if (activo != null && bloqueoPendiente != MotivoBloqueo.NINGUNO) {
                bloquear(activo, bloqueoPendiente, ciclosBloqueoPendiente);
            } else if (activo != null && !cpu.esDespacho() && activo.puedeTerminar()) {
                terminar(activo, false);
            }
            actualizarCiclos();
            medirUso();
        } finally {
            candado.abrir();
        }
    }

    private void actualizarQuantum() {
        if (politica instanceof PoliticaRoundRobin rr && rr.getQuantum() != parametros.getQuantum()) {
            rr.setQuantum(parametros.getQuantum());
        }
    }

    private void atenderBuzon() {
        Cola<Aviso> avisos;
        candadoBuzon.cerrar();
        try {
            avisos = buzon;
            buzon = new Cola<>();
        } finally {
            candadoBuzon.abrir();
        }
        boolean hubo = false;
        while (!avisos.estaVacia()) {
            Aviso a = avisos.desencolar();
            PCB p = a.pcb;
            if (p.getEstado() != EstadoProceso.BLOQUEADO) {
                continue; // por ejemplo, ya murio por deadline
            }
            MotivoBloqueo m = p.getMotivoBloqueo();
            if (m == MotivoBloqueo.SEM_VACIOS || m == MotivoBloqueo.SEM_LLENOS || m == MotivoBloqueo.SEM_MUTEX) {
                p.sumarCiclosEnSemaforos(ciclo - p.getBloqueadoDesde());
            }
            bloqueados.eliminar(p);
            p.cambiarEstado(EstadoProceso.LISTO, ciclo);
            listos.insertar(p);
            hubo = true;
            log.registrar("Computador " + computador + ": " + p.getEtiqueta() + " se desbloquea (" + a.motivo + ")");
        }
        if (hubo) {
            listos.reordenar();
        }
    }

    private void revisarDeadlines(long ciclo) {
        ListaEnlazada<PCB> vencidos = new ListaEnlazada<>();
        revisarDeadlineEn(nuevos.recorrido(), vencidos);
        revisarDeadlineEn(listos.recorrido(), vencidos);
        revisarDeadlineEn(bloqueados.recorrido(), vencidos);
        PCB activo = cpu.getActual();
        if (activo != null && activo.descontarDeadline()) {
            vencidos.agregarAlFinal(activo);
        }
        Recorrido<PCB> r = vencidos.recorrido();
        while (r.haySiguiente()) {
            terminar(r.siguiente(), true);
        }
        listos.reordenar(); // en EDF el orden depende del deadline
    }

    private static void revisarDeadlineEn(Recorrido<PCB> r, ListaEnlazada<PCB> vencidos) {
        while (r.haySiguiente()) {
            PCB p = r.siguiente();
            if (p.descontarDeadline()) {
                vencidos.agregarAlFinal(p);
            }
        }
    }

    /** Largo plazo: pasa de Nuevo a Listo solo si cabe en la RAM. */
    private void revisarMemoria(long ciclo) {
        Cola<PCB> siguen = new Cola<>();
        while (!nuevos.estaVacia()) {
            PCB p = nuevos.desencolar();
            if (ram.hayEspacio(p.getMemoria())) {
                ram.asignar(p.getMemoria());
                p.cambiarEstado(EstadoProceso.LISTO, ciclo);
                listos.insertar(p);
                log.registrar("Computador " + computador + " admite " + p.getEtiqueta()
                        + " (RAM libre: " + ram.getLibre() + ")");
            } else {
                siguen.encolar(p);
            }
        }
        nuevos = siguen;
    }

    /** Corto plazo. El cambio de contexto gasta un ciclo en modo SO. */
    private void planificar(long ciclo) {
        PCB activo = cpu.getActual();
        if (activo != null && !listos.estaVacia()) {
            PCB candidato = listos.verPrimero();
            if (politica.debeExpropiar(activo, candidato, activo.getCiclosEnCpu())) {
                cpu.liberar();
                activo.cambiarEstado(EstadoProceso.LISTO, ciclo);
                listos.insertar(activo);
                log.registrar("Computador " + computador + " expulsa a " + activo.getEtiqueta()
                        + " (" + politica.getNombre() + ")");
                activo = null;
            }
        }
        if (activo == null) {
            if (listos.estaVacia()) {
                cpu.ociosa();
                return;
            }
            PCB nuevoActivo = listos.extraerPrimero();
            nuevoActivo.cambiarEstado(EstadoProceso.EJECUCION, ciclo);
            nuevoActivo.reiniciarCiclosEnCpu();
            cpu.cargar(nuevoActivo, ModoEjecucion.SISTEMA_OPERATIVO, "despacho", false);
            log.registrar("Computador " + computador + " selecciona el proceso " + nuevoActivo.getEtiqueta());
            return;
        }
        ejecutar(activo);
    }

    /** Decide que instruccion corre el proceso en este ciclo. */
    private void ejecutar(PCB p) {
        switch (p.getTipo()) {
            case CPU_BOUND -> usuario(p);
            case IO_BOUND -> {
                if (p.getEsCada() > 0 && p.getDesdeUltimaES() >= p.getEsCada()) {
                    p.setDesdeUltimaES(0);
                    sistema(p, "llamada de E/S");
                    bloqueoPendiente = MotivoBloqueo.ENTRADA_SALIDA;
                    ciclosBloqueoPendiente = p.getDuracionES();
                } else {
                    p.setDesdeUltimaES(p.getDesdeUltimaES() + 1);
                    usuario(p);
                }
            }
            case PRODUCTOR, CONSUMIDOR -> productorConsumidor(p);
        }
    }

    /**
     * Productor-consumidor con buffer acotado (Stallings cap. 5).
     * Cada semWait, semSignal, insertar o extraer es 1 instruccion en modo SO.
     */
    private void productorConsumidor(PCB p) {
        BufferAcotado b = p.getBuffer();
        boolean productor = p.getTipo() == TipoProceso.PRODUCTOR;
        boolean remoto = !p.esLocalA(computador) && parametros.getLatenciaRed() > 0;

        if (p.getFase() == FaseProductorConsumidor.ACCESO_RED && !remoto) {
            p.setFase(productor ? FaseProductorConsumidor.ESPERAR_VACIOS : FaseProductorConsumidor.ESPERAR_LLENOS);
        }

        switch (p.getFase()) {
            case PRODUCIR, CONSUMIR -> {
                usuario(p);
                p.setContadorFase(p.getContadorFase() + 1);
                if (p.getContadorFase() >= p.getCadaCuantosCiclos()) {
                    p.setContadorFase(0);
                    p.setFase(FaseProductorConsumidor.ACCESO_RED);
                }
            }
            case ACCESO_RED -> {
                sistema(p, "acceso remoto a " + b.getNombre());
                bloqueoPendiente = MotivoBloqueo.ACCESO_REMOTO;
                ciclosBloqueoPendiente = parametros.getLatenciaRed();
                p.setFase(productor ? FaseProductorConsumidor.ESPERAR_VACIOS : FaseProductorConsumidor.ESPERAR_LLENOS);
                log.registrar("Proceso " + p.getEtiqueta() + " accede al buffer " + b.getNombre() + " de forma remota");
            }
            case ESPERAR_VACIOS -> semWait(p, b, b.getVacios(), MotivoBloqueo.SEM_VACIOS);
            case ESPERAR_LLENOS -> semWait(p, b, b.getLlenos(), MotivoBloqueo.SEM_LLENOS);
            case ESPERAR_MUTEX -> {
                semWait(p, b, b.getMutex(), MotivoBloqueo.SEM_MUTEX);
                p.setTieneMutex(true); // si se bloquea, lo recibe al despertar
            }
            case INSERTAR -> {
                sistema(p, "insertar en " + b.getNombre());
                b.cerrar();
                try {
                    b.insertar(p);
                } finally {
                    b.abrir();
                }
                p.setReservoHueco(false);
                p.sumarElemento();
                p.setFase(FaseProductorConsumidor.SENALAR_MUTEX);
                log.registrar("Proceso " + p.getEtiqueta() + " produce un elemento en " + b.getNombre());
            }
            case EXTRAER -> {
                sistema(p, "extraer de " + b.getNombre());
                b.cerrar();
                try {
                    b.extraer();
                } finally {
                    b.abrir();
                }
                p.setReservoHueco(false);
                p.sumarElemento();
                p.setFase(FaseProductorConsumidor.SENALAR_MUTEX);
                log.registrar("Proceso " + p.getEtiqueta() + " consume un elemento de " + b.getNombre());
            }
            case SENALAR_MUTEX -> {
                semSignal(p, b, b.getMutex());
                p.setTieneMutex(false);
                p.setFase(productor ? FaseProductorConsumidor.SENALAR_LLENOS : FaseProductorConsumidor.SENALAR_VACIOS);
            }
            case SENALAR_LLENOS -> {
                semSignal(p, b, b.getLlenos());
                p.setFase(FaseProductorConsumidor.PRODUCIR);
            }
            case SENALAR_VACIOS -> {
                semSignal(p, b, b.getVacios());
                p.setFase(FaseProductorConsumidor.CONSUMIR);
            }
        }
    }

    private void semWait(PCB p, BufferAcotado b, SemaforoSO s, MotivoBloqueo motivo) {
        sistema(p, "semWait(" + s.getNombre() + ") " + b.getNombre());
        boolean seBloquea;
        b.cerrar();
        try {
            seBloquea = s.esperar(p);
        } finally {
            b.abrir();
        }
        boolean productor = p.getTipo() == TipoProceso.PRODUCTOR;
        if (motivo == MotivoBloqueo.SEM_MUTEX) {
            p.setFase(productor ? FaseProductorConsumidor.INSERTAR : FaseProductorConsumidor.EXTRAER);
        } else {
            p.setReservoHueco(true);
            p.setFase(FaseProductorConsumidor.ESPERAR_MUTEX);
        }
        if (seBloquea) {
            bloqueoPendiente = motivo;
            log.registrar("Proceso " + p.getEtiqueta() + " se bloquea en el semaforo '" + s.getNombre()
                    + "' del buffer " + b.getNombre());
        }
    }

    private void semSignal(PCB p, BufferAcotado b, SemaforoSO s) {
        sistema(p, "semSignal(" + s.getNombre() + ") " + b.getNombre());
        b.cerrar();
        try {
            b.senalar(s);
        } finally {
            b.abrir();
        }
    }

    private void usuario(PCB p) {
        cpu.cargar(p, ModoEjecucion.USUARIO, "instruccion @" + p.getContadorPrograma(), true);
    }

    private void sistema(PCB p, String que) {
        cpu.cargar(p, ModoEjecucion.SISTEMA_OPERATIVO, que, true);
    }

    private void bloquear(PCB p, MotivoBloqueo motivo, int ciclos) {
        cpu.liberar();
        p.bloquear(motivo, ciclo);
        p.setBloqueadoDesde(ciclo);
        bloqueados.encolar(p);
        if (motivo == MotivoBloqueo.ENTRADA_SALIDA || motivo == MotivoBloqueo.ACCESO_REMOTO) {
            es.solicitar(p, ciclos, motivo == MotivoBloqueo.ENTRADA_SALIDA ? "fin de E/S" : "fin de latencia de red");
        }
        if (motivo == MotivoBloqueo.ENTRADA_SALIDA) {
            log.registrar("Computador " + computador + ": " + p.getEtiqueta() + " se bloquea por E/S (" + ciclos + " ciclos)");
        }
    }

    /** Termina un proceso (normal o por deadline) y libera lo que tenga. */
    private void terminar(PCB p, boolean porDeadline) {
        boolean teniaRam = p.getEstado() != EstadoProceso.NUEVO;
        if (cpu.getActual() == p) {
            cpu.liberar();
        }
        nuevos.eliminar(p);
        listos.eliminar(p);
        bloqueados.eliminar(p);
        es.cancelar(p);
        if (p.getBuffer() != null) {
            liberarBuffer(p);
        }
        if (teniaRam) {
            ram.liberar(p.getMemoria());
        }
        if (porDeadline) {
            p.terminarPorDeadline(ciclo);
        } else {
            p.cambiarEstado(EstadoProceso.TERMINADO, ciclo);
        }
        terminados.encolar(p);
        estadisticas.registrarFin(p, ciclo);
        log.registrar("Computador " + computador + ": " + p.getEtiqueta()
                + (porDeadline ? " TERMINADO por deadline vencido" : " termina"));
        revisarMemoria(ciclo); // se libero RAM
    }

    /** Si muere a mitad del protocolo devuelve mutex, hueco o signal pendiente. */
    private void liberarBuffer(PCB p) {
        BufferAcotado b = p.getBuffer();
        boolean productor = p.getTipo() == TipoProceso.PRODUCTOR;
        b.cerrar();
        try {
            boolean esperabaHueco = productor ? b.getVacios().quitar(p) : b.getLlenos().quitar(p);
            boolean esperabaMutex = b.getMutex().quitar(p);
            if (p.tieneMutex() && !esperabaMutex) {
                b.senalar(b.getMutex());
            }
            if (p.reservoHueco() && !esperabaHueco) {
                b.senalar(productor ? b.getVacios() : b.getLlenos());
            }
            FaseProductorConsumidor f = p.getFase();
            if (f == FaseProductorConsumidor.SENALAR_MUTEX || f == FaseProductorConsumidor.SENALAR_LLENOS
                    || f == FaseProductorConsumidor.SENALAR_VACIOS) {
                b.senalar(productor ? b.getLlenos() : b.getVacios());
            }
        } finally {
            b.abrir();
        }
        p.setTieneMutex(false);
        p.setReservoHueco(false);
    }

    private void actualizarCiclos() {
        actualizarCiclosEn(nuevos.recorrido());
        actualizarCiclosEn(listos.recorrido());
        actualizarCiclosEn(bloqueados.recorrido());
        PCB activo = cpu.getActual();
        if (activo != null) {
            activo.registrarCicloTranscurrido();
        }
    }

    private static void actualizarCiclosEn(Recorrido<PCB> r) {
        while (r.haySiguiente()) {
            r.siguiente().registrarCicloTranscurrido();
        }
    }

    private void medirUso() {
        ventanaUso[(int) (ciclo % ventanaUso.length)] = cpu.getModo() == ModoEjecucion.OCIOSO ? 0 : 1;
        int n = (int) Math.min(ciclo, ventanaUso.length);
        int suma = 0;
        for (int i = 0; i < n; i++) {
            suma += ventanaUso[i];
        }
        estadisticas.getUtilizacion().agregar(n == 0 ? 0 : (double) suma / n);
    }

    // =================================================================
    //  Foto para la GUI (la GUI solo mira)
    // =================================================================

    public FotoSistema foto() {
        candado.cerrar();
        try {
            FotoSistema f = new FotoSistema();
            f.computador = computador;
            f.politica = politica.getTipo();
            f.modo = cpu.getModo();
            f.instruccion = cpu.getInstruccion();
            PCB p = cpu.getActual();
            f.enCpu = p == null ? null : FotoSistema.fila(p);
            f.pcEnCpu = p == null ? -1 : p.getContadorPrograma();
            f.memUsada = ram.getUsada();
            f.memTotal = ram.getCapacidad();
            f.nuevos = filas(nuevos.recorrido(), nuevos.tamano());
            f.listos = filas(listos.recorrido(), listos.tamano());
            f.bloqueados = filas(bloqueados.recorrido(), bloqueados.tamano());
            f.terminados = filas(terminados.recorrido(), terminados.tamano());
            return f;
        } finally {
            candado.abrir();
        }
    }

    private static Object[][] filas(Recorrido<PCB> r, int n) {
        Object[][] out = new Object[n][];
        int i = 0;
        while (r.haySiguiente()) {
            out[i++] = FotoSistema.fila(r.siguiente());
        }
        return out;
    }

    public DispositivoES getES() { return es; }
    public Estadisticas getEstadisticas() { return estadisticas; }

    public Cola<PCB> getNuevos() { return nuevos; }
    public ListaOrdenada<PCB> getListos() { return listos; }
    public Cola<PCB> getBloqueados() { return bloqueados; }
    public Cola<PCB> getTerminados() { return terminados; }
}
