package dev.eministar.config;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.List;
import java.util.Map;
public record ItemDefinition(
        String key,
        Material material,
        int amount,
        Duration cooldown,
        String permission,
        String displayName,
        List<String> lore,
        Map<Enchantment, Integer> enchantments
) {
    public ItemStack createItem() {
        ItemStack itemStack = new ItemStack(material, amount);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            if (displayName != null && !displayName.isBlank()) {
                itemMeta.setDisplayName(color(displayName));
            }
            if (!lore.isEmpty()) {
                itemMeta.setLore(lore.stream().map(ItemDefinition::color).toList());
            }
            enchantments.forEach((enchantment, level) -> itemMeta.addEnchant(enchantment, level, true));
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    public String displayNamePlain() {
        if (displayName == null || displayName.isBlank()) {
            return key;
        }
        return ChatColor.stripColor(color(displayName));
    }
    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}