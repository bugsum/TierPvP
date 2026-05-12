package xyz.bugsum.tierpvp.command.impl;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.bugsum.tierpvp.command.SubCommand;
import xyz.bugsum.tierpvp.listener.ArenaSelectionListener;
import xyz.bugsum.tierpvp.manager.ArenaManager;
import xyz.bugsum.tierpvp.manager.ConfigManager;
import xyz.bugsum.tierpvp.module.Arena;
import xyz.bugsum.tierpvp.util.MessageUtil;

import java.util.List;
import java.util.stream.Stream;

public class ArenaCommand implements SubCommand {
    private final ArenaManager arenaManager;
    private final ConfigManager configManager;
    private final ArenaSelectionListener arenaSelectionListener;
    private final MessageUtil message;

    public ArenaCommand(ArenaManager arenaManager, ConfigManager configManager, ArenaSelectionListener arenaSelectionListener, MessageUtil message) {
        this.arenaManager = arenaManager;
        this.configManager = configManager;
        this.arenaSelectionListener = arenaSelectionListener;
        this.message = message;
    }

    @Override
    public boolean execute(CommandSender sender, String @NotNull [] args) {
        if (args.length == 0) {
            sender.sendMessage(message.parse("<yellow>Arena commands: create, delete, setspawn, setzone, confirmzone, cancelzone, setvoid, list, info"));
            return true;
        }

        String sub = args[0].toLowerCase();
        return switch (sub) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "setspawn" -> handleSetSpawn(sender, args);
            case "setzone" -> handleSetZone(sender, args);
            case "confirmzone" -> handleConfirmZone(sender);
            case "cancelzone" -> handleCancelZone(sender);
            case "setvoid" -> handleSetVoid(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            default -> {
                sender.sendMessage(message.parse("<red>Unknown arena command: " + sub));
                yield true;
            }
        };
    }

    private boolean handleCreate(CommandSender sender, String @NotNull [] args) {
        if (args.length < 2) {
            sender.sendMessage(message.parse("<red>Usage: /tierpvp arena create <name>"));
            return true;
        }
        String name = args[1];
        if (arenaManager.hasArena(name)) {
            message.send((Player) sender, "arena.exists", "arena", name);
            return true;
        }
        arenaManager.addArena(new Arena(name));
        configManager.saveArenas(arenaManager);
        message.send((Player) sender, "arena.created", "arena", name);
        return true;
    }

    private boolean handleDelete(CommandSender sender, String @NotNull [] args) {
        if (args.length < 2) {
            sender.sendMessage(message.parse("<red>Usage: /tierpvp arena delete <name>"));
            return true;
        }
        String name = args[1];
        if (!arenaManager.hasArena(name)) {
            message.send((Player) sender, "arena.not-found", "arena", name);
            return true;
        }
        arenaManager.removeArena(name);
        configManager.saveArenas(arenaManager);
        message.send((Player) sender, "arena.deleted", "arena", name);
        return true;
    }

    private boolean handleSetSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(message.parse("<red>Only players can use this command."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(message.parse("<red>Usage: /tierpvp arena setspawn <name>"));
            return true;
        }
        String name = args[1];
        Arena arena = arenaManager.getArena(name).orElse(null);
        if (arena == null) {
            message.send(player, "arena.not-found", "arena", name);
            return true;
        }
        arena.setSpawn(player.getLocation());
        configManager.saveArenas(arenaManager);
        message.send(player, "arena.spawn-set", "arena", name);
        return true;
    }

    private boolean handleSetZone(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(message.parse("<red>Only players can use this command."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(message.parse("<red>Usage: /tierpvp arena setzone <name>"));
            return true;
        }
        String name = args[1];
        if (!arenaManager.hasArena(name)) {
            message.send(player, "arena.not-found", "arena", name);
            return true;
        }
        arenaSelectionListener.startSelection(player, name);
        return true;
    }

    private boolean handleConfirmZone(CommandSender sender) {
        if (!(sender instanceof Player player)) return true;
        if (!arenaSelectionListener.hasSelection(player.getUniqueId())) {
            sender.sendMessage(message.parse("<red>No active zone selection."));
            return true;
        }
        ArenaSelectionListener.ArenaSelection sel = arenaSelectionListener.getSelection(player.getUniqueId());
        if (!sel.isComplete()) {
            sender.sendMessage(message.parse("<red>Select both positions first (left-click pos1, right-click pos2)."));
            return true;
        }
        arenaSelectionListener.confirmSelection(player);
        return true;
    }

    private boolean handleCancelZone(CommandSender sender) {
        if (!(sender instanceof Player player)) return true;
        if (!arenaSelectionListener.hasSelection(player.getUniqueId())) {
            sender.sendMessage(message.parse("<red>No active zone selection."));
            return true;
        }
        arenaSelectionListener.cancelSelection(player);
        return true;
    }

    private boolean handleSetVoid(CommandSender sender, String @NotNull [] args) {
        if (args.length < 3) {
            sender.sendMessage(message.parse("<red>Usage: /tierpvp arena setvoid <name> <y>"));
            return true;
        }
        String name = args[1];
        Arena arena = arenaManager.getArena(name).orElse(null);
        if (arena == null) {
            if (sender instanceof Player p) message.send(p, "arena.not-found", "arena", name);
            return true;
        }
        try {
            int y = Integer.parseInt(args[2]);
            arena.setArenaVoid(y);
            configManager.saveArenas(arenaManager);
            if (sender instanceof Player p) message.send(p, "arena.void-confirmation", "arena", name, "y", String.valueOf(y));
        } catch (NumberFormatException e) {
            sender.sendMessage(message.parse("<red>Invalid Y value."));
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (arenaManager.getAllArenas().isEmpty()) {
            if (sender instanceof Player p) message.send(p, "arena.no-arenas");
            else sender.sendMessage(message.parse("<red>No arenas configured."));
            return true;
        }
        sender.sendMessage(message.parse("<gold>Arenas:"));
        for (Arena arena : arenaManager.getAllArenas()) {
            String status = arena.isComplete() ? "<green>✔ Complete" : "<red>✘ Incomplete";
            sender.sendMessage(message.parse("  <gray>- <white>" + arena.getName() + " " + status));
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, String @NotNull [] args) {
        if (args.length < 2) {
            sender.sendMessage(message.parse("<red>Usage: /tierpvp arena info <name>"));
            return true;
        }
        String name = args[1];
        Arena arena = arenaManager.getArena(name).orElse(null);
        if (arena == null) {
            if (sender instanceof Player p) message.send(p, "arena.not-found", "arena", name);
            return true;
        }

        sender.sendMessage(message.parse("<gold>Arena: <white>" + arena.getName()));
        sender.sendMessage(message.parse("  <gray>Spawn: <white>" + (arena.getSpawn() != null ?
                formatLocation(arena.getSpawn()) : "Not set")));
        sender.sendMessage(message.parse("  <gray>Zone: <white>" + (arena.getArenaSpawn() != null ? "Set" : "Not set")));
        sender.sendMessage(message.parse("  <gray>Void Y: <white>" + arena.getArenaVoid()));
        sender.sendMessage(message.parse("  <gray>Complete: " + (arena.isComplete() ? "<green>Yes" : "<red>No")));
        return true;
    }

    private @NotNull String formatLocation(org.bukkit.@NotNull Location loc) {
        return String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
    }

    @Override
    public String getPermission() { return "tierpvp.admin"; }

    @Override
    public String getUsage() { return "/tierpvp arena <create|delete|setspawn|setzone|setvoid|list|info>"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String @NotNull [] args) {
        if (args.length == 1) {
            return Stream.of(
                            "create", "delete", "setspawn", "setzone",
                            "confirmzone", "cancelzone", "setvoid",
                            "list", "info"
                    )
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            switch (sub) {
                case "delete", "setspawn", "setzone", "setvoid", "info" -> {
                    return arenaManager.getAllArenas().stream()
                            .map(Arena::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .toList();
                }
            }
        }

        return List.of();
    }
}
