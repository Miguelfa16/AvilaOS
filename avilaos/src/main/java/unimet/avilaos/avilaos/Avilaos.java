package unimet.avilaos.avilaos;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import unimet.avilaos.cluster.Cluster;
import unimet.avilaos.gui.VentanaConfiguracion;
import unimet.avilaos.gui.VentanaPrincipal;

public class Avilaos {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignorada) {
                // se queda el look por defecto
            }
            Cluster cluster = new VentanaConfiguracion().mostrar();
            if (cluster == null) {
                System.exit(0);
            }
            new VentanaPrincipal(cluster).setVisible(true);
        });
    }
}
