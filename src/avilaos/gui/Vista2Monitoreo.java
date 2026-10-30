/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.gui;

import avilaos.datastructures.ColaProcesos;
import avilaos.enums.EstadoProceso;
import avilaos.model.ClusterManager;
import avilaos.model.Computador;
import avilaos.model.MemoriaRAM;
import avilaos.model.Proceso;
import avilaos.services.EventLog;
import avilaos.services.MetricasService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Vista 2: Panel principal de monitoreo en tiempo real del cluster ÁvilaOS (Requerimiento 6).
 * Permite visualizar el reloj global, CPU, colas de procesos, ocupacion de RAM y selector
 * dinamico de politicas de planificacion por computador.
 * Incluye temporizador Swing para avance de reloj en tiempo real e integración de eventos.
 * Sincronizado con las firmas reales de Proceso.java, MemoriaRAM.java y ClusterManager.java.
 * 
 * @author Daniel Alonso
 * @version 1.4
 */
public class Vista2Monitoreo extends JFrame {
    
    private ClusterManager clusterManager;
    private EventLog eventLog;
    private MetricasService metricasService;
    private int cicloRelojGlobal;
    private boolean simulacionEnEjecucion;

    // Colas de procesos terminados por nodo
    private ColaProcesos[] colasTerminados;

    // Motor de Simulación
    private Timer timerSimulacion;
    private int velocidadCicloMs = 1000;

    // Componentes de Control Global
    private JLabel lblCicloReloj;
    private JButton btnPausarReanudar;
    private JButton btnAvanzarCiclo;
    private JButton btnVerMetricas;

    // Contenedores Principales
    private JPanel panelNodosContainer;
    private JTextArea txtLogEventos;

    // Colores del Tema
    private static final Color COLOR_HEADER = new Color(15, 23, 42);
    private static final Color COLOR_BG = new Color(248, 250, 252);
    private static final Color COLOR_CARD_BG = Color.WHITE;
    private static final Color COLOR_TEXT_HEADER = new Color(30, 41, 59);
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);
    private static final Color COLOR_SUCCESS = new Color(16, 185, 129);
    private static final Color COLOR_DANGER = new Color(239, 68, 68);
    private static final Color COLOR_WARNING = new Color(245, 158, 11);
    private static final Color COLOR_SECONDARY = new Color(71, 85, 105);

    public Vista2Monitoreo(ClusterManager clusterManager, EventLog eventLog, MetricasService metricasService) {
        super("ÁvilaOS V2 - Panel de Monitoreo de Clúster y Procesos");
        this.clusterManager = (clusterManager != null) ? clusterManager : crearClusterEjemplo();
        this.eventLog = (eventLog != null) ? eventLog : new EventLog(100);
        this.metricasService = (metricasService != null) ? metricasService : new MetricasService();
        this.cicloRelojGlobal = 0;
        this.simulacionEnEjecucion = false;

        // Inicializar colas de terminados segun cantidad de nodos
        int cantNodos = this.clusterManager.getCantidadActual();
        this.colasTerminados = new ColaProcesos[Math.max(cantNodos, 10)];
        for (int i = 0; i < this.colasTerminados.length; i++) {
            this.colasTerminados[i] = new ColaProcesos();
        }

        aplicarLookAndFeel();
        configurarVentana();
        construirInterfaz();
        inicializarTimerSimulacion();
    }

    public Vista2Monitoreo() {
        this(null, null, null);
    }

    private ClusterManager crearClusterEjemplo() {
        ClusterManager cm = new ClusterManager(2);
        cm.agregarComputador(new Computador(1, "Computador 1", 1024, 2048));
        cm.agregarComputador(new Computador(2, "Computador 2", 1024, 2048));
        return cm;
    }

    private void aplicarLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private void configurarVentana() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1200, 750);
        this.setLocationRelativeTo(null);
        this.getContentPane().setBackground(COLOR_BG);
        this.setLayout(new BorderLayout(0, 0));
    }

    private void construirInterfaz() {
        this.add(crearPanelEncabezadoReloj(), BorderLayout.NORTH);

        panelNodosContainer = new JPanel(new BorderLayout());
        panelNodosContainer.setOpaque(false);
        panelNodosContainer.add(crearPanelNodosCluster(), BorderLayout.CENTER);
        this.add(panelNodosContainer, BorderLayout.CENTER);

        this.add(crearPanelInferiorLogs(), BorderLayout.SOUTH);
    }

    private void inicializarTimerSimulacion() {
        this.timerSimulacion = new Timer(velocidadCicloMs, (ActionEvent e) -> {
            ejecutarCicloSimulacion();
        });
    }

    private void ejecutarCicloSimulacion() {
        cicloRelojGlobal++;
        lblCicloReloj.setText("Reloj Global del Clúster: Ciclo " + cicloRelojGlobal);

        Computador[] nodos = clusterManager.getComputadores();
        for (int i = 0; i < clusterManager.getCantidadActual(); i++) {
            Computador nodo = nodos[i];
            if (nodo != null) {
                // Verificar si el proceso en CPU acaba de terminar en este ciclo
                Proceso activoPrevio = nodo.getCpu().getProcesoActual();

                // Algoritmo de Despacho (Dispatcher)
                if (!nodo.getCpu().isOcupado() && nodo.tieneProcesosEnEspera()) {
                    Proceso p = nodo.despacharSiguiente();
                    if (p != null) {
                        p.setEstado(EstadoProceso.EJECUCION);
                        nodo.getCpu().cargarProceso(p);
                        eventLog.registrar(cicloRelojGlobal, nodo.getNombre() + ": Proceso " + p.getNombre() + " despachado a CPU bajo política (" + nodo.getNombrePoliticaActual() + ").");
                    }
                }

                // Ejecutar un ciclo de la CPU
                nodo.tick(cicloRelojGlobal);

                // Si la CPU se liberó tras el tick, el proceso finalizó
                if (activoPrevio != null && nodo.getCpu().getProcesoActual() == null) {
                    activoPrevio.setEstado(EstadoProceso.TERMINADO);
                    if (i < colasTerminados.length && colasTerminados[i] != null) {
                        colasTerminados[i].encolar(activoPrevio);
                    }
                    int tiempoRespuesta = cicloRelojGlobal - activoPrevio.getTiempoLlegada();
                    boolean cumplioDeadline = cicloRelojGlobal <= activoPrevio.getDeadline();
                    metricasService.registrarProcesoTerminado(tiempoRespuesta, cumplioDeadline);
                    eventLog.registrar(cicloRelojGlobal, nodo.getNombre() + ": Proceso " + activoPrevio.getNombre() + " ha FINALIZADO su ejecución.");
                }
            }
        }

        // Refresco de componentes en pantalla
        panelNodosContainer.removeAll();
        panelNodosContainer.add(crearPanelNodosCluster(), BorderLayout.CENTER);
        panelNodosContainer.revalidate();
        panelNodosContainer.repaint();

        actualizarVisorLogs();
    }

    private JPanel crearPanelEncabezadoReloj() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_HEADER);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        JPanel panelIzquierda = new JPanel(new GridLayout(2, 1, 0, 2));
        panelIzquierda.setOpaque(false);

        JLabel lblTitulo = new JLabel("ÁVILAOS: MONITOR DE HARDWARE Y CLÚSTER DISTRIBUIDO");
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));

        lblCicloReloj = new JLabel("Reloj Global del Clúster: Ciclo " + cicloRelojGlobal);
        lblCicloReloj.setForeground(COLOR_SUCCESS);
        lblCicloReloj.setFont(new Font("Segoe UI", Font.BOLD, 13));

        panelIzquierda.add(lblTitulo);
        panelIzquierda.add(lblCicloReloj);

        JPanel panelBotonera = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panelBotonera.setOpaque(false);

        btnVerMetricas = crearBotonFlat("📊 Métricas", COLOR_SECONDARY, Color.WHITE);
        btnAvanzarCiclo = crearBotonFlat("Siguiente Ciclo  →", COLOR_PRIMARY, Color.WHITE);
        btnPausarReanudar = crearBotonFlat("Iniciar Simulación", COLOR_SUCCESS, Color.WHITE);

        btnVerMetricas.addActionListener(e -> mostrarVentanaMetricas());
        btnAvanzarCiclo.addActionListener(this::accionAvanzarCiclo);
        btnPausarReanudar.addActionListener(this::accionPausarReanudar);

        panelBotonera.add(btnVerMetricas);
        panelBotonera.add(btnAvanzarCiclo);
        panelBotonera.add(btnPausarReanudar);

        panel.add(panelIzquierda, BorderLayout.WEST);
        panel.add(panelBotonera, BorderLayout.EAST);

        return panel;
    }

    private void mostrarVentanaMetricas() {
        JDialog dialog = new JDialog(this, "Estadísticas y Métricas del Clúster", true);
        dialog.setSize(480, 360);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(15, 15));
        dialog.getContentPane().setBackground(COLOR_BG);

        JPanel panelTitulo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelTitulo.setBackground(COLOR_HEADER);
        JLabel lblTitulo = new JLabel("📊 MÉTRICAS Y RENDIMIENTO DEL SISTEMA");
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panelTitulo.add(lblTitulo);

        JPanel panelGrid = new JPanel(new GridLayout(5, 2, 10, 10));
        panelGrid.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        panelGrid.setOpaque(false);

        int completados = metricasService.getProcesosCompletadosTotales();
        double throughput = metricasService.calcularThroughput(cicloRelojGlobal);
        double tasaDeadlines = metricasService.calcularTasaCumplimientoDeadlines();
        double tiempoRespProm = metricasService.calcularTiempoRespuestaPromedio();
        int producidos = metricasService.getElementosProducidosTotales();
        int consumidos = metricasService.getElementosConsumidosTotales();

        panelGrid.add(crearLabelEtiqueta("Procesos Completados:"));
        panelGrid.add(crearLabelValor(completados + " proceso(s)", COLOR_PRIMARY));

        panelGrid.add(crearLabelEtiqueta("Throughput Global:"));
        panelGrid.add(crearLabelValor(String.format("%.3f proc/ciclo", throughput), COLOR_SUCCESS));

        panelGrid.add(crearLabelEtiqueta("Cumplimiento Deadlines:"));
        panelGrid.add(crearLabelValor(String.format("%.1f %%", tasaDeadlines), tasaDeadlines < 100 ? COLOR_WARNING : COLOR_SUCCESS));

        panelGrid.add(crearLabelEtiqueta("Tiempo Resp. Promedio:"));
        panelGrid.add(crearLabelValor(String.format("%.2f ciclos", tiempoRespProm), COLOR_TEXT_HEADER));

        panelGrid.add(crearLabelEtiqueta("Producción / Consumo:"));
        panelGrid.add(crearLabelValor(producidos + " / " + consumidos, COLOR_SECONDARY));

        JButton btnCerrar = crearBotonFlat("Cerrar", COLOR_PRIMARY, Color.WHITE);
        btnCerrar.addActionListener(e -> dialog.dispose());

        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelBoton.setOpaque(false);
        panelBoton.add(btnCerrar);

        dialog.add(panelTitulo, BorderLayout.NORTH);
        dialog.add(panelGrid, BorderLayout.CENTER);
        dialog.add(panelBoton, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private Component crearPanelNodosCluster() {
        int cantNodos = clusterManager.getCantidadActual();
        Computador[] nodos = clusterManager.getComputadores();

        JPanel panelGrid = new JPanel(new GridLayout(1, Math.max(cantNodos, 1), 15, 0));
        panelGrid.setBackground(COLOR_BG);
        panelGrid.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        for (int i = 0; i < cantNodos; i++) {
            Computador nodo = nodos[i];
            if (nodo != null) {
                JPanel tarjeta = crearTarjetaComputador(nodo, i);
                tarjeta.setPreferredSize(new Dimension(380, 480));
                panelGrid.add(tarjeta);
            }
        }

        JScrollPane scroll = new JScrollPane(panelGrid);
        scroll.setBorder(null);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        return scroll;
    }

    private JPanel crearTarjetaComputador(Computador nodo, int indiceNodo) {
        JPanel tarjeta = new JPanel(new BorderLayout(0, 10));
        tarjeta.setBackground(COLOR_CARD_BG);
        Border bordeLinea = BorderFactory.createLineBorder(COLOR_BORDER, 1, true);
        Border bordeInterno = BorderFactory.createEmptyBorder(12, 12, 12, 12);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(bordeLinea, bordeInterno));

        JPanel headerNodo = new JPanel(new BorderLayout());
        headerNodo.setOpaque(false);

        JLabel lblNombreNodo = new JLabel("🖥️ " + nodo.getNombre() + " (ID: " + nodo.getIdComputador() + ")");
        lblNombreNodo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblNombreNodo.setForeground(COLOR_TEXT_HEADER);

        JComboBox<String> comboPolitica = new JComboBox<>(new String[]{"FCFS", "Round Robin", "EDF", "Prioridades"});
        comboPolitica.setSelectedItem(nodo.getNombrePoliticaActual());
        comboPolitica.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        comboPolitica.setBackground(Color.WHITE);
        comboPolitica.addActionListener(e -> {
            String seleccion = (String) comboPolitica.getSelectedItem();
            nodo.cambiarPolitica(seleccion);
            eventLog.registrar(cicloRelojGlobal, nodo.getNombre() + ": Política de planificación cambiada activamente a " + seleccion);
            actualizarVisorLogs();
        });

        headerNodo.add(lblNombreNodo, BorderLayout.WEST);
        headerNodo.add(comboPolitica, BorderLayout.EAST);
        tarjeta.add(headerNodo, BorderLayout.NORTH);

        JPanel centroNodo = new JPanel();
        centroNodo.setLayout(new BoxLayout(centroNodo, BoxLayout.Y_AXIS));
        centroNodo.setOpaque(false);

        centroNodo.add(crearBloqueCPU(nodo));
        centroNodo.add(Box.createVerticalStrut(10));

        centroNodo.add(crearBloqueRAM(nodo));
        centroNodo.add(Box.createVerticalStrut(10));

        centroNodo.add(crearBloqueColas(nodo, indiceNodo));

        tarjeta.add(centroNodo, BorderLayout.CENTER);

        return tarjeta;
    }

    private JPanel crearBloqueCPU(Computador nodo) {
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 4));
        panel.setBackground(new Color(241, 245, 249));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDER), "Estado CPU", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 11), COLOR_TEXT_HEADER));

        Proceso activo = nodo.getCpu().getProcesoActual();
        String nombreProceso = (activo != null) ? activo.getNombre() + " (Ejecutando)" : "IDLE (Inactivo)";
        String pcValue = (activo != null) ? "PC: " + nodo.getCpu().getProgramCounter() + " / " + activo.getTiempoEjecucion() : "PC: 0";
        String modo = (nodo.getCpu().getModoActual() != null) ? nodo.getCpu().getModoActual().toString() : "USUARIO";
        String deadline = (activo != null) ? "DL: " + activo.getDeadline() + " c." : "N/A";

        panel.add(crearLabelEtiqueta("Proceso en CPU:"));
        panel.add(crearLabelValor(nombreProceso, (activo != null) ? COLOR_PRIMARY : COLOR_TEXT_HEADER));

        panel.add(crearLabelEtiqueta("Program Counter:"));
        panel.add(crearLabelValor(pcValue, COLOR_TEXT_HEADER));

        panel.add(crearLabelEtiqueta("Modo Ejecución:"));
        panel.add(crearLabelValor(modo, modo.contains("KERNEL") ? COLOR_DANGER : COLOR_SUCCESS));

        panel.add(crearLabelEtiqueta("Deadline Restante:"));
        panel.add(crearLabelValor(deadline, COLOR_WARNING));

        return panel;
    }

    private JPanel crearBloqueRAM(Computador nodo) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setOpaque(false);

        MemoriaRAM ram = nodo.getRam();
        int ramTotal = (ram != null) ? ram.getTamanioTotalKB() : 1024;
        int marcosOcupados = (ram != null) ? (ram.getTotalMarcos() - ram.getMarcosLibres()) : 0;
        int ramUsada = (ram != null) ? (marcosOcupados * ram.getTamanioMarcoKB()) : 0;
        int porcentaje = (ram != null) ? (int) ram.getPorcentajeUso() : 0;

        JLabel lblRam = new JLabel("Memoria RAM: " + ramUsada + " KB / " + ramTotal + " KB (" + porcentaje + "%)");
        lblRam.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblRam.setForeground(COLOR_TEXT_HEADER);

        JProgressBar barRam = new JProgressBar(0, 100);
        barRam.setValue(porcentaje);
        barRam.setStringPainted(true);
        barRam.setForeground(porcentaje > 80 ? COLOR_DANGER : COLOR_PRIMARY);

        panel.add(lblRam, BorderLayout.NORTH);
        panel.add(barRam, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearBloqueColas(Computador nodo, int indiceNodo) {
        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.setOpaque(false);

        // 1. Obtener procesos en Cola Nuevos
        int numNuevos = (nodo.getColaNuevos() != null) ? nodo.getColaNuevos().getTamaño() : 0;
        String textoNuevos = (numNuevos > 0) ? "En espera: " + numNuevos + " proc." : "Sin procesos";

        // 2. Obtener procesos en Cola Listos
        int numProcesosCola = (nodo.getColaProcesos() != null) ? nodo.getColaProcesos().getTamaño() : 0;
        String textoListos = (numProcesosCola > 0) ? "En cola: " + numProcesosCola + " proc." : "Sin procesos";

        // 3. Obtener procesos en Cola Terminados
        int numTerminados = (indiceNodo < colasTerminados.length && colasTerminados[indiceNodo] != null) ? colasTerminados[indiceNodo].getTamaño() : 0;
        String textoTerminados = (numTerminados > 0) ? "Finalizados: " + numTerminados : "Ninguno";

        panel.add(crearCajaCola("Cola Nuevos (" + numNuevos + ")", textoNuevos, COLOR_PRIMARY));
        panel.add(crearCajaCola("Cola Listos (" + numProcesosCola + ")", textoListos, COLOR_SUCCESS));
        panel.add(crearCajaCola("Cola Bloqueados", "Sin bloqueos", COLOR_DANGER));
        panel.add(crearCajaCola("Cola Terminados (" + numTerminados + ")", textoTerminados, COLOR_SECONDARY));

        return panel;
    }

    private JPanel crearCajaCola(String titulo, String contenidoTextual, Color colorBorde) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(colorBorde, 1), titulo, TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 10), colorBorde));

        JTextArea txt = new JTextArea((contenidoTextual == null || contenidoTextual.isEmpty()) ? "Sin procesos" : contenidoTextual);
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        txt.setEditable(false);
        txt.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(txt);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(120, 65));

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelInferiorLogs() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(COLOR_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        JLabel lblTituloLogs = new JLabel("📜 Registro de Auditoría de Eventos (EventLog)");
        lblTituloLogs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTituloLogs.setForeground(COLOR_TEXT_HEADER);

        txtLogEventos = new JTextArea(5, 80);
        txtLogEventos.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtLogEventos.setEditable(false);
        txtLogEventos.setBackground(COLOR_CARD_BG);

        JScrollPane scrollLog = new JScrollPane(txtLogEventos);
        scrollLog.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        panel.add(lblTituloLogs, BorderLayout.NORTH);
        panel.add(scrollLog, BorderLayout.CENTER);

        actualizarVisorLogs();

        return panel;
    }

    private JLabel crearLabelEtiqueta(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(new Color(100, 116, 139));
        return lbl;
    }

    private JLabel crearLabelValor(String texto, Color color) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(color);
        return lbl;
    }

    private JButton crearBotonFlat(String texto, Color bg, Color fg) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(bg.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(bg.brighter());
                } else {
                    g2.setColor(bg);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 34));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        return btn;
    }

    private void accionAvanzarCiclo(ActionEvent e) {
        if (e == null) return;
        ejecutarCicloSimulacion();
    }

    private void accionPausarReanudar(ActionEvent e) {
        if (e == null) return;
        simulacionEnEjecucion = !simulacionEnEjecucion;
        if (simulacionEnEjecucion) {
            btnPausarReanudar.setText("Pausar Simulación");
            btnPausarReanudar.setBackground(COLOR_DANGER);
            timerSimulacion.start();
        } else {
            btnPausarReanudar.setText("Iniciar Simulación");
            btnPausarReanudar.setBackground(COLOR_SUCCESS);
            timerSimulacion.stop();
        }
    }

    private void actualizarVisorLogs() {
        if (eventLog != null && txtLogEventos != null) {
            StringBuilder sb = new StringBuilder();
            String[] eventos = eventLog.obtenerEventos();
            for (int i = 0; i < eventos.length; i++) {
                if (eventos[i] != null) {
                    sb.append(eventos[i]).append("\n");
                }
            }
            txtLogEventos.setText(sb.toString());
            txtLogEventos.setCaretPosition(txtLogEventos.getDocument().getLength());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Vista2Monitoreo vista = new Vista2Monitoreo();
                vista.setVisible(true);
            }
        });
    }
}
