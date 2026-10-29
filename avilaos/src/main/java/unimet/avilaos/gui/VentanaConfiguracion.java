package unimet.avilaos.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.filechooser.FileNameExtensionFilter;
import unimet.avilaos.cluster.Cluster;
import unimet.avilaos.cluster.EspecificacionBuffer;
import unimet.avilaos.cluster.EspecificacionProceso;
import unimet.avilaos.avilaos.ConfiguradorCSV;
import unimet.avilaos.avilaos.Configuracion;
import unimet.avilaos.estructuras.Recorrido;
import unimet.avilaos.so.planificacion.TipoPolitica;

/**
 * Configuracion inicial: numero de computadores, RAM y politica de cada uno,
 * duracion del ciclo, quantum y latencia. Tambien carga un CSV guardado.
 */
public class VentanaConfiguracion extends JDialog {

    private static final int MAX = 8;

    private final JSpinner nComputadores = DialogoProceso.numero(2, 2, MAX);
    private final JSpinner duracion = DialogoProceso.numero(500, 1, 10_000);
    private final JSpinner quantum = DialogoProceso.numero(4, 1, 1000);
    private final JSpinner latencia = DialogoProceso.numero(3, 0, 1000);
    private final JSpinner[] ram = new JSpinner[MAX];
    @SuppressWarnings("unchecked")
    private final JComboBox<TipoPolitica>[] politica = new JComboBox[MAX];
    private final JPanel porComputador = new JPanel(new GridLayout(0, 3, 6, 4));
    private Configuracion cargada;
    private Cluster resultado;

    public VentanaConfiguracion() {
        super((java.awt.Frame) null, "AvilaOS - Configuracion", true);
        for (int i = 0; i < MAX; i++) {
            ram[i] = DialogoProceso.numero(512, 16, 1_000_000);
            politica[i] = new JComboBox<>(TipoPolitica.values());
        }

        JPanel global = new JPanel(new GridLayout(0, 2, 6, 4));
        global.setBorder(BorderFactory.createTitledBorder("Parametros"));
        global.add(new JLabel("Numero de computadores:")); global.add(nComputadores);
        global.add(new JLabel("Duracion del ciclo (ms):")); global.add(duracion);
        global.add(new JLabel("Quantum Round Robin:")); global.add(quantum);
        global.add(new JLabel("Latencia de red (ciclos):")); global.add(latencia);

        porComputador.setBorder(BorderFactory.createTitledBorder("Computadores"));
        armarFilas();
        nComputadores.addChangeListener(e -> armarFilas());

        JButton cargar = new JButton("Cargar configuracion...");
        cargar.addActionListener(e -> cargar());
        JButton iniciar = new JButton("Iniciar simulacion");
        iniciar.addActionListener(e -> iniciar());
        JPanel abajo = new JPanel();
        abajo.add(cargar);
        abajo.add(iniciar);

        setLayout(new BorderLayout(6, 6));
        add(global, BorderLayout.NORTH);
        add(new JScrollPane(porComputador), BorderLayout.CENTER);
        add(abajo, BorderLayout.SOUTH);
        setSize(540, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void armarFilas() {
        porComputador.removeAll();
        porComputador.add(new JLabel("Computador"));
        porComputador.add(new JLabel("RAM"));
        porComputador.add(new JLabel("Politica inicial"));
        int n = (Integer) nComputadores.getValue();
        for (int i = 0; i < n; i++) {
            porComputador.add(new JLabel("Computador " + (i + 1)));
            porComputador.add(ram[i]);
            porComputador.add(politica[i]);
        }
        porComputador.revalidate();
        porComputador.repaint();
    }

    private void cargar() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Configuracion CSV", "csv"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Configuracion c = ConfiguradorCSV.cargar(fc.getSelectedFile());
            if (c.rams.length > MAX) {
                throw new IOException("Maximo " + MAX + " computadores");
            }
            nComputadores.setValue(c.rams.length);
            duracion.setValue(c.duracionCiclo);
            quantum.setValue(c.quantum);
            latencia.setValue(c.latencia);
            for (int i = 0; i < c.rams.length; i++) {
                ram[i].setValue(c.rams[i]);
                politica[i].setSelectedItem(c.politicas[i]);
            }
            cargada = c;
            JOptionPane.showMessageDialog(this, "Cargado: " + c.buffers.tamano() + " buffers y "
                    + c.procesos.tamano() + " procesos. Se crean al iniciar.");
        } catch (IOException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void iniciar() {
        int n = (Integer) nComputadores.getValue();
        int[] rams = new int[n];
        TipoPolitica[] pols = new TipoPolitica[n];
        for (int i = 0; i < n; i++) {
            rams[i] = DialogoProceso.valor(ram[i]);
            pols[i] = (TipoPolitica) politica[i].getSelectedItem();
        }
        resultado = new Cluster(rams, pols, DialogoProceso.valor(duracion), DialogoProceso.valor(quantum),
                DialogoProceso.valor(latencia));
        if (cargada != null) {
            crearCargados();
        }
        dispose();
    }

    /** Crea los buffers y procesos del archivo. Si alguno falla, avisa y sigue. */
    private void crearCargados() {
        StringBuilder errores = new StringBuilder();
        Recorrido<EspecificacionBuffer> rb = cargada.buffers.recorrido();
        while (rb.haySiguiente()) {
            EspecificacionBuffer b = rb.siguiente();
            try {
                resultado.crearBuffer(b);
            } catch (RuntimeException ex) {
                errores.append("Buffer ").append(b.nombre()).append(": ").append(ex.getMessage()).append('\n');
            }
        }
        Recorrido<EspecificacionProceso> rp = cargada.procesos.recorrido();
        while (rp.haySiguiente()) {
            EspecificacionProceso p = rp.siguiente();
            try {
                resultado.crearProceso(p);
            } catch (RuntimeException ex) {
                errores.append("Proceso ").append(p.nombre()).append(": ").append(ex.getMessage()).append('\n');
            }
        }
        if (errores.length() > 0) {
            JOptionPane.showMessageDialog(this, errores.toString(), "Algunos datos no se cargaron",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Muestra la ventana y devuelve el cluster (null si la cerraron). */
    public Cluster mostrar() {
        setVisible(true);
        return resultado;
    }

    static File conExtension(File f) {
        return f.getName().toLowerCase().endsWith(".csv") ? f : new File(f.getPath() + ".csv");
    }
}
