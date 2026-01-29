package dev.nxms.commandblocker.manager;

import dev.nxms.commandblocker.CommandBlocker;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages plugin messages and translations.
 * Supports multiple languages and color formatting.
 */
public class MessageManager {

    private final CommandBlocker plugin;

    private FileConfiguration messagesConfig;
    private String prefix;

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public MessageManager(CommandBlocker plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    /**
     * Loads messages from the language file.
     * If language files are missing, it creates them from plugin resources.
     */
    public void loadMessages() {
        // Ensure plugin folder exists
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }

        // Ensure default message files exist
        File plFile = new File(plugin.getDataFolder(), "messages_pl.yml");
        if (!plFile.exists()) {
            plugin.getLogger().warning("Couldn't find messages_pl.yml file! Creating new messages file.");
            plugin.saveResource("messages_pl.yml", false);
        }

        File enFile = new File(plugin.getDataFolder(), "messages_en.yml");
        if (!enFile.exists()) {
            plugin.getLogger().warning("Couldn't find messages_en.yml file! Creating new messages file.");
            plugin.saveResource("messages_en.yml", false);
        }

        String language = plugin.getConfig().getString("language", "en").toLowerCase();
        String fileName = "messages_" + language + ".yml";

        File messagesFile = new File(plugin.getDataFolder(), fileName);

        // Fallback if selected language file doesn't exist
        if (!messagesFile.exists()) {
            plugin.getLogger().warning("Messages file " + fileName + " not found! Using messages_en.yml.");
            messagesFile = enFile;
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        prefix = colorize(messagesConfig.getString("prefix", "&8[&cCommandBlocker&8] &7"));

        plugin.getLogger().info("Messages file has been loaded (" + messagesFile.getName() + ").");
    }

    /**
     * Reloads messages from the language file.
     */
    public void reload() {
        loadMessages();
        plugin.getLogger().info("Messages has been reloaded.");
    }

    /**
     * Gets a raw message from config.
     */
    public String getRaw(String path) {
        return messagesConfig.getString(path, "&cMissing message: " + path);
    }

    /**
     * Gets a colorized message from config.
     */
    public String get(String path) {
        return colorize(getRaw(path));
    }

    /**
     * Gets a colorized message with placeholder replacements.
     */
    public String get(String path, String placeholder, String value) {
        return get(path).replace(placeholder, value);
    }

    /**
     * Sends a message with prefix to the sender.
     */
    public void send(CommandSender sender, String path) {
        sender.sendMessage(prefix + get(path));
    }

    /**
     * Sends a message with prefix and placeholder replacement.
     */
    public void send(CommandSender sender, String path, String placeholder, String value) {
        sender.sendMessage(prefix + get(path, placeholder, value));
    }

    /**
     * Sends a raw message without prefix.
     */
    public void sendRaw(CommandSender sender, String path) {
        sender.sendMessage(get(path));
    }

    /**
     * Sends a raw message without prefix with placeholder replacement.
     */
    public void sendRaw(CommandSender sender, String path, String placeholder, String value) {
        sender.sendMessage(get(path, placeholder, value));
    }

    /**
     * Converts color codes (& and hex) to colored string.
     */
    private String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Convert hex colors (&#RRGGBB) to Bukkit format (§x§R§R§G§G§B§B)
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append("§").append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);

        // Convert legacy color codes
        return buffer.toString().replace("&", "§");
    }
}