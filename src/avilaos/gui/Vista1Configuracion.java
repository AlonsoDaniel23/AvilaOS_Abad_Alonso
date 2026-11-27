/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.gui;

import avilaos.enums.TipoOperacionES;
import avilaos.enums.TipoProceso;
import avilaos.model.Computador;
import avilaos.model.DiscoSimulado;
import avilaos.model.Proceso;
import avilaos.model.SistemaArchivos;
import avilaos.persistence.GestorJSON;
import avilaos.services.EventLog;
import avilaos.services.MetricasService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Vista 1: Formulario principal de configuracion del computador, disco simulado,
 * carga obligatoria de los 2 JSON oficiales y creacion manual de procesos.
 * 
 * @author Daniel Alonso
 * @version 3.1
 */
public class Vista1Configuracion extends JFrame {

    private JTextField txtDuracionCiclo;
    private JTextField txtRamKB;
    private JTextField txtSwapKB;
    private JTextField txtTotalBloques;
    private JTextField txtBloquesSwap;
    private JTextField txtCabezalInicial;
    private JButton btnAplicarHardware;

    private JTextField txtNombreProceso;
    private JComboBox<String> comboTipoProceso;
    private JComboBox<TipoOperacionES> comboOperacionES;
    private JTextField txtRutaArchivoProceso;
    private JTextField txtRamProceso;
    private JTextField txtPrioridadProceso;
    private JTextField txtDeadlineProceso;
    private JTextField txtCiclosProceso;

    private JLabel lblEstadoJSONFS;
    private JLabel lblEstadoJSONProcesos;
    private JLabel lblResumenCola;

    private Computador computador;
    private DiscoSimulado discoSimulado;
    private SistemaArchivos sistemaArchivos;
    private GestorJSON gestorJSON;
    private EventLog eventLog;
    private MetricasService metricasService;

    private int contadorProcesosId = 1;
    private int procesosCargadosCount = 0;
    private boolean jsonFSCargado = false;
    private boolean jsonProcesosCargado = false;

    private static final Color COLOR_HEADER = new Color(15, 23, 42);
    private static final Color COLOR_BG = new Color(248, 250, 252);
    private static final Color COLOR_CARD_BG = Color.WHITE;
    private static final Color COLOR_TEXT_HEADER = new Color(30, 41, 59);
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);
    private static final Color COLOR_SUCCESS = new Color(16, 185, 129);
    private static final Color COLOR_SECONDARY = new Color(71, 85, 105);

    public Vista1Configuracion() {
        super("ÁvilaOS - Configuración de Hardware, Disco y Escenarios JSON");
        aplicarLookAndFeel();
        inicializarModulosBackend(1024, 512, 200, 40, 50);
        configurarVentana();
        construirInterfaz();
    }

    private void aplicarLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private void inicializarModulosBackend(int ramKB, int swapKB, int totalBloques, int bloquesSwap, int cabezalInicial) {
        this.eventLog = new EventLog(200);
        this.metricasService = new MetricasService();
        this.discoSimulado = new DiscoSimulado(totalBloques, bloquesSwap, cabezalInicial);
        this.discoSimulado.setMetricasService(this.metricasService);
        this.sistemaArchivos = new SistemaArchivos(this.discoSimulado);
        this.computador = new Computador(1, "ÁvilaOS-PC", ramKB, swapKB);
        this.gestorJSON = new GestorJSON();
        this.contadorProcesosId = 1;
        this.procesosCargadosCount = 0;
        this.jsonFSCargado = false;
        this.jsonProcesosCargado = false;
    }

    private void configurarVentana() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1120, 740);
        this.setLocationRelativeTo(null);
        this.getContentPane().setBackground(COLOR_BG);
        this.setLayout(new BorderLayout(0, 0));
    }

    private void construirInterfaz() {
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(COLOR_HEADER);
        panelHeader.setBorder(BorderFactory.createEmptyBorder(18, 25, 18, 25));

        JLabel lblTitulo = new JLabel("ÁVILAOS: SIMULADOR DE SISTEMA OPERATIVO Y GESTIÓN DE ARCHIVOS");
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JLabel lblSubtitulo = new JLabel("Configuración de Memoria, Disco Simulado (SD), Importación JSON e Inserción de Procesos");
        lblSubtitulo.setForeground(new Color(148, 163, 184));
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        panelHeader.add(lblTitulo, BorderLayout.NORTH);
        panelHeader.add(lblSubtitulo, BorderLayout.SOUTH);
        this.add(panelHeader, BorderLayout.NORTH);

        JPanel panelCuerpo = new JPanel(new GridLayout(1, 3, 15, 0));
        panelCuerpo.setBackground(COLOR_BG);
        panelCuerpo.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panelCuerpo.add(crearPanelHardwareDisco());
        panelCuerpo.add(crearPanelCreacionProcesos());
        panelCuerpo.add(crearPanelPersistenciaJSON());

        this.add(panelCuerpo, BorderLayout.CENTER);

        JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        panelInferior.setBackground(COLOR_BG);
        panelInferior.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER));

        JButton btnExportarJSON = crearBotonFlat("Exportar FS a JSON", COLOR_SECONDARY, Color.WHITE);
        JButton btnIniciarSimulacion = crearBotonFlat("Iniciar Simulación  →", COLOR_SUCCESS, Color.WHITE);

        btnExportarJSON.addActionListener(this::accionExportarJSON);
        btnIniciarSimulacion.addActionListener(this::accionIniciarSimulacion);

        panelInferior.add(btnExportarJSON);
        panelInferior.add(btnIniciarSimulacion);
        this.add(panelInferior, BorderLayout.SOUTH);
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

    private JPanel crearPanelHardwareDisco() {
        JPanel panel = crearPanelTarjeta();
        GridBagConstraints gbc = new GridBagConstraints();

        agregarTituloTarjeta(panel, "1. Memoria y Disco Simulado", gbc);

        txtDuracionCiclo = crearCampoTexto("300");
        txtRamKB = crearCampoTexto("1024");
        txtSwapKB = crearCampoTexto("512");
        txtTotalBloques = crearCampoTexto("200");
        txtBloquesSwap = crearCampoTexto("40");
        txtCabezalInicial = crearCampoTexto("50");

        btnAplicarHardware = crearBotonFlat("Aplicar Parámetros Hardware", COLOR_PRIMARY, Color.WHITE);
        btnAplicarHardware.addActionListener(this::accionAplicarHardware);

        agregarFilaFormulario(panel, "Duración Ciclo (ms):", txtDuracionCiclo, gbc, 1);
        agregarFilaFormulario(panel, "Memoria RAM (KB):", txtRamKB, gbc, 2);
        agregarFilaFormulario(panel, "Área Swap RAM (KB):", txtSwapKB, gbc, 3);
        agregarFilaFormulario(panel, "Total Bloques Disco:", txtTotalBloques, gbc, 4);
        agregarFilaFormulario(panel, "Bloques Swap Disco:", txtBloquesSwap, gbc, 5);
        agregarFilaFormulario(panel, "Cabezal Inicial:", txtCabezalInicial, gbc, 6);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 0, 0, 0);
        panel.add(btnAplicarHardware, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.weighty = 1.0;
        panel.add(Box.createGlue(), gbc);

        return panel;
    }

    private JPanel crearPanelCreacionProcesos() {
        JPanel panel = crearPanelTarjeta();
        GridBagConstraints gbc = new GridBagConstraints();

        agregarTituloTarjeta(panel, "2. Crear Proceso Manual", gbc);

        txtNombreProceso = crearCampoTexto("P1");
        comboTipoProceso = new JComboBox<>(new String[]{"CPU Bound", "I/O Bound"});
        estilitarComboBox(comboTipoProceso);

        comboOperacionES = new JComboBox<>(TipoOperacionES.values());
        estilitarComboBox(comboOperacionES);
        comboOperacionES.setEnabled(false);

        txtRutaArchivoProceso = crearCampoTexto("/documento.txt");
        txtRutaArchivoProceso.setEnabled(false);

        comboTipoProceso.addActionListener(e -> {
            boolean esIO = "I/O Bound".equalsIgnoreCase((String) comboTipoProceso.getSelectedItem());
            comboOperacionES.setEnabled(esIO);
            txtRutaArchivoProceso.setEnabled(esIO);
        });

        txtRamProceso = crearCampoTexto("64");
        txtPrioridadProceso = crearCampoTexto("1");
        txtDeadlineProceso = crearCampoTexto("25");
        txtCiclosProceso = crearCampoTexto("8");

        JButton btnAgregar = crearBotonFlat("  + Encolar Proceso  ", COLOR_PRIMARY, Color.WHITE);
        btnAgregar.addActionListener(this::accionAgregarProceso);

        agregarFilaFormulario(panel, "Nombre Proceso:", txtNombreProceso, gbc, 1);
        agregarFilaFormulario(panel, "Tipo Proceso:", comboTipoProceso, gbc, 2);
        agregarFilaFormulario(panel, "Operación E/S:", comboOperacionES, gbc, 3);
        agregarFilaFormulario(panel, "Ruta Archivo:", txtRutaArchivoProceso, gbc, 4);
        agregarFilaFormulario(panel, "RAM (KB):", txtRamProceso, gbc, 5);
        agregarFilaFormulario(panel, "Prioridad (1-5):", txtPrioridadProceso, gbc, 6);
        agregarFilaFormulario(panel, "Deadline (Ciclos):", txtDeadlineProceso, gbc, 7);
        agregarFilaFormulario(panel, "Ciclos CPU:", txtCiclosProceso, gbc, 8);

        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 0, 0, 0);
        panel.add(btnAgregar, gbc);

        return panel;
    }

    private JPanel crearPanelPersistenciaJSON() {
        JPanel panel = crearPanelTarjeta();
        GridBagConstraints gbc = new GridBagConstraints();

        agregarTituloTarjeta(panel, "3. Escenarios JSON Obligatorios", gbc);

        JButton btnCargarJSONFS = crearBotonFlat("📂 1. Cargar JSON Sistema de Archivos", COLOR_PRIMARY, Color.WHITE);
        btnCargarJSONFS.addActionListener(e -> accionCargarArchivoJSON(true));

        lblEstadoJSONFS = new JLabel("Pendiente: JSON de Disco y Archivos");
        lblEstadoJSONFS.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblEstadoJSONFS.setForeground(new Color(239, 68, 68));

        JButton btnCargarJSONProcesos = crearBotonFlat("📂 2. Cargar JSON Procesos y Config", COLOR_PRIMARY, Color.WHITE);
        btnCargarJSONProcesos.addActionListener(e -> accionCargarArchivoJSON(false));

        lblEstadoJSONProcesos = new JLabel("Pendiente: JSON de Procesos");
        lblEstadoJSONProcesos.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblEstadoJSONProcesos.setForeground(new Color(239, 68, 68));

        lblResumenCola = new JLabel("Procesos listos/nuevos en cola: 0");
        lblResumenCola.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblResumenCola.setForeground(COLOR_SUCCESS);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 4, 0);
        panel.add(btnCargarJSONFS, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 15, 0);
        panel.add(lblEstadoJSONFS, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(5, 0, 4, 0);
        panel.add(btnCargarJSONProcesos, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 18, 0);
        panel.add(lblEstadoJSONProcesos, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(8, 0, 8, 0);
        panel.add(lblResumenCola, gbc);

        gbc.gridy = 6;
        gbc.weighty = 1.0;
        panel.add(Box.createGlue(), gbc);

        return panel;
    }

    private void agregarFilaFormulario(JPanel panel, String etiqueta, JComponent comp, GridBagConstraints gbc, int fila) {
        gbc.gridwidth = 1;
        gbc.gridy = fila;
        gbc.weightx = 0.45;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 10);

        JLabel lbl = new JLabel(etiqueta);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(71, 85, 105));
        gbc.gridx = 0;
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.55;
        gbc.insets = new Insets(4, 0, 4, 0);
        panel.add(comp, gbc);
    }

    private JTextField crearCampoTexto(String textoPorDefecto) {
        JTextField txt = new JTextField(textoPorDefecto);
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txt.setPreferredSize(new Dimension(130, 26));
        txt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        return txt;
    }

    private void estilitarComboBox(JComboBox<?> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        combo.setPreferredSize(new Dimension(130, 26));
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
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 34));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));

        return btn;
    }

    private void accionAplicarHardware(ActionEvent e) {
        try {
            int ramKB = Integer.parseInt(txtRamKB.getText().trim());
            int swapKB = Integer.parseInt(txtSwapKB.getText().trim());
            int totalBloques = Integer.parseInt(txtTotalBloques.getText().trim());
            int bloquesSwap = Integer.parseInt(txtBloquesSwap.getText().trim());
            int cabezalInicial = Integer.parseInt(txtCabezalInicial.getText().trim());

            if (ramKB < 16 || swapKB < 16 || totalBloques < 20 || bloquesSwap < 0 || bloquesSwap >= totalBloques) {
                JOptionPane.showMessageDialog(this, "Verifica que RAM/Swap >= 16 KB y Bloques Swap < Total Bloques.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            inicializarModulosBackend(ramKB, swapKB, totalBloques, bloquesSwap, cabezalInicial);
            lblEstadoJSONFS.setText("Pendiente: JSON de Disco y Archivos");
            lblEstadoJSONFS.setForeground(new Color(239, 68, 68));
            lblEstadoJSONProcesos.setText("Pendiente: JSON de Procesos");
            lblEstadoJSONProcesos.setForeground(new Color(239, 68, 68));
            lblResumenCola.setText("Procesos listos/nuevos en cola: 0");

            eventLog.registrar(0, "Hardware configurado: RAM=" + ramKB + "KB, Disco=" + totalBloques + " bloques (Swap=" + bloquesSwap + "), Cabezal=" + cabezalInicial);
            JOptionPane.showMessageDialog(this, "Parámetros de hardware aplicados correctamente.", "Hardware Listo", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingrese números enteros válidos en los parámetros de hardware.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void accionAgregarProceso(ActionEvent e) {
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

            if (ram <= 0 || prioridad <= 0 || deadline < 0 || ciclos <= 0) {
                JOptionPane.showMessageDialog(this, "Los valores numéricos deben ser positivos.", "Validación", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean esIO = "I/O Bound".equalsIgnoreCase((String) comboTipoProceso.getSelectedItem());
            TipoProceso tipoEnum = esIO ? TipoProceso.IO_BOUND : TipoProceso.CPU_BOUND;
            TipoOperacionES opES = esIO ? (TipoOperacionES) comboOperacionES.getSelectedItem() : null;
            String ruta = esIO ? txtRutaArchivoProceso.getText().trim() : "";

            int totalPaginas = Math.max(1, ram / 4);
            Proceso nuevoProceso = new Proceso(
                    contadorProcesosId++,
                    0,
                    nombre,
                    prioridad,
                    ciclos,
                    totalPaginas,
                    tipoEnum,
                    opES,
                    ruta
            );
            nuevoProceso.setDeadline(deadline);

            computador.encolarProceso(nuevoProceso);
            procesosCargadosCount++;
            lblResumenCola.setText("Procesos listos/nuevos en cola: " + procesosCargadosCount);

            eventLog.registrar(0, "Proceso " + nombre + " [" + tipoEnum + "] agregado al computador.");
            txtNombreProceso.setText("P" + contadorProcesosId);
            JOptionPane.showMessageDialog(this, "Proceso '" + nombre + "' agregado a la cola con éxito.", "Proceso Creado", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingrese números enteros válidos.", "Error de Entrada", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void accionCargarArchivoJSON(boolean esDiscoYArchivos) {
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setDialogTitle(esDiscoYArchivos
                ? "Seleccionar JSON de Disco y Sistema de Archivos"
                : "Seleccionar JSON de Procesos");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File archivo = chooser.getSelectedFile();
        // Primera pasada solo para inspeccionar el esquema del archivo seleccionado
        GestorJSON.ResultadoCargaJSON meta = gestorJSON.analizarYCargarJSON(
                archivo.getAbsolutePath(), null, null, null);

        if (!meta.exito) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo leer o parsear el archivo JSON seleccionado.",
                    "Error JSON", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validacion cruzada estricta por tipo de JSON
        if (esDiscoYArchivos && !meta.contieneFS) {
            JOptionPane.showMessageDialog(this,
                    "Archivo incorrecto: este JSON no contiene la estructura del Sistema de Archivos ('fileSystem' / 'diskConfig').\n"
                    + "Seleccione el JSON de Disco y Sistema de Archivos.",
                    "JSON Inválido para Sistema de Archivos", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!esDiscoYArchivos && !meta.contieneProcesos) {
            JOptionPane.showMessageDialog(this,
                    "Archivo incorrecto: este JSON no contiene la lista de Procesos ('processes').\n"
                    + "Seleccione el JSON de Procesos y Configuración.",
                    "JSON Inválido para Procesos", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (esDiscoYArchivos) {
            int totalBloques = (meta.totalBlocks > 0)
                    ? meta.totalBlocks
                    : Integer.parseInt(txtTotalBloques.getText().trim());
            int bloquesSwap = Integer.parseInt(txtBloquesSwap.getText().trim());
            int cabezal = (meta.initialHead >= 0)
                    ? meta.initialHead
                    : Integer.parseInt(txtCabezalInicial.getText().trim());

            if (bloquesSwap > 0 && totalBloques - bloquesSwap < 180) {
                bloquesSwap = Math.max(0, totalBloques - 200);
            }

            txtTotalBloques.setText(String.valueOf(totalBloques));
            txtBloquesSwap.setText(String.valueOf(bloquesSwap));
            txtCabezalInicial.setText(String.valueOf(cabezal));

            this.discoSimulado = new DiscoSimulado(totalBloques, bloquesSwap, cabezal);
            this.discoSimulado.setMetricasService(this.metricasService);
            this.sistemaArchivos = new SistemaArchivos(this.discoSimulado);
        } else {
            if (meta.cycleDurationMs > 0) {
                txtDuracionCiclo.setText(String.valueOf(meta.cycleDurationMs));
            }
            if (meta.ramCapacity > 0) {
                txtRamKB.setText(String.valueOf(meta.ramCapacity));
                int swapKB = Integer.parseInt(txtSwapKB.getText().trim());
                this.computador = new Computador(1, "ÁvilaOS-PC", meta.ramCapacity, swapKB);
                this.procesosCargadosCount = 0;
            }
        }

        GestorJSON.ResultadoCargaJSON cargado = gestorJSON.analizarYCargarJSON(
                archivo.getAbsolutePath(), discoSimulado, sistemaArchivos, computador);

        int cant = cargado.procesosCargados.getTamaño();
        procesosCargadosCount += cant;
        contadorProcesosId += cant;
        lblResumenCola.setText("Procesos listos/nuevos en cola: " + procesosCargadosCount);

        if (esDiscoYArchivos) {
            jsonFSCargado = true;
            lblEstadoJSONFS.setText("✔ FS cargado: " + archivo.getName()
                    + " (Cabezal: " + discoSimulado.getPosicionCabezal() + ")");
            lblEstadoJSONFS.setForeground(COLOR_SUCCESS);
        } else {
            jsonProcesosCargado = true;
            lblEstadoJSONProcesos.setText("✔ Procesos cargados: " + archivo.getName()
                    + " (+" + cant + " proc.)");
            lblEstadoJSONProcesos.setForeground(COLOR_SUCCESS);
        }

        JOptionPane.showMessageDialog(this,
                "Archivo '" + archivo.getName() + "' importado correctamente.",
                "Importación JSON", JOptionPane.INFORMATION_MESSAGE);
    }

    private void accionExportarJSON(ActionEvent e) {
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setDialogTitle("Guardar estado actual del Sistema de Archivos y Disco");
        chooser.setSelectedFile(new File("estado_avilaos.json"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            boolean ok = gestorJSON.exportarSistemaArchivosJSON(chooser.getSelectedFile().getAbsolutePath(), discoSimulado, sistemaArchivos);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Estado exportado a JSON correctamente.", "Exportación JSON", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error al exportar el archivo JSON.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void accionIniciarSimulacion(ActionEvent e) {
        if (!jsonFSCargado || !jsonProcesosCargado) {
            JOptionPane.showMessageDialog(this,
                    "Debe cargar ambos archivos JSON (Sistema de Archivos y Procesos) antes de iniciar la simulación.",
                    "Archivos JSON Requeridos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int duracionMs = 300;
        try {
            duracionMs = Math.max(100, Integer.parseInt(txtDuracionCiclo.getText().trim()));
        } catch (NumberFormatException ignored) {
        }

        eventLog.registrar(0, "Iniciando monitoreo de simulación en tiempo real (Ciclo = " + duracionMs + " ms).");
        Vista2Monitoreo vista2 = new Vista2Monitoreo(
                this.computador,
                this.discoSimulado,
                this.sistemaArchivos,
                this.eventLog,
                this.metricasService,
                duracionMs
        );
        vista2.setVisible(true);
        this.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Vista1Configuracion().setVisible(true));
    }
}