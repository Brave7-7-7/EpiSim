package com.episim.gui;

import com.episim.engine.SimulationConfig;
import com.episim.model.ContactTracing;
import com.episim.model.Intervention;
import com.episim.model.Lockdown;
import com.episim.model.MaskMandate;
import com.episim.model.Pathogen;
import com.episim.model.VaccinationDrive;
import com.episim.util.AppConfig;
import com.episim.util.SimConstants;
import com.episim.util.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Left-hand configuration and playback controls. Building an Intervention here means constructing the
 * correct concrete subclass directly from the enabled checkbox — the GUI never has to know about a
 * shared "generic" intervention type.
 */
public class ControlPanel extends JPanel {

    /** Playback state, driving which buttons and config fields are enabled — see {@link #setRunState}. */
    public enum RunState { NOT_STARTED, RUNNING, PAUSED, FINISHED }

    /** Callback interface the owning dashboard implements to react to button clicks (observer pattern). */
    public interface Listener {

        /**
         * @param config        the validated configuration to start a new run with
         * @param interventions the validated, enabled interventions to attach to the new run
         */
        void onStartRequested(SimulationConfig config, List<Intervention> interventions);

        /** Fired when the user clicks "Pause". */
        void onPauseRequested();

        /**
         * @param config        the validated configuration to use if a run must be created first
         * @param interventions the validated, enabled interventions to use if a run must be created first
         */
        void onStepRequested(SimulationConfig config, List<Intervention> interventions);

        /** Fired when the user clicks "Reset". */
        void onResetRequested();

        /** Fired when the user clicks "Abort". */
        void onAbortRequested();
    }

    private static final double LOCKDOWN_COST_PER_DAY = 50_000.0;
    private static final double MASK_MANDATE_COST_PER_DAY = 5_000.0;
    private static final double VACCINATION_DRIVE_COST_PER_DAY = 20_000.0;
    private static final double CONTACT_TRACING_COST_PER_DAY = 15_000.0;

    // Not exposed as GUI inputs (the spec's widget list only covers population/days/seed
    // infections/random seed); these are reasonable fixed demographic proportions.
    private static final double HEALTHCARE_WORKER_RATIO = 0.05;
    private static final double ELDERLY_RATIO = 0.12;

    private static final DateTimeFormatter RUN_NAME_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", SimConstants.DATA_LOCALE);

    // Target width of the west region's usable content — see the constructor for how the scroll pane's
    // own preferred width is derived from this plus the vertical scrollbar's width.
    private static final int CONTROL_PANEL_WIDTH = 340;
    private static final int LABEL_COLUMN_WIDTH = 112;
    private static final Dimension FIELD_SIZE = new Dimension(150, 24);

    private final JComboBox<Pathogen> pathogenCombo = new JComboBox<>();
    private final JSpinner populationSpinner = new JSpinner(new SpinnerNumberModel(2000, 500, 20000, 500));
    private final JSpinner daysSpinner = new JSpinner(new SpinnerNumberModel(120, 30, 365, 5));
    private final JSpinner seedInfectionsSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
    private final JSpinner randomSeedSpinner = new JSpinner(new SpinnerNumberModel(42, 0, Integer.MAX_VALUE, 1));

    private final JButton startButton = new JButton("Start");
    private final JButton pauseButton = new JButton("Pause");
    private final JButton stepButton = new JButton("Step One Day");
    private final JButton resetButton = new JButton("Reset");
    private final JButton abortButton = new JButton("Abort");

    private final List<InterventionRow> interventionRows = new ArrayList<>();

    /**
     * @param pathogens the pathogens to offer in the picker, typically loaded from {@code PathogenDao} at startup
     * @param listener  callback for Start/Pause/Step/Reset/Abort button clicks
     */
    public ControlPanel(List<Pathogen> pathogens, Listener listener) {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        AppConfig appConfig = AppConfig.load();
        populationSpinner.setValue(clamp(appConfig.getDefaultPopulation(), 500, 20000));
        daysSpinner.setValue(clamp(appConfig.getDefaultDays(), 30, 365));

        for (Pathogen pathogen : pathogens) {
            pathogenCombo.addItem(pathogen);
        }
        pathogenCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Pathogen pathogen) {
                    setText(pathogen.getName());
                }
                return c;
            }
        });

        populationSpinner.setPreferredSize(FIELD_SIZE);
        daysSpinner.setPreferredSize(FIELD_SIZE);
        seedInfectionsSpinner.setPreferredSize(FIELD_SIZE);
        randomSeedSpinner.setPreferredSize(FIELD_SIZE);

        ScrollableContentPanel content = new ScrollableContentPanel();
        content.setBackground(Theme.BACKGROUND);

        content.add(sectionLabel("Scenario"));
        content.add(formRow("Pathogen:", pathogenCombo));
        content.add(formRow("Population size:", populationSpinner));
        content.add(formRow("Simulation days:", daysSpinner));
        content.add(formRow("Seed infections:", seedInfectionsSpinner));
        content.add(formRow("Random seed:", randomSeedSpinner));

        content.add(Box.createVerticalStrut(14));
        content.add(sectionLabel("Controls"));
        content.add(buildControlButtons());

        content.add(Box.createVerticalStrut(14));
        content.add(sectionLabel("Interventions"));
        content.add(buildInterventionsPanel());

        // The Scrollable override below is the actual fix for horizontal clipping: it makes the
        // viewport force `content` to the viewport's width instead of `content` dictating its own
        // (much wider) preferred width and getting silently cut off. Everything else here — the
        // NEVER horizontal policy and the explicit preferred width — only matters once that's true.
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        int scrollbarWidth = scrollPane.getVerticalScrollBar().getPreferredSize().width;
        // Preferred width is set on the scroll pane, not on `content` — content tracks the viewport's
        // actual width (see ScrollableContentPanel), so sizing it here would just be overridden.
        scrollPane.setPreferredSize(new Dimension(CONTROL_PANEL_WIDTH + scrollbarWidth, 0));
        add(scrollPane, BorderLayout.CENTER);

        startButton.addActionListener(e -> validateAndSubmit(listener::onStartRequested));
        pauseButton.addActionListener(e -> listener.onPauseRequested());
        stepButton.addActionListener(e -> validateAndSubmit(listener::onStepRequested));
        resetButton.addActionListener(e -> listener.onResetRequested());
        abortButton.addActionListener(e -> listener.onAbortRequested());

        setRunState(RunState.NOT_STARTED);
    }

    /**
     * Updates which buttons and config fields are enabled to match the given state, and toggles the
     * Start button's label between "Start" and "Resume".
     *
     * @param state the new playback state
     */
    public void setRunState(RunState state) {
        boolean configEditable = state == RunState.NOT_STARTED;
        pathogenCombo.setEnabled(configEditable);
        populationSpinner.setEnabled(configEditable);
        daysSpinner.setEnabled(configEditable);
        seedInfectionsSpinner.setEnabled(configEditable);
        randomSeedSpinner.setEnabled(configEditable);
        for (InterventionRow row : interventionRows) {
            row.setEnabledControls(configEditable);
        }

        startButton.setEnabled(state == RunState.NOT_STARTED || state == RunState.PAUSED);
        startButton.setText(state == RunState.PAUSED ? "Resume" : "Start");
        pauseButton.setEnabled(state == RunState.RUNNING);
        stepButton.setEnabled(state == RunState.NOT_STARTED || state == RunState.PAUSED);
        abortButton.setEnabled(state == RunState.RUNNING || state == RunState.PAUSED);
        resetButton.setEnabled(state != RunState.NOT_STARTED);
    }

    /**
     * Commits every spinner's pending text, validates the result, and — only if everything is valid —
     * invokes {@code action} with the built config and interventions. Shows a JOptionPane and aborts
     * without invoking {@code action} on the first validation failure found.
     */
    private void validateAndSubmit(BiConsumer<SimulationConfig, List<Intervention>> action) {
        Optional<SimulationConfig> config = validateAndBuildConfig();
        if (config.isEmpty()) {
            return;
        }
        Optional<List<Intervention>> interventions = validateAndBuildInterventions();
        if (interventions.isEmpty()) {
            return;
        }
        action.accept(config.get(), interventions.get());
    }

    private Optional<SimulationConfig> validateAndBuildConfig() {
        if (!commitEdit(populationSpinner, "Population size") || !commitEdit(daysSpinner, "Simulation days")
                || !commitEdit(seedInfectionsSpinner, "Seed infections") || !commitEdit(randomSeedSpinner, "Random seed")) {
            return Optional.empty();
        }

        int populationSize = (Integer) populationSpinner.getValue();
        int seedInfections = (Integer) seedInfectionsSpinner.getValue();
        if (seedInfections > populationSize) {
            showValidationError("Seed infections (" + seedInfections + ") cannot exceed the population size ("
                    + populationSize + ").");
            return Optional.empty();
        }

        Pathogen pathogen = (Pathogen) pathogenCombo.getSelectedItem();
        String runName = pathogen.getName() + " — " + LocalDateTime.now().format(RUN_NAME_TIMESTAMP);
        return Optional.of(new SimulationConfig()
                .setRunName(runName)
                .setPathogen(pathogen)
                .setPopulationSize(populationSize)
                .setTotalDays((Integer) daysSpinner.getValue())
                .setSeedInfections(seedInfections)
                .setHealthcareWorkerRatio(HEALTHCARE_WORKER_RATIO)
                .setElderlyRatio(ELDERLY_RATIO)
                .setRandomSeed(((Integer) randomSeedSpinner.getValue()).longValue()));
    }

    private Optional<List<Intervention>> validateAndBuildInterventions() {
        List<Intervention> result = new ArrayList<>();
        for (InterventionRow row : interventionRows) {
            if (!row.isEnabled()) {
                continue;
            }
            if (!row.commitPendingEdits()) {
                showValidationError(row.getTitle() + ": every field must be a whole number.");
                return Optional.empty();
            }
            if (row.getEndDay() < row.getStartDay()) {
                showValidationError(row.getTitle() + ": end day (" + row.getEndDay()
                        + ") cannot be before start day (" + row.getStartDay() + ").");
                return Optional.empty();
            }
            result.add(row.build());
        }
        return Optional.of(result);
    }

    /** Forces a spinner's pending (possibly unparsed) text into its model now, rather than leaving stale data in place. */
    private boolean commitEdit(JSpinner spinner, String fieldLabel) {
        try {
            spinner.commitEdit();
            return true;
        } catch (ParseException e) {
            showValidationError(fieldLabel + " must be a whole number.");
            return false;
        }
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, message, "Invalid Input", JOptionPane.WARNING_MESSAGE);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static JComponent sectionLabel(String text) {
        JLabel label = new JLabel(text.toUpperCase(Locale.ROOT));
        label.setFont(Theme.BODY_BOLD_FONT);
        label.setForeground(Theme.PRIMARY_DARK);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        return label;
    }

    private static JPanel formRow(String labelText, JComponent field) {
        return formRow(labelText, field, Theme.BACKGROUND);
    }

    /** A fixed-width label column ("the label column a fixed width") + a field that gets the remainder. */
    private static JPanel formRow(String labelText, JComponent field, Color background) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(background);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel label = new JLabel(labelText);
        label.setFont(Theme.BODY_FONT);
        label.setPreferredSize(new Dimension(LABEL_COLUMN_WIDTH, 20));
        row.add(label, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JComponent buildControlButtons() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.setBackground(Theme.BACKGROUND);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (JButton button : List.of(startButton, pauseButton, stepButton, resetButton, abortButton)) {
            panel.add(button);
        }
        // GridLayout already gives every button an equal, full-width cell; this just guarantees
        // BoxLayout (the parent's layout) is willing to stretch the whole grid to the container's width
        // instead of shrinking it to its natural preferred width.
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    private JComponent buildInterventionsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BACKGROUND);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        int defaultEnd = (Integer) daysSpinner.getValue();

        interventionRows.add(new InterventionRow("LOCKDOWN", "Lockdown", LOCKDOWN_COST_PER_DAY, defaultEnd, null, 0, 0));
        interventionRows.add(new InterventionRow("MASK_MANDATE", "Mask Mandate", MASK_MANDATE_COST_PER_DAY, defaultEnd, null, 0, 0));
        interventionRows.add(new InterventionRow("VACCINATION_DRIVE", "Vaccination Drive", VACCINATION_DRIVE_COST_PER_DAY,
                defaultEnd, "Doses/day:", 500, 20000));
        interventionRows.add(new InterventionRow("CONTACT_TRACING", "Contact Tracing", CONTACT_TRACING_COST_PER_DAY,
                defaultEnd, "Capacity/day:", 200, 20000));

        for (InterventionRow row : interventionRows) {
            panel.add(row.getPanel());
            panel.add(Box.createVerticalStrut(6));
        }
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    /**
     * The scroll pane's content panel. getScrollableTracksViewportWidth() = true is the actual fix for
     * the horizontal clipping: without it, the viewport gives this panel its own (much wider) preferred
     * width — driven by whichever child row is naturally widest — and everything beyond the visible
     * 340px is silently cut off rather than wrapped or scrolled to.
     */
    private static class ScrollableContentPanel extends JPanel implements Scrollable {

        ScrollableContentPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** One configurable row for a single intervention type: enable checkbox, intensity slider, day range, extras. */
    private static class InterventionRow {

        private final String type;
        private final String title;
        private final double costPerDay;
        private final JCheckBox enabledCheck = new JCheckBox("Enable");
        private final JSlider intensitySlider = new JSlider(0, 100, 50);
        private final JSpinner startDaySpinner;
        private final JSpinner endDaySpinner;
        private final JSpinner extraSpinner;
        private final JPanel panel;

        InterventionRow(String type, String title, double costPerDay, int defaultEndDay,
                         String extraLabel, int extraDefault, int extraMax) {
            this.type = type;
            this.title = title;
            this.costPerDay = costPerDay;
            this.startDaySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 3650, 1));
            this.endDaySpinner = new JSpinner(new SpinnerNumberModel(Math.max(1, defaultEndDay), 1, 3650, 1));

            startDaySpinner.setPreferredSize(FIELD_SIZE);
            endDaySpinner.setPreferredSize(FIELD_SIZE);
            intensitySlider.setValue(50); // enabling an intervention with no intensity set would have no effect

            panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBackground(Theme.SURFACE);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)));
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(Theme.BODY_BOLD_FONT);
            JPanel headerRow = new JPanel(new BorderLayout(6, 0));
            headerRow.setBackground(Theme.SURFACE);
            headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            headerRow.add(enabledCheck, BorderLayout.WEST);
            headerRow.add(titleLabel, BorderLayout.CENTER);

            JPanel intensityRow = new JPanel(new BorderLayout(6, 0));
            intensityRow.setBackground(Theme.SURFACE);
            intensityRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            intensityRow.add(new JLabel("Intensity:"), BorderLayout.WEST);
            intensitySlider.setMajorTickSpacing(50);
            intensitySlider.setPaintTicks(true);
            intensitySlider.setPaintLabels(true);
            intensitySlider.setBackground(Theme.SURFACE);
            intensityRow.add(intensitySlider, BorderLayout.CENTER);
            intensityRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, intensityRow.getPreferredSize().height));

            // Start day and end day each get their own row — side by side they didn't fit in 340px and
            // "End day" was being cut off entirely.
            JPanel startDayRow = formRow("Start day:", startDaySpinner, Theme.SURFACE);
            JPanel endDayRow = formRow("End day:", endDaySpinner, Theme.SURFACE);

            panel.add(headerRow);
            panel.add(intensityRow);
            panel.add(startDayRow);
            panel.add(endDayRow);

            if (extraLabel != null) {
                extraSpinner = new JSpinner(new SpinnerNumberModel(extraDefault, 0, extraMax, 10));
                extraSpinner.setPreferredSize(FIELD_SIZE);
                panel.add(formRow(extraLabel, extraSpinner, Theme.SURFACE));
            } else {
                extraSpinner = null;
            }

            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        }

        JPanel getPanel() {
            return panel;
        }

        void setEnabledControls(boolean enabled) {
            enabledCheck.setEnabled(enabled);
            intensitySlider.setEnabled(enabled);
            startDaySpinner.setEnabled(enabled);
            endDaySpinner.setEnabled(enabled);
            if (extraSpinner != null) {
                extraSpinner.setEnabled(enabled);
            }
        }

        boolean isEnabled() {
            return enabledCheck.isSelected();
        }

        String getTitle() {
            return title;
        }

        int getStartDay() {
            return (Integer) startDaySpinner.getValue();
        }

        int getEndDay() {
            return (Integer) endDaySpinner.getValue();
        }

        /**
         * Forces any pending (possibly unparsed) text in this row's spinners into their models now.
         *
         * @return {@code false} if any field's text does not parse as a whole number
         */
        boolean commitPendingEdits() {
            try {
                startDaySpinner.commitEdit();
                endDaySpinner.commitEdit();
                if (extraSpinner != null) {
                    extraSpinner.commitEdit();
                }
                return true;
            } catch (ParseException e) {
                return false;
            }
        }

        /**
         * Constructs the correct concrete Intervention subclass, or null if this row is not enabled.
         * Assumes {@link #commitPendingEdits()} has already been called and end day &gt;= start day has
         * already been validated by the caller — this method does not re-check either.
         */
        Intervention build() {
            if (!enabledCheck.isSelected()) {
                return null;
            }
            int start = getStartDay();
            int end = getEndDay();
            double intensity = intensitySlider.getValue() / 100.0;

            return switch (type) {
                case "LOCKDOWN" -> new Lockdown(0, 0, "Lockdown", start, end, intensity, costPerDay, true);
                case "MASK_MANDATE" -> new MaskMandate(0, 0, "Mask Mandate", start, end, intensity, costPerDay, true);
                case "VACCINATION_DRIVE" -> new VaccinationDrive(0, 0, "Vaccination Drive", start, end, intensity,
                        costPerDay, true, (Integer) extraSpinner.getValue());
                case "CONTACT_TRACING" -> new ContactTracing(0, 0, "Contact Tracing", start, end, intensity,
                        costPerDay, true, (Integer) extraSpinner.getValue());
                default -> throw new IllegalStateException("Unknown intervention type: " + type);
            };
        }
    }
}
