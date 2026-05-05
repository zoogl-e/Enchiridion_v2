package net.zoogle.enchiridion.client.levelrpg.journal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.zoogle.enchiridion.client.levelrpg.journal.style.JournalPagePurpose;
import net.zoogle.enchiridion.client.levelrpg.journal.style.JournalPageSlot;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JournalContentStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONTENT_PATH = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config")
            .resolve("enchiridion_journal_content.json");
    private static final JournalContentStore INSTANCE = load();

    private final Map<String, PageContentOverride> pageOverridesById;
    private final Map<Integer, PageContentOverride> legacyPageOverrides;
    private boolean migrationSavePending;

    private JournalContentStore(
            Map<String, PageContentOverride> pageOverridesById,
            Map<Integer, PageContentOverride> legacyPageOverrides
    ) {
        this.pageOverridesById = pageOverridesById;
        this.legacyPageOverrides = legacyPageOverrides;
    }

    public static JournalContentStore instance() {
        return INSTANCE;
    }

    public PageContentView page(JournalPageId pageId, int pageIndex, JournalPagePurpose purpose) {
        return new PageContentView(pageId, pageIndex, purpose);
    }

    public void putText(JournalPageId pageId, int pageIndex, JournalPagePurpose purpose, JournalPageSlot slot, String value) {
        if (pageId != null) {
            PageContentOverride page = pageOverridesById.computeIfAbsent(pageId.value(), ignored -> new PageContentOverride(purpose, new EnumMap<>(JournalPageSlot.class)));
            page.slots().put(slot, value == null ? "" : value);
            page.purpose = purpose;
            legacyPageOverrides.remove(pageIndex);
            return;
        }
        PageContentOverride page = legacyPageOverrides.computeIfAbsent(pageIndex, ignored -> new PageContentOverride(purpose, new EnumMap<>(JournalPageSlot.class)));
        page.slots().put(slot, value == null ? "" : value);
        page.purpose = purpose;
    }

    public void save() {
        SerializedContent serialized = new SerializedContent();
        serialized.pagesById = new LinkedHashMap<>();
        for (Map.Entry<String, PageContentOverride> pageEntry : pageOverridesById.entrySet()) {
            SerializedPage page = new SerializedPage();
            page.purpose = pageEntry.getValue().purpose.name();
            page.slots = new LinkedHashMap<>();
            for (Map.Entry<JournalPageSlot, String> slotEntry : pageEntry.getValue().slots().entrySet()) {
                page.slots.put(slotEntry.getKey().name(), slotEntry.getValue());
            }
            serialized.pagesById.put(pageEntry.getKey(), page);
        }
        serialized.pages = new LinkedHashMap<>();
        for (Map.Entry<Integer, PageContentOverride> pageEntry : legacyPageOverrides.entrySet()) {
            SerializedPage page = new SerializedPage();
            page.purpose = pageEntry.getValue().purpose.name();
            page.slots = new LinkedHashMap<>();
            for (Map.Entry<JournalPageSlot, String> slotEntry : pageEntry.getValue().slots().entrySet()) {
                page.slots.put(slotEntry.getKey().name(), slotEntry.getValue());
            }
            serialized.pages.put(String.valueOf(pageEntry.getKey()), page);
        }
        try {
            Files.createDirectories(CONTENT_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONTENT_PATH)) {
                GSON.toJson(serialized, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static JournalContentStore load() {
        Map<String, PageContentOverride> overridesById = new LinkedHashMap<>();
        Map<Integer, PageContentOverride> legacyOverrides = new LinkedHashMap<>();
        if (!Files.exists(CONTENT_PATH)) {
            return new JournalContentStore(overridesById, legacyOverrides);
        }
        try (Reader reader = Files.newBufferedReader(CONTENT_PATH)) {
            SerializedContent serialized = GSON.fromJson(reader, SerializedContent.class);
            if (serialized == null) {
                return new JournalContentStore(overridesById, legacyOverrides);
            }
            if (serialized.pagesById != null) {
                for (Map.Entry<String, SerializedPage> pageEntry : serialized.pagesById.entrySet()) {
                    JournalPagePurpose purpose;
                    try {
                        purpose = JournalPagePurpose.valueOf(pageEntry.getValue().purpose);
                    } catch (Exception ignored) {
                        continue;
                    }
                    Map<JournalPageSlot, String> slots = new EnumMap<>(JournalPageSlot.class);
                    if (pageEntry.getValue().slots != null) {
                        for (Map.Entry<String, String> slotEntry : pageEntry.getValue().slots.entrySet()) {
                            try {
                                slots.put(JournalPageSlot.valueOf(slotEntry.getKey()), slotEntry.getValue());
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                    overridesById.put(pageEntry.getKey(), new PageContentOverride(purpose, slots));
                }
            }
            if (serialized.pages != null) {
                for (Map.Entry<String, SerializedPage> pageEntry : serialized.pages.entrySet()) {
                int pageIndex;
                try {
                    pageIndex = Integer.parseInt(pageEntry.getKey());
                } catch (NumberFormatException ignored) {
                    continue;
                }
                JournalPagePurpose purpose;
                try {
                    purpose = JournalPagePurpose.valueOf(pageEntry.getValue().purpose);
                } catch (Exception ignored) {
                    continue;
                }
                Map<JournalPageSlot, String> slots = new EnumMap<>(JournalPageSlot.class);
                if (pageEntry.getValue().slots != null) {
                    for (Map.Entry<String, String> slotEntry : pageEntry.getValue().slots.entrySet()) {
                        try {
                            slots.put(JournalPageSlot.valueOf(slotEntry.getKey()), slotEntry.getValue());
                        } catch (IllegalArgumentException ignored) {
                            }
                    }
                }
                legacyOverrides.put(pageIndex, new PageContentOverride(purpose, slots));
                }
            }
        } catch (IOException | JsonParseException ignored) {
            return new JournalContentStore(new LinkedHashMap<>(), new LinkedHashMap<>());
        }
        return new JournalContentStore(overridesById, legacyOverrides);
    }

    private PageContentOverride resolveOverride(JournalPageId pageId, int pageIndex, JournalPagePurpose purpose) {
        if (pageId != null) {
            PageContentOverride semantic = pageOverridesById.get(pageId.value());
            if (semantic != null) {
                return semantic;
            }
            for (JournalPageId alias : JournalPageIds.aliasesFor(pageId)) {
                PageContentOverride aliased = pageOverridesById.remove(alias.value());
                if (aliased != null) {
                    pageOverridesById.put(pageId.value(), aliased);
                    saveAfterMigration();
                    return aliased;
                }
            }
            PageContentOverride legacy = legacyPageOverrides.get(pageIndex);
            if (legacy != null && legacy.purpose == purpose) {
                pageOverridesById.put(pageId.value(), legacy);
                legacyPageOverrides.remove(pageIndex);
                saveAfterMigration();
                return legacy;
            }
            return null;
        }
        return legacyPageOverrides.get(pageIndex);
    }

    private void saveAfterMigration() {
        if (migrationSavePending) {
            return;
        }
        migrationSavePending = true;
        try {
            save();
        } finally {
            migrationSavePending = false;
        }
    }

    public final class PageContentView {
        private final JournalPageId pageId;
        private final int pageIndex;
        private final JournalPagePurpose purpose;

        private PageContentView(JournalPageId pageId, int pageIndex, JournalPagePurpose purpose) {
            this.pageId = pageId;
            this.pageIndex = pageIndex;
            this.purpose = purpose;
        }

        public String text(JournalPageSlot slot, String defaultValue) {
            PageContentOverride override = JournalContentStore.this.resolveOverride(pageId, pageIndex, purpose);
            return override == null ? defaultValue : override.slots().getOrDefault(slot, defaultValue);
        }

        public void putText(JournalPageSlot slot, String value) {
            JournalContentStore.this.putText(pageId, pageIndex, purpose, slot, value);
        }
    }

    private static final class PageContentOverride {
        private JournalPagePurpose purpose;
        private final Map<JournalPageSlot, String> slots;

        private PageContentOverride(JournalPagePurpose purpose, Map<JournalPageSlot, String> slots) {
            this.purpose = purpose;
            this.slots = slots;
        }

        Map<JournalPageSlot, String> slots() {
            return slots;
        }
    }

    private static final class SerializedContent {
        Map<String, SerializedPage> pagesById;
        Map<String, SerializedPage> pages;
    }

    private static final class SerializedPage {
        String purpose;
        Map<String, String> slots;
    }
}
