package xyz.bugsum.tierpvp.module;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import xyz.bugsum.tierpvp.util.ArenaSpawn;
import xyz.bugsum.tierpvp.util.Tier;
import xyz.bugsum.tierpvp.util.TierItem;

import java.util.Map;
import java.util.UUID;

public class GamePlayer {
    @Getter
    private final UUID uuid;
    @Getter
    private int level;
    @Getter
    private int kills;
    @Getter
    private int deaths;
    @Getter
    private int killStreak;
    private boolean isInSpawn;
    @Setter
    @Getter
    private int team;
    @Setter
    @Getter
    private boolean scoreboardDirty;

    public GamePlayer(UUID uuid) {
        this.uuid = uuid;
        this.level = 1;
        this.kills = 0;
        this.deaths = 0;
        this.isInSpawn = false;
        this.team = -1;
        this.scoreboardDirty = true;
    }

    public void reset() {
        this.level = 1;
        this.kills = 0;
        this.deaths = 0;
        this.killStreak = 0;
        this.isInSpawn = false;
        this.scoreboardDirty = true;
    }

    public void promote() {
        this.level++;
        this.kills++;
        this.killStreak++;
        this.scoreboardDirty = true;
    }

    public void demote(int minLevel) {
        this.level = Math.max(minLevel, this.level - 1);
        this.deaths++;
        this.killStreak = 0;
        this.scoreboardDirty = true;
    }

    public void setLevel(int level) {
        this.level = level;
        this.scoreboardDirty = true;
    }

    public void setIsInSpawn(ArenaSpawn spawnZone, Player player) {
        this.isInSpawn = spawnZone.contains(player.getLocation());
    }

    public boolean getIsInSpawn() {
        return isInSpawn;
    }
}
