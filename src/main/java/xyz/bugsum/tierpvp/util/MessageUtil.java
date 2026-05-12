package xyz.bugsum.tierpvp.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageUtil {
    private static final MiniMessage messageFormatter = MiniMessage.miniMessage();
    private final Map<String, String> templates = new HashMap<>();
    private String prefix = "";

    public void load(File file) {
        templates.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        loadKeys(yaml, "");
        prefix = templates.getOrDefault("prefix", "");
    }

    private void loadKeys(org.bukkit.configuration.ConfigurationSection section, String parent) {
        for (String key : section.getKeys(false)) {
            String fullKey = parent.isEmpty() ? key : parent + "." + key;
            if (section.isConfigurationSection(key)) {
                loadKeys(section.getConfigurationSection(key), fullKey);
            } else {
                templates.put(fullKey, section.getString(key, ""));
            }
        }
    }

    public Component format(String key, String... placeholders) {
        String template = templates.getOrDefault(key, "<red>Missing message: " + key);
        template = template.replace("<prefix>", prefix);

        TagResolver.Builder resolver = TagResolver.builder();
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            resolver.resolver(Placeholder.parsed(placeholders[i], placeholders[i + 1]));
        }

        return messageFormatter.deserialize(template, resolver.build());
    }

    public void send(Player player, String key, String... placeholders) {
        player.sendMessage(format(key, placeholders));
    }

    public void sendActionBar(Player player, String key, String... placeholders) {
        player.sendActionBar(format(key, placeholders));
    }

    public Component parse(String miniMessageString) {
        return messageFormatter.deserialize(miniMessageString);
    }

    public String getRaw(String key) {
        return templates.getOrDefault(key, "");
    }

    public boolean has(String key) {
        return templates.containsKey(key);
    }
}
