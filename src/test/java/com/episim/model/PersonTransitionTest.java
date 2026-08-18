package com.episim.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonTransitionTest {

    @Test
    void transitionToChangesStateAndResetsDaysInCurrentState() {
        Person person = new Citizen(1, "Test Person", 30, "KL-CENTRAL", HealthState.SUSCEPTIBLE, 7, false, 0.0);

        person.transitionTo(HealthState.EXPOSED);

        assertEquals(HealthState.EXPOSED, person.getHealthState());
        assertEquals(0, person.getDaysInCurrentState());
    }

    @Test
    void multipleTransitionsEachResetTheDayCounter() {
        Person person = new ElderlyResident(2, "Test Elder", 70, "RURAL-N", HealthState.EXPOSED, 3, true, 0.5,
                "Test Care Home");

        person.transitionTo(HealthState.INFECTED);
        person.setDaysInCurrentState(person.getDaysInCurrentState() + 4);
        person.transitionTo(HealthState.RECOVERED);

        assertEquals(HealthState.RECOVERED, person.getHealthState());
        assertEquals(0, person.getDaysInCurrentState());
    }

    @Test
    void isInfectiousIsTrueOnlyForInfectedAndHospitalised() {
        assertEquals(true, HealthState.INFECTED.isInfectious());
        assertEquals(true, HealthState.HOSPITALISED.isInfectious());
        assertEquals(false, HealthState.SUSCEPTIBLE.isInfectious());
        assertEquals(false, HealthState.EXPOSED.isInfectious());
        assertEquals(false, HealthState.RECOVERED.isInfectious());
        assertEquals(false, HealthState.DECEASED.isInfectious());
    }
}
