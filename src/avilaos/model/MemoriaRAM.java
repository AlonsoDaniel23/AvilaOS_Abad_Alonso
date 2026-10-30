/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.model;

/**
 * Administra la Memoria Principal (RAM) del simulador dividida en marcos de pagina.
 * Cumple con la restriccion de arquitectura al gestionar los bloques mediante
 * arreglos nativos en lugar de colecciones de Java.
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
     * @param idProceso ID del proceso.
     * @param cantidadMarcos Número de marcos a reservar.
     * @return true si se completó la asignación, false si no había espacio suficiente.
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
     * Retorna la cantidad de marcos desocupados.
     * @return Número de marcos libres.
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
     * Libera todos los marcos ocupados por un proceso cuando termina o es destruido.
     * @param idProceso ID del proceso a limpiar de la RAM.
     */
    public void liberarMemoriaProceso(int idProceso) {
        for (int i = 0; i < marcos.length; i++) {
            if (marcos[i].isOcupado() && marcos[i].getIdProcesoAsignado() == idProceso) {
                marcos[i].liberar();
            }
        }
    }

    /**
     * Calcula el porcentaje actual de memoria utilizada.
     * @return Porcentaje de uso (0.0 a 100.0) para la interfaz gráfica.
     */
    public double getPorcentajeUso() {
        int ocupados = marcos.length - getMarcosLibres();
        return ((double) ocupados / marcos.length) * 100.0;
    }

    // Getters
    public int getTamanioTotalKB() {
        return tamanioTotalKB;
    }

    public int getTamanioMarcoKB() {
        return tamanioMarcoKB;
    }

    public MarcoMemoria[] getMarcos() {
        return marcos;
    }

    public int getTotalMarcos() {
        return marcos.length;
    }
}