package com.mxr156.orelocks;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class OreLocksCommand implements TabExecutor {

    private static final String ADMIN_PERMISSION = "orelocks.admin";

    private final OreLocksPlugin plugin;

    public OreLocksCommand(OreLocksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "list" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.message("usage"));
                    return true;
                }
                sendList(sender, player, false);
            }
            case "check" -> {
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage(plugin.message("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.message("usage"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(plugin.message("player-not-found"));
                    return true;
                }
                sendList(sender, target, true);
            }
            case "reload" -> {
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage(plugin.message("no-permission"));
                    return true;
                }
                plugin.loadSettings();
                sender.sendMessage(plugin.message("reloaded"));
            }
            default -> sender.sendMessage(plugin.message("usage"));
        }
        return true;
    }

    private void sendList(CommandSender sender, Player target, boolean other) {
        sender.sendMessage(other
                ? plugin.message("list-header-other", "player", target.getName())
                : plugin.message("list-header"));
        for (OreLock lock : plugin.getLocks()) {
            String key = plugin.hasUnlocked(target, lock) ? "list-unlocked" : "list-locked";
            sender.sendMessage(plugin.rawMessage(key,
                    "ore", OreLocksPlugin.color(lock.displayName()),
                    "permission", lock.permission()));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("list");
            if (sender.hasPermission(ADMIN_PERMISSION)) {
                options.add("check");
                options.add("reload");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("check") && sender.hasPermission(ADMIN_PERMISSION)) {
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        } else {
            return Collections.emptyList();
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(o -> !o.toLowerCase(Locale.ROOT).startsWith(prefix));
        return options;
    }
}
