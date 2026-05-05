package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.client.ui.BookScreen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LevelRpgJournalInteractionBridge {
    private static final String LEVEL_RPG_SKILL_TREE_SCREEN_CLASS = "net.zoogle.levelrpg.client.ui.SkillTreeEditorScreen";
    private static final String SPEND_SKILL_POINT_PAYLOAD_CLASS = "net.zoogle.levelrpg.net.payload.SpendSkillPointRequestPayload";
    private static final String UNLOCK_TREE_NODE_PAYLOAD_CLASS = "net.zoogle.levelrpg.net.payload.UnlockTreeNodeRequestPayload";
    private static final String CLIENT_FACTORY_CLASS = "net.zoogle.levelrpg.client.journal.ClientJournalSnapshotFactory";
    private static final String SKILL_TREE_REGISTRY_CLASS = "net.zoogle.levelrpg.skilltree.SkillTreeRegistry";
    private static final String PACKET_DISTRIBUTOR_CLASS = "net.neoforged.neoforge.network.PacketDistributor";

    private LevelRpgJournalInteractionBridge() {}

    public static List<JournalArchetypeChoice> availableArchetypes() {
        return LevelRpgArchetypeBindingBridge.availableArchetypes();
    }

    public static List<String> availableJournalSkillNames() {
        List<String> names = new ArrayList<>(LevelRpgJournalSnapshotFactory.CANONICAL_SKILL_PATHS.keySet());
        names.sort(String::compareToIgnoreCase);
        return names;
    }

    public static JournalSkillEntry skillEntry(BookContext context, String journalSkillName) {
        if (context == null || journalSkillName == null || journalSkillName.isBlank()) {
            return null;
        }
        LevelRpgJournalSnapshot snapshot = LevelRpgJournalSnapshotFactory.create(context);
        for (JournalSkillEntry entry : snapshot.skills()) {
            if (journalSkillName.equalsIgnoreCase(entry.name()) || journalSkillName.equalsIgnoreCase(entry.skillKey())) {
                return entry;
            }
        }
        return null;
    }

    static boolean openSkillScreen(BookContext context, String journalSkillName) {
        if (context == null || context.minecraft() == null || context.player() == null || journalSkillName == null) {
            return false;
        }

        String path = LevelRpgJournalSnapshotFactory.CANONICAL_SKILL_PATHS.get(journalSkillName);
        if (path == null || path.isBlank()) {
            context.player().displayClientMessage(Component.literal("No linked LevelRPG screen is recorded for " + journalSkillName + "."), true);
            return false;
        }

        Minecraft minecraft = context.minecraft();
        ResourceLocation skillId = ResourceLocation.fromNamespaceAndPath("levelrpg", path);
        int returnSpreadIndex = minecraft.screen instanceof BookScreen bookScreen ? bookScreen.currentSpreadIndex() : -1;
        try {
            Class<?> screenClass = Class.forName(LEVEL_RPG_SKILL_TREE_SCREEN_CLASS);
            Object screen = createSkillTreeScreen(screenClass, skillId, returnSpreadIndex);
            minecraft.setScreen((net.minecraft.client.gui.screens.Screen) screen);
            return true;
        } catch (ReflectiveOperationException | ClassCastException exception) {
            context.player().displayClientMessage(Component.literal("LevelRPG skill details are unavailable right now."), true);
            return false;
        }
    }

    static boolean openSkillProjection(BookContext context, String journalSkillName) {
        if (context == null || context.minecraft() == null || journalSkillName == null || journalSkillName.isBlank()) {
            return false;
        }
        if (!(context.minecraft().screen instanceof BookScreen bookScreen)) {
            return false;
        }
        return bookScreen.openSkillTreeProjection(journalSkillName);
    }

    private static Object createSkillTreeScreen(Class<?> screenClass, ResourceLocation skillId, int returnSpreadIndex)
            throws ReflectiveOperationException {
        try {
            Constructor<?> constructor = screenClass.getConstructor(ResourceLocation.class, int.class);
            return constructor.newInstance(skillId, returnSpreadIndex);
        } catch (NoSuchMethodException ignored) {
            Constructor<?> constructor = screenClass.getConstructor(ResourceLocation.class);
            return constructor.newInstance(skillId);
        }
    }

    static boolean requestSpendSkillPoint(BookContext context, String journalSkillName) {
        if (context == null || context.minecraft() == null || context.player() == null || journalSkillName == null) {
            return false;
        }

        String path = LevelRpgJournalSnapshotFactory.CANONICAL_SKILL_PATHS.get(journalSkillName);
        if (path == null || path.isBlank()) {
            context.player().displayClientMessage(Component.literal("No spend target is recorded for " + journalSkillName + "."), true);
            return false;
        }

        ResourceLocation skillId = ResourceLocation.fromNamespaceAndPath("levelrpg", path);
        try {
            Class<?> payloadClass = Class.forName(SPEND_SKILL_POINT_PAYLOAD_CLASS);
            Constructor<?> constructor = payloadClass.getConstructor(ResourceLocation.class);
            Object payload = constructor.newInstance(skillId);

            Class<?> packetDistributor = Class.forName(PACKET_DISTRIBUTOR_CLASS);
            Method sendToServer = packetDistributor.getMethod(
                    "sendToServer",
                    net.minecraft.network.protocol.common.custom.CustomPacketPayload.class,
                    net.minecraft.network.protocol.common.custom.CustomPacketPayload[].class
            );
            sendToServer.invoke(null, payload, (Object) new net.minecraft.network.protocol.common.custom.CustomPacketPayload[0]);
            return true;
        } catch (ReflectiveOperationException exception) {
            context.player().displayClientMessage(Component.literal("Skill investment is unavailable right now."), true);
            return false;
        }
    }

    public static boolean requestSkillProjectionSpend(BookContext context, String journalSkillName) {
        return requestSpendSkillPoint(context, journalSkillName);
    }

    public static SkillTreeProjectionData projectionData(BookContext context, String journalSkillName) {
        if (context == null || journalSkillName == null || journalSkillName.isBlank()) {
            return SkillTreeProjectionData.empty(journalSkillName);
        }
        try {
            Class<?> factoryClass = Class.forName(CLIENT_FACTORY_CLASS);
            Method createMethod = factoryClass.getMethod("create");
            Object profileSnapshot = createMethod.invoke(null);
            if (profileSnapshot == null) {
                return SkillTreeProjectionData.empty(journalSkillName);
            }
            Object skillSnapshot = resolveSkillSnapshot(profileSnapshot, journalSkillName);
            if (skillSnapshot == null) {
                return SkillTreeProjectionData.empty(journalSkillName);
            }
            ResourceLocation skillId = resolveSkillId(skillSnapshot);
            Object mastery = call(skillSnapshot, "mastery");
            if (mastery == null) {
                return SkillTreeProjectionData.empty(journalSkillName);
            }
            int insight = intValue(call(mastery, "insight"));
            int spentPoints = intValue(call(mastery, "spentPoints"));
            int earnedPoints = intValue(call(mastery, "earnedPoints"));
            List<?> masteryNodes = asList(call(mastery, "nodes"));
            Map<String, NodeLayout> layoutById = resolveNodeLayout(skillId);
            List<SkillTreeProjectionData.ProjectedNode> nodes = new ArrayList<>();
            for (Object node : masteryNodes) {
                String id = stringValue(call(node, "id"));
                NodeLayout layout = layoutById.getOrDefault(id, NodeLayout.ZERO);
                nodes.add(new SkillTreeProjectionData.ProjectedNode(
                        id,
                        stringValue(call(node, "title")),
                        stringValue(call(node, "description")),
                        stringValue(call(node, "branch")),
                        stringValue(call(node, "type")),
                        stringValue(call(node, "iconKey")),
                        intValue(call(node, "cost")),
                        intValue(call(node, "requiredLevel")),
                        enumName(call(node, "status")),
                        stringifyList(asList(call(node, "missingRequirements"))),
                        layout.requires(),
                        layout.x(),
                        layout.y()
                ));
            }
            nodes = normalizeLayout(nodes);
            List<SkillTreeProjectionData.ProjectedEdge> edges = buildEdges(nodes);
            String skillKey = skillId == null ? "" : skillId.getPath();
            return new SkillTreeProjectionData(journalSkillName, skillKey, insight, spentPoints, earnedPoints, nodes, edges);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return SkillTreeProjectionData.empty(journalSkillName);
        }
    }

    public static boolean requestSkillProjectionNodeUnlock(BookContext context, String journalSkillName, String nodeId) {
        if (context == null || context.minecraft() == null || context.player() == null || journalSkillName == null || nodeId == null || nodeId.isBlank()) {
            return false;
        }
        String path = LevelRpgJournalSnapshotFactory.CANONICAL_SKILL_PATHS.get(journalSkillName);
        if (path == null || path.isBlank()) {
            context.player().displayClientMessage(Component.literal("No unlock target is recorded for " + journalSkillName + "."), true);
            return false;
        }
        ResourceLocation skillId = ResourceLocation.fromNamespaceAndPath("levelrpg", path);
        try {
            Class<?> payloadClass = Class.forName(UNLOCK_TREE_NODE_PAYLOAD_CLASS);
            Constructor<?> constructor = payloadClass.getConstructor(ResourceLocation.class, String.class);
            Object payload = constructor.newInstance(skillId, nodeId);
            Class<?> packetDistributor = Class.forName(PACKET_DISTRIBUTOR_CLASS);
            Method sendToServer = packetDistributor.getMethod(
                    "sendToServer",
                    net.minecraft.network.protocol.common.custom.CustomPacketPayload.class,
                    net.minecraft.network.protocol.common.custom.CustomPacketPayload[].class
            );
            sendToServer.invoke(null, payload, (Object) new net.minecraft.network.protocol.common.custom.CustomPacketPayload[0]);
            return true;
        } catch (ReflectiveOperationException exception) {
            context.player().displayClientMessage(Component.literal("Node inscription is unavailable right now."), true);
            return false;
        }
    }

    static boolean jumpToJournalPage(BookContext context, int pageIndex) {
        if (context == null || context.minecraft() == null || pageIndex < 0) {
            return false;
        }
        if (!(context.minecraft().screen instanceof BookScreen bookScreen)) {
            return false;
        }
        return bookScreen.jumpToSpread(pageIndex / 2);
    }

    static boolean beginArchetypeSelection(BookContext context) {
        if (context == null || context.minecraft() == null) {
            return false;
        }
        if (!(context.minecraft().screen instanceof BookScreen bookScreen)) {
            return false;
        }
        return bookScreen.openArchetypeReel();
    }

    static boolean beginArchetypeBinding(BookContext context, String focusId) {
        if (context == null || context.minecraft() == null || focusId == null || focusId.isBlank()) {
            return false;
        }
        if (!(context.minecraft().screen instanceof BookScreen bookScreen)) {
            return false;
        }
        return bookScreen.beginArchetypeBinding(focusId);
    }

    static boolean openArchetypeProjection(BookContext context, String focusId) {
        if (context == null || context.minecraft() == null || focusId == null || focusId.isBlank()) {
            return false;
        }
        if (!(context.minecraft().screen instanceof BookScreen bookScreen)) {
            return false;
        }
        return bookScreen.openArchetypeReel();
    }

    private static Object resolveSkillSnapshot(Object profileSnapshot, String journalSkillName) throws ReflectiveOperationException {
        for (Object skill : asList(call(profileSnapshot, "skills"))) {
            Object skillId = call(skill, "skillId");
            String path = skillId instanceof ResourceLocation rl ? rl.getPath() : "";
            String display = stringValue(call(skill, "displayName"));
            if (journalSkillName.equalsIgnoreCase(display) || journalSkillName.equalsIgnoreCase(path)) {
                return skill;
            }
        }
        return null;
    }

    private static ResourceLocation resolveSkillId(Object skillSnapshot) throws ReflectiveOperationException {
        Object id = call(skillSnapshot, "skillId");
        if (id instanceof ResourceLocation rl) {
            return rl;
        }
        return null;
    }

    private static Map<String, NodeLayout> resolveNodeLayout(ResourceLocation skillId) throws ReflectiveOperationException {
        if (skillId == null) {
            return Map.of();
        }
        Class<?> registryClass = Class.forName(SKILL_TREE_REGISTRY_CLASS);
        Method getMethod = registryClass.getMethod("get", ResourceLocation.class);
        Object treeDefinition = getMethod.invoke(null, skillId);
        if (treeDefinition == null) {
            return Map.of();
        }
        Map<String, NodeLayout> byId = new HashMap<>();
        Object nodesValue = call(treeDefinition, "nodes");
        if (nodesValue instanceof Map<?, ?> nodeMap) {
            for (Map.Entry<?, ?> entry : nodeMap.entrySet()) {
                Object node = entry.getValue();
                if (node == null) {
                    continue;
                }
                String nodeId = stringValue(call(node, "id"));
                if (nodeId.isBlank()) {
                    continue;
                }
                List<String> requires = stringifyList(asList(call(node, "requires")));
                byId.put(nodeId, new NodeLayout(
                        intValue(call(node, "x")),
                        intValue(call(node, "y")),
                        requires
                ));
            }
        }
        return Map.copyOf(byId);
    }

    private static List<SkillTreeProjectionData.ProjectedEdge> buildEdges(List<SkillTreeProjectionData.ProjectedNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        Set<String> ids = nodes.stream().map(SkillTreeProjectionData.ProjectedNode::id).collect(java.util.stream.Collectors.toSet());
        List<SkillTreeProjectionData.ProjectedEdge> edges = new ArrayList<>();
        for (SkillTreeProjectionData.ProjectedNode node : nodes) {
            for (String parentId : node.requires()) {
                if (ids.contains(parentId)) {
                    edges.add(new SkillTreeProjectionData.ProjectedEdge(parentId, node.id()));
                }
            }
        }
        return List.copyOf(edges);
    }

    private static List<SkillTreeProjectionData.ProjectedNode> normalizeLayout(List<SkillTreeProjectionData.ProjectedNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (SkillTreeProjectionData.ProjectedNode node : nodes) {
            minX = Math.min(minX, node.layoutX());
            maxX = Math.max(maxX, node.layoutX());
            minY = Math.min(minY, node.layoutY());
            maxY = Math.max(maxY, node.layoutY());
        }
        float spanX = Math.max(1.0f, maxX - minX);
        float spanY = Math.max(1.0f, maxY - minY);
        float longest = Math.max(spanX, spanY);
        float scale = longest <= 0.0f ? 1.0f : 2.75f / longest;
        float centerX = (minX + maxX) * 0.5f;
        float centerY = (minY + maxY) * 0.5f;
        List<SkillTreeProjectionData.ProjectedNode> normalized = new ArrayList<>(nodes.size());
        for (SkillTreeProjectionData.ProjectedNode node : nodes) {
            normalized.add(new SkillTreeProjectionData.ProjectedNode(
                    node.id(),
                    node.title(),
                    node.description(),
                    node.branch(),
                    node.type(),
                    node.iconKey(),
                    node.cost(),
                    node.requiredLevel(),
                    node.status(),
                    node.missingRequirements(),
                    node.requires(),
                    (node.layoutX() - centerX) * scale,
                    (node.layoutY() - centerY) * scale
            ));
        }
        return List.copyOf(normalized);
    }

    private static Object call(Object target, String methodName) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private static List<?> asList(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private static String stringValue(Object value) {
        return value instanceof String string ? string : "";
    }

    private static String enumName(Object value) {
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        return stringValue(value);
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static List<String> stringifyList(List<?> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>(values.size());
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value);
            if (!text.isBlank()) {
                out.add(text);
            }
        }
        return List.copyOf(out);
    }

    private record NodeLayout(int x, int y, List<String> requires) {
        private static final NodeLayout ZERO = new NodeLayout(0, 0, List.of());
    }

}
