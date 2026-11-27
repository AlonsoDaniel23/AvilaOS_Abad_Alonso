package avilaos.scheduling;

import avilaos.datastructures.ColaProcesos;
import avilaos.enums.DireccionCabezal;
import avilaos.model.SolicitudDisco;

/**
 * Interfaz que define el contrato del patron Strategy para todas las politicas
 * de planificacion de solicitudes de E/S en el Disco Simulado (FIFO, SSTF, SCAN, C-SCAN).
 * Permite intercambiar algoritmos dinamicamente sin modificar el codigo del disco.
 * 
 * @author Gustavo Abad
 */
public interface PlanificadorDisco {

    /**
     * Selecciona y extrae de la cola de solicitudes la siguiente peticion a atender
     * segun el criterio de la politica activa.
     * 
     * @param colaSolicitudes Cola compartida de solicitudes pendientes de E/S.
     * @param posicionCabezal Bloque actual donde se encuentra ubicado el cabezal.
     * @param direccionActual Sentido actual de movimiento del cabezal (ASCENDENTE/DESCENDENTE).
     * @param totalBloques Capacidad total de bloques del disco (0 a totalBloques - 1).
     * @return La SolicitudDisco seleccionada y removida de la cola, o null si esta vacia.
     */
    SolicitudDisco seleccionarSiguiente(ColaProcesos colaSolicitudes, int posicionCabezal,
            DireccionCabezal direccionActual, int totalBloques);

    /**
     * Retorna el nombre identificador de la politica de planificacion de disco.
     * 
     * @return Nombre de la politica (ej. "FIFO", "SSTF", "SCAN", "C-SCAN").
     */
    String getNombrePolitica();
}