package unimet.avilaos.cluster;

import unimet.avilaos.so.proceso.TipoProceso;

/**
 * Lo que el usuario llena al crear un proceso (o lo que viene del CSV).
 * computador = 0 significa que AvilaOS lo asigna solo.
 */
public record EspecificacionProceso(
        String nombre, TipoProceso tipo, int instrucciones, int memoria, int prioridad, int deadline,
        int computador, int esCada, int duracionES, String buffer, int cadaCuantos, int elementos) {
}
