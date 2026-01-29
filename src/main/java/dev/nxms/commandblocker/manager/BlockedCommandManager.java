package dev.nxms.commandblocker.manager;

import dev.nxms.commandblocker.CommandBlocker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages the list of blocked commands.
 * Handles adding, removing, and checking blocked commands.
 */
public class BlockedCommandManager {

    private final CommandBlocker plugin;
    private final Set<String> blockedCommands;

    public BlockedCommandManager(CommandBlocker plugin) {
        this.plugin = plugin;
        this.blockedCommands = new HashSet<>();
        reload();
    }

    /**
     * Reloads blocked commands from config.
     */
    public void reload() {
        blockedCommands.clear();
        List<String> commands = plugin.getConfig().getStringList("blocked-commands");
        for (String command : commands) {
            blockedCommands.add(command.toLowerCase());
        }

        plugin.getLogger().info("Blocked Commands has been reloaded.");
    }

    /**
     * Saves blocked commands to config.
     */
    private void save() {
        plugin.getConfig().set("blocked-commands", blockedCommands.stream().sorted().toList());
        plugin.saveConfig();
    }

    /**
     * Adds a command to the blocked list.
     * Returns true if added, false if already exists.
     */
    public boolean add(String command) {
        String lowerCommand = command.toLowerCase();
        if (blockedCommands.contains(lowerCommand)) {
            return false;
        }
        blockedCommands.add(lowerCommand);
        save();
        plugin.updateCommandsForAllPlayers();
        return true;
    }

    /**
     * Removes a command from the blocked list.
     * Returns true if removed, false if not found.
     */
    public boolean remove(String command) {
        String lowerCommand = command.toLowerCase();
        if (!blockedCommands.contains(lowerCommand)) {
            return false;
        }
        blockedCommands.remove(lowerCommand);
        save();
        plugin.updateCommandsForAllPlayers();
        return true;
    }

    /**
     * Checks if a command is blocked.
     * Supports both namespaced (plugin:command) and simple command formats.
     */
    public boolean isBlocked(String command) {
        String lowerCommand = command.toLowerCase();

        // Direct match
        if (blockedCommands.contains(lowerCommand)) {
            return true;
        }

        // Check if command matches any blocked command
        for (String blocked : blockedCommands) {
            if (blocked.contains(":")) {
                // Blocked command has namespace
                String blockedName = blocked.split(":")[1];

                if (lowerCommand.contains(":")) {
                    // Input also has namespace - exact match needed
                    if (blocked.equals(lowerCommand)) {
                        return true;
                    }
                } else {
                    // Input has no namespace - match command name
                    if (blockedName.equals(lowerCommand)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Returns a copy of the blocked commands set.
     */
    public Set<String> getBlockedCommands() {
        return new HashSet<>(blockedCommands);
    }

    /**
     * Checks if blocked list is empty.
     */
    public boolean isEmpty() {
        return blockedCommands.isEmpty();
    }
}