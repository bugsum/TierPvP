package xyz.bugsum.tierpvp.util;

import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class PlayerKit {
    private static final MiniMessage message = MiniMessage.miniMessage();

    public static void apply(@NotNull Player player, @NotNull Tier level) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);

        inventory.setItem(level.weapon().slot(), buildItem(level.weapon()));

        for (TierItem extra : level.extras()) {
            inventory.setItem(extra.slot(), buildItem(extra));
        }

        Map<String, TierItem> armor = level.armor();
        if (armor.containsKey("helmet")) inventory.setHelmet(buildItem(armor.get("helmet")));
        if (armor.containsKey("chestplate")) inventory.setChestplate(buildItem(armor.get("chestplate")));
        if (armor.containsKey("leggings")) inventory.setLeggings(buildItem(armor.get("leggings")));
        if (armor.containsKey("boots")) inventory.setBoots(buildItem(armor.get("boots")));

        player.updateInventory();
    }

    private static @NotNull ItemStack buildItem(@NotNull TierItem config) {
        ItemStack item = new ItemStack(config.material(), config.amount());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setUnbreakable(true);

        if (config.name() != null && !config.name().isEmpty()) {
            meta.displayName(message.deserialize(config.name()));
        }

        if (!config.lore().isEmpty()) {
            meta.lore(config.lore().stream()
                    .map(message::deserialize)
                    .toList());
        }

        for (Map.Entry<String, Integer> entry : config.enchantments().entrySet()) {
            NamespacedKey key = NamespacedKey.minecraft(entry.getKey());

            Enchantment enchant = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(key);

            if (enchant != null) {
                meta.addEnchant(enchant, entry.getValue(), true);
            }
        }

        for (ItemFlag flag : config.itemFlags()) {
            meta.addItemFlags(flag);
        }

        item.setItemMeta(meta);
        return item;
    }
}
