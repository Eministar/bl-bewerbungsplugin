package dev.eministar.cooldown;

import dev.eministar.SpawnItemPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CooldownService {
    private final SpawnItemPlugin plugin;
    private final File cooldownFile;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public CooldownService(SpawnItemPlugin plugin) {
        this.plugin = plugin;
        this.cooldownFile = new File(plugin.getDataFolder(), "cooldowns.yml");
        load();
    }

    public void start(UUID playerId, String itemKey, Duration cooldown) {
        if (cooldown.isZero() || cooldown.isNegative()) {
            clear(playerId, itemKey);
            return;
        }

        cooldowns.computeIfAbsent(playerId, ignored -> new HashMap<>())
                .put(itemKey, System.currentTimeMillis() + cooldown.toMillis());
        save();
    }

    public Duration remaining(UUID playerId, String itemKey) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return Duration.ZERO;
        }
        Long expiresAt = playerCooldowns.get(itemKey);
        if (expiresAt == null) {
            return Duration.ZERO;
        }
        long remainingMillis = expiresAt - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            clear(playerId, itemKey);
            return Duration.ZERO;
        }

        return Duration.ofMillis(remainingMillis);
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, Map<String, Long>> playerEntry : cooldowns.entrySet()) {
            for (Map.Entry<String, Long> cooldownEntry : playerEntry.getValue().entrySet()) {
                long expiresAt = cooldownEntry.getValue();
                if (expiresAt > now) {
                    config.set(playerEntry.getKey() + "." + cooldownEntry.getKey(), expiresAt);
                }
            }
        }

        try {
            config.save(cooldownFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("cooldowns.yml konnte nicht gespeichert werden: " + exception.getMessage());
        }
    }

    private void load() {
        cooldowns.clear();
        if (!cooldownFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(cooldownFile);
        long now = System.currentTimeMillis();

        for (String uuidKey : config.getKeys(false)) {
            UUID playerId = parseUuid(uuidKey);
            if (playerId == null) {
                plugin.getLogger().warning("Ungültige UUID in cooldowns.yml wurde übersprungen: " + uuidKey);
                continue;
            }

            ConfigurationSection playerSection = config.getConfigurationSection(uuidKey);
            if (playerSection == null) {
                continue;
            }

            for (String itemKey : playerSection.getKeys(false)) {
                long expiresAt = playerSection.getLong(itemKey, 0);
                if (expiresAt > now) {
                    cooldowns.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(itemKey, expiresAt);
                }
            }
        }

        save();
    }

    private void clear(UUID playerId, String itemKey) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return;
        }

        playerCooldowns.remove(itemKey);
        if (playerCooldowns.isEmpty()) {
            cooldowns.remove(playerId);
        }

        save();
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
