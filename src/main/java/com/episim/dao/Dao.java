package com.episim.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD contract implemented by every entity-specific DAO.
 *
 * @param <T> the entity type this DAO persists
 */
public interface Dao<T> {

    /**
     * Inserts a new row and writes the generated primary key back onto {@code t}.
     *
     * @param t the entity to insert
     * @throws SQLException if the insert fails
     */
    void insert(T t) throws SQLException;

    /**
     * @param id the entity's primary key
     * @return the matching entity, or {@link Optional#empty()} if no row has that id
     * @throws SQLException if the query fails
     */
    Optional<T> findById(int id) throws SQLException;

    /**
     * @return every row in the table, mapped to its entity type
     * @throws SQLException if the query fails
     */
    List<T> findAll() throws SQLException;

    /**
     * Updates the row matching {@code t}'s primary key with {@code t}'s current field values.
     *
     * @param t the entity to update
     * @throws SQLException if the update fails
     */
    void update(T t) throws SQLException;

    /**
     * @param id the primary key of the row to delete
     * @throws SQLException if the delete fails
     */
    void delete(int id) throws SQLException;
}
