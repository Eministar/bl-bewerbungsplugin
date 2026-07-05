package dev.eministar.command;

import dev.eministar.SpawnItemPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public final class MyPluginCommand implements CommandExecutor, TabCompleter {
    private static final String RELOAD_PERMISSION = "bewerbungsplugin.admin.reload";

    private final SpawnItemPlugin plugin;

    public MyPluginCommand(SpawnItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(RELOAD_PERMISSION)) {
                plugin.pluginConfig().send(sender, "no-permission");
                return true;
            }

            plugin.reloadPluginConfig();
            plugin.pluginConfig().send(sender, "reload-success");
            return true;
        }

        plugin.pluginConfig().send(sender, "myplugin-usage");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission(RELOAD_PERMISSION) && "reload".startsWith(args[0].toLowerCase())) {
            return List.of("reload");
        }

        return List.of();
    }
}
