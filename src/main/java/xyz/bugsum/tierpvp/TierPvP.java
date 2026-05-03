package xyz.bugsum.tierpvp;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.bugsum.tierpvp.listener.CombatListener;
import xyz.bugsum.tierpvp.manager.CombatManager;


public final class TierPvP extends JavaPlugin {
    @Override
    public void onEnable() {
        CombatManager combatManager = new CombatManager(10);

        getServer().getPluginManager().registerEvents(new CombatListener(combatManager), this);

        getLogger().info("TierPvP enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("TierPvP disabled!");
    }
}
