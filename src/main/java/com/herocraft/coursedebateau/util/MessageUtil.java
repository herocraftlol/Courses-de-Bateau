package com.herocraft.coursedebateau.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Petite classe utilitaire pour traduire les codes couleur (&) et envoyer des
 * messages formates, avec ou sans le prefixe du plugin.
 */
public final class MessageUtil {

    private static String prefix = "&b&lCDB &8» &r";

    private MessageUtil() {
    }

    public static void setPrefix(String rawPrefix) {
        prefix = rawPrefix == null ? "" : rawPrefix;
    }

    public static String format(String raw) {
        if (raw == null) return "";
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    /** Envoie un message brut (sans prefixe), avec traduction des couleurs. */
    public static void send(CommandSender target, String raw) {
        if (raw == null || raw.isEmpty()) return;
        target.sendMessage(format(raw));
    }

    /** Envoie un message precede du prefixe du plugin. */
    public static void sendPrefixed(CommandSender target, String raw) {
        if (raw == null || raw.isEmpty()) return;
        target.sendMessage(format(prefix + raw));
    }
}
