package xyz.bugsum.tierpvp.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.List;
import java.util.Map;

public record TierItem(Material material, int amount, int slot, String name, List<String> lore, Map<String, Integer> enchantments, List<ItemFlag> itemFlags) {
    public TierItem {
        if (amount <= 0) amount = 1;
        if (lore == null) lore = List.of();
        if (enchantments == null) enchantments = Map.of();
        if (itemFlags == null) itemFlags = List.of();
    }
}
