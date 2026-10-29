/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package avilaos.enums;

/**
 * Enumera los estados posibles de un proceso segun el Modelo de 7 Estados de Stallings.
 * Incluye estados de suspension para la gestion avanzada en memoria secundaria (Swap).
 * 
 * @author Daniel Alonso
 */
public enum EstadoProceso {
    
    NUEVO,
    LISTO,
    EJECUCION,
    BLOQUEADO,
    SUSPENDIDO_LISTO,
    SUSPENDIDO_BLOQUEADO,
    TERMINADO
}
