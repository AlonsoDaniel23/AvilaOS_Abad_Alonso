package avilaos.model;

import java.awt.Color;

/**
 * Representa un bloque fisico individual dentro del Disco Simulado (SD).
 * Soporta asignacion encadenada mediante un puntero al siguiente bloque del archivo
 * y permite reservar bloques dedicados al area de intercambio (Swap).
 * 
 * @author Gustavo Abad
 */
public class BloqueDisco {

    private final int numeroBloque; // Indice o direccion fisica del bloque en el disco
    private boolean ocupado;        // Indica si el bloque contiene datos activos
    private boolean esAreaSwap;     // Indica si el bloque pertenece a la particion de Swap
    private String propietario;     // Nombre del archivo dueno o identificador del proceso en Swap
    private int idProcesoSwap;      // ID del proceso suspendido que ocupa el bloque (-1 si no aplica)
    private int siguienteBloque;    // Puntero al siguiente bloque en asignacion encadenada (-1 si es el fin)
    private Color colorArchivo;     // Color distintivo del archivo que ocupa el bloque

    public BloqueDisco(int numeroBloque, boolean esAreaSwap) {
        this.numeroBloque = numeroBloque;
        this.esAreaSwap = esAreaSwap;
        this.ocupado = false;
        this.propietario = "";
        this.idProcesoSwap = -1;
        this.siguienteBloque = -1;
        this.colorArchivo = null;
    }

    public void asignarArchivo(String nombreArchivo, int siguienteBloque) {
        asignarArchivo(nombreArchivo, siguienteBloque, null);
    }

    public void asignarArchivo(String nombreArchivo, int siguienteBloque, Color colorArchivo) {
        this.ocupado = true;
        this.propietario = (nombreArchivo != null) ? nombreArchivo : "Archivo";
        this.idProcesoSwap = -1;
        this.siguienteBloque = siguienteBloque;
        this.colorArchivo = colorArchivo;
    }

    public void asignarSwap(int idProceso, String nombreProceso) {
        this.ocupado = true;
        this.idProcesoSwap = idProceso;
        this.propietario = "SWAP:" + ((nombreProceso != null) ? nombreProceso : ("P" + idProceso));
        this.siguienteBloque = -1;
        this.colorArchivo = null;
    }

    public void liberar() {
        this.ocupado = false;
        this.propietario = "";
        this.idProcesoSwap = -1;
        this.siguienteBloque = -1;
        this.colorArchivo = null;
    }

    // ==================== GETTERS Y SETTERS ====================

    public int getNumeroBloque() {
        return numeroBloque;
    }

    public boolean isOcupado() {
        return ocupado;
    }

    public boolean isEsAreaSwap() {
        return esAreaSwap;
    }

    public void setEsAreaSwap(boolean esAreaSwap) {
        this.esAreaSwap = esAreaSwap;
    }

    public String getPropietario() {
        return propietario;
    }

    public int getIdProcesoSwap() {
        return idProcesoSwap;
    }

    public int getSiguienteBloque() {
        return siguienteBloque;
    }

    public void setSiguienteBloque(int siguienteBloque) {
        this.siguienteBloque = siguienteBloque;
    }

    public Color getColorArchivo() {
        return colorArchivo;
    }

    public void setColorArchivo(Color colorArchivo) {
        this.colorArchivo = colorArchivo;
    }
}