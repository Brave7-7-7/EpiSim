package com.episim.gui;

import com.episim.model.HealthState;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/** Colours the State column cell using the Color carried on each HealthState constant. */
public class HealthStateCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                     boolean hasFocus, int row, int column) {
        Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof HealthState state && !isSelected) {
            Color background = state.getColor();
            setBackground(background);
            setForeground(readableTextColorFor(background));
            setText(state.getLabel());
        }
        setHorizontalAlignment(CENTER);
        return component;
    }

    private Color readableTextColorFor(Color background) {
        double luminance = (0.299 * background.getRed() + 0.587 * background.getGreen() + 0.114 * background.getBlue()) / 255.0;
        return luminance > 0.55 ? Color.BLACK : Color.WHITE;
    }
}
