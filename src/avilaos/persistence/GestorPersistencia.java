/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.persistence;

import avilaos.datastructures.ColaProcesos;
import avilaos.datastructures.ListaEnlazada;
import avilaos.enums.TipoProceso;
import avilaos.model.ClusterManager;
import avilaos.model.Computador;
import avilaos.model.Proceso;
import avilaos.synchronization.BufferCircular;

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
     * Guarda la configuración inicial completa del clúster (nodos, buffers, procesos y parámetros globales) en un CSV.
     * @param duracionCicloMs Duración en milisegundos de cada tick de reloj.
     * @param latenciaRedCiclos Latencia de red para accesos remotos.
     * @param cluster Instancia del administrador del clúster a exportar.
     * @return true si se guardó con éxito, false en caso de error de E/S.
     */
    public boolean guardarConfiguracion(int duracionCicloMs, int latenciaRedCiclos, ClusterManager cluster) {
        if (cluster == null) return false;

        File archivo = new File(this.rutaArchivo);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
            // 1. Cabecera y Parámetros Globales
            writer.write("# CONFIGURACION GLOBAL AVILAOS\n");
            writer.write("PARAMETRO,DuracionCicloMs," + duracionCicloMs + "\n");
            writer.write("PARAMETRO,LatenciaRedCiclos," + latenciaRedCiclos + "\n");
            writer.write("PARAMETRO,CantidadComputadores," + cluster.getCantidadActual() + "\n");

            // 2. Detalle por Nodo de Cómputo
            writer.write("# NODOS DE COMPUTO (NODO,ID,Nombre,RAM_KB,Swap_KB)\n");
            Computador[] nodos = cluster.getComputadores();
            for (int i = 0; i < cluster.getCantidadActual(); i++) {
                Computador c = nodos[i];
                if (c != null) {
                    writer.write("NODO," + c.getIdComputador() + "," 
                               + c.getNombre() + "," 
                               + c.getRam().getTamanioTotalKB() + "," 
                               + c.getDiscoSwap().getTamanioTotalKB() + "\n");
                }
            }

            // 3. Detalle de Buffers Registrados
            writer.write("# BUFFERS (BUFFER,Nombre,Capacidad,IdAnfitrion)\n");
            ListaEnlazada listaBuf = cluster.getListaBuffers();
            if (listaBuf != null) {
                for (int i = 0; i < listaBuf.getTamaño(); i++) {
                    BufferCircular b = (BufferCircular) listaBuf.obtener(i);
                    if (b != null) {
                        writer.write("BUFFER," + b.getNombre() + "," 
                                   + b.getCapacidad() + "," 
                                   + b.getIdComputadorAnfitrion() + "\n");
                    }
                }
            }

            // 4. Detalle de Procesos Cargados en cada Computador
            writer.write("# PROCESOS (PROCESO,Id,IdPadre,Nombre,Prioridad,TiempoEjecucion,Tipo,NombreBuffer,IdNodo)\n");
            for (int i = 0; i < cluster.getCantidadActual(); i++) {
                Computador c = nodos[i];
                if (c != null) {
                    exportarColaProcesos(writer, c.getColaNuevos(), c.getIdComputador());
                    exportarColaProcesos(writer, c.getColaListosEfectiva(), c.getIdComputador());
                }
            }

            return true;
        } catch (IOException e) {
            System.err.println("Error al guardar configuración: " + e.getMessage());
            return false;
        }
    }

    private void exportarColaProcesos(BufferedWriter writer, ColaProcesos cola, int idNodo) throws IOException {
        if (cola == null) return;
        for (int k = 0; k < cola.getTamaño(); k++) {
            Proceso p = (Proceso) cola.obtener(k);
            if (p != null) {
                writer.write("PROCESO," + p.getId() + "," 
                           + p.getIdPadre() + "," 
                           + p.getNombre() + "," 
                           + p.getPrioridad() + "," 
                           + p.getTiempoEjecucion() + "," 
                           + p.getTipo() + "," 
                           + p.getNombreBuffer() + "," 
                           + idNodo + "\n");
            }
        }
    }

    /**
     * Carga la configuración completa desde un archivo CSV y reconstruye el ClusterManager.
     * @param capacidadMaximaCluster Capacidad reservada para el arreglo de nodos.
     * @return Objeto ClusterManager reconstruido con nodos, buffers y procesos.
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
                if (linea.startsWith("#") || linea.isEmpty()) {
                    continue;
                }

                String[] partes = linea.split(",");
                if (partes.length < 2) continue;

                String tipoRegistro = partes[0].trim();

                switch (tipoRegistro) {
                    case "PARAMETRO":
                        if (partes.length >= 3 && "LatenciaRedCiclos".equalsIgnoreCase(partes[1])) {
                            cluster.setLatenciaRed(Integer.parseInt(partes[2]));
                        }
                        break;

                    case "NODO":
                        if (partes.length >= 5) {
                            int id = Integer.parseInt(partes[1]);
                            String nombre = partes[2];
                            int ramKB = Integer.parseInt(partes[3]);
                            int swapKB = Integer.parseInt(partes[4]);
                            Computador nodo = new Computador(id, nombre, ramKB, swapKB);
                            cluster.agregarComputador(nodo);
                        }
                        break;

                    case "BUFFER":
                        if (partes.length >= 4) {
                            String nombreBuf = partes[1];
                            int cap = Integer.parseInt(partes[2]);
                            int idAnfitrion = Integer.parseInt(partes[3]);
                            BufferCircular buf = new BufferCircular(nombreBuf, cap, idAnfitrion, null);
                            cluster.registrarBuffer(buf);
                        }
                        break;

                    case "PROCESO":
                        if (partes.length >= 9) {
                            int idProc = Integer.parseInt(partes[1]);
                            int idPadre = Integer.parseInt(partes[2]);
                            String nomProc = partes[3];
                            int prio = Integer.parseInt(partes[4]);
                            int tEjec = Integer.parseInt(partes[5]);
                            TipoProceso tipoEnum = TipoProceso.valueOf(partes[6]);
                            String bufNombre = partes[7];
                            int idNodoAsignado = Integer.parseInt(partes[8]);

                            Proceso p = new Proceso(idProc, idPadre, nomProc, prio, tEjec, 4, tipoEnum, bufNombre);
                            Computador nodoDestino = cluster.buscarPorId(idNodoAsignado);
                            if (nodoDestino != null) {
                                nodoDestino.encolarProceso(p);
                            }
                        }
                        break;

                    default:
                        // Compatibilidad con archivos CSV creados en versiones anteriores (sin etiquetas)
                        if (partes.length == 4) {
                            try {
                                int id = Integer.parseInt(partes[0]);
                                String nombre = partes[1];
                                int ramKB = Integer.parseInt(partes[2]);
                                int swapKB = Integer.parseInt(partes[3]);
                                Computador nodo = new Computador(id, nombre, ramKB, swapKB);
                                cluster.agregarComputador(nodo);
                            } catch (NumberFormatException ignored) {}
                        }
                        break;
                }
            }
            return cluster;
        } catch (Exception e) {
            System.err.println("Error al cargar configuración: " + e.getMessage());
            return null;
        }
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }
}