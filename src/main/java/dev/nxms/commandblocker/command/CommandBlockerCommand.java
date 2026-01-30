package dev.nxms.commandblocker.command;

import dev.nxms.commandblocker.CommandBlocker;
import dev.nxms.commandblocker.manager.BlockedCommandManager;
import dev.nxms.commandblocker.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Main command executor for /commandblocker.
 * Handles all subcommands: add, remove, list, reload, help.
 */
public class CommandBlockerCommand implements CommandExecutor {

    private final CommandBlocker plugin;
    private final MessageManager messages;
    private final BlockedCommandManager blockedManager;

    public CommandBlockerCommand(CommandBlocker plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessageManager();
        this.blockedManager = plugin.getBlockedCommandManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!hasBasePermission(sender)) {
            messages.send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            handleHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "help" -> handleHelp(sender);
            default -> messages.send(sender, "unknown-subcommand");
        }

        return true;
    }

    /**
     * Handles the add subcommand.
     */
    private void handleAdd(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "commandblocker.add")) {
            messages.send(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            messages.send(sender, "add.usage");
            return;
        }

        String cmd = args[1].toLowerCase();

        // Validate command format
        if (!isValidCommandFormat(cmd)) {
            messages.send(sender, "invalid-format");
            return;
        }

        if (blockedManager.add(cmd)) {
            messages.send(sender, "add.success", "%command%", cmd);
        } else {
            messages.send(sender, "add.already-blocked", "%command%", cmd);
        }
    }

    /**
     * Handles the remove subcommand.
     */
    private void handleRemove(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "commandblocker.remove")) {
            messages.send(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            messages.send(sender, "remove.usage");
            return;
        }

        String cmd = args[1].toLowerCase();

        // Validate command format
        if (!isValidCommandFormat(cmd)) {
            messages.send(sender, "invalid-format");
            return;
        }

        if (blockedManager.remove(cmd)) {
            messages.send(sender, "remove.success", "%command%", cmd);
        } else {
            messages.send(sender, "remove.not-found", "%command%", cmd);
        }
    }

    /**
     * Handles the list subcommand.
     */
    private void handleList(CommandSender sender) {
        if (!hasPermission(sender, "commandblocker.list")) {
            messages.send(sender, "no-permission");
            return;
        }

        messages.send(sender, "list.header");

        if (blockedManager.isEmpty()) {
            messages.send(sender, "list.empty");
        } else {
            for (String cmd : blockedManager.getBlockedCommands().stream().sorted().toList()) {
                messages.send(sender, "list.entry", "%command%", cmd);
            }
        }

        messages.send(sender, "list.footer");
    }

    /**
     * Handles the reload subcommand.
     */
    private void handleReload(CommandSender sender) {
        if (!hasPermission(sender, "commandblocker.reload")) {
            messages.send(sender, "no-permission");
            return;
        }

        plugin.reload();
        messages.send(sender, "reload.success");
    }

    /**
     * Handles the help subcommand.
     */
    private void handleHelp(CommandSender sender) {
        messages.send(sender, "help.header");

        if (hasPermission(sender, "commandblocker.add")) {
            messages.send(sender, "help.add");
        }
        if (hasPermission(sender, "commandblocker.remove")) {
            messages.send(sender, "help.remove");
        }
        if (hasPermission(sender, "commandblocker.list")) {
            messages.send(sender, "help.list");
        }
        if (hasPermission(sender, "commandblocker.reload")) {
            messages.send(sender, "help.reload");
        }
        messages.send(sender, "help.help");

        messages.send(sender, "help.footer");
    }

    /**
     * Validates command format.
     * Command must contain ":" to specify namespace (e.g., minecraft:me).
     */
    private boolean isValidCommandFormat(String command) {
        if (command == null || command.isEmpty()) {
            return false;
        }

        // Must contain exactly one ":" and not at start or end
        int colonIndex = command.indexOf(':');
        return colonIndex > 0 && colonIndex < command.length() - 1;
    }

    /**
     * Checks if sender has base permission to use the command.
     */
    private boolean hasBasePermission(CommandSender sender) {
        return sender.hasPermission("commandblocker.command") || sender.hasPermission("commandblocker.admin");
    }

    /**
     * Checks if sender has specific permission or admin permission.
     */
    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("commandblocker.admin");
    }
}