package dev.eministar.command;

import dev.eministar.SpawnItemPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public final class InfoCommand implements CommandExecutor, TabCompleter {
    private final SpawnItemPlugin plugin;

    public InfoCommand(SpawnItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 0) {
            plugin.pluginConfig().send(sender, "info-usage");
            return true;
        }

        plugin.pluginConfig().send(sender, "info");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
