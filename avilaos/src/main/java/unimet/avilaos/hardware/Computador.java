package unimet.avilaos.hardware;

import java.util.concurrent.Semaphore;
import unimet.avilaos.so.ParametrosSimulacion;
import unimet.avilaos.so.RegistroEventos;
import unimet.avilaos.so.SistemaOperativo;
import unimet.avilaos.so.planificacion.PoliticaPlanificacion;

/**
 * Un computador: CPU + RAM + sistema operativo, en su propio hilo.
 * Todos son instancias de esta clase; agregar uno mas es otro new.
 */
public class Computador implements ComponenteDeReloj, RelojGlobal.Participante, Runnable {

    private final int id;
    private final MemoriaPrincipal ram;
    private final CPU cpu;
    private final SistemaOperativo os;

    private final Semaphore inicioCiclo = new Semaphore(0);
    private RelojGlobal reloj;

    public Computador(int id, int capacidadRam, PoliticaPlanificacion politica,
            ParametrosSimulacion parametros, RegistroEventos log) {
        this.id = id;
        this.ram = new MemoriaPrincipal(capacidadRam);
        this.cpu = new CPU();
        this.os = new SistemaOperativo(id, this.ram, this.cpu, politica, parametros, log);
    }

    public int getId() {
        return id;
    }

    public MemoriaPrincipal getRam() {
        return ram;
    }

    public CPU getCpu() {
        return cpu;
    }

    public SistemaOperativo getOs() {
        return os;
    }

    /** Lo conecta al reloj global y arranca su hilo. */
    public void conectar(RelojGlobal reloj) {
        this.reloj = reloj;
        Thread hilo = new Thread(this, "Computador-" + id);
        hilo.setDaemon(true);
        hilo.start();
    }

    @Override
    public Semaphore inicioCiclo() {
        return inicioCiclo;
    }

    @Override
    public void run() {
        while (true) {
            try {
                inicioCiclo.acquire();
            } catch (InterruptedException e) {
                return;
            }
            try {
                alTick(reloj.getCiclo());
            } catch (RuntimeException ex) {
                ex.printStackTrace(); // que un error no tranque el reloj
            } finally {
                reloj.finCiclo().release();
            }
        }
    }

    /** Un ciclo: E/S -> el SO decide -> la CPU ejecuta -> el SO aplica. */
    @Override
    public void alTick(long ciclo) {
        os.getES().alTick(ciclo);
        os.alTick(ciclo);
        cpu.alTick(ciclo);
        os.despuesDeEjecutar();
    }
}
