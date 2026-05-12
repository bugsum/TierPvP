package xyz.bugsum.tierpvp.listener;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import xyz.bugsum.tierpvp.manager.CombatManager;
import xyz.bugsum.tierpvp.manager.GameManager;
import xyz.bugsum.tierpvp.manager.PlayerManager;
import xyz.bugsum.tierpvp.module.Arena;
import xyz.bugsum.tierpvp.util.MessageUtil;

public class ArenaSpawnListener implements Listener {
    private final GameManager gameManager;
    private final PlayerManager playerManager;
    private final CombatManager combatManager;
    private final MessageUtil message;

    public ArenaSpawnListener(GameManager gameManager, PlayerManager playerManager, CombatManager combatManager, MessageUtil message) {
        this.gameManager = gameManager;
        this.playerManager = playerManager;
        this.combatManager = combatManager;
        this.message = message;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamageInSpawnArea(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        // Resolve the real attacker: could be a direct Player hit or a Projectile shot by a Player
        Player attacker = resolveAttacker(event);
        if (attacker == null) return;

        if (!playerManager.isPlaying(victim.getUniqueId()) || !playerManager.isPlaying(attacker.getUniqueId())) return;
        if (gameManager.getSession() == null) return;

        Arena arena = gameManager.getSession().getArena();
        if (arena == null) return;

        // Block damage FROM anyone inside spawn who is not combat-tagged
        // This prevents spawn-camping: you can't hit people from inside the safe zone
        if (arena.isInSpawnArea(attacker.getLocation())) {
            if (!combatManager.isTagged(attacker.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }

        // Block damage TO anyone inside spawn who is not combat-tagged
        // This prevents sneaking near spawn to hit protected players
        if (arena.isInSpawnArea(victim.getLocation())) {
            if (!combatManager.isTagged(victim.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Resolves the actual Player attacker from the damage event.
     * Handles both direct melee hits (damager is Player) and
     * projectile hits (damager is Arrow/Trident/etc. shot by a Player).
     */
    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }

        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }

        return null;
    }
}
