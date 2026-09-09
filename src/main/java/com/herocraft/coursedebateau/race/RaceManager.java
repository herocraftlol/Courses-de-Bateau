package com.herocraft.coursedebateau.race;

import com.herocraft.coursedebateau.CourseDeBateauPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registre central de toutes les courses de bateau configurees sur le serveur.
 * Chaque course possede sa propre {@link Race} (configuration) et sa propre
 * {@link RaceSession} (etat en direct), ce qui permet plusieurs courses
 * independantes et simultanees.
 */
public class RaceManager {

    private final CourseDeBateauPlugin plugin;
    private final Map<String, Race> races = new LinkedHashMap<>();
    private final Map<String, RaceSession> sessions = new LinkedHashMap<>();
    private final File racesDir;
    private final File registryFile;

    /**
     * Jetons a usage unique autorisant la prochaine sortie de vehicule d'un joueur a
     * passer, meme si une course verrouille normalement le bateau. Poses juste avant
     * un ejectement programmatique (leave, fin de course...), consommes par le listener.
     */
    private final Set<UUID> exitAllowed = ConcurrentHashMap.newKeySet();

    public RaceManager(CourseDeBateauPlugin plugin) {
        this.plugin = plugin;
        this.racesDir = new File(plugin.getDataFolder(), "races");
        if (!racesDir.exists()) {
            racesDir.mkdirs();
        }
        this.registryFile = new File(racesDir, "races.yml");
    }

    public CourseDeBateauPlugin getPlugin() {
        return plugin;
    }

    // ---- Chargement / sauvegarde ----

    public void loadAll() {
        YamlConfiguration registry = YamlConfiguration.loadConfiguration(registryFile);
        for (String name : registry.getStringList("races")) {
            loadOne(name);
        }
    }

    private void loadOne(String name) {
        Race race = new Race(name);
        File file = raceFile(name);
        if (file.exists()) {
            race.loadFromConfig(YamlConfiguration.loadConfiguration(file));
        }
        races.put(name.toLowerCase(), race);
        sessions.put(name.toLowerCase(), new RaceSession(plugin, this, race));
    }

    private void saveRegistry() {
        YamlConfiguration registry = new YamlConfiguration();
        registry.set("races", new ArrayList<>(races.values().stream().map(Race::getName).toList()));
        try {
            registry.save(registryFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder la liste des courses : " + e.getMessage());
        }
    }

    public void saveRace(Race race) {
        YamlConfiguration config = new YamlConfiguration();
        race.saveToConfig(config);
        try {
            config.save(raceFile(race.getName()));
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder la course " + race.getName() + " : " + e.getMessage());
        }
    }

    private File raceFile(String name) {
        return new File(racesDir, name.toLowerCase() + ".yml");
    }

    public void reloadAll() {
        for (RaceSession session : sessions.values()) {
            session.forceStop();
        }
        races.clear();
        sessions.clear();
        loadAll();
    }

    // ---- Gestion des courses ----

    public boolean create(String name) {
        String key = name.toLowerCase();
        if (races.containsKey(key)) {
            return false;
        }
        Race race = new Race(name);
        races.put(key, race);
        sessions.put(key, new RaceSession(plugin, this, race));
        saveRace(race);
        saveRegistry();
        return true;
    }

    public boolean delete(String name) {
        String key = name.toLowerCase();
        RaceSession session = sessions.remove(key);
        if (session == null) {
            return false;
        }
        session.forceStop();
        races.remove(key);
        saveRegistry();
        File file = raceFile(name);
        if (file.exists()) file.delete();
        return true;
    }

    public Race getRace(String name) {
        if (name == null) return null;
        return races.get(name.toLowerCase());
    }

    public RaceSession getSession(String name) {
        if (name == null) return null;
        return sessions.get(name.toLowerCase());
    }

    public Collection<Race> getRaces() {
        return races.values();
    }

    public Collection<RaceSession> getSessions() {
        return sessions.values();
    }

    // ---- Valeurs globales de repli (config.yml) ----

    public int resolveMinPlayers(Race race) {
        return race.getMinPlayers() > 0 ? race.getMinPlayers() : plugin.getConfig().getInt("default-min-players", 2);
    }

    public int resolveMaxPlayers(Race race) {
        return race.getMaxPlayers() > 0 ? race.getMaxPlayers() : plugin.getConfig().getInt("default-max-players", 8);
    }

    /**
     * Comme {@link #resolveMaxPlayers}, mais plafonne au nombre de spawns bateau reellement
     * configures (s'il y en a au moins un) : on ne peut jamais accueillir plus de joueurs
     * que de bateaux disponibles au depart.
     */
    public int resolveEffectiveMaxPlayers(Race race) {
        int max = resolveMaxPlayers(race);
        if (!race.getBoatSpawns().isEmpty()) {
            max = Math.min(max, race.getBoatSpawns().size());
        }
        return max;
    }

    public int resolveLobbyCountdown(Race race) {
        return race.getLobbyCountdownSeconds() > 0 ? race.getLobbyCountdownSeconds()
                : plugin.getConfig().getInt("default-lobby-countdown-seconds", 20);
    }

    public int resolveStartCountdown(Race race) {
        return race.getStartCountdownSeconds() > 0 ? race.getStartCountdownSeconds()
                : plugin.getConfig().getInt("default-start-countdown-seconds", 5);
    }

    // ---- Jetons de sortie de vehicule autorisee ----

    public void allowNextExit(UUID playerId) {
        exitAllowed.add(playerId);
    }

    /** Consomme le jeton s'il existe. Renvoie true si la sortie doit etre autorisee. */
    public boolean consumeExitAllowed(UUID playerId) {
        return exitAllowed.remove(playerId);
    }

    /** Renvoie la session dans laquelle ce joueur est actuellement inscrit (attente ou course), ou null. */
    public RaceSession getSessionOf(UUID playerId) {
        for (RaceSession session : sessions.values()) {
            if (session.hasPlayer(playerId)) {
                return session;
            }
        }
        return null;
    }
}
