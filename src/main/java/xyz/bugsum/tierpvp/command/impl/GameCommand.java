package xyz.bugsum.tierpvp.command.impl;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.bugsum.tierpvp.command.SubCommand;
import xyz.bugsum.tierpvp.manager.ArenaManager;
import xyz.bugsum.tierpvp.manager.ConfigManager;
import xyz.bugsum.tierpvp.manager.GameManager;
import xyz.bugsum.tierpvp.util.MessageUtil;

import java.util.List;

public class GameCommand implements SubCommand {
    private final GameManager gameManager;
    private final ConfigManager configManager;
    private final ArenaManager arenaManager;
    private final MessageUtil message;
    private final String type;

    public GameCommand(GameManager gameManager, ConfigManager configManager, ArenaManager arenaManager, MessageUtil message, String type) {
        this.gameManager = gameManager;
        this.configManager = configManager;
        this.arenaManager = arenaManager;
        this.message = message;
        this.type = type;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        return switch (type) {
            case "forcestart" -> {
                gameManager.forceRotate();
                yield true;
            }
            case "reload" -> {
                configManager.loadAll(arenaManager);
                if (sender instanceof Player p) {
                    message.send(p, "reload");
                } else {
                    sender.sendMessage(message.parse("<green>Configuration reloaded!"));
                }
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public String getPermission() { return "tierpvp.admin"; }

    @Override
    public String getUsage() {
        return switch (type) {
            case "forcestart" -> "/tierpvp forcestart";
            case "reload" -> "/tierpvp reload";
            default -> "/tierpvp " + type;
        };
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
