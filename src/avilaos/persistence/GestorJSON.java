package avilaos.persistence;

import avilaos.datastructures.ColaProcesos;
import avilaos.datastructures.ListaEnlazada;
import avilaos.enums.PermisoArchivo;
import avilaos.enums.TipoOperacionES;
import avilaos.enums.TipoProceso;
import avilaos.model.Archivo;
import avilaos.model.Computador;
import avilaos.model.Directorio;
import avilaos.model.DiscoSimulado;
import avilaos.model.EntradaSistemaArchivos;
import avilaos.model.Proceso;
import avilaos.model.SistemaArchivos;
import avilaos.scheduling.PlanificadorDiscoCSCAN;
import avilaos.scheduling.PlanificadorDiscoFIFO;
import avilaos.scheduling.PlanificadorDiscoSCAN;
import avilaos.scheduling.PlanificadorDiscoSSTF;
import avilaos.services.EventLog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Gestiona la importacion y exportacion en formato JSON de la estructura inicial
 * del disco, el arbol jerarquico de directorios/archivos y la configuracion de
 * procesos de AvilaOS (soporta formato oficial del enunciado y formato anidado).
 * 
 * @author Gustavo Abad
 */
public class GestorJSON {

    public GestorJSON() {
    }

    /**
     * Resultado estructurado de la carga de un archivo JSON (hardware, config y procesos).
     */
    public static class ResultadoCargaJSON {
        public boolean exito;
        public boolean contieneFS;
        public boolean contieneProcesos;
        public int totalBlocks = -1;
        public int initialHead = -1;
        public int cycleDurationMs = -1;
        public int swapSizeBlocks = -1;
        public String initialDiskPolicy = "";
        public int ramCapacity = -1;
        public String cpuPolicy = "";
        public int quantum = -1;
        public ColaProcesos procesosCargados = new ColaProcesos();
    }

    // =========================================================================
    // 1. EXPORTAR ESTADO ACTUAL DEL SISTEMA DE ARCHIVOS Y DISCO A JSON
    // =========================================================================

    public boolean exportarSistemaArchivosJSON(String rutaArchivoJSON, DiscoSimulado disco,
            SistemaArchivos sistemaArchivos) {
        if (rutaArchivoJSON == null || rutaArchivoJSON.trim().isEmpty()
                || disco == null || sistemaArchivos == null) {
            return false;
        }

        File archivo = new File(rutaArchivoJSON.trim());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"diskConfig\": {\n");
            sb.append("    \"totalBlocks\": ").append(disco.getTotalBloques()).append(",\n");
            sb.append("    \"initialHead\": ").append(disco.getPosicionCabezal()).append("\n");
            sb.append("  },\n");
            sb.append("  \"fileSystem\": [\n");
            ListaEnlazada listaEntradas = new ListaEnlazada();
            recolectarEntradasPlanas(sistemaArchivos.getRaiz(), listaEntradas);
            for (int i = 0; i < listaEntradas.getTamaño(); i++) {
                EntradaSistemaArchivos ent = (EntradaSistemaArchivos) listaEntradas.obtener(i);
                sb.append("    {\n");
                sb.append("      \"path\": \"").append(escaparJSON(ent.getRutaCompleta())).append("\",\n");
                sb.append("      \"type\": \"").append(ent.esDirectorio() ? "DIRECTORY" : "FILE").append("\",\n");
                if (!ent.esDirectorio()) {
                    Archivo arch = (Archivo) ent;
                    sb.append("      \"startBlock\": ").append(arch.getPrimerBloque()).append(",\n");
                    sb.append("      \"blocks\": ").append(arch.getTamanoBloques()).append(",\n");
                }
                sb.append("      \"owner\": \"").append(escaparJSON(ent.getPropietario())).append("\",\n");
                sb.append("      \"permissions\": \"").append(ent.getPermiso() == PermisoArchivo.PRIVADO ? "private" : "public").append("\"\n");
                sb.append("    }").append(i < listaEntradas.getTamaño() - 1 ? "," : "").append("\n");
            }
            sb.append("  ]\n");
            sb.append("}\n");

            writer.write(sb.toString());
            EventLog.getInstancia().registrar("Estado del sistema de archivos exportado a JSON: " + rutaArchivoJSON);
            return true;
        } catch (IOException e) {
            System.err.println("Error al exportar JSON: " + e.getMessage());
            return false;
        }
    }

    private void recolectarEntradasPlanas(EntradaSistemaArchivos entrada, ListaEnlazada destino) {
        if (entrada == null) return;
        if (!"/".equals(entrada.getRutaCompleta())) {
            destino.insertarFinal(entrada);
        }
        if (entrada.esDirectorio()) {
            ListaEnlazada hijos = ((Directorio) entrada).getHijos();
            for (int i = 0; i < hijos.getTamaño(); i++) {
                recolectarEntradasPlanas((EntradaSistemaArchivos) hijos.obtener(i), destino);
            }
        }
    }

    // =========================================================================
    // 2. IMPORTAR ESTRUCTURA DE DISCO, ARCHIVOS Y PROCESOS DESDE JSON
    // =========================================================================

    public ResultadoCargaJSON analizarYCargarJSON(String rutaArchivoJSON, DiscoSimulado disco,
            SistemaArchivos sistemaArchivos, Computador computador) {
        ResultadoCargaJSON res = new ResultadoCargaJSON();
        String contenidoJSON = leerArchivoTexto(rutaArchivoJSON);
        if (contenidoJSON == null || contenidoJSON.trim().isEmpty()) {
            return res;
        }

        try {
            // A. Detectar configuracion de disco ("diskConfig" oficial o "disco")
            String bloqueDiskConfig = extraerValorObjeto(contenidoJSON, "diskConfig");
            if (bloqueDiskConfig == null) {
                bloqueDiskConfig = extraerValorObjeto(contenidoJSON, "disco");
            }
            if (bloqueDiskConfig != null) {
                res.totalBlocks = extraerEntero(bloqueDiskConfig, "totalBlocks",
                        extraerEntero(bloqueDiskConfig, "total_bloques", -1));
                res.initialHead = extraerEntero(bloqueDiskConfig, "initialHead",
                        extraerEntero(bloqueDiskConfig, "posicion_cabezal", -1));
            }

            // B. Detectar configuracion global y de computador ("globalConfig" y "computer")
            String bloqueGlobal = extraerValorObjeto(contenidoJSON, "globalConfig");
            if (bloqueGlobal != null) {
                res.cycleDurationMs = extraerEntero(bloqueGlobal, "cycleDurationMs", -1);
                res.swapSizeBlocks = extraerEntero(bloqueGlobal, "swapSizeBlocks", -1);
                res.initialDiskPolicy = extraerCadena(bloqueGlobal, "initialDiskPolicy", "");
            }

            String bloqueComp = extraerValorObjeto(contenidoJSON, "computer");
            if (bloqueComp != null) {
                res.ramCapacity = extraerEntero(bloqueComp, "ramCapacity", -1);
                res.cpuPolicy = extraerCadena(bloqueComp, "policy", "");
                res.quantum = extraerEntero(bloqueComp, "quantum", -1);
            }

            // C. Cargar Sistema de Archivos ("fileSystem" arreglo plano oficial o "sistema_archivos" arbol)
            String arregloFSOficial = extraerValorArreglo(contenidoJSON, "fileSystem");
            String objetoFSAnidado = extraerValorObjeto(contenidoJSON, "sistema_archivos");

            if (arregloFSOficial != null) {
                res.contieneFS = true;
                if (sistemaArchivos != null && disco != null) {
                    if (res.initialHead >= 0) {
                        disco.setPosicionCabezal(res.initialHead);
                    }
                    sistemaArchivos.reiniciarArbol();
                    cargarFileSystemPlanoOficial(arregloFSOficial, sistemaArchivos, disco);
                }
            } else if (objetoFSAnidado != null) {
                res.contieneFS = true;
                if (sistemaArchivos != null && disco != null) {
                    if (res.initialHead >= 0) {
                        disco.setPosicionCabezal(res.initialHead);
                    }
                    sistemaArchivos.reiniciarArbol();
                    cargarNodoRecursivo(objetoFSAnidado, sistemaArchivos.getRaiz(), disco, true);
                }
            }

            // D. Cargar Procesos ("processes" oficial o "procesos")
            String bloqueProcesos = extraerValorArreglo(contenidoJSON, "processes");
            if (bloqueProcesos == null) {
                bloqueProcesos = extraerValorArreglo(contenidoJSON, "procesos");
            }
            if (bloqueProcesos != null) {
                res.contieneProcesos = true;
                if (computador != null && !res.cpuPolicy.isEmpty()) {
                    computador.cambiarPolitica(normalizarPoliticaCPU(res.cpuPolicy));
                }
                if (disco != null && !res.initialDiskPolicy.isEmpty()) {
                    aplicarPoliticaDisco(disco, res.initialDiskPolicy);
                }

                String[] objetosProceso = dividirElementosArregloJSON(bloqueProcesos);
                for (int i = 0; i < objetosProceso.length; i++) {
                    Proceso p = parsearProcesoJSON(objetosProceso[i], i + 1);
                    if (p != null) {
                        res.procesosCargados.encolar(p);
                        if (computador != null) {
                            computador.encolarProceso(p);
                        }
                    }
                }
            }

            res.exito = res.contieneFS || res.contieneProcesos || (bloqueDiskConfig != null) || (bloqueGlobal != null);
            if (res.exito && (disco != null || computador != null)) {
                EventLog.getInstancia().registrar("JSON importado: " + rutaArchivoJSON
                        + " (FS=" + res.contieneFS + ", Procesos=" + res.procesosCargados.getTamaño() + ")");
            }
            return res;

        } catch (Exception e) {
            System.err.println("Error al parsear archivo JSON: " + e.getMessage());
            return res;
        }
    }

    public ColaProcesos cargarDesdeJSON(String rutaArchivoJSON, DiscoSimulado disco,
            SistemaArchivos sistemaArchivos, Computador computador) {
        ResultadoCargaJSON res = analizarYCargarJSON(rutaArchivoJSON, disco, sistemaArchivos, computador);
        return res.exito ? res.procesosCargados : null;
    }

    /**
     * Carga el arreglo plano "fileSystem" del formato oficial del enunciado.
     */
    private void cargarFileSystemPlanoOficial(String arregloFS, SistemaArchivos fs, DiscoSimulado disco) {
        String[] items = dividirElementosArregloJSON(arregloFS);
        for (int i = 0; i < items.length; i++) {
            String item = items[i];
            String path = extraerCadena(item, "path", "");
            if (path.isEmpty() || "/".equals(path)) continue;

            String type = extraerCadena(item, "type", "FILE");
            String owner = extraerCadena(item, "owner", "admin");
            String permStr = extraerCadena(item, "permissions", "public");
            PermisoArchivo permiso = "private".equalsIgnoreCase(permStr) || "PRIVADO".equalsIgnoreCase(permStr)
                    ? PermisoArchivo.PRIVADO : PermisoArchivo.PUBLICO;

            int ultimoSlash = path.lastIndexOf('/');
            String rutaPadre = (ultimoSlash <= 0) ? "/" : path.substring(0, ultimoSlash);
            String nombre = path.substring(ultimoSlash + 1);

            Directorio padre = asegurarRutaDirectorios(fs, rutaPadre, owner);

            if ("DIRECTORY".equalsIgnoreCase(type) || "DIRECTORIO".equalsIgnoreCase(type)) {
                if (padre.buscarHijo(nombre) == null) {
                    Directorio nuevoDir = new Directorio(nombre, owner, permiso, padre);
                    padre.agregarHijo(nuevoDir);
                }
            } else {
                int startBlock = extraerEntero(item, "startBlock", extraerEntero(item, "primer_bloque", -1));
                int blocks = Math.max(1, extraerEntero(item, "blocks", extraerEntero(item, "tamano_bloques", 1)));
                String contenido = extraerCadena(item, "content", extraerCadena(item, "contenido", "Archivo " + nombre));

                int bloqueAsignado = -1;
                if (startBlock >= 0 && disco.asignarBloquesEspecificos(nombre, startBlock, blocks)) {
                    bloqueAsignado = startBlock;
                } else {
                    bloqueAsignado = disco.asignarBloquesEncadenados(nombre, blocks);
                }

                if (bloqueAsignado != -1 && padre.buscarHijo(nombre) == null) {
                    Archivo arch = new Archivo(nombre, owner, permiso, padre, blocks, bloqueAsignado);
                    arch.setContenido(contenido);
                    arch.setProcesoCreador("JSON Inicial");
                    disco.asignarColorACadena(bloqueAsignado, arch.getColorDistintivo());
                    padre.agregarHijo(arch);
                }
            }
        }
    }

    private Directorio asegurarRutaDirectorios(SistemaArchivos fs, String rutaPadre, String ownerPorDefecto) {
        if (rutaPadre == null || rutaPadre.isEmpty() || "/".equals(rutaPadre)) {
            return fs.getRaiz();
        }
        String[] partes = rutaPadre.split("/");
        Directorio actual = fs.getRaiz();
        for (int i = 0; i < partes.length; i++) {
            String seg = partes[i].trim();
            if (seg.isEmpty()) continue;
            EntradaSistemaArchivos hijo = actual.buscarHijo(seg);
            if (hijo == null) {
                Directorio creado = new Directorio(seg, ownerPorDefecto, PermisoArchivo.PUBLICO, actual);
                actual.agregarHijo(creado);
                actual = creado;
            } else if (hijo.esDirectorio()) {
                actual = (Directorio) hijo;
            }
        }
        return actual;
    }

    private void cargarNodoRecursivo(String jsonNodo, Directorio padre, DiscoSimulado disco, boolean esRaiz) {
        if (jsonNodo == null || padre == null) return;

        String tipo = extraerCadena(jsonNodo, "tipo", "DIRECTORIO");
        String nombre = extraerCadena(jsonNodo, "nombre", esRaiz ? "/" : "sin_nombre");
        String propietario = extraerCadena(jsonNodo, "propietario", "admin");
        String permisoStr = extraerCadena(jsonNodo, "permiso", "PUBLICO");
        PermisoArchivo permiso = "PRIVADO".equalsIgnoreCase(permisoStr)
                ? PermisoArchivo.PRIVADO : PermisoArchivo.PUBLICO;

        if ("ARCHIVO".equalsIgnoreCase(tipo)) {
            int tamano = Math.max(1, extraerEntero(jsonNodo, "tamano_bloques", extraerEntero(jsonNodo, "size", 1)));
            int primerBloque = extraerEntero(jsonNodo, "primer_bloque", extraerEntero(jsonNodo, "start_block", -1));
            String contenido = extraerCadena(jsonNodo, "contenido", "");

            int bloqueAsignado = -1;
            if (primerBloque >= 0 && disco.asignarBloquesEspecificos(nombre, primerBloque, tamano)) {
                bloqueAsignado = primerBloque;
            } else {
                bloqueAsignado = disco.asignarBloquesEncadenados(nombre, tamano);
            }

            if (bloqueAsignado != -1) {
                Archivo archivo = new Archivo(nombre, propietario, permiso, padre, tamano, bloqueAsignado);
                archivo.setContenido(contenido);
                archivo.setProcesoCreador("JSON Inicial");
                disco.asignarColorACadena(bloqueAsignado, archivo.getColorDistintivo());
                padre.agregarHijo(archivo);
            }
        } else {
            Directorio dirActual;
            if (esRaiz) {
                padre.setPropietario(propietario);
                padre.setPermiso(permiso);
                dirActual = padre;
            } else {
                dirActual = new Directorio(nombre, propietario, permiso, padre);
                padre.agregarHijo(dirActual);
            }

            String arregloHijos = extraerValorArreglo(jsonNodo, "hijos");
            if (arregloHijos != null) {
                String[] hijosJSON = dividirElementosArregloJSON(arregloHijos);
                for (int i = 0; i < hijosJSON.length; i++) {
                    cargarNodoRecursivo(hijosJSON[i], dirActual, disco, false);
                }
            }
        }
    }

    private Proceso parsearProcesoJSON(String jsonProc, int idPorDefecto) {
        if (jsonProc == null || jsonProc.trim().isEmpty()) {
            return null;
        }

        int id = extraerEntero(jsonProc, "id", idPorDefecto);
        int idPadre = extraerEntero(jsonProc, "id_padre", 0);
        String nombre = extraerCadena(jsonProc, "name", extraerCadena(jsonProc, "nombre", "P" + id));
        int prioridad = extraerEntero(jsonProc, "priority", extraerEntero(jsonProc, "prioridad", 1));

        int tiempoEjecucion = Math.max(1, extraerEntero(jsonProc, "instructions",
                extraerEntero(jsonProc, "tiempo_ejecucion", 2)));

        int memoryKB = extraerEntero(jsonProc, "memory", -1);
        int paginas = (memoryKB > 0)
                ? Math.max(1, memoryKB / 4)
                : Math.max(1, extraerEntero(jsonProc, "paginas", 4));

        int deadline = extraerEntero(jsonProc, "deadline", 0);
        int tiempoLlegada = extraerEntero(jsonProc, "tiempo_llegada", 0);

        String targetPath = extraerCadena(jsonProc, "targetPath", extraerCadena(jsonProc, "ruta_archivo", ""));
        String opRaw = extraerCadena(jsonProc, "operation", extraerCadena(jsonProc, "operacion_es", ""));
        String tipoRaw = extraerCadena(jsonProc, "tipo", "");

        TipoProceso tipo;
        if (!tipoRaw.isEmpty()) {
            tipo = "IO_BOUND".equalsIgnoreCase(tipoRaw) ? TipoProceso.IO_BOUND : TipoProceso.CPU_BOUND;
        } else {
            tipo = (!opRaw.isEmpty() || !targetPath.isEmpty()) ? TipoProceso.IO_BOUND : TipoProceso.CPU_BOUND;
        }

        TipoOperacionES operacionES = null;
        if (!opRaw.isEmpty()) {
            operacionES = mapearOperacionES(opRaw);
        } else if (tipo == TipoProceso.IO_BOUND) {
            operacionES = TipoOperacionES.LEER;
        }

        Proceso proceso = new Proceso(id, idPadre, nombre, prioridad, tiempoEjecucion,
                paginas, tipo, operacionES, targetPath);
        proceso.setDeadline(deadline);
        proceso.setTiempoLlegada(tiempoLlegada);
        return proceso;
    }

    private TipoOperacionES mapearOperacionES(String op) {
        String norm = op.trim().toUpperCase();
        switch (norm) {
            case "CREATE":
            case "CREAR":
                return TipoOperacionES.CREAR;
            case "UPDATE":
            case "WRITE":
            case "ACTUALIZAR":
                return TipoOperacionES.ACTUALIZAR;
            case "DELETE":
            case "ELIMINAR":
                return TipoOperacionES.ELIMINAR;
            case "READ":
            case "LEER":
            default:
                return TipoOperacionES.LEER;
        }
    }

    private String normalizarPoliticaCPU(String pol) {
        String p = pol.trim().toUpperCase();
        if (p.equals("RR") || p.contains("ROUND")) return "Round Robin";
        if (p.equals("EDF")) return "EDF";
        if (p.startsWith("PRIO")) return "Prioridades";
        return "FCFS";
    }

    private void aplicarPoliticaDisco(DiscoSimulado disco, String pol) {
        String p = pol.trim().toUpperCase();
        switch (p) {
            case "SSTF":
                disco.cambiarPlanificador(new PlanificadorDiscoSSTF());
                break;
            case "SCAN":
                disco.cambiarPlanificador(new PlanificadorDiscoSCAN());
                break;
            case "C-SCAN":
            case "CSCAN":
                disco.cambiarPlanificador(new PlanificadorDiscoCSCAN());
                break;
            case "FIFO":
            default:
                disco.cambiarPlanificador(new PlanificadorDiscoFIFO());
                break;
        }
    }

    // =========================================================================
    // 3. UTILIDADES DE PARSEO Y SERIALIZACION JSON NATIVO (SIN JAVA.UTIL)
    // =========================================================================

    private String leerArchivoTexto(String ruta) {
        if (ruta == null) return null;
        File archivo = new File(ruta.trim());
        if (!archivo.exists()) return null;
        StringBuilder contenido = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                contenido.append(linea).append("\n");
            }
            return contenido.toString();
        } catch (IOException e) {
            return null;
        }
    }

    private String extraerCadena(String json, String clave, String valorDefecto) {
        int posValor = localizarInicioValor(json, clave);
        if (posValor == -1 || posValor >= json.length() || json.charAt(posValor) != '"') {
            return valorDefecto;
        }
        StringBuilder sb = new StringBuilder();
        int i = posValor + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char sig = json.charAt(i + 1);
                if (sig == 'n') sb.append('\n');
                else if (sig == 't') sb.append('\t');
                else sb.append(sig);
                i += 2;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
                i++;
            }
        }
        return valorDefecto;
    }

    private int extraerEntero(String json, String clave, int valorDefecto) {
        int posValor = localizarInicioValor(json, clave);
        if (posValor == -1 || posValor >= json.length()) {
            return valorDefecto;
        }
        int fin = posValor;
        while (fin < json.length() && (Character.isDigit(json.charAt(fin)) || json.charAt(fin) == '-')) {
            fin++;
        }
        if (fin == posValor) {
            return valorDefecto;
        }
        try {
            return Integer.parseInt(json.substring(posValor, fin));
        } catch (NumberFormatException e) {
            return valorDefecto;
        }
    }

    private String extraerValorObjeto(String json, String clave) {
        int posValor = localizarInicioValor(json, clave);
        if (posValor == -1 || posValor >= json.length() || json.charAt(posValor) != '{') {
            return null;
        }
        return extraerBloqueBalanceado(json, posValor, '{', '}');
    }

    private String extraerValorArreglo(String json, String clave) {
        int posValor = localizarInicioValor(json, clave);
        if (posValor == -1 || posValor >= json.length() || json.charAt(posValor) != '[') {
            return null;
        }
        return extraerBloqueBalanceado(json, posValor, '[', ']');
    }

    private int localizarInicioValor(String json, String clave) {
        String patron = "\"" + clave + "\"";
        int idx = 0;
        while (idx < json.length()) {
            int encontrado = json.indexOf(patron, idx);
            if (encontrado == -1) return -1;
            int cursor = encontrado + patron.length();
            while (cursor < json.length() && Character.isWhitespace(json.charAt(cursor))) {
                cursor++;
            }
            if (cursor < json.length() && json.charAt(cursor) == ':') {
                cursor++;
                while (cursor < json.length() && Character.isWhitespace(json.charAt(cursor))) {
                    cursor++;
                }
                return cursor;
            }
            idx = encontrado + patron.length();
        }
        return -1;
    }

    private String extraerBloqueBalanceado(String texto, int inicio, char apertura, char cierre) {
        int nivel = 0;
        boolean enComillas = false;
        for (int i = inicio; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '"' && (i == 0 || texto.charAt(i - 1) != '\\')) {
                enComillas = !enComillas;
            }
            if (!enComillas) {
                if (c == apertura) {
                    nivel++;
                } else if (c == cierre) {
                    nivel--;
                    if (nivel == 0) {
                        return texto.substring(inicio, i + 1);
                    }
                }
            }
        }
        return null;
    }

    private String[] dividirElementosArregloJSON(String arregloJSON) {
        if (arregloJSON == null || arregloJSON.length() < 2) {
            return new String[0];
        }
        ListaEnlazada listaTemporal = new ListaEnlazada();
        int nivelLlaves = 0;
        int nivelCorchetes = 0;
        boolean enComillas = false;
        int inicioElemento = -1;

        for (int i = 1; i < arregloJSON.length() - 1; i++) {
            char c = arregloJSON.charAt(i);
            if (c == '"' && arregloJSON.charAt(i - 1) != '\\') {
                enComillas = !enComillas;
            }
            if (!enComillas) {
                if (c == '{') {
                    if (nivelLlaves == 0 && nivelCorchetes == 0) {
                        inicioElemento = i;
                    }
                    nivelLlaves++;
                } else if (c == '}') {
                    nivelLlaves--;
                    if (nivelLlaves == 0 && nivelCorchetes == 0 && inicioElemento != -1) {
                        listaTemporal.insertarFinal(arregloJSON.substring(inicioElemento, i + 1));
                        inicioElemento = -1;
                    }
                } else if (c == '[') {
                    nivelCorchetes++;
                } else if (c == ']') {
                    nivelCorchetes--;
                }
            }
        }

        String[] resultado = new String[listaTemporal.getTamaño()];
        for (int i = 0; i < listaTemporal.getTamaño(); i++) {
            resultado[i] = (String) listaTemporal.obtener(i);
        }
        return resultado;
    }

    private String escaparJSON(String texto) {
        if (texto == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:   sb.append(c); break;
            }
        }
        return sb.toString();
    }
}