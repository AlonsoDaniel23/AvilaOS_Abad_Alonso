package avilaos;

import avilaos.model.Proceso;
import avilaos.services.EventLog;
import avilaos.services.MetricasService;
import avilaos.synchronization.BufferCircular;
import avilaos.synchronization.RelojGlobal;
import avilaos.scheduling.PlanificadorEDF;

/**
 * Punto de entrada principal para pruebas de integracion de AvilaOS.
 * 
 * @author Equipo AvilaOS
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=== Iniciando Simulacion AvilaOS ===");

        // 1. Inicializar servicios globales
        MetricasService metricas = new MetricasService();
        EventLog log = new EventLog(50);
        RelojGlobal reloj = new RelojGlobal(100); // 100ms por ciclo

        // 2. Instanciar Buffer Circular (Productor-Consumidor) y Planificador EDF
        BufferCircular buffer = new BufferCircular(5, metricas);
        PlanificadorEDF planificadorEDF = new PlanificadorEDF();

        // 3. Crear procesos usando el constructor de Daniel:
        // Proceso(id, idPadre, nombre, prioridad, tiempoEjecucion, totalPaginas)
        Proceso p1 = new Proceso(1, 0, "Proceso_A", 1, 5, 4);
        Proceso p2 = new Proceso(2, 0, "Proceso_B", 2, 3, 4);
        
        // Asignar deadline para EDF
        p1.setDeadline(10);
        p2.setDeadline(5); // Deadline mas cercano

        // 4. Probar encolado en Buffer y Planificador
        buffer.producir(p1);
        buffer.producir(p2);
        
        planificadorEDF.agregarProceso(buffer.consumir());
        planificadorEDF.agregarProceso(buffer.consumir());

        log.registrar(reloj.getCicloActual(), "Procesos cargados en el planificador EDF");

        // 5. Verificar despacho
        Proceso siguiente = planificadorEDF.despacharSiguiente();
        if (siguiente != null) {
            System.out.println("Proceso despachado: " + siguiente.getNombre());
        }

        System.out.println("Elementos producidos en buffer: " + metricas.getElementosProducidosTotales());
        System.out.println("Elementos consumidos en buffer: " + metricas.getElementosConsumidosTotales());
        System.out.println("=== Prueba finalizada con exito ===");
    }
}