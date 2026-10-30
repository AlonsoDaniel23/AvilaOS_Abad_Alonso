package avilaos.scheduling;

import avilaos.model.Proceso;

/**
 * Interfaz que define el comportamiento estándar para todos los
 * algoritmos de planificación del sistema operativo.
 * 
 * @author Gustavo Abad
 */
public interface Planificador {
    
    /**
     * Agrega un nuevo proceso a la estructura de planificación.
     * 
     * @param proceso El proceso que llega o pasa a estado Listo.
     */
    void agregarProceso(Proceso proceso);
    
    /**
     * Selecciona y extrae el siguiente proceso a ejecutar según la política.
     * 
     * @return El proceso seleccionado, o null si la cola está vacía.
     */
    Proceso despacharSiguiente();
    
    /**
     * Verifica si existen procesos en espera dentro del planificador.
     * 
     * @return true si hay procesos pendientes, false en caso contrario.
     */
    boolean hayProcesos();
    
    /**
     * Retorna el número total de procesos listos actualmente en espera.
     * 
     * @return Cantidad de procesos en la cola interna.
     */
    int getCantidadProcesos();
}