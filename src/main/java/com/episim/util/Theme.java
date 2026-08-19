package com.episim.util;

import java.awt.Color;
import java.awt.Font;

/**
 * Central colour and font palette for the Swing GUI — a professional
 * teal/slate scheme appropriate for a public-health application.
 */
public final class Theme {

    public static final Color PRIMARY = new Color(0x00, 0x6D, 0x77);
    public static final Color PRIMARY_DARK = new Color(0x00, 0x4D, 0x54);
    public static final Color ACCENT = new Color(0x83, 0xC5, 0xBE);

    public static final Color SLATE = new Color(0x2B, 0x2D, 0x42);
    public static final Color SLATE_LIGHT = new Color(0x8D, 0x99, 0xAE);

    public static final Color BACKGROUND = new Color(0xF3, 0xF6, 0xF7);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER = new Color(0xD9, 0xDE, 0xE1);

    public static final Color TEXT_PRIMARY = new Color(0x1B, 0x1F, 0x23);
    public static final Color TEXT_SECONDARY = new Color(0x5A, 0x63, 0x6B);
    public static final Color TEXT_ON_PRIMARY = Color.WHITE;

    public static final Color SUCCESS = new Color(0x2E, 0x7D, 0x32);
    public static final Color WARNING = new Color(0xED, 0x6C, 0x02);
    public static final Color DANGER = new Color(0xC6, 0x28, 0x28);

    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font BODY_BOLD_FONT = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font MONO_FONT = new Font("Consolas", Font.PLAIN, 12);

    private Theme() {
    }
}
