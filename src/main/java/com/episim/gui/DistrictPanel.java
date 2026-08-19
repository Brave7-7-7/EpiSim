package com.episim.gui;

import com.episim.model.District;
import com.episim.model.HealthState;
import com.episim.util.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** One card per district: population, a custom-painted SEIR breakdown bar, and bed occupancy. */
public class DistrictPanel extends JPanel {

    private final JPanel cardContainer = new JPanel(new GridLayout(0, 2, 16, 16));

    public DistrictPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        cardContainer.setBackground(Theme.BACKGROUND);
        cardContainer.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(new JScrollPane(cardContainer), BorderLayout.CENTER);
    }

    public void setDistricts(Map<String, District> districts) {
        cardContainer.removeAll();
        for (District district : new TreeMap<>(districts).values()) {
            cardContainer.add(new DistrictCard(district));
        }
        cardContainer.revalidate();
        cardContainer.repaint();
    }

    private static class DistrictCard extends JPanel {

        DistrictCard(District district) {
            setLayout(new BorderLayout(8, 8));
            setBackground(Theme.SURFACE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER),
                    BorderFactory.createEmptyBorder(12, 14, 12, 14)));

            JLabel title = new JLabel(district.getName() + " (" + district.getId() + ")");
            title.setFont(Theme.HEADER_FONT);
            title.setForeground(Theme.TEXT_PRIMARY);

            // district.getPopulation() is the DESIGN population from the district table, not how many
            // residents this run actually assigned here — the resident list is the source of truth for
            // that (see District.addResident(), called once per generated Person).
            JLabel populationLabel = new JLabel("Population: " + district.getResidents().size());
            populationLabel.setFont(Theme.BODY_FONT);
            populationLabel.setForeground(Theme.TEXT_SECONDARY);

            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(Theme.SURFACE);
            header.add(title, BorderLayout.NORTH);
            header.add(populationLabel, BorderLayout.SOUTH);

            SeirBar seirBar = new SeirBar(district.stateBreakdown());
            seirBar.setPreferredSize(new Dimension(100, 22));

            int occupied = district.occupiedBeds();
            int capacity = Math.max(district.getHospitalCapacity(), 1);
            JProgressBar bedsBar = new JProgressBar(0, capacity);
            bedsBar.setValue(Math.min(occupied, capacity));
            bedsBar.setStringPainted(true);
            bedsBar.setString(occupied + " / " + district.getHospitalCapacity() + " beds occupied");
            bedsBar.setForeground(district.isHospitalOverwhelmed() ? Theme.DANGER : Theme.PRIMARY);

            JPanel body = new JPanel(new BorderLayout(6, 6));
            body.setBackground(Theme.SURFACE);
            JLabel seirLabel = new JLabel("Current SEIR breakdown");
            seirLabel.setFont(Theme.BODY_FONT);
            seirLabel.setForeground(Theme.TEXT_SECONDARY);
            body.add(seirLabel, BorderLayout.NORTH);
            body.add(seirBar, BorderLayout.CENTER);
            body.add(bedsBar, BorderLayout.SOUTH);

            add(header, BorderLayout.NORTH);
            add(body, BorderLayout.CENTER);
        }
    }

    /** A small custom-painted horizontal bar showing proportional segments per HealthState. */
    private static class SeirBar extends JPanel {

        private final Map<HealthState, Integer> breakdown;

        SeirBar(Map<HealthState, Integer> breakdown) {
            this.breakdown = breakdown;
            setBackground(Theme.SURFACE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int total = breakdown.values().stream().mapToInt(Integer::intValue).sum();
            int width = getWidth();
            int height = getHeight();

            g2.setColor(Theme.BORDER);
            g2.fillRect(0, 0, width, height);

            if (total == 0) {
                return;
            }

            int x = 0;
            for (HealthState state : List.of(HealthState.SUSCEPTIBLE, HealthState.EXPOSED, HealthState.INFECTED,
                    HealthState.HOSPITALISED, HealthState.RECOVERED, HealthState.DECEASED)) {
                int count = breakdown.getOrDefault(state, 0);
                if (count == 0) {
                    continue;
                }
                int segmentWidth = (int) Math.round(count / (double) total * width);
                g2.setColor(state.getColor());
                g2.fillRect(x, 0, segmentWidth, height);
                x += segmentWidth;
            }
        }
    }
}
