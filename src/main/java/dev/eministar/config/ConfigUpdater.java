package dev.eministar.config;

import dev.eministar.SpawnItemPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ConfigUpdater {
    public static final int CONFIG_VERSION = 3;

    private ConfigUpdater() {
    }

    public static void update(SpawnItemPlugin plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            return;
        }

        YamlConfiguration currentConfig = loadCurrentConfig(plugin, configFile);
        YamlConfiguration defaultConfig = loadDefaultConfig(plugin);
        if (currentConfig == null || defaultConfig == null) {
            return;
        }

        boolean changed = false;
        int currentVersion = currentConfig.getInt("config-version", 0);
        if (currentVersion < CONFIG_VERSION) {
            currentConfig.set("config-version", CONFIG_VERSION);
            copyComments(defaultConfig, currentConfig, "config-version");
            changed = true;
        }

        boolean shouldRestoreExampleItems = !currentConfig.isConfigurationSection("items");
        for (String path : defaultConfig.getKeys(true)) {
            if (path.equals("config-version") || shouldSkipItemPath(path, shouldRestoreExampleItems)) {
                continue;
            }

            copyComments(defaultConfig, currentConfig, path);
            if (defaultConfig.isConfigurationSection(path) || currentConfig.isSet(path)) {
                continue;
            }

            currentConfig.set(path, defaultConfig.get(path));
            changed = true;
        }

        if (!changed) {
            return;
        }

        try {
            currentConfig.save(configFile);
            plugin.getLogger().info("config.yml wurde automatisch aktualisiert. Bestehende Werte wurden beibehalten.");
        } catch (IOException exception) {
            plugin.getLogger().severe("Die config.yml konnte nicht automatisch aktualisiert werden: " + exception.getMessage());
        }
    }

    private static YamlConfiguration loadCurrentConfig(SpawnItemPlugin plugin, File configFile) {
        YamlConfiguration config = new YamlConfiguration();
        config.options().parseComments(true);

        try {
            config.load(configFile);
            return config;
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Die config.yml konnte nicht gelesen werden: " + exception.getMessage());
            return null;
        }
    }

    private static YamlConfiguration loadDefaultConfig(SpawnItemPlugin plugin) {
        InputStream inputStream = plugin.getResource("config.yml");
        if (inputStream == null) {
            plugin.getLogger().severe("Die Standard-config.yml fehlt in der Plugin-Jar.");
            return null;
        }

        YamlConfiguration config = new YamlConfiguration();
        config.options().parseComments(true);

        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            config.load(reader);
            return config;
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Die Standard-config.yml konnte nicht gelesen werden: " + exception.getMessage());
            return null;
        }
    }

    private static boolean shouldSkipItemPath(String path, boolean shouldRestoreExampleItems) {
        return path.startsWith("items.") && !shouldRestoreExampleItems;
    }

    private static void copyComments(YamlConfiguration source, YamlConfiguration target, String path) {
        List<String> comments = source.getComments(path);
        if (!comments.isEmpty() && target.getComments(path).isEmpty()) {
            target.setComments(path, comments);
        }

        List<String> inlineComments = source.getInlineComments(path);
        if (!inlineComments.isEmpty() && target.getInlineComments(path).isEmpty()) {
            target.setInlineComments(path, inlineComments);
        }
    }
}
