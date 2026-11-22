/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.model;

/**
 * Administra la Memoria Principal (RAM) dividida en marcos de pagina.
 * Gestiona los bloques mediante arreglos nativos sin colecciones de Java.
 * 
 * @author Daniel Alonso
 */
public class MemoriaRAM {

    private final int tamanioTotalKB;
    private final int tamanioMarcoKB;
    private final MarcoMemoria[] marcos;

    public MemoriaRAM(int tamanioTotalKB, int tamanioMarcoKB) {
        this.tamanioTotalKB = tamanioTotalKB;
        this.tamanioMarcoKB = tamanioMarcoKB;
        int totalMarcos = tamanioTotalKB / tamanioMarcoKB;
        this.marcos = new MarcoMemoria[totalMarcos];

        for (int i = 0; i < totalMarcos; i++) {
            this.marcos[i] = new MarcoMemoria(i);
        }
    }

    /**
     * Asigna una cantidad de marcos libres de RAM a un proceso.
     */
    public boolean asignarMemoriaProceso(int idProceso, int cantidadMarcos) {
        if (getMarcosLibres() < cantidadMarcos) {
            return false;
        }

        int asignados = 0;
        for (int i = 0; i < marcos.length && asignados < cantidadMarcos; i++) {
            if (!marcos[i].isOcupado()) {
                marcos[i].ocupar(idProceso, asignados);
                asignados++;
            }
        }
        return true;
    }

    /**
     * Indica si existen suficientes marcos libres para alojar la cantidad solicitada.
     */
    public boolean tieneEspacio(int cantidadMarcos) {
        return getMarcosLibres() >= cantidadMarcos;
    }

    /**
     * Retorna cuantos marcos ocupa actualmente un proceso especifico en RAM.
     */
    public int getMarcosPorProceso(int idProceso) {
        int contador = 0;
        for (int i = 0; i < marcos.length; i++) {
            if (marcos[i].isOcupado() && marcos[i].getIdProcesoAsignado() == idProceso) {
                contador++;
            }
        }
        return contador;
    }

    /**
     * Retorna la cantidad de marcos desocupados en RAM.
     */
    public int getMarcosLibres() {
        int libres = 0;
        for (int i = 0; i < marcos.length; i++) {
            if (!marcos[i].isOcupado()) {
                libres++;
            }
        }
        return libres;
    }

    /**
     * Libera todos los marcos ocupados por un proceso.
     */
    public void liberarMemoriaProceso(int idProceso) {
        for (int i = 0; i < marcos.length; i++) {
            if (marcos[i].isOcupado() && marcos[i].getIdProcesoAsignado() == idProceso) {
                marcos[i].liberar();
            }
        }
    }

    /**
     * Calcula el porcentaje actual de memoria RAM utilizada.
     */
    public double getPorcentajeUso() {
        int ocupados = marcos.length - getMarcosLibres();
        return ((double) ocupados / marcos.length) * 100.0;
    }

    // Getters
    public int getTamanioTotalKB() { return tamanioTotalKB; }
    public int getTamanioMarcoKB() { return tamanioMarcoKB; }
    public MarcoMemoria[] getMarcos() { return marcos; }
    public int getTotalMarcos() { return marcos.length; }
}