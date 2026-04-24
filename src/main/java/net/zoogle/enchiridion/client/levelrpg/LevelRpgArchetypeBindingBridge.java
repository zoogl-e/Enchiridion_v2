package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.api.BookContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class LevelRpgArchetypeBindingBridge {
    private static final String CLIENT_PROFILE_CACHE_CLASS = "net.zoogle.levelrpg.client.data.ClientProfileCache";
    private static final String ARCHETYPE_REGISTRY_CLASS = "net.zoogle.levelrpg.profile.ArchetypeRegistry";
    private static final String BIND_ARCHETYPE_PAYLOAD_CLASS = "net.zoogle.levelrpg.net.payload.BindArchetypeRequestPayload";
    private static final String PACKET_DISTRIBUTOR_CLASS = "net.neoforged.neoforge.network.PacketDistributor";

    private LevelRpgArchetypeBindingBridge() {}

    static boolean isBookBound(BookContext context) {
        try {
            Class<?> cacheClass = Class.forName(CLIENT_PROFILE_CACHE_CLASS);
            Method appliedMethod = cacheClass.getMethod("isArchetypeApplied");
            return Boolean.TRUE.equals(appliedMethod.invoke(null));
        } catch (ReflectiveOperationException exception) {
            return selectedArchetypeId(context) != null;
        }
    }

    static ResourceLocation selectedArchetypeId(BookContext context) {
        try {
            Class<?> cacheClass = Class.forName(CLIENT_PROFILE_CACHE_CLASS);
            Method idMethod = cacheClass.getMethod("getArchetypeId");
            Object value = idMethod.invoke(null);
            return value instanceof ResourceLocation id ? id : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    static List<JournalArchetypeChoice> availableArchetypes() {
        try {
            Class<?> registryClass = Class.forName(ARCHETYPE_REGISTRY_CLASS);
            Method valuesMethod = registryClass.getMethod("values");
            Object rawValues = valuesMethod.invoke(null);
            if (!(rawValues instanceof Collection<?> values) || values.isEmpty()) {
                return List.of();
            }

            List<JournalArchetypeChoice> choices = new ArrayList<>(values.size());
            for (Object value : values) {
                ResourceLocation id = invokeResource(value, "id");
                String title = invokeString(value, "displayName");
                String description = invokeString(value, "description");
                Map<String, Integer> startingLevels = startingLevels(value);
                choices.add(new JournalArchetypeChoice(
                        id != null ? id.toString() : title,
                        title.isBlank() ? LevelRpgJournalSnapshotFactory.humanizePath(id == null ? "unknown" : id.getPath()) : title,
                        description,
                        summarizeStartingLevels(startingLevels),
                        id == null ? "unknown" : id.toString()
                ));
            }
            return List.copyOf(choices);
        } catch (ReflectiveOperationException exception) {
            return List.of();
        }
    }

    static boolean requestBindArchetype(BookContext context, String focusId) {
        if (context == null || context.minecraft() == null || context.player() == null || focusId == null || focusId.isBlank()) {
            return false;
        }
        try {
            ResourceLocation archetypeId = ResourceLocation.parse(focusId);
            Class<?> payloadClass = Class.forName(BIND_ARCHETYPE_PAYLOAD_CLASS);
            Constructor<?> constructor = payloadClass.getConstructor(ResourceLocation.class);
            Object payload = constructor.newInstance(archetypeId);

            Class<?> packetDistributor = Class.forName(PACKET_DISTRIBUTOR_CLASS);
            Method sendToServer = packetDistributor.getMethod(
                    "sendToServer",
                    net.minecraft.network.protocol.common.custom.CustomPacketPayload.class,
                    net.minecraft.network.protocol.common.custom.CustomPacketPayload[].class
            );
            sendToServer.invoke(null, payload, (Object) new net.minecraft.network.protocol.common.custom.CustomPacketPayload[0]);
            return true;
        } catch (ReflectiveOperationException exception) {
            context.player().displayClientMessage(Component.literal("Archetype binding is unavailable right now."), true);
            return false;
        }
    }

    private static Map<String, Integer> startingLevels(Object definition) throws ReflectiveOperationException {
        Object raw = definition.getClass().getMethod("startingLevels").invoke(definition);
        if (!(raw instanceof Map<?, ?> map) || map.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Integer> levels = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String label = progressionSkillDisplayName(entry.getKey());
            int value = entry.getValue() instanceof Number number ? number.intValue() : 0;
            if (!label.isBlank() && value > 0) {
                levels.put(label, value);
            }
        }
        return Map.copyOf(levels);
    }

    private static String summarizeStartingLevels(Map<String, Integer> startingLevels) {
        if (startingLevels.isEmpty()) {
            return "No disciplines are inscribed yet.";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : startingLevels.entrySet()) {
            parts.add(entry.getKey() + " " + entry.getValue());
        }
        return String.join("   ", parts);
    }

    private static String progressionSkillDisplayName(Object progressionSkill) {
        try {
            Object value = progressionSkill.getClass().getMethod("displayName").invoke(progressionSkill);
            return value instanceof String string ? string : "";
        } catch (ReflectiveOperationException exception) {
            return "";
        }
    }

    private static ResourceLocation invokeResource(Object target, String methodName) throws ReflectiveOperationException {
        Object value = target.getClass().getMethod(methodName).invoke(target);
        return value instanceof ResourceLocation id ? id : null;
    }

    private static String invokeString(Object target, String methodName) throws ReflectiveOperationException {
        Object value = target.getClass().getMethod(methodName).invoke(target);
        return value instanceof String string ? string : "";
    }
}
