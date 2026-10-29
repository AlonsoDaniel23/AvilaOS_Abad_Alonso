/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.model;

/**
 * Representa un marco de pagina (frame) dentro de la memoria RAM fisica.
 * Modela la unidad comun de asignacion de memoria segun el esquema de paginacion de Stallings.
 * 
 * @author Daniel Alonso
 */
public class MarcoMemoria {
    
    private final int idMarco;
    private boolean ocupado;
    private int idProcesoAsignado;
    private int numeroPagina;

    public MarcoMemoria(int idMarco) {
        this.idMarco = idMarco;
        this.ocupado = false;
        this.idProcesoAsignado = -1;
        this.numeroPagina = -1;
    }

    /**
     * Asigna el marco a una pagina de un proceso especifico.
     * @param idProceso ID del proceso propietario.
     * @param numeroPagina Numero de pagina logica del proceso.
     */
    public void ocupar(int idProceso, int numeroPagina) {
        this.ocupado = true;
        this.idProcesoAsignado = idProceso;
        this.numeroPagina = numeroPagina;
    }

    /**
     * Libera el marco de memoria.
     */
    public void liberar() {
        this.ocupado = false;
        this.idProcesoAsignado = -1;
        this.numeroPagina = -1;
    }

    // Getters
    public int getIdMarco() {
        return idMarco;
    }

    public boolean isOcupado() {
        return ocupado;
    }

    public int getIdProcesoAsignado() {
        return idProcesoAsignado;
    }

    public int getNumeroPagina() {
        return numeroPagina;
    }
}
