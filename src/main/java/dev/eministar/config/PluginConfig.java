package dev.eministar.config;

import dev.eministar.SpawnItemPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PluginConfig {
    private static final String DEFAULT_PREFIX = "&8[&aBewerbung&8] &r";
    private static final String DEFAULT_COOLDOWN_BYPASS_PERMISSION = "bewerbungsplugin.cooldown.bypass";
    private static final String COOLDOWN_PERMISSION_PREFIX = "bewerbungsplugin.cooldown.";
    private static final String SPAWN_ITEM_WILDCARD_PERMISSION = "bewerbungsplugin.spawnitem.*";

    private final SpawnItemPlugin plugin;
    private final Map<String, ItemDefinition> items = new LinkedHashMap<>();

    private String prefix = DEFAULT_PREFIX;
    private String cooldownBypassPermission = DEFAULT_COOLDOWN_BYPASS_PERMISSION;

    public PluginConfig(SpawnItemPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        items.clear();
        checkConfigVersion();

        prefix = plugin.getConfig().getString("settings.prefix", DEFAULT_PREFIX);
        cooldownBypassPermission = plugin.getConfig().getString(
                "settings.cooldown-bypass-permission",
                DEFAULT_COOLDOWN_BYPASS_PERMISSION
        );

        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("items");
        if (itemsSection == null) {
            plugin.getLogger().warning("In der config.yml fehlt der Abschnitt 'items'. Es wurden keine Items geladen.");
            return;
        }

        for (String itemKey : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
            if (itemSection == null) {
                plugin.getLogger().warning("Item '" + itemKey + "' ist kein gültiger Config-Abschnitt und wurde übersprungen.");
                continue;
            }

            ItemDefinition itemDefinition = readItem(itemKey, itemSection);
            if (itemDefinition != null) {
                items.put(itemDefinition.key(), itemDefinition);
            }
        }

        plugin.getLogger().info(items.size() + " Item(s) aus der config.yml geladen.");
    }

    public ItemDefinition item(String itemName) {
        if (itemName == null) {
            return null;
        }

        return items.get(normalize(itemName));
    }

    public Collection<ItemDefinition> items() {
        return items.values().stream()
                .sorted(Comparator.comparing(ItemDefinition::key))
                .toList();
    }

    public String cooldownBypassPermission() {
        return cooldownBypassPermission;
    }

    public Duration cooldownFor(Player player, ItemDefinition itemDefinition) {
        if (player.hasPermission(cooldownBypassPermission)) {
            return Duration.ZERO;
        }

        Integer permissionCooldownSeconds = cooldownFromPermission(player);
        if (permissionCooldownSeconds != null) {
            return Duration.ofSeconds(permissionCooldownSeconds);
        }

        return itemDefinition.cooldown();
    }

    public boolean canUseItem(Player player, ItemDefinition itemDefinition) {
        return player.hasPermission(SPAWN_ITEM_WILDCARD_PERMISSION) || player.hasPermission(itemDefinition.permission());
    }

    public void sendStartupMessage() {
        if (!plugin.getConfig().getBoolean("startup-message.enabled", true)) {
            return;
        }

        Map<String, String> placeholders = Map.of(
                "version", plugin.getDescription().getVersion(),
                "configVersion", String.valueOf(configVersion()),
                "items", String.valueOf(items.size())
        );

        for (String line : plugin.getConfig().getStringList("startup-message.lines")) {
            String formattedLine = line;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                formattedLine = formattedLine.replace("{" + entry.getKey() + "}", entry.getValue());
            }

            plugin.getServer().getConsoleSender().sendMessage(color(formattedLine));
        }
    }

    public void send(CommandSender sender, String messageKey) {
        send(sender, messageKey, Map.of());
    }

    public void send(CommandSender sender, String messageKey, Map<String, String> placeholders) {
        String message = plugin.getConfig().getString("messages." + messageKey);
        if (message == null || message.isBlank()) {
            message = "&cNachricht fehlt in der config.yml: " + messageKey;
        }

        String formatted = prefix + message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            formatted = formatted.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        sender.sendMessage(color(formatted));
        playSound(sender, messageKey);
    }

    private void checkConfigVersion() {
        int configVersion = configVersion();
        if (configVersion == ConfigUpdater.CONFIG_VERSION) {
            return;
        }

        if (configVersion < ConfigUpdater.CONFIG_VERSION) {
            plugin.getLogger().warning("Die config.yml ist veraltet. Gefunden: " + configVersion
                    + ", erwartet: " + ConfigUpdater.CONFIG_VERSION + ". Bitte neue Optionen aus der Standard-Config übernehmen.");
            return;
        }

        plugin.getLogger().warning("Die config.yml ist neuer als diese Plugin-Version. Gefunden: " + configVersion
                + ", unterstützt: " + ConfigUpdater.CONFIG_VERSION + ".");
    }

    private int configVersion() {
        return plugin.getConfig().getInt("config-version", 0);
    }

    private Integer cooldownFromPermission(Player player) {
        Integer bestCooldown = null;

        for (PermissionAttachmentInfo permissionInfo : player.getEffectivePermissions()) {
            if (!permissionInfo.getValue()) {
                continue;
            }

            String permission = permissionInfo.getPermission().toLowerCase(Locale.ROOT);
            if (!permission.startsWith(COOLDOWN_PERMISSION_PREFIX)) {
                continue;
            }

            String rawSeconds = permission.substring(COOLDOWN_PERMISSION_PREFIX.length());
            if (!rawSeconds.matches("\\d+")) {
                continue;
            }

            int seconds;
            try {
                seconds = Integer.parseInt(rawSeconds);
            } catch (NumberFormatException ignored) {
                continue;
            }

            if (bestCooldown == null || seconds < bestCooldown) {
                bestCooldown = seconds;
            }
        }

        return bestCooldown;
    }

    private ItemDefinition readItem(String itemKey, ConfigurationSection itemSection) {
        String normalizedKey = normalize(itemKey);
        String materialName = itemSection.getString("material");

        if (materialName == null || materialName.isBlank()) {
            plugin.getLogger().warning("Item '" + itemKey + "' hat kein Material und wurde übersprungen.");
            return null;
        }

        Material material = Material.matchMaterial(materialName);
        if (material == null || !material.isItem()) {
            plugin.getLogger().warning("Item '" + itemKey + "' nutzt ein ungültiges Material: " + materialName);
            return null;
        }

        int amount = clamp(itemSection.getInt("amount", 1), 1, material.getMaxStackSize());
        int cooldownSeconds = Math.max(0, itemSection.getInt("cooldown", 0));
        String permission = itemSection.getString("permission", "bewerbungsplugin.spawnitem." + normalizedKey);
        String displayName = itemSection.getString("display-name");
        List<String> lore = itemSection.getStringList("lore");
        Map<Enchantment, Integer> enchantments = readEnchantments(itemKey, itemSection);

        return new ItemDefinition(
                normalizedKey,
                material,
                amount,
                Duration.ofSeconds(cooldownSeconds),
                permission,
                displayName,
                lore,
                enchantments
        );
    }

    private Map<Enchantment, Integer> readEnchantments(String itemKey, ConfigurationSection itemSection) {
        Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
        ConfigurationSection enchantmentSection = itemSection.getConfigurationSection("enchantments");

        if (enchantmentSection == null) {
            return enchantments;
        }

        for (String enchantmentName : enchantmentSection.getKeys(false)) {
            Enchantment enchantment = findEnchantment(enchantmentName);
            if (enchantment == null) {
                plugin.getLogger().warning("Item '" + itemKey + "' nutzt eine ungültige Verzauberung: " + enchantmentName);
                continue;
            }

            int level = Math.max(1, enchantmentSection.getInt(enchantmentName, 1));
            enchantments.put(enchantment, level);
        }

        return enchantments;
    }

    private Enchantment findEnchantment(String enchantmentName) {
        String normalized = enchantmentName.toLowerCase(Locale.ROOT).replace(' ', '_');
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(legacyEnchantmentAlias(normalized)));
    }

    private String legacyEnchantmentAlias(String enchantmentName) {
        return switch (enchantmentName) {
            case "arrow_damage" -> "power";
            case "arrow_fire" -> "flame";
            case "arrow_infinite" -> "infinity";
            case "arrow_knockback" -> "punch";
            case "damage_all" -> "sharpness";
            case "damage_arthropods" -> "bane_of_arthropods";
            case "damage_undead" -> "smite";
            case "dig_speed" -> "efficiency";
            case "durability" -> "unbreaking";
            case "fire_aspect" -> "fire_aspect";
            case "loot_bonus_blocks" -> "fortune";
            case "loot_bonus_mobs" -> "looting";
            case "oxygen" -> "respiration";
            case "protection_environmental" -> "protection";
            case "protection_explosions" -> "blast_protection";
            case "protection_fall" -> "feather_falling";
            case "protection_fire" -> "fire_protection";
            case "protection_projectile" -> "projectile_protection";
            case "water_worker" -> "aqua_affinity";
            default -> enchantmentName;
        };
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private void playSound(CommandSender sender, String messageKey) {
        if (!(sender instanceof Player player) || !plugin.getConfig().getBoolean("sounds.enabled", true)) {
            return;
        }

        String path = "sounds.messages." + messageKey;
        String soundName = plugin.getConfig().getString(path + ".name");
        if (soundName == null || soundName.isBlank() || soundName.equalsIgnoreCase("none")) {
            return;
        }

        Sound sound = resolveSound(soundName);
        if (sound == null) {
            plugin.getLogger().warning("Ungültiger Sound in der config.yml bei '" + path + ".name': " + soundName);
            return;
        }

        float volume = (float) plugin.getConfig().getDouble(path + ".volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble(path + ".pitch", 1.0);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    @SuppressWarnings("deprecation")
    private Sound resolveSound(String soundName) {
        Sound sound = Registry.SOUNDS.match(soundName);
        if (sound != null) {
            return sound;
        }

        String normalizedKey = soundName.toLowerCase(Locale.ROOT).replace('_', '.');
        sound = Registry.SOUNDS.get(NamespacedKey.minecraft(normalizedKey));
        if (sound != null) {
            return sound;
        }

        try {
            return Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
