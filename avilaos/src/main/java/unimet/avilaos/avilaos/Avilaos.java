package unimet.avilaos.avilaos;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import unimet.avilaos.gui.VentanaPrincipal;

/** Punto de entrada. Abre la ventana de configuracion y despues la principal. */
public class Avilaos {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignorada) {
                // se queda el look por defecto
            }
            VentanaPrincipal.abrir();
        });
    }
}
