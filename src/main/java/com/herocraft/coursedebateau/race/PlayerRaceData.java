package com.herocraft.coursedebateau.race;

import org.bukkit.entity.Boat;

import java.util.UUID;

/**
 * Progression d'un joueur pendant une course en cours : bateau associe, prochain
 * checkpoint attendu (empeche de tricher en passant les points dans le desordre,
 * ou en faisant l'aller-retour sur la ligne de depart) et nombre de tours valides.
 */
public class PlayerRaceData {

    private final UUID playerId;
    private Boat boat;

    /**
     * Index (0-based) du prochain checkpoint que le joueur doit atteindre. La course
     * commence sur le checkpoint 0 (ligne de depart/arrivee) : nextCheckpointIndex
     * demarre donc a 1. Une fois le dernier checkpoint atteint, il repasse a 0 pour
     * indiquer qu'il ne reste plus qu'a retraverser la ligne d'arrivee pour valider
     * le tour (voir waitingForFinishLine).
     */
    private int nextCheckpointIndex = 1;

    /** True une fois tous les checkpoints intermediaires d'un tour valides : il ne reste
     *  qu'a retraverser le checkpoint 0 (ligne d'arrivee) pour valider le tour. */
    private boolean waitingForFinishLine = false;

    private int lapsCompleted = 0;
    private boolean finished = false;
    private int finishRank = -1;
    private long startTimeMillis;
    private long finishTimeMillis = -1;

    public PlayerRaceData(UUID playerId) {
        this.playerId = playerId;
        this.startTimeMillis = System.currentTimeMillis();
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

    /**
     * Avance la progression d'un cran apres avoir valide le checkpoint attendu.
     * A appeler uniquement quand le checkpoint atteint correspond bien a celui
     * attendu (voir RaceSession).
     */
    public void advanceCheckpoint(int totalCheckpoints) {
        nextCheckpointIndex++;
        if (nextCheckpointIndex >= totalCheckpoints) {
            waitingForFinishLine = true;
            nextCheckpointIndex = 0;
        }
    }

    /** Valide un tour complet (le joueur vient de retraverser la ligne d'arrivee). */
    public void completeLap() {
        lapsCompleted++;
        waitingForFinishLine = false;
        nextCheckpointIndex = 1;
    }

    public int getLapsCompleted() {
        return lapsCompleted;
    }

    public boolean isFinished() {
        return finished;
    }

    public void markFinished(int rank) {
        this.finished = true;
        this.finishRank = rank;
        this.finishTimeMillis = System.currentTimeMillis();
    }

    public int getFinishRank() {
        return finishRank;
    }

    public long getElapsedMillis() {
        long end = finishTimeMillis > 0 ? finishTimeMillis : System.currentTimeMillis();
        return end - startTimeMillis;
    }
}
