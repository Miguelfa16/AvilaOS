package unimet.avilaos.avilaos;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

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
}
