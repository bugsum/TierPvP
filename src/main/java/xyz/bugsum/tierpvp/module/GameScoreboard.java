package xyz.bugsum.tierpvp.module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import xyz.bugsum.tierpvp.manager.ConfigManager;
import xyz.bugsum.tierpvp.manager.PlayerManager;
import xyz.bugsum.tierpvp.util.Tier;

import java.util.List;

public class GameScoreboard {
    private static final String OBJECTIVE_NAME = "tierpvp";
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final ConfigManager configManager;

    public GameScoreboard(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void setup(@NotNull Player player, PlayerManager playerManager, GameSession session, Tiers tiers) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Component title = MINI.deserialize(configManager.getScoreboardTitle());
        Objective obj = board.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(board);
        update(player, playerManager, session, tiers);
    }

    public void update(Player player, PlayerManager playerManager, GameSession session, Tiers tiers) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective(OBJECTIVE_NAME);
        if (obj == null) return;

        board.getEntries().forEach(board::resetScores);

        GamePlayer gp = playerManager.getPlayer(player.getUniqueId()).orElse(null);
        if (gp == null) return;

        Tier currentLevel = tiers.getLevel(gp.getLevel()).orElse(null);
        String weaponName = currentLevel != null ? stripMiniMessage(currentLevel.displayName()) : "None";
        String weaponRaw = currentLevel != null ? currentLevel.displayName() : "None";
        String arenaName = session != null && session.getArena() != null ? session.getArena().getName() : "None";

        List<GamePlayer> top = playerManager.getTopPlayers(3);
        String top1Name = getTopName(top, 0);
        String top1Level = getTopLevel(top, 0);
        String top2Name = getTopName(top, 1);
        String top2Level = getTopLevel(top, 1);
        String top3Name = getTopName(top, 2);
        String top3Level = getTopLevel(top, 2);

        List<String> lines = configManager.getScoreboardLines();
        int score = lines.size();

        for (String line : lines) {
            String resolved = line
                    .replace("<level>", String.valueOf(gp.getLevel()))
                    .replace("<weapon>", weaponRaw)
                    .replace("<kills>", String.valueOf(gp.getKills()))
                    .replace("<deaths>", String.valueOf(gp.getDeaths()))
                    .replace("<killstreak>", String.valueOf(gp.getKillStreak()))
                    .replace("<arena>", arenaName)
                    .replace("<players>", String.valueOf(playerManager.getPlayerCount()))
                    .replace("<top_1_name>", top1Name)
                    .replace("<top_1_level>", top1Level)
                    .replace("<top_2_name>", top2Name)
                    .replace("<top_2_level>", top2Level)
                    .replace("<top_3_name>", top3Name)
                    .replace("<top_3_level>", top3Level)
                    .replace("<player>", player.getName())
                    .replace("<level_max>", String.valueOf(tiers.getMaxLevel()));

            Component component = MINI.deserialize(resolved);
            String entry = generateUniqueEntry(score);
            Score s = obj.getScore(entry);
            s.setScore(score);
            s.customName(component);
            score--;
        }
    }

    public void updateAll(PlayerManager playerManager, GameSession session, Tiers tiers) {
        for (GamePlayer gp : playerManager.getAllPlayers()) {
            Player player = Bukkit.getPlayer(gp.getUuid());
            if (player != null && player.isOnline()) {
                update(player, playerManager, session, tiers);
                gp.setScoreboardDirty(false);
            }
        }
    }

    public void remove(@NotNull Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private @NotNull String getTopName(@NotNull List<GamePlayer> players, int index) {
        if (index >= players.size()) return "---";
        Player p = Bukkit.getPlayer(players.get(index).getUuid());
        return p != null ? p.getName() : "???";
    }

    private @NotNull String getTopLevel(@NotNull List<GamePlayer> players, int index) {
        if (index >= players.size()) return "0";
        return String.valueOf(players.get(index).getLevel());
    }

    private @NotNull String generateUniqueEntry(int index) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= index; i++) {
            sb.append("§").append(Integer.toHexString(i % 16));
        }
        return sb.toString();
    }

    @Contract(pure = true)
    private @NotNull String stripMiniMessage(@NotNull String input) {
        return input.replaceAll("<[^>]+>", "");
    }
}
