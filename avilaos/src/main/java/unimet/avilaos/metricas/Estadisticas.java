package unimet.avilaos.metricas;

import unimet.avilaos.so.proceso.PCB;

/** Metricas de un computador. */
public class Estadisticas {

    private long completados;
    private long vencidos;
    private long sumaRespuesta;
    private long conRespuesta;
    private long sumaSemaforos;
    private long procesosBuffer;
    private double sumaEquidad;
    private double sumaEquidadCuadrado;
    private long nEquidad;

    private final Historial utilizacion = new Historial();

    /** Se llama cuando un proceso termina (bien o por deadline). */
    public void registrarFin(PCB p, long ciclo) {
        if (p.isTerminadoPorDeadline()) {
            vencidos++;
        } else {
            completados++;
        }
        long resp = p.tiempoRespuesta();
        if (resp >= 0) {
            sumaRespuesta += resp;
            conRespuesta++;
        }
        if (p.getTipo().usaBuffer()) {
            sumaSemaforos += p.getCiclosEnSemaforos();
            procesosBuffer++;
        }
        long enSistema = Math.max(1, ciclo - p.getCicloCreacion());
        double x = (double) p.getContadorPrograma() / enSistema;
        sumaEquidad += x;
        sumaEquidadCuadrado += x * x;
        nEquidad++;
    }

    public long getCompletados() { return completados; }
    public long getVencidos() { return vencidos; }
    public long getTerminados() { return completados + vencidos; }

    public double getRespuestaPromedio() {
        return conRespuesta == 0 ? 0 : (double) sumaRespuesta / conRespuesta;
    }

    public double getSemaforosPromedio() {
        return procesosBuffer == 0 ? 0 : (double) sumaSemaforos / procesosBuffer;
    }

    public double getCumplimientoDeadlines() {
        long t = getTerminados();
        return t == 0 ? 0 : (double) completados / t;
    }

    /**
     * Indice de equidad de Jain: (suma x)^2 / (n * suma x^2). 1 = todos
     * recibieron la misma proporcion de CPU por tiempo en el sistema.
     */
    public double getEquidad() {
        return nEquidad == 0 || sumaEquidadCuadrado == 0 ? 1
                : (sumaEquidad * sumaEquidad) / (nEquidad * sumaEquidadCuadrado);
    }

    public Historial getUtilizacion() { return utilizacion; }

    // usados por el cluster para el promedio global
    public long getSumaRespuesta() { return sumaRespuesta; }
    public long getConRespuesta() { return conRespuesta; }
}
