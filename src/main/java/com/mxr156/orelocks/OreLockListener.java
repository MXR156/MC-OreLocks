package com.mxr156.orelocks;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OreLockListener implements Listener {

    private final OreLocksPlugin plugin;
    private final Map<UUID, Long> lastMessage = new HashMap<>();

    public OreLockListener(OreLocksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        OreLock lock = plugin.getLock(event.getBlock().getType());
        if (lock == null) {
            return;
        }

        Player player = event.getPlayer();
        if (plugin.isExempt(player) || plugin.hasUnlocked(player, lock)) {
            return;
        }

        event.setCancelled(true);
        sendLockedMessage(player, lock);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (plugin.isProtectFromExplosions()) {
            stripLockedBlocks(event.blockList());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (plugin.isProtectFromExplosions()) {
            stripLockedBlocks(event.blockList());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastMessage.remove(event.getPlayer().getUniqueId());
    }

    private void stripLockedBlocks(List<Block> blocks) {
        blocks.removeIf(block -> plugin.getLock(block.getType()) != null);
    }

    private void sendLockedMessage(Player player, OreLock lock) {
        long now = System.currentTimeMillis();
        Long last = lastMessage.get(player.getUniqueId());
        if (last != null && now - last < plugin.getMessageCooldownMillis()) {
            return;
        }
        lastMessage.put(player.getUniqueId(), now);

        String text = plugin.message("locked",
                "ore", OreLocksPlugin.color(lock.displayName()),
                "permission", lock.permission());
        if (plugin.isUseActionBar()) {
            try {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(text));
                return;
            } catch (LinkageError | UnsupportedOperationException e) {
                // BungeeCord chat API unavailable on this server; fall back to chat.
            }
        }
        player.sendMessage(text);
    }
}
