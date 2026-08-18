package com.episim.dao;

import com.episim.model.Citizen;
import com.episim.model.ElderlyResident;
import com.episim.model.HealthState;
import com.episim.model.HealthcareWorker;
import com.episim.model.Person;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PersonDao implements Dao<Person> {

    private static final String INSERT_SQL = "INSERT INTO person "
            + "(run_id, full_name, age, person_type, district_id, health_state, days_in_state, vaccinated, "
            + "immunity_level, has_ppe, hospital_assigned, care_home_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    @Override
    public void insert(Person person) throws SQLException {
        // PreparedStatement with bound parameters throughout — never string-concatenated SQL — to prevent SQL injection.
        // run_id is left NULL: a standalone insert() call represents a person who is not yet attached to a
        // simulation run (e.g. a district's master roster). Use insertBatch(..., runId) to attach one.
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, person, null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    person.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert person '" + person.getFullName() + "'", e);
        }
    }

    /** Bulk-inserts a run's population inside a single transaction — far faster than 5000 individual inserts. */
    public void insertBatch(List<Person> people, int runId) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
                for (Person person : people) {
                    bind(ps, person, runId);
                    ps.addBatch();
                }
                ps.executeBatch();

                // The SQLite JDBC driver does not reliably return one generated key per row from
                // getGeneratedKeys() after executeBatch(), so ids are back-computed instead: person_id is
                // INTEGER PRIMARY KEY AUTOINCREMENT, which guarantees strictly sequential, gap-free values
                // within one transaction on one connection, so the batch occupies [lastRowId - n + 1, lastRowId].
                if (!people.isEmpty()) {
                    try (Statement idStatement = conn.createStatement();
                         ResultSet rs = idStatement.executeQuery("SELECT last_insert_rowid()")) {
                        if (rs.next()) {
                            long lastId = rs.getLong(1);
                            long firstId = lastId - people.size() + 1;
                            for (int i = 0; i < people.size(); i++) {
                                people.get(i).setId((int) (firstId + i));
                            }
                        }
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new DataAccessException("Batch insert of persons failed; transaction rolled back", e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to open transaction for person batch insert", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    throw new DataAccessException("Failed to restore autocommit or close the connection", e);
                }
            }
        }
    }

    @Override
    public Optional<Person> findById(int id) throws SQLException {
        String sql = "SELECT * FROM person WHERE person_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find person with id " + id, e);
        }
    }

    @Override
    public List<Person> findAll() throws SQLException {
        String sql = "SELECT * FROM person ORDER BY person_id";
        List<Person> people = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                people.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load persons", e);
        }
        return people;
    }

    public List<Person> findByRun(int runId) throws SQLException {
        String sql = "SELECT * FROM person WHERE run_id = ? ORDER BY person_id";
        List<Person> people = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    people.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load persons for run " + runId, e);
        }
        return people;
    }

    @Override
    public void update(Person person) throws SQLException {
        String sql = "UPDATE person SET full_name = ?, age = ?, person_type = ?, district_id = ?, "
                + "health_state = ?, days_in_state = ?, vaccinated = ?, immunity_level = ?, has_ppe = ?, "
                + "hospital_assigned = ?, care_home_name = ? WHERE person_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, person.getFullName());
            ps.setInt(2, person.getAge());
            ps.setString(3, personType(person));
            ps.setString(4, person.getDistrictId());
            ps.setString(5, person.getHealthState().name());
            ps.setInt(6, person.getDaysInCurrentState());
            ps.setInt(7, person.isVaccinated() ? 1 : 0);
            ps.setDouble(8, person.getImmunityLevel());
            bindSubtypeColumns(ps, person, 9, 10, 11);
            ps.setInt(12, person.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update person with id " + person.getId(), e);
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM person WHERE person_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete person with id " + id, e);
        }
    }

    /**
     * Batch-writes each person's final health_state/days_in_state on a connection the caller owns and
     * will commit/rollback itself — used by SimulationEngine's end-of-run finalisation transaction.
     */
    public void updateHealthStates(List<Person> people, Connection conn) throws SQLException {
        String sql = "UPDATE person SET health_state = ?, days_in_state = ? WHERE person_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Person person : people) {
                ps.setString(1, person.getHealthState().name());
                ps.setInt(2, person.getDaysInCurrentState());
                ps.setInt(3, person.getId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Breakdown of headcount by person_type then health_state, for the run report screen. */
    public Map<String, Map<HealthState, Integer>> countByStateAndType(int runId) throws SQLException {
        String sql = "SELECT person_type, health_state, COUNT(*) AS cnt FROM person "
                + "WHERE run_id = ? GROUP BY person_type, health_state";
        Map<String, Map<HealthState, Integer>> result = new HashMap<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("person_type");
                    HealthState state = HealthState.valueOf(rs.getString("health_state"));
                    int count = rs.getInt("cnt");
                    result.computeIfAbsent(type, k -> new HashMap<>()).put(state, count);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count persons by state and type for run " + runId, e);
        }
        return result;
    }

    private String personType(Person person) {
        if (person instanceof HealthcareWorker) {
            return "HEALTHCARE_WORKER";
        } else if (person instanceof ElderlyResident) {
            return "ELDERLY";
        }
        return "CITIZEN";
    }

    private void bind(PreparedStatement ps, Person person, Integer runId) throws SQLException {
        if (runId != null) {
            ps.setInt(1, runId);
        } else {
            ps.setNull(1, Types.INTEGER);
        }
        ps.setString(2, person.getFullName());
        ps.setInt(3, person.getAge());
        ps.setString(4, personType(person));
        ps.setString(5, person.getDistrictId());
        ps.setString(6, person.getHealthState().name());
        ps.setInt(7, person.getDaysInCurrentState());
        ps.setInt(8, person.isVaccinated() ? 1 : 0);
        ps.setDouble(9, person.getImmunityLevel());
        bindSubtypeColumns(ps, person, 10, 11, 12);
    }

    private void bindSubtypeColumns(PreparedStatement ps, Person person, int ppeCol, int hospitalCol,
                                     int careHomeCol) throws SQLException {
        if (person instanceof HealthcareWorker hw) {
            ps.setInt(ppeCol, hw.isHasPPE() ? 1 : 0);
            ps.setString(hospitalCol, hw.getHospitalAssigned());
            ps.setNull(careHomeCol, Types.VARCHAR);
        } else if (person instanceof ElderlyResident er) {
            ps.setNull(ppeCol, Types.INTEGER);
            ps.setNull(hospitalCol, Types.VARCHAR);
            ps.setString(careHomeCol, er.getCareHomeName());
        } else {
            ps.setNull(ppeCol, Types.INTEGER);
            ps.setNull(hospitalCol, Types.VARCHAR);
            ps.setNull(careHomeCol, Types.VARCHAR);
        }
    }

    // Polymorphic reconstruction: the concrete subclass is chosen from the
    // discriminator column, so the caller receives List<Person> with mixed
    // runtime types.
    private Person mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("person_id");
        String fullName = rs.getString("full_name");
        int age = rs.getInt("age");
        String districtId = rs.getString("district_id");
        HealthState state = HealthState.valueOf(rs.getString("health_state"));
        int daysInState = rs.getInt("days_in_state");
        boolean vaccinated = rs.getInt("vaccinated") == 1;
        double immunity = rs.getDouble("immunity_level");
        String type = rs.getString("person_type");

        return switch (type) {
            case "HEALTHCARE_WORKER" -> new HealthcareWorker(id, fullName, age, districtId, state, daysInState,
                    vaccinated, immunity, rs.getInt("has_ppe") == 1, rs.getString("hospital_assigned"));
            case "ELDERLY" -> new ElderlyResident(id, fullName, age, districtId, state, daysInState,
                    vaccinated, immunity, rs.getString("care_home_name"));
            default -> new Citizen(id, fullName, age, districtId, state, daysInState, vaccinated, immunity);
        };
    }
}
