package com.episim;

import com.episim.dao.DataAccessException;
import com.episim.dao.DatabaseManager;
import com.episim.dao.PathogenDao;
import com.episim.gui.MainDashboard;
import com.episim.model.Pathogen;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.List;

/**
 * Application entry point: sets Nimbus look and feel, boots the database, and opens the GUI.
 */
public class Main {

    public static void main(String[] args) {
        setNimbusLookAndFeel();
        DatabaseManager.initialise();

        if (!DatabaseManager.isHealthy()) {
            JOptionPane.showMessageDialog(null,
                    "The database at data/episim.db failed its integrity check. "
                            + "Delete the file and restart to rebuild it, or restore from a backup.",
                    "Database Integrity Check Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Pathogen> pathogens;
        try {
            pathogens = new PathogenDao().findAll();
        } catch (Exception e) {
            throw new DataAccessException("Failed to load pathogens at startup", e);
        }
        if (pathogens.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "No pathogens are configured in the database. Delete data/episim.db and restart to reseed it.",
                    "No Pathogens Available", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> new MainDashboard(pathogens).setVisible(true));
    }

    private static void setNimbusLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (Exception e) {
            // Nimbus is unavailable on this JVM; fall back to the platform default look and feel.
        }
    }
}
