package com.episim.dao;

import com.episim.model.District;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * District's primary key is the natural String district_id from schema.sql
 * (e.g. "KL-CENTRAL"), not a generated surrogate integer, so this DAO offers
 * an equivalent String-keyed CRUD API rather than implementing the
 * int-keyed generic {@link Dao} contract.
 */
public class DistrictDao {

    /**
     * Inserts a new row into {@code district}.
     *
     * @param district the district to insert (its {@code id} is the primary key, supplied by the caller)
     * @throws SQLException if the insert fails
     */
    public void insert(District district) throws SQLException {
        // PreparedStatement with bound parameters throughout — never string-concatenated SQL — to prevent SQL injection.
        String sql = "INSERT INTO district (district_id, name, population, density_factor, hospital_capacity) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, district);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert district '" + district.getId() + "'", e);
        }
    }

    /**
     * @param id the district's natural key, e.g. {@code "KL-CENTRAL"}
     * @return the matching district, or {@link Optional#empty()} if no row has that id
     * @throws SQLException if the query fails
     */
    public Optional<District> findById(String id) throws SQLException {
        String sql = "SELECT * FROM district WHERE district_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find district with id " + id, e);
        }
    }

    /**
     * @return every district, ordered alphabetically by name
     * @throws SQLException if the query fails
     */
    public List<District> findAll() throws SQLException {
        String sql = "SELECT * FROM district ORDER BY name";
        List<District> districts = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                districts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load districts", e);
        }
        return districts;
    }

    /**
     * Updates the row matching {@code district}'s id with its current field values.
     *
     * @param district the district to update
     * @throws SQLException if the update fails
     */
    public void update(District district) throws SQLException {
        String sql = "UPDATE district SET name = ?, population = ?, density_factor = ?, hospital_capacity = ? "
                + "WHERE district_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, district.getName());
            ps.setInt(2, district.getPopulation());
            ps.setDouble(3, district.getDensityFactor());
            ps.setInt(4, district.getHospitalCapacity());
            ps.setString(5, district.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update district with id " + district.getId(), e);
        }
    }

    /**
     * @param id the natural key of the row to delete
     * @throws SQLException if the delete fails
     */
    public void delete(String id) throws SQLException {
        String sql = "DELETE FROM district WHERE district_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete district with id " + id, e);
        }
    }

    /** Binds every column, in column order, for insert. */
    private void bind(PreparedStatement ps, District district) throws SQLException {
        ps.setString(1, district.getId());
        ps.setString(2, district.getName());
        ps.setInt(3, district.getPopulation());
        ps.setDouble(4, district.getDensityFactor());
        ps.setInt(5, district.getHospitalCapacity());
    }

    /** Builds a {@link District} from the current row of a {@code district} query. */
    private District mapRow(ResultSet rs) throws SQLException {
        return new District(
                rs.getString("district_id"),
                rs.getString("name"),
                rs.getInt("population"),
                rs.getDouble("density_factor"),
                rs.getInt("hospital_capacity"));
    }
}
