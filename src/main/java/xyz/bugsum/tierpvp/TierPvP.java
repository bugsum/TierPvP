package xyz.bugsum.tierpvp;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.bugsum.tierpvp.command.TierPvPCommand;
import xyz.bugsum.tierpvp.command.impl.ArenaCommand;
import xyz.bugsum.tierpvp.command.impl.GameCommand;
import xyz.bugsum.tierpvp.command.impl.StatsCommand;
import xyz.bugsum.tierpvp.listener.*;
import xyz.bugsum.tierpvp.manager.*;
import xyz.bugsum.tierpvp.module.GameScoreboard;
import xyz.bugsum.tierpvp.module.PAPIExpansion;
import xyz.bugsum.tierpvp.module.StatsRepository;
import xyz.bugsum.tierpvp.task.ActionBarTask;
import xyz.bugsum.tierpvp.task.DeathTask;
import xyz.bugsum.tierpvp.util.MessageUtil;

import java.util.Objects;


public final class TierPvP extends JavaPlugin {
    private StatsRepository statsRepository;

    @Override
    public void onEnable() {
        MessageUtil message = new MessageUtil();
        ArenaManager arenaManager = new ArenaManager();
        ConfigManager configManager = new ConfigManager(this, message);
        configManager.loadAll(arenaManager);

        statsRepository = new StatsRepository();
        statsRepository.init(this);

        GameScoreboard scoreboard = configManager.isScoreboardEnabled() ? new GameScoreboard(configManager) : null;
        CombatManager combatManager = new CombatManager(configManager.getCombatTagSeconds());
        PlayerManager playerManager = new PlayerManager();
        GameManager gameManager = new GameManager(this, configManager, arenaManager, playerManager, combatManager, scoreboard, statsRepository, message);

        PluginManager pluginManager = getServer().getPluginManager();
        ArenaSelectionListener arenaSelectionListener = new ArenaSelectionListener(arenaManager, configManager, message);

        // Register events
        pluginManager.registerEvents(new CombatListener(combatManager, playerManager, gameManager, configManager), this);
        pluginManager.registerEvents(new ConnectionListener(this, gameManager, configManager), this);
        pluginManager.registerEvents(new ProtectionListener(playerManager), this);
        pluginManager.registerEvents(new ArenaSpawnListener(gameManager, playerManager, combatManager, message),this);
        pluginManager.registerEvents(arenaSelectionListener, this);
        pluginManager.registerEvents(new RespawnListener(gameManager, playerManager, configManager), this);

        TierPvPCommand mainCommand = new TierPvPCommand(message);

        mainCommand.registerSubCommands("arena", new ArenaCommand(arenaManager, configManager, arenaSelectionListener, message));
        mainCommand.registerSubCommands("stats", new StatsCommand(statsRepository, message));
        mainCommand.registerSubCommands("forcestart", new GameCommand(gameManager, configManager, arenaManager, message, "forcestart"));
        mainCommand.registerSubCommands("reload", new GameCommand(gameManager, configManager, arenaManager, message, "reload"));

        Objects.requireNonNull(getCommand("tierpvp")).setExecutor(mainCommand);
        Objects.requireNonNull(getCommand("tierpvp")).setTabCompleter(mainCommand);

        new DeathTask(gameManager, playerManager).runTaskTimer(this, 20L, 5L);
        new ActionBarTask(gameManager, playerManager, combatManager, message).runTaskTimer(this, 20L, 10L);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(gameManager, playerManager, combatManager, configManager).register();
            getLogger().info("PlaceholderAPI hooked! Placeholders available with %tierpvp_<placeholder>%");
        }

        gameManager.startNewGame();
    }

    @Override
    public void onDisable() {
        if (statsRepository != null) {
            statsRepository.shutdown();
        }
    }
}
