package com.smartcity.view;

import com.smartcity.model.RecommendationResult;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * RadarChartPanel
 *
 * Renders a 7-axis spider/radar chart for a single RecommendationResult.
 * All rendering is pure Java2D — no external libraries required.
 *
 * Layout contract:
 *   - Preferred size is square; caller controls width via setPreferredSize.
 *   - The chart scales automatically to fill available space with a fixed margin.
 *   - Axis labels are drawn outside the outermost ring.
 *
 * Integration: drop into any panel with a simple:
 *   RadarChartPanel chart = new RadarChartPanel(result);
 *   parent.add(chart);
 */
public class RadarChartPanel extends JPanel {

    // ── Configurable appearance ──────────────────────────────────────────────

    /** Number of concentric grid rings drawn behind the data polygon. */
    private static final int GRID_RINGS       = 5;

    /** Fraction of the half-dimension used as outer margin (for axis labels). */
    private static final double LABEL_MARGIN  = 0.28;

    /** Alpha (0–255) of the filled data polygon. */
    private static final int FILL_ALPHA       = 55;

    /** Stroke width of the data polygon outline. */
    private static final float OUTLINE_WIDTH  = 2.2f;

    /** Stroke width of the axis lines. */
    private static final float AXIS_WIDTH     = 0.9f;

    /** Stroke width of the concentric grid rings. */
    private static final float GRID_WIDTH     = 0.7f;

    /** Font used for axis labels. */
    private static final Font  LABEL_FONT     = new Font("Segoe UI", Font.BOLD, 10);

    /** Font used for the 0/5/10 ring annotations on the right axis. */
    private static final Font  RING_FONT      = new Font("Segoe UI", Font.PLAIN, 8);

    // ── Data ─────────────────────────────────────────────────────────────────

    /**
     * Canonical axis order — determines the angular positions.
     * Kept in a List so ordering is deterministic across JVM versions.
     */
    private static final List<String> AXIS_ORDER = List.of(
            "Job Opportunities",
            "Cost of Living",
            "Healthcare",
            "Transport",
            "Safety",
            "Environment",
            "Lifestyle"
    );

    /**
     * Short display labels matching AXIS_ORDER (saves label-wrapping logic).
     */
    private static final List<String> AXIS_LABELS = List.of(
            "Jobs",
            "Cost",
            "Health",
            "Transport",
            "Safety",
            "Environ.",
            "Lifestyle"
    );

    private final double[]  values;   // normalised 0–10 for each axis
    private final Color     chartColor;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * @param result      The scored locality whose breakdown drives the chart.
     * @param chartColor  The accent colour used for fill and outline.
     */
    public RadarChartPanel(RecommendationResult result, Color chartColor) {
        this.chartColor = chartColor;
        this.values     = extractValues(result);

        setOpaque(false);
        setPreferredSize(new Dimension(220, 220));
    }

    // ── Value extraction ──────────────────────────────────────────────────────

    /**
     * Pulls 7 axis values from the result's score breakdown.
     * Falls back to 5.0 (neutral) for any missing key so the chart never
     * crashes when the engine omits a factor.
     */
    private double[] extractValues(RecommendationResult result) {
        Map<String, Double> bd = result != null ? result.getScoreBreakdown() : Collections.emptyMap();
        double[] v = new double[AXIS_ORDER.size()];
        for (int i = 0; i < AXIS_ORDER.size(); i++) {
            v[i] = bd.getOrDefault(AXIS_ORDER.get(i), 5.0);
            v[i] = Math.max(0, Math.min(10, v[i]));   // defensive clamp
        }
        return v;
    }

    // ── Paint ─────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        applyRenderingHints(g2);

        int w = getWidth();
        int h = getHeight();

        // Centre and radius of the chart area (inside label margin)
        double cx      = w / 2.0;
        double cy      = h / 2.0;
        double radius  = Math.min(cx, cy) * (1.0 - LABEL_MARGIN);

        int n = AXIS_ORDER.size();

        // Pre-compute angles: first axis points straight up (−π/2)
        double[] angles = new double[n];
        for (int i = 0; i < n; i++) {
            angles[i] = -Math.PI / 2.0 + (2.0 * Math.PI * i) / n;
        }

        drawGridRings(g2, cx, cy, radius, n, angles);
        drawAxes(g2, cx, cy, radius, n, angles);
        drawDataPolygon(g2, cx, cy, radius, n, angles);
        drawAxisLabels(g2, cx, cy, radius, n, angles);
        drawValueDots(g2, cx, cy, radius, n, angles);

        g2.dispose();
    }

    // ── Drawing sub-routines ──────────────────────────────────────────────────

    /** Concentric grid rings at equal score intervals. */
    private void drawGridRings(Graphics2D g2, double cx, double cy,
                                double radius, int n, double[] angles) {

        g2.setStroke(new BasicStroke(GRID_WIDTH, BasicStroke.CAP_ROUND,
                                     BasicStroke.JOIN_ROUND));

        for (int ring = 1; ring <= GRID_RINGS; ring++) {

            double r = radius * ring / GRID_RINGS;

            // Build the polygon for this ring
            Path2D poly = new Path2D.Double();
            for (int i = 0; i < n; i++) {
                double px = cx + r * Math.cos(angles[i]);
                double py = cy + r * Math.sin(angles[i]);
                if (i == 0) poly.moveTo(px, py);
                else        poly.lineTo(px, py);
            }
            poly.closePath();

            // Subtle filled background for innermost ring
            if (ring == 1) {
                g2.setColor(new Color(240, 244, 255, 40));
                g2.fill(poly);
            }

            g2.setColor(new Color(180, 190, 210, 90));
            g2.draw(poly);

            // Annotate the rightmost axis with the score value
            double labelAngle = angles[0];               // first axis = "Jobs"
            double annotR     = r + 2;
            double ax = cx + annotR * Math.cos(labelAngle) + 4;
            double ay = cy + annotR * Math.sin(labelAngle);
            g2.setFont(RING_FONT);
            g2.setColor(new Color(140, 150, 170, 160));
            double scoreAtRing = 10.0 * ring / GRID_RINGS;
            g2.drawString(String.valueOf((int) scoreAtRing), (float) ax, (float) ay);
        }
    }

    /** Thin lines from centre to each vertex. */
    private void drawAxes(Graphics2D g2, double cx, double cy,
                           double radius, int n, double[] angles) {

        g2.setStroke(new BasicStroke(AXIS_WIDTH));
        g2.setColor(new Color(160, 170, 195, 110));

        for (int i = 0; i < n; i++) {
            double ex = cx + radius * Math.cos(angles[i]);
            double ey = cy + radius * Math.sin(angles[i]);
            g2.draw(new Line2D.Double(cx, cy, ex, ey));
        }
    }

    /** Filled + outlined polygon connecting the data points. */
    private void drawDataPolygon(Graphics2D g2, double cx, double cy,
                                  double radius, int n, double[] angles) {

        Path2D polygon = new Path2D.Double();

        for (int i = 0; i < n; i++) {
            double r  = radius * (values[i] / 10.0);
            double px = cx + r * Math.cos(angles[i]);
            double py = cy + r * Math.sin(angles[i]);
            if (i == 0) polygon.moveTo(px, py);
            else        polygon.lineTo(px, py);
        }
        polygon.closePath();

        // Semi-transparent fill
        g2.setColor(new Color(
                chartColor.getRed(),
                chartColor.getGreen(),
                chartColor.getBlue(),
                FILL_ALPHA));
        g2.fill(polygon);

        // Solid outline
        g2.setStroke(new BasicStroke(OUTLINE_WIDTH, BasicStroke.CAP_ROUND,
                                     BasicStroke.JOIN_ROUND));
        g2.setColor(chartColor);
        g2.draw(polygon);
    }

    /** Short text labels at the tip of each axis, offset beyond the outermost ring. */
    private void drawAxisLabels(Graphics2D g2, double cx, double cy,
                                 double radius, int n, double[] angles) {

        g2.setFont(LABEL_FONT);
        FontMetrics fm = g2.getFontMetrics();

        // Push labels just past the outermost ring
        double labelR = radius + 14;

        for (int i = 0; i < n; i++) {
            String label = AXIS_LABELS.get(i);
            int    tw    = fm.stringWidth(label);
            int    th    = fm.getAscent();

            double lx = cx + labelR * Math.cos(angles[i]);
            double ly = cy + labelR * Math.sin(angles[i]);

            // Nudge so the label centre sits on the computed point
            float drawX = (float) (lx - tw / 2.0);
            float drawY = (float) (ly + th / 2.0);

            // Score value for this axis — small coloured chip
            double score     = values[i];
            Color  scoreCol  = scoreColor(score);

            // Label shadow for legibility on any background
            g2.setColor(new Color(255, 255, 255, 180));
            g2.drawString(label, drawX + 1, drawY + 1);

            g2.setColor(new Color(30, 40, 60));
            g2.drawString(label, drawX, drawY);

            // Tiny score badge below label
            String badge  = String.format("%.1f", score);
            int    bw     = fm.stringWidth(badge);
            float  badgeX = (float) (lx - bw / 2.0);
            float  badgeY = drawY + fm.getDescent() + 3;

            g2.setFont(RING_FONT);
            g2.setColor(scoreCol);
            g2.drawString(badge, badgeX, badgeY);
            g2.setFont(LABEL_FONT);   // reset
        }
    }

    /** Small filled circles at each data point vertex for precision reading. */
    private void drawValueDots(Graphics2D g2, double cx, double cy,
                                double radius, int n, double[] angles) {

        int dotR = 3;
        for (int i = 0; i < n; i++) {
            double r  = radius * (values[i] / 10.0);
            double px = cx + r * Math.cos(angles[i]);
            double py = cy + r * Math.sin(angles[i]);

            // White halo
            g2.setColor(Color.WHITE);
            g2.fillOval((int)(px - dotR - 1), (int)(py - dotR - 1),
                        (dotR + 1) * 2, (dotR + 1) * 2);

            // Coloured dot
            g2.setColor(chartColor);
            g2.fillOval((int)(px - dotR), (int)(py - dotR), dotR * 2, dotR * 2);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void applyRenderingHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                            RenderingHints.VALUE_STROKE_PURE);
    }

    /**
     * Maps a 0–10 score to a traffic-light colour consistent with
     * the app's existing {@code UIUtils.getScoreColor()} conventions.
     */
    private static Color scoreColor(double score) {
        if (score >= 8.0) return new Color(34, 197, 94);    // green
        if (score >= 6.5) return new Color(59, 130, 246);   // blue
        if (score >= 5.0) return new Color(245, 158, 11);   // amber
        return                     new Color(239, 68, 68);  // red
    }
}