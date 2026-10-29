package unimet.avilaos.avilaos;

import unimet.avilaos.cluster.EspecificacionBuffer;
import unimet.avilaos.cluster.EspecificacionProceso;
import unimet.avilaos.estructuras.ListaEnlazada;
import unimet.avilaos.so.planificacion.TipoPolitica;

/** Todo lo que se guarda en el archivo para repetir una simulacion. */
public class Configuracion {

    public int duracionCiclo = 500;
    public int quantum = 4;
    public int latencia = 3;
    public int[] rams = {512, 512};
    public TipoPolitica[] politicas = {TipoPolitica.FCFS, TipoPolitica.FCFS};
    public final ListaEnlazada<EspecificacionBuffer> buffers = new ListaEnlazada<>();
    public final ListaEnlazada<EspecificacionProceso> procesos = new ListaEnlazada<>();
}
