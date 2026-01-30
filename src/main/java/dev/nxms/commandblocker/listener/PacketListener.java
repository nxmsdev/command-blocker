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
 * Hides blocked commands from the DECLARE_COMMANDS packet using PacketEvents.
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

            FilterResult result = filterCommands(nodes, rootIndex);

            if (!result.changed) {
                return;
            }

            packet.setNodes(result.nodes);
            packet.setRootIndex(result.rootIndex);
            packet.write();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to filter commands packet: " + e.getMessage());
        }
    }

    /**
     * Filters command nodes, removing blocked commands.
     */
    private FilterResult filterCommands(List<Node> originalNodes, int originalRootIndex) {
        List<Node> newNodes = new ArrayList<>();
        Map<Integer, Integer> indexMapping = new HashMap<>();
        boolean changed = false;

        for (int i = 0; i < originalNodes.size(); i++) {
            Node node = originalNodes.get(i);

            if (shouldRemoveNode(node, i, originalRootIndex, originalNodes)) {
                changed = true;
                continue;
            }

            indexMapping.put(i, newNodes.size());
            newNodes.add(node);
        }

        List<Node> updatedNodes = new ArrayList<>();
        for (Node node : newNodes) {
            List<Integer> remappedChildren = new ArrayList<>();
            for (int oldChild : node.getChildren()) {
                Integer mapped = indexMapping.get(oldChild);
                if (mapped != null) {
                    remappedChildren.add(mapped);
                } else {
                    changed = true;
                }
            }

            byte newFlags = node.getFlags();
            int newRedirectIndex = node.getRedirectNodeIndex();

            boolean hasRedirect = (newFlags & Node.FLAG_REDIRECT) == Node.FLAG_REDIRECT;
            if (hasRedirect) {
                Integer mappedRedirect = indexMapping.get(node.getRedirectNodeIndex());
                if (mappedRedirect != null) {
                    newRedirectIndex = mappedRedirect;
                } else {
                    newFlags = (byte) (newFlags & ~Node.FLAG_REDIRECT);
                    newRedirectIndex = 0;
                    changed = true;
                }
            }

            updatedNodes.add(new Node(
                    newFlags,
                    remappedChildren,
                    newRedirectIndex,
                    node.getName().orElse(null),
                    node.getParser().orElse(null),
                    node.getProperties().orElse(null),
                    node.getSuggestionsType().orElse(null)
            ));
        }

        int newRootIndex = indexMapping.getOrDefault(originalRootIndex, 0);
        if (newRootIndex != originalRootIndex) {
            changed = true;
        }

        return new FilterResult(updatedNodes, newRootIndex, changed);
    }

    /**
     * Determines if a node should be removed.
     */
    private boolean shouldRemoveNode(Node node, int nodeIndex, int rootIndex, List<Node> allNodes) {
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
                return blockedManager.isBlocked(name.toLowerCase());
            }
        }

        return false;
    }

    /**
     * Result of command filtering operation.
     */
    private record FilterResult(List<Node> nodes, int rootIndex, boolean changed) {}
}