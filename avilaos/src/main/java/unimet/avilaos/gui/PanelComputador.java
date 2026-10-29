package unimet.avilaos.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import unimet.avilaos.hardware.Computador;
import unimet.avilaos.hardware.ModoEjecucion;
import unimet.avilaos.so.FotoSistema;
import unimet.avilaos.so.planificacion.TipoPolitica;

/** Vista de un computador: CPU, modo, memoria, politica y colas. Solo observa. */
public class PanelComputador extends JPanel {

    private final Computador computador;
    private final JComboBox<TipoPolitica> politica = new JComboBox<>(TipoPolitica.values());
    private final JLabel modo = new JLabel();
    private final JLabel cpu = new JLabel();
    private final JProgressBar ram = new JProgressBar();
    private final JTabbedPane colas = new JTabbedPane();
    private final ModeloTablaSoloLectura enCpu = new ModeloTablaSoloLectura(FotoSistema.COLUMNAS);
    private final ModeloTablaSoloLectura nuevos = new ModeloTablaSoloLectura(FotoSistema.COLUMNAS);
    private final ModeloTablaSoloLectura listos = new ModeloTablaSoloLectura(FotoSistema.COLUMNAS);
    private final ModeloTablaSoloLectura bloqueados = new ModeloTablaSoloLectura(FotoSistema.COLUMNAS);
    private final ModeloTablaSoloLectura terminados = new ModeloTablaSoloLectura(FotoSistema.COLUMNAS);

    public PanelComputador(Computador computador) {
        super(new BorderLayout(4, 4));
        this.computador = computador;
        setBorder(BorderFactory.createTitledBorder("Computador " + computador.getId()));

        politica.setSelectedItem(computador.getOs().getTipoPolitica());
        politica.addActionListener(e ->
                computador.getOs().cambiarPolitica((TipoPolitica) politica.getSelectedItem()));
        politica.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> l, Object v,
                    int i, boolean sel, boolean foco) {
                return super.getListCellRendererComponent(l, ((TipoPolitica) v).getEtiqueta(), i, sel, foco);
            }
        });

        JPanel arriba = new JPanel(new GridLayout(0, 1, 2, 2));
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        fila.add(new JLabel("Politica:"));
        fila.add(politica);
        modo.setOpaque(true);
        modo.setFont(modo.getFont().deriveFont(Font.BOLD));
        fila.add(modo);
        arriba.add(fila);
        arriba.add(cpu);
        ram.setStringPainted(true);
        arriba.add(ram);

        JScrollPane tablaCpu = new JScrollPane(new JTable(enCpu));
        tablaCpu.setPreferredSize(new Dimension(100, 70));
        tablaCpu.setBorder(BorderFactory.createTitledBorder("PCB en CPU"));

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(arriba, BorderLayout.NORTH);
        norte.add(tablaCpu, BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);

        colas.addTab("Nuevos", new JScrollPane(new JTable(nuevos)));
        colas.addTab("Listos", new JScrollPane(new JTable(listos)));
        colas.addTab("Bloqueados", new JScrollPane(new JTable(bloqueados)));
        colas.addTab("Terminados", new JScrollPane(new JTable(terminados)));
        colas.setSelectedIndex(1);
        add(colas, BorderLayout.CENTER);
        setPreferredSize(new Dimension(620, 380));
    }

    public void refrescar() {
        FotoSistema f = computador.getOs().foto();
        if (politica.getSelectedItem() != f.politica) {
            politica.setSelectedItem(f.politica);
        }
        modo.setText(" " + f.modo.getEtiqueta() + " ");
        modo.setBackground(f.modo == ModoEjecucion.USUARIO ? new Color(0xC8E6C9)
                : f.modo == ModoEjecucion.SISTEMA_OPERATIVO ? new Color(0xFFE0B2) : new Color(0xE0E0E0));
        cpu.setText(f.enCpu == null ? "CPU: libre"
                : "CPU: " + f.enCpu[0] + "   PC=" + f.pcEnCpu + "   Prioridad=" + f.enCpu[5]
                + "   Deadline=" + f.enCpu[7] + "   Instruccion: " + f.instruccion);
        ram.setMaximum(Math.max(1, f.memTotal));
        ram.setValue(f.memUsada);
        ram.setString("RAM usada " + f.memUsada + " / " + f.memTotal + "  (libre " + (f.memTotal - f.memUsada) + ")");
        enCpu.setFilas(f.enCpu == null ? new Object[0][] : new Object[][] {f.enCpu});
        nuevos.setFilas(f.nuevos);
        listos.setFilas(f.listos);
        bloqueados.setFilas(f.bloqueados);
        terminados.setFilas(f.terminados);
        colas.setTitleAt(0, "Nuevos (" + f.nuevos.length + ")");
        colas.setTitleAt(1, "Listos (" + f.listos.length + ")");
        colas.setTitleAt(2, "Bloqueados (" + f.bloqueados.length + ")");
        colas.setTitleAt(3, "Terminados (" + f.terminados.length + ")");
    }
}
