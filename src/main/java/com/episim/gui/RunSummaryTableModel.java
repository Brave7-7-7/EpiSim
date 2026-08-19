package com.episim.gui;

import com.episim.model.RunSummary;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** Backs the Run History tab's JTable over the v_run_summary rows — the persistence proof. */
public class RunSummaryTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Run ID", "Name", "Pathogen", "Population", "Days", "Peak Infections", "Total Deaths",
            "Days Over Capacity", "Status"
    };

    private List<RunSummary> summaries = new ArrayList<>();

    public void setSummaries(List<RunSummary> summaries) {
        this.summaries = new ArrayList<>(summaries);
        fireTableDataChanged();
    }

    public RunSummary getSummaryAt(int row) {
        return summaries.get(row);
    }

    @Override
    public int getRowCount() {
        return summaries.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        RunSummary summary = summaries.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> summary.getRunId();
            case 1 -> summary.getRunName();
            case 2 -> summary.getPathogenName();
            case 3 -> summary.getPopulationSize();
            case 4 -> summary.getTotalDays();
            case 5 -> summary.getPeakInfections();
            case 6 -> summary.getTotalDeaths();
            case 7 -> summary.getDaysOverCapacity();
            case 8 -> summary.getStatus();
            default -> null;
        };
    }
}
