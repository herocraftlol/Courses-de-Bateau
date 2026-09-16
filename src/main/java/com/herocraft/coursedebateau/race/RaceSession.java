package com.herocraft.coursedebateau.race;

import com.herocraft.coursedebateau.CourseDeBateauPlugin;
import com.herocraft.coursedebateau.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Etat "en direct" d'une course de bateau : joueurs presents, avancement dans le
 * compte a rebours, bateaux, progression sur les checkpoints, chronometrage,
 * classement et confinement des spectateurs apres l'arrivee.
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
    private final RaceScoreboard scoreboard = new RaceScoreboard();

    private RaceState state = RaceState.WAITING;

    /** Ordre d'insertion = ordre d'arrivee, utilise pour attribuer les spawns bateau. */
    private final Map<UUID, PlayerRaceData> participants = new LinkedHashMap<>();
    private final Map<UUID, Location> returnLocations = new LinkedHashMap<>();
    private final Map<UUID, Location> assignedBoatSpawns = new LinkedHashMap<>();

    /** Contenu original du slot 0 de la hotbar des admins, le temps qu'ils tiennent le diamant de lancement. */
    private final Map<UUID, ItemStack> savedSlot0Items = new LinkedHashMap<>();

    private BukkitTask lobbyCountdownTask;
    private BukkitTask freezeTask;
    private BukkitTask startCountdownTask;
    private BukkitTask tickTask;
    private BukkitTask resetTask;

    private int lobbyCountdownRemaining;
    private int startCountdownRemaining;
    private int finishedCount;
    private long raceStartTimeMillis;

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

    /**
     * Position figee assignee au bateau de ce joueur tant que la course est en
     * phase STARTING (verrouillage avant le depart). Renvoie null en dehors de
     * cette phase, ou si le joueur n'a pas de bateau assigne.
     */
    public Location getLockedBoatSpawn(UUID playerId) {
        if (state != RaceState.STARTING) return null;
        return assignedBoatSpawns.get(playerId);
    }

    /** Verifie si un item est bien le diamant de lancement admin (via sa persistent data). */
    public static boolean isStartItem(CourseDeBateauPlugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        Byte tag = meta.getPersistentDataContainer().get(plugin.getStartItemKey(), PersistentDataType.BYTE);
        return tag != null && tag == (byte) 1;
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
        giveStartItemIfAdmin(player);

        broadcast("&a" + player.getName() + " &7a rejoint la course &b" + race.getName()
                + " &7(" + participants.size() + "/" + resolvedMax + ")");

        checkAutoStart();
        return JoinResult.SUCCESS;
    }

    /** Retire un joueur present (que ce soit au lobby, en course, ou en spectateur). */
    public boolean leave(Player player) {
        UUID id = player.getUniqueId();
        PlayerRaceData data = participants.remove(id);
        if (data == null) {
            return false;
        }

        if (data.isSpectating()) {
            restoreFromSpectator(player, data);
        } else {
            releaseFromBoat(player, data);
        }
        teleportBack(player, id);
        scoreboard.remove(player);
        removeStartItem(player);
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

    /**
     * Donne le diamant de lancement (slot 0 de la hotbar) a un admin qui rejoint le
     * lobby d'attente, en sauvegardant ce qu'il y avait avant pour le restaurer plus
     * tard. Ne fait rien pour un joueur sans la permission cdb.admin.
     */
    private void giveStartItemIfAdmin(Player player) {
        if (!player.hasPermission("cdb.admin")) return;
        UUID id = player.getUniqueId();
        if (savedSlot0Items.containsKey(id)) return; // deja donne

        savedSlot0Items.put(id, player.getInventory().getItem(0));

        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.format("&b&lLancer la course"));
        meta.setLore(List.of(MessageUtil.format("&7Clic droit pour lancer"),
                MessageUtil.format("&7immediatement &e" + race.getName() + "&7.")));
        meta.getPersistentDataContainer().set(plugin.getStartItemKey(), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        player.getInventory().setItem(0, item);
    }

    /** Retire le diamant de lancement (s'il l'a) et restaure ce qu'il y avait avant. */
    private void removeStartItem(Player player) {
        UUID id = player.getUniqueId();
        if (!savedSlot0Items.containsKey(id)) return;
        ItemStack previous = savedSlot0Items.remove(id);
        player.getInventory().setItem(0, previous);
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

    // ================= STARTING (verrouillage dans le bateau) =================

    private void beginStarting() {
        state = RaceState.STARTING;
        // On retire le diamant de lancement admin : on quitte le lobby d'attente.
        for (UUID id : new ArrayList<>(savedSlot0Items.keySet())) {
            Player admin = Bukkit.getPlayer(id);
            if (admin != null) removeStartItem(admin);
        }
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

        // Filet de securite : en plus de la correction instantanee sur VehicleMoveEvent
        // (voir RaceProtectionListener), on reverrouille aussi chaque tick au cas ou.
        freezeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, Location> entry : assignedBoatSpawns.entrySet()) {
                PlayerRaceData data = participants.get(entry.getKey());
                if (data == null || data.getBoat() == null || data.getBoat().isDead()) continue;
                Boat boat = data.getBoat();
                boat.setVelocity(boat.getVelocity().zero());
                Location locked = entry.getValue();
                if (boat.getLocation().distanceSquared(locked) > 0.0001
                        || Math.abs(boat.getLocation().getYaw() - locked.getYaw()) > 0.5) {
                    boat.teleport(locked);
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
        raceStartTimeMillis = System.currentTimeMillis();
        for (UUID id : participants.keySet()) {
            PlayerRaceData data = participants.get(id);
            data.resetProgress(raceStartTimeMillis);
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                scoreboard.assign(player);
            }
        }
        broadcast("&a&lC'EST PARTI !");

        int totalCheckpoints = race.getCheckpoints().size();
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID id : new ArrayList<>(participants.keySet())) {
                PlayerRaceData data = participants.get(id);
                if (data == null) continue;
                Player player = Bukkit.getPlayer(id);
                if (player == null) continue;

                if (data.isSpectating()) {
                    enforceSpectatorBounds(player);
                } else if (!data.isFinished()) {
                    checkCheckpoint(player, data, totalCheckpoints);
                }

                scoreboard.update(player, race, this, data, raceManager.getRecordManager(), raceStartTimeMillis);
            }
        }, 2L, 2L);
    }

    private void checkCheckpoint(Player player, PlayerRaceData data, int totalCheckpoints) {
        Location loc = player.getLocation();

        if (data.isWaitingForFinishLine()) {
            CuboidRegion finishLine = race.getStartZone();
            if (finishLine != null && finishLine.contains(loc)) {
                long now = System.currentTimeMillis();
                data.completeLap(now);
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
            // Validation silencieuse : pas de message de chat, le sidebar suffit a
            // suivre la progression (le joueur ne doit pas voir qu'un checkpoint a ete pointe).
            data.advanceCheckpoint(totalCheckpoints);
        }
    }

    private void finishPlayer(Player player, PlayerRaceData data) {
        finishedCount++;
        long totalMillis = System.currentTimeMillis() - raceStartTimeMillis;
        data.markFinished(finishedCount, totalMillis);

        RecordManager records = raceManager.getRecordManager();
        RecordManager.SubmitResult result = records.submitTime(race.getName(), player.getUniqueId(),
                player.getName(), totalMillis);

        String extra = "";
        if (result.newGlobalRecord()) {
            extra = " &6&l[NOUVEAU RECORD SERVEUR !]";
        } else if (result.newPersonalRecord()) {
            extra = " &a&l[Nouveau record personnel !]";
        }
        broadcast("&6&l#" + finishedCount + " &b" + player.getName() + " &7a termine la course &b"
                + race.getName() + " &7en &e" + RecordManager.format(totalMillis) + extra);

        releaseFromBoat(player, data);
        enterSpectatorMode(player, data);

        if (activeRacerCount() == 0) {
            endRace();
        }
    }

    // ================= MODE SPECTATEUR (apres l'arrivee) =================

    private void enterSpectatorMode(Player player, PlayerRaceData data) {
        if (race.getSpectatorZone() == null) {
            // Pas de zone configuree : comportement simple, on renvoie le joueur direct.
            teleportBack(player, player.getUniqueId());
            return;
        }
        data.enterSpectatorMode(player.getGameMode());
        player.setGameMode(GameMode.SPECTATOR);
        Location anchor = race.resolveSpectatorAnchor();
        if (anchor != null) {
            player.teleport(anchor);
        }
        MessageUtil.sendPrefixed(player, "&7Tu es maintenant spectateur de la course. Fais &e/cdb leave&7 pour sortir.");
    }

    private void restoreFromSpectator(Player player, PlayerRaceData data) {
        if (!data.isSpectating()) return;
        GameMode previous = data.getPreviousGameMode();
        player.setGameMode(previous != null ? previous : GameMode.SURVIVAL);
        data.exitSpectatorMode();
    }

    /** Verifie que le spectateur reste dans la zone autorisee ; sinon le renvoie au centre. */
    private void enforceSpectatorBounds(Player player) {
        CuboidRegion zone = race.getSpectatorZone();
        if (zone == null) return;
        if (!zone.contains(player.getLocation())) {
            Location anchor = race.resolveSpectatorAnchor();
            if (anchor != null) {
                player.teleport(anchor);
                MessageUtil.sendPrefixed(player, "&cTu ne peux pas sortir de la zone spectateur de la course.");
            }
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
                broadcast("&e#" + data.getFinishRank() + " &f" + name + " &7- &e"
                        + RecordManager.format(data.getCurrentTotalMillis(raceStartTimeMillis)));
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
                if (data.isSpectating()) {
                    restoreFromSpectator(player, data);
                } else {
                    releaseFromBoat(player, data);
                }
                teleportBack(player, id);
                scoreboard.remove(player);
                removeStartItem(player);
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
                if (data.isSpectating()) {
                    restoreFromSpectator(player, data);
                } else {
                    releaseFromBoat(player, data);
                }
                teleportBack(player, id);
                scoreboard.remove(player);
                removeStartItem(player);
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
