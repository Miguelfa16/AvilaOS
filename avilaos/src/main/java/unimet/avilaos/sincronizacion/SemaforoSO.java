package unimet.avilaos.sincronizacion;

import unimet.avilaos.estructuras.Cola;
import unimet.avilaos.estructuras.Recorrido;
import unimet.avilaos.so.proceso.PCB;

/**
 * Semaforo de AvilaOS: valor + cola de procesos bloqueados.
 * Lo protege el Mutex del buffer al que pertenece.
 */
public abstract class SemaforoSO {

    protected final String nombre;
    protected int valor;
    protected final Cola<PCB> cola = new Cola<>();

    protected SemaforoSO(String nombre, int valorInicial) {
        this.nombre = nombre;
        this.valor = valorInicial;
    }

    /** semWait. true si el proceso se tiene que bloquear (ya quedo en la cola). */
    public abstract boolean esperar(PCB pcb);

    /** semSignal. Devuelve el proceso a despertar o null. */
    public abstract PCB senalar();

    /** Saca un proceso de la cola (por ejemplo si murio por deadline). */
    public abstract boolean quitar(PCB pcb);

    public String getNombre() { return nombre; }
    public int getValor() { return valor; }

    public String colaComoTexto() {
        StringBuilder sb = new StringBuilder();
        Recorrido<PCB> r = cola.recorrido();
        while (r.haySiguiente()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(r.siguiente().getEtiqueta());
        }
        return sb.length() == 0 ? "-" : sb.toString();
    }
}
