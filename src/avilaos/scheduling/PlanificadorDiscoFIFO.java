package avilaos.scheduling;

import avilaos.datastructures.ColaProcesos;
import avilaos.enums.DireccionCabezal;
import avilaos.model.SolicitudDisco;

/**
 * Implementa la politica de planificacion de disco First-In, First-Out (FIFO).
 * Atiende las solicitudes de E/S en el estricto orden cronologico en que llegaron a la cola.
 * 
 * @author Gustavo Abad
 */
public class PlanificadorDiscoFIFO implements PlanificadorDisco {

    @Override
    public SolicitudDisco seleccionarSiguiente(ColaProcesos colaSolicitudes, int posicionCabezal,
            DireccionCabezal direccionActual, int totalBloques) {
        if (colaSolicitudes == null || colaSolicitudes.estaVacia()) {
            return null;
        }
        // Desencola directamente el primer elemento en haber ingresado
        return (SolicitudDisco) colaSolicitudes.desencolar();
    }

    @Override
    public String getNombrePolitica() {
        return "FIFO";
    }
}