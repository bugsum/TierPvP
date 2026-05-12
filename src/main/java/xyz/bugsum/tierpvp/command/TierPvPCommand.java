package xyz.bugsum.tierpvp.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.bugsum.tierpvp.util.MessageUtil;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TierPvPCommand implements CommandExecutor, TabCompleter {
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();
    private final MessageUtil message;

    public TierPvPCommand(MessageUtil message) {
        this.message = message;
    }

    public void registerSubCommands(String name, SubCommand command) {
        subCommands.put(name.toLowerCase(), command);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            sender.sendMessage(message.parse("<gold>TierPvP</gold> <gray>— Use /tierpvp help for commands"));
            return true;
        }

        String sub = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(sub);

        if (subCommand == null) {
            sender.sendMessage(message.parse("<red>Unknown command. Use /tierpvp help"));
            return true;
        }

        if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(message.format("no-permission"));
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        if (!subCommand.execute(sender, subArgs)) {
            sender.sendMessage(message.parse("<red>Usage: " + subCommand.getUsage()));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return subCommands.entrySet().stream()
                    .filter(e -> e.getValue().getPermission() == null || sender.hasPermission(e.getValue().getPermission()))
                    .map(Map.Entry::getKey)
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());

        if (subCommand == null) return List.of();

        if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
            return List.of();
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        return subCommand.tabComplete(sender, subArgs);
    }
}
