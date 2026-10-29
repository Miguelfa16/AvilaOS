package unimet.avilaos.metricas;

import java.util.concurrent.Semaphore;

/** Arreglo que crece, para guardar la utilizacion de la CPU en el tiempo. */
public class Historial {

    private final Semaphore mutex = new Semaphore(1);
    private double[] datos = new double[128];
    private int tamano;

    public void agregar(double v) {
        mutex.acquireUninterruptibly();
        try {
            if (tamano == datos.length) {
                double[] nuevo = new double[datos.length * 2];
                System.arraycopy(datos, 0, nuevo, 0, tamano);
                datos = nuevo;
            }
            datos[tamano++] = v;
        } finally {
            mutex.release();
        }
    }

    public double[] copia() {
        mutex.acquireUninterruptibly();
        try {
            double[] c = new double[tamano];
            System.arraycopy(datos, 0, c, 0, tamano);
            return c;
        } finally {
            mutex.release();
        }
    }
}
