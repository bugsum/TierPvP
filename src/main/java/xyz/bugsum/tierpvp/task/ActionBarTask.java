package xyz.bugsum.tierpvp.task;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.bugsum.tierpvp.manager.CombatManager;
import xyz.bugsum.tierpvp.manager.GameManager;
import xyz.bugsum.tierpvp.manager.PlayerManager;
import xyz.bugsum.tierpvp.module.Arena;
import xyz.bugsum.tierpvp.module.GamePlayer;
import xyz.bugsum.tierpvp.module.GameSession;
import xyz.bugsum.tierpvp.util.MessageUtil;

public class ActionBarTask extends BukkitRunnable {
    private final GameManager gameManager;
    private final PlayerManager playerManager;
    private final CombatManager combatManager;
    private final MessageUtil message;

    public ActionBarTask(GameManager gameManager, PlayerManager playerManager, CombatManager combatManager, MessageUtil message) {
        this.gameManager = gameManager;
        this.playerManager = playerManager;
        this.combatManager = combatManager;
        this.message = message;
    }

    @Override
    public void run() {
        GameSession session = gameManager.getSession();
        if (session == null) return;

        Arena arena = session.getArena();
        if (arena == null) return;

        for (GamePlayer gp : playerManager.getAllPlayers()) {
            Player player = Bukkit.getPlayer(gp.getUuid());
            if (player == null || !player.isOnline()) continue;

            boolean inSpawnZone = arena.isInSpawnArea(player.getLocation());
            boolean combatTagged = combatManager.isTagged(player.getUniqueId());

            if (combatTagged) {
                long seconds = combatManager.getRemainingSeconds(player.getUniqueId());
                message.sendActionBar(player, "combat-tagged", "seconds", String.valueOf(seconds));
            } else if (inSpawnZone) {
                message.sendActionBar(player, "spawn-protection");
            }
        }
    }
}
