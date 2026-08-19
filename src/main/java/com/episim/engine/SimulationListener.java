package com.episim.engine;

import com.episim.model.DailyRecord;

import java.util.List;

/**
 * Observer pattern: the GUI registers as a listener on {@link SimulationEngine}
 * to react to progress without the engine knowing anything about Swing.
 */
public interface SimulationListener {

    /**
     * Fired synchronously at the end of every {@link SimulationEngine#stepOneDay()} call — on whichever
     * thread called it, which may not be the Event Dispatch Thread, so GUI listeners must marshal any
     * Swing updates via {@code SwingUtilities.invokeLater()}.
     *
     * @param record the day that just completed
     */
    void onDayCompleted(DailyRecord record);

    /**
     * Fired once, after the engine's end-of-run persistence transaction has committed.
     *
     * @param history the run's complete, immutable day-by-day history
     */
    void onSimulationFinished(List<DailyRecord> history);
}
