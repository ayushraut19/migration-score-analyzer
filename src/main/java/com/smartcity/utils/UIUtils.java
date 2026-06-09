package com.smartcity.utils;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class UIUtils {

    // ================= BUTTONS =================

    public static JButton createStyledButton(String text, Color buttonColor) {

        JButton button = new JButton(text) {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = buttonColor;

                if (getModel().isPressed()) {
                    bg = darkenColor(bg, 0.15);
                } else if (getModel().isRollover()) {
                    bg = lighter(bg, 0.92);
                }

                // Shadow
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fillRoundRect(
                        2,
                        4,
                        getWidth() - 4,
                        getHeight() - 4,
                        UIConstants.BORDER_RADIUS_FULL,
                        UIConstants.BORDER_RADIUS_FULL
                );

                // Button
                g2.setColor(bg);
                g2.fillRoundRect(
                        0,
                        0,
                        getWidth(),
                        getHeight(),
                        UIConstants.BORDER_RADIUS_FULL,
                        UIConstants.BORDER_RADIUS_FULL
                );

                g2.dispose();

                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
            }
        };

        button.setForeground(Color.WHITE);
        button.setFont(UIConstants.BUTTON_FONT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    public static JButton createSecondaryButton(String text) {

        JButton button = new JButton(text) {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = Color.WHITE;

                if (getModel().isRollover()) {
                    bg = UIConstants.HOVER_BACKGROUND;
                }

                g2.setColor(bg);

                g2.fillRoundRect(
                        0,
                        0,
                        getWidth(),
                        getHeight(),
                        UIConstants.BORDER_RADIUS_FULL,
                        UIConstants.BORDER_RADIUS_FULL
                );

                g2.setColor(UIConstants.BORDER_COLOR);

                g2.drawRoundRect(
                        0,
                        0,
                        getWidth() - 1,
                        getHeight() - 1,
                        UIConstants.BORDER_RADIUS_FULL,
                        UIConstants.BORDER_RADIUS_FULL
                );

                g2.dispose();

                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
            }
        };

        button.setForeground(UIConstants.TEXT_PRIMARY);
        button.setFont(UIConstants.BUTTON_FONT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    public static JButton createPillButton(String text, Color bg, Color fg) {

        JButton button = createStyledButton(text, bg);

        button.setForeground(fg);

        return button;
    }

    // ================= LABELS =================

    public static JLabel createDisplayLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(UIConstants.DISPLAY_FONT);
        label.setForeground(UIConstants.TEXT_PRIMARY);

        return label;
    }

    public static JLabel createTitleLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(UIConstants.TITLE_FONT);
        label.setForeground(UIConstants.TEXT_PRIMARY);

        return label;
    }

    public static JLabel createSubtitleLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(UIConstants.SUBTITLE_FONT);
        label.setForeground(UIConstants.TEXT_PRIMARY);

        return label;
    }

    public static JLabel createHeadingLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(UIConstants.HEADING_FONT);
        label.setForeground(UIConstants.TEXT_PRIMARY);

        return label;
    }

    public static JLabel createLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(UIConstants.LABEL_FONT);
        label.setForeground(UIConstants.TEXT_PRIMARY);

        return label;
    }

    public static JLabel createSecondaryLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(UIConstants.LABEL_FONT);
        label.setForeground(UIConstants.TEXT_SECONDARY);

        return label;
    }

    public static JLabel createSmallLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(UIConstants.SMALL_FONT);
        label.setForeground(UIConstants.TEXT_SECONDARY);

        return label;
    }

    // ================= COMBO BOX =================

    public static <T> void styleComboBox(JComboBox<T> combo) {

        combo.setFont(UIConstants.LABEL_FONT);

        combo.setBackground(Color.WHITE);

        combo.setForeground(UIConstants.TEXT_PRIMARY);

        combo.setBorder(new CompoundBorder(
                new LineBorder(UIConstants.BORDER_COLOR, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        combo.setPreferredSize(
                new Dimension(220, UIConstants.INPUT_HEIGHT_LARGE)
        );

        combo.setFocusable(false);
    }

    // ================= PANELS =================

    public static JPanel createRoundedPanel(Color background) {

        JPanel panel = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                // Shadow
                g2.setColor(new Color(15, 23, 42, 10));

                g2.fillRoundRect(
                        2,
                        4,
                        getWidth() - 4,
                        getHeight() - 4,
                        UIConstants.BORDER_RADIUS,
                        UIConstants.BORDER_RADIUS
                );

                // Card
                g2.setColor(background);

                g2.fillRoundRect(
                        0,
                        0,
                        getWidth() - 2,
                        getHeight() - 2,
                        UIConstants.BORDER_RADIUS,
                        UIConstants.BORDER_RADIUS
                );

                g2.dispose();

                super.paintComponent(g);
            }
        };

        panel.setOpaque(false);

        return panel;
    }

    public static JPanel createFilledRoundedPanel(Color background) {

        JPanel panel = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                g2.setColor(background);

                g2.fillRoundRect(
                        0,
                        0,
                        getWidth(),
                        getHeight(),
                        UIConstants.BORDER_RADIUS,
                        UIConstants.BORDER_RADIUS
                );

                g2.dispose();

                super.paintComponent(g);
            }
        };

        panel.setOpaque(false);

        return panel;
    }

    public static JPanel createGradientPanel(Color start, Color end) {

        JPanel panel = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                GradientPaint gradient = new GradientPaint(
                        0,
                        0,
                        start,
                        getWidth(),
                        getHeight(),
                        end
                );

                g2.setPaint(gradient);

                g2.fillRoundRect(
                        0,
                        0,
                        getWidth(),
                        getHeight(),
                        UIConstants.BORDER_RADIUS_LG,
                        UIConstants.BORDER_RADIUS_LG
                );

                g2.dispose();

                super.paintComponent(g);
            }
        };

        panel.setOpaque(false);

        return panel;
    }

    // ================= SCORE COLORS =================

    public static Color getScoreColor(double score) {

        if (score >= 8) {
            return UIConstants.SUCCESS_COLOR;
        }

        if (score >= 6.5) {
            return UIConstants.INFO_COLOR;
        }

        if (score >= 5) {
            return UIConstants.WARNING_COLOR;
        }

        return UIConstants.DANGER_COLOR;
    }

    // ================= UTILS =================

    public static Color darkenColor(Color color, double factor) {

        int r = Math.max(0, (int) (color.getRed() * (1 - factor)));
        int g = Math.max(0, (int) (color.getGreen() * (1 - factor)));
        int b = Math.max(0, (int) (color.getBlue() * (1 - factor)));

        return new Color(r, g, b, color.getAlpha());
    }

    public static Color lighter(Color color, double factor) {

        int r = Math.min(255,
                (int) (color.getRed()
                        + (255 - color.getRed()) * (1 - factor)));

        int g = Math.min(255,
                (int) (color.getGreen()
                        + (255 - color.getGreen()) * (1 - factor)));

        int b = Math.min(255,
                (int) (color.getBlue()
                        + (255 - color.getBlue()) * (1 - factor)));

        return new Color(r, g, b, color.getAlpha());
    }

    // ================= BROWSER =================

    public static void openURL(String url) {

        try {

            if (Desktop.isDesktopSupported()) {

                Desktop.getDesktop().browse(
                        new java.net.URI(url)
                );
            }

        } catch (Exception e) {

            System.out.println("Unable to open URL");
        }
    }
}