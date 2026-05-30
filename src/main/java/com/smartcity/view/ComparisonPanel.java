package com.smartcity.view;

import com.smartcity.controller.RecommendationController;
import com.smartcity.model.Locality;
import com.smartcity.utils.UIConstants;
import com.smartcity.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Comparison Panel - Compare multiple localities side by side
 */
public class ComparisonPanel extends JPanel {
    
    private RecommendationController controller;
    private JTable comparisonTable;
    private DefaultTableModel tableModel;

    public ComparisonPanel(RecommendationController controller) {
        this.controller = controller;
        
        setLayout(new BorderLayout());
        setBackground(UIConstants.BACKGROUND_COLOR);
        
        // Header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Table
        createComparisonTable();
        JScrollPane scrollPane = new JScrollPane(comparisonTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
        
        // Footer
        JPanel footerPanel = createFooterPanel();
        add(footerPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = UIUtils.createRoundedPanel(UIConstants.CARD_BACKGROUND);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(UIConstants.DEFAULT_PADDING,
                                                        UIConstants.DEFAULT_PADDING,
                                                        UIConstants.DEFAULT_PADDING,
                                                        UIConstants.DEFAULT_PADDING));
        
        JLabel titleLabel = UIUtils.createTitleLabel("📊 Locality Comparison");
        panel.add(titleLabel, BorderLayout.WEST);
        
        return panel;
    }

    private void createComparisonTable() {
        String[] columnNames = {
            "Locality", "City", "Avg Rent (₹)", "Jobs", "Healthcare", 
            "Transport", "Safety", "Environment", "Lifestyle"
        };
        
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        comparisonTable = new JTable(tableModel);
        comparisonTable.setRowHeight(24);
        comparisonTable.setFont(UIConstants.LABEL_FONT);
        comparisonTable.setShowGrid(true);
        comparisonTable.setGridColor(UIConstants.BORDER_COLOR);
        comparisonTable.getTableHeader().setFont(UIConstants.BUTTON_FONT);
        
        // Column widths
        comparisonTable.getColumn("Locality").setPreferredWidth(100);
        comparisonTable.getColumn("City").setPreferredWidth(80);
        
        populateTable();
    }

    private void populateTable() {
        tableModel.setRowCount(0);
        
        List<Locality> localities = controller.getLastResults().stream()
            .map(r -> r.getLocality())
            .toList();
        
        if (localities.isEmpty()) {
            // Fallback to all localities
            localities = controller.getRecommendationService().getAllLocalities();
        }
        
        DecimalFormat df = new DecimalFormat("0.0");
        
        for (Locality locality : localities) {
            Object[] row = {
                locality.getName(),
                locality.getCity(),
                String.format("₹%,.0f", locality.getAvgRent()),
                df.format(locality.getJobIndex()),
                df.format(locality.getHospitalRating()),
                df.format(locality.getTransportScore()),
                df.format(locality.getSafetyScore()),
                df.format(10 - locality.getPollutionIndex()),
                df.format(locality.getLifestyleScore())
            };
            tableModel.addRow(row);
        }
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, UIConstants.DEFAULT_PADDING, UIConstants.DEFAULT_PADDING));
        panel.setBackground(UIConstants.CARD_BACKGROUND);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER_COLOR));
        
        JButton exportButton = UIUtils.createStyledButton("Export to CSV", UIConstants.PRIMARY_COLOR);
        exportButton.addActionListener(e -> exportToCSV());
        
        JButton refreshButton = UIUtils.createStyledButton("Refresh", UIConstants.PRIMARY_COLOR);
        refreshButton.addActionListener(e -> populateTable());
        
        panel.add(exportButton);
        panel.add(refreshButton);
        
        return panel;
    }

    private void exportToCSV() {
        StringBuilder csv = new StringBuilder();
        
        // Header
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            csv.append(tableModel.getColumnName(i));
            if (i < tableModel.getColumnCount() - 1) {
                csv.append(",");
            }
        }
        csv.append("\n");
        
        // Data
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            for (int j = 0; j < tableModel.getColumnCount(); j++) {
                csv.append(tableModel.getValueAt(i, j));
                if (j < tableModel.getColumnCount() - 1) {
                    csv.append(",");
                }
            }
            csv.append("\n");
        }
        
        // Save to file
        try {
            java.nio.file.Files.write(
                java.nio.file.Paths.get("locality_comparison.csv"),
                csv.toString().getBytes()
            );
            JOptionPane.showMessageDialog(this, 
                "Comparison exported to locality_comparison.csv", 
                "Export Success", 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Error exporting: " + ex.getMessage(), 
                "Export Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
