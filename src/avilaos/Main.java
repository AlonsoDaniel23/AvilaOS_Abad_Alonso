package avilaos;

import avilaos.gui.Vista1Configuracion;
import javax.swing.SwingUtilities;

/**
 * Punto de entrada principal para pruebas de integracion de AvilaOS.
 * 
 * @author Equipo AvilaOS
 */
public class Main {

    public static void main(String[] args) {
        // Lanzar la interfaz gráfica de configuración e inserción
        SwingUtilities.invokeLater(() -> {
            Vista1Configuracion vista1 = new Vista1Configuracion();
            vista1.setVisible(true);
        });
    }
}