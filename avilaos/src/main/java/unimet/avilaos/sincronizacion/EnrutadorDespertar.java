package unimet.avilaos.sincronizacion;

import unimet.avilaos.so.proceso.PCB;

/** Lleva un proceso despertado por un semSignal al nucleo de SU computador. */
public interface EnrutadorDespertar {
    void despertar(PCB pcb, String motivo);
}
