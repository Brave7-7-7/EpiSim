package com.episim.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistrictTest {

    private Citizen hospitalisedCitizen(int id, String districtId) {
        return new Citizen(id, "Patient-" + id, 40, districtId, HealthState.HOSPITALISED, 0, false, 0.0);
    }

    @Test
    void isHospitalOverwhelmedIsTrueWhenEveryBedIsTakenNotOnlyWhenOverbooked() {
        // Reproduces the exact reported scenario: PJ-URBAN scaled to 14 beds at population 2000 (see
        // scaleHospitalCapacitiesScalesEachDistrictProportionallyToSimulatedPopulation), completely full.
        District pjUrban = new District("PJ-URBAN", "Petaling Jaya", 3000, 1.25, 14);
        for (int i = 0; i < 14; i++) {
            pjUrban.addResident(hospitalisedCitizen(i, "PJ-URBAN"));
        }

        assertEquals(14, pjUrban.occupiedBeds());
        assertTrue(pjUrban.isHospitalOverwhelmed(),
                "A district at exactly 100% bed occupancy is overwhelmed — the next patient has nowhere to go");
    }

    @Test
    void isHospitalOverwhelmedIsFalseOneBedBelowCapacity() {
        District pjUrban = new District("PJ-URBAN", "Petaling Jaya", 3000, 1.25, 14);
        for (int i = 0; i < 13; i++) {
            pjUrban.addResident(hospitalisedCitizen(i, "PJ-URBAN"));
        }

        assertEquals(13, pjUrban.occupiedBeds());
        assertFalse(pjUrban.isHospitalOverwhelmed());
    }

    @Test
    void scaleHospitalCapacitiesScalesEachDistrictProportionallyToSimulatedPopulation() {
        // Matches the seeded reference data: design population 10,000, design capacity 220.
        District klCentral = new District("KL-CENTRAL", "Kuala Lumpur City Centre", 4000, 1.65, 90);
        District pjUrban = new District("PJ-URBAN", "Petaling Jaya", 3000, 1.25, 70);
        District shahSub = new District("SHAH-SUB", "Shah Alam Suburbs", 2000, 0.95, 45);
        District ruralN = new District("RURAL-N", "Northern Rural Zone", 1000, 0.55, 15);
        List<District> districts = List.of(klCentral, pjUrban, shahSub, ruralN);

        District.scaleHospitalCapacities(districts, 2000);

        assertEquals(18, klCentral.getHospitalCapacity());
        assertEquals(14, pjUrban.getHospitalCapacity());
        assertEquals(9, shahSub.getHospitalCapacity());
        assertEquals(3, ruralN.getHospitalCapacity());

        // designHospitalCapacity must never change — it's what a later re-scale (e.g. a different run)
        // computes from, and it's the value the persisted DB row still holds.
        assertEquals(90, klCentral.getDesignHospitalCapacity());
        assertEquals(70, pjUrban.getDesignHospitalCapacity());
        assertEquals(45, shahSub.getDesignHospitalCapacity());
        assertEquals(15, ruralN.getDesignHospitalCapacity());
    }

    @Test
    void scaleHospitalCapacitiesNeverScalesBelowOneBed() {
        District tiny = new District("TINY", "Tiny District", 1000, 1.0, 15);

        District.scaleHospitalCapacities(List.of(tiny), 1);

        assertEquals(1, tiny.getHospitalCapacity(), "Every district must retain at least one bed, however small the simulated population");
    }

    @Test
    void scaleHospitalCapacitiesIsIdempotentAcrossRepeatedCalls() {
        // Single-district list, so totalDesignPopulation is this district's own population (4000):
        // scaleFactor = 2000/4000 = 0.5, so 90 * 0.5 = 45.
        District district = new District("KL-CENTRAL", "Kuala Lumpur City Centre", 4000, 1.65, 90);

        District.scaleHospitalCapacities(List.of(district), 2000);
        District.scaleHospitalCapacities(List.of(district), 2000);

        assertEquals(45, district.getHospitalCapacity(),
                "Re-scaling from the same design capacity must always land on the same value, not compound");
    }
}
