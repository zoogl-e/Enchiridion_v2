package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.api.BookContext;

import java.util.Map;
import java.util.Objects;

public final class LevelRpgJournalSnapshotFactory {
    static final Map<String, String> CANONICAL_SKILL_PATHS = Map.of(
            "Valor", "valor",
            "Vitality", "vitality",
            "Mining", "mining",
            "Culinary", "culinary",
            "Forging", "forging",
            "Artificing", "artificing",
            "Magick", "magick",
            "Exploration", "exploration"
    );
    private LevelRpgJournalSnapshotFactory() {}

    public static LevelRpgJournalSnapshot create(BookContext context) {
        return LevelRpgJournalSnapshotBridge.create(context, MockLevelRpgJournalSnapshots.create(context));
    }

    static String journalSkillName(ResourceLocation skillId) {
        String path = skillId.getPath();
        for (Map.Entry<String, String> entry : CANONICAL_SKILL_PATHS.entrySet()) {
            if (Objects.equals(entry.getValue(), path)) {
                return entry.getKey();
            }
        }
        return null;
    }

    static String humanizePath(String path) {
        if (path == null || path.isBlank()) {
            return "Unknown";
        }
        String[] parts = path.split("[_-]");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }
}
