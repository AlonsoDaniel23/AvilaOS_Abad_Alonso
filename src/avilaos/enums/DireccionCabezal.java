package avilaos.enums;

/**
 * Define la direccion actual de movimiento del cabezal de lectura/escritura
 * del Disco Simulado (SD). Permite a politicas como SCAN y C-SCAN determinar
 * el sentido de recorrido sobre las pistas o bloques.
 * 
 * @author Gustavo Abad
 */
public enum DireccionCabezal {
    ASCENDENTE,  // Movimiento hacia bloques con numeracion mayor (↑)
    DESCENDENTE  // Movimiento hacia bloques con numeracion menor (↓)
}