/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.model;

/**
 * Representa una entrada individual dentro de la Tabla de Paginas de un proceso.
 * Relaciona una pagina logica con un marco de RAM o una direccion en el disco de intercambio (Swap).
 * 
 * @author Daniel Alonso
 */
public class EntradaTablaPaginas {
    
    private final int numeroPagina;
    private int numeroMarco;
    private boolean presente; // true = en RAM, false = en Swap (Fallo de pagina)
    private boolean modificado;
    private int bloqueSwap;

    public EntradaTablaPaginas(int numeroPagina) {
        this.numeroPagina = numeroPagina;
        this.numeroMarco = -1;
        this.presente = false;
        this.modificado = false;
        this.bloqueSwap = -1;
    }

    /**
     * Asigna la pagina a un marco especifico en la Memoria RAM.
     * @param numeroMarco Indice del marco asignado.
     */
    public void cargarEnRAM(int numeroMarco) {
        this.numeroMarco = numeroMarco;
        this.presente = true;
    }

    /**
     * Registra el desalojo de la pagina hacia el almacenamiento secundario (Swap).
     * @param bloqueSwap Indice del bloque asignado en el DiscoSwap.
     */
    public void enviarASwap(int bloqueSwap) {
        this.numeroMarco = -1;
        this.presente = false;
        this.bloqueSwap = bloqueSwap;
    }

    // Getters y Setters
    public int getNumeroPagina() {
        return numeroPagina;
    }

    public int getNumeroMarco() {
        return numeroMarco;
    }

    public boolean isPresente() {
        return presente;
    }

    public void setPresente(boolean presente) {
        this.presente = presente;
    }

    public boolean isModificado() {
        return modificado;
    }

    public void setModificado(boolean modificado) {
        this.modificado = modificado;
    }

    public int getBloqueSwap() {
        return bloqueSwap;
    }

    public void setBloqueSwap(int bloqueSwap) {
        this.bloqueSwap = bloqueSwap;
    }
}
