package xyz.bugsum.tierpvp.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import xyz.bugsum.tierpvp.module.Tiers;
import xyz.bugsum.tierpvp.util.Tier;
import xyz.bugsum.tierpvp.util.TierItem;

import java.io.File;
import java.util.*;

public class TiersConfig {
    @Contract("_ -> new")
    public static @NotNull Tiers load(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection levelsSection = yaml.getConfigurationSection("levels");
        if (levelsSection == null) return new Tiers(List.of());

        List<Tier> levels = new ArrayList<>();
        for (String key : levelsSection.getKeys(false)) {
            int index = Integer.parseInt(key);
            ConfigurationSection section = levelsSection.getConfigurationSection(key);
            if (section == null) continue;

            String displayName = section.getString("display-name", "Level " + index);
            TierItem weapon = parseItemConfig(section.getConfigurationSection("weapon"));
            List<TierItem> extras = parseExtrasList(section);
            Map<String, TierItem> armor = parseArmorMap(section.getConfigurationSection("armor"));

            levels.add(new Tier(index, displayName, weapon, extras, armor));
        }

        levels.sort(Comparator.comparingInt(Tier::index));
        return new Tiers(levels);
    }

    @Contract("null -> new")
    private static @NotNull TierItem parseItemConfig(ConfigurationSection section) {
        if (section == null) return new TierItem(Material.AIR, 1, 0, null, null, null, null);

        Material material = Material.matchMaterial(section.getString("material", "AIR"));
        if (material == null) material = Material.AIR;
        int amount = section.getInt("amount", 1);
        int slot = section.getInt("slot", 0);
        String name = section.getString("name", null);

        List<String> lore = section.getStringList("lore");
        Map<String, Integer> enchantments = parseEnchantments(section.getConfigurationSection("enchantments"));
        List<ItemFlag> itemFlags = parseItemFlags(section.getStringList("item-flags"));

        return new TierItem(material, amount, slot, name, lore, enchantments, itemFlags);
    }

    private static List<TierItem> parseExtrasList(ConfigurationSection levelSection) {
        List<TierItem> extras = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> extrasList = (List<Map<String, Object>>) (List<?>) levelSection.getMapList("extras");

        for (Map<String, Object> map : extrasList) {
            String materialName = (String) map.getOrDefault("material", "AIR");
            Material mat = Material.matchMaterial(materialName);

            if (mat == null) mat = Material.AIR;

            int amount = map.containsKey("amount") ? ((Number) map.get("amount")).intValue() : 1;
            int slot = map.containsKey("slot") ? ((Number) map.get("slot")).intValue() : 0;

            extras.add(new TierItem(mat, amount, slot, null, null, null, null));
        }
        return extras;
    }

    private static @NotNull Map<String, TierItem> parseArmorMap(ConfigurationSection armorSection) {
        Map<String, TierItem> armor = new LinkedHashMap<>();
        if (armorSection == null) return armor;

        for (String slot : List.of("helmet", "chestplate", "leggings", "boots")) {
            ConfigurationSection pieceSection = armorSection.getConfigurationSection(slot);
            if (pieceSection != null) {
                armor.put(slot, parseItemConfig(pieceSection));
            }
        }
        return armor;
    }

    private static @NotNull Map<String, Integer> parseEnchantments(ConfigurationSection section) {
        Map<String, Integer> enchants = new LinkedHashMap<>();
        if (section == null) return enchants;
        for (String key : section.getKeys(false)) {
            enchants.put(key.toLowerCase(), section.getInt(key));
        }
        return enchants;
    }

    private static @NotNull List<ItemFlag> parseItemFlags(@NotNull List<String> flags) {
        List<ItemFlag> result = new ArrayList<>();

        for (String flag : flags) {
            try {
                result.add(ItemFlag.valueOf(flag.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return result;
    }
}
