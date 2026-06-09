package com.smartcity.view;

import com.smartcity.model.LiveMetricType;
import com.smartcity.model.MetricSourceStatus;
import com.smartcity.model.RecommendationResult;
import com.smartcity.service.APIHealthManager;
import com.smartcity.service.ApiService;
import com.smartcity.utils.UIConstants;
import com.smartcity.utils.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class LiveDataStatusPanel extends JPanel {
    public LiveDataStatusPanel(List<RecommendationResult> results) {
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        RecommendationResult representative = results == null || results.isEmpty() ? null : results.get(0);
        Map<LiveMetricType, MetricSourceStatus> statuses = representative == null
                ? new EnumMap<>(LiveMetricType.class)
                : representative.getMetricStatuses();
        Map<LiveMetricType, APIHealthManager.ApiHealthSnapshot> health = ApiService.getHealthManager().snapshot();

        add(createSummary(representative), BorderLayout.NORTH);
        add(createMetricGrid(statuses, health), BorderLayout.CENTER);
    }

    private JPanel createSummary(RecommendationResult result) {
        JPanel summary = UIUtils.createRoundedPanel(UIConstants.CARD_BACKGROUND);
        summary.setLayout(new BorderLayout(16, 0));
        summary.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel title = UIUtils.createSubtitleLabel("Live Data Status");
        JLabel subtitle = UIUtils.createSecondaryLabel(result == null
                ? "Waiting for recommendation data"
                : "Primary scoring uses live metrics with per-metric fallback");
        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(subtitle);

        JPanel confidence = createConfidenceChip(result);
        summary.add(text, BorderLayout.WEST);
        summary.add(confidence, BorderLayout.EAST);
        return summary;
    }

    private JPanel createConfidenceChip(RecommendationResult result) {
        double confidenceValue = result == null ? 0.0 : result.getConfidenceScore();
        int confidence = (int) Math.round(confidenceValue * 100);
        JPanel chip = UIUtils.createFilledRoundedPanel(new Color(239, 246, 255));
        chip.setLayout(new BoxLayout(chip, BoxLayout.Y_AXIS));
        chip.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));

        JLabel value = new JLabel(confidence + "%");
        value.setFont(UIConstants.SUBTITLE_FONT);
        value.setForeground(confidence >= 70 ? UIConstants.SUCCESS_COLOR : UIConstants.WARNING_COLOR);
        value.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel label = UIUtils.createSmallLabel("Confidence");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        chip.add(value);
        chip.add(label);
        return chip;
    }

    private JPanel createMetricGrid(Map<LiveMetricType, MetricSourceStatus> statuses,
                                    Map<LiveMetricType, APIHealthManager.ApiHealthSnapshot> health) {
        JPanel grid = new JPanel(new GridLayout(1, LiveMetricType.values().length, 12, 0));
        grid.setOpaque(false);
        for (LiveMetricType type : LiveMetricType.values()) {
            grid.add(createMetricTile(type, statuses.get(type), health.get(type)));
        }
        return grid;
    }

    private JPanel createMetricTile(LiveMetricType type, MetricSourceStatus status,
                                    APIHealthManager.ApiHealthSnapshot health) {
        MetricSourceStatus displayStatus = status == null
                ? MetricSourceStatus.cached("No metric status available.")
                : status;
        String sourceText = displayStatus.getSource() == null ? "CACHED" : displayStatus.getSource().toUpperCase();
        boolean live = displayStatus.isLive();
        boolean local = "LOCAL".equalsIgnoreCase(sourceText);

        JPanel tile = UIUtils.createRoundedPanel(UIConstants.CARD_BACKGROUND);
        tile.setLayout(new BorderLayout(0, 10));
        tile.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel name = UIUtils.createHeadingLabel(type.getDisplayName());
        JLabel source = createSourceBadge(sourceText, live, local);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(name, BorderLayout.WEST);
        top.add(source, BorderLayout.EAST);

        JPanel meta = new JPanel(new GridLayout(4, 1, 0, 4));
        meta.setOpaque(false);
        meta.add(createMeta("Timestamp", compactTime(displayStatus.getTimestamp())));
        meta.add(createMeta("Success", health == null ? "0%" : Math.round(health.getSuccessRate() * 100) + "%"));
        meta.add(createMeta("Avg", health == null ? "0 ms" : health.getAverageResponseTimeMs() + " ms"));
        meta.add(createMeta("Reason", diagnosticReason(displayStatus, health)));
        if (health != null && health.getFailureCount() > 0) {
            tile.setToolTipText(health.getLastFailureReason());
        } else {
            tile.setToolTipText(displayStatus.getMessage());
        }

        tile.add(top, BorderLayout.NORTH);
        tile.add(meta, BorderLayout.CENTER);
        return tile;
    }

    private JLabel createSourceBadge(String text, boolean live, boolean local) {
        JLabel label = UIUtils.createSmallLabel(text);
        label.setOpaque(true);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setForeground(live ? new Color(21, 128, 61) : (local ? new Color(29, 78, 216) : new Color(146, 64, 14)));
        label.setBackground(live ? new Color(220, 252, 231) : (local ? new Color(219, 234, 254) : new Color(254, 243, 199)));
        label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        label.setPreferredSize(new Dimension(72, 24));
        return label;
    }

    private JPanel createMeta(String label, String value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        JLabel text = UIUtils.createSmallLabel(label + ": " + value);
        text.setForeground(UIConstants.TEXT_SECONDARY);
        row.add(text);
        return row;
    }

    private String diagnosticReason(MetricSourceStatus status, APIHealthManager.ApiHealthSnapshot health) {
        if (health != null && health.getFailureCount() > 0) {
            return compactReason(health.getLastFailureReason());
        }
        return compactReason(status.getMessage());
    }

    private String compactReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "OK";
        }
        return reason.length() > 22 ? reason.substring(0, 21) + "..." : reason;
    }

    private String compactTime(String timestamp) {
        if (timestamp == null || timestamp.isBlank() || "N/A".equals(timestamp)) {
            return "N/A";
        }
        int timeStart = timestamp.indexOf('T');
        if (timeStart >= 0 && timestamp.length() >= timeStart + 9) {
            return timestamp.substring(timeStart + 1, timeStart + 9);
        }
        return timestamp;
    }
}
