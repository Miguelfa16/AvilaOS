package unimet.avilaos.so.proceso;


public class PCB {

    private final long id;
    private final String nombre;
    private final int idComputador;
    private final TipoProceso tipo;
    private final int prioridad;
    private final int memoria;
    private final int instruccionesTotales;
    private final int deadlineInicial;
    private final long cicloCreacion;

    private EstadoProceso estado = EstadoProceso.NUEVO;
    private MotivoBloqueo motivoBloqueo = MotivoBloqueo.NINGUNO;
    private int contadorPrograma = 0;
    private int tiempoRestante;
    private int deadlineRestante;
    private boolean terminadoPorDeadline = false;

    private long cicloPrimeraEjecucion = -1;
    private long cicloFinalizacion = -1;
    private long ciclosEnNuevo = 0;
    private long ciclosEnListo = 0;
    private long ciclosEnEjecucion = 0;
    private long ciclosBloqueado = 0;

    public PCB(long id, String nombre, int idComputador, TipoProceso tipo, int prioridad,
            int memoria, int instrucciones, int deadline, long cicloCreacion) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo no puede ser null");
        }
        if (idComputador < 0) {
            throw new IllegalArgumentException("Computador invalido: " + idComputador);
        }
        if (prioridad < 0) {
            throw new IllegalArgumentException("La prioridad no puede ser negativa");
        }
        if (memoria < 1) {
            throw new IllegalArgumentException("La memoria debe ser al menos 1");
        }
        if (instrucciones < 1) {
            throw new IllegalArgumentException("Las instrucciones deben ser al menos 1");
        }
        if (deadline < 1) {
            throw new IllegalArgumentException("El deadline debe ser al menos 1");
        }
        this.id = id;
        this.nombre = nombre.trim();
        this.idComputador = idComputador;
        this.tipo = tipo;
        this.prioridad = prioridad;
        this.memoria = memoria;
        this.instruccionesTotales = instrucciones;
        this.deadlineInicial = deadline;
        this.cicloCreacion = cicloCreacion;
        this.tiempoRestante = instrucciones;
        this.deadlineRestante = deadline;
    }

    public void cambiarEstado(EstadoProceso destino, long ciclo) {
        if (destino == EstadoProceso.BLOQUEADO) {
            throw new IllegalArgumentException("Para bloquear use bloquear(motivo, ciclo)");
        }
        transicionar(destino, ciclo);
    }

    public void bloquear(MotivoBloqueo motivo, long ciclo) {
        if (motivo == null || motivo == MotivoBloqueo.NINGUNO) {
            throw new IllegalArgumentException("Un proceso bloqueado necesita un motivo");
        }
        transicionar(EstadoProceso.BLOQUEADO, ciclo);
        this.motivoBloqueo = motivo;
    }

    public void terminarPorDeadline(long ciclo) {
        transicionar(EstadoProceso.TERMINADO, ciclo);
        this.terminadoPorDeadline = true;
    }

    private void transicionar(EstadoProceso destino, long ciclo) {
        if (!estado.puedePasarA(destino)) {
            throw new IllegalStateException("Transicion invalida en " + this + ": "
                    + estado + " -> " + destino);
        }
        if (destino == EstadoProceso.EJECUCION && cicloPrimeraEjecucion < 0) {
            cicloPrimeraEjecucion = ciclo;
        }
        if (destino == EstadoProceso.TERMINADO) {
            cicloFinalizacion = ciclo;
        }
        if (estado == EstadoProceso.BLOQUEADO) {
            motivoBloqueo = MotivoBloqueo.NINGUNO;
        }
        estado = destino;
    }

    public void ejecutarInstruccion() {
        if (estado != EstadoProceso.EJECUCION) {
            throw new IllegalStateException(this + " no esta en ejecucion");
        }
        contadorPrograma++;
        if (tiempoRestante > 0) {
            tiempoRestante--;
        }
    }

    public boolean descontarDeadline() {
        if (deadlineRestante > 0) {
            deadlineRestante--;
        }
        return deadlineRestante == 0;
    }

    public void registrarCicloTranscurrido() {
        switch (estado) {
            case NUEVO -> ciclosEnNuevo++;
            case LISTO -> ciclosEnListo++;
            case EJECUCION -> ciclosEnEjecucion++;
            case BLOQUEADO -> ciclosBloqueado++;
            case TERMINADO -> { }
        }
    }

    public boolean haTerminadoTrabajo() {
        return tiempoRestante == 0;
    }

    public boolean cumplioDeadline() {
        return estado == EstadoProceso.TERMINADO && !terminadoPorDeadline;
    }

    public long tiempoRespuesta() {
        return cicloPrimeraEjecucion < 0 ? -1 : cicloPrimeraEjecucion - cicloCreacion;
    }

    public long tiempoRetorno() {
        return cicloFinalizacion < 0 ? -1 : cicloFinalizacion - cicloCreacion;
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public int getIdComputador() { return idComputador; }
    public TipoProceso getTipo() { return tipo; }
    public int getPrioridad() { return prioridad; }
    public int getMemoria() { return memoria; }
    public int getInstruccionesTotales() { return instruccionesTotales; }
    public int getDeadlineInicial() { return deadlineInicial; }
    public long getCicloCreacion() { return cicloCreacion; }
    public EstadoProceso getEstado() { return estado; }
    public MotivoBloqueo getMotivoBloqueo() { return motivoBloqueo; }
    public int getContadorPrograma() { return contadorPrograma; }
    public int getTiempoRestante() { return tiempoRestante; }
    public int getDeadlineRestante() { return deadlineRestante; }
    public boolean isTerminadoPorDeadline() { return terminadoPorDeadline; }
    public long getCicloPrimeraEjecucion() { return cicloPrimeraEjecucion; }
    public long getCicloFinalizacion() { return cicloFinalizacion; }
    public long getCiclosEnNuevo() { return ciclosEnNuevo; }
    public long getCiclosEnListo() { return ciclosEnListo; }
    public long getCiclosEnEjecucion() { return ciclosEnEjecucion; }
    public long getCiclosBloqueado() { return ciclosBloqueado; }

    @Override
    public String toString() {
        return "P" + id + "[" + nombre + "]";
    }
}
