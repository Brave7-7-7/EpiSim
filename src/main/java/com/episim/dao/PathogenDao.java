package com.episim.dao;

import com.episim.model.Pathogen;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD access to the {@code pathogen} reference-data table. */
public class PathogenDao implements Dao<Pathogen> {

    /** {@inheritDoc} Writes a new row to {@code pathogen}. */
    @Override
    public void insert(Pathogen pathogen) throws SQLException {
        // PreparedStatement with bound parameters throughout — never string-concatenated SQL — to prevent SQL injection.
        String sql = "INSERT INTO pathogen (name, r0, incubation_days, infectious_days, "
                + "hospitalisation_rate, mortality_rate, vaccine_effectiveness, description) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, pathogen);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    pathogen.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert pathogen '" + pathogen.getName() + "'", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Pathogen> findById(int id) throws SQLException {
        String sql = "SELECT * FROM pathogen WHERE pathogen_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find pathogen with id " + id, e);
        }
    }

    /** {@inheritDoc} Ordered alphabetically by name, for a stable picker order in the GUI. */
    @Override
    public List<Pathogen> findAll() throws SQLException {
        String sql = "SELECT * FROM pathogen ORDER BY name";
        List<Pathogen> pathogens = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                pathogens.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load pathogens", e);
        }
        return pathogens;
    }

    /** {@inheritDoc} */
    @Override
    public void update(Pathogen pathogen) throws SQLException {
        String sql = "UPDATE pathogen SET name = ?, r0 = ?, incubation_days = ?, infectious_days = ?, "
                + "hospitalisation_rate = ?, mortality_rate = ?, vaccine_effectiveness = ?, description = ? "
                + "WHERE pathogen_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, pathogen);
            ps.setInt(9, pathogen.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update pathogen with id " + pathogen.getId(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM pathogen WHERE pathogen_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete pathogen with id " + id, e);
        }
    }

    /** Binds every column except the primary key, in column order, for both insert and update. */
    private void bind(PreparedStatement ps, Pathogen pathogen) throws SQLException {
        ps.setString(1, pathogen.getName());
        ps.setDouble(2, pathogen.getR0());
        ps.setInt(3, pathogen.getIncubationDays());
        ps.setInt(4, pathogen.getInfectiousDays());
        ps.setDouble(5, pathogen.getHospitalisationRate());
        ps.setDouble(6, pathogen.getMortalityRate());
        ps.setDouble(7, pathogen.getVaccineEffectiveness());
        ps.setString(8, pathogen.getDescription());
    }

    /** Builds a {@link Pathogen} from the current row of a {@code pathogen} query. */
    private Pathogen mapRow(ResultSet rs) throws SQLException {
        return new Pathogen(
                rs.getInt("pathogen_id"),
                rs.getString("name"),
                rs.getDouble("r0"),
                rs.getInt("incubation_days"),
                rs.getInt("infectious_days"),
                rs.getDouble("hospitalisation_rate"),
                rs.getDouble("mortality_rate"),
                rs.getDouble("vaccine_effectiveness"),
                rs.getString("description"));
    }
}
