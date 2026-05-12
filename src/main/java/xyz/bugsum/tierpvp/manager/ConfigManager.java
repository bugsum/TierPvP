package xyz.bugsum.tierpvp.manager;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import xyz.bugsum.tierpvp.TierPvP;
import xyz.bugsum.tierpvp.config.ArenaConfig;
import xyz.bugsum.tierpvp.config.TiersConfig;
import xyz.bugsum.tierpvp.module.Tiers;
import xyz.bugsum.tierpvp.util.MessageUtil;

import java.io.File;
import java.util.List;

public class ConfigManager {
    private final TierPvP instance;
    private final MessageUtil message;

    @Getter
    private String gamemode;
    @Getter
    private boolean demotionOnDeath;
    @Getter
    private int demotionMinLevel;
    @Getter
    private boolean environmentDeathDemotes;
    @Getter
    private int combatTagSeconds;
    @Getter
    private int celebrationSeconds;
    @Getter
    private boolean spawnProtection;
    @Getter
    private boolean levelUpTitle;
    @Getter
    private boolean scoreboardEnabled;
    @Getter
    private int killStreakBroadcast;
    @Getter
    private String language;
    @Getter
    private boolean autoJoinOnConnect;
    @Getter
    private int teamCount;
    @Getter
    private boolean friendlyFire;
    @Getter
    private boolean balanceOnJoin;
    @Getter
    private String scoreboardTitle;
    @Getter
    private List<String> scoreboardLines;

    @Getter
    private Tiers tiers;

    public ConfigManager(TierPvP instance, MessageUtil message) {
        this.instance = instance;
        this.message = message;
    }

    public void loadAll(ArenaManager arenaManager) {
        instance.saveDefaultConfig();
        instance.reloadConfig();
        loadMainConfig();
        loadMessageConfig();
        loadTiersConfig();
        loadArenas(arenaManager);
    }

    private void loadMainConfig() {
        FileConfiguration config = instance.getConfig();

        gamemode = config.getString("game.mode", "FFA");
        demotionOnDeath = config.getBoolean("game.demotion-on-death", true);
        demotionMinLevel = config.getInt("game.demotion-min-level", 1);
        environmentDeathDemotes = config.getBoolean("game.environment-death-demotes", true);
        combatTagSeconds = config.getInt("game.combat-tag-seconds", 10);
        celebrationSeconds = config.getInt("game.celebration-seconds", 5);
        spawnProtection = config.getBoolean("game.spawn-protection", true);
        levelUpTitle = config.getBoolean("display.level-up-title", true);
        scoreboardEnabled = config.getBoolean("display.scoreboard-enabled", true);
        killStreakBroadcast = config.getInt("display.kill-streak-broadcast", 3);
        language = config.getString("settings.language", "en");
        autoJoinOnConnect = config.getBoolean("settings.auto-join-on-connect", true);
        teamCount = config.getInt("teams.count", 2);
        friendlyFire = config.getBoolean("teams.friendly-fire", false);
        balanceOnJoin = config.getBoolean("teams.balance-on-join", true);
        scoreboardTitle = config.getString("scoreboard.title", "<gold><bold>★ GunGame ★");
        scoreboardLines = config.getStringList("scoreboard.lines");

        if (scoreboardLines.isEmpty()) {
            scoreboardLines = List.of("", "<gray>Level: <gold><level>", "<gray>Weapon: <white><weapon>",
                    " ", "<gray>Kills: <green><kills>", "<gray>Deaths: <red><deaths>",
                    "  ", "<gray>Arena: <aqua><arena>");
        }
    }

    private void loadMessageConfig() {
        File messageDir = new File(instance.getDataFolder(), "messages");

        if (!messageDir.exists()) {
            messageDir.mkdirs();
            instance.saveResource("messages/en.yml", false);
        }

        File langFile = new File(messageDir, language + ".yml");
        if (!langFile.exists()) {
            langFile = new File(messageDir, "en.yml");
        }

        message.load(langFile);
    }

    private void loadTiersConfig() {
        File levelsFile = new File(instance.getDataFolder(), "tiers.yml");

        if (!levelsFile.exists()) {
            instance.saveResource("tiers.yml", false);
        }

        tiers = TiersConfig.load(levelsFile);
    }

    private void loadArenas(ArenaManager arenaManager) {
        File arenasFile = new File(instance.getDataFolder(), "arenas.yml");

        if (arenasFile.exists()) {
            ArenaConfig.load(arenasFile, arenaManager);
        }
    }

    public void saveArenas(ArenaManager arenaManager) {
        File arenasFile = new File(instance.getDataFolder(), "arenas.yml");
        ArenaConfig.save(arenasFile, arenaManager);
    }

    public boolean isTeamMode() { return "TEAMS".equalsIgnoreCase(gamemode); }
}
