package com.smartcity.view;

import com.smartcity.controller.RecommendationController;
import com.smartcity.model.RecommendationResult;
import com.smartcity.utils.UIConstants;
import com.smartcity.utils.UIUtils;
import com.smartcity.utils.ValidationUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class ResultsPanel extends JPanel {

    private static final int MAX_DISPLAY_RESULTS = 5;

    private final JPanel contentPanel;

    public ResultsPanel(RecommendationController controller) {

        setLayout(new BorderLayout());

        setBackground(UIConstants.BACKGROUND_COLOR);

        add(createHeaderPanel(), BorderLayout.NORTH);

        contentPanel = new JPanel();

        contentPanel.setOpaque(false);

        contentPanel.setLayout(new BoxLayout(
                contentPanel,
                BoxLayout.Y_AXIS
        ));

        contentPanel.setBorder(BorderFactory.createEmptyBorder(
                24,
                24,
                24,
                24
        ));

        JScrollPane scrollPane =
                new JScrollPane(contentPanel);

        scrollPane.setBorder(null);

        scrollPane.getViewport().setBackground(
                UIConstants.BACKGROUND_COLOR
        );

        scrollPane.getVerticalScrollBar()
                .setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        add(createFooterPanel(), BorderLayout.SOUTH);
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
                28,
                32,
                28,
                32
        ));

        JPanel text = new JPanel();

        text.setOpaque(false);

        text.setLayout(new BoxLayout(
                text,
                BoxLayout.Y_AXIS
        ));

        JLabel title = UIUtils.createDisplayLabel(
                "Recommended Localities"
        );

        title.setForeground(Color.WHITE);

        JLabel subtitle = UIUtils.createSecondaryLabel(
                "AI-ranked top 5 areas based on your preferences and migration score"
        );

        subtitle.setForeground(
                UIConstants.COLOR_WHITE_70
        );

        text.add(title);
        text.add(Box.createVerticalStrut(6));
        text.add(subtitle);

        header.add(text, BorderLayout.WEST);

        return header;
    }

    // =========================================================
    // FOOTER
    // =========================================================

    private JPanel createFooterPanel() {

        JPanel footer = new JPanel(
                new FlowLayout(
                        FlowLayout.LEFT,
                        20,
                        16
                )
        );

        footer.setBackground(
                UIConstants.BACKGROUND_COLOR
        );

        JButton backButton =
                UIUtils.createSecondaryButton(
                        "Back to Preferences"
                );

        backButton.setPreferredSize(
                new Dimension(220, 44)
        );

        backButton.addActionListener(e -> {

            Container parent = getParent();

            while (parent != null &&
                    !(parent.getLayout() instanceof CardLayout)) {

                parent = parent.getParent();
            }

            if (parent != null) {

                ((CardLayout) parent.getLayout())
                        .show(parent, "INPUT");
            }
        });

        footer.add(backButton);

        return footer;
    }

    // =========================================================
    // DISPLAY RESULTS
    // =========================================================

    public void displayResults(
            List<RecommendationResult> results
    ) {

        contentPanel.removeAll();

        if (results == null || results.isEmpty()) {

            contentPanel.add(createEmptyState());

        } else {

            List<RecommendationResult> topResults = results.size() > MAX_DISPLAY_RESULTS
                    ? results.subList(0, MAX_DISPLAY_RESULTS)
                    : results;

            for (RecommendationResult result : topResults) {

                contentPanel.add(
                        createRecommendationCard(result)
                );

                contentPanel.add(
                        Box.createVerticalStrut(22)
                );
            }
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // =========================================================
    // EMPTY STATE
    // =========================================================

    private JPanel createEmptyState() {

        JPanel card = UIUtils.createRoundedPanel(
                UIConstants.CARD_BACKGROUND
        );

        card.setLayout(new BoxLayout(
                card,
                BoxLayout.Y_AXIS
        ));

        card.setBorder(BorderFactory.createEmptyBorder(
                40,
                40,
                40,
                40
        ));

        JLabel title = UIUtils.createTitleLabel(
                "No recommendations found"
        );

        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle =
                UIUtils.createSecondaryLabel(
                        "Try changing your filters and weights"
                );

        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(10));
        card.add(subtitle);

        return card;
    }

    // =========================================================
    // CARD
    // =========================================================

    private JPanel createRecommendationCard(
            RecommendationResult result
    ) {

        JPanel card = UIUtils.createRoundedPanel(
                UIConstants.CARD_BACKGROUND
        );

        card.setLayout(new BorderLayout(0, 20));

        card.setBorder(BorderFactory.createEmptyBorder(
                28,
                28,
                28,
                28
        ));

        // TOP
        card.add(createTopSection(result),
                BorderLayout.NORTH);

        // CENTER
        card.add(createCenterSection(result),
                BorderLayout.CENTER);

        // BOTTOM
        card.add(createBottomSection(result),
                BorderLayout.SOUTH);

        return card;
    }

    // =========================================================
    // TOP
    // =========================================================

    private JPanel createTopSection(
            RecommendationResult result
    ) {

        JPanel panel = new JPanel(
                new BorderLayout()
        );

        panel.setOpaque(false);

        JPanel left = new JPanel();

        left.setOpaque(false);

        left.setLayout(new BoxLayout(
                left,
                BoxLayout.Y_AXIS
        ));

        JLabel rank = UIUtils.createSmallLabel(
                "Rank #" + result.getRank()
        );

        rank.setForeground(
                UIConstants.INFO_COLOR
        );

        JLabel name = UIUtils.createTitleLabel(
                result.getLocality().getName()
        );

        JLabel location =
                UIUtils.createSecondaryLabel(
                        result.getLocality().getCity()
                                + ", "
                                + result.getLocality().getState()
                );

        left.add(rank);
        left.add(Box.createVerticalStrut(6));
        left.add(name);
        left.add(Box.createVerticalStrut(4));
        left.add(location);

        JPanel scoreCard =
                UIUtils.createFilledRoundedPanel(
                        UIConstants.ACCENT_SOFT
                );

        scoreCard.setLayout(new BoxLayout(
                scoreCard,
                BoxLayout.Y_AXIS
        ));

        scoreCard.setBorder(BorderFactory.createEmptyBorder(
                18,
                24,
                18,
                24
        ));

        double finalScore =
                ValidationUtils.clampScore10(
                        result.getFinalScore()
                );

        JLabel score =
                new JLabel(
                        String.format("%.1f", finalScore)
                );

        score.setFont(UIConstants.SCORE_FONT);

        score.setForeground(
                UIUtils.getScoreColor(finalScore)
        );

        score.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel scoreText =
                UIUtils.createSmallLabel(
                        "Migration Score"
                );

        scoreText.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        scoreCard.add(score);
        scoreCard.add(Box.createVerticalStrut(4));
        scoreCard.add(scoreText);

        panel.add(left, BorderLayout.WEST);
        panel.add(scoreCard, BorderLayout.EAST);

        return panel;
    }

    // =========================================================
    // CENTER
    // =========================================================

    private JPanel createCenterSection(
            RecommendationResult result
    ) {

        JPanel panel = new JPanel();

        panel.setOpaque(false);

        panel.setLayout(new BoxLayout(
                panel,
                BoxLayout.Y_AXIS
        ));

        JLabel title = UIUtils.createSubtitleLabel(
                "Score Breakdown"
        );

        panel.add(title);
        panel.add(Box.createVerticalStrut(16));

        for (Map.Entry<String, Double> entry :
                result.getScoreBreakdown().entrySet()) {

            panel.add(createScoreRow(
                    entry.getKey(),
                    entry.getValue()
            ));

            panel.add(Box.createVerticalStrut(14));
        }

        return panel;
    }

    // =========================================================
    // SCORE ROW
    // =========================================================

    private JPanel createScoreRow(
            String label,
            double scoreOutOfTen
    ) {

        JPanel row = new JPanel(
                new BorderLayout(14, 0)
        );

        row.setOpaque(false);

        JLabel name =
                UIUtils.createHeadingLabel(label);

        name.setPreferredSize(
                new Dimension(180, 20)
        );

        int percentage =
                ValidationUtils.clampScore100(
                        scoreOutOfTen * 10
                );

        JPanel progress =
                createModernProgressBar(
                        percentage,
                        scoreOutOfTen
                );

        JLabel value = new JLabel(
                percentage + "%"
        );

        value.setFont(UIConstants.HEADING_FONT);

        value.setForeground(
                UIUtils.getScoreColor(scoreOutOfTen)
        );

        value.setPreferredSize(
                new Dimension(60, 20)
        );

        value.setHorizontalAlignment(
                SwingConstants.RIGHT
        );

        row.add(name, BorderLayout.WEST);
        row.add(progress, BorderLayout.CENTER);
        row.add(value, BorderLayout.EAST);

        return row;
    }

    // =========================================================
    // MODERN BAR
    // =========================================================

    private JPanel createModernProgressBar(
            int percentage,
            double score
    ) {

        JPanel wrapper = new JPanel(
                new BorderLayout()
        );

        wrapper.setOpaque(false);

        JPanel bar = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 =
                        (Graphics2D) g.create();

                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                // Background
                g2.setColor(
                        UIConstants.SURFACE_COLOR
                );

                g2.fillRoundRect(
                        0,
                        0,
                        getWidth(),
                        getHeight(),
                        20,
                        20
                );

                // Fill
                int width =
                        (int) (getWidth()
                                * (percentage / 100.0));

                g2.setColor(
                        UIUtils.getScoreColor(score)
                );

                g2.fillRoundRect(
                        0,
                        0,
                        width,
                        getHeight(),
                        20,
                        20
                );

                g2.dispose();
            }
        };

        bar.setOpaque(false);

        bar.setPreferredSize(
                new Dimension(300, 14)
        );

        wrapper.add(bar);

        return wrapper;
    }

    // =========================================================
    // BOTTOM
    // =========================================================

    private JPanel createBottomSection(
            RecommendationResult result
    ) {

        JPanel panel = new JPanel(
                new BorderLayout(20, 0)
        );

        panel.setOpaque(false);

        JTextArea explanation =
                new JTextArea(
                        result.getExplanation()
                );

        explanation.setEditable(false);

        explanation.setLineWrap(true);

        explanation.setWrapStyleWord(true);

        explanation.setOpaque(false);

        explanation.setFont(
                UIConstants.LABEL_FONT
        );

        explanation.setForeground(
                UIConstants.TEXT_SECONDARY
        );

        JButton mapButton =
                UIUtils.createStyledButton(
                        "View on Map",
                        UIConstants.PRIMARY_COLOR
                );

        mapButton.setPreferredSize(
                new Dimension(180, 44)
        );

        mapButton.addActionListener(e ->
                openMapForLocality(result)
        );

        panel.add(explanation, BorderLayout.CENTER);
        panel.add(mapButton, BorderLayout.EAST);

        return panel;
    }

    // =========================================================
    // MAP
    // =========================================================

    private void openMapForLocality(
            RecommendationResult result
    ) {

        String query =
                result.getLocality().getName()
                        + ", "
                        + result.getLocality().getCity();

        UIUtils.openURL(
                "https://www.google.com/maps/search/"
                        + query.replace(" ", "+")
        );
    }
}