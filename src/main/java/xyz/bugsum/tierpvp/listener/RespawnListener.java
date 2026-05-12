package xyz.bugsum.tierpvp.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import xyz.bugsum.tierpvp.manager.ConfigManager;
import xyz.bugsum.tierpvp.manager.GameManager;
import xyz.bugsum.tierpvp.manager.PlayerManager;
import xyz.bugsum.tierpvp.module.GamePlayer;
import xyz.bugsum.tierpvp.module.Tiers;
import xyz.bugsum.tierpvp.util.PlayerKit;

import java.util.Optional;

public class RespawnListener implements Listener {
    private final GameManager gameManager;
    private final PlayerManager playerManager;
    private final ConfigManager configManager;

    public RespawnListener(GameManager gameManager, PlayerManager playerManager, ConfigManager configManager) {
        this.gameManager = gameManager;
        this.playerManager = playerManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!gameManager.hasActiveGame()) return;
        if (gameManager.getSession() == null || gameManager.getSession().getArena() == null) return;

        Player player = event.getPlayer();
        Optional<GamePlayer> optionalGamePlayer = playerManager.getPlayer(player.getUniqueId());

        if (optionalGamePlayer.isEmpty()) return;

        GamePlayer gamePlayer = optionalGamePlayer.get();
        Tiers tiers = configManager.getTiers();

        event.setRespawnLocation(gameManager.getSession().getArena().getSpawn());

        player.setFoodLevel(20);
        player.setSaturation(20f);
        tiers.getLevel(gamePlayer.getLevel()).ifPresent(tier -> PlayerKit.apply(player, tier));
    }
}
