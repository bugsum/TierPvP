package xyz.bugsum.tierpvp.module;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.bugsum.tierpvp.util.PlayerStats;

import java.io.File;
import java.sql.*;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsRepository {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Connection connection;

    public void init(JavaPlugin plugin) {
        try {
            File dbFile = new File(plugin.getDataFolder(), "stats.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS player_stats (" +
                                "uuid TEXT PRIMARY KEY, " +
                                "name TEXT, " +
                                "kills INTEGER DEFAULT 0, " +
                                "deaths INTEGER DEFAULT 0, " +
                                "wins INTEGER DEFAULT 0, " +
                                "games_played INTEGER DEFAULT 0, " +
                                "highest_level INTEGER DEFAULT 0)"
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<Optional<PlayerStats>> getStats(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT * FROM player_stats WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return Optional.of(new PlayerStats(
                            rs.getString("name"),
                            rs.getInt("kills"),
                            rs.getInt("deaths"),
                            rs.getInt("wins"),
                            rs.getInt("games_played"),
                            rs.getInt("highest_level")
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Optional.empty();
        }, executor);
    }

    public CompletableFuture<Optional<PlayerStats>> getStatsByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT * FROM player_stats WHERE LOWER(name) = LOWER(?)")) {
                ps.setString(1, name);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return Optional.of(new PlayerStats(
                            rs.getString("name"),
                            rs.getInt("kills"),
                            rs.getInt("deaths"),
                            rs.getInt("wins"),
                            rs.getInt("games_played"),
                            rs.getInt("highest_level")
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Optional.empty();
        }, executor);
    }

    public void incrementKills(UUID uuid, String name) {
        executor.submit(() -> upsert(uuid, name, "kills"));
    }

    public void incrementDeaths(UUID uuid, String name) {
        executor.submit(() -> upsert(uuid, name, "deaths"));
    }

    public void incrementWins(UUID uuid, String name) {
        executor.submit(() -> upsert(uuid, name, "wins"));
    }

    public void incrementGamesPlayed(UUID uuid, String name) {
        executor.submit(() -> upsert(uuid, name, "games_played"));
    }

    public void updateHighestLevel(UUID uuid, String name, int level) {
        executor.submit(() -> {
            try {
                ensurePlayer(uuid, name);
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE player_stats SET highest_level = MAX(highest_level, ?) WHERE uuid = ?")) {
                    ps.setInt(1, level);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void upsert(UUID uuid, String name, String column) {
        try {
            ensurePlayer(uuid, name);
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE player_stats SET " + column + " = " + column + " + 1 WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void ensurePlayer(UUID uuid, String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO player_stats (uuid, name) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_stats SET name = ? WHERE uuid = ?")) {
            ps.setString(1, name);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }
}
