package dev.nxms.commandblocker;

import com.github.retrooper.packetevents.PacketEvents;
import dev.nxms.commandblocker.command.CommandBlockerCommand;
import dev.nxms.commandblocker.command.CommandBlockerTabCompleter;
import dev.nxms.commandblocker.listener.CommandListener;
import dev.nxms.commandblocker.listener.PacketListener;
import dev.nxms.commandblocker.manager.BlockedCommandManager;
import dev.nxms.commandblocker.manager.MessageManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for CommandBlocker.
 * Handles initialization and management of all plugin components.
 */
public class CommandBlocker extends JavaPlugin {

    private static CommandBlocker instance;
    private BlockedCommandManager blockedCommandManager;
    private MessageManager messageManager;
    private PacketListener packetListener;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        messageManager = new MessageManager(this);
        blockedCommandManager = new BlockedCommandManager(this);

        registerCommands();
        registerListeners();
        registerPacketListener();

        getLogger().info("CommandBlocker has been enabled!");
    }

    @Override
    public void onDisable() {
        unregisterPacketListener();
        getLogger().info("CommandBlocker has been disabled!");
    }

    /**
     * Registers plugin commands and tab completers.
     */
    private void registerCommands() {
        PluginCommand command = getCommand("commandblocker");
        if (command != null) {
            command.setExecutor(new CommandBlockerCommand(this));
            command.setTabCompleter(new CommandBlockerTabCompleter(this));
        }
        getLogger().info("Commands has been registered.");
    }

    /**
     * Registers event listeners.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new CommandListener(this), this);
        getLogger().info("Listeners has been registered.");
    }

    /**
     * Registers PacketEvents listener.
     */
    private void registerPacketListener() {
        packetListener = new PacketListener(this);
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);
        getLogger().info("PacketEvents listener has been registered.");
    }

    /**
     * Unregisters PacketEvents listener.
     */
    private void unregisterPacketListener() {
        if (packetListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
        }
    }

    /**
     * Reloads all plugin configuration and managers.
     */
    public void reload() {
        reloadConfig();
        messageManager.reload();
        blockedCommandManager.reload();
        updateCommandsForAllPlayers();
        getLogger().info("CommandBlocker plugin has been reloaded.");
    }

    /**
     * Updates command list for all online players.
     */
    public void updateCommandsForAllPlayers() {
        for (Player player : getServer().getOnlinePlayers()) {
            player.updateCommands();
        }
    }

    public static CommandBlocker getInstance() {
        return instance;
    }

    public BlockedCommandManager getBlockedCommandManager() {
        return blockedCommandManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
}