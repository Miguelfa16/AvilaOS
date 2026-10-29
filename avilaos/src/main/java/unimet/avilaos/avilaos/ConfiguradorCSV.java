package unimet.avilaos.avilaos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import unimet.avilaos.cluster.EspecificacionBuffer;
import unimet.avilaos.cluster.EspecificacionProceso;
import unimet.avilaos.estructuras.Recorrido;
import unimet.avilaos.so.planificacion.TipoPolitica;
import unimet.avilaos.so.proceso.TipoProceso;

/**
 * Guarda y carga la configuracion en CSV.
 * Los dos primeros metodos guardan solo los parametros globales en una linea.
 * guardar/cargar guardan todo (computadores, buffers y procesos). Cada linea empieza con su tipo:
 *   parametro,duracionCiclo,500
 *   computador,1,512,FCFS
 *   buffer,B1,5,1,5                      (nombre, capacidad, anfitrion, memoria)
 *   proceso,P,CPU_BOUND,50,32,5,300,0,0,0,,0,0
 *   (nombre, tipo, instrucciones, memoria, prioridad, deadline, computador,
 *    esCada, duracionES, buffer, cadaCuantos, elementos)
 */
public class ConfiguradorCSV {

    private final String rutaArchivo;

    public ConfiguradorCSV(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public void guardarParametrosGlobales(int duracionCiclo, int numComputadores, int ram, String politica, int quantum, int latencia) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(rutaArchivo))) {
            writer.println("DuracionCiclo,NumeroComputadores,RAM,Politica,Quantum,Latencia");
            writer.println(duracionCiclo + "," + numComputadores + "," + ram + "," + politica + "," + quantum + "," + latencia);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String[] cargarParametrosGlobales() {
        try (BufferedReader reader = new BufferedReader(new FileReader(rutaArchivo))) {
            reader.readLine();
            String linea = reader.readLine();
            if (linea != null) {
                return linea.split(",");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String[0];
    }

    // ---- configuracion completa ----

    public static void guardar(File archivo, Configuracion c) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(archivo.toPath(), StandardCharsets.UTF_8)) {
            w.write("# AvilaOS - configuracion\n");
            w.write("parametro,duracionCiclo," + c.duracionCiclo + "\n");
            w.write("parametro,quantum," + c.quantum + "\n");
            w.write("parametro,latencia," + c.latencia + "\n");
            for (int i = 0; i < c.rams.length; i++) {
                w.write("computador," + (i + 1) + "," + c.rams[i] + "," + c.politicas[i].name() + "\n");
            }
            Recorrido<EspecificacionBuffer> rb = c.buffers.recorrido();
            while (rb.haySiguiente()) {
                EspecificacionBuffer b = rb.siguiente();
                w.write("buffer," + b.nombre() + "," + b.capacidad() + "," + b.anfitrion() + "," + b.memoria() + "\n");
            }
            Recorrido<EspecificacionProceso> rp = c.procesos.recorrido();
            while (rp.haySiguiente()) {
                EspecificacionProceso p = rp.siguiente();
                w.write("proceso," + p.nombre() + "," + p.tipo().name() + "," + p.instrucciones() + ","
                        + p.memoria() + "," + p.prioridad() + "," + p.deadline() + "," + p.computador() + ","
                        + p.esCada() + "," + p.duracionES() + "," + (p.buffer() == null ? "" : p.buffer()) + ","
                        + p.cadaCuantos() + "," + p.elementos() + "\n");
            }
        }
    }

    /** Lee el archivo. Si una linea esta mala lanza IOException diciendo cual. */
    public static Configuracion cargar(File archivo) throws IOException {
        Configuracion c = new Configuracion();
        int nComputadores = 0;
        int[] rams = new int[16];
        TipoPolitica[] politicas = new TipoPolitica[16];
        int numLinea = 0;
        try (BufferedReader r = Files.newBufferedReader(archivo.toPath(), StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = r.readLine()) != null) {
                numLinea++;
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) {
                    continue;
                }
                String[] p = linea.split(",", -1);
                try {
                    switch (p[0]) {
                        case "parametro" -> {
                            int v = Integer.parseInt(p[2].trim());
                            switch (p[1]) {
                                case "duracionCiclo" -> c.duracionCiclo = v;
                                case "quantum" -> c.quantum = v;
                                case "latencia" -> c.latencia = v;
                                default -> throw new IllegalArgumentException("parametro desconocido " + p[1]);
                            }
                        }
                        case "computador" -> {
                            int id = Integer.parseInt(p[1].trim());
                            if (id < 1 || id > rams.length) {
                                throw new IllegalArgumentException("computador fuera de rango");
                            }
                            rams[id - 1] = Integer.parseInt(p[2].trim());
                            politicas[id - 1] = TipoPolitica.valueOf(p[3].trim());
                            nComputadores = Math.max(nComputadores, id);
                        }
                        case "buffer" -> c.buffers.agregarAlFinal(new EspecificacionBuffer(p[1].trim(),
                                Integer.parseInt(p[2].trim()), Integer.parseInt(p[3].trim()), Integer.parseInt(p[4].trim())));
                        case "proceso" -> c.procesos.agregarAlFinal(new EspecificacionProceso(p[1].trim(),
                                TipoProceso.valueOf(p[2].trim()), entero(p[3]), entero(p[4]), entero(p[5]), entero(p[6]),
                                entero(p[7]), entero(p[8]), entero(p[9]), p[10].trim().isEmpty() ? null : p[10].trim(),
                                entero(p[11]), entero(p[12])));
                        default -> throw new IllegalArgumentException("tipo de linea desconocido " + p[0]);
                    }
                } catch (RuntimeException ex) {
                    throw new IOException("Linea " + numLinea + " invalida: " + ex.getMessage());
                }
            }
        }
        if (nComputadores < 2) {
            throw new IOException("El archivo debe tener al menos 2 computadores");
        }
        c.rams = new int[nComputadores];
        c.politicas = new TipoPolitica[nComputadores];
        for (int i = 0; i < nComputadores; i++) {
            if (politicas[i] == null) {
                throw new IOException("Falta el computador " + (i + 1));
            }
            c.rams[i] = rams[i];
            c.politicas[i] = politicas[i];
        }
        return c;
    }

    private static int entero(String s) {
        return Integer.parseInt(s.trim());
    }
}
