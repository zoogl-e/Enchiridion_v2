package net.zoogle.enchiridion.client.levelrpg;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <b>Player-facing Enchiridion projection – data layer.</b>
 *
 * <p>Immutable snapshot of a single skill tree as prepared for rendering inside the Enchiridion
 * book's 3-D projection overlay. Instances are produced on the client by
 * {@link LevelRpgJournalInteractionBridge#projectionData} and consumed by
 * {@code BookSceneRenderer} / {@code BookModeCoordinator}.
 *
 * <p>The data is bridge-assembled via reflection from LevelRPG's
 * {@code ClientJournalSnapshotFactory} so that Enchiridion does not carry a hard compile-time
 * dependency on the LevelRPG jar. Node layout coordinates ({@code layoutX}/{@code layoutY}) are
 * normalised to a [-1.375, 1.375] range by {@code LevelRpgJournalInteractionBridge#normalizeLayout}
 * before being stored here.
 *
 * @see SkillTreeProjectionState
 * @see LevelRpgJournalInteractionBridge
 */
public record SkillTreeProjectionData(
        String skillName,
        String skillKey,
        int insight,
        int spentPoints,
        int earnedPoints,
        List<ProjectedNode> nodes,
        List<ProjectedEdge> edges
) {
    public SkillTreeProjectionData {
        skillName = safe(skillName);
        skillKey = safe(skillKey);
        insight = Math.max(0, insight);
        spentPoints = Math.max(0, spentPoints);
        earnedPoints = Math.max(0, earnedPoints);
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
    }

    public static SkillTreeProjectionData empty(String skillName) {
        return new SkillTreeProjectionData(skillName, "", 0, 0, 0, List.of(), List.of());
    }

    public Map<String, ProjectedNode> nodeMap() {
        LinkedHashMap<String, ProjectedNode> map = new LinkedHashMap<>();
        for (ProjectedNode node : nodes) {
            map.put(node.id(), node);
        }
        return Map.copyOf(map);
    }

    public ProjectedNode nodeByIndex(int index) {
        if (index < 0 || index >= nodes.size()) {
            return null;
        }
        return nodes.get(index);
    }

    public int indexOfNode(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return -1;
        }
        for (int i = 0; i < nodes.size(); i++) {
            if (Objects.equals(nodeId, nodes.get(i).id())) {
                return i;
            }
        }
        return -1;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public record ProjectedNode(
            String id,
            String title,
            String description,
            String branch,
            String type,
            String iconKey,
            int cost,
            int requiredLevel,
            String status,
            List<String> missingRequirements,
            List<String> requires,
            float layoutX,
            float layoutY
    ) {
        public ProjectedNode {
            id = safe(id);
            title = safe(title);
            description = safe(description);
            branch = safe(branch);
            type = safe(type);
            iconKey = safe(iconKey);
            cost = Math.max(0, cost);
            requiredLevel = Math.max(0, requiredLevel);
            status = safe(status);
            missingRequirements = missingRequirements == null ? List.of() : List.copyOf(missingRequirements);
            requires = requires == null ? List.of() : List.copyOf(requires);
        }

        public boolean unlocked() {
            return "UNLOCKED".equalsIgnoreCase(status);
        }

        public boolean available() {
            return "AVAILABLE".equalsIgnoreCase(status);
        }
    }

    public record ProjectedEdge(String fromNodeId, String toNodeId) {
        public ProjectedEdge {
            fromNodeId = safe(fromNodeId);
            toNodeId = safe(toNodeId);
        }
    }
}
