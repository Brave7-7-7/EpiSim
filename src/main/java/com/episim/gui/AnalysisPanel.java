package com.episim.gui;

import com.episim.engine.OutbreakAnalyser;
import com.episim.io.DailyRecordCsv;
import com.episim.io.ReportIoException;
import com.episim.io.TextReportExporter;
import com.episim.model.DailyRecord;
import com.episim.model.District;
import com.episim.model.Intervention;
import com.episim.util.SimConstants;
import com.episim.util.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class AnalysisPanel extends JPanel {

    private final JLabel peakInfectionsValue = new JLabel("—");
    private final JLabel peakDayValue = new JLabel("—");
    private final JLabel peakBedsValue = new JLabel("—");
    private final JLabel attackRateValue = new JLabel("—");
    private final JLabel cfrValue = new JLabel("—");
    private final JLabel overCapacityValue = new JLabel("—");
    private final JLabel interventionCostValue = new JLabel("—");
    private final JTextArea narrativeArea = new JTextArea();

    private String runName = "Untitled Run";
    private List<DailyRecord> history = List.of();
    private List<District> districts = List.of();
    private List<Intervention> interventions = List.of();
    private int populationSize;

    private Consumer<List<DailyRecord>> onCsvImported = imported -> {
    };

    public AnalysisPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(buildStatGrid(), BorderLayout.NORTH);

        narrativeArea.setEditable(false);
        narrativeArea.setLineWrap(true);
        narrativeArea.setWrapStyleWord(true);
        narrativeArea.setFont(Theme.BODY_FONT);
        narrativeArea.setBackground(Theme.SURFACE);
        narrativeArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        add(new JScrollPane(narrativeArea), BorderLayout.CENTER);

        add(buildButtonBar(), BorderLayout.SOUTH);
        refreshStats();
    }

    private JComponent buildStatGrid() {
        JPanel grid = new JPanel(new GridLayout(2, 4, 12, 12));
        grid.setBackground(Theme.BACKGROUND);
        grid.add(statCard("Peak Infections", peakInfectionsValue));
        grid.add(statCard("Peak Day", peakDayValue));
        grid.add(statCard("Peak Hospital Occupancy", peakBedsValue));
        grid.add(statCard("Attack Rate", attackRateValue));
        grid.add(statCard("Case Fatality Rate", cfrValue));
        grid.add(statCard("Days Over Capacity", overCapacityValue));
        grid.add(statCard("Total Intervention Cost", interventionCostValue));
        return grid;
    }

    private JComponent statCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.BODY_FONT);
        titleLabel.setForeground(Theme.TEXT_SECONDARY);
        valueLabel.setFont(Theme.TITLE_FONT.deriveFont(20f));
        valueLabel.setForeground(Theme.PRIMARY_DARK);
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setBackground(Theme.BACKGROUND);

        JButton exportCsvButton = new JButton("Export CSV");
        exportCsvButton.addActionListener(e -> exportCsv());

        JButton exportTextButton = new JButton("Export Text Report");
        exportTextButton.addActionListener(e -> exportTextReport());

        JButton importCsvButton = new JButton("Import CSV");
        importCsvButton.addActionListener(e -> importCsv());

        bar.add(exportCsvButton);
        bar.add(exportTextButton);
        bar.add(importCsvButton);
        return bar;
    }

    public void setOnCsvImported(Consumer<List<DailyRecord>> onCsvImported) {
        this.onCsvImported = onCsvImported;
    }

    public void setData(String runName, List<DailyRecord> history, List<District> districts,
                         List<Intervention> interventions, int populationSize) {
        this.runName = runName;
        this.history = history;
        this.districts = districts;
        this.interventions = interventions;
        this.populationSize = populationSize;
        refreshStats();
    }

    private void refreshStats() {
        if (history.isEmpty()) {
            peakInfectionsValue.setText("—");
            peakDayValue.setText("—");
            peakBedsValue.setText("—");
            attackRateValue.setText("—");
            cfrValue.setText("—");
            overCapacityValue.setText("—");
            interventionCostValue.setText("—");
            narrativeArea.setText("No simulation data is available to summarise.");
            return;
        }

        peakInfectionsValue.setText(Integer.toString(OutbreakAnalyser.peakInfections(history)));
        peakDayValue.setText("Day " + OutbreakAnalyser.peakDay(history));
        peakBedsValue.setText(Integer.toString(OutbreakAnalyser.peakHospitalOccupancy(history)));
        attackRateValue.setText(String.format(SimConstants.DATA_LOCALE, "%.1f%%",
                OutbreakAnalyser.attackRate(history, populationSize) * 100));
        cfrValue.setText(String.format(SimConstants.DATA_LOCALE, "%.2f%%",
                OutbreakAnalyser.caseFatalityRate(history) * 100));
        overCapacityValue.setText(OutbreakAnalyser.daysHospitalOverCapacity(history) + " days");
        interventionCostValue.setText(String.format(SimConstants.DATA_LOCALE, "RM %.2f",
                OutbreakAnalyser.totalInterventionCost(interventions)));

        narrativeArea.setText(OutbreakAnalyser.generateNarrativeSummary(history, interventions, populationSize));
        narrativeArea.setCaretPosition(0);
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(runName.replaceAll("\\s+", "_") + "_daily_records.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                DailyRecordCsv.export(history, chooser.getSelectedFile().toPath());
                JOptionPane.showMessageDialog(this, "CSV exported successfully.", "Export CSV",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (ReportIoException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Export failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportTextReport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(runName.replaceAll("\\s+", "_") + "_report.txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                TextReportExporter.export(runName, history, districts, interventions, populationSize,
                        chooser.getSelectedFile().toPath());
                JOptionPane.showMessageDialog(this, "Text report exported successfully.", "Export Text Report",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (ReportIoException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Export failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importCsv() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                List<DailyRecord> imported = DailyRecordCsv.importRecords(chooser.getSelectedFile().toPath());
                this.history = imported;
                this.runName = chooser.getSelectedFile().getName();
                refreshStats();
                onCsvImported.accept(imported);
                JOptionPane.showMessageDialog(this, "Imported " + imported.size() + " daily records.",
                        "Import CSV", JOptionPane.INFORMATION_MESSAGE);
            } catch (ReportIoException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Import failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
