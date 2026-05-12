package xyz.bugsum.tierpvp.module;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.bugsum.tierpvp.manager.CombatManager;
import xyz.bugsum.tierpvp.manager.ConfigManager;
import xyz.bugsum.tierpvp.manager.GameManager;
import xyz.bugsum.tierpvp.manager.PlayerManager;
import xyz.bugsum.tierpvp.util.Tier;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PAPIExpansion extends PlaceholderExpansion {
    private static final Pattern TOP_PATTERN = Pattern.compile("top_(\\d+)_(name|level|kills|killstreak)");

    private final GameManager gameManager;
    private final PlayerManager playerManager;
    private final CombatManager combatManager;
    private final ConfigManager configManager;

    public PAPIExpansion(GameManager gameManager, PlayerManager playerManager, CombatManager combatManager, ConfigManager configManager) {
        this.gameManager = gameManager;
        this.playerManager = playerManager;
        this.combatManager = combatManager;
        this.configManager = configManager;
    }

    @Override
    public @NotNull String getIdentifier() { return "tierpvp"; }

    @Override
    public @NotNull String getAuthor() { return "Samarth Sharma"; }

    @Override
    public @NotNull String getVersion() { return "1.0.0"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        GamePlayer gp = playerManager.getPlayer(player.getUniqueId()).orElse(null);
        Tiers tiers = configManager.getTiers();

        return switch (params.toLowerCase()) {
            case "level" -> gp != null ? String.valueOf(gp.getLevel()) : "0";
            case "level_max" -> String.valueOf(tiers.getMaxLevel());
            case "kills" -> gp != null ? String.valueOf(gp.getKills()) : "0";
            case "deaths" -> gp != null ? String.valueOf(gp.getDeaths()) : "0";
            case "killstreak" -> gp != null ? String.valueOf(gp.getKillStreak()) : "0";
            case "weapon" -> {
                if (gp == null) yield "None";
                Tier tier = tiers.getLevel(gp.getLevel()).orElse(null);
                yield tier != null ? stripTags(tier.displayName()) : "None";
            }
            case "arena" -> {
                GameSession session = gameManager.getSession();
                yield session != null && session.getArena() != null ? session.getArena().getName() : "None";
            }
            case "players" -> String.valueOf(playerManager.getPlayerCount());
            case "combat_tagged" -> combatManager.isTagged(player.getUniqueId()) ? "true" : "false";
            case "combat_time" -> String.valueOf(combatManager.getRemainingSeconds(player.getUniqueId()));
            case "team" -> gp != null && gp.getTeam() >= 0 ? String.valueOf(gp.getTeam() + 1) : "None";
            case "state" -> {
                GameSession s = gameManager.getSession();
                yield s != null ? s.getState().name() : "NONE";
            }
            case "spawn_protected" -> {
                GameSession s = gameManager.getSession();
                yield s != null && s.getArena() != null
                        && s.getArena().isInSpawnArea(player.getLocation())
                        && !combatManager.isTagged(player.getUniqueId()) ? "true" : "false";
            }
            default -> resolveTopPlaceholder(params);
        };
    }

    private String resolveTopPlaceholder(String params) {
        Matcher matcher = TOP_PATTERN.matcher(params.toLowerCase());
        if (!matcher.matches()) return null;

        int rank = Integer.parseInt(matcher.group(1));
        if (rank < 1) return null;

        String type = matcher.group(2);
        int index = rank - 1;

        List<GamePlayer> top = playerManager.getTopPlayers(rank);
        if (index >= top.size()) {
            return "name".equals(type) ? "---" : "0";
        }

        GamePlayer gp = top.get(index);
        return switch (type) {
            case "name" -> {
                Player p = Bukkit.getPlayer(gp.getUuid());
                yield p != null ? p.getName() : "???";
            }
            case "level" -> String.valueOf(gp.getLevel());
            case "kills" -> String.valueOf(gp.getKills());
            case "killstreak" -> String.valueOf(gp.getKillStreak());
            default -> null;
        };
    }

    private String stripTags(String input) {
        return input.replaceAll("<[^>]+>", "");
    }
}
