package unimet.avilaos.sincronizacion;

import unimet.avilaos.so.proceso.PCB;

/** Semaforo de conteo (vacios y llenos). Valor negativo = procesos esperando. */
public class SemaforoConteo extends SemaforoSO {

    public SemaforoConteo(String nombre, int valorInicial) {
        super(nombre, valorInicial);
    }

    @Override
    public boolean esperar(PCB pcb) {
        valor--;
        if (valor < 0) {
            cola.encolar(pcb);
            return true;
        }
        return false;
    }

    @Override
    public PCB senalar() {
        valor++;
        if (valor <= 0 && !cola.estaVacia()) {
            return cola.desencolar();
        }
        return null;
    }

    @Override
    public boolean quitar(PCB pcb) {
        if (cola.eliminar(pcb)) {
            valor++;
            return true;
        }
        return false;
    }
}
