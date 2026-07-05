package dev.eministar;

import dev.eministar.command.MyPluginCommand;
import dev.eministar.command.InfoCommand;
import dev.eministar.command.SpawnItemCommand;
import dev.eministar.config.ConfigUpdater;
import dev.eministar.config.PluginConfig;
import dev.eministar.cooldown.CooldownService;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnItemPlugin extends JavaPlugin {
    private PluginConfig pluginConfig;
    private CooldownService cooldownService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.cooldownService = new CooldownService(this);
        this.pluginConfig = new PluginConfig(this);
        reloadPluginConfig();

        registerCommands();
        pluginConfig.sendStartupMessage();
        getLogger().info("BL-Bewerbungsplugin wurde aktiviert.");
    }

    @Override
    public void onDisable() {
        if (cooldownService != null) {
            cooldownService.save();
        }

        getLogger().info("BL-Bewerbungsplugin wurde deaktiviert.");
    }

    public void reloadPluginConfig() {
        ConfigUpdater.update(this);
        reloadConfig();
        pluginConfig.load();
    }

    public PluginConfig pluginConfig() {
        return pluginConfig;
    }

    public CooldownService cooldownService() {
        return cooldownService;
    }

    private void registerCommands() {
        SpawnItemCommand spawnItemCommand = new SpawnItemCommand(this);
        registerCommand("spawnitem", spawnItemCommand, spawnItemCommand);

        MyPluginCommand myPluginCommand = new MyPluginCommand(this);
        registerCommand("myplugin", myPluginCommand, myPluginCommand);

        InfoCommand infoCommand = new InfoCommand(this);
        registerCommand("info", infoCommand, infoCommand);
    }

    private void registerCommand(String commandName, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = getCommand(commandName);
        if (command == null) {
            throw new IllegalStateException("Der Befehl /" + commandName + " ist nicht in der plugin.yml registriert.");
        }

        command.setExecutor(executor);
        command.setTabCompleter(tabCompleter);
    }
}
