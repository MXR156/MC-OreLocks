# OreLocks

A Spigot/Paper plugin that locks ores behind permission nodes. A player can't mine a
locked ore until they have its node, for example `orelock.diamond`. Pair it with LuckPerms
and your store (Tebex, CraftingStore, an EssentialsX sign shop, etc.) to sell ore unlocks.

Works with any permissions plugin, since it uses Bukkit's standard `hasPermission`. LuckPerms
is recommended. There are no hard dependencies.

## Install
1. Build it (you only need a JDK, since Maven downloads itself):
   - Windows: `.\mvnw.cmd package`
   - macOS/Linux: `./mvnw package`

   Then put `target/OreLocks-1.0.0.jar` in `plugins/`.
2. Start the server. This creates `plugins/OreLocks/config.yml`.
3. Edit the locks you want, then run `/orelocks reload`.

Supports Minecraft 1.20 through 26.x (Spigot, Paper, Purpur). The jar targets Java 17, so it
runs on whatever Java your server needs (26.x servers need Java 25). To compile against a
newer API: `mvn package -Dspigot.version=26.3-R0.1-SNAPSHOT`.

## Default permission nodes
| Node | Unlocks |
|---|---|
| `orelock.coal` | Coal ore *(lock disabled by default)* |
| `orelock.copper` | Copper ore *(lock disabled by default)* |
| `orelock.iron` | Iron ore |
| `orelock.gold` | Gold ore and Nether gold ore |
| `orelock.redstone` | Redstone ore |
| `orelock.lapis` | Lapis ore |
| `orelock.diamond` | Diamond ore |
| `orelock.emerald` | Emerald ore |
| `orelock.quartz` | Nether quartz ore |
| `orelock.netherite` | Ancient debris |
| `orelock.*` | Every ore |
| `orelock.bypass` | Ignore all locks |
| `orelocks.admin` | `/orelocks reload` and `/orelocks check <player>` (op by default) |

Deepslate variants are included in each lock. You can add, remove, or rename locks and
nodes freely in `config.yml`.

Lock nodes default to **false for everyone, including ops** (unless `ops-bypass: true`).

## Granting unlocks (LuckPerms)
```
/lp user Steve permission set orelock.diamond true
/lp group vip permission set orelock.* true
```
From a Tebex package, run the same command with `{username}` in place of the player name.

## Commands
- `/orelocks` or `/orelocks list`: shows which ores you've unlocked
- `/orelocks check <player>`: shows another online player's unlocks (admin)
- `/orelocks reload`: reloads the config (admin)

## Behaviour
- Breaking a locked ore without the node is cancelled, and the player gets a message
  (rate-limited, optionally shown on the action bar).
- Creative-mode players aren't blocked (`ignore-creative`).
- Explosions (TNT, creepers, beds) don't destroy locked ores (`protect-from-explosions`),
  so players can't blow them up to get around the lock.
