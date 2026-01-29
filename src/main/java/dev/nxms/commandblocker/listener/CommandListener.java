package dev.nxms.commandblocker.listener;

import dev.nxms.commandblocker.CommandBlocker;
import dev.nxms.commandblocker.manager.BlockedCommandManager;
import dev.nxms.commandblocker.manager.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Listens for command events and blocks restricted commands.
 * Makes blocked commands appear as if they don't exist.
 */
public class CommandListener implements Listener {

    private final BlockedCommandManager blockedManager;
    private final MessageManager messages;

    public CommandListener(CommandBlocker plugin) {
        this.blockedManager = plugin.getBlockedCommandManager();
        this.messages = plugin.getMessageManager();
    }

    /**
     * Intercepts commands before execution and blocks restricted ones.
     * Sends "unknown command" message to make it appear as non-existent.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (canBypass(player)) {
            return;
        }

        String message = event.getMessage();
        String commandLine = message.substring(1); // Remove leading slash
        String commandName = commandLine.split(" ")[0].toLowerCase();

        if (blockedManager.isBlocked(commandName)) {
            event.setCancelled(true);
            messages.sendRaw(player, "command-not-found");
        }
    }

    /**
     * Filters command suggestions sent to clients.
     * Removes blocked commands from tab completion.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();

        if (canBypass(player)) {
            return;
        }

        Set<String> toRemove = new HashSet<>();

        for (String command : event.getCommands()) {
            if (blockedManager.isBlocked(command)) {
                toRemove.add(command);
            }
        }

        event.getCommands().removeAll(toRemove);
    }

    /**
     * Checks if player can bypass command blocking.
     */
    private boolean canBypass(Player player) {
        return player.hasPermission("commandblocker.bypass");
    }
}