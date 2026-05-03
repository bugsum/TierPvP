package xyz.bugsum.tierpvp.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import xyz.bugsum.tierpvp.manager.CombatManager;

public class CombatListener implements Listener {
    private final CombatManager combatManager;

    public CombatListener(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    @EventHandler
    private void onDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity attacker = event.getDamager();

        if (!(victim instanceof Player)) return;
        if (!(attacker instanceof Player)) return;

        combatManager.tagBothPlayers(attacker.getUniqueId(), victim.getUniqueId());
        attacker.sendMessage(MiniMessage.miniMessage().deserialize("<gold>You have been combat tagged!</gold>"));
    }
}
