package com.herocraft.coursedebateau;

import com.herocraft.coursedebateau.commands.CDBCommand;
import com.herocraft.coursedebateau.gui.RaceGUIListener;
import com.herocraft.coursedebateau.listeners.RaceProtectionListener;
import com.herocraft.coursedebateau.race.RaceManager;
import com.herocraft.coursedebateau.util.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class CourseDeBateauPlugin extends JavaPlugin {

    private RaceManager raceManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        MessageUtil.setPrefix(getConfig().getString("messages.prefix", "&b&lCDB &8» &r"));

        this.raceManager = new RaceManager(this);
        raceManager.loadAll();

        CDBCommand cdbCommand = new CDBCommand(this);
        var command = getCommand("cdb");
        if (command != null) {
            command.setExecutor(cdbCommand);
            command.setTabCompleter(cdbCommand);
        }

        getServer().getPluginManager().registerEvents(new RaceGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new RaceProtectionListener(this), this);

        getLogger().info("CourseDeBateau active - " + raceManager.getRaces().size() + " course(s) chargee(s).");
    }

    @Override
    public void onDisable() {
        if (raceManager != null) {
            for (var session : raceManager.getSessions()) {
                session.forceStop();
            }
        }
    }

    public RaceManager getRaceManager() {
        return raceManager;
    }
}
