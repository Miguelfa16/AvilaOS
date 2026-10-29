package unimet.avilaos.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import unimet.avilaos.cluster.Cluster;
import unimet.avilaos.hardware.Computador;
import unimet.avilaos.avilaos.ConfiguradorCSV;
import unimet.avilaos.avilaos.Configuracion;
import unimet.avilaos.estructuras.ListaEnlazada;
import unimet.avilaos.estructuras.Recorrido;
import unimet.avilaos.metricas.Estadisticas;
import unimet.avilaos.sincronizacion.BufferAcotado;

/** Ventana principal. Cada 100 ms pide fotos al cluster y las pinta. */
public class VentanaPrincipal extends JFrame {

    private final Cluster cluster;
    private final PanelComputador[] paneles;
    private final JLabel ciclo = new JLabel("Ciclo: 0");
    private final JButton iniciarPausar = new JButton("Iniciar");
    private final ModeloTablaSoloLectura tablaBuffers = new ModeloTablaSoloLectura(BufferAcotado.COLUMNAS);
    private final ModeloTablaSoloLectura tablaMetricas = new ModeloTablaSoloLectura(new String[] {
        "Computador", "Throughput (proc/ciclo)", "Uso de CPU", "T. respuesta prom.",
        "Cumplen deadline", "Equidad (Jain)", "Bloqueo prom. en semaforos", "Completados", "Vencidos"
    });
    private final JTextArea log = new JTextArea();
    private final GraficaUtilizacion grafica;

    public VentanaPrincipal(Cluster cluster) {
        super("AvilaOS - Simulador de sistema operativo distribuido");
        this.cluster = cluster;

        // ---- barra de arriba
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        iniciarPausar.addActionListener(e -> iniciarOPausar());
        JButton paso = new JButton("Paso");
        paso.addActionListener(e -> new Thread(() -> cluster.getReloj().paso()).start());
        JSpinner ms = DialogoProceso.numero(cluster.getReloj().getDuracionCiclo(), 1, 10_000);
        ms.addChangeListener(e -> cluster.getReloj().setDuracionCiclo((Integer) ms.getValue()));
        JSpinner quantum = DialogoProceso.numero(cluster.getParametros().getQuantum(), 1, 1000);
        quantum.addChangeListener(e -> cluster.getParametros().setQuantum((Integer) quantum.getValue()));
        JSpinner latencia = DialogoProceso.numero(cluster.getParametros().getLatenciaRed(), 0, 1000);
        latencia.addChangeListener(e -> cluster.getParametros().setLatenciaRed((Integer) latencia.getValue()));
        JButton nuevoProceso = new JButton("Nuevo proceso");
        nuevoProceso.addActionListener(e -> new DialogoProceso(this, cluster).setVisible(true));
        JButton nuevoBuffer = new JButton("Nuevo buffer");
        nuevoBuffer.addActionListener(e -> new DialogoBuffer(this, cluster).setVisible(true));
        JButton guardar = new JButton("Guardar configuracion");
        guardar.addActionListener(e -> guardar());

        ciclo.setFont(ciclo.getFont().deriveFont(16f));
        barra.add(iniciarPausar);
        barra.add(paso);
        barra.add(ciclo);
        barra.add(new JLabel("  Ciclo (ms):"));
        barra.add(ms);
        barra.add(new JLabel("Quantum:"));
        barra.add(quantum);
        barra.add(new JLabel("Latencia:"));
        barra.add(latencia);
        barra.add(nuevoProceso);
        barra.add(nuevoBuffer);
        barra.add(guardar);
        barra.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        // ---- computadores
        Computador[] comps = cluster.getComputadores();
        paneles = new PanelComputador[comps.length];
        JPanel rejilla = new JPanel(new GridLayout(0, 2, 6, 6));
        for (int i = 0; i < comps.length; i++) {
            paneles[i] = new PanelComputador(comps[i]);
            rejilla.add(paneles[i]);
        }

        // ---- abajo
        grafica = new GraficaUtilizacion(comps);
        JTabbedPane abajo = new JTabbedPane();
        abajo.addTab("Buffers", new JScrollPane(new JTable(tablaBuffers)));
        abajo.addTab("Metricas", new JScrollPane(new JTable(tablaMetricas)));
        log.setEditable(false);
        abajo.addTab("Log de eventos", new JScrollPane(log));
        abajo.addTab("Grafica de uso de CPU", grafica);
        abajo.setPreferredSize(new Dimension(100, 250));
        abajo.setSelectedIndex(2);

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(rejilla), abajo);
        division.setResizeWeight(0.7);

        setLayout(new BorderLayout());
        add(barra, BorderLayout.NORTH);
        add(division, BorderLayout.CENTER);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1350, 900);
        setLocationRelativeTo(null);

        new Timer(100, e -> refrescar()).start();
    }

    private void iniciarOPausar() {
        if (cluster.getReloj().estaCorriendo()) {
            cluster.getReloj().pausar();
        } else {
            cluster.getReloj().iniciar();
        }
    }

    private void guardar() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Configuracion CSV", "csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Configuracion c = new Configuracion();
        Computador[] comps = cluster.getComputadores();
        c.duracionCiclo = cluster.getReloj().getDuracionCiclo();
        c.quantum = cluster.getParametros().getQuantum();
        c.latencia = cluster.getParametros().getLatenciaRed();
        c.rams = new int[comps.length];
        c.politicas = new unimet.avilaos.so.planificacion.TipoPolitica[comps.length];
        for (int i = 0; i < comps.length; i++) {
            c.rams[i] = comps[i].getRam().getCapacidad();
            c.politicas[i] = comps[i].getOs().getTipoPolitica();
        }
        Recorrido<unimet.avilaos.cluster.EspecificacionBuffer> rb = cluster.getBuffersCreados().recorrido();
        while (rb.haySiguiente()) {
            c.buffers.agregarAlFinal(rb.siguiente());
        }
        Recorrido<unimet.avilaos.cluster.EspecificacionProceso> rp = cluster.getProcesosCreados().recorrido();
        while (rp.haySiguiente()) {
            c.procesos.agregarAlFinal(rp.siguiente());
        }
        try {
            ConfiguradorCSV.guardar(VentanaConfiguracion.conExtension(fc.getSelectedFile()), c);
            JOptionPane.showMessageDialog(this, "Configuracion guardada.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refrescar() {
        ciclo.setText("Ciclo: " + cluster.getReloj().getCiclo());
        iniciarPausar.setText(cluster.getReloj().estaCorriendo() ? "Pausar" : "Iniciar");
        for (PanelComputador p : paneles) {
            p.refrescar();
        }

        ListaEnlazada<BufferAcotado> bufs = cluster.getBuffers();
        Object[][] filas = new Object[bufs.tamano()][];
        Recorrido<BufferAcotado> r = bufs.recorrido();
        int i = 0;
        while (r.haySiguiente()) {
            filas[i++] = r.siguiente().fila();
        }
        tablaBuffers.setFilas(filas);

        refrescarMetricas();
        grafica.repaint();

        ListaEnlazada<String> lineas = cluster.getLog().vaciar();
        if (!lineas.estaVacia()) {
            StringBuilder sb = new StringBuilder();
            Recorrido<String> rl = lineas.recorrido();
            while (rl.haySiguiente()) {
                sb.append(rl.siguiente()).append('\n');
            }
            log.append(sb.toString());
            log.setCaretPosition(log.getDocument().getLength());
        }
    }

    private void refrescarMetricas() {
        Computador[] comps = cluster.getComputadores();
        Object[][] filas = new Object[comps.length + 1][];
        long c = Math.max(1, cluster.getReloj().getCiclo());
        for (int k = 0; k < comps.length; k++) {
            Estadisticas e = comps[k].getOs().getEstadisticas();
            long total = Math.max(1, comps[k].getCpu().getCiclosTotales());
            filas[k] = new Object[] {
                "Computador " + comps[k].getId(),
                num((double) e.getCompletados() / c),
                pct((double) comps[k].getCpu().getCiclosOcupada() / total),
                num(e.getRespuestaPromedio()),
                pct(e.getCumplimientoDeadlines()),
                num(e.getEquidad()),
                num(e.getSemaforosPromedio()),
                e.getCompletados(), e.getVencidos()
            };
        }
        filas[comps.length] = new Object[] {
            "SISTEMA", num(cluster.throughputGlobal()), pct(cluster.utilizacionGlobal()),
            num(cluster.respuestaPromedioGlobal()), pct(cluster.cumplimientoGlobal()), "-", "-", "", ""
        };
        tablaMetricas.setFilas(filas);
    }

    private static String num(double d) {
        return String.format("%.3f", d);
    }

    private static String pct(double d) {
        return String.format("%.1f %%", d * 100);
    }
}
