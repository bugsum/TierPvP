package xyz.bugsum.tierpvp.listener;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.bugsum.tierpvp.manager.CombatManager;
import xyz.bugsum.tierpvp.manager.ConfigManager;
import xyz.bugsum.tierpvp.manager.GameManager;
import xyz.bugsum.tierpvp.manager.PlayerManager;
import xyz.bugsum.tierpvp.module.GamePlayer;

public class CombatListener implements Listener {
    private final CombatManager combatManager;
    private final PlayerManager playerManager;
    private final GameManager gameManager;
    private final ConfigManager configManager;

    public CombatListener(CombatManager combatManager, PlayerManager playerManager, GameManager gameManager, ConfigManager configManager) {
        this.combatManager = combatManager;
        this.playerManager = playerManager;
        this.gameManager = gameManager;
        this.configManager = configManager;
    }

    @EventHandler(ignoreCancelled = true)
    private void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        // Resolve attacker: direct Player hit or Projectile shot by a Player
        Player attacker = resolveAttacker(event);
        if (attacker == null) return;

        if (!playerManager.isPlaying(victim.getUniqueId()) || !playerManager.isPlaying(attacker.getUniqueId())) return;

        if (gameManager.getSession() == null || !gameManager.getSession().isActive()) {
            event.setCancelled(true);
            return;
        }

        GamePlayer attackerGp = playerManager.getPlayer(attacker.getUniqueId()).orElse(null);
        GamePlayer victimGp = playerManager.getPlayer(victim.getUniqueId()).orElse(null);

        if (attackerGp == null || victimGp == null) return;

        if (configManager.isTeamMode() && !configManager.isFriendlyFire()
                && attackerGp.getTeam() == victimGp.getTeam() && attackerGp.getTeam() != -1) {
            event.setCancelled(true);
            return;
        }

        combatManager.tagBothPlayers(attacker.getUniqueId(), victim.getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!playerManager.isPlaying(victim.getUniqueId())) return;

        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.deathMessage(null);

        Player killer = victim.getKiller();

        if (killer != null && playerManager.isPlaying(killer.getUniqueId())) {
            gameManager.handleKill(killer, victim);
        } else {
            gameManager.handleEnvironmentDeath(victim);
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
