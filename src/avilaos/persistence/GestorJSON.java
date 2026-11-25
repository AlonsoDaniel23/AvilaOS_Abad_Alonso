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
 * procesos de AvilaOS, implementado en Java puro sin colecciones de java.util
 * ni librerias externas.
 * 
 * @author Gustavo Abad
 */
public class GestorJSON {

    private int indiceParser; // Cursor global utilizado por el analizador sintactico JSON

    /**
     * Constructor por defecto del gestor de persistencia JSON.
     */
    public GestorJSON() {
        this.indiceParser = 0;
    }

    // =========================================================================
    // 1. EXPORTAR ESTADO ACTUAL DEL SISTEMA DE ARCHIVOS Y DISCO A JSON
    // =========================================================================

    /**
     * Exporta el estado actual del DiscoSimulado y todo el arbol jerarquico del
     * SistemaArchivos a un archivo en formato JSON indentado.
     * 
     * @param rutaArchivoJSON Ruta destino del archivo .json a generar.
     * @param disco Instancia del DiscoSimulado actual.
     * @param sistemaArchivos Instancia del SistemaArchivos con el arbol a serializar.
     * @return true si el archivo JSON se escribio correctamente, false en caso de error.
     */
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

            // 1. Metadatos del estado actual del Disco Simulado
            sb.append("  \"disco\": {\n");
            sb.append("    \"total_bloques\": ").append(disco.getTotalBloques()).append(",\n");
            sb.append("    \"bloques_swap\": ").append(disco.getBloquesSwap()).append(",\n");
            sb.append("    \"posicion_cabezal\": ").append(disco.getPosicionCabezal()).append(",\n");
            sb.append("    \"politica_planificacion\": \"").append(escaparJSON(disco.getNombrePoliticaActual())).append("\",\n");
            sb.append("    \"bloques_libres_archivos\": ").append(disco.getBloquesLibresArchivos()).append(",\n");
            sb.append("    \"bloques_libres_swap\": ").append(disco.getBloquesLibresSwap()).append("\n");
            sb.append("  },\n");

            // 2. Arbol jerarquico completo del Sistema de Archivos desde la raiz "/"
            sb.append("  \"sistema_archivos\": ");
            serializarEntradaRecursiva(sistemaArchivos.getRaiz(), sb, 2);
            sb.append("\n");

            sb.append("}\n");

            writer.write(sb.toString());
            EventLog.getInstancia().registrar("Estado del sistema de archivos exportado a JSON: " + rutaArchivoJSON);
            return true;
        } catch (IOException e) {
            System.err.println("Error al exportar JSON: " + e.getMessage());
            return false;
        }
    }

    /**
     * Serializa recursivamente un nodo del sistema de archivos (Directorio o Archivo)
     * en formato JSON respetando la indentacion.
     * 
     * @param entrada Nodo actual del arbol jerarquico.
     * @param sb Acumulador de texto JSON.
     * @param nivelIndentacion Nivel actual de sangria (en espacios).
     */
    private void serializarEntradaRecursiva(EntradaSistemaArchivos entrada, StringBuilder sb, int nivelIndentacion) {
        if (entrada == null) {
            sb.append("null");
            return;
        }

        String pad = generarEspacios(nivelIndentacion);
        String padInterno = generarEspacios(nivelIndentacion + 2);

        sb.append("{\n");
        sb.append(padInterno).append("\"tipo\": \"").append(entrada.esDirectorio() ? "DIRECTORIO" : "ARCHIVO").append("\",\n");
        sb.append(padInterno).append("\"nombre\": \"").append(escaparJSON(entrada.getNombre())).append("\",\n");
        sb.append(padInterno).append("\"ruta\": \"").append(escaparJSON(entrada.getRutaCompleta())).append("\",\n");
        sb.append(padInterno).append("\"propietario\": \"").append(escaparJSON(entrada.getPropietario())).append("\",\n");
        sb.append(padInterno).append("\"permiso\": \"").append(entrada.getPermiso().name()).append("\"");

        if (!entrada.esDirectorio()) {
            // Serializar atributos especificos de un Archivo
            Archivo arch = (Archivo) entrada;
            sb.append(",\n");
            sb.append(padInterno).append("\"tamano_bloques\": ").append(arch.getTamanoBloques()).append(",\n");
            sb.append(padInterno).append("\"primer_bloque\": ").append(arch.getPrimerBloque()).append(",\n");
            sb.append(padInterno).append("\"contenido\": \"").append(escaparJSON(arch.getContenido())).append("\"\n");
        } else {
            // Serializar arreglo JSON de hijos para un Directorio
            Directorio dir = (Directorio) entrada;
            ListaEnlazada hijos = dir.getHijos();
            sb.append(",\n");
            sb.append(padInterno).append("\"hijos\": [");

            if (hijos != null && !hijos.estaVacia()) {
                sb.append("\n");
                for (int i = 0; i < hijos.getTamaño(); i++) {
                    EntradaSistemaArchivos hijo = (EntradaSistemaArchivos) hijos.obtener(i);
                    sb.append(generarEspacios(nivelIndentacion + 4));
                    serializarEntradaRecursiva(hijo, sb, nivelIndentacion + 4);
                    if (i < hijos.getTamaño() - 1) {
                        sb.append(",");
                    }
                    sb.append("\n");
                }
                sb.append(padInterno).append("]\n");
            } else {
                sb.append("]\n");
            }
        }

        sb.append(pad).append("}");
    }

    // =========================================================================
    // 2. IMPORTAR ESTRUCTURA DE DISCO, ARCHIVOS Y PROCESOS DESDE JSON
    // =========================================================================

    /**
     * Carga desde un archivo JSON la configuracion inicial del disco, la estructura
     * jerarquica de archivos/directorios y los procesos iniciales.
     * 
     * @param rutaArchivoJSON Ruta del archivo .json de entrada.
     * @param disco Instancia de DiscoSimulado donde se asignaran los bloques.
     * @param sistemaArchivos Instancia de SistemaArchivos donde se creara el arbol.
     * @param computador Instancia de Computador donde se encolaran los procesos cargados (puede ser null).
     * @return ColaProcesos con todos los procesos importados desde el JSON, o null si fallo la lectura.
     */
    public ColaProcesos cargarDesdeJSON(String rutaArchivoJSON, DiscoSimulado disco,
            SistemaArchivos sistemaArchivos, Computador computador) {
        String contenidoJSON = leerArchivoTexto(rutaArchivoJSON);
        if (contenidoJSON == null || contenidoJSON.trim().isEmpty()) {
            return null;
        }

        ColaProcesos procesosCargados = new ColaProcesos();

        try {
            // 1. Extraer y aplicar configuracion de disco (si existe bloque "disco")
            String bloqueDisco = extraerValorObjeto(contenidoJSON, "disco");
            if (bloqueDisco != null && disco != null) {
                int cabezal = extraerEntero(bloqueDisco, "posicion_cabezal",
                        extraerEntero(bloqueDisco, "initial_head", disco.getPosicionCabezal()));
                disco.setPosicionCabezal(cabezal);
            }

            // 2. Reconstruir el arbol jerarquico de SistemaArchivos (si existe "sistema_archivos")
            String bloqueFS = extraerValorObjeto(contenidoJSON, "sistema_archivos");
            if (bloqueFS != null && sistemaArchivos != null && disco != null) {
                sistemaArchivos.reiniciarArbol();
                cargarNodoRecursivo(bloqueFS, sistemaArchivos.getRaiz(), disco, true);
            }

            // 3. Cargar lista de procesos iniciales (si existe arreglo "procesos")
            String bloqueProcesos = extraerValorArreglo(contenidoJSON, "procesos");
            if (bloqueProcesos != null) {
                String[] objetosProceso = dividirElementosArregloJSON(bloqueProcesos);
                for (int i = 0; i < objetosProceso.length; i++) {
                    Proceso p = parsearProcesoJSON(objetosProceso[i]);
                    if (p != null) {
                        procesosCargados.encolar(p);
                        if (computador != null) {
                            computador.encolarProceso(p);
                        }
                    }
                }
            }

            EventLog.getInstancia().registrar("Configuracion importada exitosamente desde JSON: " + rutaArchivoJSON
                    + " (" + procesosCargados.getTamaño() + " procesos cargados).");
            return procesosCargados;

        } catch (Exception e) {
            System.err.println("Error al parsear archivo JSON: " + e.getMessage());
            return null;
        }
    }

    /**
     * Reconstruye recursivamente un Directorio o Archivo a partir de su objeto JSON
     * y reserva sus bloques en DiscoSimulado.
     * 
     * @param jsonNodo Subcadena JSON correspondiente al nodo actual.
     * @param padre Directorio padre donde se insertara (o raiz si es el nodo superior).
     * @param disco DiscoSimulado donde se reservaran los bloques del archivo.
     * @param esRaiz true si el objeto JSON representa directamente al directorio raiz "/".
     */
    private void cargarNodoRecursivo(String jsonNodo, Directorio padre, DiscoSimulado disco, boolean esRaiz) {
        if (jsonNodo == null || padre == null) {
            return;
        }

        String tipo = extraerCadena(jsonNodo, "tipo", "DIRECTORIO");
        String nombre = extraerCadena(jsonNodo, "nombre", esRaiz ? "/" : "sin_nombre");
        String propietario = extraerCadena(jsonNodo, "propietario", "admin");
        String permisoStr = extraerCadena(jsonNodo, "permiso", "PUBLICO");
        PermisoArchivo permiso = "PRIVADO".equalsIgnoreCase(permisoStr)
                ? PermisoArchivo.PRIVADO : PermisoArchivo.PUBLICO;

        if ("ARCHIVO".equalsIgnoreCase(tipo)) {
            int tamano = Math.max(1, extraerEntero(jsonNodo, "tamano_bloques",
                    extraerEntero(jsonNodo, "size", 1)));
            int primerBloque = extraerEntero(jsonNodo, "primer_bloque",
                    extraerEntero(jsonNodo, "start_block", -1));
            String contenido = extraerCadena(jsonNodo, "contenido", "");

            // Intentar asignar en el bloque especifico del JSON; si no, buscar bloques libres
            int bloqueAsignado = -1;
            if (primerBloque >= 0 && disco.asignarBloquesEspecificos(nombre, primerBloque, tamano)) {
                bloqueAsignado = primerBloque;
            } else {
                bloqueAsignado = disco.asignarBloquesEncadenados(nombre, tamano);
            }

            if (bloqueAsignado != -1) {
                Archivo archivo = new Archivo(nombre, propietario, permiso, padre, tamano, bloqueAsignado);
                archivo.setContenido(contenido);
                padre.agregarHijo(archivo);
            }
        } else {
            // Nodo de tipo DIRECTORIO
            Directorio dirActual;
            if (esRaiz) {
                padre.setPropietario(propietario);
                padre.setPermiso(permiso);
                dirActual = padre;
            } else {
                dirActual = new Directorio(nombre, propietario, permiso, padre);
                padre.agregarHijo(dirActual);
            }

            // Procesar recursivamente el arreglo de "hijos" si existe
            String arregloHijos = extraerValorArreglo(jsonNodo, "hijos");
            if (arregloHijos != null) {
                String[] hijosJSON = dividirElementosArregloJSON(arregloHijos);
                for (int i = 0; i < hijosJSON.length; i++) {
                    cargarNodoRecursivo(hijosJSON[i], dirActual, disco, false);
                }
            }
        }
    }

    /**
     * Construye un objeto Proceso (PCB) a partir de su definicion en JSON.
     * 
     * @param jsonProc Subcadena JSON con los atributos del proceso.
     * @return Instancia de Proceso configurada.
     */
    private Proceso parsearProcesoJSON(String jsonProc) {
        if (jsonProc == null || jsonProc.trim().isEmpty()) {
            return null;
        }

        int id = extraerEntero(jsonProc, "id", 1);
        int idPadre = extraerEntero(jsonProc, "id_padre", 0);
        String nombre = extraerCadena(jsonProc, "nombre", "P" + id);
        int prioridad = extraerEntero(jsonProc, "prioridad", 1);
        int tiempoEjecucion = Math.max(1, extraerEntero(jsonProc, "tiempo_ejecucion", 5));
        int paginas = Math.max(1, extraerEntero(jsonProc, "paginas", 4));
        int deadline = extraerEntero(jsonProc, "deadline", 0);
        int tiempoLlegada = extraerEntero(jsonProc, "tiempo_llegada", 0);

        String tipoStr = extraerCadena(jsonProc, "tipo", "CPU_BOUND");
        TipoProceso tipo = "IO_BOUND".equalsIgnoreCase(tipoStr)
                ? TipoProceso.IO_BOUND : TipoProceso.CPU_BOUND;

        String opStr = extraerCadena(jsonProc, "operacion_es", "");
        TipoOperacionES operacionES = null;
        if (!opStr.isEmpty()) {
            try {
                operacionES = TipoOperacionES.valueOf(opStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                operacionES = TipoOperacionES.LEER;
            }
        }

        String rutaArchivo = extraerCadena(jsonProc, "ruta_archivo", "");

        Proceso proceso = new Proceso(id, idPadre, nombre, prioridad, tiempoEjecucion,
                paginas, tipo, operacionES, rutaArchivo);
        proceso.setDeadline(deadline);
        proceso.setTiempoLlegada(tiempoLlegada);
        return proceso;
    }

    // =========================================================================
    // 3. UTILIDADES DE PARSEO Y SERIALIZACION JSON NATIVO (SIN JAVA.UTIL)
    // =========================================================================

    /**
     * Lee todo el contenido de un archivo de texto plano desde disco.
     */
    private String leerArchivoTexto(String ruta) {
        if (ruta == null) {
            return null;
        }
        File archivo = new File(ruta.trim());
        if (!archivo.exists()) {
            return null;
        }
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

    /**
     * Extrae el valor String asociado a una clave JSON (ej. "nombre": "valor").
     */
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

    /**
     * Extrae el valor entero asociado a una clave JSON (ej. "tamano_bloques": 4).
     */
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

    /**
     * Extrae la subcadena completa de un objeto JSON {...} asociado a una clave.
     */
    private String extraerValorObjeto(String json, String clave) {
        int posValor = localizarInicioValor(json, clave);
        if (posValor == -1 || posValor >= json.length() || json.charAt(posValor) != '{') {
            return null;
        }
        return extraerBloqueBalanceado(json, posValor, '{', '}');
    }

    /**
     * Extrae la subcadena completa de un arreglo JSON [...] asociado a una clave.
     */
    private String extraerValorArreglo(String json, String clave) {
        int posValor = localizarInicioValor(json, clave);
        if (posValor == -1 || posValor >= json.length() || json.charAt(posValor) != '[') {
            return null;
        }
        return extraerBloqueBalanceado(json, posValor, '[', ']');
    }

    /**
     * Localiza el indice donde comienza el valor de una clave JSON respetando comillas.
     */
    private int localizarInicioValor(String json, String clave) {
        String patron = "\"" + clave + "\"";
        int idx = 0;
        while (idx < json.length()) {
            int encontrado = json.indexOf(patron, idx);
            if (encontrado == -1) {
                return -1;
            }
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

    /**
     * Extrae un bloque delimitado por caracteres de apertura/cierre balanceados ({} o []).
     */
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

    /**
     * Divide los objetos JSON de primer nivel dentro de un arreglo "[ {...}, {...} ]"
     * en un arreglo nativo String[] sin utilizar java.util.
     */
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

        // Convertir ListaEnlazada a arreglo nativo String[]
        String[] resultado = new String[listaTemporal.getTamaño()];
        for (int i = 0; i < listaTemporal.getTamaño(); i++) {
            resultado[i] = (String) listaTemporal.obtener(i);
        }
        return resultado;
    }

    /**
     * Escapa caracteres especiales para producir cadenas JSON validas.
     */
    private String escaparJSON(String texto) {
        if (texto == null) {
            return "";
        }
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

    /**
     * Genera una cadena con 'cantidad' espacios en blanco para indentacion JSON.
     */
    private String generarEspacios(int cantidad) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cantidad; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}