package com.episim.gui;

import com.episim.model.DailyRecord;
import com.episim.model.HealthState;
import com.episim.model.Intervention;
import com.episim.util.Theme;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Custom-painted SEIR line chart — no external charting library. Data is pushed in via setData(), which
 * only ever calls repaint(); all drawing happens inside paintComponent(), as Swing requires.
 */
public class EpidemicCurvePanel extends JPanel {

    private record StateSeries(HealthState state, ToIntFunction<DailyRecord> extractor) {
    }

    private static final List<StateSeries> SERIES = List.of(
            new StateSeries(HealthState.SUSCEPTIBLE, DailyRecord::getSusceptible),
            new StateSeries(HealthState.EXPOSED, DailyRecord::getExposed),
            new StateSeries(HealthState.INFECTED, DailyRecord::getInfected),
            new StateSeries(HealthState.HOSPITALISED, DailyRecord::getHospitalised),
            new StateSeries(HealthState.RECOVERED, DailyRecord::getRecovered),
            new StateSeries(HealthState.DECEASED, DailyRecord::getDeceased));

    private static final int MARGIN_LEFT = 65;
    private static final int MARGIN_RIGHT = 30;
    private static final int MARGIN_TOP = 45;
    private static final int MARGIN_BOTTOM = 55;

    private List<DailyRecord> history = List.of();
    private List<Intervention> interventions = List.of();
    private int hospitalCapacity;

    public EpidemicCurvePanel() {
        setBackground(Theme.SURFACE);
    }

    public void setData(List<DailyRecord> history, List<Intervention> interventions, int hospitalCapacity) {
        this.history = history;
        this.interventions = interventions;
        this.hospitalCapacity = hospitalCapacity;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.SURFACE);
        g2.fillRect(0, 0, getWidth(), getHeight());

        if (history.isEmpty()) {
            drawEmptyState(g2);
            return;
        }

        int plotWidth = getWidth() - MARGIN_LEFT - MARGIN_RIGHT;
        int plotHeight = getHeight() - MARGIN_TOP - MARGIN_BOTTOM;
        if (plotWidth <= 10 || plotHeight <= 10) {
            return;
        }

        int minDay = history.get(0).getDayNumber();
        int maxDay = history.get(history.size() - 1).getDayNumber();
        int dayRange = Math.max(1, maxDay - minDay);
        int yMax = computeYMax();

        shadeInterventionRanges(g2, minDay, dayRange, plotWidth, plotHeight);
        drawGridAndAxisLabels(g2, minDay, maxDay, dayRange, plotWidth, plotHeight, yMax);
        drawAxes(g2, plotWidth, plotHeight);
        drawAxisTitles(g2, plotWidth, plotHeight);
        drawHospitalCapacityLine(g2, plotWidth, plotHeight, yMax);
        drawSeries(g2, minDay, dayRange, plotWidth, plotHeight, yMax);
        drawLegend(g2, MARGIN_LEFT, 8);
    }

    private int computeYMax() {
        int maxValue = 1;
        for (DailyRecord record : history) {
            for (StateSeries series : SERIES) {
                maxValue = Math.max(maxValue, series.extractor().applyAsInt(record));
            }
        }
        maxValue = Math.max(maxValue, hospitalCapacity);
        return niceCeiling(maxValue);
    }

    private int niceCeiling(int value) {
        int padded = (int) Math.ceil(value * 1.1);
        int magnitude = 1;
        while (magnitude * 10 <= padded) {
            magnitude *= 10;
        }
        int step = Math.max(1, magnitude / 5);
        return ((padded / step) + 1) * step;
    }

    private void shadeInterventionRanges(Graphics2D g2, int minDay, int dayRange, int plotWidth, int plotHeight) {
        int maxDay = minDay + dayRange;
        Color shade = new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 55);
        g2.setColor(shade);
        for (Intervention intervention : interventions) {
            int start = Math.max(intervention.getStartDay(), minDay);
            int end = Math.min(intervention.getEndDay(), maxDay);
            if (end < start) {
                continue;
            }
            int x1 = MARGIN_LEFT + (int) ((start - minDay) / (double) dayRange * plotWidth);
            int x2 = MARGIN_LEFT + (int) ((end - minDay) / (double) dayRange * plotWidth);
            g2.fillRect(x1, MARGIN_TOP, Math.max(1, x2 - x1), plotHeight);
        }
    }

    private void drawGridAndAxisLabels(Graphics2D g2, int minDay, int maxDay, int dayRange, int plotWidth,
                                        int plotHeight, int yMax) {
        g2.setFont(Theme.BODY_FONT);
        int horizontalLines = 5;
        for (int i = 0; i <= horizontalLines; i++) {
            int y = MARGIN_TOP + plotHeight - (int) ((double) i / horizontalLines * plotHeight);
            g2.setColor(Theme.BORDER);
            g2.drawLine(MARGIN_LEFT, y, MARGIN_LEFT + plotWidth, y);
            String label = Integer.toString((int) ((double) i / horizontalLines * yMax));
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawString(label, MARGIN_LEFT - g2.getFontMetrics().stringWidth(label) - 8, y + 4);
        }

        int xTicks = Math.min(10, dayRange);
        for (int i = 0; i <= xTicks; i++) {
            int day = minDay + (int) Math.round((double) i / xTicks * dayRange);
            int x = MARGIN_LEFT + (int) ((day - minDay) / (double) dayRange * plotWidth);
            g2.setColor(Theme.BORDER);
            g2.drawLine(x, MARGIN_TOP, x, MARGIN_TOP + plotHeight);
            String label = "Day " + day;
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawString(label, x - g2.getFontMetrics().stringWidth(label) / 2, MARGIN_TOP + plotHeight + 18);
        }
    }

    private void drawAxes(Graphics2D g2, int plotWidth, int plotHeight) {
        g2.setColor(Theme.SLATE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(MARGIN_LEFT, MARGIN_TOP, MARGIN_LEFT, MARGIN_TOP + plotHeight);
        g2.drawLine(MARGIN_LEFT, MARGIN_TOP + plotHeight, MARGIN_LEFT + plotWidth, MARGIN_TOP + plotHeight);
    }

    private void drawAxisTitles(Graphics2D g2, int plotWidth, int plotHeight) {
        g2.setFont(Theme.BODY_BOLD_FONT);
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.drawString("Day", MARGIN_LEFT + plotWidth / 2 - 12, getHeight() - 8);

        Graphics2D rotated = (Graphics2D) g2.create();
        rotated.rotate(-Math.PI / 2);
        rotated.setFont(Theme.BODY_BOLD_FONT);
        rotated.setColor(Theme.TEXT_PRIMARY);
        rotated.drawString("People", -(MARGIN_TOP + plotHeight / 2) - 20, 16);
        rotated.dispose();
    }

    private void drawHospitalCapacityLine(Graphics2D g2, int plotWidth, int plotHeight, int yMax) {
        if (hospitalCapacity <= 0) {
            return;
        }
        int y = MARGIN_TOP + plotHeight - (int) (hospitalCapacity / (double) yMax * plotHeight);
        Graphics2D dashed = (Graphics2D) g2.create();
        dashed.setColor(Theme.DANGER);
        dashed.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{7, 5}, 0));
        dashed.drawLine(MARGIN_LEFT, y, MARGIN_LEFT + plotWidth, y);
        dashed.dispose();
    }

    private void drawSeries(Graphics2D g2, int minDay, int dayRange, int plotWidth, int plotHeight, int yMax) {
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (StateSeries series : SERIES) {
            g2.setColor(series.state().getColor());
            int prevX = -1;
            int prevY = -1;
            for (DailyRecord record : history) {
                int x = MARGIN_LEFT + (int) ((record.getDayNumber() - minDay) / (double) dayRange * plotWidth);
                int value = series.extractor().applyAsInt(record);
                int y = MARGIN_TOP + plotHeight - (int) (value / (double) yMax * plotHeight);
                if (prevX >= 0) {
                    g2.drawLine(prevX, prevY, x, y);
                }
                prevX = x;
                prevY = y;
            }
        }
    }

    private void drawLegend(Graphics2D g2, int startX, int y) {
        g2.setFont(Theme.BODY_FONT);
        int x = startX;
        for (StateSeries series : SERIES) {
            g2.setColor(series.state().getColor());
            g2.fillRect(x, y, 12, 12);
            g2.setColor(Theme.TEXT_PRIMARY);
            String label = series.state().getLabel();
            g2.drawString(label, x + 16, y + 11);
            x += 16 + g2.getFontMetrics().stringWidth(label) + 20;
        }

        Graphics2D dashed = (Graphics2D) g2.create();
        dashed.setColor(Theme.DANGER);
        dashed.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{7, 5}, 0));
        dashed.drawLine(x, y + 6, x + 22, y + 6);
        dashed.dispose();
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.drawString("Hospital Capacity", x + 28, y + 11);
    }

    private void drawEmptyState(Graphics2D g2) {
        g2.setFont(Theme.HEADER_FONT);
        g2.setColor(Theme.TEXT_SECONDARY);
        String message = "No simulation data yet — configure and start a run.";
        int textWidth = g2.getFontMetrics().stringWidth(message);
        g2.drawString(message, (getWidth() - textWidth) / 2, getHeight() / 2);
    }
}
