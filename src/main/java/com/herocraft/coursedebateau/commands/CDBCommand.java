package com.herocraft.coursedebateau.commands;

import com.herocraft.coursedebateau.CourseDeBateauPlugin;
import com.herocraft.coursedebateau.gui.RaceGUI;
import com.herocraft.coursedebateau.race.CuboidRegion;
import com.herocraft.coursedebateau.race.Race;
import com.herocraft.coursedebateau.race.RaceManager;
import com.herocraft.coursedebateau.race.RaceSession;
import com.herocraft.coursedebateau.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CDBCommand implements CommandExecutor, TabCompleter {

    private static final Set<String> GLOBAL_SUBCOMMANDS = Set.of(
            "gui", "list", "join", "leave", "create", "delete", "reload", "help");

    private static final Set<String> RACE_ADMIN_ACTIONS = Set.of(
            "info", "setlobby", "addboatspawn", "removeboatspawn", "setcheckpoint", "removecheckpoint",
            "laps", "maxplayers", "minplayers", "lobbycountdown", "startcountdown", "forcestart", "stop");

    private final CourseDeBateauPlugin plugin;
    private final RaceGUI raceGUI;

    /** pos1 en attente pour un checkpoint, cle = joueur + course + index. */
    private final Map<String, Location> pendingCorner1 = new HashMap<>();

    public CDBCommand(CourseDeBateauPlugin plugin) {
        this.plugin = plugin;
        this.raceGUI = new RaceGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (GLOBAL_SUBCOMMANDS.contains(sub)) {
            switch (sub) {
                case "gui" -> handleGui(sender);
                case "list" -> handleList(sender);
                case "join" -> handleJoin(sender, args);
                case "leave" -> handleLeave(sender);
                case "create" -> handleCreate(sender, args);
                case "delete" -> handleDelete(sender, args);
                case "reload" -> handleReload(sender);
                case "help" -> sendHelp(sender);
            }
            return true;
        }

        // Sinon : /cdb <course> <action> ...
        handleRaceAction(sender, args);
        return true;
    }

    // ================= COMMANDES GLOBALES =================

    private void handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendPrefixed(sender, "&cCommande reservee aux joueurs.");
            return;
        }
        player.openInventory(raceGUI.build(player));
    }

    private void handleList(CommandSender sender) {
        RaceManager rm = plugin.getRaceManager();
        if (rm.getRaces().isEmpty()) {
            MessageUtil.sendPrefixed(sender, "&7Aucune course n'a ete creee pour le moment.");
            return;
        }
        MessageUtil.sendPrefixed(sender, "&bCourses disponibles :");
        for (Race race : rm.getRaces()) {
            RaceSession session = rm.getSession(race.getName());
            int max = rm.resolveEffectiveMaxPlayers(race);
            MessageUtil.send(sender, " &7- &b" + race.getName() + " &7(" + session.getPlayerCount() + "/" + max
                    + ") &f[" + session.getState() + "]");
        }
    }

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendPrefixed(sender, "&cCommande reservee aux joueurs.");
            return;
        }
        if (args.length < 2) {
            MessageUtil.sendPrefixed(sender, "&cUsage : /cdb join <course>");
            return;
        }
        RaceManager rm = plugin.getRaceManager();
        Race race = rm.getRace(args[1]);
        if (race == null) {
            MessageUtil.sendPrefixed(sender, "&cCette course n'existe pas.");
            return;
        }
        RaceSession session = rm.getSession(race.getName());
        RaceSession.JoinResult result = session.join(player);
        switch (result) {
            case SUCCESS -> { /* le message de bienvenue est deja diffuse par la session */ }
            case NOT_CONFIGURED -> MessageUtil.sendPrefixed(player, "&cCette course n'est pas encore configuree.");
            case ALREADY_IN_THIS_RACE -> MessageUtil.sendPrefixed(player, "&cTu es deja inscrit a cette course.");
            case ALREADY_IN_ANOTHER_RACE -> MessageUtil.sendPrefixed(player, "&cTu es deja inscrit a une autre course. Fais &e/cdb leave&c d'abord.");
            case RACE_FULL -> MessageUtil.sendPrefixed(player, "&cCette course est complete.");
            case RACE_IN_PROGRESS -> MessageUtil.sendPrefixed(player, "&cCette course est deja en cours, reessaie plus tard.");
        }
    }

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendPrefixed(sender, "&cCommande reservee aux joueurs.");
            return;
        }
        RaceSession session = plugin.getRaceManager().getSessionOf(player.getUniqueId());
        if (session == null) {
            MessageUtil.sendPrefixed(player, "&cTu n'es inscrit a aucune course.");
            return;
        }
        session.leave(player);
        MessageUtil.sendPrefixed(player, "&aTu as quitte la course.");
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!hasAdminPerm(sender)) return;
        if (args.length < 2) {
            MessageUtil.sendPrefixed(sender, "&cUsage : /cdb create <course>");
            return;
        }
        boolean ok = plugin.getRaceManager().create(args[1]);
        MessageUtil.sendPrefixed(sender, ok ? "&aCourse &e" + args[1] + " &acreee !"
                : "&cUne course avec ce nom existe deja.");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!hasAdminPerm(sender)) return;
        if (args.length < 2) {
            MessageUtil.sendPrefixed(sender, "&cUsage : /cdb delete <course>");
            return;
        }
        boolean ok = plugin.getRaceManager().delete(args[1]);
        MessageUtil.sendPrefixed(sender, ok ? "&aCourse &e" + args[1] + " &asupprimee."
                : "&cCette course n'existe pas.");
    }

    private void handleReload(CommandSender sender) {
        if (!hasAdminPerm(sender)) return;
        plugin.reloadConfig();
        MessageUtil.setPrefix(plugin.getConfig().getString("messages.prefix", "&b&lCDB &8» &r"));
        plugin.getRaceManager().reloadAll();
        MessageUtil.sendPrefixed(sender, "&aConfiguration et courses rechargees.");
    }

    // ================= COMMANDES ADMIN PAR COURSE =================

    private void handleRaceAction(CommandSender sender, String[] args) {
        RaceManager rm = plugin.getRaceManager();
        Race race = rm.getRace(args[0]);
        if (race == null) {
            sendHelp(sender);
            return;
        }
        if (args.length < 2) {
            MessageUtil.sendPrefixed(sender, "&cUsage : /cdb " + race.getName() + " <action>");
            return;
        }
        String action = args[1].toLowerCase();
        if (!RACE_ADMIN_ACTIONS.contains(action)) {
            MessageUtil.sendPrefixed(sender, "&cAction inconnue : " + action);
            return;
        }
        if (!action.equals("info") && !hasAdminPerm(sender)) {
            return;
        }

        RaceSession session = rm.getSession(race.getName());

        switch (action) {
            case "info" -> handleInfo(sender, race, session);
            case "setlobby" -> handleSetLobby(sender, race);
            case "addboatspawn" -> handleAddBoatSpawn(sender, race);
            case "removeboatspawn" -> handleRemoveBoatSpawn(sender, race, args);
            case "setcheckpoint" -> handleSetCheckpoint(sender, race, args);
            case "removecheckpoint" -> handleRemoveCheckpoint(sender, race, args);
            case "laps" -> handleIntSetting(sender, race, args, "laps");
            case "maxplayers" -> handleIntSetting(sender, race, args, "maxplayers");
            case "minplayers" -> handleIntSetting(sender, race, args, "minplayers");
            case "lobbycountdown" -> handleIntSetting(sender, race, args, "lobbycountdown");
            case "startcountdown" -> handleIntSetting(sender, race, args, "startcountdown");
            case "forcestart" -> handleForceStart(sender, race, session);
            case "stop" -> handleStop(sender, race, session);
        }
    }

    private void handleInfo(CommandSender sender, Race race, RaceSession session) {
        RaceManager rm = plugin.getRaceManager();
        MessageUtil.sendPrefixed(sender, "&b&lCourse " + race.getName());
        MessageUtil.send(sender, " &7Etat : &f" + session.getState());
        MessageUtil.send(sender, " &7Joueurs : &f" + session.getPlayerCount() + "/" + rm.resolveEffectiveMaxPlayers(race)
                + " &7(min " + rm.resolveMinPlayers(race) + ")");
        MessageUtil.send(sender, " &7Tours : &f" + race.getLaps());
        MessageUtil.send(sender, " &7Lobby : &f" + (race.getLobbySpawn() != null ? "OK" : "&cnon defini"));
        MessageUtil.send(sender, " &7Spawns bateau : &f" + race.getBoatSpawns().size());
        MessageUtil.send(sender, " &7Points de passage : &f" + race.getCheckpoints().size()
                + " &7(dont 1 ligne depart/arrivee)");
        MessageUtil.send(sender, " &7Compte a rebours lobby : &f" + rm.resolveLobbyCountdown(race) + "s");
        MessageUtil.send(sender, " &7Compte a rebours depart : &f" + rm.resolveStartCountdown(race) + "s");
        MessageUtil.send(sender, " &7Configuree et jouable : &f"
                + (race.isFullyConfigured(rm.resolveMinPlayers(race)) ? "&aoui" : "&cnon"));
    }

    private void handleSetLobby(CommandSender sender, Race race) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        race.setLobbySpawn(player.getLocation());
        plugin.getRaceManager().saveRace(race);
        MessageUtil.sendPrefixed(sender, "&aLobby de &e" + race.getName() + " &adefini a ta position.");
    }

    private void handleAddBoatSpawn(CommandSender sender, Race race) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        int index = race.addBoatSpawn(player.getLocation());
        plugin.getRaceManager().saveRace(race);
        MessageUtil.sendPrefixed(sender, "&aSpawn bateau &e#" + index + " &aajoute pour &e" + race.getName() + "&a.");
    }

    private void handleRemoveBoatSpawn(CommandSender sender, Race race, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendPrefixed(sender, "&cUsage : /cdb " + race.getName() + " removeboatspawn <index>");
            return;
        }
        Integer index = parseInt(sender, args[2]);
        if (index == null) return;
        boolean ok = race.removeBoatSpawn(index);
        if (ok) plugin.getRaceManager().saveRace(race);
        MessageUtil.sendPrefixed(sender, ok ? "&aSpawn bateau &e#" + index + " &asupprime." : "&cIndex invalide.");
    }

    private void handleSetCheckpoint(CommandSender sender, Race race, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        if (args.length < 4) {
            MessageUtil.sendPrefixed(sender, "&cUsage : /cdb " + race.getName() + " setcheckpoint <index> <pos1|pos2>");
            return;
        }
        Integer index = parseInt(sender, args[2]);
        if (index == null) return;
        String posArg = args[3].toLowerCase();
        String key = player.getUniqueId() + ":" + race.getName() + ":" + index;

        if (posArg.equals("pos1")) {
            pendingCorner1.put(key, player.getTargetBlockExact(5) != null
                    ? player.getTargetBlockExact(5).getLocation() : player.getLocation());
            MessageUtil.sendPrefixed(sender, "&aCoin 1 du checkpoint &e#" + index + " &aenregistre. Place-toi au "
                    + "coin oppose et fais &7/cdb " + race.getName() + " setcheckpoint " + index + " pos2");
        } else if (posArg.equals("pos2")) {
            Location corner1 = pendingCorner1.remove(key);
            if (corner1 == null) {
                MessageUtil.sendPrefixed(sender, "&cDefinis d'abord le coin 1 avec &e/cdb " + race.getName()
                        + " setcheckpoint " + index + " pos1");
                return;
            }
            Location targetLoc = player.getTargetBlockExact(5) != null
                    ? player.getTargetBlockExact(5).getLocation() : player.getLocation();
            if (corner1.getWorld() == null || !corner1.getWorld().equals(targetLoc.getWorld())) {
                MessageUtil.sendPrefixed(sender, "&cLes deux coins doivent etre dans le meme monde.");
                return;
            }
            boolean ok = race.setCheckpoint(index, new CuboidRegion(corner1, targetLoc));
            if (!ok) {
                MessageUtil.sendPrefixed(sender, "&cIndex invalide : defini d'abord le checkpoint &e#" + (index - 1) + "&c.");
                return;
            }
            plugin.getRaceManager().saveRace(race);
            String label = index == 1 ? " &7(ligne de depart/arrivee)" : "";
            MessageUtil.sendPrefixed(sender, "&aCheckpoint &e#" + index + " &adefini pour &e" + race.getName() + "&a." + label);
        } else {
            MessageUtil.sendPrefixed(sender, "&cUsage : /cdb " + race.getName() + " setcheckpoint <index> <pos1|pos2>");
        }
    }

    private void handleRemoveCheckpoint(CommandSender sender, Race race, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendPrefixed(sender, "&cUsage : /cdb " + race.getName() + " removecheckpoint <index>");
            return;
        }
        Integer index = parseInt(sender, args[2]);
        if (index == null) return;
        boolean ok = race.removeCheckpoint(index);
        if (ok) plugin.getRaceManager().saveRace(race);
        MessageUtil.sendPrefixed(sender, ok ? "&aCheckpoint &e#" + index + " &asupprime." : "&cIndex invalide.");
    }

    private void handleIntSetting(CommandSender sender, Race race, String[] args, String field) {
        if (args.length < 3) {
            MessageUtil.sendPrefixed(sender, "&cUsage : /cdb " + race.getName() + " " + field + " <nombre>");
            return;
        }
        Integer value = parseInt(sender, args[2]);
        if (value == null) return;

        switch (field) {
            case "laps" -> race.setLaps(value);
            case "maxplayers" -> race.setMaxPlayers(value);
            case "minplayers" -> race.setMinPlayers(value);
            case "lobbycountdown" -> race.setLobbyCountdownSeconds(value);
            case "startcountdown" -> race.setStartCountdownSeconds(value);
        }
        plugin.getRaceManager().saveRace(race);
        MessageUtil.sendPrefixed(sender, "&a" + field + " de &e" + race.getName() + " &adefini a &e" + value + "&a.");
    }

    private void handleForceStart(CommandSender sender, Race race, RaceSession session) {
        boolean ok = session.forceStart();
        MessageUtil.sendPrefixed(sender, ok ? "&aDepart force pour &e" + race.getName() + "&a."
                : "&cImpossible de forcer le depart (course non configuree, deja lancee ou vide).");
    }

    private void handleStop(CommandSender sender, Race race, RaceSession session) {
        session.forceStop();
        MessageUtil.sendPrefixed(sender, "&aCourse &e" + race.getName() + " &aarretee et reinitialisee.");
    }

    // ================= UTILITAIRES =================

    private boolean hasAdminPerm(CommandSender sender) {
        if (!sender.hasPermission("cdb.admin")) {
            MessageUtil.sendPrefixed(sender, "&cTu n'as pas la permission d'utiliser cette commande.");
            return false;
        }
        return true;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;
        MessageUtil.sendPrefixed(sender, "&cCette commande doit etre executee par un joueur.");
        return null;
    }

    private Integer parseInt(CommandSender sender, String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            MessageUtil.sendPrefixed(sender, "&cValeur numerique invalide : " + raw);
            return null;
        }
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.sendPrefixed(sender, "&b&lCourse de Bateau &7- Aide");
        MessageUtil.send(sender, " &e/cdb gui &7- Ouvre le menu des courses");
        MessageUtil.send(sender, " &e/cdb list &7- Liste les courses");
        MessageUtil.send(sender, " &e/cdb join <course> &7- Rejoindre une course");
        MessageUtil.send(sender, " &e/cdb leave &7- Quitter la course en cours");
        if (sender.hasPermission("cdb.admin")) {
            MessageUtil.send(sender, " &e/cdb create <course> &7- Creer une course");
            MessageUtil.send(sender, " &e/cdb delete <course> &7- Supprimer une course");
            MessageUtil.send(sender, " &e/cdb reload &7- Recharger la config");
            MessageUtil.send(sender, " &e/cdb <course> info &7- Infos de configuration");
            MessageUtil.send(sender, " &e/cdb <course> setlobby &7- Definir le lobby a ta position");
            MessageUtil.send(sender, " &e/cdb <course> addboatspawn &7- Ajouter un spawn bateau a ta position");
            MessageUtil.send(sender, " &e/cdb <course> removeboatspawn <i> &7- Retirer un spawn bateau");
            MessageUtil.send(sender, " &e/cdb <course> setcheckpoint <i> <pos1|pos2> &7- Definir un checkpoint (1 = depart/arrivee)");
            MessageUtil.send(sender, " &e/cdb <course> removecheckpoint <i> &7- Retirer un checkpoint");
            MessageUtil.send(sender, " &e/cdb <course> laps <n> &7- Nombre de tours");
            MessageUtil.send(sender, " &e/cdb <course> maxplayers <n> &7- Joueurs max");
            MessageUtil.send(sender, " &e/cdb <course> minplayers <n> &7- Joueurs min");
            MessageUtil.send(sender, " &e/cdb <course> lobbycountdown <s> &7- Duree du compte a rebours du lobby");
            MessageUtil.send(sender, " &e/cdb <course> startcountdown <s> &7- Duree du compte a rebours de depart");
            MessageUtil.send(sender, " &e/cdb <course> forcestart &7- Forcer le depart");
            MessageUtil.send(sender, " &e/cdb <course> stop &7- Arreter et reinitialiser la course");
        }
    }

    // ================= TAB COMPLETE =================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        RaceManager rm = plugin.getRaceManager();
        List<String> raceNames = rm.getRaces().stream().map(Race::getName).collect(Collectors.toList());

        if (args.length == 1) {
            List<String> options = new ArrayList<>(GLOBAL_SUBCOMMANDS);
            options.addAll(raceNames);
            return filter(options, args[0]);
        }

        if (args.length == 2) {
            String first = args[0].toLowerCase();
            if (first.equals("join") || first.equals("delete")) {
                return filter(raceNames, args[1]);
            }
            if (raceNames.stream().anyMatch(n -> n.equalsIgnoreCase(first))) {
                return filter(new ArrayList<>(RACE_ADMIN_ACTIONS), args[1]);
            }
            return List.of();
        }

        if (args.length == 3) {
            String action = args[1].toLowerCase();
            if (action.equals("setcheckpoint") || action.equals("removecheckpoint") || action.equals("removeboatspawn")
                    || action.equals("laps") || action.equals("maxplayers") || action.equals("minplayers")
                    || action.equals("lobbycountdown") || action.equals("startcountdown")) {
                return List.of("<nombre>");
            }
        }

        if (args.length == 4 && args[1].equalsIgnoreCase("setcheckpoint")) {
            return filter(List.of("pos1", "pos2"), args[3]);
        }

        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
