package com.mxr156.orelocks;

import org.bukkit.Material;

import java.util.Set;

/** One configured lock: a set of blocks guarded by a single permission node. */
public record OreLock(String id, String displayName, String permission, Set<Material> blocks) {
}
