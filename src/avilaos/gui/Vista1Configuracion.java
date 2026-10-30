/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.gui;

import avilaos.model.ClusterManager;
import avilaos.model.Computador;
import avilaos.model.Proceso;
import avilaos.persistence.GestorPersistencia;
import avilaos.services.EventLog;
import avilaos.services.MetricasService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Vista 1: Formulario principal de configuracion del cluster, creacion de procesos
 * y declaracion de buffers compartidos (Requerimientos 6 y 7).
 * 
 * @author Daniel Alonso
 * @version 2.1
 */
public class Vista1Configuracion extends JFrame {
    
   // Componentes de Configuracion Global
    private JTextField txtDuracionCiclo;
    private JTextField txtLatenciaRed;
    private JComboBox<String> comboNumComputadores;

    // Componentes de Creacion de Procesos
    private JTextField txtNombreProceso;
    private JComboBox<String> comboTipoProceso;
    private JTextField txtRamProceso;
    private JTextField txtPrioridadProceso;
    private JTextField txtDeadlineProceso;
    private JTextField txtCiclosProceso;
    private JComboBox<String> comboNodoDestino;

    // Componentes de Creacion de Buffers
    private JTextField txtNombreBuffer;
    private JTextField txtCapacidadBuffer;
    private JComboBox<String> comboNodoAnfitrion;

    // Modulos del backend
    private ClusterManager clusterManager;
    private GestorPersistencia gestorPersistencia;
    private EventLog eventLog;
    private MetricasService metricasService;

    private int contadorProcesosId = 1;

    // Colores del Tema
    private static final Color COLOR_HEADER = new Color(15, 23, 42);      // Slate Dark
    private static final Color COLOR_BG = new Color(248, 250, 252);        // Off-White
    private static final Color COLOR_CARD_BG = Color.WHITE;
    private static final Color COLOR_TEXT_HEADER = new Color(30, 41, 59);
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);    // Royal Blue
    private static final Color COLOR_SUCCESS = new Color(16, 185, 129);   // Emerald Green
    private static final Color COLOR_SECONDARY = new Color(71, 85, 105);  // Slate Gray

    public Vista1Configuracion() {
        super("ÁvilaOS V2 - Panel de Configuración e Inserción");
        aplicarLookAndFeel();
        inicializarModulosBackend(2);
        configurarVentana();
        construirInterfaz();
    }

    private void aplicarLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private void inicializarModulosBackend(int numNodos) {
        this.clusterManager = new ClusterManager(numNodos);
        for (int i = 1; i <= numNodos; i++) {
            this.clusterManager.agregarComputador(new Computador(i, "Computador " + i, 1024, 2048));
        }
        this.gestorPersistencia = new GestorPersistencia("config_avilaos.csv");
        this.eventLog = new EventLog(100);
        this.metricasService = new MetricasService();
    }

    private void configurarVentana() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1050, 680);
        this.setLocationRelativeTo(null);
        this.getContentPane().setBackground(COLOR_BG);
        this.setLayout(new BorderLayout(0, 0));
    }

    private void construirInterfaz() {
        // Encabezado
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(COLOR_HEADER);
        panelHeader.setBorder(BorderFactory.createEmptyBorder(18, 25, 18, 25));

        JLabel lblTitulo = new JLabel("ÁVILAOS: SIMULADOR DE SISTEMA OPERATIVO DISTRIBUIDO");
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JLabel lblSubtitulo = new JLabel("Módulo de Configuración de Clúster, Inserción de Procesos y Buffers");
        lblSubtitulo.setForeground(new Color(148, 163, 184));
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        panelHeader.add(lblTitulo, BorderLayout.NORTH);
        panelHeader.add(lblSubtitulo, BorderLayout.SOUTH);
        this.add(panelHeader, BorderLayout.NORTH);

        // Panel Central (Tarjetas)
        JPanel panelCuerpo = new JPanel(new GridLayout(1, 3, 15, 0));
        panelCuerpo.setBackground(COLOR_BG);
        panelCuerpo.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panelCuerpo.add(crearPanelConfiguracionGlobal());
        panelCuerpo.add(crearPanelCreacionProcesos());
        panelCuerpo.add(crearPanelCreacionBuffers());

        this.add(panelCuerpo, BorderLayout.CENTER);

        // Panel Inferior (Acciones)
        JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        panelInferior.setBackground(COLOR_BG);
        panelInferior.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER));

        JButton btnGuardarConfig = crearBotonFlat("Guardar CSV", COLOR_SECONDARY, Color.WHITE);
        JButton btnCargarConfig = crearBotonFlat("Cargar CSV", COLOR_SECONDARY, Color.WHITE);
        JButton btnIniciarSimulacion = crearBotonFlat("Iniciar Simulación  →", COLOR_SUCCESS, Color.WHITE);

        btnGuardarConfig.addActionListener(this::accionGuardarCSV);
        btnCargarConfig.addActionListener(this::accionCargarCSV);
        btnIniciarSimulacion.addActionListener(this::accionIniciarSimulacion);

        panelInferior.add(btnGuardarConfig);
        panelInferior.add(btnCargarConfig);
        panelInferior.add(btnIniciarSimulacion);
        this.add(panelInferior, BorderLayout.SOUTH);

        // Sincronizar listas desplegables iniciales
        actualizarCantidadNodos();
    }

    private JPanel crearPanelTarjeta() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_CARD_BG);
        Border bordeLinea = BorderFactory.createLineBorder(COLOR_BORDER, 1, true);
        Border bordeInterno = BorderFactory.createEmptyBorder(15, 15, 15, 15);
        panel.setBorder(BorderFactory.createCompoundBorder(bordeLinea, bordeInterno));
        return panel;
    }

    private void agregarTituloTarjeta(JPanel panel, String titulo, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 15, 0);

        JLabel lbl = new JLabel(titulo);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(COLOR_TEXT_HEADER);
        panel.add(lbl, gbc);

        gbc.gridwidth = 1;
    }

    private JPanel crearPanelConfiguracionGlobal() {
        JPanel panel = crearPanelTarjeta();
        GridBagConstraints gbc = new GridBagConstraints();

        agregarTituloTarjeta(panel, "1. Parámetros del Clúster", gbc);

        txtDuracionCiclo = crearCampoTexto("1000");
        txtLatenciaRed = crearCampoTexto("2");
        comboNumComputadores = new JComboBox<>(new String[]{"2 Computadores", "3 Computadores", "4 Computadores"});
        estilizarComboBox(comboNumComputadores);

        // Listener para reconfigurar la cantidad de nodos dinámicamente
        comboNumComputadores.addActionListener(e -> actualizarCantidadNodos());

        agregarFilaFormulario(panel, "Duración Ciclo (ms):", txtDuracionCiclo, gbc, 1);
        agregarFilaFormulario(panel, "Latencia Red (ciclos):", txtLatenciaRed, gbc, 2);
        agregarFilaFormulario(panel, "Cantidad de Nodos:", comboNumComputadores, gbc, 3);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weighty = 1.0;
        panel.add(Box.createGlue(), gbc);

        return panel;
    }

    private void actualizarCantidadNodos() {
        if (comboNumComputadores == null) return;

        String seleccion = (String) comboNumComputadores.getSelectedItem();
        int numNodos = 2;
        if (seleccion != null) {
            if (seleccion.contains("3")) numNodos = 3;
            else if (seleccion.contains("4")) numNodos = 4;
        }

        // Reconstruir el clusterManager con el número seleccionado
        this.clusterManager = new ClusterManager(numNodos);
        for (int i = 1; i <= numNodos; i++) {
            this.clusterManager.agregarComputador(new Computador(i, "Computador " + i, 1024, 2048));
        }

        // Actualizar selector de nodo destino
        if (comboNodoDestino != null) {
            comboNodoDestino.removeAllItems();
            comboNodoDestino.addItem("Asignación Automática");
            for (int i = 1; i <= numNodos; i++) {
                comboNodoDestino.addItem("Nodo " + i);
            }
        }

        // Actualizar selector de nodo anfitrión de buffers
        if (comboNodoAnfitrion != null) {
            comboNodoAnfitrion.removeAllItems();
            for (int i = 1; i <= numNodos; i++) {
                comboNodoAnfitrion.addItem("Nodo " + i);
            }
        }

        if (eventLog != null) {
            eventLog.registrar(0, "Clúster reconfigurado dinámicamente a " + numNodos + " nodos.");
        }
    }

    private JPanel crearPanelCreacionProcesos() {
        JPanel panel = crearPanelTarjeta();
        GridBagConstraints gbc = new GridBagConstraints();

        agregarTituloTarjeta(panel, "2. Crear Proceso", gbc);

        txtNombreProceso = crearCampoTexto("P1");
        comboTipoProceso = new JComboBox<>(new String[]{"CPU Bound", "I/O Bound", "Productor", "Consumidor"});
        estilizarComboBox(comboTipoProceso);
        txtRamProceso = crearCampoTexto("128");
        txtPrioridadProceso = crearCampoTexto("1");
        txtDeadlineProceso = crearCampoTexto("20");
        txtCiclosProceso = crearCampoTexto("10");
        comboNodoDestino = new JComboBox<>(new String[]{"Asignación Automática", "Nodo 1", "Nodo 2"});
        estilizarComboBox(comboNodoDestino);

        JButton btnAgregar = crearBotonFlat("  + Agregar Proceso  ", COLOR_PRIMARY, Color.WHITE);
        btnAgregar.addActionListener(this::accionAgregarProceso);

        agregarFilaFormulario(panel, "Nombre Proceso:", txtNombreProceso, gbc, 1);
        agregarFilaFormulario(panel, "Tipo:", comboTipoProceso, gbc, 2);
        agregarFilaFormulario(panel, "RAM (KB):", txtRamProceso, gbc, 3);
        agregarFilaFormulario(panel, "Prioridad (1-5):", txtPrioridadProceso, gbc, 4);
        agregarFilaFormulario(panel, "Deadline (Ciclos):", txtDeadlineProceso, gbc, 5);
        agregarFilaFormulario(panel, "Ciclos Requeridos:", txtCiclosProceso, gbc, 6);
        agregarFilaFormulario(panel, "Nodo Destino:", comboNodoDestino, gbc, 7);

        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 0, 0, 0);
        panel.add(btnAgregar, gbc);

        return panel;
    }

    private JPanel crearPanelCreacionBuffers() {
        JPanel panel = crearPanelTarjeta();
        GridBagConstraints gbc = new GridBagConstraints();

        agregarTituloTarjeta(panel, "3. Buffer Compartido", gbc);

        txtNombreBuffer = crearCampoTexto("Buffer_B1");
        txtCapacidadBuffer = crearCampoTexto("5");
        comboNodoAnfitrion = new JComboBox<>(new String[]{"Nodo 1", "Nodo 2"});
        estilizarComboBox(comboNodoAnfitrion);

        JButton btnAgregar = crearBotonFlat("  + Agregar Buffer  ", COLOR_PRIMARY, Color.WHITE);
        btnAgregar.addActionListener(this::accionAgregarBuffer);

        agregarFilaFormulario(panel, "Nombre Buffer:", txtNombreBuffer, gbc, 1);
        agregarFilaFormulario(panel, "Capacidad (Elem):", txtCapacidadBuffer, gbc, 2);
        agregarFilaFormulario(panel, "Nodo Anfitrión:", comboNodoAnfitrion, gbc, 3);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 0, 0, 0);
        panel.add(btnAgregar, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.weighty = 1.0;
        panel.add(Box.createGlue(), gbc);

        return panel;
    }

    private void agregarFilaFormulario(JPanel panel, String etiqueta, JComponent comp, GridBagConstraints gbc, int fila) {
        gbc.gridwidth = 1;
        gbc.gridy = fila;
        gbc.weightx = 0.4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 10);

        JLabel lbl = new JLabel(etiqueta);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(71, 85, 105));
        gbc.gridx = 0;
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        gbc.insets = new Insets(4, 0, 4, 0);
        panel.add(comp, gbc);
    }

    private JTextField crearCampoTexto(String textoPorDefecto) {
        JTextField txt = new JTextField(textoPorDefecto);
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txt.setPreferredSize(new Dimension(130, 28));
        txt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        return txt;
    }

    private void estilizarComboBox(JComboBox<?> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        combo.setPreferredSize(new Dimension(130, 28));
        combo.setBackground(Color.WHITE);
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
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 36));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));

        return btn;
    }

    private void accionAgregarProceso(ActionEvent e) {
        if (e == null) return;
        try {
            String nombre = txtNombreProceso.getText().trim();
            if (nombre.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre del proceso no puede estar vacío.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int ram = Integer.parseInt(txtRamProceso.getText().trim());
            int prioridad = Integer.parseInt(txtPrioridadProceso.getText().trim());
            int deadline = Integer.parseInt(txtDeadlineProceso.getText().trim());
            int ciclos = Integer.parseInt(txtCiclosProceso.getText().trim());

            if (ram <= 0 || prioridad <= 0 || deadline <= 0 || ciclos <= 0) {
                JOptionPane.showMessageDialog(this, "Los valores numéricos deben ser mayores a cero.", "Validación", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Calcular total de páginas según la RAM (4 KB por marco)
            int totalPaginas = Math.max(1, ram / 4);

            // Instanciación usando el constructor de 6 parámetros de Proceso.java
            Proceso nuevoProceso = new Proceso(contadorProcesosId++, 0, nombre, prioridad, ciclos, totalPaginas);
            nuevoProceso.setDeadline(deadline);

            // Asignación dinámica de nodo por ID
            String dest = (String) comboNodoDestino.getSelectedItem();
            Computador nodoDestino = null;

            if (dest != null && dest.startsWith("Nodo ")) {
                try {
                    int idNodo = Integer.parseInt(dest.replace("Nodo ", "").trim());
                    nodoDestino = clusterManager.buscarPorId(idNodo);
                } catch (Exception ex) {
                    nodoDestino = clusterManager.obtenerNodoMenosCargado();
                }
            } else {
                nodoDestino = clusterManager.obtenerNodoMenosCargado();
            }

            if (nodoDestino != null) {
                nodoDestino.encolarProceso(nuevoProceso);
                eventLog.registrar(0, "Proceso " + nombre + " (" + ram + " KB, " + ciclos + " ciclos) asignado a " + nodoDestino.getNombre());
                JOptionPane.showMessageDialog(this, "Proceso '" + nombre + "' asignado a " + nodoDestino.getNombre() + " con éxito.", "Proceso Creado", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "No hay nodos disponibles para asignar el proceso.", "Error", JOptionPane.ERROR_MESSAGE);
            }

            txtNombreProceso.setText("P" + contadorProcesosId);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingrese números enteros válidos.", "Error de Entrada", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void accionAgregarBuffer(ActionEvent e) {
        if (e == null) return;
        try {
            String nombre = txtNombreBuffer.getText().trim();
            int capacidad = Integer.parseInt(txtCapacidadBuffer.getText().trim());

            if (nombre.isEmpty() || capacidad <= 0) {
                JOptionPane.showMessageDialog(this, "Ingrese datos válidos para el buffer.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            eventLog.registrar(0, "Buffer " + nombre + " creado en " + comboNodoAnfitrion.getSelectedItem());
            JOptionPane.showMessageDialog(this, "Buffer '" + nombre + "' configurado correctamente.", "Buffer Creado", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "La capacidad debe ser un número entero válido.", "Error de Entrada", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void accionGuardarCSV(ActionEvent e) {
        if (e == null) return;
        try {
            int duracion = Integer.parseInt(txtDuracionCiclo.getText().trim());
            int latencia = Integer.parseInt(txtLatenciaRed.getText().trim());
            boolean exito = gestorPersistencia.guardarConfiguracion(duracion, latencia, clusterManager);
            if (exito) {
                JOptionPane.showMessageDialog(this, "Configuración exportada a 'config_avilaos.csv'.", "Persistencia CSV", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Verifique la duración de ciclo y latencia de red.", "Error de Entrada", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void accionCargarCSV(ActionEvent e) {
        if (e == null) return;
        ClusterManager cargado = gestorPersistencia.cargarConfiguracionCluster(4);
        if (cargado != null) {
            this.clusterManager = cargado;
            JOptionPane.showMessageDialog(this, "Configuración cargada desde CSV. Nodos activos: " + cargado.getCantidadActual(), "Persistencia CSV", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "No se encontró el archivo 'config_avilaos.csv'.", "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void accionIniciarSimulacion(ActionEvent e) {
        if (e == null) return;

        eventLog.registrar(0, "Configuración completada. Transicionando a la Vista de Monitoreo.");

        // Abrir la Vista 2 con la instancia real de los módulos
        Vista2Monitoreo vista2 = new Vista2Monitoreo(this.clusterManager, this.eventLog, this.metricasService);
        vista2.setVisible(true);

        this.dispose();
    }

    // Getters
    public ClusterManager getClusterManager() { return clusterManager; }
    public GestorPersistencia getGestorPersistencia() { return gestorPersistencia; }
    public EventLog getEventLog() { return eventLog; }
    public MetricasService getMetricasService() { return metricasService; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Vista1Configuracion vista = new Vista1Configuracion();
                vista.setVisible(true);
            }
        });
    }
}