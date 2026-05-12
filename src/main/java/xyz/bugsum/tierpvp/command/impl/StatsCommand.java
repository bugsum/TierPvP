package xyz.bugsum.tierpvp.command.impl;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.bugsum.tierpvp.command.SubCommand;
import xyz.bugsum.tierpvp.module.StatsRepository;
import xyz.bugsum.tierpvp.util.MessageUtil;
import xyz.bugsum.tierpvp.util.PlayerStats;

import java.util.List;
import java.util.Objects;

public class StatsCommand implements SubCommand {
    private final StatsRepository statsRepository;
    private final MessageUtil message;

    public StatsCommand(StatsRepository statsRepository, MessageUtil message) {
        this.statsRepository = statsRepository;
        this.message = message;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String targetName;
        if (args.length >= 1) {
            targetName = args[0];
        } else if (sender instanceof Player player) {
            targetName = player.getName();
        } else {
            sender.sendMessage(message.parse("<red>Usage: /tierpvp stats <player>"));
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            statsRepository.getStats(target.getUniqueId()).thenAccept(opt -> {
                Bukkit.getScheduler().runTask(
                        Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("TierPvP")),
                        () -> displayStats(sender, opt.orElse(PlayerStats.empty(targetName)))
                );
            });
        } else {
            statsRepository.getStatsByName(targetName).thenAccept(opt -> {
                Bukkit.getScheduler().runTask(
                        Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("TierPvP")),
                        () -> {
                            if (opt.isPresent()) {
                                displayStats(sender, opt.get());
                            } else {
                                if (sender instanceof Player p) {
                                    message.send(p, "stats.not-found", "player", targetName);
                                }
                            }
                        }
                );
            });
        }
        return true;
    }

    private void displayStats(CommandSender sender, PlayerStats stats) {
        if (sender instanceof Player p) {
            message.send(p, "stats.header", "player", stats.playerName());
            message.send(p, "stats.kills", "kills", String.valueOf(stats.kills()));
            message.send(p, "stats.deaths", "deaths", String.valueOf(stats.deaths()));
            message.send(p, "stats.wins", "wins", String.valueOf(stats.wins()));
            message.send(p, "stats.games", "games", String.valueOf(stats.gamesPlayed()));
            message.send(p, "stats.highest", "level", String.valueOf(stats.highestLevel()));
        }
    }

    @Override
    public String getPermission() { return "tierpvp.stats"; }

    @Override
    public String getUsage() { return "/tierpvp stats [player]"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {

        // Suggest player names for first argument
        if (args.length == 1) {
            String input = args[0].toLowerCase();

            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .toList();
        }

        return List.of();
    }
}
