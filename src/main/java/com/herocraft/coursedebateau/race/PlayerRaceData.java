package com.herocraft.coursedebateau.race;

import org.bukkit.GameMode;
import org.bukkit.entity.Boat;

import java.util.UUID;

/**
 * Progression d'un joueur pendant une course en cours : bateau associe, prochain
 * checkpoint attendu (empeche de tricher en passant les points dans le desordre,
 * ou en faisant l'aller-retour sur la ligne de depart), nombre de tours valides,
 * et chronometrage (temps du tour en cours + temps total de la course).
 */
public class PlayerRaceData {

    private final UUID playerId;
    private Boat boat;

    /**
     * Index (0-based) du prochain checkpoint intermediaire que le joueur doit
     * atteindre. Une fois tous les checkpoints valides, il ne reste plus qu'a
     * retraverser la startZone (voir waitingForFinishLine) pour valider le tour.
     */
    private int nextCheckpointIndex = 0;

    /** True une fois tous les checkpoints d'un tour valides : il ne reste plus qu'a
     *  retraverser la startZone pour valider le tour. */
    private boolean waitingForFinishLine = false;

    private int lapsCompleted = 0;
    private boolean finished = false;
    private int finishRank = -1;

    // ---- Chronometrage ----
    private long lapStartTimeMillis;
    private long totalMillisAtFinish = -1;

    // ---- Mode spectateur (apres la ligne d'arrivee finale) ----
    private boolean spectating = false;
    private GameMode previousGameMode;

    public PlayerRaceData(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Boat getBoat() {
        return boat;
    }

    public void setBoat(Boat boat) {
        this.boat = boat;
    }

    public int getNextCheckpointIndex() {
        return nextCheckpointIndex;
    }

    public boolean isWaitingForFinishLine() {
        return waitingForFinishLine;
    }

    /** A appeler quand la course RUNNING demarre pour ce joueur : remet toute la progression a zero. */
    public void resetProgress(long raceStartTimeMillis) {
        nextCheckpointIndex = 0;
        waitingForFinishLine = false;
        lapsCompleted = 0;
        finished = false;
        finishRank = -1;
        totalMillisAtFinish = -1;
        lapStartTimeMillis = raceStartTimeMillis;
        spectating = false;
    }

    /**
     * Avance la progression d'un cran apres avoir valide le checkpoint attendu.
     * A appeler uniquement quand le checkpoint atteint correspond bien a celui
     * attendu (voir RaceSession).
     */
    public void advanceCheckpoint(int totalCheckpoints) {
        nextCheckpointIndex++;
        if (nextCheckpointIndex >= totalCheckpoints) {
            waitingForFinishLine = true;
        }
    }

    /** Valide un tour complet (le joueur vient de retraverser la startZone). */
    public void completeLap(long nowMillis) {
        lapsCompleted++;
        waitingForFinishLine = false;
        nextCheckpointIndex = 0;
        lapStartTimeMillis = nowMillis;
    }

    public int getLapsCompleted() {
        return lapsCompleted;
    }

    public boolean isFinished() {
        return finished;
    }

    public void markFinished(int rank, long totalMillis) {
        this.finished = true;
        this.finishRank = rank;
        this.totalMillisAtFinish = totalMillis;
    }

    public int getFinishRank() {
        return finishRank;
    }

    /** Temps total actuel de la course pour ce joueur (fige une fois arrive). */
    public long getCurrentTotalMillis(long raceStartTimeMillis) {
        if (finished) return totalMillisAtFinish;
        return System.currentTimeMillis() - raceStartTimeMillis;
    }

    /** Temps du tour en cours (fige une fois arrive). */
    public long getCurrentLapMillis() {
        if (finished) return 0;
        return System.currentTimeMillis() - lapStartTimeMillis;
    }

    // ---- Spectateur ----

    public boolean isSpectating() {
        return spectating;
    }

    public void enterSpectatorMode(GameMode previousMode) {
        this.spectating = true;
        this.previousGameMode = previousMode;
    }

    public GameMode getPreviousGameMode() {
        return previousGameMode;
    }

    public void exitSpectatorMode() {
        this.spectating = false;
    }
}
