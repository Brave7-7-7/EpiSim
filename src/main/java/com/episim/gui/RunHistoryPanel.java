package com.episim.gui;

import com.episim.dao.EventLogDao;
import com.episim.dao.InterventionDao;
import com.episim.dao.SimulationRunDao;
import com.episim.model.EventLogEntry;
import com.episim.model.Intervention;
import com.episim.model.RunSummary;
import com.episim.util.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The persistence proof: lists every row from v_run_summary and lets the user drill into a past run's
 * interventions and recent events, all read straight from SQLite via SwingWorker so the UI never blocks.
 */
public class RunHistoryPanel extends JPanel {

    private final RunSummaryTableModel tableModel = new RunSummaryTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextArea detailArea = new JTextArea();
    private final JButton loadButton = new JButton("Load Selected Run");
    private final JButton deleteButton = new JButton("Delete Run");
    private final JButton refreshButton = new JButton("Refresh");

    private final SimulationRunDao simulationRunDao = new SimulationRunDao();
    private final InterventionDao interventionDao = new InterventionDao();
    private final EventLogDao eventLogDao = new EventLogDao();

    private IntConsumer onLoadRunRequested = runId -> {
    };

    public RunHistoryPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        table.setRowHeight(22);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshDetailPane();
            }
        });

        detailArea.setEditable(false);
        detailArea.setFont(Theme.MONO_FONT);
        detailArea.setBackground(Theme.SURFACE);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(table), new JScrollPane(detailArea));
        splitPane.setResizeWeight(0.55);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonBar.setBackground(Theme.BACKGROUND);
        loadButton.addActionListener(e -> requestLoadSelectedRun());
        deleteButton.addActionListener(e -> deleteSelectedRun());
        refreshButton.addActionListener(e -> refresh());
        buttonBar.add(loadButton);
        buttonBar.add(deleteButton);
        buttonBar.add(refreshButton);

        add(splitPane, BorderLayout.CENTER);
        add(buttonBar, BorderLayout.SOUTH);
    }

    public void setOnLoadRunRequested(IntConsumer callback) {
        this.onLoadRunRequested = callback;
    }

    public void refresh() {
        SwingWorker<List<RunSummary>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<RunSummary> doInBackground() throws Exception {
                return simulationRunDao.findAllSummaries();
            }

            @Override
            protected void done() {
                try {
                    tableModel.setSummaries(get());
                } catch (Exception e) {
                    showError("Failed to load run history", e);
                }
            }
        };
        worker.execute();
    }

    private void requestLoadSelectedRun() {
        int row = selectedModelRow();
        if (row < 0) {
            return;
        }
        onLoadRunRequested.accept(tableModel.getSummaryAt(row).getRunId());
    }

    private void deleteSelectedRun() {
        int row = selectedModelRow();
        if (row < 0) {
            return;
        }
        RunSummary summary = tableModel.getSummaryAt(row);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete run '" + summary.getRunName() + "' (id " + summary.getRunId() + ")? This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Relies on ON DELETE CASCADE to remove the run's persons, daily_records, interventions and event_log rows.
                simulationRunDao.delete(summary.getRunId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    refresh();
                    detailArea.setText("");
                } catch (Exception e) {
                    showError("Failed to delete run", e);
                }
            }
        };
        worker.execute();
    }

    private void refreshDetailPane() {
        int row = selectedModelRow();
        if (row < 0) {
            detailArea.setText("");
            return;
        }
        int runId = tableModel.getSummaryAt(row).getRunId();

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                List<Intervention> interventions = interventionDao.findByRun(runId);
                List<EventLogEntry> events = eventLogDao.findByRun(runId, 20);

                StringBuilder text = new StringBuilder();
                text.append("Interventions\n-------------\n");
                if (interventions.isEmpty()) {
                    text.append("(none)\n");
                } else {
                    for (Intervention intervention : interventions) {
                        text.append(String.format("Day %d-%d: %s%n",
                                intervention.getStartDay(), intervention.getEndDay(), intervention.getDescription()));
                    }
                }

                text.append("\nRecent Events (last 20)\n-----------------------\n");
                if (events.isEmpty()) {
                    text.append("(none)\n");
                } else {
                    for (EventLogEntry event : events) {
                        text.append(String.format("[Day %d] %s — %s%n",
                                event.getDayNumber(), event.getEventType(), event.getDescription()));
                    }
                }
                return text.toString();
            }

            @Override
            protected void done() {
                try {
                    detailArea.setText(get());
                    detailArea.setCaretPosition(0);
                } catch (Exception e) {
                    showError("Failed to load run details", e);
                }
            }
        };
        worker.execute();
    }

    private int selectedModelRow() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? -1 : table.convertRowIndexToModel(viewRow);
    }

    private void showError(String title, Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }
}
