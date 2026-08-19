package com.episim.util;

import com.episim.io.ReportIoException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads config.properties from the working directory if present, otherwise falls back to sensible
 * defaults. A second, simple File I/O example alongside com.episim.io: java.util.Properties.load() to
 * read, Properties.store() to write changes back.
 */
public final class AppConfig {

    private static final Path CONFIG_PATH = Path.of("config.properties");

    private static final String KEY_DEFAULT_POPULATION = "default.population";
    private static final String KEY_DEFAULT_DAYS = "default.days";
    private static final String KEY_EXPORT_DIRECTORY = "export.directory";
    private static final String KEY_AUTOSAVE_ENABLED = "autosave.enabled";

    private static final int DEFAULT_POPULATION = 2000;
    private static final int DEFAULT_DAYS = 120;
    private static final String DEFAULT_EXPORT_DIRECTORY = "exports";
    private static final boolean DEFAULT_AUTOSAVE_ENABLED = false;

    private final Properties properties;

    private AppConfig(Properties properties) {
        this.properties = properties;
    }

    /**
     * Loads {@code config.properties} from the working directory. If the file does not exist, returns
     * an {@code AppConfig} whose getters all fall back to built-in defaults.
     *
     * @return a config instance reflecting the file's current contents (or the defaults, if absent)
     * @throws ReportIoException if the file exists but cannot be read
     */
    public static AppConfig load() {
        Properties properties = new Properties();
        if (Files.exists(CONFIG_PATH)) {
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                properties.load(in);
            } catch (IOException e) {
                throw new ReportIoException("Failed to read " + CONFIG_PATH.toAbsolutePath(), e);
            }
        }
        return new AppConfig(properties);
    }

    /** @return the default population size for a new run (falls back to 2000 if unset or unparsable) */
    public int getDefaultPopulation() {
        return getInt(KEY_DEFAULT_POPULATION, DEFAULT_POPULATION);
    }

    /**
     * Sets and immediately persists the default population size.
     *
     * @param value the new default, written to {@code config.properties} straight away
     * @throws ReportIoException if the file cannot be written
     */
    public void setDefaultPopulation(int value) {
        properties.setProperty(KEY_DEFAULT_POPULATION, Integer.toString(value));
        save();
    }

    /** @return the default simulation length in days for a new run (falls back to 120 if unset or unparsable) */
    public int getDefaultDays() {
        return getInt(KEY_DEFAULT_DAYS, DEFAULT_DAYS);
    }

    /**
     * Sets and immediately persists the default simulation length.
     *
     * @param value the new default, written to {@code config.properties} straight away
     * @throws ReportIoException if the file cannot be written
     */
    public void setDefaultDays(int value) {
        properties.setProperty(KEY_DEFAULT_DAYS, Integer.toString(value));
        save();
    }

    /** @return the folder CSV/text exports default to (falls back to {@code "exports"} if unset) */
    public String getExportDirectory() {
        return properties.getProperty(KEY_EXPORT_DIRECTORY, DEFAULT_EXPORT_DIRECTORY);
    }

    /**
     * Sets and immediately persists the default export directory — called after every successful
     * export so the next one remembers where the user last saved.
     *
     * @param value the new default directory path, written to {@code config.properties} straight away
     * @throws ReportIoException if the file cannot be written
     */
    public void setExportDirectory(String value) {
        properties.setProperty(KEY_EXPORT_DIRECTORY, value);
        save();
    }

    /** @return whether autosave is enabled (falls back to {@code false} if unset); reserved for future use */
    public boolean isAutosaveEnabled() {
        String value = properties.getProperty(KEY_AUTOSAVE_ENABLED);
        return value == null ? DEFAULT_AUTOSAVE_ENABLED : Boolean.parseBoolean(value);
    }

    /**
     * Sets and immediately persists the autosave flag.
     *
     * @param value the new value, written to {@code config.properties} straight away
     * @throws ReportIoException if the file cannot be written
     */
    public void setAutosaveEnabled(boolean value) {
        properties.setProperty(KEY_AUTOSAVE_ENABLED, Boolean.toString(value));
        save();
    }

    /** Parses an integer property, falling back silently (not throwing) on a missing or malformed value. */
    private int getInt(String key, int fallback) {
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void save() {
        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            properties.store(out, "EpiSim configuration");
        } catch (IOException e) {
            throw new ReportIoException("Failed to write " + CONFIG_PATH.toAbsolutePath(), e);
        }
    }
}
