package com.herocraft.coursedebateau.race;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stocke toute la configuration statique (definie par un admin) d'une course de
 * bateau :
 * - le nom de la course
 * - le point de lobby (attente avant le depart)
 * - les points de depart individuels (un par joueur, avec orientation du bateau)
 * - la zone de depart/arrivee (startZone), DISTINCTE des points de passage : c'est
 *   elle qui declenche la validation d'un tour, pas un checkpoint classique
 * - les points de passage (checkpoints), dans l'ordre, purement intermediaires
 * - la zone dans laquelle les joueurs ayant termine restent en mode spectateur
 *   (spectatorZone) et le point ou ils apparaissent en spectateur (spectatorSpawn)
 * - le nombre de tours a effectuer
 * - min/max joueurs et durees de compte a rebours specifiques a CETTE course
 *   (-1 = non defini, on retombe alors sur les valeurs globales du config.yml)
 *
 * La logique "en direct" (joueurs presents, etat, bateaux, chronos...) est geree a
 * part par {@link RaceSession}, afin de garder cette classe uniquement dediee a la
 * configuration persistee sur le disque.
 */
public class Race {

    private final String name;

    private Location lobbySpawn;
    private final List<Location> boatSpawns = new ArrayList<>();

    private CuboidRegion startZone;
    private final List<CuboidRegion> checkpoints = new ArrayList<>();

    private CuboidRegion spectatorZone;
    private Location spectatorSpawn;

    private int laps = 1;
    private int maxPlayers = -1;
    private int minPlayers = -1;
    private int lobbyCountdownSeconds = -1;
    private int startCountdownSeconds = -1;

    public Race(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // ---- Configuration de base ----

    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    public void setLobbySpawn(Location lobbySpawn) {
        this.lobbySpawn = lobbySpawn;
    }

    public List<Location> getBoatSpawns() {
        return Collections.unmodifiableList(boatSpawns);
    }

    public int addBoatSpawn(Location loc) {
        boatSpawns.add(loc);
        return boatSpawns.size();
    }

    /** Supprime le spawn a l'index donne (1-based). Renvoie false si invalide. */
    public boolean removeBoatSpawn(int index1Based) {
        if (index1Based < 1 || index1Based > boatSpawns.size()) {
            return false;
        }
        boatSpawns.remove(index1Based - 1);
        return true;
    }

    // ---- Zone de depart / arrivee ----

    public CuboidRegion getStartZone() {
        return startZone;
    }

    public void setStartZone(CuboidRegion startZone) {
        this.startZone = startZone;
    }

    // ---- Points de passage (purement intermediaires, distincts de la startZone) ----

    public List<CuboidRegion> getCheckpoints() {
        return Collections.unmodifiableList(checkpoints);
    }

    /** Renvoie le checkpoint a l'index 0-based donne, ou null si hors bornes. */
    public CuboidRegion getCheckpoint(int index0Based) {
        if (index0Based < 0 || index0Based >= checkpoints.size()) return null;
        return checkpoints.get(index0Based);
    }

    /**
     * Definit (ou remplace) le checkpoint a l'index donne (1-based, comme vu par
     * l'admin). Pas de "trous" autorises : il faut definir 1 avant 2, etc.
     * Renvoie false si l'index est invalide.
     */
    public boolean setCheckpoint(int index1Based, CuboidRegion region) {
        if (index1Based < 1) return false;
        if (index1Based <= checkpoints.size()) {
            checkpoints.set(index1Based - 1, region);
            return true;
        }
        if (index1Based == checkpoints.size() + 1) {
            checkpoints.add(region);
            return true;
        }
        return false;
    }

    public boolean removeCheckpoint(int index1Based) {
        if (index1Based < 1 || index1Based > checkpoints.size()) return false;
        checkpoints.remove(index1Based - 1);
        return true;
    }

    // ---- Zone spectateur (apres la course pour un joueur) ----

    public CuboidRegion getSpectatorZone() {
        return spectatorZone;
    }

    public void setSpectatorZone(CuboidRegion spectatorZone) {
        this.spectatorZone = spectatorZone;
    }

    public Location getSpectatorSpawn() {
        return spectatorSpawn;
    }

    public void setSpectatorSpawn(Location spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn;
    }

    /**
     * Point de reference ou renvoyer un spectateur qui essaie de sortir de la
     * spectatorZone : le spawn spectateur explicite si defini, sinon le centre
     * geometrique de la zone, sinon (repli) le lobby.
     */
    public Location resolveSpectatorAnchor() {
        if (spectatorSpawn != null) return spectatorSpawn;
        if (spectatorZone != null) return spectatorZone.getCenter();
        return lobbySpawn;
    }

    // ---- Reglages divers ----

    public int getLaps() {
        return laps;
    }

    public void setLaps(int laps) {
        this.laps = Math.max(1, laps);
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers <= 0 ? -1 : maxPlayers;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers <= 0 ? -1 : minPlayers;
    }

    public int getLobbyCountdownSeconds() {
        return lobbyCountdownSeconds;
    }

    public void setLobbyCountdownSeconds(int seconds) {
        this.lobbyCountdownSeconds = seconds <= 0 ? -1 : seconds;
    }

    public int getStartCountdownSeconds() {
        return startCountdownSeconds;
    }

    public void setStartCountdownSeconds(int seconds) {
        this.startCountdownSeconds = seconds <= 0 ? -1 : seconds;
    }

    /**
     * Une course est jouable des lors qu'elle a : un lobby, une zone de
     * depart/arrivee, au moins 1 point de passage intermediaire (ce qui empeche de
     * valider un tour en restant sur la ligne de depart) et au moins autant de
     * spawns bateau que le minimum de joueurs resolu (verifie au niveau du
     * RaceManager qui connait les valeurs globales de repli). La zone spectateur
     * est OPTIONNELLE : sans elle, les joueurs ayant termine sont simplement
     * renvoyes a leur position d'origine (pas de confinement spectateur).
     */
    public boolean isFullyConfigured(int resolvedMinPlayers) {
        return lobbySpawn != null
                && startZone != null
                && !checkpoints.isEmpty()
                && boatSpawns.size() >= Math.max(1, resolvedMinPlayers);
    }

    // ---- Sauvegarde / chargement ----

    public void saveToConfig(FileConfiguration config) {
        saveLocation(config, "lobby", lobbySpawn);

        config.set("boatspawns", null);
        for (int i = 0; i < boatSpawns.size(); i++) {
            saveLocation(config, "boatspawns." + (i + 1), boatSpawns.get(i));
        }

        config.set("startzone", null);
        saveRegion(config, "startzone", startZone);

        config.set("checkpoints", null);
        for (int i = 0; i < checkpoints.size(); i++) {
            saveRegion(config, "checkpoints." + (i + 1), checkpoints.get(i));
        }

        config.set("spectatorzone", null);
        saveRegion(config, "spectatorzone", spectatorZone);
        config.set("spectatorspawn", null);
        saveLocation(config, "spectatorspawn", spectatorSpawn);

        config.set("laps", laps);
        config.set("max-players", maxPlayers > 0 ? maxPlayers : null);
        config.set("min-players", minPlayers > 0 ? minPlayers : null);
        config.set("lobby-countdown-seconds", lobbyCountdownSeconds > 0 ? lobbyCountdownSeconds : null);
        config.set("start-countdown-seconds", startCountdownSeconds > 0 ? startCountdownSeconds : null);
    }

    public void loadFromConfig(FileConfiguration config) {
        this.lobbySpawn = loadLocation(config, "lobby");

        boatSpawns.clear();
        int i = 1;
        while (config.isSet("boatspawns." + i + ".world")) {
            Location loc = loadLocation(config, "boatspawns." + i);
            if (loc != null) boatSpawns.add(loc);
            i++;
        }

        this.startZone = loadRegion(config, "startzone");

        checkpoints.clear();
        i = 1;
        while (config.isSet("checkpoints." + i + ".corner1.world")) {
            CuboidRegion region = loadRegion(config, "checkpoints." + i);
            if (region != null) checkpoints.add(region);
            i++;
        }

        this.spectatorZone = loadRegion(config, "spectatorzone");
        this.spectatorSpawn = loadLocation(config, "spectatorspawn");

        this.laps = Math.max(1, config.getInt("laps", 1));
        this.maxPlayers = config.isSet("max-players") ? config.getInt("max-players") : -1;
        this.minPlayers = config.isSet("min-players") ? config.getInt("min-players") : -1;
        this.lobbyCountdownSeconds = config.isSet("lobby-countdown-seconds") ? config.getInt("lobby-countdown-seconds") : -1;
        this.startCountdownSeconds = config.isSet("start-countdown-seconds") ? config.getInt("start-countdown-seconds") : -1;
    }

    private void saveLocation(FileConfiguration config, String path, Location loc) {
        if (loc == null) return;
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
    }

    private Location loadLocation(FileConfiguration config, String path) {
        if (!config.isSet(path + ".world")) return null;
        World world = org.bukkit.Bukkit.getWorld(config.getString(path + ".world"));
        if (world == null) return null;
        return new Location(
                world,
                config.getDouble(path + ".x"),
                config.getDouble(path + ".y"),
                config.getDouble(path + ".z"),
                (float) config.getDouble(path + ".yaw"),
                (float) config.getDouble(path + ".pitch")
        );
    }

    private void saveRegion(FileConfiguration config, String path, CuboidRegion region) {
        if (region == null) return;
        saveLocation(config, path + ".corner1", region.getCorner1());
        saveLocation(config, path + ".corner2", region.getCorner2());
    }

    private CuboidRegion loadRegion(FileConfiguration config, String path) {
        Location c1 = loadLocation(config, path + ".corner1");
        Location c2 = loadLocation(config, path + ".corner2");
        if (c1 == null || c2 == null) return null;
        return new CuboidRegion(c1, c2);
    }
}
