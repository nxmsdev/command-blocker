package dev.nxms.commandblock;

import dev.nxms.commandblock.command.CommandBlockCommand;
import dev.nxms.commandblock.command.CommandBlockTabCompleter;
import dev.nxms.commandblock.listener.CommandListener;
import dev.nxms.commandblock.manager.BlockedCommandManager;
import dev.nxms.commandblock.manager.MessageManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for CommandBlock.
 * Handles initialization and management of all plugin components.
 */
public class CommandBlock extends JavaPlugin {

    private static CommandBlock instance;
    private BlockedCommandManager blockedCommandManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("messages_en.yml", false);
        saveResource("messages_pl.yml", false);

        messageManager = new MessageManager(this);
        blockedCommandManager = new BlockedCommandManager(this);

        registerCommands();
        registerListeners();

        getLogger().info("CommandBlock has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CommandBlock has been disabled!");
    }

    /**
     * Registers plugin commands and tab completers.
     */
    private void registerCommands() {
        PluginCommand command = getCommand("commandblock");
        if (command != null) {
            command.setExecutor(new CommandBlockCommand(this));
            command.setTabCompleter(new CommandBlockTabCompleter(this));
        }

        getLogger().info("Commands has been registered.");
    }

    /**
     * Registers event listeners.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new CommandListener(this), this);
        getLogger().info("Listeners has been registered");
    }

    /**
     * Reloads all plugin configuration and managers.
     */
    public void reload() {
        reloadConfig();
        messageManager.reload();
        blockedCommandManager.reload();
        updateCommandsForAllPlayers();

        getLogger().info("CommandBlock plugin has been reloaded.");
    }

    /**
     * Updates command list for all online players.
     * Forces client to refresh available commands.
     */
    public void updateCommandsForAllPlayers() {
        for (Player player : getServer().getOnlinePlayers()) {
            player.updateCommands();
        }
    }

    public static CommandBlock getInstance() {
        return instance;
    }

    public BlockedCommandManager getBlockedCommandManager() {
        return blockedCommandManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
}