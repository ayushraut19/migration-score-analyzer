package com.smartcity.view;

import com.smartcity.controller.RecommendationController;
import com.smartcity.model.UserPreferences;
import com.smartcity.utils.UIConstants;
import com.smartcity.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class InputPanel extends JPanel {

    private static final int BUDGET_STEP = 50000;

    private final RecommendationController controller;
    private final Map<String, JSlider> sliders;

    private JComboBox<String> cityCombo;

    private JSlider budgetSlider;
    private JLabel budgetValueLabel;

    private JSlider safetyThresholdSlider;
    private JSlider budgetMatchSlider;

    private JLabel safetyThresholdValueLabel;
    private JLabel budgetMatchValueLabel;

    public InputPanel(
            RecommendationController controller,
            CardLayout cardLayout,
            JPanel contentPanel
    ) {

        this.controller = controller;
        this.sliders = new HashMap<>();

        setLayout(new BorderLayout());
        setBackground(UIConstants.BACKGROUND_COLOR);

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createScrollableContent(), BorderLayout.CENTER);
    }

    // =========================================================
    // HEADER
    // =========================================================

    private JPanel createHeaderPanel() {

        JPanel header = UIUtils.createGradientPanel(
                new Color(15, 23, 42),
                new Color(37, 99, 235)
        );

        header.setLayout(new BorderLayout());

        header.setBorder(BorderFactory.createEmptyBorder(
                24,
                32,
                24,
                32
        ));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel title = UIUtils.createDisplayLabel(
                "Smart City Finder"
        );

        title.setForeground(Color.WHITE);

        JLabel subtitle = UIUtils.createSecondaryLabel(
                "AI-powered migration recommendation dashboard"
        );

        subtitle.setForeground(UIConstants.COLOR_WHITE_70);

        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(subtitle);

        header.add(textPanel, BorderLayout.WEST);

        return header;
    }

    // =========================================================
    // SCROLL CONTENT
    // =========================================================

    private JComponent createScrollableContent() {

        JPanel content = new JPanel();

        content.setOpaque(false);

        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        content.setBorder(BorderFactory.createEmptyBorder(
                24,
                24,
                24,
                24
        ));

        content.add(createMainFormCard());
        content.add(Box.createVerticalStrut(20));

        content.add(createSmartModeCard());
        content.add(Box.createVerticalStrut(20));

        content.add(createFilterCard());
        content.add(Box.createVerticalStrut(20));

        content.add(createWeightsCard());
        content.add(Box.createVerticalStrut(28));

        content.add(createBottomButton());
        content.add(Box.createVerticalStrut(40));

        JScrollPane scrollPane = new JScrollPane(content);

        scrollPane.setBorder(null);

        scrollPane.getViewport().setBackground(
                UIConstants.BACKGROUND_COLOR
        );

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        return scrollPane;
    }

    // =========================================================
    // MAIN FORM
    // =========================================================

    private JPanel createMainFormCard() {

        JPanel card = UIUtils.createRoundedPanel(
                UIConstants.CARD_BACKGROUND
        );

        card.setLayout(new GridLayout(1, 2, 20, 20));

        card.setBorder(BorderFactory.createEmptyBorder(
                28,
                28,
                28,
                28
        ));

        card.add(createFieldPanel(
                "City",
                buildCityField()
        ));

        card.add(createFieldPanel(
                "Budget",
                buildBudgetField()
        ));

        return card;
    }

    // =========================================================
    // SMART MODE
    // =========================================================

    private JPanel createSmartModeCard() {

        JPanel card = UIUtils.createRoundedPanel(
                UIConstants.CARD_BACKGROUND
        );

        card.setLayout(new BorderLayout());

        card.setBorder(BorderFactory.createEmptyBorder(
                24,
                24,
                24,
                24
        ));

        JLabel title = UIUtils.createSubtitleLabel(
                "Smart Recommendation Modes"
        );

        JPanel buttonPanel = new JPanel(
                new FlowLayout(
                        FlowLayout.LEFT,
                        16,
                        12
                )
        );

        buttonPanel.setOpaque(false);

        buttonPanel.add(
                createProfileButton("Student", "Student")
        );

        buttonPanel.add(
                createProfileButton("Bachelor", "Bachelor")
        );

        buttonPanel.add(
                createProfileButton("Family", "Family")
        );

        card.add(title, BorderLayout.NORTH);
        card.add(buttonPanel, BorderLayout.CENTER);

        return card;
    }

    // =========================================================
    // FILTER CARD
    // =========================================================

    private JPanel createFilterCard() {

        JPanel card = UIUtils.createRoundedPanel(
                UIConstants.CARD_BACKGROUND
        );

        card.setLayout(new GridLayout(1, 2, 20, 0));

        card.setBorder(BorderFactory.createEmptyBorder(
                24,
                24,
                24,
                24
        ));

        safetyThresholdSlider = new JSlider(0, 10, 5);

        safetyThresholdValueLabel =
                createPill("5 / 10");

        safetyThresholdSlider.addChangeListener(e -> {

            safetyThresholdValueLabel.setText(
                    safetyThresholdSlider.getValue()
                            + " / 10"
            );

            controller.updatePreference(
                    "minimumSafetyThreshold",
                    (double) safetyThresholdSlider.getValue()
            );
        });

        budgetMatchSlider = new JSlider(0, 100, 70);

        budgetMatchValueLabel =
                createPill("70%");

        budgetMatchSlider.addChangeListener(e -> {

            budgetMatchValueLabel.setText(
                    budgetMatchSlider.getValue()
                            + "%"
            );

            controller.updatePreference(
                    "budgetMatchThreshold",
                    (double) budgetMatchSlider.getValue()
            );
        });

        card.add(createSliderCard(
                "Minimum Safety",
                safetyThresholdSlider,
                safetyThresholdValueLabel
        ));

        card.add(createSliderCard(
                "Budget Match",
                budgetMatchSlider,
                budgetMatchValueLabel
        ));

        return card;
    }

    // =========================================================
    // WEIGHTS
    // =========================================================

    private JPanel createWeightsCard() {

        JPanel card = UIUtils.createRoundedPanel(
                UIConstants.CARD_BACKGROUND
        );

        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        card.setBorder(BorderFactory.createEmptyBorder(
                24,
                24,
                24,
                24
        ));

        JLabel title = UIUtils.createSubtitleLabel(
                "Preference Weights"
        );

        card.add(title);
        card.add(Box.createVerticalStrut(18));

        String[][] items = {
                {"Job Opportunities", "jobWeight"},
                {"Cost of Living", "costOfLivingWeight"},
                {"Healthcare", "healthcareWeight"},
                {"Transport", "transportWeight"},
                {"Safety", "safetyWeight"},
                {"Environment", "environmentWeight"},
                {"Lifestyle", "lifestyleWeight"}
        };

        for (String[] item : items) {

            card.add(createWeightSlider(
                    item[0],
                    item[1]
            ));

            card.add(Box.createVerticalStrut(16));
        }

        return card;
    }

    // =========================================================
    // BOTTOM BUTTON
    // =========================================================

    private JPanel createBottomButton() {

        JPanel panel = new JPanel(
                new FlowLayout(FlowLayout.CENTER)
        );

        panel.setOpaque(false);

        JButton button = UIUtils.createStyledButton(
                "Get Recommendations",
                UIConstants.ACCENT_COLOR
        );

        button.setPreferredSize(
                new Dimension(
                        320,
                        54
                )
        );

        button.addActionListener(
                e -> submitRecommendationRequest()
        );

        panel.add(button);

        return panel;
    }

    // =========================================================
    // FIELD PANELS
    // =========================================================

    private JPanel createFieldPanel(
            String title,
            JComponent component
    ) {

        JPanel panel = new JPanel();

        panel.setOpaque(false);

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = UIUtils.createHeadingLabel(title);

        panel.add(label);
        panel.add(Box.createVerticalStrut(10));
        panel.add(component);

        return panel;
    }

    private JPanel createSliderCard(
            String title,
            JSlider slider,
            JLabel value
    ) {

        JPanel panel = new JPanel(
                new BorderLayout()
        );

        panel.setOpaque(false);

        JLabel label = UIUtils.createHeadingLabel(title);

        JPanel top = new JPanel(
                new BorderLayout()
        );

        top.setOpaque(false);

        top.add(label, BorderLayout.WEST);
        top.add(value, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);
        panel.add(slider, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createWeightSlider(
            String label,
            String key
    ) {

        JPanel panel = new JPanel(
                new BorderLayout(16, 0)
        );

        panel.setOpaque(false);

        JLabel title = UIUtils.createHeadingLabel(label);

        title.setPreferredSize(
                new Dimension(180, 20)
        );

        JSlider slider = new JSlider(
                UIConstants.SLIDER_MIN,
                UIConstants.SLIDER_MAX,
                UIConstants.SLIDER_INITIAL
        );

        JLabel value = createPill(
                String.valueOf(UIConstants.SLIDER_INITIAL)
        );

        slider.addChangeListener(e -> {

            value.setText(
                    String.valueOf(slider.getValue())
            );

            controller.updatePreference(
                    key,
                    (double) slider.getValue()
            );
        });

        sliders.put(key, slider);

        panel.add(title, BorderLayout.WEST);
        panel.add(slider, BorderLayout.CENTER);
        panel.add(value, BorderLayout.EAST);

        return panel;
    }

    // =========================================================
    // FORM FIELDS
    // =========================================================

    private JPanel buildCityField() {

        cityCombo = new JComboBox<>();

        for (String city : controller.getCities()) {
            cityCombo.addItem(city);
        }

        UIUtils.styleComboBox(cityCombo);

        cityCombo.addActionListener(e -> {
            if (cityCombo.getSelectedItem() != null) {
                controller.updatePreference(
                        "selectedCity",
                        (String) cityCombo.getSelectedItem()
                );
            }
        });

        JPanel panel = new JPanel(
                new BorderLayout()
        );

        panel.setOpaque(false);

        panel.add(cityCombo);

        return panel;
    }

    private JPanel buildBudgetField() {

        budgetSlider = new JSlider(
                2,
                200,
                10
        );

        budgetValueLabel = createPill(
                formatBudgetFromSlider()
        );

        budgetSlider.addChangeListener(e -> {

            budgetValueLabel.setText(
                    formatBudgetFromSlider()
            );

            controller.updatePreference(
                    "budget",
                    getBudgetFromSlider()
            );
        });

        JPanel panel = new JPanel(
                new BorderLayout(14, 0)
        );

        panel.setOpaque(false);

        panel.add(budgetSlider, BorderLayout.CENTER);
        panel.add(budgetValueLabel, BorderLayout.EAST);

        return panel;
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private JLabel createPill(String text) {

        JLabel label = new JLabel(
                text,
                SwingConstants.CENTER
        );

        label.setOpaque(true);

        label.setBackground(
                UIConstants.ACCENT_SOFT
        );

        label.setForeground(
                UIConstants.PRIMARY_DARK
        );

        label.setFont(
                UIConstants.HEADING_FONT
        );

        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        UIConstants.BORDER_COLOR,
                        1,
                        true
                ),
                BorderFactory.createEmptyBorder(
                        6,
                        12,
                        6,
                        12
                )
        ));

        return label;
    }

    private String formatBudgetFromSlider() {

        return "₹ "
                + String.format(
                "%,.0f",
                getBudgetFromSlider()
        );
    }

    private double getBudgetFromSlider() {

        return budgetSlider.getValue()
                * (double) BUDGET_STEP;
    }

    // =========================================================
    // PROFILE BUTTONS
    // =========================================================

    private JButton createProfileButton(
            String text,
            String profileKey
    ) {

        JButton button =
                UIUtils.createStyledButton(
                        text,
                        UIConstants.PRIMARY_COLOR
                );

        button.setPreferredSize(
                new Dimension(
                        160,
                        44
                )
        );

        button.addActionListener(e -> {

            controller.updatePreference(
                    "profileType",
                    profileKey
            );

            syncSlidersWithProfilePreset();

            if (controller.getCurrentPreferences().getSelectedCity() == null
                    && cityCombo != null
                    && cityCombo.getSelectedItem() != null) {

                controller.updatePreference(
                        "selectedCity",
                        (String) cityCombo.getSelectedItem()
                );
            }

            controller.recalculate();
        });

        return button;
    }

    private void syncSlidersWithProfilePreset() {
        UserPreferences preferences = controller.getCurrentPreferences();
        if (preferences == null) {
            return;
        }

        setSliderValue("jobWeight", (int) Math.round(preferences.getJobWeight()));
        setSliderValue("costOfLivingWeight", (int) Math.round(preferences.getCostOfLivingWeight()));
        setSliderValue("healthcareWeight", (int) Math.round(preferences.getHealthcareWeight()));
        setSliderValue("transportWeight", (int) Math.round(preferences.getTransportWeight()));
        setSliderValue("safetyWeight", (int) Math.round(preferences.getSafetyWeight()));
        setSliderValue("environmentWeight", (int) Math.round(preferences.getEnvironmentWeight()));
        setSliderValue("lifestyleWeight", (int) Math.round(preferences.getLifestyleWeight()));
    }

    private void setSliderValue(String key, int value) {
        JSlider slider = sliders.get(key);
        if (slider != null) {
            slider.setValue(value);
        }
    }

    // =========================================================
    // SUBMIT
    // =========================================================

    private void submitRecommendationRequest() {

        controller.updatePreference(
                "budget",
                getBudgetFromSlider()
        );

        controller.updatePreference(
                "minimumSafetyThreshold",
                (double) safetyThresholdSlider.getValue()
        );

        controller.updatePreference(
                "budgetMatchThreshold",
                (double) budgetMatchSlider.getValue()
        );

        controller.calculateRecommendations(
                (String) cityCombo.getSelectedItem()
        );
    }
}