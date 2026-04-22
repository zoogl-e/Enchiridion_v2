package net.zoogle.enchiridion.client.levelrpg;

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
import java.util.LinkedHashMap;
import java.util.Map;

final class JournalContentStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONTENT_PATH = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config")
            .resolve("enchiridion_journal_content.json");
    private static final JournalContentStore INSTANCE = load();

    private final Map<Integer, PageContentOverride> pageOverrides;

    private JournalContentStore(Map<Integer, PageContentOverride> pageOverrides) {
        this.pageOverrides = pageOverrides;
    }

    static JournalContentStore instance() {
        return INSTANCE;
    }

    String text(int pageIndex, JournalPageSlot slot, String defaultValue) {
        PageContentOverride page = pageOverrides.get(pageIndex);
        if (page == null) {
            return defaultValue;
        }
        return page.slots().getOrDefault(slot, defaultValue);
    }

    void putText(int pageIndex, JournalPagePurpose purpose, JournalPageSlot slot, String value) {
        PageContentOverride page = pageOverrides.computeIfAbsent(pageIndex, ignored -> new PageContentOverride(purpose, new EnumMap<>(JournalPageSlot.class)));
        page.slots().put(slot, value == null ? "" : value);
        page.purpose = purpose;
    }

    void save() {
        SerializedContent serialized = new SerializedContent();
        serialized.pages = new LinkedHashMap<>();
        for (Map.Entry<Integer, PageContentOverride> pageEntry : pageOverrides.entrySet()) {
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
        Map<Integer, PageContentOverride> overrides = new LinkedHashMap<>();
        if (!Files.exists(CONTENT_PATH)) {
            return new JournalContentStore(overrides);
        }
        try (Reader reader = Files.newBufferedReader(CONTENT_PATH)) {
            SerializedContent serialized = GSON.fromJson(reader, SerializedContent.class);
            if (serialized == null || serialized.pages == null) {
                return new JournalContentStore(overrides);
            }
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
                overrides.put(pageIndex, new PageContentOverride(purpose, slots));
            }
        } catch (IOException | JsonParseException ignored) {
            return new JournalContentStore(new LinkedHashMap<>());
        }
        return new JournalContentStore(overrides);
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
        Map<String, SerializedPage> pages;
    }

    private static final class SerializedPage {
        String purpose;
        Map<String, String> slots;
    }
}
