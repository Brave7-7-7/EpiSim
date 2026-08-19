package com.episim.model;

import java.awt.Color;

/**
 * The finite set of epidemiological states a {@link Person} can occupy.
 * Each constant carries display metadata used directly by the Swing charts.
 */
public enum HealthState {

    SUSCEPTIBLE("Susceptible", new Color(0x4C, 0xAF, 0x50)),
    EXPOSED("Exposed", new Color(0xFF, 0xC1, 0x07)),
    INFECTED("Infected", new Color(0xF4, 0x43, 0x36)),
    HOSPITALISED("Hospitalised", new Color(0x9C, 0x27, 0xB0)),
    RECOVERED("Recovered", new Color(0x21, 0x96, 0xF3)),
    DECEASED("Deceased", new Color(0x42, 0x42, 0x42));

    private final String label;
    private final Color color;

    HealthState(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    /** @return the human-readable display label, e.g. "Hospitalised" */
    public String getLabel() {
        return label;
    }

    /** @return the colour this state is drawn in on every chart and the Districts tab */
    public Color getColor() {
        return color;
    }

    /** @return {@code true} for {@link #INFECTED} and {@link #HOSPITALISED} — the states that drive transmission */
    public boolean isInfectious() {
        return this == INFECTED || this == HOSPITALISED;
    }
}
