package com.episim.engine;

import com.episim.model.Citizen;
import com.episim.model.District;
import com.episim.model.ElderlyResident;
import com.episim.model.HealthState;
import com.episim.model.HealthcareWorker;
import com.episim.model.Person;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Builds a run's starting population: a polymorphic mix of {@link Citizen},
 * {@link HealthcareWorker} and {@link ElderlyResident} objects, distributed
 * across the supplied districts in proportion to their population column,
 * and seeded with a reproducible {@link Random} so a run's results can be
 * claimed as repeatable.
 */
public class PopulationGenerator {

    private static final String[] MALE_FIRST_NAMES = {
            "Ahmad", "Muhammad", "Hafiz", "Farid", "Azman", "Wei Jian", "Kai Xuan", "Zhi Hao",
            "Kumar", "Ravi", "Suresh", "Arjun"
    };

    private static final String[] FEMALE_FIRST_NAMES = {
            "Siti", "Nur", "Aina", "Farah", "Mei Ling", "Xin Yi", "Shu Wen", "Priya",
            "Kavitha", "Deepa", "Aishah", "Nadia"
    };

    private static final String[] SURNAMES = {
            "bin Abdullah", "bin Hassan", "binti Ismail", "binti Rahman",
            "Tan", "Lim", "Lee", "Wong", "Chong",
            "a/l Muthu", "a/l Raman", "a/p Suppiah", "a/p Kandiah"
    };

    private static final String[] HOSPITAL_NAMES = {
            "Hospital Kuala Lumpur", "Hospital Sungai Buloh", "Hospital Selayang", "Hospital Shah Alam"
    };

    private static final String[] CARE_HOME_NAMES = {
            "Rumah Seri Kenangan", "Golden Years Care Home", "Taman Sejahtera Elder Care", "Bayu Senja Care Centre"
    };

    private static final double VACCINATION_PROBABILITY = 0.4;

    private final List<District> districts;
    private final Random random;

    public PopulationGenerator(List<District> districts, long randomSeed) {
        this.districts = districts;
        this.random = new Random(randomSeed);
    }

    public List<Person> generate(SimulationConfig config) {
        List<Person> people = new ArrayList<>(config.getPopulationSize());
        int totalDistrictPopulation = districts.stream().mapToInt(District::getPopulation).sum();

        for (int i = 0; i < config.getPopulationSize(); i++) {
            String districtId = pickWeightedDistrict(totalDistrictPopulation);
            double roleRoll = random.nextDouble();

            Person person;
            if (roleRoll < config.getElderlyRatio()) {
                person = createElderlyResident(i, districtId, config);
            } else if (roleRoll < config.getElderlyRatio() + config.getHealthcareWorkerRatio()) {
                person = createHealthcareWorker(i, districtId, config);
            } else {
                person = createCitizen(i, districtId, config);
            }
            people.add(person);
        }

        seedInitialInfections(people, config.getSeedInfections());
        return people;
    }

    private String pickWeightedDistrict(int totalPopulation) {
        if (districts.isEmpty()) {
            throw new IllegalStateException("Cannot generate a population with no districts available");
        }
        if (totalPopulation <= 0) {
            return districts.get(random.nextInt(districts.size())).getId();
        }
        int roll = random.nextInt(totalPopulation);
        int cumulative = 0;
        for (District district : districts) {
            cumulative += district.getPopulation();
            if (roll < cumulative) {
                return district.getId();
            }
        }
        return districts.get(districts.size() - 1).getId();
    }

    private Citizen createCitizen(int index, String districtId, SimulationConfig config) {
        int age = 1 + random.nextInt(90);
        boolean vaccinated = rollVaccinated();
        return new Citizen(0, randomName(), age, districtId, HealthState.SUSCEPTIBLE, 0,
                vaccinated, rollImmunityLevel(vaccinated, config));
    }

    private HealthcareWorker createHealthcareWorker(int index, String districtId, SimulationConfig config) {
        int age = 22 + random.nextInt(40);
        boolean vaccinated = rollVaccinated();
        boolean hasPPE = random.nextDouble() < 0.8;
        String hospitalAssigned = HOSPITAL_NAMES[random.nextInt(HOSPITAL_NAMES.length)];
        return new HealthcareWorker(0, randomName(), age, districtId, HealthState.SUSCEPTIBLE, 0,
                vaccinated, rollImmunityLevel(vaccinated, config), hasPPE, hospitalAssigned);
    }

    private ElderlyResident createElderlyResident(int index, String districtId, SimulationConfig config) {
        int age = 60 + random.nextInt(35);
        boolean vaccinated = rollVaccinated();
        String careHomeName = CARE_HOME_NAMES[random.nextInt(CARE_HOME_NAMES.length)];
        return new ElderlyResident(0, randomName(), age, districtId, HealthState.SUSCEPTIBLE, 0,
                vaccinated, rollImmunityLevel(vaccinated, config), careHomeName);
    }

    private boolean rollVaccinated() {
        return random.nextDouble() < VACCINATION_PROBABILITY;
    }

    /** Immunity level is derived from the same vaccination roll passed in, not a fresh one — keeps the two consistent. */
    private double rollImmunityLevel(boolean vaccinated, SimulationConfig config) {
        if (!vaccinated) {
            return 0.0;
        }
        double effectiveness = config.getPathogen() != null ? config.getPathogen().getVaccineEffectiveness() : 0.0;
        return effectiveness * (0.7 + random.nextDouble() * 0.3);
    }

    private String randomName() {
        String[] firstNamePool = random.nextBoolean() ? MALE_FIRST_NAMES : FEMALE_FIRST_NAMES;
        String firstName = firstNamePool[random.nextInt(firstNamePool.length)];
        String surname = SURNAMES[random.nextInt(SURNAMES.length)];
        return firstName + " " + surname;
    }

    private void seedInitialInfections(List<Person> people, int seedInfections) {
        List<Integer> indices = new ArrayList<>(people.size());
        for (int i = 0; i < people.size(); i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, random);

        int count = Math.min(seedInfections, people.size());
        for (int i = 0; i < count; i++) {
            people.get(indices.get(i)).transitionTo(HealthState.INFECTED);
        }
    }
}
