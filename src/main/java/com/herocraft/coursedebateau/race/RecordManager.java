package com.herocraft.coursedebateau.race;

import com.herocraft.coursedebateau.CourseDeBateauPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Gere les records de temps (record personnel par joueur, record global du serveur)
 * pour chaque course, persistes sur disque dans plugins/CourseDeBateau/records/<course>.yml.
 */
public class RecordManager {

    /** Resultat de la soumission d'un temps : indique si un ou plusieurs records ont ete battus. */
    public record SubmitResult(boolean newPersonalRecord, boolean newGlobalRecord,
                                long previousPersonal, long previousGlobal) {
    }

    private final CourseDeBateauPlugin plugin;
    private final File recordsDir;

    public RecordManager(CourseDeBateauPlugin plugin) {
        this.plugin = plugin;
        this.recordsDir = new File(plugin.getDataFolder(), "records");
        if (!recordsDir.exists()) {
            recordsDir.mkdirs();
        }
    }

    private File fileFor(String raceName) {
        return new File(recordsDir, raceName.toLowerCase() + ".yml");
    }

    public long getGlobalRecordMillis(String raceName) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(fileFor(raceName));
        return config.getLong("global.millis", -1);
    }

    public String getGlobalRecordHolderName(String raceName) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(fileFor(raceName));
        return config.getString("global.holder", null);
    }

    public long getPersonalRecordMillis(String raceName, UUID playerId) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(fileFor(raceName));
        return config.getLong("players." + playerId + ".millis", -1);
    }

    /**
     * Enregistre un temps de course termine pour un joueur, met a jour son record
     * personnel et le record global si ameliores, et sauvegarde immediatement.
     */
    public SubmitResult submitTime(String raceName, UUID playerId, String playerName, long millis) {
        File file = fileFor(raceName);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        long previousPersonal = config.getLong("players." + playerId + ".millis", -1);
        long previousGlobal = config.getLong("global.millis", -1);

        boolean newPersonal = previousPersonal < 0 || millis < previousPersonal;
        boolean newGlobal = previousGlobal < 0 || millis < previousGlobal;

        if (newPersonal) {
            config.set("players." + playerId + ".millis", millis);
            config.set("players." + playerId + ".name", playerName);
        }
        if (newGlobal) {
            config.set("global.millis", millis);
            config.set("global.holder", playerName);
            config.set("global.holder-uuid", playerId.toString());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder les records de " + raceName + " : " + e.getMessage());
        }

        return new SubmitResult(newPersonal, newGlobal, previousPersonal, previousGlobal);
    }

    /** Supprime completement les records d'une course (utilise si la course est supprimee). */
    public void deleteRecords(String raceName) {
        File file = fileFor(raceName);
        if (file.exists()) {
            file.delete();
        }
    }

    /** Formate un temps en millisecondes au format m:ss (ou --:-- si negatif/absent). */
    public static String format(long millis) {
        if (millis < 0) return "--:--";
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
