/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.model;

/**
 * Simula el area de intercambio en almacenamiento secundario (Swap).
 * Gestiona bloques equivalentes a marcos de RAM mediante arreglos nativos.
 * 
 * @author Daniel Alonso
 */
public class DiscoSwap {

    private final int tamanioTotalKB;
    private final int tamanioBloqueKB;
    private final boolean[] bloquesOcupados;
    private final int[] procesoPorBloque;

    public DiscoSwap(int tamanioTotalKB, int tamanioBloqueKB) {
        this.tamanioTotalKB = tamanioTotalKB;
        this.tamanioBloqueKB = tamanioBloqueKB;
        int totalBloques = tamanioTotalKB / tamanioBloqueKB;
        this.bloquesOcupados = new boolean[totalBloques];
        this.procesoPorBloque = new int[totalBloques];

        for (int i = 0; i < totalBloques; i++) {
            this.procesoPorBloque[i] = -1;
        }
    }

    /**
     * Reserva una cantidad de bloques libres en Swap para un proceso suspendido.
     */
    public boolean asignarSwapProceso(int idProceso, int cantidadBloques) {
        if (getBloquesLibres() < cantidadBloques) {
            return false;
        }

        int asignados = 0;
        for (int i = 0; i < bloquesOcupados.length && asignados < cantidadBloques; i++) {
            if (!bloquesOcupados[i]) {
                bloquesOcupados[i] = true;
                procesoPorBloque[i] = idProceso;
                asignados++;
            }
        }
        return true;
    }

    /**
     * Libera todos los bloques de Swap asignados a un proceso al reactivarlo o finalizar.
     */
    public void liberarSwapProceso(int idProceso) {
        for (int i = 0; i < bloquesOcupados.length; i++) {
            if (bloquesOcupados[i] && procesoPorBloque[i] == idProceso) {
                bloquesOcupados[i] = false;
                procesoPorBloque[i] = -1;
            }
        }
    }

    /**
     * Busca y reserva el primer bloque disponible en el disco Swap.
     */
    public int asignarBloqueLibre() {
        for (int i = 0; i < bloquesOcupados.length; i++) {
            if (!bloquesOcupados[i]) {
                bloquesOcupados[i] = true;
                return i;
            }
        }
        return -1;
    }

    /**
     * Libera un bloque puntual del disco Swap.
     */
    public void liberarBloque(int indiceBloque) {
        if (indiceBloque >= 0 && indiceBloque < bloquesOcupados.length) {
            bloquesOcupados[indiceBloque] = false;
            procesoPorBloque[indiceBloque] = -1;
        }
    }

    /**
     * Retorna la cantidad de bloques desocupados en almacenamiento secundario.
     */
    public int getBloquesLibres() {
        int libres = 0;
        for (int i = 0; i < bloquesOcupados.length; i++) {
            if (!bloquesOcupados[i]) {
                libres++;
            }
        }
        return libres;
    }

    /**
     * Calcula el porcentaje actual de espacio Swap ocupado.
     */
    public double getPorcentajeUso() {
        int ocupados = bloquesOcupados.length - getBloquesLibres();
        return ((double) ocupados / bloquesOcupados.length) * 100.0;
    }

    // Getters
    public int getTotalBloques() { return bloquesOcupados.length; }
    public int getTamanioTotalKB() { return tamanioTotalKB; }
    public int getTamanioBloqueKB() { return tamanioBloqueKB; }
    public boolean[] getBloquesOcupados() { return bloquesOcupados; }
    public int[] getProcesoPorBloque() { return procesoPorBloque; }
}