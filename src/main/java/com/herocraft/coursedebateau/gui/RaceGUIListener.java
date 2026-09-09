package com.herocraft.coursedebateau.gui;

import com.herocraft.coursedebateau.CourseDeBateauPlugin;
import com.herocraft.coursedebateau.race.Race;
import com.herocraft.coursedebateau.race.RaceManager;
import com.herocraft.coursedebateau.race.RaceSession;
import com.herocraft.coursedebateau.util.MessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RaceGUIListener implements Listener {

    private final CourseDeBateauPlugin plugin;

    public RaceGUIListener(CourseDeBateauPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!ChatColor.stripColor(event.getView().getTitle())
                .equals(ChatColor.stripColor(RaceGUI.GUI_TITLE))) {
            return;
        }
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getDisplayName() == null || meta.getDisplayName().isEmpty()) {
            return;
        }
        String raceName = ChatColor.stripColor(meta.getDisplayName());

        Player player = (Player) event.getWhoClicked();
        RaceManager raceManager = plugin.getRaceManager();
        Race race = raceManager.getRace(raceName);
        if (race == null) {
            return;
        }
        RaceSession session = raceManager.getSession(raceName);

        if (session.hasPlayer(player.getUniqueId())) {
            session.leave(player);
            player.closeInventory();
            return;
        }

        RaceSession.JoinResult result = session.join(player);
        switch (result) {
            case SUCCESS -> player.closeInventory();
            case NOT_CONFIGURED -> MessageUtil.sendPrefixed(player, "&cCette course n'est pas encore configuree.");
            case RACE_FULL -> MessageUtil.sendPrefixed(player, "&cCette course est complete.");
            case RACE_IN_PROGRESS -> MessageUtil.sendPrefixed(player, "&cCette course est deja en cours.");
            case ALREADY_IN_ANOTHER_RACE -> MessageUtil.sendPrefixed(player, "&cTu es deja inscrit a une autre course.");
            case ALREADY_IN_THIS_RACE -> { /* ne devrait pas arriver, gere plus haut */ }
        }
    }
}
