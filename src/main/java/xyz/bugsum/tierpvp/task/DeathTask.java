package xyz.bugsum.tierpvp.task;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.bugsum.tierpvp.manager.GameManager;
import xyz.bugsum.tierpvp.manager.PlayerManager;
import xyz.bugsum.tierpvp.module.Arena;
import xyz.bugsum.tierpvp.module.GamePlayer;
import xyz.bugsum.tierpvp.module.GameSession;

public class DeathTask extends BukkitRunnable {
    private final GameManager gameManager;
    private final PlayerManager playerManager;

    public DeathTask(GameManager gameManager, PlayerManager playerManager) {
        this.gameManager = gameManager;
        this.playerManager = playerManager;
    }

    @Override
    public void run() {
        GameSession session = gameManager.getSession();
        if (session == null || !session.isActive()) return;

        Arena arena = session.getArena();
        if (arena == null) return;

        for (GamePlayer gp : playerManager.getAllPlayers()) {
            Player player = Bukkit.getPlayer(gp.getUuid());
            if (player == null || !player.isOnline() || player.isDead()) continue;

            Material blockType = player.getLocation().getBlock().getType();

            boolean inWater = blockType == Material.WATER;
            boolean inVoid = player.getLocation().getY() < arena.getArenaVoid();

            if (inWater || inVoid) {
                player.setHealth(0);
            }
        }
    }
}
