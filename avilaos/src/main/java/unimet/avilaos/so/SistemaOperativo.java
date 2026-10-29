package unimet.avilaos.so;

import unimet.avilaos.estructuras.Cola;
import unimet.avilaos.estructuras.ListaOrdenada;
import unimet.avilaos.estructuras.Recorrido;
import unimet.avilaos.hardware.CPU;
import unimet.avilaos.hardware.ComponenteDeReloj;
import unimet.avilaos.hardware.MemoriaPrincipal;
import unimet.avilaos.hardware.ModoEjecucion;
import unimet.avilaos.so.planificacion.PoliticaPlanificacion;
import unimet.avilaos.so.proceso.EstadoProceso;
import unimet.avilaos.so.proceso.PCB;

public class SistemaOperativo implements ComponenteDeReloj {

    private final Cola<PCB> nuevos = new Cola<>();
    private ListaOrdenada<PCB> listos;
    private final Cola<PCB> bloqueados = new Cola<>();
    private final Cola<PCB> terminados = new Cola<>();
    private final MemoriaPrincipal ram;
    private final CPU cpu;
    private PoliticaPlanificacion politica;
    private int ciclosEnCpuActivo = 0;

    public SistemaOperativo(MemoriaPrincipal ram, CPU cpu, PoliticaPlanificacion politica) {
        this.ram = ram;
        this.cpu = cpu;
        this.politica = politica;
        this.listos = new ListaOrdenada<>(politica.getComparador());
    }

    public void cambiarPolitica(PoliticaPlanificacion nuevaPolitica) {
        this.politica = nuevaPolitica;
        this.listos.cambiarComparador(nuevaPolitica.getComparador());
    }

    public void admitirProceso(PCB pcb) {
        nuevos.encolar(pcb);
    }

    @Override
    public void alTick(long ciclo) {
        revisarDeadlines(ciclo);
        revisarMemoria(ciclo);
        planificar(ciclo);
        actualizarCiclos();
        if (cpu.getActual() != null) {
            ciclosEnCpuActivo++;
        }
    }

    private void revisarDeadlines(long ciclo) {
        revisarDeadlineEnCola(nuevos, ciclo);
        revisarDeadlineEnListaOrdenada(listos, ciclo);
        revisarDeadlineEnCola(bloqueados, ciclo);
        
        PCB activo = cpu.getActual();
        if (activo != null) {
            if (activo.descontarDeadline()) {
                cpu.ociosa();
                activo.terminarPorDeadline(ciclo);
                ram.liberar(activo.getMemoria());
                terminados.encolar(activo);
                ciclosEnCpuActivo = 0;
            } else if (activo.haTerminadoTrabajo()) {
                cpu.ociosa();
                activo.cambiarEstado(EstadoProceso.TERMINADO, ciclo);
                ram.liberar(activo.getMemoria());
                terminados.encolar(activo);
                ciclosEnCpuActivo = 0;
            }
        }
    }

    private void revisarDeadlineEnCola(Cola<PCB> cola, long ciclo) {
        int tamano = cola.tamano();
        for (int i = 0; i < tamano; i++) {
            PCB p = cola.desencolar();
            if (p.descontarDeadline()) {
                if (p.getEstado() != EstadoProceso.NUEVO) {
                    ram.liberar(p.getMemoria());
                }
                p.terminarPorDeadline(ciclo);
                terminados.encolar(p);
            } else {
                cola.encolar(p);
            }
        }
    }

    private void revisarDeadlineEnListaOrdenada(ListaOrdenada<PCB> lista, long ciclo) {
        Cola<PCB> temporales = new Cola<>();
        Recorrido<PCB> r = lista.recorrido();
        while (r.haySiguiente()) {
            temporales.encolar(r.siguiente());
        }
        lista.limpiar();
        while (!temporales.estaVacia()) {
            PCB p = temporales.desencolar();
            if (p.descontarDeadline()) {
                ram.liberar(p.getMemoria());
                p.terminarPorDeadline(ciclo);
                terminados.encolar(p);
            } else {
                lista.insertar(p);
            }
        }
    }

    private void revisarMemoria(long ciclo) {
        int tamano = nuevos.tamano();
        for (int i = 0; i < tamano; i++) {
            PCB p = nuevos.desencolar();
            if (ram.hayEspacio(p.getMemoria())) {
                ram.asignar(p.getMemoria());
                p.cambiarEstado(EstadoProceso.LISTO, ciclo);
                listos.insertar(p);
            } else {
                nuevos.encolar(p);
            }
        }
    }

    private void planificar(long ciclo) {
        PCB activo = cpu.getActual();
        if (activo != null && !listos.estaVacia()) {
            PCB candidato = listos.verPrimero();
            if (politica.debeExpropiar(activo, candidato, ciclosEnCpuActivo)) {
                cpu.ociosa();
                activo.cambiarEstado(EstadoProceso.LISTO, ciclo);
                listos.insertar(activo);
                activo = null;
                ciclosEnCpuActivo = 0;
            }
        }
        if (activo == null && !listos.estaVacia()) {
            PCB nuevoActivo = listos.extraerPrimero();
            nuevoActivo.cambiarEstado(EstadoProceso.EJECUCION, ciclo);
            cpu.cargar(nuevoActivo, ModoEjecucion.USUARIO, "Ejecutando", true);
            ciclosEnCpuActivo = 0;
        }
    }

    private void actualizarCiclos() {
        actualizarCiclosCola(nuevos);
        actualizarCiclosLista(listos);
        actualizarCiclosCola(bloqueados);
        PCB activo = cpu.getActual();
        if (activo != null) {
            activo.registrarCicloTranscurrido();
        }
    }

    private void actualizarCiclosCola(Cola<PCB> cola) {
        Recorrido<PCB> r = cola.recorrido();
        while (r.haySiguiente()) {
            r.siguiente().registrarCicloTranscurrido();
        }
    }

    private void actualizarCiclosLista(ListaOrdenada<PCB> lista) {
        Recorrido<PCB> r = lista.recorrido();
        while (r.haySiguiente()) {
            r.siguiente().registrarCicloTranscurrido();
        }
    }

    public Cola<PCB> getNuevos() { return nuevos; }
    public ListaOrdenada<PCB> getListos() { return listos; }
    public Cola<PCB> getBloqueados() { return bloqueados; }
    public Cola<PCB> getTerminados() { return terminados; }
}