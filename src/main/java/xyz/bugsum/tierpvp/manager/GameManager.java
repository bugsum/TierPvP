package xyz.bugsum.tierpvp.manager;

import lombok.Getter;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import xyz.bugsum.tierpvp.TierPvP;
import xyz.bugsum.tierpvp.module.*;
import xyz.bugsum.tierpvp.util.GameState;
import xyz.bugsum.tierpvp.util.MessageUtil;
import xyz.bugsum.tierpvp.util.PlayerKit;
import xyz.bugsum.tierpvp.util.Tier;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class GameManager {
    private final TierPvP instance;
    private final ConfigManager configManager;
    private final ArenaManager arenaManager;
    private final PlayerManager playerManager;
    private final CombatManager combatManager;
    private final GameScoreboard scoreboard;
    private final StatsRepository statsRepository;
    private final MessageUtil message;

    @Getter
    private GameSession session;

    public GameManager(TierPvP instance, ConfigManager configManager, ArenaManager arenaManager, PlayerManager playerManager, CombatManager combatManager, GameScoreboard scoreboard, StatsRepository statsRepository, MessageUtil message) {
        this.instance = instance;
        this.configManager = configManager;
        this.arenaManager = arenaManager;
        this.playerManager = playerManager;
        this.combatManager = combatManager;
        this.scoreboard = scoreboard;
        this.statsRepository = statsRepository;
        this.message = message;
    }

    public void startNewGame() {
        List<Arena> arenas = arenaManager.getCompleteArenas();
        if (arenas.isEmpty()) {
            instance.getLogger().warning("No complete arenas configured. Game cannot start.");
            return;
        }

        Arena arena = arenaManager.next(arenas);
        if (arena == null) return;

        session = new GameSession(arena);
        playerManager.resetAll();
        combatManager.clearAll();

        Tiers tiers = configManager.getTiers();
        for (GamePlayer gp : playerManager.getAllPlayers()) {
            Player player = Bukkit.getPlayer(gp.getUuid());
            if (player != null && player.isOnline()) {
                setupPlayer(player, gp, tiers);
            }
        }

        if (scoreboard != null) scoreboard.updateAll(playerManager, session, configManager.getTiers());

        Bukkit.getOnlinePlayers().forEach(p -> {
            message.send(p, "arena.switch", "arena", arena.getName());
        });
    }

    public void addPlayer(@NotNull Player player) {
        if (playerManager.isPlaying(player.getUniqueId())) return;

        GamePlayer gp = playerManager.addPlayer(player.getUniqueId());
        Tiers tiers = configManager.getTiers();

        if (configManager.isTeamMode()) {
            int team = playerManager.getSmallestTeam(configManager.getTeamCount());
            gp.setTeam(team);
        }

        if (session != null && session.isActive()) {
            setupPlayer(player, gp, tiers);
        }

        statsRepository.incrementGamesPlayed(player.getUniqueId(), player.getName());
        if (scoreboard != null) scoreboard.updateAll(playerManager, session, tiers);

        for (GamePlayer other : playerManager.getAllPlayers()) {
            Player otherPlayer = Bukkit.getPlayer(other.getUuid());
            if (otherPlayer != null) {
                message.send(otherPlayer, "join",
                        "player", player.getName(),
                        "count", String.valueOf(playerManager.getPlayerCount()));
            }
        }
    }

    public void removePlayer(@NotNull Player player) {
        if (!playerManager.isPlaying(player.getUniqueId())) return;

        combatManager.clearTag(player.getUniqueId());
        if (scoreboard != null) scoreboard.remove(player);
        playerManager.removePlayer(player.getUniqueId());

        Tiers tiers = configManager.getTiers();
        if (scoreboard != null) scoreboard.updateAll(playerManager, session, tiers);

        for (GamePlayer other : playerManager.getAllPlayers()) {
            Player otherPlayer = Bukkit.getPlayer(other.getUuid());
            if (otherPlayer != null) {
                message.send(otherPlayer, "leave",
                        "player", player.getName(),
                        "count", String.valueOf(playerManager.getPlayerCount()));
            }
        }
    }

    public void handleKill(Player killer, Player victim) {
        if (session == null || !session.isActive()) return;

        Optional<GamePlayer> killerGp = playerManager.getPlayer(killer.getUniqueId());
        Optional<GamePlayer> victimGp = playerManager.getPlayer(victim.getUniqueId());
        if (killerGp.isEmpty() || victimGp.isEmpty()) return;

        if (configManager.isTeamMode() && !configManager.isFriendlyFire()
                && killerGp.get().getTeam() == victimGp.get().getTeam()) {
            return;
        }

        processKill(killer, killerGp.get(), victim, victimGp.get());
    }

    public void handleEnvironmentDeath(Player victim) {
        if (session == null || !session.isActive()) return;

        Optional<GamePlayer> victimGp = playerManager.getPlayer(victim.getUniqueId());
        if (victimGp.isEmpty()) return;

        GamePlayer vgp = victimGp.get();
        Tiers tiers = configManager.getTiers();

        if (configManager.isEnvironmentDeathDemotes()) {
            vgp.demote(configManager.getDemotionMinLevel());

            tiers.getLevel(vgp.getLevel()).ifPresent(victimLevel -> PlayerKit.apply(victim, victimLevel));
        }

        Optional<UUID> taggerUuid = combatManager.getTagger(victim.getUniqueId());
        if (taggerUuid.isPresent()) {
            Player tagger = Bukkit.getPlayer(taggerUuid.get());
            Optional<GamePlayer> taggerGp = playerManager.getPlayer(taggerUuid.get());
            if (tagger != null && tagger.isOnline() && taggerGp.isPresent()) {
                GamePlayer tgp = taggerGp.get();

                if (tiers.isFinal(tgp.getLevel())) {
                    triggerWin(tagger, tgp);
                    return;
                }

                tgp.promote();

                double maxHealth = Objects.requireNonNull(tagger.getAttribute(Attribute.MAX_HEALTH)).getValue();
                tagger.heal(maxHealth);

                Tier newTier = tiers.getLevel(tgp.getLevel()).orElse(null);
                if (newTier != null) {
                    PlayerKit.apply(tagger, newTier);
                    sendLevelUpFeedback(tagger, tgp, newTier);
                }

                String deathKey = victim.getLocation().getBlock().getType() == Material.WATER
                        ? "death.pushed-in-water" : "death.pushed-in-void";
                broadcastMessage(deathKey,
                        "killer", tagger.getName(),
                        "victim", victim.getName());

                statsRepository.incrementKills(tagger.getUniqueId(), tagger.getName());
                combatManager.clearTag(tagger.getUniqueId());

                int highestLevel = tagger.getLevel();
                statsRepository.updateHighestLevel(tagger.getUniqueId(), tagger.getName(), highestLevel);

                int streakThreshold = configManager.getKillStreakBroadcast();
                if (streakThreshold > 0 && tgp.getKillStreak() >= streakThreshold
                        && tgp.getKillStreak() % streakThreshold == 0) {
                    broadcastMessage("level-up.kill-streak-message",
                            "player", tagger.getName(),
                            "streak", String.valueOf(tgp.getKillStreak()));
                }
            }
        } else {
            String deathKey = victim.getLocation().getBlock().getType() == Material.WATER
                    ? "death.fell-in-water" : "death.fell-in-void";
            broadcastMessage(deathKey, "player", victim.getName());
        }

        combatManager.clearTag(victim.getUniqueId());
        statsRepository.incrementDeaths(victim.getUniqueId(), victim.getName());

        if (scoreboard != null) scoreboard.updateAll(playerManager, session, tiers);
    }

    private void processKill(Player killer, @NotNull GamePlayer killerGp, Player victim, GamePlayer victimGp) {
        Tiers tiers = configManager.getTiers();

        if (tiers.isFinal(killerGp.getLevel())) {
            triggerWin(killer, killerGp);
            return;
        }

        killerGp.promote();

        double maxHealth = Objects.requireNonNull(killer.getAttribute(Attribute.MAX_HEALTH)).getValue();
        killer.heal(maxHealth);
        if (configManager.isDemotionOnDeath()) {
            victimGp.demote(configManager.getDemotionMinLevel());
        }

        Tier killerLevel = tiers.getLevel(killerGp.getLevel()).orElse(null);
        if (killerLevel != null) {
            PlayerKit.apply(killer, killerLevel);
            sendLevelUpFeedback(killer, killerGp, killerLevel);
        }

        Tier victimLevel = tiers.getLevel(victimGp.getLevel()).orElse(null);
        if (victimLevel != null) {
            PlayerKit.apply(victim, victimLevel);
        }

        String weaponName = killerLevel != null ? killerLevel.displayName() : "Unknown";

        broadcastMessage("death.kill-message",
                "killer", killer.getName(),
                "victim", victim.getName(),
                "weapon", weaponName);

        message.send(victim, "death.demotion-message",
                "level", String.valueOf(victimGp.getLevel()),
                "weapon", victimLevel != null ? victimLevel.displayName() : "Unknown");

        combatManager.clearTag(victim.getUniqueId());
        combatManager.clearTag(killer.getUniqueId());
        statsRepository.incrementKills(killer.getUniqueId(), killer.getName());
        statsRepository.incrementDeaths(victim.getUniqueId(), victim.getName());

        int highestLevel = killerGp.getLevel();
        statsRepository.updateHighestLevel(killer.getUniqueId(), killer.getName(), highestLevel);

//        respawnVictim(victim, victimGp, chain);
        if (scoreboard != null) scoreboard.updateAll(playerManager, session, tiers);

        int streakThreshold = configManager.getKillStreakBroadcast();
        if (streakThreshold > 0 && killerGp.getKillStreak() >= streakThreshold
                && killerGp.getKillStreak() % streakThreshold == 0) {
            broadcastMessage("level-up.kill-streak-message",
                    "player", killer.getName(),
                    "streak", String.valueOf(killerGp.getKillStreak()));
        }
    }

    private void triggerWin(@NotNull Player winner, @NotNull GamePlayer winnerGp) {
        session.setState(GameState.CELEBRATING);

        broadcastMessage("win.celebration-message",
                "winner", winner.getName(),
                "level", String.valueOf(winnerGp.getLevel()),
                "seconds", String.valueOf(configManager.getCelebrationSeconds()));

        Title winTitle = Title.title(
                message.format("win.title"),
                message.format("win.subtitle", "winner", winner.getName()),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))
        );

        for (GamePlayer gp : playerManager.getAllPlayers()) {
            Player p = Bukkit.getPlayer(gp.getUuid());
            if (p != null) {
                p.showTitle(winTitle);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
        }

        statsRepository.incrementWins(winner.getUniqueId(), winner.getName());
        spawnFireworks(winner.getLocation());

        new BukkitRunnable() {
            @Override
            public void run() {
                startNewGame();
            }
        }.runTaskLater(instance, configManager.getCelebrationSeconds() * 20L);
    }

    private void setupPlayer(@NotNull Player player, @NotNull GamePlayer gp, @NotNull Tiers tiers) {
        player.teleport(session.getArena().getSpawn());

        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(attr != null ? attr.getValue() : player.getHealth());

        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.getInventory().clear();

        tiers.getLevel(gp.getLevel()).ifPresent(level -> PlayerKit.apply(player, level));

        if (scoreboard != null) scoreboard.setup(player, playerManager, session, tiers);
    }

    private void sendLevelUpFeedback(Player player, @NotNull GamePlayer gp, @NotNull Tier tier) {
        message.send(player, "level-up.message",
                "level", String.valueOf(gp.getLevel()),
                "weapon", tier.displayName());

        if (configManager.isLevelUpTitle()) {
            Title title = Title.title(
                    message.format("level-up.title", "level", String.valueOf(gp.getLevel())),
                    message.format("level-up.subtitle", "weapon", tier.displayName()),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(1), Duration.ofMillis(300))
            );
            player.showTitle(title);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }

    private void broadcastMessage(String key, String... placeholders) {
        for (GamePlayer gp : playerManager.getAllPlayers()) {
            Player p = Bukkit.getPlayer(gp.getUuid());
            if (p != null) {
                message.send(p, key, placeholders);
            }
        }
    }

    private void spawnFireworks(Location loc) {
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= 3 || session == null || !session.isCelebrating()) {
                    cancel();
                    return;
                }
                Firework fw = loc.getWorld().spawn(loc.clone().add(0, 1, 0), Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .withColor(Color.YELLOW, Color.ORANGE, Color.RED)
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withFlicker()
                        .build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);
                count++;
            }
        }.runTaskTimer(instance, 0L, 20L);
    }

    public boolean hasActiveGame() {
        return session != null;
    }

    public void forceRotate() {
        if (session != null) {
            session.setState(GameState.CELEBRATING);
        }

        broadcastMessage("force-start");
        new BukkitRunnable() {
            @Override
            public void run() {
                startNewGame();
            }
        }.runTaskLater(instance, 40L);
    }
}
