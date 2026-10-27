package avilaos.datastructures;

/**
 * Clase que representa un nodo individual en una estructura de datos enlazada.
 * Almacena datos de tipo genérico (Object) y una referencia al siguiente nodo.
 */
public class Nodo {
    private Object dato; // Almacena la información o el proceso
    private Nodo siguiente; // Apunta al siguiente nodo en la secuencia

    /**
     * Constructor para inicializar un nuevo nodo.
     * @param dato La información que almacenará el nodo.
     */
    public Nodo(Object dato) {
        this.dato = dato;
        this.siguiente = null; // Por defecto, el siguiente nodo es nulo
    }

    // Métodos Getters y Setters para acceder y modificar los atributos

    public Object getDato() {
        return dato;
    }

    public void setDato(Object dato) {
        this.dato = dato;
    }

    public Nodo getSiguiente() {
        return siguiente;
    }

    public void setSiguiente(Nodo siguiente) {
        this.siguiente = siguiente;
    }
}