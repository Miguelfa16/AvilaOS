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
import unimet.avilaos.cluster.Cluster;
import unimet.avilaos.cluster.EspecificacionBuffer;

/** Crear buffers: nombre, capacidad, anfitrion y memoria que ocupa. */
public class DialogoBuffer extends JDialog {

    public DialogoBuffer(JFrame duena, Cluster cluster) {
        super(duena, "Nuevo buffer", true);
        JTextField nombre = new JTextField("B" + (cluster.getBuffers().tamano() + 1));
        JSpinner capacidad = DialogoProceso.numero(5, 1, 10_000);
        JSpinner memoria = DialogoProceso.numero(5, 1, 1_000_000);
        JComboBox<String> anfitrion = new JComboBox<>();
        for (int i = 1; i <= cluster.getComputadores().length; i++) {
            anfitrion.addItem("Computador " + i);
        }

        JPanel f = new JPanel(new GridLayout(0, 2, 6, 4));
        f.add(new JLabel("Nombre:")); f.add(nombre);
        f.add(new JLabel("Capacidad (elementos):")); f.add(capacidad);
        f.add(new JLabel("Computador anfitrion:")); f.add(anfitrion);
        f.add(new JLabel("Memoria que ocupa:")); f.add(memoria);

        JButton crear = new JButton("Crear");
        crear.addActionListener(e -> {
            String n = nombre.getText().trim();
            if (n.isEmpty() || n.length() > 20 || n.contains(",")) {
                JOptionPane.showMessageDialog(this, "Nombre obligatorio, maximo 20 caracteres y sin comas.");
                return;
            }
            try {
                cluster.crearBuffer(new EspecificacionBuffer(n, DialogoProceso.valor(capacidad),
                        anfitrion.getSelectedIndex() + 1, DialogoProceso.valor(memoria)));
                dispose();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo crear", JOptionPane.WARNING_MESSAGE);
            }
        });
        JPanel botones = new JPanel();
        botones.add(crear);

        setLayout(new BorderLayout(6, 6));
        add(f, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(duena);
    }
}
