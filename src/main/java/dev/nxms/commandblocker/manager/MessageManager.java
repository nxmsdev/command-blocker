package dev.nxms.commandblocker.manager;

import dev.nxms.commandblocker.CommandBlocker;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages plugin messages and translations.
 * Supports multiple languages, color formatting, and config placeholders.
 */
public class MessageManager {

    private final CommandBlocker plugin;

    private FileConfiguration messagesConfig;
    private String language;

    // Pattern for hex colors (&#RRGGBB)
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    // Pattern for config placeholders ({key})
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)}");

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
        saveDefaultMessageFile("messages_en.yml");
        saveDefaultMessageFile("messages_pl.yml");

        // Load language from config
        language = plugin.getConfig().getString("language", "en").toLowerCase();
        String fileName = "messages_" + language + ".yml";
        File messagesFile = new File(plugin.getDataFolder(), fileName);

        // Fallback to English if selected language doesn't exist
        if (!messagesFile.exists()) {
            plugin.getLogger().warning("Messages file " + fileName + " not found! Using messages_en.yml.");
            fileName = "messages_en.yml";
            messagesFile = new File(plugin.getDataFolder(), fileName);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Load defaults from JAR
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            messagesConfig.setDefaults(defaultConfig);
        }

        plugin.getLogger().info("Messages file has been loaded (" + messagesFile.getName() + ").");
    }

    /**
     * Saves a default message file if it doesn't exist.
     */
    private void saveDefaultMessageFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.getLogger().warning("Couldn't find " + fileName + " file! Creating new messages file.");
            plugin.saveResource(fileName, false);
        }
    }

    /**
     * Reloads messages from the language file.
     */
    public void reload() {
        loadMessages();
        plugin.getLogger().info("Messages have been reloaded.");
    }

    /**
     * Gets a raw message from config without color processing.
     */
    public String getRaw(String path) {
        return messagesConfig.getString(path, "");
    }

    /**
     * Replaces all {key} placeholders with values from messages file.
     * This allows referencing other message keys within messages.
     */
    private String replaceConfigPlaceholders(String message) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String value = getRaw(placeholder);

            // Only replace if key exists in config and is not empty
            if (!value.isEmpty()) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(value));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Gets a colorized message from config with config placeholders replaced.
     */
    public String get(String path) {
        String message = getRaw(path);
        if (message.isEmpty()) {
            return colorize("&cMissing message: " + path);
        }
        message = replaceConfigPlaceholders(message);
        return colorize(message);
    }

    /**
     * Gets a colorized message with single placeholder replacement.
     */
    public String get(String path, String placeholder, String value) {
        return get(path).replace(placeholder, value);
    }

    /**
     * Gets a colorized message with multiple placeholder replacements.
     */
    public String get(String path, Map<String, String> placeholders) {
        String message = get(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    /**
     * Gets the prefix from messages file.
     */
    public String getPrefix() {
        return get("prefix");
    }

    /**
     * Gets the current language.
     */
    public String getLanguage() {
        return language;
    }

    // ==================== SEND METHODS ====================

    /**
     * Sends a message with prefix to the sender.
     */
    public void send(CommandSender sender, String path) {
        sender.sendMessage(getPrefix() + get(path));
    }

    /**
     * Sends a message with prefix and single placeholder replacement.
     */
    public void send(CommandSender sender, String path, String placeholder, String value) {
        sender.sendMessage(getPrefix() + get(path, placeholder, value));
    }

    /**
     * Sends a message with prefix and multiple placeholder replacements.
     */
    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(getPrefix() + get(path, placeholders));
    }

    /**
     * Sends a message without prefix.
     */
    public void sendRaw(CommandSender sender, String path) {
        sender.sendMessage(get(path));
    }

    /**
     * Sends a message without prefix with single placeholder replacement.
     */
    public void sendRaw(CommandSender sender, String path, String placeholder, String value) {
        sender.sendMessage(get(path, placeholder, value));
    }

    /**
     * Sends a message without prefix with multiple placeholder replacements.
     */
    public void sendRaw(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(get(path, placeholders));
    }

    // ==================== UTILITY METHODS ====================

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

    /**
     * Helper method to create placeholder maps easily.
     * Usage: placeholders("player", "Steve", "command", "/help")
     */
    public static Map<String, String> placeholders(String... keyValues) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValues.length - 1; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}