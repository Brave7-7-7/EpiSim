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

    public int getDefaultPopulation() {
        return getInt(KEY_DEFAULT_POPULATION, DEFAULT_POPULATION);
    }

    public void setDefaultPopulation(int value) {
        properties.setProperty(KEY_DEFAULT_POPULATION, Integer.toString(value));
        save();
    }

    public int getDefaultDays() {
        return getInt(KEY_DEFAULT_DAYS, DEFAULT_DAYS);
    }

    public void setDefaultDays(int value) {
        properties.setProperty(KEY_DEFAULT_DAYS, Integer.toString(value));
        save();
    }

    public String getExportDirectory() {
        return properties.getProperty(KEY_EXPORT_DIRECTORY, DEFAULT_EXPORT_DIRECTORY);
    }

    public void setExportDirectory(String value) {
        properties.setProperty(KEY_EXPORT_DIRECTORY, value);
        save();
    }

    public boolean isAutosaveEnabled() {
        String value = properties.getProperty(KEY_AUTOSAVE_ENABLED);
        return value == null ? DEFAULT_AUTOSAVE_ENABLED : Boolean.parseBoolean(value);
    }

    public void setAutosaveEnabled(boolean value) {
        properties.setProperty(KEY_AUTOSAVE_ENABLED, Boolean.toString(value));
        save();
    }

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
