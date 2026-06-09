package com.smartcity.view;

import com.smartcity.controller.RecommendationController;
import com.smartcity.model.RecommendationResult;
import com.smartcity.model.UserPreferences;
import com.smartcity.utils.UIConstants;
import com.smartcity.utils.UIUtils;
import com.smartcity.utils.ValidationUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * ResultsPanel (modified)
 *
 * Changes from original:
 *  1. Each recommendation card now contains a {@link RadarChartPanel} alongside
 *     the score breakdown bars (side-by-side layout).
 *  2. The plain JTextArea explanation is replaced by a {@link WhyRecommendedPanel}
 *     that shows profile-aware headline, colour-coded chips, narrative, and stat row.
 *  3. A per-card accent colour is chosen from a fixed palette so the three cards
 *     are visually distinguishable at a glance.
 *
 * Only createRecommendationCard(), createCenterSection(), and createBottomSection()
 * are substantially changed.  All other methods are identical to the original.
 */
public class ResultsPanel extends JPanel {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final int MAX_DISPLAY_RESULTS = 5;

    /**
     * Per-rank accent colours used for the radar chart fill/outline and the
     * rank badge. Chosen to be visually distinct and harmonious with the app's
     * existing blue/slate palette.
     *
     * Index 0 = rank 1 (gold-teal), 1 = rank 2 (indigo), 2 = rank 3 (rose),
     * 3+ = slate fallback.
     */
    private static final Color[] RANK_COLORS = {
            new Color(13, 148, 136),    // teal-600   — rank 1
            new Color(99,  102, 241),   // indigo-500 — rank 2
            new Color(244,  63,  94),   // rose-500   — rank 3
            new Color(100, 116, 139),   // slate-500  — rank 4+
            new Color(100, 116, 139),
    };

    // ── State ─────────────────────────────────────────────────────────────────

    private final RecommendationController controller;
    private final JPanel                   contentPanel;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ResultsPanel(RecommendationController controller) {
        this.controller = controller;

        setLayout(new BorderLayout());
        setBackground(UIConstants.BACKGROUND_COLOR);

        add(createHeaderPanel(), BorderLayout.NORTH);

        contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(UIConstants.BACKGROUND_COLOR);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void displayResults(List<RecommendationResult> results) {
        contentPanel.removeAll();

        if (results == null || results.isEmpty()) {
            contentPanel.add(createEmptyState());
        } else {
            contentPanel.add(new LiveDataStatusPanel(results));
            contentPanel.add(Box.createVerticalStrut(8));

            List<RecommendationResult> topResults = results.size() > MAX_DISPLAY_RESULTS
                    ? results.subList(0, MAX_DISPLAY_RESULTS)
                    : results;

            for (RecommendationResult result : topResults) {
                contentPanel.add(createRecommendationCard(result));
                contentPanel.add(Box.createVerticalStrut(22));
            }
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private JPanel createHeaderPanel() {
        JPanel header = UIUtils.createGradientPanel(
                new Color(15, 23, 42),
                new Color(37, 99, 235));
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel title = UIUtils.createDisplayLabel("Recommended Localities");
        title.setForeground(Color.WHITE);

        JLabel subtitle = UIUtils.createSecondaryLabel(
                "Ranked areas — radar chart shows each locality's strengths at a glance");
        subtitle.setForeground(UIConstants.COLOR_WHITE_70);

        text.add(title);
        text.add(Box.createVerticalStrut(6));
        text.add(subtitle);

        header.add(text, BorderLayout.WEST);
        return header;
    }

    // ── Footer ────────────────────────────────────────────────────────────────

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 16));
        footer.setBackground(UIConstants.BACKGROUND_COLOR);

        JButton backButton = UIUtils.createSecondaryButton("Back to Preferences");
        backButton.setPreferredSize(new Dimension(220, 44));
        backButton.addActionListener(e -> {
            Container parent = getParent();
            while (parent != null && !(parent.getLayout() instanceof CardLayout)) {
                parent = parent.getParent();
            }
            if (parent != null) {
                ((CardLayout) parent.getLayout()).show(parent, "INPUT");
            }
        });

        footer.add(backButton);
        return footer;
    }

    // ── Empty state ───────────────────────────────────────────────────────────

    private JPanel createEmptyState() {
        JPanel card = UIUtils.createRoundedPanel(UIConstants.CARD_BACKGROUND);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel title    = UIUtils.createTitleLabel("No recommendations found");
        JLabel subtitle = UIUtils.createSecondaryLabel("Try adjusting your filters and weights");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(10));
        card.add(subtitle);
        return card;
    }

    // ── Card ──────────────────────────────────────────────────────────────────

    /**
     * Full recommendation card.
     *
     * Layout (BorderLayout):
     *   NORTH  — top section: rank badge + locality name + score chip
     *   CENTER — radar chart (WEST) + score breakdown bars (CENTER)
     *   SOUTH  — WhyRecommendedPanel + map button
     */
    private JPanel createRecommendationCard(RecommendationResult result) {
        JPanel card = UIUtils.createRoundedPanel(UIConstants.CARD_BACKGROUND);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        Color accent = accentColor(result.getRank());

        card.add(createTopSection(result, accent),    BorderLayout.NORTH);
        card.add(createCenterSection(result, accent), BorderLayout.CENTER);
        card.add(createBottomSection(result),         BorderLayout.SOUTH);

        return card;
    }

    // ── Top section ───────────────────────────────────────────────────────────

    private JPanel createTopSection(RecommendationResult result, Color accent) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        // --- Left: rank + name + location ---
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel rank = UIUtils.createSmallLabel("Rank #" + result.getRank());
        rank.setForeground(accent);

        JLabel name = UIUtils.createTitleLabel(result.getLocality().getName());

        JLabel location = UIUtils.createSecondaryLabel(
                result.getLocality().getCity() + ", " + result.getLocality().getState());

        JLabel liveMeta = UIUtils.createSmallLabel(String.format("%s data | %.0f%% confidence",
                result.getDataSource(), result.getConfidenceScore() * 100));
        liveMeta.setForeground(result.getConfidenceScore() >= 0.70
                ? UIConstants.SUCCESS_COLOR
                : UIConstants.WARNING_COLOR);

        left.add(rank);
        left.add(Box.createVerticalStrut(6));
        left.add(name);
        left.add(Box.createVerticalStrut(4));
        left.add(location);
        left.add(Box.createVerticalStrut(4));
        left.add(liveMeta);

        // --- Right: score chip ---
        JPanel scoreCard = UIUtils.createFilledRoundedPanel(UIConstants.ACCENT_SOFT);
        scoreCard.setLayout(new BoxLayout(scoreCard, BoxLayout.Y_AXIS));
        scoreCard.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        double finalScore = ValidationUtils.clampScore10(result.getFinalScore());

        JLabel score = new JLabel(String.format("%.1f", finalScore));
        score.setFont(UIConstants.SCORE_FONT);
        score.setForeground(UIUtils.getScoreColor(finalScore));
        score.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel scoreText = UIUtils.createSmallLabel("Migration Score");
        scoreText.setAlignmentX(Component.CENTER_ALIGNMENT);

        scoreCard.add(score);
        scoreCard.add(Box.createVerticalStrut(4));
        scoreCard.add(scoreText);

        panel.add(left,      BorderLayout.WEST);
        panel.add(scoreCard, BorderLayout.EAST);
        return panel;
    }

    // ── Center section — MODIFIED ─────────────────────────────────────────────

    /**
     * Horizontal split:
     *   LEFT  (fixed 220px) — RadarChartPanel
     *   CENTER              — score breakdown bars (unchanged from original)
     *
     * A thin vertical separator is inserted between the two halves.
     */
    private JPanel createCenterSection(RecommendationResult result, Color accent) {
        JPanel outer = new JPanel(new BorderLayout(0, 0));
        outer.setOpaque(false);

        // --- Radar chart (left column) ---
        JPanel radarWrapper = new JPanel(new BorderLayout());
        radarWrapper.setOpaque(false);
        radarWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));

        RadarChartPanel radar = new RadarChartPanel(result, accent);
        radar.setPreferredSize(new Dimension(210, 210));
        radarWrapper.add(radar, BorderLayout.CENTER);

        // Radar column label
        JLabel chartLabel = UIUtils.createSmallLabel("Factor Profile");
        chartLabel.setHorizontalAlignment(SwingConstants.CENTER);
        chartLabel.setForeground(new Color(148, 163, 184));
        radarWrapper.add(chartLabel, BorderLayout.SOUTH);

        // --- Separator ---
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setForeground(UIConstants.BORDER_COLOR);
        sep.setPreferredSize(new Dimension(1, 200));

        // --- Score bars (right column) ---
        JPanel barsPanel = buildBreakdownBars(result);
        barsPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));

        outer.add(radarWrapper, BorderLayout.WEST);
        outer.add(sep,          BorderLayout.CENTER);
        outer.add(barsPanel,    BorderLayout.EAST);

        // Force the bars panel to take all remaining space
        outer.setLayout(new BoxLayout(outer, BoxLayout.X_AXIS));
        outer.removeAll();
        outer.add(radarWrapper);
        outer.add(createThinVerticalSeparator());
        outer.add(barsPanel);

        return outer;
    }

    /** Builds the score-breakdown bar rows — logic unchanged from original. */
    private JPanel buildBreakdownBars(RecommendationResult result) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = UIUtils.createSubtitleLabel("Score Breakdown");
        panel.add(title);
        panel.add(Box.createVerticalStrut(16));

        for (Map.Entry<String, Double> entry : result.getScoreBreakdown().entrySet()) {
            panel.add(createScoreRow(entry.getKey(), entry.getValue()));
            panel.add(Box.createVerticalStrut(14));
        }

        return panel;
    }

    // ── Bottom section — MODIFIED ─────────────────────────────────────────────

    /**
     * Replaces the original plain JTextArea explanation with
     * {@link WhyRecommendedPanel}, then adds the map button below it.
     */
    private JPanel createBottomSection(RecommendationResult result) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        UserPreferences prefs = controller.getCurrentPreferences();

        WhyRecommendedPanel why = new WhyRecommendedPanel(result, prefs, result.getRank());
        panel.add(why, BorderLayout.CENTER);

        JButton mapButton = UIUtils.createStyledButton("View on Map",
                                                       UIConstants.PRIMARY_COLOR);
        mapButton.setPreferredSize(new Dimension(160, 44));
        mapButton.addActionListener(e -> openMapForLocality(result));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(mapButton);
        panel.add(btnRow, BorderLayout.SOUTH);

        return panel;
    }

    // ── Score row (unchanged from original) ───────────────────────────────────

    private JPanel createScoreRow(String label, double scoreOutOfTen) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);

        JLabel name = UIUtils.createHeadingLabel(label);
        name.setPreferredSize(new Dimension(160, 20));

        int percentage = ValidationUtils.clampScore100(scoreOutOfTen * 10);

        JPanel progress = createModernProgressBar(percentage, scoreOutOfTen);

        JLabel value = new JLabel(percentage + "%");
        value.setFont(UIConstants.HEADING_FONT);
        value.setForeground(UIUtils.getScoreColor(scoreOutOfTen));
        value.setPreferredSize(new Dimension(48, 20));
        value.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(name,     BorderLayout.WEST);
        row.add(progress, BorderLayout.CENTER);
        row.add(value,    BorderLayout.EAST);
        return row;
    }

    // ── Progress bar (unchanged from original) ────────────────────────────────

    private JPanel createModernProgressBar(int percentage, double score) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(UIConstants.SURFACE_COLOR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                int width = (int) (getWidth() * (percentage / 100.0));
                g2.setColor(UIUtils.getScoreColor(score));
                g2.fillRoundRect(0, 0, width, getHeight(), 20, 20);

                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(300, 14));
        wrapper.add(bar);
        return wrapper;
    }

    // ── Utility helpers ───────────────────────────────────────────────────────

    /** Maps a 1-based rank to an accent colour from the fixed palette. */
    private static Color accentColor(int rank) {
        int idx = Math.max(0, Math.min(rank - 1, RANK_COLORS.length - 1));
        return RANK_COLORS[idx];
    }

    /** Thin 1px vertical rule used between the radar and the bars. */
    private static JComponent createThinVerticalSeparator() {
        JPanel sep = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(UIConstants.BORDER_COLOR);
                g.fillRect(0, 4, 1, getHeight() - 8);
            }
        };
        sep.setOpaque(false);
        sep.setPreferredSize(new Dimension(1, 200));
        sep.setMaximumSize(new Dimension(1, Integer.MAX_VALUE));
        return sep;
    }

    private void openMapForLocality(RecommendationResult result) {
        String query = result.getLocality().getName()
                       + ", "
                       + result.getLocality().getCity();
        UIUtils.openURL("https://www.google.com/maps/search/"
                        + query.replace(" ", "+"));
    }
}
