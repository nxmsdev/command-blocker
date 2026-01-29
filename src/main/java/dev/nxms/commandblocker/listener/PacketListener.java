package dev.nxms.commandblocker.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.chat.Node;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDeclareCommands;
import dev.nxms.commandblocker.CommandBlocker;
import dev.nxms.commandblocker.manager.BlockedCommandManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles packet-level command blocking using PacketEvents.
 * Intercepts command packets to completely hide blocked commands from clients.
 */
public class PacketListener extends PacketListenerAbstract {

    private final CommandBlocker plugin;
    private final BlockedCommandManager blockedManager;

    public PacketListener(CommandBlocker plugin) {
        super(PacketListenerPriority.HIGHEST);
        this.plugin = plugin;
        this.blockedManager = plugin.getBlockedCommandManager();
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.DECLARE_COMMANDS) {
            return;
        }

        Object playerObj = event.getPlayer();
        if (!(playerObj instanceof Player player)) {
            return;
        }

        if (player.hasPermission("commandblocker.bypass")) {
            return;
        }

        try {
            WrapperPlayServerDeclareCommands packet = new WrapperPlayServerDeclareCommands(event);
            List<Node> nodes = packet.getNodes();
            int rootIndex = packet.getRootIndex();

            if (nodes == null || nodes.isEmpty() || rootIndex < 0 || rootIndex >= nodes.size()) {
                return;
            }

            FilterResult result = filterCommands(nodes, rootIndex, player);

            packet.setNodes(result.nodes);
            packet.setRootIndex(result.rootIndex);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to filter commands packet: " + e.getMessage());
        }
    }

    /**
     * Filters command nodes, removing blocked commands.
     */
    private FilterResult filterCommands(List<Node> originalNodes, int originalRootIndex, Player player) {
        List<Node> newNodes = new ArrayList<>();
        Map<Integer, Integer> indexMapping = new HashMap<>();

        for (int i = 0; i < originalNodes.size(); i++) {
            Node node = originalNodes.get(i);

            if (shouldRemoveNode(node, i, originalRootIndex, originalNodes, player)) {
                continue;
            }

            indexMapping.put(i, newNodes.size());
            newNodes.add(node);
        }

        List<Node> updatedNodes = new ArrayList<>();
        for (Node node : newNodes) {
            List<Integer> oldChildren = node.getChildren();
            List<Integer> newChildren = new ArrayList<>();

            for (int oldChild : oldChildren) {
                Integer newIndex = indexMapping.get(oldChild);
                if (newIndex != null) {
                    newChildren.add(newIndex);
                }
            }

            Node updatedNode = createNodeWithNewChildren(node, newChildren);
            updatedNodes.add(updatedNode);
        }

        int newRootIndex = indexMapping.getOrDefault(originalRootIndex, 0);

        return new FilterResult(updatedNodes, newRootIndex);
    }

    /**
     * Determines if a node should be removed.
     */
    private boolean shouldRemoveNode(Node node, int nodeIndex, int rootIndex, List<Node> allNodes, Player player) {
        byte nodeType = (byte) (node.getFlags() & Node.TYPE_MASK);
        if (nodeType != Node.TYPE_LITERAL) {
            return false;
        }

        String name = node.getName().orElse(null);
        if (name == null) {
            return false;
        }

        Node rootNode = allNodes.get(rootIndex);
        List<Integer> rootChildren = rootNode.getChildren();

        for (int childIndex : rootChildren) {
            if (childIndex == nodeIndex) {
                if (blockedManager.isBlocked(name.toLowerCase())) {
                    if (plugin.isPacketLoggerEnabled()) {
                        plugin.getLogger().info("Blocked command '" + name + "' from packet for player: " + player.getName() + ".");
                    }
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Creates a copy of node with updated children indices.
     */
    private Node createNodeWithNewChildren(Node original, List<Integer> newChildren) {
        return new Node(
                original.getFlags(),
                newChildren,
                original.getRedirectNodeIndex(),
                original.getName().orElse(null),
                original.getParser().orElse(null),
                original.getProperties().orElse(null),
                original.getSuggestionsType().orElse(null)
        );
    }

    /**
     * Result of command filtering operation.
     */
    private record FilterResult(List<Node> nodes, int rootIndex) {}
}