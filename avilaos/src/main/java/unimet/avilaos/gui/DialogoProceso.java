package unimet.avilaos.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import unimet.avilaos.cluster.Cluster;
import unimet.avilaos.cluster.EspecificacionProceso;
import unimet.avilaos.estructuras.Recorrido;
import unimet.avilaos.sincronizacion.BufferAcotado;
import unimet.avilaos.so.proceso.TipoProceso;

/**
 * Vista 1: crear procesos. Los JSpinner solo aceptan numeros en rango
 * (si escriben basura vuelve al ultimo valor bueno). El nombre y el buffer
 * se validan al darle a Crear.
 */
public class DialogoProceso extends JDialog {

    private final Cluster cluster;
    private final JTextField nombre = new JTextField("Proceso");
    private final JComboBox<TipoProceso> tipo = new JComboBox<>(TipoProceso.values());
    private final JSpinner instrucciones = numero(50, 1, 1_000_000);
    private final JSpinner memoria = numero(32, 1, 1_000_000);
    private final JSpinner prioridad = numero(5, 0, 99);
    private final JSpinner deadline = numero(300, 1, 10_000_000);
    private final JComboBox<String> computador = new JComboBox<>();
    private final JSpinner esCada = numero(5, 1, 1_000_000);
    private final JSpinner duracionES = numero(4, 1, 1_000_000);
    private final JComboBox<BufferAcotado> buffer = new JComboBox<>();
    private final JSpinner cadaCuantos = numero(2, 1, 1_000_000);
    private final JSpinner elementos = numero(5, 1, 1_000_000);

    public DialogoProceso(JFrame duena, Cluster cluster) {
        super(duena, "Nuevo proceso", true);
        this.cluster = cluster;

        computador.addItem("Automatico (lo decide AvilaOS)");
        for (int i = 1; i <= cluster.getComputadores().length; i++) {
            computador.addItem("Computador " + i);
        }
        Recorrido<BufferAcotado> r = cluster.getBuffers().recorrido();
        while (r.haySiguiente()) {
            buffer.addItem(r.siguiente());
        }

        JPanel f = new JPanel(new GridLayout(0, 2, 6, 4));
        f.add(new JLabel("Nombre:")); f.add(nombre);
        f.add(new JLabel("Tipo:")); f.add(tipo);
        f.add(new JLabel("Instrucciones:")); f.add(instrucciones);
        f.add(new JLabel("Memoria:")); f.add(memoria);
        f.add(new JLabel("Prioridad (0 = la mas alta):")); f.add(prioridad);
        f.add(new JLabel("Deadline (ciclos):")); f.add(deadline);
        f.add(new JLabel("Computador:")); f.add(computador);
        f.add(new JLabel("I/O bound: E/S cada (instrucciones):")); f.add(esCada);
        f.add(new JLabel("I/O bound: ciclos que tarda la E/S:")); f.add(duracionES);
        f.add(new JLabel("Prod/Cons: buffer:")); f.add(buffer);
        f.add(new JLabel("Prod/Cons: cada cuantos ciclos:")); f.add(cadaCuantos);
        f.add(new JLabel("Prod/Cons: elementos para terminar:")); f.add(elementos);

        tipo.addActionListener(e -> habilitar());
        habilitar();

        JButton crear = new JButton("Crear");
        crear.addActionListener(e -> crear());
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> dispose());
        JPanel botones = new JPanel();
        botones.add(crear);
        botones.add(cancelar);

        setLayout(new BorderLayout(6, 6));
        add(f, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(duena);
    }

    private void habilitar() {
        TipoProceso t = (TipoProceso) tipo.getSelectedItem();
        boolean io = t == TipoProceso.IO_BOUND;
        boolean pc = t != null && t.usaBuffer();
        esCada.setEnabled(io);
        duracionES.setEnabled(io);
        buffer.setEnabled(pc);
        cadaCuantos.setEnabled(pc);
        elementos.setEnabled(pc);
    }

    private void crear() {
        String n = nombre.getText().trim();
        if (n.isEmpty() || n.length() > 30 || n.contains(",")) {
            error("El nombre es obligatorio, de maximo 30 caracteres y sin comas.");
            return;
        }
        TipoProceso t = (TipoProceso) tipo.getSelectedItem();
        BufferAcotado b = (BufferAcotado) buffer.getSelectedItem();
        if (t.usaBuffer() && b == null) {
            error("Primero crea un buffer para los productores y consumidores.");
            return;
        }
        int comp = computador.getSelectedIndex();
        int mem = valor(memoria);
        if (comp > 0 && mem > cluster.getComputadores()[comp - 1].getRam().getCapacidad()) {
            error("Pide mas memoria que toda la RAM del Computador " + comp + ", nunca entraria.");
            return;
        }
        try {
            cluster.crearProceso(new EspecificacionProceso(n, t, valor(instrucciones), mem, valor(prioridad),
                    valor(deadline), comp, valor(esCada), valor(duracionES), b == null ? null : b.getNombre(),
                    valor(cadaCuantos), valor(elementos)));
            dispose();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            error(ex.getMessage());
        }
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Dato invalido", JOptionPane.WARNING_MESSAGE);
    }

    static JSpinner numero(int v, int min, int max) {
        return new JSpinner(new SpinnerNumberModel(v, min, max, 1));
    }

    static int valor(JSpinner s) {
        try {
            s.commitEdit();
        } catch (java.text.ParseException ignorada) {
            // se queda el ultimo valor valido
        }
        return (Integer) s.getValue();
    }
}
