package com.herocraft.coursedebateau.race;

import com.herocraft.coursedebateau.CourseDeBateauPlugin;
import com.herocraft.coursedebateau.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Etat "en direct" d'une course de bateau : joueurs presents, avancement dans le
 * compte a rebours, bateaux, progression sur les checkpoints, classement.
 *
 * Une instance existe en permanence par course (creee par le {@link RaceManager}),
 * meme quand personne n'y joue (elle est alors simplement en etat WAITING, vide).
 */
public class RaceSession {

    public enum JoinResult {
        SUCCESS,
        NOT_CONFIGURED,
        ALREADY_IN_THIS_RACE,
        ALREADY_IN_ANOTHER_RACE,
        RACE_FULL,
        RACE_IN_PROGRESS
    }

    private final CourseDeBateauPlugin plugin;
    private final RaceManager raceManager;
    private final Race race;

    private RaceState state = RaceState.WAITING;

    /** Ordre d'insertion = ordre d'arrivee, utilise pour attribuer les spawns bateau. */
    private final Map<UUID, PlayerRaceData> participants = new LinkedHashMap<>();
    private final Map<UUID, Location> returnLocations = new LinkedHashMap<>();
    private final Map<UUID, Location> assignedBoatSpawns = new LinkedHashMap<>();

    private BukkitTask lobbyCountdownTask;
    private BukkitTask freezeTask;
    private BukkitTask startCountdownTask;
    private BukkitTask tickTask;
    private BukkitTask resetTask;

    private int lobbyCountdownRemaining;
    private int startCountdownRemaining;
    private int finishedCount;

    public RaceSession(CourseDeBateauPlugin plugin, RaceManager raceManager, Race race) {
        this.plugin = plugin;
        this.raceManager = raceManager;
        this.race = race;
    }

    public Race getRace() {
        return race;
    }

    public RaceState getState() {
        return state;
    }

    public int getPlayerCount() {
        return participants.size();
    }

    public boolean hasPlayer(UUID id) {
        return participants.containsKey(id);
    }

    public List<UUID> getParticipantIds() {
        return new ArrayList<>(participants.keySet());
    }

    // ================= JOIN / LEAVE =================

    public JoinResult join(Player player) {
        int resolvedMin = raceManager.resolveMinPlayers(race);
        int resolvedMax = raceManager.resolveEffectiveMaxPlayers(race);

        if (!race.isFullyConfigured(resolvedMin)) {
            return JoinResult.NOT_CONFIGURED;
        }
        if (participants.containsKey(player.getUniqueId())) {
            return JoinResult.ALREADY_IN_THIS_RACE;
        }
        if (state == RaceState.STARTING || state == RaceState.RUNNING || state == RaceState.ENDING) {
            return JoinResult.RACE_IN_PROGRESS;
        }
        RaceSession elsewhere = raceManager.getSessionOf(player.getUniqueId());
        if (elsewhere != null && elsewhere != this) {
            return JoinResult.ALREADY_IN_ANOTHER_RACE;
        }
        if (participants.size() >= resolvedMax) {
            return JoinResult.RACE_FULL;
        }

        returnLocations.put(player.getUniqueId(), player.getLocation());
        participants.put(player.getUniqueId(), new PlayerRaceData(player.getUniqueId()));
        player.teleport(race.getLobbySpawn());

        broadcast("&a" + player.getName() + " &7a rejoint la course &b" + race.getName()
                + " &7(" + participants.size() + "/" + resolvedMax + ")");

        checkAutoStart();
        return JoinResult.SUCCESS;
    }

    /** Retire un joueur present (que ce soit au lobby ou en pleine course). */
    public boolean leave(Player player) {
        UUID id = player.getUniqueId();
        PlayerRaceData data = participants.remove(id);
        if (data == null) {
            return false;
        }

        releaseFromBoat(player, data);
        teleportBack(player, id);
        returnLocations.remove(id);
        assignedBoatSpawns.remove(id);

        broadcast("&c" + player.getName() + " &7a quitte la course &b" + race.getName());

        if (state == RaceState.LOBBY_COUNTDOWN && participants.size() < raceManager.resolveMinPlayers(race)) {
            cancelLobbyCountdown();
        } else if ((state == RaceState.RUNNING || state == RaceState.STARTING) && activeRacerCount() == 0) {
            endRace();
        }
        return true;
    }

    /** Appele par le listener de deconnexion : pas de teleport (le joueur est hors ligne). */
    public void handleDisconnect(UUID id) {
        PlayerRaceData data = participants.remove(id);
        if (data == null) return;
        if (data.getBoat() != null && !data.getBoat().isDead()) {
            data.getBoat().remove();
        }
        returnLocations.remove(id);
        assignedBoatSpawns.remove(id);

        if (state == RaceState.LOBBY_COUNTDOWN && participants.size() < raceManager.resolveMinPlayers(race)) {
            cancelLobbyCountdown();
        } else if ((state == RaceState.RUNNING || state == RaceState.STARTING) && activeRacerCount() == 0) {
            endRace();
        }
    }

    private long activeRacerCount() {
        return participants.values().stream().filter(d -> !d.isFinished()).count();
    }

    private void releaseFromBoat(Player player, PlayerRaceData data) {
        Boat boat = data.getBoat();
        if (boat != null) {
            raceManager.allowNextExit(player.getUniqueId());
            if (player.getVehicle() == boat) {
                player.leaveVehicle();
            }
            if (!boat.isDead()) {
                boat.remove();
            }
        }
    }

    private void teleportBack(Player player, UUID id) {
        Location back = returnLocations.get(id);
        player.teleport(back != null ? back : player.getWorld().getSpawnLocation());
    }

    // ================= LOBBY COUNTDOWN =================

    private void checkAutoStart() {
        if (state == RaceState.WAITING && participants.size() >= raceManager.resolveMinPlayers(race)) {
            startLobbyCountdown();
        }
    }

    private void startLobbyCountdown() {
        state = RaceState.LOBBY_COUNTDOWN;
        lobbyCountdownRemaining = raceManager.resolveLobbyCountdown(race);
        broadcast("&eNombre de joueurs minimum atteint ! Depart dans &6" + lobbyCountdownRemaining + "s&e.");

        lobbyCountdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            lobbyCountdownRemaining--;
            if (lobbyCountdownRemaining <= 0) {
                lobbyCountdownTask.cancel();
                beginStarting();
                return;
            }
            if (lobbyCountdownRemaining <= 5 || lobbyCountdownRemaining % 10 == 0) {
                broadcast("&eDepart dans &6" + lobbyCountdownRemaining + "s&e...");
            }
        }, 20L, 20L);
    }

    private void cancelLobbyCountdown() {
        if (lobbyCountdownTask != null) {
            lobbyCountdownTask.cancel();
            lobbyCountdownTask = null;
        }
        state = RaceState.WAITING;
        broadcast("&cPas assez de joueurs, compte a rebours annule.");
    }

    // ================= STARTING (freeze dans le bateau) =================

    private void beginStarting() {
        state = RaceState.STARTING;
        List<Location> spawns = race.getBoatSpawns();
        List<UUID> order = getParticipantIds();

        for (int i = 0; i < order.size() && i < spawns.size(); i++) {
            UUID id = order.get(i);
            Player player = Bukkit.getPlayer(id);
            PlayerRaceData data = participants.get(id);
            if (player == null || data == null) continue;

            Location spawnLoc = spawns.get(i).clone();
            assignedBoatSpawns.put(id, spawnLoc);
            player.teleport(spawnLoc);

            Boat boat = spawnLoc.getWorld().spawn(spawnLoc, Boat.class);
            boat.setInvulnerable(true);
            boat.addPassenger(player);
            data.setBoat(boat);
        }

        // Verrouille les bateaux en place tant que le compte a rebours de depart tourne.
        freezeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, Location> entry : assignedBoatSpawns.entrySet()) {
                PlayerRaceData data = participants.get(entry.getKey());
                if (data == null || data.getBoat() == null || data.getBoat().isDead()) continue;
                Boat boat = data.getBoat();
                boat.setVelocity(boat.getVelocity().zero());
                if (boat.getLocation().distanceSquared(entry.getValue()) > 0.01) {
                    boat.teleport(entry.getValue());
                }
            }
        }, 1L, 1L);

        startCountdownRemaining = raceManager.resolveStartCountdown(race);
        broadcast("&bAccrochez-vous ! Depart dans &e" + startCountdownRemaining + "s&b.");

        startCountdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            startCountdownRemaining--;
            if (startCountdownRemaining <= 0) {
                startCountdownTask.cancel();
                beginRunning();
                return;
            }
            broadcast("&e" + startCountdownRemaining + "&b...");
        }, 20L, 20L);
    }

    // ================= RUNNING =================

    private void beginRunning() {
        state = RaceState.RUNNING;
        if (freezeTask != null) {
            freezeTask.cancel();
            freezeTask = null;
        }
        finishedCount = 0;
        broadcast("&a&lC'EST PARTI !");

        int totalCheckpoints = race.getCheckpoints().size();
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID id : new ArrayList<>(participants.keySet())) {
                PlayerRaceData data = participants.get(id);
                if (data == null || data.isFinished()) continue;
                Player player = Bukkit.getPlayer(id);
                if (player == null) continue;
                checkCheckpoint(player, data, totalCheckpoints);
            }
        }, 2L, 2L);
    }

    private void checkCheckpoint(Player player, PlayerRaceData data, int totalCheckpoints) {
        Location loc = player.getLocation();

        if (data.isWaitingForFinishLine()) {
            CuboidRegion finishLine = race.getCheckpoint(0);
            if (finishLine != null && finishLine.contains(loc)) {
                data.completeLap();
                if (data.getLapsCompleted() >= race.getLaps()) {
                    finishPlayer(player, data);
                } else {
                    MessageUtil.sendPrefixed(player, "&aTour &e" + data.getLapsCompleted() + "&a/&e"
                            + race.getLaps() + " &avalide !");
                }
            }
            return;
        }

        CuboidRegion next = race.getCheckpoint(data.getNextCheckpointIndex());
        if (next != null && next.contains(loc)) {
            data.advanceCheckpoint(totalCheckpoints);
            if (data.isWaitingForFinishLine()) {
                MessageUtil.sendPrefixed(player, "&eDernier point de passage ! Retourne a la ligne d'arrivee.");
            } else {
                MessageUtil.sendPrefixed(player, "&aPoint de passage &e" + (data.getNextCheckpointIndex() - 1)
                        + "&a valide !");
            }
        }
    }

    private void finishPlayer(Player player, PlayerRaceData data) {
        finishedCount++;
        data.markFinished(finishedCount);
        broadcast("&6&l#" + finishedCount + " &b" + player.getName() + " &7a termine la course &b"
                + race.getName() + " &7!");

        releaseFromBoat(player, data);
        teleportBack(player, player.getUniqueId());

        if (activeRacerCount() == 0) {
            endRace();
        }
    }

    // ================= FIN DE COURSE =================

    private void endRace() {
        if (state == RaceState.ENDING) return;
        state = RaceState.ENDING;
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (freezeTask != null) {
            freezeTask.cancel();
            freezeTask = null;
        }

        List<PlayerRaceData> ranked = new ArrayList<>(participants.values());
        ranked.sort(Comparator.comparingInt(d -> d.isFinished() ? d.getFinishRank() : Integer.MAX_VALUE));

        broadcast("&b&l=== Resultats de la course " + race.getName() + " ===");
        for (PlayerRaceData data : ranked) {
            Player p = Bukkit.getPlayer(data.getPlayerId());
            String name = p != null ? p.getName() : "???";
            if (data.isFinished()) {
                broadcast("&e#" + data.getFinishRank() + " &f" + name);
            } else {
                broadcast("&7Non termine : &f" + name);
            }
        }

        resetTask = Bukkit.getScheduler().runTaskLater(plugin, this::resetToWaiting, 100L);
    }

    private void resetToWaiting() {
        for (UUID id : new ArrayList<>(participants.keySet())) {
            Player player = Bukkit.getPlayer(id);
            PlayerRaceData data = participants.get(id);
            if (player != null && data != null) {
                releaseFromBoat(player, data);
                teleportBack(player, id);
            }
        }
        participants.clear();
        returnLocations.clear();
        assignedBoatSpawns.clear();
        state = RaceState.WAITING;
    }

    /** Arret immediat et complet de la course (utilise par un admin ou a la suppression de l'arene). */
    public void forceStop() {
        cancelAllTasks();
        for (UUID id : new ArrayList<>(participants.keySet())) {
            Player player = Bukkit.getPlayer(id);
            PlayerRaceData data = participants.get(id);
            if (player != null && data != null) {
                releaseFromBoat(player, data);
                teleportBack(player, id);
            } else if (data != null && data.getBoat() != null && !data.getBoat().isDead()) {
                data.getBoat().remove();
            }
        }
        participants.clear();
        returnLocations.clear();
        assignedBoatSpawns.clear();
        state = RaceState.WAITING;
    }

    private void cancelAllTasks() {
        if (lobbyCountdownTask != null) { lobbyCountdownTask.cancel(); lobbyCountdownTask = null; }
        if (freezeTask != null) { freezeTask.cancel(); freezeTask = null; }
        if (startCountdownTask != null) { startCountdownTask.cancel(); startCountdownTask = null; }
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
        if (resetTask != null) { resetTask.cancel(); resetTask = null; }
    }

    /** Force le lancement immediat de la course (admin), en sautant le compte a rebours du lobby. */
    public boolean forceStart() {
        if (state != RaceState.WAITING && state != RaceState.LOBBY_COUNTDOWN) {
            return false;
        }
        if (participants.isEmpty() || !race.isFullyConfigured(raceManager.resolveMinPlayers(race))) {
            return false;
        }
        if (lobbyCountdownTask != null) {
            lobbyCountdownTask.cancel();
            lobbyCountdownTask = null;
        }
        beginStarting();
        return true;
    }

    private void broadcast(String message) {
        for (UUID id : participants.keySet()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                MessageUtil.sendPrefixed(player, message);
            }
        }
    }
}
