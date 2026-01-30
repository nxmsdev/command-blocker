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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages plugin messages and translations.
 * Supports multiple languages, modular prefixes, and color formatting.
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
     */
    public void loadMessages() {
        // Ensure plugin folder exists
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }

        // Save default message file if exists in JAR
        saveDefaultMessageFile("messages_en.yml");

        // Load language from config
        language = plugin.getConfig().getString("language", "en").toLowerCase();
        String fileName = "messages_" + language + ".yml";
        File messagesFile = new File(plugin.getDataFolder(), fileName);

        // Fallback to English if selected language doesn't exist
        if (!messagesFile.exists()) {
            plugin.getLogger().warning("Messages file " + fileName + " not found! Using messages_en.yml.");
            fileName = "messages_en.yml";
            messagesFile = new File(plugin.getDataFolder(), fileName);

            // Create English file if it doesn't exist
            if (!messagesFile.exists() && plugin.getResource("messages_en.yml") != null) {
                plugin.saveResource("messages_en.yml", false);
            }
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
     * Only saves if the resource exists in the JAR.
     */
    private void saveDefaultMessageFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists() && plugin.getResource(fileName) != null) {
            plugin.saveResource(fileName, false);
            plugin.getLogger().info("Created default " + fileName + " file.");
        }
    }

    /**
     * Reloads messages from the language file.
     */
    public void reload() {
        loadMessages();
    }

    /**
     * Gets a raw message from config without any processing.
     */
    public String getRaw(String path) {
        return messagesConfig.getString(path, "");
    }

    /**
     * Checks if a key exists in the messages config.
     */
    public boolean hasKey(String key) {
        return messagesConfig.contains(key) && !getRaw(key).isEmpty();
    }

    /**
     * Replaces {key} placeholders with values from messages file.
     * Only replaces placeholders that exist as keys in the config.
     * Prevents infinite recursion by tracking already processed keys.
     */
    private String replaceConfigPlaceholders(String message, Set<String> processedKeys) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String fullMatch = matcher.group(0);  // {placeholder}
            String placeholder = matcher.group(1); // placeholder

            // Skip if already processed (prevents infinite recursion)
            if (processedKeys.contains(placeholder)) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(fullMatch));
                continue;
            }

            // Check if this placeholder exists in config
            if (!hasKey(placeholder)) {
                // Keep original placeholder for custom placeholders like {player}, {command}
                matcher.appendReplacement(result, Matcher.quoteReplacement(fullMatch));
                continue;
            }

            // Mark as processed
            Set<String> newProcessedKeys = new HashSet<>(processedKeys);
            newProcessedKeys.add(placeholder);

            // Get and recursively process the value
            String value = getRaw(placeholder);
            value = replaceConfigPlaceholders(value, newProcessedKeys);

            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Gets a formatted message with config placeholders and colors.
     */
    public String get(String path) {
        String message = getRaw(path);
        if (message.isEmpty()) {
            return colorize("&cMissing message: " + path);
        }

        // Replace config placeholders (like {prefix-error}, {prefix-success}, etc.)
        Set<String> processedKeys = new HashSet<>();
        processedKeys.add(path); // Prevent self-reference
        message = replaceConfigPlaceholders(message, processedKeys);

        return colorize(message);
    }

    /**
     * Gets a formatted message with single placeholder replacement.
     */
    public String get(String path, String placeholder, String value) {
        return get(path).replace(placeholder, value);
    }

    /**
     * Gets a formatted message with multiple placeholder replacements.
     */
    public String get(String path, Map<String, String> placeholders) {
        String message = get(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    /**
     * Gets the current language.
     */
    public String getLanguage() {
        return language;
    }

    // ==================== SEND METHODS ====================

    /**
     * Sends a message to the sender.
     */
    public void send(CommandSender sender, String path) {
        sender.sendMessage(get(path));
    }

    /**
     * Sends a message with single placeholder replacement.
     */
    public void send(CommandSender sender, String path, String placeholder, String value) {
        sender.sendMessage(get(path, placeholder, value));
    }

    /**
     * Sends a message with multiple placeholder replacements.
     */
    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(get(path, placeholders));
    }

    /**
     * Sends raw text (not from config) to sender.
     */
    public void sendText(CommandSender sender, String text) {
        sender.sendMessage(colorize(text));
    }

    /**
     * Sends raw text with placeholder replacements.
     */
    public void sendText(CommandSender sender, String text, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        sender.sendMessage(colorize(text));
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