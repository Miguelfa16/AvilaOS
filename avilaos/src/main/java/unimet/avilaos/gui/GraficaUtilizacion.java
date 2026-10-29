package unimet.avilaos.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import unimet.avilaos.hardware.Computador;

/**
 * Una sola grafica con la utilizacion de la CPU de cada computador en el
 * tiempo (ventana movil de 20 ciclos). Dibujada con Java2D.
 */
public class GraficaUtilizacion extends JPanel {

    private static final Color[] COLORES = {
        new Color(0x1E88E5), new Color(0xE53935), new Color(0x43A047), new Color(0xFB8C00),
        new Color(0x8E24AA), new Color(0x00ACC1), new Color(0x6D4C41), new Color(0x546E7A)
    };

    private final Computador[] computadores;

    public GraficaUtilizacion(Computador[] computadores) {
        this.computadores = computadores;
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int izq = 50, der = 130, arr = 15, aba = 30;
        int ancho = getWidth() - izq - der;
        int alto = getHeight() - arr - aba;
        if (ancho < 20 || alto < 20) {
            return;
        }

        // ejes y lineas guia
        g.setColor(new Color(0xEEEEEE));
        for (int i = 0; i <= 4; i++) {
            int y = arr + alto - i * alto / 4;
            g.drawLine(izq, y, izq + ancho, y);
        }
        g.setColor(Color.DARK_GRAY);
        g.drawLine(izq, arr, izq, arr + alto);
        g.drawLine(izq, arr + alto, izq + ancho, arr + alto);
        for (int i = 0; i <= 4; i++) {
            int y = arr + alto - i * alto / 4;
            g.drawString((i * 25) + "%", 10, y + 4);
        }

        double[][] series = new double[computadores.length][];
        int maxCiclos = 1;
        for (int i = 0; i < computadores.length; i++) {
            series[i] = computadores[i].getOs().getEstadisticas().getUtilizacion().copia();
            maxCiclos = Math.max(maxCiclos, series[i].length);
        }
        g.drawString("ciclo 0", izq, arr + alto + 18);
        String fin = "ciclo " + maxCiclos;
        g.drawString(fin, izq + ancho - g.getFontMetrics().stringWidth(fin), arr + alto + 18);

        g.setStroke(new BasicStroke(1.8f));
        for (int i = 0; i < series.length; i++) {
            double[] s = series[i];
            g.setColor(COLORES[i % COLORES.length]);
            // si hay muchos puntos se toma uno cada "paso" para no dibujar de mas
            int paso = Math.max(1, s.length / Math.max(1, ancho));
            int xAnt = -1, yAnt = -1;
            for (int k = 0; k < s.length; k += paso) {
                int x = izq + (int) ((long) k * ancho / maxCiclos);
                int y = arr + alto - (int) (s[k] * alto);
                if (xAnt >= 0) {
                    g.drawLine(xAnt, yAnt, x, y);
                }
                xAnt = x;
                yAnt = y;
            }
            // leyenda
            int ly = arr + 10 + i * 18;
            g.fillRect(izq + ancho + 15, ly - 8, 12, 4);
            g.setColor(Color.DARK_GRAY);
            g.drawString("Computador " + computadores[i].getId(), izq + ancho + 32, ly);
        }
    }
}
