package com.smartcity.view;

import com.smartcity.controller.RecommendationController;
import com.smartcity.model.RecommendationResult;
import com.smartcity.utils.UIConstants;
import com.smartcity.utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Main application window - Smart City Recommendation System
 */
public class MainWindow extends JFrame implements RecommendationController.ControllerListener {
    
    private RecommendationController controller;
    private JPanel inputPanel;
    private JPanel resultsPanel;
    private CardLayout cardLayout;
    private JPanel contentPanel;

    public MainWindow() {
        // Initialize controller
        this.controller = new RecommendationController();
        this.controller.addListener(this);
        
        // Setup frame
        setTitle("Smart City Recommendation System - Migration Score Analyzer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(UIConstants.WINDOW_WIDTH, UIConstants.WINDOW_HEIGHT);
        setMinimumSize(new Dimension(UIConstants.MIN_WINDOW_WIDTH, UIConstants.MIN_WINDOW_HEIGHT));
        setLocationRelativeTo(null);
        setResizable(true);
        
        // Setup UI
        setupUI();
    }

    private void setupUI() {
        // Set modern frame appearance
        setBackground(UIConstants.BACKGROUND_COLOR);
        getContentPane().setBackground(UIConstants.BACKGROUND_COLOR);
        
        // Create header panel
        add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Create main container with card layout for switching views
        cardLayout = new CardLayout(UIConstants.DEFAULT_PADDING, UIConstants.DEFAULT_PADDING);
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        contentPanel.setBorder(new EmptyBorder(UIConstants.DEFAULT_PADDING, UIConstants.DEFAULT_PADDING, UIConstants.DEFAULT_PADDING, UIConstants.DEFAULT_PADDING));
        
        // Create input panel
        inputPanel = new InputPanel(controller, cardLayout, contentPanel);
        contentPanel.add(inputPanel, "INPUT");
        
        // Create results panel
        resultsPanel = new ResultsPanel(controller);
        contentPanel.add(resultsPanel, "RESULTS");
        
        // Add content panel
        add(contentPanel, BorderLayout.CENTER);
        
        // Show input panel initially
        cardLayout.show(contentPanel, "INPUT");
    }
    
    /**
     * Creates the header panel with title and branding
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = UIUtils.createGradientPanel(UIConstants.PRIMARY_DARK, UIConstants.PRIMARY_COLOR);
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(
                UIConstants.SPACING_MD,
                UIConstants.SPACING_XL,
                UIConstants.SPACING_MD,
                UIConstants.SPACING_XL));

        JLabel titleLabel = UIUtils.createTitleLabel("Migration City Score Analyzer");
        titleLabel.setForeground(UIConstants.COLOR_WHITE);

        JLabel subtitleLabel = UIUtils.createSmallLabel("Score Analyzer Dashboard");
        subtitleLabel.setForeground(UIConstants.COLOR_WHITE_70);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(subtitleLabel);

        JLabel badge = UIUtils.createSmallLabel("LIVE UI");
        badge.setOpaque(true);
        badge.setBackground(new Color(255, 255, 255, 35));
        badge.setForeground(UIConstants.COLOR_WHITE);
        badge.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(badge, BorderLayout.EAST);

        return headerPanel;
    }

    @Override
    public void onRecommendationsUpdated(List<RecommendationResult> results) {
        // Update results panel when recommendations are calculated
        if (resultsPanel instanceof ResultsPanel) {
            ((ResultsPanel) resultsPanel).displayResults(results);
        }
        cardLayout.show(contentPanel, "RESULTS");
    }

    @Override
    public void onLoadingStateChanged(boolean loading, String message) {
        // Handle loading state changes (could show/hide loading indicator)
        if (loading) {
            setTitle("Smart City Recommendation System - " + message);
        } else {
            setTitle("Smart City Recommendation System - Migration Score Analyzer");
        }
    }

    @Override
    public void onRecommendationError(String message) {
        // Handle recommendation errors (could show error dialog)
        JOptionPane.showMessageDialog(this, message, "Recommendation Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
