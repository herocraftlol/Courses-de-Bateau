package com.herocraft.coursedebateau.listeners;

import com.herocraft.coursedebateau.CourseDeBateauPlugin;
import com.herocraft.coursedebateau.race.RaceManager;
import com.herocraft.coursedebateau.race.RaceSession;
import com.herocraft.coursedebateau.race.RaceState;
import com.herocraft.coursedebateau.util.MessageUtil;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;

/**
 * Empeche les joueurs inscrits a une course de sortir de leur bateau pendant le
 * compte a rebours de depart ou la course elle-meme (seul /cdb leave le permet),
 * les protege des degats pendant ce temps, et gere la deconnexion en cours de course.
 */
public class RaceProtectionListener implements Listener {

    private final CourseDeBateauPlugin plugin;

    public RaceProtectionListener(CourseDeBateauPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player player)) {
            return;
        }
        if (!(event.getVehicle() instanceof Boat)) {
            return;
        }

        RaceManager raceManager = plugin.getRaceManager();

        // Jeton pose par RaceSession juste avant une sortie programmatique (leave, fin de course...).
        if (raceManager.consumeExitAllowed(player.getUniqueId())) {
            return;
        }

        RaceSession session = raceManager.getSessionOf(player.getUniqueId());
        if (session == null) {
            return;
        }
        RaceState state = session.getState();
        if (state == RaceState.STARTING || state == RaceState.RUNNING) {
            event.setCancelled(true);
            MessageUtil.sendPrefixed(player, "&cTu ne peux pas quitter ton bateau pendant la course ! Utilise &e/cdb leave&c pour abandonner.");
        }
    }

    /**
     * Verrouille reellement le bateau pendant la phase STARTING : des qu'il bouge
     * (meme legerement, sous l'effet de la pagaie), on le reteleporte instantanement
     * a sa position assignee et on annule sa vitesse. Combine au filet de securite
     * de RaceSession (verification chaque tick), cela rend le bateau immobile.
     */
    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) {
            return;
        }
        if (boat.getPassengers().isEmpty() || !(boat.getPassengers().get(0) instanceof Player player)) {
            return;
        }
        RaceManager raceManager = plugin.getRaceManager();
        RaceSession session = raceManager.getSessionOf(player.getUniqueId());
        if (session == null) {
            return;
        }
        var locked = session.getLockedBoatSpawn(player.getUniqueId());
        if (locked == null) {
            return; // pas (ou plus) en phase STARTING pour ce joueur
        }
        if (boat.getLocation().distanceSquared(locked) > 0.0001 || boat.getVelocity().lengthSquared() > 0.0001) {
            boat.teleport(locked);
            boat.setVelocity(new Vector(0, 0, 0));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        RaceManager raceManager = plugin.getRaceManager();
        RaceSession session = raceManager.getSessionOf(player.getUniqueId());
        if (session == null) {
            return;
        }
        RaceState state = session.getState();
        if (state == RaceState.STARTING || state == RaceState.RUNNING || state == RaceState.LOBBY_COUNTDOWN) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!plugin.getConfig().getBoolean("remove-on-disconnect", true)) {
            return;
        }
        RaceManager raceManager = plugin.getRaceManager();
        RaceSession session = raceManager.getSessionOf(event.getPlayer().getUniqueId());
        if (session != null) {
            session.handleDisconnect(event.getPlayer().getUniqueId());
        }
    }
}
