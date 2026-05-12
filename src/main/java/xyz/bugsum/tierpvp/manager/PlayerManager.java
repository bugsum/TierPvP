package xyz.bugsum.tierpvp.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.bugsum.tierpvp.module.GamePlayer;

import java.util.*;

public class PlayerManager {
    private final Map<UUID, GamePlayer> players = new HashMap<>();

    private static final long TOP_CACHE_TTL = 2000L;
    private List<GamePlayer> cachedTopPlayers = List.of();
    private long topPlayersCacheTime = 0;

    public GamePlayer addPlayer(UUID uuid) {
        GamePlayer gp = new GamePlayer(uuid);

        players.put(uuid, gp);
        return gp;
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }

    public Optional<GamePlayer> getPlayer(UUID uuid) {
        return Optional.ofNullable(players.get(uuid));
    }

    public boolean isPlaying(UUID uuid) {
        return players.containsKey(uuid);
    }

    public Collection<GamePlayer> getAllPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    public int getPlayerCount() {
        return players.size();
    }

    public void resetAll() {
        players.values().forEach(GamePlayer::reset);
        invalidateTopCache();
    }

    public List<GamePlayer> getTopPlayers(int limit) {
        long now = System.currentTimeMillis();
        if (now - topPlayersCacheTime > TOP_CACHE_TTL) {
            cachedTopPlayers = players.values().stream()
                    .sorted(Comparator.comparingInt(GamePlayer::getLevel)
                            .thenComparingInt(GamePlayer::getKills)
                            .reversed())
                    .toList();
            topPlayersCacheTime = now;
        }
        return cachedTopPlayers.subList(0, Math.min(limit, cachedTopPlayers.size()));
    }

    public void invalidateTopCache() {
        topPlayersCacheTime = 0;
    }

    public Optional<Player> getBukkitPlayer(@NotNull GamePlayer gp) {
        return Optional.ofNullable(Bukkit.getPlayer(gp.getUuid()));
    }

    public int getTeamCount(int teamId) {
        return (int) players.values().stream().filter(p -> p.getTeam() == teamId).count();
    }

    public int getSmallestTeam(int teamCount) {
        int smallest = 0;
        int smallestSize = Integer.MAX_VALUE;

        for (int i = 0; i < teamCount; i++) {
            int size = getTeamCount(i);
            if (size < smallestSize) {
                smallestSize = size;
                smallest = i;
            }
        }
        return smallest;
    }
}
