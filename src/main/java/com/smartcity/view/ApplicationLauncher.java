package com.smartcity.view;

import com.smartcity.controller.RecommendationController;
import com.smartcity.model.RecommendationResult;
import com.smartcity.utils.UIConstants;
import com.smartcity.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Application Launcher with initialization and error handling
 */
public class ApplicationLauncher {
    
    private static MainWindow mainWindow;

    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Warning: Could not set system look and feel: " + e.getMessage());
        }

        // Launch on EDT
        SwingUtilities.invokeLater(() -> {
            try {
                mainWindow = new MainWindow();
                mainWindow.setVisible(true);
                
                System.out.println("✓ Application started successfully");
                System.out.println("✓ Window size: " + UIConstants.WINDOW_WIDTH + "x" + UIConstants.WINDOW_HEIGHT);
                
            } catch (Exception e) {
                showErrorDialog("Application Launch Error", 
                    "Failed to start application:\n" + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        });
    }

    /**
     * Show error dialog
     */
    private static void showErrorDialog(String title, String message) {
        JOptionPane.showMessageDialog(
            null,
            message,
            title,
            JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Get main window instance
     */
    public static MainWindow getMainWindow() {
        return mainWindow;
    }
}
