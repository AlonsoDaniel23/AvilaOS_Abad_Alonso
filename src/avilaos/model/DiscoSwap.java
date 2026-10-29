/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.model;

/**
 * Simula el area de intercambio en almacenamiento secundario (Memoria Virtual / Swap).
 * Almacena las paginas de los procesos cuando la memoria RAM fisica se encuentra llena.
 * Utiliza un mapa de bits/booleanos mediante arreglos nativos para prescindir de java.util.
 * 
 * @author Daniel Alonso
 */
public class DiscoSwap {
    
    private final int tamanioTotalKB;
    private final int tamanioBloqueKB;
    private final boolean[] bloquesOcupados;

    public DiscoSwap(int tamanioTotalKB, int tamanioBloqueKB) {
        this.tamanioTotalKB = tamanioTotalKB;
        this.tamanioBloqueKB = tamanioBloqueKB;
        int totalBloques = tamanioTotalKB / tamanioBloqueKB;
        this.bloquesOcupados = new boolean[totalBloques];
    }

    /**
     * Busca y reserva el primer bloque disponible en el disco Swap.
     * @return Indice del bloque reservado, o -1 si el espacio de Swap esta agotado.
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
     * Libera un bloque del disco Swap.
     * @param indiceBloque Indice del bloque a liberar.
     */
    public void liberarBloque(int indiceBloque) {
        if (indiceBloque >= 0 && indiceBloque < bloquesOcupados.length) {
            bloquesOcupados[indiceBloque] = false;
        }
    }

    /**
     * Retorna la cantidad de bloques desocupados en almacenamiento secundario.
     * @return Cantidad de bloques libres.
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
     * @return Porcentaje de uso (0.0 a 100.0) para el panel de monitoreo gráfico.
     */
    public double getPorcentajeUso() {
        int ocupados = bloquesOcupados.length - getBloquesLibres();
        return ((double) ocupados / bloquesOcupados.length) * 100.0;
    }

    // Getters
    public int getTotalBloques() {
        return bloquesOcupados.length;
    }

    public int getTamanioTotalKB() {
        return tamanioTotalKB;
    }

    public int getTamanioBloqueKB() {
        return tamanioBloqueKB;
    }
}
