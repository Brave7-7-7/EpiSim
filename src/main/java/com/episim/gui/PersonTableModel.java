package com.episim.gui;

import com.episim.model.HealthState;
import com.episim.model.Person;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Backs the Population tab's JTable over a (filterable) List<Person>. Every column reads through the
 * Person superclass reference — getRoleLabel() in particular is resolved polymorphically per subclass.
 */
public class PersonTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "ID", "Name", "Age", "Role", "District", "State", "Days in State", "Vaccinated"
    };

    private List<Person> allPeople = new ArrayList<>();
    private List<Person> visiblePeople = new ArrayList<>();

    public void setPeople(List<Person> people) {
        this.allPeople = new ArrayList<>(people);
        applyFilter(null, "");
    }

    /** Recomputes the visible rows: stateFilter (null = all states) AND a case-insensitive name search. */
    public void applyFilter(HealthState stateFilter, String nameSearch) {
        String needle = nameSearch == null ? "" : nameSearch.trim().toLowerCase(Locale.ROOT);
        visiblePeople = new ArrayList<>();
        for (Person person : allPeople) {
            boolean stateMatches = stateFilter == null || person.getHealthState() == stateFilter;
            boolean nameMatches = needle.isEmpty() || person.getFullName().toLowerCase(Locale.ROOT).contains(needle);
            if (stateMatches && nameMatches) {
                visiblePeople.add(person);
            }
        }
        fireTableDataChanged();
    }

    public Person getPersonAt(int row) {
        return visiblePeople.get(row);
    }

    @Override
    public int getRowCount() {
        return visiblePeople.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 0, 2, 6 -> Integer.class;
            case 5 -> HealthState.class;
            case 7 -> Boolean.class;
            default -> String.class;
        };
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Person person = visiblePeople.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> person.getId();
            case 1 -> person.getFullName();
            case 2 -> person.getAge();
            case 3 -> person.getRoleLabel(); // polymorphic: resolved per concrete subclass
            case 4 -> person.getDistrictId();
            case 5 -> person.getHealthState();
            case 6 -> person.getDaysInCurrentState();
            case 7 -> person.isVaccinated();
            default -> null;
        };
    }
}
