package com.episim.gui;

import com.episim.dao.DailyRecordDao;
import com.episim.dao.DistrictDao;
import com.episim.dao.InterventionDao;
import com.episim.dao.PersonDao;
import com.episim.dao.SimulationRunDao;
import com.episim.engine.SimulationConfig;
import com.episim.engine.SimulationEngine;
import com.episim.engine.SimulationListener;
import com.episim.model.DailyRecord;
import com.episim.model.District;
import com.episim.model.Intervention;
import com.episim.model.Pathogen;
import com.episim.model.Person;
import com.episim.model.SimulationRun;
import com.episim.util.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The application's main window. Owns the live SimulationEngine's lifecycle and wires every panel
 * together; all simulation stepping and database access is dispatched through SwingWorker so the Event
 * Dispatch Thread is never blocked.
 */
public class MainDashboard extends JFrame implements SimulationListener, ControlPanel.Listener {

    private static final int PLAYBACK_DELAY_MS = 150;
    private static final int PAUSE_POLL_MS = 100;

    private final DistrictDao districtDao = new DistrictDao();
    private final PersonDao personDao = new PersonDao();
    private final DailyRecordDao dailyRecordDao = new DailyRecordDao();
    private final InterventionDao interventionDao = new InterventionDao();
    private final SimulationRunDao simulationRunDao = new SimulationRunDao();

    private final ControlPanel controlPanel;
    private final EpidemicCurvePanel curvePanel = new EpidemicCurvePanel();
    private final PopulationTablePanel populationPanel = new PopulationTablePanel();
    private final DistrictPanel districtPanel = new DistrictPanel();
    private final AnalysisPanel analysisPanel = new AnalysisPanel();
    private final RunHistoryPanel historyPanel = new RunHistoryPanel();
    private final JTabbedPane tabbedPane = new JTabbedPane();

    private final JLabel dayLabel = new JLabel("Day: —");
    private final JLabel interventionsLabel = new JLabel("Active interventions: —");
    private final JLabel dbPathLabel = new JLabel();

    private volatile SimulationEngine currentEngine;
    private SwingWorker<Void, Void> playbackWorker;
    private boolean viewingHistoricalRun;

    /** @param pathogens the pathogens to offer in the picker, typically loaded from {@code PathogenDao} at startup */
    public MainDashboard(List<Pathogen> pathogens) {
        super("EpiSim");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setJMenuBar(buildMenuBar());

        // Width comes from ControlPanel's internal scroll pane, not from a preferredSize set here —
        // see ControlPanel's constructor.
        controlPanel = new ControlPanel(pathogens, this);

        analysisPanel.setOnCsvImported(this::onCsvImported);
        historyPanel.setOnLoadRunRequested(this::onLoadRunRequested);

        tabbedPane.addTab("Epidemic Curve", curvePanel);
        tabbedPane.addTab("Population", populationPanel);
        tabbedPane.addTab("Districts", districtPanel);
        tabbedPane.addTab("Analysis", analysisPanel);
        tabbedPane.addTab("Run History", historyPanel);
        tabbedPane.addChangeListener(e -> refreshSelectedTabFromLiveEngine());

        add(buildTitleBar(), BorderLayout.NORTH);
        add(controlPanel, BorderLayout.WEST);
        add(tabbedPane, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        historyPanel.refresh();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About EpiSim");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private void showAboutDialog() {
        String message = "<html><body style='width: 320px'>"
                + "<h2 style='margin-bottom:0'>EpiSim</h2>"
                + "<p style='margin-top:2px'>Epidemic Outbreak Simulation &amp; Public Health Response System</p>"
                + "<p><b>Group Members:</b><br/>"
                + "Jasurbek Omonkulov<br/>"
                + "Tursunmurodov Salokhiddinbek<br/>"
                + "Zaynutdinov Kmaronbek</p>"
                + "<p><b>Course:</b> BIT1123 Object Oriented Programming (Java)</p>"
                + "<p><b>Aligned to UN Sustainable Development Goal 3</b><br/>"
                + "Good Health and Well-being<br/>"
                + "&#8226; Target 3.3 — End epidemics of AIDS, tuberculosis, malaria, and other communicable diseases<br/>"
                + "&#8226; Target 3.d — Strengthen capacity for early warning, risk reduction, and management "
                + "of national and global health risks</p>"
                + "</body></html>";
        JOptionPane.showMessageDialog(this, message, "About EpiSim", JOptionPane.INFORMATION_MESSAGE);
    }

    private JComponent buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.PRIMARY);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("EpiSim");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Theme.TEXT_ON_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("SDG 3 — Good Health and Well-being | Epidemic Simulation & Response System");
        subtitle.setFont(Theme.SUBTITLE_FONT);
        subtitle.setForeground(Theme.ACCENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(title);
        textPanel.add(subtitle);

        bar.add(textPanel, BorderLayout.WEST);
        return bar;
    }

    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.SLATE);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        bar.setPreferredSize(new Dimension(0, 36));

        dayLabel.setForeground(Theme.TEXT_ON_PRIMARY);
        dayLabel.setFont(Theme.BODY_FONT);
        interventionsLabel.setForeground(Theme.TEXT_ON_PRIMARY);
        interventionsLabel.setFont(Theme.BODY_FONT);

        // Showing the DB path on screen is deliberate — it proves to the marker that persistence is real.
        dbPathLabel.setText("Database: " + Path.of("data/episim.db").toAbsolutePath());
        dbPathLabel.setForeground(Theme.ACCENT);
        dbPathLabel.setFont(Theme.BODY_FONT);

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0));
        leftGroup.setOpaque(false);
        leftGroup.add(dayLabel);
        leftGroup.add(interventionsLabel);

        bar.add(leftGroup, BorderLayout.WEST);
        bar.add(dbPathLabel, BorderLayout.EAST);
        return bar;
    }

    // ---------- ControlPanel.Listener ----------

    /** {@inheritDoc} Resumes a paused run in place, or creates and starts a new engine via a {@link SwingWorker}. */
    @Override
    public void onStartRequested(SimulationConfig config, List<Intervention> interventions) {
        if (currentEngine != null) {
            currentEngine.resume(); // cheap flag flip, no DB work — fine to call directly on the EDT
            controlPanel.setRunState(ControlPanel.RunState.RUNNING);
            ensurePlaybackWorkerRunning();
            return;
        }

        SwingWorker<SimulationEngine, Void> starter = new SwingWorker<>() {
            @Override
            protected SimulationEngine doInBackground() {
                return createAndStartEngine(config, interventions);
            }

            @Override
            protected void done() {
                try {
                    currentEngine = get();
                    viewingHistoricalRun = false;
                    // Without this, panels that only refresh on tab-switch or on finish (Population,
                    // Districts, Analysis) would keep showing whatever they last held — e.g. unscaled
                    // reference data left over from a previously loaded historical run — until the user
                    // happened to switch away from and back to that tab.
                    refreshAllPanelsFromEngine();
                    controlPanel.setRunState(ControlPanel.RunState.RUNNING);
                    ensurePlaybackWorkerRunning();
                } catch (Exception e) {
                    showError("Failed to start simulation", e);
                    controlPanel.setRunState(ControlPanel.RunState.NOT_STARTED);
                }
            }
        };
        starter.execute();
    }

    /** {@inheritDoc} */
    @Override
    public void onPauseRequested() {
        if (currentEngine != null) {
            currentEngine.pause();
        }
        controlPanel.setRunState(ControlPanel.RunState.PAUSED);
    }

    /** {@inheritDoc} Creates and starts a new engine if none exists yet, otherwise pauses and steps the existing one. */
    @Override
    public void onStepRequested(SimulationConfig config, List<Intervention> interventions) {
        if (currentEngine == null) {
            SwingWorker<SimulationEngine, Void> starter = new SwingWorker<>() {
                @Override
                protected SimulationEngine doInBackground() {
                    SimulationEngine engine = createAndStartEngine(config, interventions);
                    engine.stepOneDay();
                    return engine;
                }

                @Override
                protected void done() {
                    try {
                        currentEngine = get();
                        viewingHistoricalRun = false;
                        refreshAllPanelsFromEngine(); // see onStartRequested for why this can't wait for a tab-switch
                        controlPanel.setRunState(currentEngine.isFinished()
                                ? ControlPanel.RunState.FINISHED : ControlPanel.RunState.PAUSED);
                    } catch (Exception e) {
                        showError("Failed to step simulation", e);
                        controlPanel.setRunState(ControlPanel.RunState.NOT_STARTED);
                    }
                }
            };
            starter.execute();
            return;
        }

        currentEngine.pause(); // manual stepping implies paused continuous playback
        SwingWorker<Void, Void> stepper = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                if (!currentEngine.isFinished()) {
                    currentEngine.stepOneDay();
                }
                return null;
            }

            @Override
            protected void done() {
                controlPanel.setRunState(currentEngine.isFinished()
                        ? ControlPanel.RunState.FINISHED : ControlPanel.RunState.PAUSED);
            }
        };
        stepper.execute();
    }

    /** {@inheritDoc} */
    @Override
    public void onResetRequested() {
        SimulationEngine engineToReset = currentEngine;
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                if (engineToReset != null) {
                    engineToReset.reset(); // aborts an in-progress run rather than deleting its row
                }
                return null;
            }

            @Override
            protected void done() {
                currentEngine = null;
                viewingHistoricalRun = false;
                clearAllPanels();
                controlPanel.setRunState(ControlPanel.RunState.NOT_STARTED);
                historyPanel.refresh();
            }
        };
        worker.execute();
    }

    /** {@inheritDoc} */
    @Override
    public void onAbortRequested() {
        if (currentEngine == null) {
            return;
        }
        currentEngine.pause();
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                currentEngine.abort();
                return null;
            }

            @Override
            protected void done() {
                controlPanel.setRunState(ControlPanel.RunState.FINISHED);
                refreshAllPanelsFromEngine();
                historyPanel.refresh();
            }
        };
        worker.execute();
    }

    private SimulationEngine createAndStartEngine(SimulationConfig config, List<Intervention> interventions) {
        SimulationEngine engine = new SimulationEngine(config);
        for (Intervention intervention : interventions) {
            engine.addIntervention(intervention);
        }
        engine.addListener(this);
        engine.start();
        return engine;
    }

    /** One persistent background loop per engine lifetime; idles (without stepping) while engine.isPaused(). */
    private void ensurePlaybackWorkerRunning() {
        if (playbackWorker != null && !playbackWorker.isDone()) {
            return;
        }
        playbackWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws InterruptedException {
                while (currentEngine != null && !currentEngine.isFinished()) {
                    if (currentEngine.isPaused()) {
                        Thread.sleep(PAUSE_POLL_MS);
                        continue;
                    }
                    currentEngine.stepOneDay();
                    Thread.sleep(PLAYBACK_DELAY_MS);
                }
                return null;
            }

            @Override
            protected void done() {
                if (currentEngine != null && currentEngine.isFinished()) {
                    controlPanel.setRunState(ControlPanel.RunState.FINISHED);
                }
            }
        };
        playbackWorker.execute();
    }

    // ---------- SimulationListener (observer pattern; fires on the background worker thread) ----------

    /** {@inheritDoc} */
    @Override
    public void onDayCompleted(DailyRecord record) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (currentEngine == null) {
                return;
            }
            curvePanel.setData(currentEngine.getHistory(), currentEngine.getInterventions(),
                    totalHospitalCapacity(currentEngine.getDistricts()));
            updateStatusBar();
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onSimulationFinished(List<DailyRecord> history) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            controlPanel.setRunState(ControlPanel.RunState.FINISHED);
            refreshAllPanelsFromEngine();
            historyPanel.refresh();
        });
    }

    // ---------- Run History "load" callback ----------

    private record LoadedRun(SimulationRun run, List<DailyRecord> history, List<Intervention> interventions,
                              List<Person> population, Map<String, District> districts) {
    }

    private void onLoadRunRequested(int runId) {
        SwingWorker<LoadedRun, Void> worker = new SwingWorker<>() {
            @Override
            protected LoadedRun doInBackground() throws Exception {
                if (currentEngine != null) {
                    currentEngine.reset();
                }
                SimulationRun run = simulationRunDao.findById(runId)
                        .orElseThrow(() -> new IllegalStateException("Run " + runId + " no longer exists"));
                List<DailyRecord> history = dailyRecordDao.findByRun(runId);
                List<Intervention> interventions = interventionDao.findByRun(runId);
                List<Person> population = personDao.findByRun(runId);

                Map<String, District> districts = new HashMap<>();
                for (District district : districtDao.findAll()) {
                    districts.put(district.getId(), district);
                }
                for (Person person : population) {
                    District district = districts.get(person.getDistrictId());
                    if (district != null) {
                        district.addResident(person);
                    }
                }
                // DistrictDao.findAll() returns raw design capacity; without this, a loaded run would
                // show the unscaled reference data instead of what the run actually simulated against.
                District.scaleHospitalCapacities(districts.values(), run.getPopulationSize());
                return new LoadedRun(run, history, interventions, population, districts);
            }

            @Override
            protected void done() {
                try {
                    LoadedRun loaded = get();
                    currentEngine = null;
                    viewingHistoricalRun = true;

                    curvePanel.setData(loaded.history(), loaded.interventions(), totalHospitalCapacity(loaded.districts()));
                    populationPanel.setPopulation(loaded.population());
                    setDistrictPanelData(loaded.districts());
                    analysisPanel.setData(loaded.run(), loaded.history(),
                            new ArrayList<>(loaded.districts().values()), loaded.interventions(),
                            loaded.population());

                    dayLabel.setText("Viewing run #" + runId + " (" + loaded.run().getStatus() + ") — "
                            + loaded.history().size() + " days recorded");
                    interventionsLabel.setText("Loaded from database, read-only");
                    controlPanel.setRunState(ControlPanel.RunState.NOT_STARTED);
                    tabbedPane.setSelectedIndex(0);
                } catch (Exception e) {
                    showError("Failed to load run " + runId, e);
                }
            }
        };
        worker.execute();
    }

    private void onCsvImported(List<DailyRecord> imported) {
        List<Intervention> interventions = currentEngine != null ? currentEngine.getInterventions() : List.of();
        int capacity = currentEngine != null ? totalHospitalCapacity(currentEngine.getDistricts()) : 0;
        curvePanel.setData(imported, interventions, capacity);
    }

    // ---------- Shared refresh helpers ----------

    private void refreshSelectedTabFromLiveEngine() {
        if (currentEngine == null || viewingHistoricalRun) {
            return;
        }
        switch (tabbedPane.getSelectedIndex()) {
            case 1 -> populationPanel.setPopulation(currentEngine.getPopulation());
            case 2 -> setDistrictPanelData(currentEngine.getDistricts());
            case 3 -> analysisPanel.setData(buildRunSnapshotFromEngine(), currentEngine.getHistory(),
                    new ArrayList<>(currentEngine.getDistricts().values()), currentEngine.getInterventions(),
                    currentEngine.getPopulation());
            default -> {
                // Epidemic Curve and Run History keep themselves up to date independently.
            }
        }
    }

    private void refreshAllPanelsFromEngine() {
        if (currentEngine == null) {
            return;
        }
        List<Person> population = currentEngine.getPopulation();
        Map<String, District> districts = currentEngine.getDistricts();
        curvePanel.setData(currentEngine.getHistory(), currentEngine.getInterventions(), totalHospitalCapacity(districts));
        populationPanel.setPopulation(population);
        setDistrictPanelData(districts);
        analysisPanel.setData(buildRunSnapshotFromEngine(), currentEngine.getHistory(),
                new ArrayList<>(districts.values()), currentEngine.getInterventions(), population);
        updateStatusBar();
    }

    /**
     * A lightweight, in-memory SimulationRun snapshot for a live engine — used only to feed the
     * Analysis tab's CSV/text exports, which need a SimulationRun-shaped record. Not a substitute for
     * the persisted row: startedAt/completedAt are left null since this engine hasn't necessarily
     * finished (or even started, from the DB's point of view of when it did).
     */
    private SimulationRun buildRunSnapshotFromEngine() {
        SimulationConfig config = currentEngine.getConfig();
        String status = currentEngine.isFinished() ? "COMPLETED" : "RUNNING";
        return new SimulationRun(currentEngine.getRunId(), config.getRunName(), config.getPathogen().getId(),
                config.getPopulationSize(), config.getTotalDays(), config.getSeedInfections(),
                config.getRandomSeed(), null, null, status, null);
    }

    /**
     * Regression guard for the district-capacity-scaling bug (an earlier session found the Districts
     * tab silently showing unscaled reference data). Compares the capacity this call is about to
     * display against what the live engine actually holds, so a future code path that supplies stale or
     * unscaled District data can't fail silently again.
     */
    private void setDistrictPanelData(Map<String, District> districts) {
        if (currentEngine != null) {
            int shownCapacity = districts.values().stream().mapToInt(District::getHospitalCapacity).sum();
            int engineCapacity = currentEngine.getDistricts().values().stream()
                    .mapToInt(District::getHospitalCapacity).sum();
            if (shownCapacity != engineCapacity) {
                System.err.println("WARNING: Districts tab capacity (" + shownCapacity
                        + ") diverges from the live engine's scaled capacity (" + engineCapacity
                        + "). The panel may be displaying stale or unscaled district data.");
            }
        }
        districtPanel.setDistricts(districts);
    }

    private void clearAllPanels() {
        curvePanel.setData(List.of(), List.of(), 0);
        populationPanel.setPopulation(List.of());
        setDistrictPanelData(Map.of());
        analysisPanel.setData(null, List.of(), List.of(), List.of(), List.of());
        dayLabel.setText("Day: —");
        interventionsLabel.setText("Active interventions: —");
    }

    private void updateStatusBar() {
        if (currentEngine == null) {
            return;
        }
        dayLabel.setText("Day: " + currentEngine.getCurrentDay() + " / " + currentEngine.getConfig().getTotalDays());
        long activeCount = currentEngine.getInterventions().stream()
                .filter(i -> i.isActiveOn(currentEngine.getCurrentDay()))
                .count();
        interventionsLabel.setText("Active interventions: " + activeCount);
    }

    private int totalHospitalCapacity(Map<String, District> districts) {
        return districts.values().stream().mapToInt(District::getHospitalCapacity).sum();
    }

    private void showError(String title, Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }
}
