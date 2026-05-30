package com.smartcity.utils;

import java.awt.*;

public class UIConstants {

    // ===== MODERN PREMIUM COLOR SYSTEM =====

    // Primary Brand Colors
    public static final Color PRIMARY_COLOR = new Color(37, 99, 235);        // Blue
    public static final Color PRIMARY_DARK = new Color(15, 23, 42);          // Navy
    public static final Color PRIMARY_LIGHT = new Color(96, 165, 250);

    // Accent Colors
    public static final Color ACCENT_COLOR = new Color(16, 185, 129);        // Emerald
    public static final Color ACCENT_SOFT = new Color(220, 252, 231);

    // Background System
    public static final Color BACKGROUND_COLOR = new Color(241, 245, 249);
    public static final Color SURFACE_COLOR = new Color(248, 250, 252);
    public static final Color CARD_BACKGROUND = Color.WHITE;
    public static final Color HOVER_BACKGROUND = new Color(239, 246, 255);

    // Semantic Colors
    public static final Color SUCCESS_COLOR = new Color(34, 197, 94);
    public static final Color WARNING_COLOR = new Color(245, 158, 11);
    public static final Color DANGER_COLOR = new Color(239, 68, 68);
    public static final Color INFO_COLOR = new Color(59, 130, 246);

    // Text Colors
    public static final Color TEXT_PRIMARY = new Color(15, 23, 42);
    public static final Color TEXT_SECONDARY = new Color(100, 116, 139);
    public static final Color TEXT_TERTIARY = new Color(148, 163, 184);

    // White Variants
    public static final Color COLOR_WHITE = Color.WHITE;
    public static final Color COLOR_WHITE_70 = new Color(255, 255, 255, 180);

    // Border Colors
    public static final Color BORDER_COLOR = new Color(226, 232, 240);
    public static final Color BORDER_SUBTLE = new Color(241, 245, 249);
    public static final Color DIVIDER_COLOR = new Color(226, 232, 240);

    // Shadow
    public static final Color SHADOW_COLOR = new Color(15, 23, 42, 18);

    // ===== TYPOGRAPHY =====

    public static final Font DISPLAY_FONT =
            new Font("Segoe UI", Font.BOLD, 34);

    public static final Font TITLE_FONT =
            new Font("Segoe UI", Font.BOLD, 26);

    public static final Font SUBTITLE_FONT =
            new Font("Segoe UI", Font.BOLD, 18);

    public static final Font HEADING_FONT =
            new Font("Segoe UI", Font.BOLD, 15);

    public static final Font LABEL_FONT =
            new Font("Segoe UI", Font.PLAIN, 14);

    public static final Font SMALL_FONT =
            new Font("Segoe UI", Font.PLAIN, 12);

    public static final Font BUTTON_FONT =
            new Font("Segoe UI", Font.BOLD, 14);

    public static final Font SCORE_FONT =
            new Font("Segoe UI", Font.BOLD, 36);

    // ===== WINDOW =====

    public static final int WINDOW_WIDTH = 1400;
    public static final int WINDOW_HEIGHT = 900;

    public static final int MIN_WINDOW_WIDTH = 1000;
    public static final int MIN_WINDOW_HEIGHT = 700;

    // ===== SPACING =====

    public static final int SPACING_XS = 8;
    public static final int SPACING_SM = 12;
    public static final int SPACING_MD = 16;
    public static final int SPACING_LG = 24;
    public static final int SPACING_XL = 32;
    public static final int SPACING_XXL = 48;

    public static final int DEFAULT_PADDING = 20;

    // ===== COMPONENTS =====

    public static final int CARD_PADDING = 24;
    public static final int CARD_SPACING = 22;

    public static final int BORDER_RADIUS = 18;
    public static final int BORDER_RADIUS_LG = 28;
    public static final int BORDER_RADIUS_FULL = 40;

    public static final int BUTTON_HEIGHT = 44;
    public static final int BUTTON_HEIGHT_LARGE = 52;

    public static final int INPUT_HEIGHT = 44;
    public static final int INPUT_HEIGHT_LARGE = 48;

    // ===== SLIDER =====

    public static final int SLIDER_MIN = 0;
    public static final int SLIDER_MAX = 10;
    public static final int SLIDER_INITIAL = 5;

    // ===== SCORE BAR =====

    public static final int SCORE_BAR_HEIGHT = 14;
}