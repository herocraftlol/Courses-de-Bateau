package com.herocraft.coursedebateau.race;

import com.herocraft.coursedebateau.util.MessageUtil;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gere un sidebar (scoreboard a droite) par joueur pendant une course : tour en
 * cours, temps du tour, temps total, record personnel et record global. Chaque
 * joueur recoit un Scoreboard prive (independant du scoreboard serveur) pendant la
 * course, restaure a la fin (via {@link #remove(Player)}).
 */
public class RaceScoreboard {

    private static final String OBJECTIVE_NAME = "cdb_timer";
    private static final int MAX_LINES = 9;

    private final Map<UUID, Scoreboard> previousScoreboards = new HashMap<>();

    /** Cree et assigne un nouveau scoreboard prive au joueur pour la duree de la course. */
    public void assign(Player player) {
        if (previousScoreboards.containsKey(player.getUniqueId())) {
            return; // deja assigne
        }
        previousScoreboards.put(player.getUniqueId(), player.getScoreboard());

        ScoreboardManager manager = org.bukkit.Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective(OBJECTIVE_NAME, "dummy",
                MessageUtil.format("&b&l\u26F5 COURSE DE BATEAU"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(board);
    }

    /**
     * Rafraichit le contenu du sidebar avec l'etat courant de la course pour ce joueur.
     */
    public void update(Player player, Race race, RaceSession session, PlayerRaceData data,
                        RecordManager recordManager, long raceStartTimeMillis) {
        Scoreboard board = player.getScoreboard();
        Objective objective = board.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            return; // pas (ou plus) assigne
        }

        // On efface les anciennes lignes avant de reecrire (les entrees sont uniques par ligne).
        for (String entry : new java.util.ArrayList<>(board.getEntries())) {
            board.resetScores(entry);
        }

        int laps = race.getLaps();
        String lapLabel = data.isFinished() ? "&aTerminee !" : "&e" + (data.getLapsCompleted() + 1) + "&7/&e" + laps;
        String lapTime = data.isFinished() ? "&7-" : "&b" + formatMillis(data.getCurrentLapMillis());
        String totalTime = "&b" + formatMillis(data.getCurrentTotalMillis(raceStartTimeMillis));

        long personalMillis = recordManager.getPersonalRecordMillis(race.getName(), player.getUniqueId());
        long globalMillis = recordManager.getGlobalRecordMillis(race.getName());
        String globalHolder = recordManager.getGlobalRecordHolderName(race.getName());

        String personalLine = "&a" + RecordManager.format(personalMillis);
        String globalLine = "&6" + RecordManager.format(globalMillis)
                + (globalHolder != null ? " &7(" + globalHolder + ")" : "");

        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("&7Course : &f" + race.getName());
        lines.add("&7Tour : " + lapLabel);
        lines.add("&7Temps tour : " + lapTime);
        lines.add("&7Temps total : " + totalTime);
        lines.add(" ");
        lines.add("&7Record perso : " + personalLine);
        lines.add("&7Record serveur : " + globalLine);

        int score = lines.size();
        for (int i = 0; i < lines.size() && i < MAX_LINES; i++) {
            // Chaque entree doit etre unique : on complete avec des codes couleur invisibles.
            String entry = MessageUtil.format(lines.get(i)) + uniqueSuffix(i);
            var scoreObj = objective.getScore(entry);
            scoreObj.setScore(score);
            try {
                scoreObj.numberFormat(NumberFormat.blank());
            } catch (Throwable ignored) {
                // API indisponible sur certaines versions : le numero restera visible, pas bloquant.
            }
            score--;
        }
    }

    private String uniqueSuffix(int index) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < index + 1; i++) {
            sb.append(org.bukkit.ChatColor.RESET);
        }
        return sb.toString();
    }

    private String formatMillis(long millis) {
        long totalSeconds = Math.max(0, millis) / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /** Restaure le scoreboard d'origine du joueur (appele a la sortie de la course). */
    public void remove(Player player) {
        Scoreboard previous = previousScoreboards.remove(player.getUniqueId());
        if (previous != null) {
            player.setScoreboard(previous);
        } else {
            player.setScoreboard(org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }
}
