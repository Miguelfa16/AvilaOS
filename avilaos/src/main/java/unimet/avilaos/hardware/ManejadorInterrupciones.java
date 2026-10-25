package unimet.avilaos.hardware;

import unimet.avilaos.so.proceso.PCB;

/** Quien atiende las interrupciones de fin de E/S o de latencia de red (el nucleo). */
public interface ManejadorInterrupciones {
    void interrumpir(PCB pcb, String motivo);
}
