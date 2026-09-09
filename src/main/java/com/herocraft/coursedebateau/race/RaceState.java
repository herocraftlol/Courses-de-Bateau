package com.herocraft.coursedebateau.race;

/**
 * Etats possibles du cycle de vie d'une course de bateau.
 */
public enum RaceState {
    /** La course n'est pas encore entierement configuree par un admin (points manquants). */
    NOT_CONFIGURED,
    /** En attente de joueurs dans le lobby (sous le minimum requis). */
    WAITING,
    /** Le minimum de joueurs est atteint : compte a rebours du lobby en cours. */
    LOBBY_COUNTDOWN,
    /** Joueurs deja teleportes dans leur bateau, geles, en attente du top depart. */
    STARTING,
    /** Course en cours : les joueurs peuvent naviguer et passer les points de passage. */
    RUNNING,
    /** Course terminee : affichage du classement final avant reinitialisation. */
    ENDING
}
