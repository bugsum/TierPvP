package xyz.bugsum.tierpvp.util;

public record PlayerStats(
        String playerName,
        int kills,
        int deaths,
        int wins,
        int gamesPlayed,
        int highestLevel
) {
    public static PlayerStats empty(String playerName) {
        return new PlayerStats(playerName, 0, 0, 0, 0, 0);
    }
}
