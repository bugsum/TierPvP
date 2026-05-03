package xyz.bugsum.tierpvp.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {
    boolean execute(CommandSender sender, String[] args);
    String getPermission();
    String getUsage();
    List<String> tabComplete(CommandSender sender, String[] args);
}
