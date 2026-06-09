package com.smartcity.view;

import com.smartcity.model.RecommendationResult;
import com.smartcity.model.UserPreferences;
import com.smartcity.utils.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * WhyRecommendedPanel
 *
 * A structured, visually rich panel that explains why a locality was
 * recommended for the current user profile.
 *
 * Layout (top to bottom):
 *   1. Profile-aware headline   — "Top pick for your Family profile"
 *   2. Strength chips           — green pills for top-scoring factors
 *   3. Concern chips            — amber pills for below-average factors
 *   4. Score narrative          — one plain-English sentence
 *   5. Key stat row             — rent / safety / top factor inline
 *
 * Design constraints:
 *   - Pure Swing, no external libraries.
 *   - Opaque background is intentionally false — caller card provides it.
 *   - Works with the existing UIConstants colour tokens.
 *   - All text is generated deterministically from model data so it is
 *     never generic filler — every field reacts to the actual scores.
 *
 * Integration: instantiate inside ResultsPanel.createBottomSection()
 * replacing the existing plain JTextArea for result.getExplanation().
 */
public class WhyRecommendedPanel extends JPanel {

    // ── Colour tokens (local, keeps component self-contained) ─────────────────

    private static final Color STRENGTH_BG      = new Color(220, 252, 231);   // soft green
    private static final Color STRENGTH_FG      = new Color(22, 101, 52);
    private static final Color STRENGTH_BORDER  = new Color(134, 239, 172);

    private static final Color CONCERN_BG       = new Color(255, 237, 213);   // soft amber
    private static final Color CONCERN_FG       = new Color(124, 45, 18);
    private static final Color CONCERN_BORDER   = new Color(253, 186, 116);

    private static final Color NEUTRAL_BG       = new Color(241, 245, 249);
    private static final Color NEUTRAL_FG       = new Color(71, 85, 105);
    private static final Color NEUTRAL_BORDER   = new Color(203, 213, 225);

    private static final Color HEADLINE_FG      = new Color(15, 23, 42);
    private static final Color NARRATIVE_FG     = new Color(71, 85, 105);
    private static final Color STAT_LABEL_FG    = new Color(100, 116, 139);
    private static final Color DIVIDER_COLOR    = new Color(226, 232, 240);

    // ── Fonts ─────────────────────────────────────────────────────────────────

    private static final Font HEADLINE_FONT     = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font CHIP_FONT         = new Font("Segoe UI", Font.BOLD,  10);
    private static final Font NARRATIVE_FONT    = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font STAT_LABEL_FONT   = new Font("Segoe UI", Font.PLAIN, 10);
    private static final Font STAT_VALUE_FONT   = new Font("Segoe UI", Font.BOLD,  11);
    private static final Font SECTION_HDR_FONT  = new Font("Segoe UI", Font.BOLD,   9);

    // ── Score thresholds ──────────────────────────────────────────────────────

    /** Factor score above which the factor is counted as a "strength". */
    private static final double STRENGTH_THRESHOLD = 7.5;

    /** Factor score below which the factor is counted as a "concern". */
    private static final double CONCERN_THRESHOLD  = 5.5;

    /** Maximum chips per row before wrapping to the next line. */
    private static final int    MAX_CHIPS_ROW      = 4;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * @param result      The recommendation whose scores drive all generated text.
     * @param preferences The current user preferences (profile, budget, family size).
     * @param rank        1-based rank of this result (used in headline).
     */
    public WhyRecommendedPanel(RecommendationResult result,
                               UserPreferences preferences,
                               int rank) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        if (result == null) return;

        Map<String, Double> bd    = result.getScoreBreakdown();
        List<FactorEntry>   sorted = sortedFactors(bd);

        add(buildDivider());
        add(Box.createVerticalStrut(10));
        add(buildHeadline(result, preferences, rank, sorted));
        add(Box.createVerticalStrut(10));
        add(buildChipRows(sorted));
        add(Box.createVerticalStrut(10));
        add(buildNarrative(result, preferences, sorted));
        add(Box.createVerticalStrut(10));
        add(buildStatRow(result, preferences, sorted));
        add(Box.createVerticalStrut(4));
    }

    // ── Section builders ──────────────────────────────────────────────────────

    /** Thin horizontal rule that visually separates this section from the score bars above. */
    private JComponent buildDivider() {
        JPanel line = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(DIVIDER_COLOR);
                g.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
            }
        };
        line.setOpaque(false);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setPreferredSize(new Dimension(0, 1));
        return line;
    }

    /**
     * "Why We Recommend This" headline row.
     * Profile-aware: changes depending on Student / Bachelor / Family / Custom.
     */
    private JPanel buildHeadline(RecommendationResult result,
                                  UserPreferences preferences,
                                  int rank,
                                  List<FactorEntry> sorted) {

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Icon label
        JLabel icon = new JLabel(rankIcon(rank) + "  ");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));

        // Headline text
        JLabel text = new JLabel(buildHeadlineText(result, preferences, rank, sorted));
        text.setFont(HEADLINE_FONT);
        text.setForeground(HEADLINE_FG);

        row.add(icon);
        row.add(text);
        return row;
    }

    /**
     * Two rows of coloured chips: strengths (green) then concerns (amber).
     * Each chip is a rounded-corner label with a leading emoji indicator.
     */
    private JPanel buildChipRows(List<FactorEntry> sorted) {

        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- Strengths ---
        List<FactorEntry> strengths = new ArrayList<>();
        for (FactorEntry fe : sorted) {
            if (fe.score >= STRENGTH_THRESHOLD) strengths.add(fe);
        }

        if (!strengths.isEmpty()) {
            JPanel sectionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            sectionRow.setOpaque(false);
            sectionRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel hdr = sectionHeaderLabel("STRENGTHS");
            sectionRow.add(hdr);

            int count = 0;
            for (FactorEntry fe : strengths) {
                if (count >= MAX_CHIPS_ROW) break;
                sectionRow.add(buildChip(
                        "✓  " + shortLabel(fe.name),
                        fe.score,
                        STRENGTH_BG, STRENGTH_FG, STRENGTH_BORDER));
                count++;
            }
            container.add(sectionRow);
        }

        // --- Concerns ---
        List<FactorEntry> concerns = new ArrayList<>();
        // Traverse in reverse (worst first)
        for (int i = sorted.size() - 1; i >= 0; i--) {
            if (sorted.get(i).score < CONCERN_THRESHOLD) concerns.add(sorted.get(i));
        }

        if (!concerns.isEmpty()) {
            JPanel cRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            cRow.setOpaque(false);
            cRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel hdr = sectionHeaderLabel("CONSIDERATIONS");
            cRow.add(hdr);

            int count = 0;
            for (FactorEntry fe : concerns) {
                if (count >= MAX_CHIPS_ROW) break;
                cRow.add(buildChip(
                        "△  " + shortLabel(fe.name),
                        fe.score,
                        CONCERN_BG, CONCERN_FG, CONCERN_BORDER));
                count++;
            }
            container.add(cRow);
        }

        // No concerns fallback
        if (concerns.isEmpty() && strengths.isEmpty()) {
            JPanel fallback = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            fallback.setOpaque(false);
            fallback.setAlignmentX(Component.LEFT_ALIGNMENT);
            fallback.add(buildChip("●  Balanced profile", 6.0,
                    NEUTRAL_BG, NEUTRAL_FG, NEUTRAL_BORDER));
            container.add(fallback);
        }

        return container;
    }

    /**
     * A single plain-English sentence that reacts to the top-priority factor
     * and the user's profile type.
     */
    private JPanel buildNarrative(RecommendationResult result,
                                   UserPreferences preferences,
                                   List<FactorEntry> sorted) {

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea text = new JTextArea(buildNarrativeText(result, preferences, sorted));
        text.setOpaque(false);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(NARRATIVE_FONT);
        text.setForeground(NARRATIVE_FG);
        text.setBorder(null);
        // Constrain width so it wraps sensibly inside the card
        text.setMaximumSize(new Dimension(460, Integer.MAX_VALUE));
        text.setPreferredSize(new Dimension(420, 34));

        row.add(text);
        return row;
    }

    /**
     * Compact horizontal stats row: rent / safety / top factor.
     * Gives evaluators a quick reference row of numbers.
     */
    private JPanel buildStatRow(RecommendationResult result,
                                 UserPreferences preferences,
                                 List<FactorEntry> sorted) {

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Rent stat
        double rent = result.getLocality().getAvgRent();
        row.add(statBlock("Monthly Rent",
                String.format("₹%,.0f", rent),
                rentAffordability(rent, preferences.getBudget())));

        row.add(statSeparator());

        // Safety stat
        double safety = result.getLocality().getSafetyScore();
        row.add(statBlock("Safety Score",
                String.format("%.1f / 10", safety),
                safetyBadgeColor(safety)));

        // Top factor (only if meaningful)
        if (!sorted.isEmpty()) {
            FactorEntry best = sorted.get(0);
            row.add(statSeparator());
            row.add(statBlock("Top Factor",
                    shortLabel(best.name) + "  " + String.format("%.1f", best.score),
                    UIConstants.PRIMARY_COLOR));
        }

        // Budget utilisation (only when budget is set)
        if (preferences.getBudget() > 0) {
            double pct = (rent * 12.0) / preferences.getBudget() * 100.0;
            row.add(statSeparator());
            row.add(statBlock("Budget Used",
                    String.format("%.0f%%", pct),
                    pct <= 35 ? new Color(22, 163, 74)
                              : pct <= 60 ? new Color(234, 179, 8)
                              : new Color(220, 38, 38)));
        }

        return row;
    }

    // ── Text generators ───────────────────────────────────────────────────────

    /**
     * Profile-aware headline.
     * Examples:
     *   "Best match for your Family profile · ranked #1"
     *   "Top pick for remote workers · strong lifestyle fit"
     */
    private String buildHeadlineText(RecommendationResult result,
                                      UserPreferences preferences,
                                      int rank,
                                      List<FactorEntry> sorted) {

        String profile  = preferences != null ? preferences.getProfileType() : "Custom";
        String workType = preferences != null ? preferences.getWorkType()    : null;

        // Profile-specific opening
        String opening;
        if ("Family".equalsIgnoreCase(profile)) {
            opening = "Family-friendly pick";
        } else if ("Student".equalsIgnoreCase(profile)) {
            opening = "Strong student choice";
        } else if ("Bachelor".equalsIgnoreCase(profile)) {
            opening = "Ideal bachelor lifestyle";
        } else if ("Remote".equalsIgnoreCase(workType)) {
            opening = "Top remote-work locality";
        } else {
            opening = rank == 1 ? "Best overall match" : "Strong recommendation";
        }

        // Top factor rider
        String rider = "";
        if (!sorted.isEmpty() && sorted.get(0).score >= STRENGTH_THRESHOLD) {
            rider = " · leads on " + shortLabel(sorted.get(0).name).toLowerCase();
        }

        return opening + "  ·  #" + rank + rider;
    }

    /**
     * Deterministic narrative sentence — never generic.
     * Selects wording based on the highest-weight user priority and its score.
     */
    private String buildNarrativeText(RecommendationResult result,
                                       UserPreferences preferences,
                                       List<FactorEntry> sorted) {

        if (sorted.isEmpty()) {
            return result.getLocality().getDescription();
        }

        FactorEntry top = sorted.get(0);
        String name     = result.getLocality().getName();

        // Budget line
        String budgetLine = "";
        if (preferences != null && preferences.getBudget() > 0) {
            double pct = (result.getLocality().getAvgRent() * 12.0)
                         / preferences.getBudget() * 100.0;
            if (pct <= 35) {
                budgetLine = " Monthly rent sits comfortably within budget at "
                             + String.format("%.0f%%", pct) + ".";
            } else if (pct > 70) {
                budgetLine = " Note: rent consumes "
                             + String.format("%.0f%%", pct) + " of annual budget.";
            }
        }

        // Concern rider
        String concernLine = "";
        List<FactorEntry> concerns = new ArrayList<>();
        for (int i = sorted.size() - 1; i >= 0; i--) {
            if (sorted.get(i).score < CONCERN_THRESHOLD) {
                concerns.add(sorted.get(i));
                break;  // show at most one concern in narrative
            }
        }
        if (!concerns.isEmpty()) {
            concernLine = " " + shortLabel(concerns.get(0).name)
                          + " (" + String.format("%.1f", concerns.get(0).score) + "/10)"
                          + " is the main trade-off.";
        }

        return name + " scores " + String.format("%.1f", top.score)
               + "/10 for " + shortLabel(top.name).toLowerCase()
               + ", its strongest dimension."
               + budgetLine + concernLine;
    }

    // ── Component helpers ─────────────────────────────────────────────────────

    /**
     * A single rounded-corner chip (pill label).
     * Painted manually so we can use arbitrary corner radii without
     * depending on third-party borders.
     */
    private JLabel buildChip(String text, double score,
                              Color bg, Color fg, Color border) {

        JLabel chip = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        chip.setFont(CHIP_FONT);
        chip.setForeground(fg);
        chip.setOpaque(false);
        chip.setBorder(new EmptyBorder(3, 8, 3, 8));
        return chip;
    }

    /** Small uppercase section header label ("STRENGTHS", "CONSIDERATIONS"). */
    private JLabel sectionHeaderLabel(String text) {
        JLabel lbl = new JLabel(text + "   ");
        lbl.setFont(SECTION_HDR_FONT);
        lbl.setForeground(new Color(148, 163, 184));
        return lbl;
    }

    /**
     * Compact two-line block: label + coloured value.
     * Used in the stat row at the bottom of the panel.
     */
    private JPanel statBlock(String label, String value, Color valueColor) {

        JPanel block = new JPanel();
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(new EmptyBorder(0, 10, 0, 10));

        JLabel lbl = new JLabel(label);
        lbl.setFont(STAT_LABEL_FONT);
        lbl.setForeground(STAT_LABEL_FG);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel val = new JLabel(value);
        val.setFont(STAT_VALUE_FONT);
        val.setForeground(valueColor);
        val.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(lbl);
        block.add(val);
        return block;
    }

    /** Thin 1px vertical separator between stat blocks. */
    private JComponent statSeparator() {
        JPanel sep = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(DIVIDER_COLOR);
                g.drawLine(0, 4, 0, getHeight() - 4);
            }
        };
        sep.setOpaque(false);
        sep.setPreferredSize(new Dimension(1, 32));
        sep.setMaximumSize(new Dimension(1, 32));
        return sep;
    }

    // ── Data helpers ──────────────────────────────────────────────────────────

    /**
     * Returns factors sorted descending by score.
     * Only includes factors that are actually present in the breakdown map.
     */
    private static List<FactorEntry> sortedFactors(Map<String, Double> bd) {
        if (bd == null || bd.isEmpty()) return Collections.emptyList();

        List<FactorEntry> list = new ArrayList<>();
        for (Map.Entry<String, Double> e : bd.entrySet()) {
            list.add(new FactorEntry(e.getKey(), e.getValue()));
        }
        list.sort((a, b) -> Double.compare(b.score, a.score));
        return list;
    }

    /**
     * Abbreviated display label for a full factor name.
     * Keeps chips compact without truncation.
     */
    private static String shortLabel(String factor) {
        switch (factor) {
            case "Job Opportunities": return "Jobs";
            case "Cost of Living":    return "Cost";
            case "Healthcare":        return "Healthcare";
            case "Transport":         return "Transport";
            case "Safety":            return "Safety";
            case "Environment":       return "Environment";
            case "Lifestyle":         return "Lifestyle";
            default:                  return factor;
        }
    }

    /** Emoji icon that varies by rank to give each card a distinct identity. */
    private static String rankIcon(int rank) {
        switch (rank) {
            case 1:  return "🏆";
            case 2:  return "🥈";
            case 3:  return "🥉";
            default: return "★";
        }
    }

    /** Colour for the rent stat block based on affordability. */
    private static Color rentAffordability(double rent, double budget) {
        if (budget <= 0) return UIConstants.TEXT_PRIMARY;
        double pct = (rent * 12.0) / budget;
        if (pct <= 0.35) return new Color(22, 163, 74);
        if (pct <= 0.60) return new Color(234, 179, 8);
        return new Color(220, 38, 38);
    }

    /** Colour for the safety stat badge. */
    private static Color safetyBadgeColor(double score) {
        if (score >= 8.0) return new Color(22, 163, 74);
        if (score >= 6.0) return new Color(234, 179, 8);
        return new Color(220, 38, 38);
    }

    // ── Inner record ──────────────────────────────────────────────────────────

    /** Lightweight tuple: factor name + score. */
    private static final class FactorEntry {
        final String name;
        final double score;
        FactorEntry(String name, double score) {
            this.name  = name;
            this.score = score;
        }
    }
}