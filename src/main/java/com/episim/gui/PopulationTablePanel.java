package com.episim.gui;

import com.episim.model.HealthState;
import com.episim.model.Person;
import com.episim.util.Theme;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

public class PopulationTablePanel extends JPanel {

    private final PersonTableModel tableModel = new PersonTableModel();
    private final JComboBox<String> stateFilterCombo = new JComboBox<>();
    private final JTextField searchField = new JTextField(16);

    public PopulationTablePanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        filterBar.setBackground(Theme.BACKGROUND);
        filterBar.add(new JLabel("Filter by state:"));
        stateFilterCombo.addItem("All");
        for (HealthState state : HealthState.values()) {
            stateFilterCombo.addItem(state.getLabel());
        }
        stateFilterCombo.addActionListener(e -> applyFilter());
        filterBar.add(stateFilterCombo);

        filterBar.add(new JLabel("Search name:"));
        searchField.getDocument().addDocumentListener((SimpleDocumentListener) e -> applyFilter());
        filterBar.add(searchField);

        JTable table = new JTable(tableModel);
        table.setRowHeight(22);
        table.getColumnModel().getColumn(5).setCellRenderer(new HealthStateCellRenderer());
        table.setAutoCreateRowSorter(true);

        add(filterBar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void setPopulation(List<Person> population) {
        tableModel.setPeople(population);
        applyFilter();
    }

    private void applyFilter() {
        int selected = stateFilterCombo.getSelectedIndex();
        HealthState stateFilter = selected <= 0 ? null : HealthState.values()[selected - 1];
        tableModel.applyFilter(stateFilter, searchField.getText());
    }

    /** Adapts the three DocumentListener methods to a single lambda-friendly callback. */
    @FunctionalInterface
    private interface SimpleDocumentListener extends DocumentListener {
        void onChange(DocumentEvent e);

        @Override
        default void insertUpdate(DocumentEvent e) {
            onChange(e);
        }

        @Override
        default void removeUpdate(DocumentEvent e) {
            onChange(e);
        }

        @Override
        default void changedUpdate(DocumentEvent e) {
            onChange(e);
        }
    }
}
