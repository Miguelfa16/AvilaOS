package unimet.avilaos.hardware;

import unimet.avilaos.so.SistemaOperativo;
import unimet.avilaos.so.planificacion.PoliticaPlanificacion;

public class Computador implements ComponenteDeReloj {

    private final int id;
    private final MemoriaPrincipal ram;
    private final CPU cpu;
    private final SistemaOperativo os;

    public Computador(int id, int capacidadRam, PoliticaPlanificacion politica) {
        this.id = id;
        this.ram = new MemoriaPrincipal(capacidadRam);
        this.cpu = new CPU();
        this.os = new SistemaOperativo(this.ram, this.cpu, politica);
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

    @Override
    public void alTick(long ciclo) {
        os.alTick(ciclo);
        cpu.alTick(ciclo);
    }
}