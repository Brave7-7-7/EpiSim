package com.episim.engine;

import com.episim.model.DailyRecord;

import java.util.List;

/**
 * Observer pattern: the GUI registers as a listener on {@link SimulationEngine}
 * to react to progress without the engine knowing anything about Swing.
 */
public interface SimulationListener {

    void onDayCompleted(DailyRecord record);

    void onSimulationFinished(List<DailyRecord> history);
}
