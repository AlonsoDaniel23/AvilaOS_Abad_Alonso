package avilaos.model;

import avilaos.datastructures.ListaEnlazada;
import avilaos.enums.PermisoArchivo;

/**
 * Representa un directorio (carpeta) dentro del arbol jerarquico de AvilaOS.
 * Contiene una lista enlazada propia de entradas hijas (archivos y subdirectorios)
 * sin utilizar colecciones de java.util.
 * 
 * @author Gustavo Abad
 */
public class Directorio extends EntradaSistemaArchivos {

    private ListaEnlazada hijos; // Lista enlazada de EntradaSistemaArchivos contenidas en este directorio

    /**
     * Constructor que inicializa un directorio vacio.
     * 
     * @param nombre Nombre del directorio.
     * @param propietario Usuario propietario del directorio.
     * @param permiso Nivel de acceso (PUBLICO o PRIVADO).
     * @param padre Directorio padre (null si es el directorio raiz "/").
     */
    public Directorio(String nombre, String propietario, PermisoArchivo permiso, Directorio padre) {
        super(nombre, propietario, permiso, padre);
        this.hijos = new ListaEnlazada();
    }

    /**
     * Agrega una nueva entrada (Archivo o Directorio) como hija de este directorio,
     * siempre que no exista ya otro elemento con el mismo nombre.
     * 
     * @param entrada Archivo o subdirectorio a insertar.
     * @return true si se agrego correctamente, false si ya existia un nombre duplicado.
     */
    public boolean agregarHijo(EntradaSistemaArchivos entrada) {
        if (entrada == null || buscarHijo(entrada.getNombre()) != null) {
            return false;
        }
        entrada.setPadre(this);
        hijos.insertarFinal(entrada);
        return true;
    }

    /**
     * Busca un hijo directo por su nombre exacto (sin distinguir mayusculas/minusculas).
     * 
     * @param nombreHijo Nombre del archivo o subdirectorio a buscar.
     * @return La EntradaSistemaArchivos encontrada, o null si no existe.
     */
    public EntradaSistemaArchivos buscarHijo(String nombreHijo) {
        if (nombreHijo == null || hijos == null) {
            return null;
        }
        for (int i = 0; i < hijos.getTamaño(); i++) {
            EntradaSistemaArchivos actual = (EntradaSistemaArchivos) hijos.obtener(i);
            if (actual != null && actual.getNombre().equalsIgnoreCase(nombreHijo.trim())) {
                return actual;
            }
        }
        return null;
    }

    /**
     * Elimina un hijo directo de la lista enlazada reconstruyendo la secuencia sin el elemento.
     * 
     * @param nombreHijo Nombre del hijo a remover.
     * @return true si fue eliminado, false si no se encontro.
     */
    public boolean eliminarHijo(String nombreHijo) {
        if (nombreHijo == null || hijos == null || hijos.estaVacia()) {
            return false;
        }

        boolean eliminado = false;
        ListaEnlazada nuevaLista = new ListaEnlazada();

        // Reconstruir la lista enlazada omitiendo la entrada a eliminar
        for (int i = 0; i < hijos.getTamaño(); i++) {
            EntradaSistemaArchivos actual = (EntradaSistemaArchivos) hijos.obtener(i);
            if (actual != null) {
                if (!eliminado && actual.getNombre().equalsIgnoreCase(nombreHijo.trim())) {
                    actual.setPadre(null);
                    eliminado = true;
                } else {
                    nuevaLista.insertarFinal(actual);
                }
            }
        }

        this.hijos = nuevaLista;
        return eliminado;
    }

    @Override
    public boolean esDirectorio() {
        return true;
    }

    @Override
    public int getTamanoBloques() {
        int total = 0;
        for (int i = 0; i < hijos.getTamaño(); i++) {
            EntradaSistemaArchivos actual = (EntradaSistemaArchivos) hijos.obtener(i);
            if (actual != null) {
                total += actual.getTamanoBloques();
            }
        }
        return total;
    }

    // ==================== GETTERS ====================

    public ListaEnlazada getHijos() {
        return hijos;
    }
}