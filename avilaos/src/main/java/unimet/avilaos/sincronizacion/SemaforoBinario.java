package unimet.avilaos.sincronizacion;

import unimet.avilaos.so.proceso.PCB;

/** Semaforo binario (mutex de la region critica). Valor 0 o 1. */
public class SemaforoBinario extends SemaforoSO {

    public SemaforoBinario(String nombre) {
        super(nombre, 1);
    }

    @Override
    public boolean esperar(PCB pcb) {
        if (valor == 1) {
            valor = 0;
            return false;
        }
        cola.encolar(pcb);
        return true;
    }

    @Override
    public PCB senalar() {
        if (cola.estaVacia()) {
            valor = 1;
            return null;
        }
        return cola.desencolar(); // el mutex pasa directo al que despierta
    }

    @Override
    public boolean quitar(PCB pcb) {
        return cola.eliminar(pcb);
    }
}
