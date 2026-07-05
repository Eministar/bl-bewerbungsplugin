package dev.eministar.command;

import dev.eministar.SpawnItemPlugin;
import dev.eministar.config.ItemDefinition;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SpawnItemCommand implements CommandExecutor, TabCompleter {
    private final SpawnItemPlugin plugin;

    public SpawnItemCommand(SpawnItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.pluginConfig().send(sender, "only-players");
            return true;
        }

        if (args.length != 1) {
            plugin.pluginConfig().send(player, "spawnitem-usage");
            return true;
        }

        String itemName = args[0];
        ItemDefinition itemDefinition = plugin.pluginConfig().item(itemName);
        if (itemDefinition == null) {
            plugin.pluginConfig().send(player, "item-not-found", Map.of("item", itemName));
            return true;
        }

        if (!plugin.pluginConfig().canUseItem(player, itemDefinition)) {
            plugin.pluginConfig().send(player, "no-permission");
            return true;
        }

        Duration cooldown = plugin.pluginConfig().cooldownFor(player, itemDefinition);
        if (!cooldown.isZero()) {
            Duration remainingCooldown = plugin.cooldownService().remaining(player.getUniqueId(), itemDefinition.key());
            if (!remainingCooldown.isZero()) {
                plugin.pluginConfig().send(player, "cooldown-active", Map.of(
                        "item", itemDefinition.displayNamePlain(),
                        "seconds", String.valueOf(Math.max(1, (remainingCooldown.toMillis() + 999) / 1000))
                ));
                return true;
            }
        }

        ItemStack itemStack = itemDefinition.createItem();
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));

        plugin.cooldownService().start(player.getUniqueId(), itemDefinition.key(), cooldown);

        if (leftovers.isEmpty()) {
            plugin.pluginConfig().send(player, "item-received", Map.of("item", itemDefinition.displayNamePlain()));
        } else {
            plugin.pluginConfig().send(player, "item-dropped", Map.of("item", itemDefinition.displayNamePlain()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        String input = args[0].toLowerCase();
        List<String> suggestions = new ArrayList<>();

        for (ItemDefinition itemDefinition : plugin.pluginConfig().items()) {
            if (sender instanceof Player player && !plugin.pluginConfig().canUseItem(player, itemDefinition)) {
                continue;
            }

            String key = itemDefinition.key();
            if (key.startsWith(input)) {
                suggestions.add(key);
            }
        }

        return suggestions;
    }
}
