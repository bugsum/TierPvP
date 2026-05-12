package xyz.bugsum.tierpvp.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.bugsum.tierpvp.manager.ArenaManager;
import xyz.bugsum.tierpvp.module.Arena;
import xyz.bugsum.tierpvp.util.ArenaSpawn;

import java.io.File;
import java.io.IOException;

public class ArenaConfig {
    public static void load(File file, ArenaManager manager) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection arenasSection = yaml.getConfigurationSection("arenas");

        if (arenasSection == null) return;

        for (String name : arenasSection.getKeys(false)) {
            ConfigurationSection section = arenasSection.getConfigurationSection(name);
            if (section == null) continue;

            Location spawn = parseLocation(section.getConfigurationSection("spawn"));
            ArenaSpawn zone = parseArenaSpawn(section, spawn);
            int arenaVoid = section.getInt("kill-below-y", 0);
            boolean waterKills = section.getBoolean("water-kills", true);

            Arena arena = new Arena(name, spawn, zone, arenaVoid);
            manager.addArena(arena);
        }
    }

    public static void save(File file, @NotNull ArenaManager manager) {
        YamlConfiguration yaml = new YamlConfiguration();

        for (Arena arena : manager.getAllArenas()) {
            String base = "arenas." + arena.getName();

            if (arena.getSpawn() != null) {
                Location s = arena.getSpawn();
                yaml.set(base + ".spawn.world", s.getWorld().getName());
                yaml.set(base + ".spawn.x", s.getX());
                yaml.set(base + ".spawn.y", s.getY());
                yaml.set(base + ".spawn.z", s.getZ());
                yaml.set(base + ".spawn.yaw", (double) s.getYaw());
                yaml.set(base + ".spawn.pitch", (double) s.getPitch());
            }

            if (arena.getArenaSpawn() != null) {
                ArenaSpawn z = arena.getArenaSpawn();
                yaml.set(base + ".spawn-zone.pos1.x", z.loc1().getBlockX());
                yaml.set(base + ".spawn-zone.pos1.y", z.loc1().getBlockY());
                yaml.set(base + ".spawn-zone.pos1.z", z.loc1().getBlockZ());
                yaml.set(base + ".spawn-zone.pos2.x", z.loc2().getBlockX());
                yaml.set(base + ".spawn-zone.pos2.y", z.loc2().getBlockY());
                yaml.set(base + ".spawn-zone.pos2.z", z.loc2().getBlockZ());
                yaml.set(base + ".spawn-zone.world", z.loc1().getWorld().getName());
            }

            yaml.set(base + ".kill-below-y", arena.getArenaVoid());
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Location parseLocation(ConfigurationSection section) {
        if (section == null) return null;

        World world = Bukkit.getWorld(section.getString("world", "world"));
        if (world == null) return null;

        return new Location(
                world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw", 0),
                (float) section.getDouble("pitch", 0)
        );
    }

    private static @Nullable ArenaSpawn parseArenaSpawn(@NotNull ConfigurationSection arenaSection, Location spawnForWorld) {
        ConfigurationSection zoneSection = arenaSection.getConfigurationSection("spawn-zone");
        if (zoneSection == null) return null;

        String worldName = zoneSection.getString("world", "world");
        World world = Bukkit.getWorld(worldName);

        if (world == null && spawnForWorld != null) world = spawnForWorld.getWorld();
        if (world == null) return null;

        ConfigurationSection p1 = zoneSection.getConfigurationSection("pos1");
        ConfigurationSection p2 = zoneSection.getConfigurationSection("pos2");
        if (p1 == null || p2 == null) return null;

        Location pos1 = new Location(world, p1.getInt("x"), p1.getInt("y"), p1.getInt("z"));
        Location pos2 = new Location(world, p2.getInt("x"), p2.getInt("y"), p2.getInt("z"));

        return new ArenaSpawn(pos1, pos2);
    }
}
