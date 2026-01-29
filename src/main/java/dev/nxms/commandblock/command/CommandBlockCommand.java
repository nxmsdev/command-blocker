package dev.nxms.commandblock.command;

import dev.nxms.commandblock.CommandBlock;
import dev.nxms.commandblock.manager.BlockedCommandManager;
import dev.nxms.commandblock.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Main command executor for /commandblock.
 * Handles all subcommands: add, remove, list, reload, help.
 */
public class CommandBlockCommand implements CommandExecutor {

    private final CommandBlock plugin;
    private final MessageManager messages;
    private final BlockedCommandManager blockedManager;

    public CommandBlockCommand(CommandBlock plugin) {
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
        if (!hasPermission(sender, "commandblock.add")) {
            messages.send(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            messages.send(sender, "add.usage");
            return;
        }

        String cmd = args[1].toLowerCase();
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
        if (!hasPermission(sender, "commandblock.remove")) {
            messages.send(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            messages.send(sender, "remove.usage");
            return;
        }

        String cmd = args[1].toLowerCase();
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
        if (!hasPermission(sender, "commandblock.list")) {
            messages.send(sender, "no-permission");
            return;
        }

        messages.sendRaw(sender, "list.header");

        if (blockedManager.isEmpty()) {
            messages.sendRaw(sender, "list.empty");
        } else {
            for (String cmd : blockedManager.getBlockedCommands().stream().sorted().toList()) {
                messages.sendRaw(sender, "list.entry", "%command%", cmd);
            }
        }
    }

    /**
     * Handles the reload subcommand.
     */
    private void handleReload(CommandSender sender) {
        if (!hasPermission(sender, "commandblock.reload")) {
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
        messages.sendRaw(sender, "help.header");

        if (hasPermission(sender, "commandblock.add")) {
            messages.sendRaw(sender, "help.add");
        }
        if (hasPermission(sender, "commandblock.remove")) {
            messages.sendRaw(sender, "help.remove");
        }
        if (hasPermission(sender, "commandblock.list")) {
            messages.sendRaw(sender, "help.list");
        }
        if (hasPermission(sender, "commandblock.reload")) {
            messages.sendRaw(sender, "help.reload");
        }
        messages.sendRaw(sender, "help.help");
    }

    /**
     * Checks if sender has base permission to use the command.
     */
    private boolean hasBasePermission(CommandSender sender) {
        return sender.hasPermission("commandblock.command") || sender.hasPermission("commandblock.admin");
    }

    /**
     * Checks if sender has specific permission or admin permission.
     */
    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("commandblock.admin");
    }
}