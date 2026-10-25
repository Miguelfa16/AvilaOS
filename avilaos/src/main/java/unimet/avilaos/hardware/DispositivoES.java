package unimet.avilaos.hardware;

import unimet.avilaos.estructuras.ListaEnlazada;
import unimet.avilaos.estructuras.Recorrido;
import unimet.avilaos.so.proceso.PCB;

/**
 * Controlador de E/S tipo DMA. Lleva los temporizadores de E/S y de
 * latencia de red y al terminar interrumpe al nucleo.
 */
public class DispositivoES implements ComponenteDeReloj {

    private static final class Solicitud {
        final PCB pcb;
        final String motivo;
        int restante;

        Solicitud(PCB pcb, int ciclos, String motivo) {
            this.pcb = pcb;
            this.restante = Math.max(1, ciclos);
            this.motivo = motivo;
        }
    }

    private final ListaEnlazada<Solicitud> pendientes = new ListaEnlazada<>();
    private final ManejadorInterrupciones manejador;

    public DispositivoES(ManejadorInterrupciones manejador) {
        this.manejador = manejador;
    }

    public void solicitar(PCB pcb, int ciclos, String motivo) {
        pendientes.agregarAlFinal(new Solicitud(pcb, ciclos, motivo));
    }

    public void cancelar(PCB pcb) {
        ListaEnlazada<Solicitud> quitar = new ListaEnlazada<>();
        Recorrido<Solicitud> r = pendientes.recorrido();
        while (r.haySiguiente()) {
            Solicitud s = r.siguiente();
            if (s.pcb == pcb) {
                quitar.agregarAlFinal(s);
            }
        }
        Recorrido<Solicitud> q = quitar.recorrido();
        while (q.haySiguiente()) {
            pendientes.eliminar(q.siguiente());
        }
    }

    @Override
    public void alTick(long ciclo) {
        ListaEnlazada<Solicitud> listas = new ListaEnlazada<>();
        Recorrido<Solicitud> r = pendientes.recorrido();
        while (r.haySiguiente()) {
            Solicitud s = r.siguiente();
            s.restante--;
            if (s.restante <= 0) {
                listas.agregarAlFinal(s);
            }
        }
        Recorrido<Solicitud> l = listas.recorrido();
        while (l.haySiguiente()) {
            Solicitud s = l.siguiente();
            pendientes.eliminar(s);
            manejador.interrumpir(s.pcb, s.motivo);
        }
    }
}
