package com.herocraft.coursedebateau.gui;

import com.herocraft.coursedebateau.CourseDeBateauPlugin;
import com.herocraft.coursedebateau.race.Race;
import com.herocraft.coursedebateau.race.RaceManager;
import com.herocraft.coursedebateau.race.RaceSession;
import com.herocraft.coursedebateau.race.RaceState;
import com.herocraft.coursedebateau.util.MessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI d'inventaire listant toutes les courses de bateau configurees, une icone par
 * course, avec son etat et son nombre de joueurs. Clic pour rejoindre / quitter.
 */
public class RaceGUI {

    public static final String GUI_TITLE = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "\u26F5 Courses de bateau";
    private static final int GUI_SIZE = 54;

    private final CourseDeBateauPlugin plugin;

    public RaceGUI(CourseDeBateauPlugin plugin) {
        this.plugin = plugin;
    }

    public Inventory build(Player viewer) {
        Inventory inv = org.bukkit.Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        RaceManager raceManager = plugin.getRaceManager();

        int slot = 0;
        for (Race race : raceManager.getRaces()) {
            if (slot >= GUI_SIZE) break;
            inv.setItem(slot, buildIcon(viewer, race));
            slot++;
        }
        return inv;
    }

    private ItemStack buildIcon(Player viewer, Race race) {
        RaceManager raceManager = plugin.getRaceManager();
        RaceSession session = raceManager.getSession(race.getName());
        RaceState state = session.getState();
        int min = raceManager.resolveMinPlayers(race);
        int max = raceManager.resolveEffectiveMaxPlayers(race);
        boolean configured = race.isFullyConfigured(min);
        boolean playerInIt = session.hasPlayer(viewer.getUniqueId());

        Material material;
        String stateLabel;
        if (!configured) {
            material = Material.BARRIER;
            stateLabel = "&cNon configuree";
        } else switch (state) {
            case WAITING -> {
                material = Material.OAK_BOAT;
                stateLabel = "&aEn attente de joueurs";
            }
            case LOBBY_COUNTDOWN -> {
                material = Material.CLOCK;
                stateLabel = "&eDepart imminent";
            }
            case STARTING -> {
                material = Material.CLOCK;
                stateLabel = "&6Depart dans quelques secondes";
            }
            case RUNNING -> {
                material = Material.REDSTONE_BLOCK;
                stateLabel = "&cCourse en cours";
            }
            default -> {
                material = Material.NAME_TAG;
                stateLabel = "&7Resultats en cours d'affichage";
            }
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.format("&b&l" + race.getName()));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.format(stateLabel));
        lore.add(MessageUtil.format("&7Joueurs : &f" + session.getPlayerCount() + "/" + max + " &7(min " + min + ")"));
        lore.add(MessageUtil.format("&7Tours : &f" + race.getLaps()));
        lore.add("");
        if (playerInIt) {
            lore.add(MessageUtil.format("&c&lClique pour quitter"));
        } else if (!configured) {
            lore.add(MessageUtil.format("&7Cette course n'est pas encore prete"));
        } else if (state == RaceState.RUNNING || state == RaceState.STARTING) {
            lore.add(MessageUtil.format("&7Course deja en cours, patiente..."));
        } else if (session.getPlayerCount() >= max) {
            lore.add(MessageUtil.format("&7Course complete"));
        } else {
            lore.add(MessageUtil.format("&a&lClique pour rejoindre"));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
