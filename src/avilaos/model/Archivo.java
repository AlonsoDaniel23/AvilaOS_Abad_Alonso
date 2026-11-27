package avilaos.model;

import avilaos.enums.PermisoArchivo;
import java.awt.Color;

/**
 * Representa un archivo dentro del sistema de archivos jerarquico de AvilaOS.
 * Almacena su tamano en bloques, el indice del primer bloque asignado en el
 * DiscoSimulado (para asignacion encadenada), su proceso creador, color distintivo
 * y su contenido textual.
 * 
 * @author Gustavo Abad
 */
public class Archivo extends EntradaSistemaArchivos {

    private static int contadorColor = 0;
    private static final Color[] PALETA_ARCHIVOS = {
        new Color(37, 99, 235),   // Azul real
        new Color(16, 185, 129),  // Esmeralda
        new Color(139, 92, 246),  // Violeta
        new Color(236, 72, 153),  // Magenta
        new Color(14, 165, 233),  // Cian
        new Color(234, 88, 12),   // Naranja intenso
        new Color(20, 184, 166),  // Turquesa
        new Color(99, 102, 241),  // Indigo
        new Color(132, 204, 22),  // Lima
        new Color(217, 70, 239)   // Fucsia
    };

    private int tamanoBloques;     // Cantidad de bloques fisicos que ocupa el archivo en disco
    private int primerBloque;      // Indice del bloque inicial en DiscoSimulado (-1 si no tiene bloques)
    private String contenido;      // Contenido de datos o texto almacenado en el archivo
    private String procesoCreador; // Nombre o ID del proceso que creo el archivo
    private Color colorDistintivo; // Color unico asociado al archivo en tabla y mapa de disco

    public Archivo(String nombre, String propietario, PermisoArchivo permiso,
            Directorio padre, int tamanoBloques, int primerBloque) {
        this(nombre, propietario, permiso, padre, tamanoBloques, primerBloque, "Sistema");
    }

    public Archivo(String nombre, String propietario, PermisoArchivo permiso,
            Directorio padre, int tamanoBloques, int primerBloque, String procesoCreador) {
        super(nombre, propietario, permiso, padre);
        this.tamanoBloques = Math.max(1, tamanoBloques);
        this.primerBloque = primerBloque;
        this.contenido = "";
        this.procesoCreador = (procesoCreador != null && !procesoCreador.trim().isEmpty())
                ? procesoCreador.trim() : "Sistema";
        this.colorDistintivo = PALETA_ARCHIVOS[Math.abs(contadorColor++) % PALETA_ARCHIVOS.length];
    }

    @Override
    public boolean esDirectorio() {
        return false;
    }

    @Override
    public int getTamanoBloques() {
        return tamanoBloques;
    }

    // ==================== GETTERS Y SETTERS ====================

    public void setTamanoBloques(int tamanoBloques) {
        this.tamanoBloques = Math.max(1, tamanoBloques);
    }

    public int getPrimerBloque() {
        return primerBloque;
    }

    public void setPrimerBloque(int primerBloque) {
        this.primerBloque = primerBloque;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = (contenido != null) ? contenido : "";
    }

    public String getProcesoCreador() {
        return procesoCreador;
    }

    public void setProcesoCreador(String procesoCreador) {
        if (procesoCreador != null && !procesoCreador.trim().isEmpty()) {
            this.procesoCreador = procesoCreador.trim();
        }
    }

    public Color getColorDistintivo() {
        return colorDistintivo;
    }

    public void setColorDistintivo(Color colorDistintivo) {
        if (colorDistintivo != null) {
            this.colorDistintivo = colorDistintivo;
        }
    }
}