package net.zoogle.enchiridion.client.levelrpg.journal.style;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public final class JournalTemplateStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path TEMPLATE_PATH = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config")
            .resolve("enchiridion_journal_templates.json");

    private final Map<JournalPagePurpose, Map<JournalPageSlot, NormalizedSlotRegion>> overrides;

    private JournalTemplateStore(Map<JournalPagePurpose, Map<JournalPageSlot, NormalizedSlotRegion>> overrides) {
        this.overrides = overrides;
    }

    public static JournalTemplateStore load() {
        Map<JournalPagePurpose, Map<JournalPageSlot, NormalizedSlotRegion>> overrides = new EnumMap<>(JournalPagePurpose.class);
        if (!Files.exists(TEMPLATE_PATH)) {
            return new JournalTemplateStore(overrides);
        }
        try (Reader reader = Files.newBufferedReader(TEMPLATE_PATH)) {
            SerializedTemplates serialized = GSON.fromJson(reader, SerializedTemplates.class);
            if (serialized == null || serialized.templates == null) {
                return new JournalTemplateStore(overrides);
            }
            for (Map.Entry<String, Map<String, NormalizedSlotRegion>> templateEntry : serialized.templates.entrySet()) {
                JournalPagePurpose purpose = parsePurpose(templateEntry.getKey());
                if (purpose == null) {
                    continue;
                }
                Map<JournalPageSlot, NormalizedSlotRegion> slotMap = new EnumMap<>(JournalPageSlot.class);
                for (Map.Entry<String, NormalizedSlotRegion> slotEntry : templateEntry.getValue().entrySet()) {
                    JournalPageSlot slot = parseSlot(slotEntry.getKey());
                    if (slot != null && slotEntry.getValue() != null) {
                        slotMap.put(slot, slotEntry.getValue().clamped());
                    }
                }
                if (!slotMap.isEmpty()) {
                    overrides.put(purpose, slotMap);
                }
            }
        } catch (IOException | JsonParseException ignored) {
            return new JournalTemplateStore(new EnumMap<>(JournalPagePurpose.class));
        }
        return new JournalTemplateStore(overrides);
    }

    public NormalizedSlotRegion regionFor(JournalPagePurpose purpose, JournalPageSlot slot, NormalizedSlotRegion defaults) {
        return overrides.getOrDefault(purpose, Map.of()).getOrDefault(slot, defaults).clamped();
    }

    public void update(JournalPagePurpose purpose, JournalPageSlot slot, NormalizedSlotRegion region) {
        overrides.computeIfAbsent(purpose, ignored -> new EnumMap<>(JournalPageSlot.class)).put(slot, region.clamped());
    }

    public JournalTemplateStore copy() {
        Map<JournalPagePurpose, Map<JournalPageSlot, NormalizedSlotRegion>> copied = new EnumMap<>(JournalPagePurpose.class);
        for (Map.Entry<JournalPagePurpose, Map<JournalPageSlot, NormalizedSlotRegion>> entry : overrides.entrySet()) {
            copied.put(entry.getKey(), new EnumMap<>(entry.getValue()));
        }
        return new JournalTemplateStore(copied);
    }

    public void restoreFrom(JournalTemplateStore other) {
        overrides.clear();
        for (Map.Entry<JournalPagePurpose, Map<JournalPageSlot, NormalizedSlotRegion>> entry : other.overrides.entrySet()) {
            overrides.put(entry.getKey(), new EnumMap<>(entry.getValue()));
        }
    }

    public void save() {
        SerializedTemplates serialized = new SerializedTemplates();
        serialized.templates = new java.util.LinkedHashMap<>();
        for (Map.Entry<JournalPagePurpose, Map<JournalPageSlot, NormalizedSlotRegion>> templateEntry : overrides.entrySet()) {
            Map<String, NormalizedSlotRegion> slots = new java.util.LinkedHashMap<>();
            for (Map.Entry<JournalPageSlot, NormalizedSlotRegion> slotEntry : templateEntry.getValue().entrySet()) {
                slots.put(slotEntry.getKey().name(), slotEntry.getValue().clamped());
            }
            serialized.templates.put(templateEntry.getKey().name(), slots);
        }
        try {
            Files.createDirectories(TEMPLATE_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(TEMPLATE_PATH)) {
                GSON.toJson(serialized, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static JournalPagePurpose parsePurpose(String value) {
        try {
            return JournalPagePurpose.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static JournalPageSlot parseSlot(String value) {
        try {
            return JournalPageSlot.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public record NormalizedSlotRegion(double x, double y, double width, double height) {
        NormalizedSlotRegion clamped() {
            double clampedX = clamp(x, 0.0, 1.0);
            double clampedY = clamp(y, 0.0, 1.0);
            double clampedWidth = clamp(width, 0.02, 1.0 - clampedX);
            double clampedHeight = clamp(height, 0.02, 1.0 - clampedY);
            return new NormalizedSlotRegion(clampedX, clampedY, clampedWidth, clampedHeight);
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private static final class SerializedTemplates {
        Map<String, Map<String, NormalizedSlotRegion>> templates;
    }
}
