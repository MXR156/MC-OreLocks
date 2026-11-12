package com.mxr156.orelocks;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OreLocksPlugin extends JavaPlugin {

    public static final String BYPASS_PERMISSION = "orelock.bypass";
    public static final String WILDCARD_PERMISSION = "orelock.*";

    private final Map<String, OreLock> locksById = new LinkedHashMap<>();
    private final Map<Material, OreLock> locksByMaterial = new EnumMap<>(Material.class);
    private final List<String> registeredPermissions = new ArrayList<>();

    private boolean opsBypass;
    private boolean ignoreCreative;
    private boolean protectFromExplosions;
    private boolean useActionBar;
    private long messageCooldownMillis;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        getServer().getPluginManager().registerEvents(new OreLockListener(this), this);

        PluginCommand command = getCommand("orelocks");
        if (command != null) {
            OreLocksCommand executor = new OreLocksCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("Loaded " + locksById.size() + " ore lock(s).");
    }

    @Override
    public void onDisable() {
        unregisterPermissions();
    }

    /** (Re)reads config.yml and rebuilds the lock tables and permission registrations. */
    public void loadSettings() {
        reloadConfig();
        FileConfiguration config = getConfig();

        opsBypass = config.getBoolean("ops-bypass", false);
        ignoreCreative = config.getBoolean("ignore-creative", true);
        protectFromExplosions = config.getBoolean("protect-from-explosions", true);
        useActionBar = config.getBoolean("use-action-bar", false);
        messageCooldownMillis = Math.max(0L, (long) (config.getDouble("message-cooldown-seconds", 2) * 1000));

        locksById.clear();
        locksByMaterial.clear();

        ConfigurationSection locksSection = config.getConfigurationSection("locks");
        if (locksSection != null) {
            for (String id : locksSection.getKeys(false)) {
                ConfigurationSection section = locksSection.getConfigurationSection(id);
                if (section == null || !section.getBoolean("enabled", true)) {
                    continue;
                }

                String permission = section.getString("permission", "orelock." + id).toLowerCase();
                String displayName = section.getString("display-name", id);

                Set<Material> blocks = EnumSet.noneOf(Material.class);
                for (String name : section.getStringList("blocks")) {
                    Material material = Material.matchMaterial(name);
                    if (material == null || !material.isBlock()) {
                        getLogger().warning("Lock '" + id + "': unknown block '" + name + "', skipping.");
                        continue;
                    }
                    OreLock existing = locksByMaterial.get(material);
                    if (existing != null) {
                        getLogger().warning("Lock '" + id + "': " + material + " is already locked by '"
                                + existing.id() + "', skipping.");
                        continue;
                    }
                    blocks.add(material);
                }

                if (blocks.isEmpty()) {
                    getLogger().warning("Lock '" + id + "' has no valid blocks, skipping.");
                    continue;
                }

                OreLock lock = new OreLock(id, displayName, permission, Collections.unmodifiableSet(blocks));
                locksById.put(id, lock);
                for (Material material : blocks) {
                    locksByMaterial.put(material, lock);
                }
            }
        }

        registerPermissions();
    }

    /**
     * Registers every lock node with a default of FALSE so that, without a permissions
     * plugin granting them, nobody (not even ops) has them. Also builds orelock.* as a
     * parent of all nodes for permission plugins that don't do wildcard expansion.
     */
    private void registerPermissions() {
        unregisterPermissions();
        PluginManager pm = getServer().getPluginManager();

        Map<String, Boolean> children = new LinkedHashMap<>();
        for (OreLock lock : locksById.values()) {
            if (children.containsKey(lock.permission())) {
                continue; // two locks may share one node
            }
            children.put(lock.permission(), true);
            if (pm.getPermission(lock.permission()) == null) {
                pm.addPermission(new Permission(lock.permission(),
                        "Allows mining " + ChatColor.stripColor(color(lock.displayName())),
                        PermissionDefault.FALSE));
                registeredPermissions.add(lock.permission());
            }
        }

        if (pm.getPermission(WILDCARD_PERMISSION) == null) {
            pm.addPermission(new Permission(WILDCARD_PERMISSION, "Unlocks every ore",
                    PermissionDefault.FALSE, children));
            registeredPermissions.add(WILDCARD_PERMISSION);
        }
    }

    private void unregisterPermissions() {
        PluginManager pm = getServer().getPluginManager();
        for (String node : registeredPermissions) {
            pm.removePermission(node);
        }
        registeredPermissions.clear();
    }

    /** The lock guarding this block type, or null if it is not locked. */
    public OreLock getLock(Material material) {
        return locksByMaterial.get(material);
    }

    public Collection<OreLock> getLocks() {
        return Collections.unmodifiableCollection(locksById.values());
    }

    /** Whether the player is allowed to mine blocks covered by this lock. */
    public boolean hasUnlocked(Player player, OreLock lock) {
        if (opsBypass && player.isOp()) {
            return true;
        }
        return player.hasPermission(BYPASS_PERMISSION) || player.hasPermission(lock.permission());
    }

    /** Whether the lock check should be skipped entirely for this player right now. */
    public boolean isExempt(Player player) {
        return ignoreCreative && player.getGameMode() == GameMode.CREATIVE;
    }

    public boolean isProtectFromExplosions() {
        return protectFromExplosions;
    }

    public boolean isUseActionBar() {
        return useActionBar;
    }

    public long getMessageCooldownMillis() {
        return messageCooldownMillis;
    }

    /** A configured message with the prefix applied and colour codes translated. */
    public String message(String key, String... placeholders) {
        String raw = getConfig().getString("messages." + key, "");
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return color(getConfig().getString("messages.prefix", "") + raw);
    }

    /** A configured message without the prefix (for list lines). */
    public String rawMessage(String key, String... placeholders) {
        String raw = getConfig().getString("messages." + key, "");
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return color(raw);
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
