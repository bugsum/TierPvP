package xyz.bugsum.tierpvp.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.bugsum.tierpvp.TierPvP;
import xyz.bugsum.tierpvp.manager.ConfigManager;
import xyz.bugsum.tierpvp.manager.GameManager;

public class ConnectionListener implements Listener {
    private final TierPvP instance;
    private final GameManager gameManager;
    private final ConfigManager configManager;

    public ConnectionListener(TierPvP instance, GameManager gameManager, ConfigManager configManager) {
        this.instance = instance;
        this.gameManager = gameManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!configManager.isAutoJoinOnConnect()) return;

        Player player = event.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && gameManager.hasActiveGame()) {
                    gameManager.addPlayer(player);
                }
            }
        }.runTaskLater(instance, 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        gameManager.removePlayer(event.getPlayer());
    }
}
