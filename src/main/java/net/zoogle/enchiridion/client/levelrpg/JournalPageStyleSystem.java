package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookRegionAction;
import net.zoogle.enchiridion.api.BookTextBlock;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class JournalPageStyleSystem {
    private static boolean debugTemplateLayout = true;
    private static final int DEBUG_SLOT_FILL = 0x00000000;
    private static final int DEBUG_SLOT_BORDER = 0x6647C7FF;
    private static final int DEBUG_INVALID_FILL = 0x00000000;
    private static final int DEBUG_INVALID_BORDER = 0xAAFF4D4D;
    private static final int CENTER_TOLERANCE = 2;
    private static final JournalTemplateStore TEMPLATE_STORE = JournalTemplateStore.load();

    private JournalPageStyleSystem() {}

    static StyledPageBuilder builder(JournalPagePurpose purpose, BookPageSide pageSide) {
        return new StyledPageBuilder(purpose, pageSide, templateFor(purpose, pageSide));
    }

    static boolean debugTemplateLayoutEnabled() {
        return debugTemplateLayout;
    }

    static void setDebugTemplateLayoutEnabled(boolean enabled) {
        debugTemplateLayout = enabled;
    }

    static TemplateSpec currentTemplate(JournalPagePurpose purpose, BookPageSide pageSide) {
        return templateFor(purpose, pageSide);
    }

    static void updateTemplateRegion(JournalPagePurpose purpose, JournalPageSlot slot, BookPageSide pageSide, SlotRegion region) {
        TEMPLATE_STORE.update(purpose, slot, normalize(pageSide, region));
    }

    static void saveTemplates() {
        TEMPLATE_STORE.save();
    }

    static JournalTemplateStore snapshotTemplates() {
        return TEMPLATE_STORE.copy();
    }

    static void restoreTemplates(JournalTemplateStore snapshot) {
        TEMPLATE_STORE.restoreFrom(snapshot);
    }

    static int ledgerRowsPerPage(BookPageSide pageSide) {
        TemplateSpec template = templateFor(JournalPagePurpose.LEDGER, pageSide);
        SlotDefinition rows = template.slot(JournalPageSlot.ROWS);
        return Math.max(1, rows.region().height() / Math.max(1, JournalElementFactory.ledgerRowHeight()));
    }

    static BookTextBlock.Kind kindFor(JournalTextRole role) {
        return switch (role) {
            case TITLE -> BookTextBlock.Kind.TITLE;
            case FOCAL -> BookTextBlock.Kind.LEVEL;
            case SUBTITLE -> BookTextBlock.Kind.SUBTITLE;
            case BODY -> BookTextBlock.Kind.BODY;
            case FOOTER -> BookTextBlock.Kind.BODY;
            case INTERACTION -> BookTextBlock.Kind.SUBTITLE;
        };
    }

    static String singleLine(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] parts = text.split("\\R");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return "";
    }

    static String distinctLine(String candidate, String fallback, String disallow) {
        String normalizedCandidate = singleLine(candidate);
        String normalizedDisallow = singleLine(disallow);
        if (normalizedCandidate.isEmpty() || normalizedCandidate.equalsIgnoreCase(normalizedDisallow)) {
            return singleLine(fallback);
        }
        return normalizedCandidate;
    }

    static String polishedSentence(String text) {
        String normalized = singleLine(text)
                .replace("Skill Levels", "skill levels")
                .replace("Skill Points", "skill points")
                .replace("Mastery", "mastery")
                .trim();
        if (normalized.isEmpty()) {
            return "";
        }
        if (!normalized.endsWith(".") && !normalized.endsWith("!") && !normalized.endsWith("?")) {
            normalized = normalized + ".";
        }
        return normalized;
    }

    static TemplateSpec templateFor(JournalPagePurpose purpose, BookPageSide pageSide) {
        JournalLayoutMetrics.PageContentRect content = JournalLayoutMetrics.pageContentRect(pageSide);
        return switch (purpose) {
            case CHARACTER_IDENTITY -> new TemplateSpec(
                    purpose,
                    pageSide,
                    Map.of(
                            JournalPageSlot.TITLE, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.TITLE, new JournalTemplateStore.NormalizedSlotRegion(0.0, 10.0 / 145.0, 1.0, 12.0 / 145.0)),
                                    new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 1.0f, 1.0f)
                            ),
                            JournalPageSlot.FOCAL, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.FOCAL, new JournalTemplateStore.NormalizedSlotRegion(0.0, 42.0 / 145.0, 1.0, 24.0 / 145.0)),
                                    new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 1.0f, 1.0f)
                            ),
                            JournalPageSlot.SUBTITLE, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.SUBTITLE, new JournalTemplateStore.NormalizedSlotRegion(4.0 / content.contentWidth(), 82.0 / 145.0, (double) (content.contentWidth() - 8) / content.contentWidth(), 12.0 / 145.0)),
                                    new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 1.0f, 1.0f)
                            ),
                            JournalPageSlot.BODY, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.BODY, new JournalTemplateStore.NormalizedSlotRegion(12.0 / content.contentWidth(), 106.0 / 145.0, (double) (content.contentWidth() - 24) / content.contentWidth(), 28.0 / 145.0)),
                                    new SlotFit(Alignment.CENTER, 4, true, OverflowPolicy.CLAMP, 1.0f, 1.0f)
                            ),
                            JournalPageSlot.FOOTER, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.FOOTER, new JournalTemplateStore.NormalizedSlotRegion(0.0, (double) (JournalLayoutMetrics.PAGE_CONTENT_HEIGHT - 6) / 145.0, 1.0, 8.0 / 145.0)),
                                    new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 1.0f, 1.0f)
                            )
                    ),
                    EnumSet.of(
                            JournalPageSlot.TITLE,
                            JournalPageSlot.FOCAL,
                            JournalPageSlot.SUBTITLE,
                            JournalPageSlot.BODY
                    ),
                    0
            );
            case CHARACTER_STANDING -> new TemplateSpec(
                    purpose,
                    pageSide,
                    Map.of(
                            JournalPageSlot.TITLE, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.TITLE, new JournalTemplateStore.NormalizedSlotRegion(0.0, 10.0 / 145.0, 1.0, 12.0 / 145.0)),
                                    new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 1.0f, 1.0f)
                            ),
                            JournalPageSlot.FOCAL, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.FOCAL, new JournalTemplateStore.NormalizedSlotRegion(0.0, 48.0 / 145.0, 1.0, 30.0 / 145.0)),
                                    new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 1.0f, 1.0f)
                            ),
                            JournalPageSlot.STATS, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.STATS, new JournalTemplateStore.NormalizedSlotRegion(6.0 / content.contentWidth(), 114.0 / 145.0, (double) (content.contentWidth() - 12) / content.contentWidth(), 28.0 / 145.0)),
                                    new SlotFit(Alignment.CENTER, 4, false, OverflowPolicy.CLAMP, 1.0f, 1.0f)
                            ),
                            JournalPageSlot.INTERACTION, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.INTERACTION, new JournalTemplateStore.NormalizedSlotRegion(0.0, (double) (JournalLayoutMetrics.PAGE_CONTENT_HEIGHT - 8) / 145.0, 1.0, 8.0 / 145.0)),
                                    new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 1.0f, 1.0f)
                            )
                    ),
                    EnumSet.of(
                            JournalPageSlot.TITLE,
                            JournalPageSlot.FOCAL,
                            JournalPageSlot.STATS,
                            JournalPageSlot.INTERACTION
                    ),
                    1
            );
            case LEDGER -> new TemplateSpec(
                    purpose,
                    pageSide,
                    Map.of(
                            JournalPageSlot.TITLE, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.TITLE, new JournalTemplateStore.NormalizedSlotRegion(0.0, 10.0 / 145.0, 1.0, 14.0 / 145.0)),
                                    new SlotFit(Alignment.LEFT, 1, false, OverflowPolicy.ELLIPSIZE, 1.0f, 1.0f)
                            ),
                            JournalPageSlot.ROWS, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.ROWS, new JournalTemplateStore.NormalizedSlotRegion(0.0, 30.0 / 145.0, 1.0, (double) (JournalLayoutMetrics.PAGE_CONTENT_HEIGHT - 36) / 145.0)),
                                    new SlotFit(Alignment.LEFT, Integer.MAX_VALUE, false, OverflowPolicy.INVALID, 1.0f, 1.0f)
                            )
                    ),
                    EnumSet.of(JournalPageSlot.ROWS),
                    0
            );
            case SKILL_DETAIL -> new TemplateSpec(
                    purpose,
                    pageSide,
                    Map.of(
                            JournalPageSlot.TITLE, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.TITLE, new JournalTemplateStore.NormalizedSlotRegion(0.0, 10.0 / 145.0, 1.0, 12.0 / 145.0)),
                                    new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 1.0f, 1.0f)
                            ),
                            JournalPageSlot.FOCAL, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.FOCAL, new JournalTemplateStore.NormalizedSlotRegion(0.0, 46.0 / 145.0, 1.0, 24.0 / 145.0)),
                                    new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 1.0f, 1.0f)
                            ),
                            JournalPageSlot.BODY, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.BODY, new JournalTemplateStore.NormalizedSlotRegion(10.0 / content.contentWidth(), 88.0 / 145.0, (double) (content.contentWidth() - 20) / content.contentWidth(), 28.0 / 145.0)),
                                    new SlotFit(Alignment.CENTER, 4, true, OverflowPolicy.CLAMP, 1.0f, 1.0f)
                            ),
                            JournalPageSlot.INTERACTION, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.INTERACTION, new JournalTemplateStore.NormalizedSlotRegion(0.0, (double) (JournalLayoutMetrics.PAGE_CONTENT_HEIGHT - 10) / 145.0, 1.0, 10.0 / 145.0)),
                                    new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 1.0f, 1.0f)
                            )
                    ),
                    EnumSet.of(
                            JournalPageSlot.TITLE,
                            JournalPageSlot.FOCAL,
                            JournalPageSlot.BODY,
                            JournalPageSlot.INTERACTION
                    ),
                    1
            );
            case FRONT_MATTER -> new TemplateSpec(
                    purpose,
                    pageSide,
                    Map.of(
                            JournalPageSlot.TITLE, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.TITLE, new JournalTemplateStore.NormalizedSlotRegion(0.0, 16.0 / 145.0, 1.0, 14.0 / 145.0)),
                                    new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 1.0f, 1.0f)
                            ),
                            JournalPageSlot.BODY, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.BODY, new JournalTemplateStore.NormalizedSlotRegion(8.0 / content.contentWidth(), 52.0 / 145.0, (double) (content.contentWidth() - 16) / content.contentWidth(), 44.0 / 145.0)),
                                    new SlotFit(Alignment.CENTER, 4, true, OverflowPolicy.CLAMP, 1.0f, 1.0f)
                            ),
                            JournalPageSlot.FOOTER, new SlotDefinition(
                                    region(purpose, pageSide, JournalPageSlot.FOOTER, new JournalTemplateStore.NormalizedSlotRegion(0.0, (double) (JournalLayoutMetrics.PAGE_CONTENT_HEIGHT - 10) / 145.0, 1.0, 10.0 / 145.0)),
                                    new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 1.0f, 1.0f)
                            )
                    ),
                    EnumSet.of(
                            JournalPageSlot.TITLE,
                            JournalPageSlot.BODY
                    ),
                    0
            );
        };
    }

    static final class StyledPageBuilder {
        private final TemplateSpec template;
        private final List<BookPageElement> elements = new ArrayList<>();
        private final List<PlacedBounds> measuredBounds = new ArrayList<>();
        private final Map<JournalPageSlot, Integer> slotUse = new EnumMap<>(JournalPageSlot.class);
        private final List<String> validationErrors = new ArrayList<>();
        private int visibleInteractionCount;

        private StyledPageBuilder(JournalPagePurpose purpose, BookPageSide pageSide, TemplateSpec template) {
            this.template = template;
        }

        void addCenteredText(JournalPageSlot slot, JournalTextRole role, String text) {
            SlotDefinition definition = registerSlot(slot);
            FittedLine line = fitSingleLine(text, role, definition);
            elements.add(JournalElementFactory.centeredRoleTextElement(role, line.text(), definition.region().x(), definition.region().y(), definition.region().width()));
            measuredBounds.add(new PlacedBounds(slot, new SlotRegion(line.x(), definition.region().y(), line.width(), line.height()), definition.region(), role, false));
        }

        void addCenteredBody(JournalPageSlot slot, String text) {
            SlotDefinition definition = registerSlot(slot);
            List<String> lines = fitWrappedText(text, definition);
            int cursorY = definition.region().y();
            int lineHeight = JournalLayoutMetrics.lineHeightFor(kindFor(JournalTextRole.BODY));
            for (String line : lines) {
                int textWidth = Math.max(1, Minecraft.getInstance().font.width(line));
                int drawX = centeredX(definition.region(), textWidth);
                elements.add(JournalElementFactory.centeredRoleTextElement(JournalTextRole.BODY, line, definition.region().x(), cursorY, definition.region().width()));
                measuredBounds.add(new PlacedBounds(slot, new SlotRegion(drawX, cursorY, textWidth, lineHeight), definition.region(), JournalTextRole.BODY, false));
                cursorY += lineHeight;
            }
            if (cursorY > definition.region().bottom()) {
                validationErrors.add("Body overflowed slot " + slot);
            }
        }

        void addCenteredStats(JournalPageSlot slot, List<String> rows, int gap) {
            SlotDefinition definition = registerSlot(slot);
            List<String> clamped = clampRows(rows, definition.fit().maxLines());
            int cursorY = definition.region().y();
            int lineHeight = JournalLayoutMetrics.lineHeightFor(kindFor(JournalTextRole.BODY));
            for (String row : clamped) {
                int textWidth = Math.max(1, Minecraft.getInstance().font.width(row));
                int drawX = centeredX(definition.region(), textWidth);
                elements.add(JournalElementFactory.centeredRoleTextElement(JournalTextRole.BODY, row, definition.region().x(), cursorY, definition.region().width()));
                measuredBounds.add(new PlacedBounds(slot, new SlotRegion(drawX, cursorY, textWidth, lineHeight), definition.region(), JournalTextRole.BODY, false));
                cursorY += lineHeight + Math.max(0, gap);
            }
            if (!clamped.isEmpty()) {
                cursorY -= Math.max(0, gap);
            }
            if (cursorY > definition.region().bottom()) {
                validationErrors.add("Stats overflowed slot " + slot);
            }
        }

        void addLedgerRows(JournalPageSlot slot, List<JournalCharacterStat> rows, Map<String, Integer> skillStartPages) {
            SlotDefinition definition = registerSlot(slot);
            int rowHeight = JournalElementFactory.ledgerRowHeight();
            int maxRows = Math.max(1, definition.region().height() / Math.max(1, rowHeight));
            int cursorY = definition.region().y();
            int placed = 0;
            for (JournalCharacterStat row : rows) {
                Integer targetPageIndex = skillStartPages.get(row.name());
                if (targetPageIndex == null) {
                    continue;
                }
                if (placed >= maxRows) {
                    validationErrors.add("Ledger rows overflowed slot " + slot);
                    break;
                }
                JournalElementFactory.LedgerRowLayout layout = JournalElementFactory.ledgerRowLayout(template.pageSide(), row, cursorY, targetPageIndex);
                elements.add(JournalElementFactory.ledgerLabel(layout));
                elements.add(JournalElementFactory.decorationElement(BookTextBlock.Kind.BODY, layout.dotsText(), layout.dotsX(), layout.labelY()));
                elements.add(JournalElementFactory.decorationElement(BookTextBlock.Kind.BODY, layout.valueText(), layout.valueX(), layout.labelY()));
                measuredBounds.add(new PlacedBounds(slot, new SlotRegion(layout.labelX(), layout.labelY(), layout.labelWidth(), layout.labelHeight()), definition.region(), JournalTextRole.INTERACTION, true));
                measuredBounds.add(new PlacedBounds(slot, new SlotRegion(layout.dotsX(), layout.labelY(), Math.max(1, Minecraft.getInstance().font.width(layout.dotsText())), JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.BODY)), definition.region(), JournalTextRole.BODY, false));
                measuredBounds.add(new PlacedBounds(slot, new SlotRegion(layout.valueX(), layout.labelY(), Math.max(1, Minecraft.getInstance().font.width(layout.valueText())), JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.BODY)), definition.region(), JournalTextRole.BODY, false));
                cursorY += layout.rowHeight();
                placed++;
            }
        }

        void addBottomInteraction(String stableId, String text, Component tooltip, BookRegionAction action) {
            SlotDefinition definition = registerSlot(JournalPageSlot.INTERACTION);
            if (visibleInteractionCount >= template.maxVisibleInteractions()) {
                validationErrors.add("Too many visible interactions for " + template.purpose());
                return;
            }
            visibleInteractionCount++;
            FittedLine line = fitSingleLine(text, JournalTextRole.INTERACTION, definition);
            elements.add(JournalElementFactory.toInteractiveTextElement(
                    JournalElementFactory.centeredRoleInteractiveTextLayout(
                            stableId,
                            JournalTextRole.INTERACTION,
                            line.text(),
                            template.pageSide(),
                            definition.region().y(),
                            tooltip,
                            action
                    )
            ));
            measuredBounds.add(new PlacedBounds(JournalPageSlot.INTERACTION, new SlotRegion(line.x(), definition.region().y(), line.width(), line.height()), definition.region(), JournalTextRole.INTERACTION, true));
        }

        BookPage build() {
            validateNoCollisions();
            validateExpectedSlots();
            if (debugTemplateLayout) {
                appendDebugRegions();
            }
            return BookPage.of(List.of(), elements);
        }

        private SlotDefinition registerSlot(JournalPageSlot slot) {
            SlotDefinition definition = template.slot(slot);
            if (definition == null) {
                throw new IllegalStateException("Slot " + slot + " is not allowed for page " + template.purpose());
            }
            int uses = slotUse.getOrDefault(slot, 0);
            if (slot != JournalPageSlot.BODY && slot != JournalPageSlot.STATS && uses > 0) {
                throw new IllegalStateException("Slot " + slot + " already used for page " + template.purpose());
            }
            slotUse.put(slot, uses + 1);
            return definition;
        }

        private void validateExpectedSlots() {
            for (JournalPageSlot slot : template.requiredSlots()) {
                if (!slotUse.containsKey(slot)) {
                    validationErrors.add("Missing slot " + slot + " for page " + template.purpose());
                }
            }
        }

        private void validateNoCollisions() {
            for (int leftIndex = 0; leftIndex < measuredBounds.size(); leftIndex++) {
                PlacedBounds left = measuredBounds.get(leftIndex);
                validateCentered(left);
                validateInsideRegion(left);
                for (int rightIndex = leftIndex + 1; rightIndex < measuredBounds.size(); rightIndex++) {
                    PlacedBounds right = measuredBounds.get(rightIndex);
                    if (intersects(left.bounds(), right.bounds())) {
                        validationErrors.add("Collision between " + left.slot() + " and " + right.slot());
                    }
                }
            }
        }

        private void validateCentered(PlacedBounds bounds) {
            SlotDefinition definition = template.slot(bounds.slot());
            if (definition != null && definition.fit().alignment() == Alignment.CENTER) {
                int expectedX = centeredX(bounds.region(), bounds.bounds().width());
                if (Math.abs(bounds.bounds().x() - expectedX) > CENTER_TOLERANCE) {
                    validationErrors.add("Centered element drift in slot " + bounds.slot());
                }
            }
        }

        private void validateInsideRegion(PlacedBounds bounds) {
            if (bounds.bounds().x() < bounds.region().x()
                    || bounds.bounds().right() > bounds.region().right()
                    || bounds.bounds().y() < bounds.region().y()
                    || bounds.bounds().bottom() > bounds.region().bottom()) {
                validationErrors.add("Bounds escaped slot " + bounds.slot());
            }
        }

        private void appendDebugRegions() {
            for (Map.Entry<JournalPageSlot, SlotDefinition> entry : template.slots().entrySet()) {
                SlotRegion region = entry.getValue().region();
                elements.add(new BookPageElement.BoxElement(region.x(), region.y(), region.width(), region.height(), DEBUG_SLOT_FILL, DEBUG_SLOT_BORDER, BookPageElement.PanelVisualStyle.PANEL));
            }
            if (!validationErrors.isEmpty()) {
                for (PlacedBounds bounds : measuredBounds) {
                    elements.add(new BookPageElement.BoxElement(bounds.bounds().x(), bounds.bounds().y(), bounds.bounds().width(), bounds.bounds().height(), DEBUG_INVALID_FILL, DEBUG_INVALID_BORDER, BookPageElement.PanelVisualStyle.EMPHASIS));
                }
            }
        }

        private FittedLine fitSingleLine(String text, JournalTextRole role, SlotDefinition definition) {
            String normalized = singleLine(text);
            int availableWidth = definition.region().width();
            String fitted = applyOverflow(normalized, availableWidth, definition.fit().overflowPolicy());
            int width = Math.max(1, Minecraft.getInstance().font.width(fitted));
            int x = switch (definition.fit().alignment()) {
                case CENTER -> centeredX(definition.region(), width);
                case LEFT -> definition.region().x();
            };
            int height = JournalLayoutMetrics.lineHeightFor(kindFor(role));
            return new FittedLine(fitted, x, width, height);
        }

        private List<String> fitWrappedText(String text, SlotDefinition definition) {
            List<String> lines = clampBodyLines(text, definition.region().width(), Math.min(definition.fit().maxLines(), maxLinesThatFit(definition.region(), kindFor(JournalTextRole.BODY))));
            if (lines.isEmpty() && !singleLine(text).isBlank()) {
                validationErrors.add("Body produced no visible lines");
            }
            return lines;
        }

        private int maxLinesThatFit(SlotRegion region, BookTextBlock.Kind kind) {
            int lineHeight = JournalLayoutMetrics.lineHeightFor(kind);
            return Math.max(1, region.height() / Math.max(1, lineHeight));
        }

        private String applyOverflow(String text, int width, OverflowPolicy policy) {
            if (Minecraft.getInstance().font.width(text) <= width) {
                return text;
            }
            return switch (policy) {
                case ELLIPSIZE, CLAMP -> withEllipsis(text, width);
                case INVALID -> {
                    validationErrors.add("Text overflow for page " + template.purpose());
                    yield text;
                }
            };
        }
    }

    static List<String> clampRows(List<String> rows, int maxRows) {
        return List.copyOf(rows.subList(0, Math.min(rows.size(), maxRows)));
    }

    private static int centeredX(SlotRegion region, int textWidth) {
        return region.x() + Math.max(0, (region.width() - textWidth) / 2);
    }

    private static String withEllipsis(String line, int width) {
        String base = line == null ? "" : line.trim();
        if (base.isEmpty()) {
            return "...";
        }
        String candidate = base + "...";
        while (!base.isEmpty() && Minecraft.getInstance().font.width(candidate) > width) {
            base = base.substring(0, base.length() - 1).trim();
            candidate = base + "...";
        }
        return candidate;
    }

    static List<String> clampBodyLines(String text, int width, int maxLines) {
        List<JournalPaginationEngine.MeasuredLine> wrapped = JournalPaginationEngine.wrapBodyText(text, width);
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < wrapped.size() && lines.size() < maxLines; index++) {
            String line = wrapped.get(index).text();
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        if (wrapped.size() > maxLines && !lines.isEmpty()) {
            int lastIndex = lines.size() - 1;
            lines.set(lastIndex, withEllipsis(lines.get(lastIndex), width));
        }
        return lines;
    }

    private static boolean intersects(SlotRegion left, SlotRegion right) {
        return left.x() < right.right()
                && left.right() > right.x()
                && left.y() < right.bottom()
                && left.bottom() > right.y();
    }

    private static SlotRegion region(
            JournalPagePurpose purpose,
            BookPageSide pageSide,
            JournalPageSlot slot,
            JournalTemplateStore.NormalizedSlotRegion defaults
    ) {
        JournalTemplateStore.NormalizedSlotRegion normalized = TEMPLATE_STORE.regionFor(purpose, slot, defaults);
        return denormalize(pageSide, normalized);
    }

    private static JournalTemplateStore.NormalizedSlotRegion normalize(BookPageSide pageSide, SlotRegion region) {
        JournalLayoutMetrics.PageContentRect content = JournalLayoutMetrics.pageContentRect(pageSide);
        return new JournalTemplateStore.NormalizedSlotRegion(
                (double) (region.x() - content.contentX()) / Math.max(1, content.contentWidth()),
                (double) region.y() / 145.0,
                (double) region.width() / Math.max(1, content.contentWidth()),
                (double) region.height() / 145.0
        ).clamped();
    }

    private static SlotRegion denormalize(BookPageSide pageSide, JournalTemplateStore.NormalizedSlotRegion normalized) {
        JournalLayoutMetrics.PageContentRect content = JournalLayoutMetrics.pageContentRect(pageSide);
        int x = content.contentX() + (int) Math.round(normalized.x() * content.contentWidth());
        int y = (int) Math.round(normalized.y() * 145.0);
        int width = Math.max(4, (int) Math.round(normalized.width() * content.contentWidth()));
        int height = Math.max(4, (int) Math.round(normalized.height() * 145.0));
        int maxX = content.contentX() + content.contentWidth();
        int clampedX = Math.max(content.contentX(), Math.min(x, maxX - width));
        int clampedY = Math.max(0, Math.min(y, 145 - height));
        return new SlotRegion(clampedX, clampedY, Math.min(width, maxX - clampedX), Math.min(height, 145 - clampedY));
    }

    record TemplateSpec(
            JournalPagePurpose purpose,
            BookPageSide pageSide,
            Map<JournalPageSlot, SlotDefinition> slots,
            Set<JournalPageSlot> requiredSlots,
            int maxVisibleInteractions
    ) {
        SlotDefinition slot(JournalPageSlot slot) {
            return slots.get(slot);
        }
    }

    record SlotDefinition(SlotRegion region, SlotFit fit) {}

    record SlotRegion(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }
    }

    record SlotFit(
            Alignment alignment,
            int maxLines,
            boolean wrapAllowed,
            OverflowPolicy overflowPolicy,
            float minScale,
            float maxScale
    ) {}

    private record FittedLine(String text, int x, int width, int height) {}

    private record PlacedBounds(
            JournalPageSlot slot,
            SlotRegion bounds,
            SlotRegion region,
            JournalTextRole role,
            boolean interactive
    ) {}

    enum Alignment {
        LEFT,
        CENTER
    }

    enum OverflowPolicy {
        CLAMP,
        ELLIPSIZE,
        INVALID
    }
}
