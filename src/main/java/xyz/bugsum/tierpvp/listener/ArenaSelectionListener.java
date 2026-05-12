package xyz.bugsum.tierpvp.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import xyz.bugsum.tierpvp.manager.ArenaManager;
import xyz.bugsum.tierpvp.manager.ConfigManager;
import xyz.bugsum.tierpvp.util.ArenaSpawn;
import xyz.bugsum.tierpvp.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArenaSelectionListener implements Listener {
    private final ArenaManager arenaManager;
    private final ConfigManager configManager;
    private final MessageUtil message;

    private final Map<UUID, ArenaSelection> selections = new HashMap<>();

    public ArenaSelectionListener(ArenaManager arenaManager, ConfigManager configManager, MessageUtil message) {
        this.arenaManager = arenaManager;
        this.configManager = configManager;
        this.message = message;
    }

    public void startSelection(@NotNull Player player, String arenaName) {
        selections.put(player.getUniqueId(), new ArenaSelection(arenaName));

        ItemStack wand = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = wand.getItemMeta();
        meta.displayName(Component.text("Zone Selector", NamedTextColor.GOLD));
        wand.setItemMeta(meta);
        player.getInventory().addItem(wand);

        message.send(player, "arena.selection-tool");
    }

    public boolean hasSelection(UUID uuid) {
        return selections.containsKey(uuid);
    }

    public ArenaSelection getSelection(UUID uuid) {
        return selections.get(uuid);
    }

    public void confirmSelection(@NotNull Player player) {
        ArenaSelection sel = selections.get(player.getUniqueId());
        if (sel == null || sel.pos1 == null || sel.pos2 == null) return;

        arenaManager.getArena(sel.arenaName).ifPresent(arena -> {
            arena.setArenaSpawn(new ArenaSpawn(sel.pos1, sel.pos2));
            configManager.saveArenas(arenaManager);
            message.send(player, "arena.selection-confirmation", "arena", sel.arenaName);
        });

        removeWand(player);
        selections.remove(player.getUniqueId());
    }

    public void cancelSelection(Player player) {
        removeWand(player);
        selections.remove(player.getUniqueId());
        message.send(player, "arena.selection-cancellation");
    }

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ArenaSelection sel = selections.get(player.getUniqueId());
        if (sel == null) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.GOLDEN_AXE) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.displayName() == null) return;

        event.setCancelled(true);

        if (event.getClickedBlock() == null) return;
        Location loc = event.getClickedBlock().getLocation();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            sel.pos1 = loc;
            message.send(player, "arena.selection-pos1");
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            sel.pos2 = loc;
            message.send(player, "arena.selection-pos2");
        }
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        selections.remove(event.getPlayer().getUniqueId());
    }

    private void removeWand(@NotNull Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.GOLDEN_AXE) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.displayName() != null) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
    }

    public static class ArenaSelection {
        public final String arenaName;
        public Location pos1;
        public Location pos2;

        public ArenaSelection(String arenaName) {
            this.arenaName = arenaName;
        }

        public boolean isComplete() {
            return pos1 != null && pos2 != null;
        }
    }
}
