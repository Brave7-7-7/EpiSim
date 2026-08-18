package com.episim.engine;

import com.episim.model.ContactTracing;
import com.episim.model.DailyRecord;
import com.episim.model.Intervention;
import com.episim.model.Lockdown;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutbreakAnalyserTest {

    private static final double DELTA = 1e-9;

    private List<DailyRecord> history;
    private List<Intervention> interventions;

    @BeforeEach
    void setUp() {
        history = List.of(
                dailyRecord(1, 90, 5, 10, 0, 0, 0, 10, false),
                dailyRecord(2, 75, 5, 20, 0, 0, 0, 15, false),
                dailyRecord(3, 55, 5, 35, 0, 0, 0, 20, true),
                dailyRecord(4, 50, 5, 25, 5, 5, 2, 5, true),
                dailyRecord(5, 48, 3, 10, 20, 11, 8, 2, false));

        interventions = List.of(
                new Lockdown(1, 1, "City Lockdown", 1, 10, 0.8, 100.0, true),
                new ContactTracing(2, 1, "Contact Tracing", 1, 20, 0.5, 50.0, true, 200));
    }

    private DailyRecord dailyRecord(int day, int susceptible, int exposed, int infected, int hospitalised,
                                     int recovered, int deceased, int newInfections, boolean overCapacity) {
        return new DailyRecord(0, 1, day, susceptible, exposed, infected, hospitalised, recovered, deceased,
                newInfections, 0.0, hospitalised, overCapacity);
    }

    @Test
    void peakInfectionsFindsTheHighestInfectedCount() {
        assertEquals(35, OutbreakAnalyser.peakInfections(history));
    }

    @Test
    void peakDayFindsTheDayOfTheHighestInfectedCount() {
        assertEquals(3, OutbreakAnalyser.peakDay(history));
    }

    @Test
    void peakInfectionsAndPeakDayHandleEmptyHistory() {
        assertEquals(0, OutbreakAnalyser.peakInfections(List.of()));
        assertEquals(0, OutbreakAnalyser.peakDay(List.of()));
    }

    @Test
    void peakDayShiftsWhenHospitalisationsAreHighEnoughToOutweighInfectedAlone() {
        // Day 1 has the highest infected-ALONE count (40); an infected-only peak definition would
        // report day 1. Day 2 has far fewer infected (10) but many more hospitalised (50), so under
        // the correct infected+hospitalised prevalence definition, day 2 is the true peak (60 vs 40).
        List<DailyRecord> hospHeavyHistory = List.of(
                dailyRecord(1, 200, 0, 40, 0, 0, 0, 40, false),
                dailyRecord(2, 150, 0, 10, 50, 0, 0, 10, true),
                dailyRecord(3, 140, 0, 5, 5, 40, 0, 5, false));

        assertEquals(60, OutbreakAnalyser.peakInfections(hospHeavyHistory));
        assertEquals(2, OutbreakAnalyser.peakDay(hospHeavyHistory));
        assertNotEquals(1, OutbreakAnalyser.peakDay(hospHeavyHistory),
                "Peak day must not fall back to the infected-only peak (day 1)");
    }

    @Test
    void peakHospitalOccupancyFindsTheHighestBedsOccupiedCount() {
        assertEquals(20, OutbreakAnalyser.peakHospitalOccupancy(history));
    }

    @Test
    void attackRateSumsNewInfectionsOverPopulation() {
        // newInfections: 10 + 15 + 20 + 5 + 2 = 52
        assertEquals(52.0 / 100, OutbreakAnalyser.attackRate(history, 100), DELTA);
    }

    @Test
    void attackRateGuardsAgainstZeroPopulation() {
        assertEquals(0.0, OutbreakAnalyser.attackRate(history, 0), DELTA);
    }

    @Test
    void caseFatalityRateDividesFinalDeceasedByTotalInfections() {
        // final deceased = 8, total infections = 52
        assertEquals(8.0 / 52, OutbreakAnalyser.caseFatalityRate(history), DELTA);
    }

    @Test
    void caseFatalityRateGuardsAgainstEmptyHistoryAndZeroInfections() {
        assertEquals(0.0, OutbreakAnalyser.caseFatalityRate(List.of()), DELTA);

        List<DailyRecord> noInfections = List.of(dailyRecord(1, 100, 0, 0, 0, 0, 0, 0, false));
        assertEquals(0.0, OutbreakAnalyser.caseFatalityRate(noInfections), DELTA);
    }

    @Test
    void daysHospitalOverCapacityCountsFlaggedDays() {
        assertEquals(2, OutbreakAnalyser.daysHospitalOverCapacity(history));
    }

    @Test
    void totalInterventionCostSumsEachInterventionsCost() {
        // Lockdown: 100 * 10 days = 1000; ContactTracing: 50 * 20 days = 1000
        assertEquals(2000.0, OutbreakAnalyser.totalInterventionCost(interventions), DELTA);
    }

    @Test
    void narrativeSummaryMentionsThePeakDayAndInfectionCount() {
        String summary = OutbreakAnalyser.generateNarrativeSummary(history, interventions, 100);

        assertTrue(summary.contains("day 3"));
        assertTrue(summary.contains("35"));
        assertFalse(summary.isBlank());
    }

    @Test
    void narrativeSummaryUsesRootLocaleRegardlessOfTheDefaultLocale() {
        Locale originalDefault = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY); // uses ',' as the decimal separator
            String summary = OutbreakAnalyser.generateNarrativeSummary(history, interventions, 100);

            assertFalse(summary.matches("(?s).*\\d,\\d.*"),
                    "Narrative summary must not contain locale-formatted comma decimals: " + summary);
        } finally {
            Locale.setDefault(originalDefault);
        }
    }

    @Test
    void emptyHistoryProducesAFallbackSummaryInsteadOfThrowing() {
        String summary = OutbreakAnalyser.generateNarrativeSummary(List.of(), List.of(), 100);
        assertFalse(summary.isBlank());
    }
}
