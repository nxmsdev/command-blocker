package dev.nxms.commandblock.command;

import dev.nxms.commandblock.CommandBlock;
import dev.nxms.commandblock.manager.BlockedCommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tab completer for /commandblock command.
 * Hides subcommands from users without proper permissions.
 */
public class CommandBlockTabCompleter implements TabCompleter {

    private final CommandBlock plugin;
    private final BlockedCommandManager blockedManager;

    public CommandBlockTabCompleter(CommandBlock plugin) {
        this.plugin = plugin;
        this.blockedManager = plugin.getBlockedCommandManager();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {

        if (!hasBasePermission(sender)) {
            return List.of();
        }

        if (args.length == 1) {
            return filterStartsWith(getAvailableSubcommands(sender), args[0]);
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();

            if (subcommand.equals("add") && hasPermission(sender, "commandblock.add")) {
                // Show only commands that are NOT blocked
                return filterStartsWith(getUnblockedCommands(), args[1]);
            }

            if (subcommand.equals("remove") && hasPermission(sender, "commandblock.remove")) {
                // Show only commands that ARE blocked
                return filterStartsWith(getBlockedCommands(), args[1]);
            }
        }

        return List.of();
    }

    /**
     * Gets available subcommands based on sender's permissions.
     */
    private List<String> getAvailableSubcommands(CommandSender sender) {
        List<String> subcommands = new ArrayList<>();

        if (hasPermission(sender, "commandblock.add")) {
            subcommands.add("add");
        }
        if (hasPermission(sender, "commandblock.remove")) {
            subcommands.add("remove");
        }
        if (hasPermission(sender, "commandblock.list")) {
            subcommands.add("list");
        }
        if (hasPermission(sender, "commandblock.reload")) {
            subcommands.add("reload");
        }
        subcommands.add("help");

        return subcommands;
    }

    /**
     * Gets all server commands that are NOT blocked.
     */
    private List<String> getUnblockedCommands() {
        Set<String> blocked = blockedManager.getBlockedCommands();

        return plugin.getServer().getCommandMap().getKnownCommands().entrySet().stream()
                .map(entry -> {
                    String name = entry.getKey();
                    if (!name.contains(":")) {
                        org.bukkit.command.Command cmd = entry.getValue();
                        if (cmd instanceof PluginCommand pluginCmd) {
                            return pluginCmd.getPlugin().getName().toLowerCase() + ":" + name;
                        }
                        return "minecraft:" + name;
                    }
                    return name;
                })
                .filter(cmd -> !blocked.contains(cmd.toLowerCase()))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Gets all blocked commands.
     */
    private List<String> getBlockedCommands() {
        return blockedManager.getBlockedCommands().stream()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Filters list to only include entries starting with given prefix.
     */
    private List<String> filterStartsWith(List<String> list, String prefix) {
        String lowerPrefix = prefix.toLowerCase();
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(lowerPrefix))
                .sorted()
                .collect(Collectors.toList());
    }

    private boolean hasBasePermission(CommandSender sender) {
        return sender.hasPermission("commandblock.command") || sender.hasPermission("commandblock.admin");
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("commandblock.admin");
    }
}