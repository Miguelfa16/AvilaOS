package unimet.avilaos.gui;

import javax.swing.table.DefaultTableModel;

/** Tabla que no se puede editar y se rellena con filas de una foto. */
public class ModeloTablaSoloLectura extends DefaultTableModel {

    public ModeloTablaSoloLectura(String[] columnas) {
        super(columnas, 0);
    }

    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }

    public void setFilas(Object[][] filas) {
        setRowCount(0);
        for (Object[] f : filas) {
            addRow(f);
        }
    }
}
