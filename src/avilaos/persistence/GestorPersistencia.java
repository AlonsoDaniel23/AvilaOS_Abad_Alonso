/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.persistence;

import avilaos.model.ClusterManager;
import avilaos.model.Computador;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Gestiona la lectura y escritura de parametros de configuracion de AvilaOS
 * en archivos CSV sin utilizar el framework java.util.
 * 
 * @author Daniel Alonso
 */
public class GestorPersistencia {
    
    private final String rutaArchivo;

    public GestorPersistencia(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    /**
     * Guarda la configuracion actual del cluster y parametros globales en un archivo CSV.
     * 
     * @param duracionCicloMs Duracion en milisegundos de cada tick de reloj.
     * @param latenciaRedCiclos Latencia de red para accesos remotos.
     * @param cluster Instancia del administrador del cluster a exportar.
     * @return true si se guardo con exito, false en caso de error de E/S.
     */
    public boolean guardarConfiguracion(int duracionCicloMs, int latenciaRedCiclos, ClusterManager cluster) {
        File archivo = new File(this.rutaArchivo);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
            // Cabecera de parametros globales
            writer.write("# CONFIGURACION GLOBAL AVILAOS\n");
            writer.write("DuracionCicloMs," + duracionCicloMs + "\n");
            writer.write("LatenciaRedCiclos," + latenciaRedCiclos + "\n");
            writer.write("CantidadComputadores," + cluster.getCantidadActual() + "\n");
            
            // Detalle por nodo de computo
            writer.write("# NODOS DE COMPUTO (ID,Nombre,RAM_KB,Swap_KB)\n");
            Computador[] nodos = cluster.getComputadores();
            for (int i = 0; i < cluster.getCantidadActual(); i++) {
                Computador c = nodos[i];
                writer.write(c.getIdComputador() + "," 
                           + c.getNombre() + "," 
                           + c.getRam().getTamanioTotalKB() + "," 
                           + c.getDiscoSwap().getTamanioTotalKB() + "\n");
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error al guardar configuracion: " + e.getMessage());
            return false;
        }
    }

    /**
     * Carga la configuracion de nodos desde un archivo CSV y reconstruye el ClusterManager.
     * 
     * @param capacidadMaximaCluster Capacidad reservada para el arreglo de nodos.
     * @return Objeto ClusterManager reconstruido o null si falla la lectura.
     */
    public ClusterManager cargarConfiguracionCluster(int capacidadMaximaCluster) {
        File archivo = new File(this.rutaArchivo);
        if (!archivo.exists()) {
            return null;
        }

        ClusterManager cluster = new ClusterManager(capacidadMaximaCluster);

        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                linea = linea.trim();
                // Omitir lineas vacias y comentarios
                if (linea.startsWith("#") || linea.isEmpty()) {
                    continue;
                }

                String[] partes = linea.split(",");
                // Parseo de renglones con 4 atributos de un nodo Computador
                if (partes.length == 4 && !partes[0].equals("DuracionCicloMs") 
                        && !partes[0].equals("LatenciaRedCiclos") 
                        && !partes[0].equals("CantidadComputadores")) {
                    
                    int id = Integer.parseInt(partes[0]);
                    String nombre = partes[1];
                    int ramKB = Integer.parseInt(partes[2]);
                    int swapKB = Integer.parseInt(partes[3]);

                    Computador nodo = new Computador(id, nombre, ramKB, swapKB);
                    cluster.agregarComputador(nodo);
                }
            }
            return cluster;
        } catch (Exception e) {
            System.err.println("Error al cargar configuracion: " + e.getMessage());
            return null;
        }
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }
}
