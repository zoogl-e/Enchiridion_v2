package net.zoogle.enchiridion.client.levelrpg.journal.style;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookPageElement;
import net.zoogle.enchiridion.api.BookPageSide;
import net.zoogle.enchiridion.api.BookRegionAction;
import net.zoogle.enchiridion.api.BookTextBlock;
import net.zoogle.enchiridion.client.render.PageCanvasRenderer;
import net.zoogle.enchiridion.client.ui.BookDebugSettings;
import net.zoogle.enchiridion.client.levelrpg.model.JournalCharacterStat;
import net.zoogle.enchiridion.client.levelrpg.journal.layout.JournalElementFactory;
import net.zoogle.enchiridion.client.levelrpg.journal.layout.JournalLayoutMetrics;
import net.zoogle.enchiridion.client.levelrpg.journal.layout.JournalPaginationEngine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JournalPageStyleSystem {
    private static boolean debugTemplateLayout = false;
    private static final int DEBUG_SLOT_FILL = 0x00000000;
    private static final int DEBUG_SLOT_BORDER = 0x6647C7FF;
    private static final int DEBUG_MEASURED_FILL = 0x00000000;
    private static final int DEBUG_MEASURED_BORDER = 0x88FF6B6B;
    private static final int DEBUG_INTERACTION_MEASURED_BORDER = 0x88FFE27A;
    private static final int DEBUG_INVALID_FILL = 0x00000000;
    private static final int DEBUG_INVALID_BORDER = 0xAAFF4D4D;
    private static final int CENTER_TOLERANCE = 2;
    private static final JournalTemplateStore TEMPLATE_STORE = JournalTemplateStore.load();
    private static final Map<JournalPagePurpose, Map<JournalPageSlot, DefaultSlotSpec>> DEFAULT_TEMPLATES = createDefaultTemplates();

    private JournalPageStyleSystem() {}

    public static StyledPageBuilder builder(JournalPagePurpose purpose, BookPageSide pageSide) {
        return new StyledPageBuilder(purpose, pageSide, templateFor(purpose, pageSide));
    }

    public static boolean debugTemplateLayoutEnabled() {
        return debugTemplateLayout;
    }

    public static void setDebugTemplateLayoutEnabled(boolean enabled) {
        debugTemplateLayout = enabled;
    }

    public static TemplateSpec currentTemplate(JournalPagePurpose purpose, BookPageSide pageSide) {
        return templateFor(purpose, pageSide);
    }

    public static void updateTemplateRegion(JournalPagePurpose purpose, JournalPageSlot slot, BookPageSide pageSide, SlotRegion region) {
        TEMPLATE_STORE.update(purpose, slot, normalize(pageSide, region));
    }

    public static void saveTemplates() {
        TEMPLATE_STORE.save();
    }

    public static JournalTemplateStore snapshotTemplates() {
        return TEMPLATE_STORE.copy();
    }

    public static void restoreTemplates(JournalTemplateStore snapshot) {
        TEMPLATE_STORE.restoreFrom(snapshot);
    }

    public static int ledgerRowsPerPage(BookPageSide pageSide) {
        TemplateSpec template = templateFor(JournalPagePurpose.LEDGER, pageSide);
        SlotDefinition rows = template.slot(JournalPageSlot.ROWS);
        return Math.max(1, rows.region().height() / Math.max(1, JournalElementFactory.ledgerRowHeight()));
    }

    public static BookTextBlock.Kind kindFor(JournalTextRole role) {
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

    public static String distinctLine(String candidate, String fallback, String disallow) {
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

    public static TemplateSpec templateFor(JournalPagePurpose purpose, BookPageSide pageSide) {
        Map<JournalPageSlot, DefaultSlotSpec> defaults = DEFAULT_TEMPLATES.get(purpose);
        Map<JournalPageSlot, SlotDefinition> slots = new EnumMap<>(JournalPageSlot.class);
        for (Map.Entry<JournalPageSlot, DefaultSlotSpec> entry : defaults.entrySet()) {
            slots.put(
                    entry.getKey(),
                    new SlotDefinition(
                            region(purpose, pageSide, entry.getKey(), entry.getValue().region()),
                            entry.getValue().fit()
                    )
            );
        }
        return new TemplateSpec(purpose, pageSide, slots, requiredSlotsFor(purpose), maxVisibleInteractionsFor(purpose));
    }

    public static final class StyledPageBuilder {
        private final TemplateSpec template;
        private final List<BookPageElement> elements = new ArrayList<>();
        private final List<PlacedBounds> measuredBounds = new ArrayList<>();
        private final Map<JournalPageSlot, Integer> slotUse = new EnumMap<>(JournalPageSlot.class);
        private final List<String> validationErrors = new ArrayList<>();
        private int visibleInteractionCount;

        private StyledPageBuilder(JournalPagePurpose purpose, BookPageSide pageSide, TemplateSpec template) {
            this.template = template;
        }

        public void addTitle(String text) {
            addCenteredText(JournalPageSlot.TITLE, JournalTextRole.TITLE, text);
        }

        public void addFocal(String text) {
            addCenteredText(JournalPageSlot.FOCAL, JournalTextRole.FOCAL, text);
        }

        public void addSubtitle(String text) {
            addCenteredText(JournalPageSlot.SUBTITLE, JournalTextRole.SUBTITLE, text);
        }

        public void addBody(String text) {
            addCenteredBody(JournalPageSlot.BODY, text);
        }

        public void addStats(List<String> rows, int gap) {
            addCenteredStats(JournalPageSlot.STATS, rows, gap);
        }

        public void addRadarChart(
                JournalPageSlot slot,
                java.util.List<Float> values,
                java.util.List<Float> masteryLevelValues,
                java.util.List<Float> nextMasteryRingValues,
                java.util.List<String> labels) {
            SlotDefinition definition = registerSlot(slot);
            SlotRegion region = definition.region();
            BookPageElement.RadarChartElement element = new BookPageElement.RadarChartElement(
                    region.x(), region.y(), region.width(), region.height(),
                    values, masteryLevelValues, nextMasteryRingValues, labels,
                    0x40A08060,
                    0xCC5A3F29,
                    0x705A3F29,
                    0x305A3F29,
                    0x3AA0C4D0,
                    0xD8C4883A
            );
            elements.add(element);
            trackMeasuredBounds(slot, region, JournalTextRole.BODY, false, element, 1.0f);
        }

        public void addFooter(String text) {
            addCenteredText(JournalPageSlot.FOOTER, JournalTextRole.FOOTER, text);
        }

        public void addInteraction(String stableId, String text, Component tooltip, BookRegionAction action) {
            addBottomInteraction(stableId, text, tooltip, action);
        }

        void addCenteredText(JournalPageSlot slot, JournalTextRole role, String text) {
            SlotDefinition definition = registerSlot(slot);
            if (definition.fit().wrapAllowed() && role != JournalTextRole.BODY) {
                addCenteredWrappedText(slot, role, text, definition);
                return;
            }
            FittedLine line = fitSingleLine(text, role, definition);
            BookPageElement.TextElement element = JournalElementFactory.centeredRoleTextElement(role, line.text(), line.x(), definition.region().y(), line.width(), line.height(), line.scale());
            elements.add(element);
            trackMeasuredBounds(slot, definition.region(), role, false, element, line.scale());
        }

        private void addCenteredWrappedText(JournalPageSlot slot, JournalTextRole role, String text, SlotDefinition definition) {
            List<String> lines = fitWrappedStaticText(text, definition, role);
            int cursorY = definition.region().y();
            int lineHeight = JournalLayoutMetrics.lineHeightFor(kindFor(role));
            for (String line : lines) {
                BookPageElement.TextElement element = JournalElementFactory.centeredRoleTextElement(role, line, definition.region().x(), cursorY, definition.region().width());
                elements.add(element);
                trackMeasuredBounds(slot, definition.region(), role, false, element, 1.0f);
                cursorY += lineHeight;
            }
            if (cursorY > definition.region().bottom()) {
                validationErrors.add("Wrapped text overflowed slot " + slot);
            }
        }

        void addCenteredBody(JournalPageSlot slot, String text) {
            SlotDefinition definition = registerSlot(slot);
            List<String> lines = fitWrappedText(text, definition);
            int cursorY = definition.region().y();
            int lineHeight = JournalLayoutMetrics.lineHeightFor(kindFor(JournalTextRole.BODY));
            for (String line : lines) {
                BookPageElement.TextElement element = JournalElementFactory.centeredRoleTextElement(JournalTextRole.BODY, line, definition.region().x(), cursorY, definition.region().width());
                elements.add(element);
                trackMeasuredBounds(slot, definition.region(), JournalTextRole.BODY, false, element, 1.0f);
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
                int drawX = switch (definition.fit().alignment()) {
                    case CENTER -> centeredX(definition.region(), textWidth);
                    case LEFT -> definition.region().x();
                };
                BookPageElement.TextElement element = new BookPageElement.TextElement(
                        kindFor(JournalTextRole.BODY),
                        Component.literal(row),
                        drawX,
                        cursorY,
                        textWidth,
                        lineHeight,
                        1.0f
                );
                elements.add(element);
                trackMeasuredBounds(slot, definition.region(), JournalTextRole.BODY, false, element, 1.0f);
                cursorY += lineHeight + Math.max(0, gap);
            }
            if (!clamped.isEmpty()) {
                cursorY -= Math.max(0, gap);
            }
            if (cursorY > definition.region().bottom()) {
                validationErrors.add("Stats overflowed slot " + slot);
            }
        }

        public void addLedgerRows(JournalPageSlot slot, List<JournalCharacterStat> rows, Map<String, Integer> skillStartPages) {
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
                BookPageElement.InteractiveTextElement label = JournalElementFactory.ledgerLabel(layout);
                BookPageElement.DecorationElement dots = JournalElementFactory.decorationElement(BookTextBlock.Kind.BODY, layout.dotsText(), layout.dotsX(), layout.labelY());
                BookPageElement.DecorationElement value = JournalElementFactory.decorationElement(BookTextBlock.Kind.BODY, layout.valueText(), layout.valueX(), layout.labelY());
                elements.add(label);
                elements.add(dots);
                elements.add(value);
                trackMeasuredBounds(slot, definition.region(), JournalTextRole.INTERACTION, false, label, label.scale());
                trackMeasuredBounds(slot, definition.region(), JournalTextRole.BODY, false, dots, 1.0f);
                trackMeasuredBounds(slot, definition.region(), JournalTextRole.BODY, false, value, 1.0f);
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
            BookPageElement.InteractiveTextElement element = JournalElementFactory.toInteractiveTextElement(
                    JournalElementFactory.centeredRoleInteractiveTextLayout(
                            stableId,
                            JournalTextRole.INTERACTION,
                            line.text(),
                            line.x(),
                            definition.region().y(),
                            line.width(),
                            line.height(),
                            line.scale(),
                            tooltip,
                            action
                    )
            );
            elements.add(element);
            trackMeasuredBounds(JournalPageSlot.INTERACTION, definition.region(), JournalTextRole.INTERACTION, false, element, line.scale());
        }

        public BookPage build() {
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
            if (BookDebugSettings.templateRegionsDebug()) {
                for (Map.Entry<JournalPageSlot, SlotDefinition> entry : template.slots().entrySet()) {
                    SlotRegion region = entry.getValue().region();
                    elements.add(new BookPageElement.BoxElement(region.x(), region.y(), region.width(), region.height(), DEBUG_SLOT_FILL, DEBUG_SLOT_BORDER, BookPageElement.PanelVisualStyle.PANEL));
                }
            }
            if (BookDebugSettings.measuredTextBoundsDebug()) {
                for (PlacedBounds bounds : measuredBounds) {
                    elements.add(new BookPageElement.BoxElement(
                            bounds.bounds().x(),
                            bounds.bounds().y(),
                            bounds.bounds().width(),
                            bounds.bounds().height(),
                            DEBUG_MEASURED_FILL,
                            DEBUG_MEASURED_BORDER,
                            BookPageElement.PanelVisualStyle.EMPHASIS
                    ));
                    if (bounds.scale() < 0.999f && BookDebugSettings.debugLabels()) {
                        elements.add(JournalElementFactory.decorationElement(
                                BookTextBlock.Kind.BODY,
                                String.format("x%.2f", bounds.scale()),
                                bounds.bounds().x(),
                                Math.max(0, bounds.bounds().y() - JournalLayoutMetrics.lineHeightFor(BookTextBlock.Kind.BODY))
                        ));
                    }
                }
            }
            if (!validationErrors.isEmpty()) {
                for (PlacedBounds bounds : measuredBounds) {
                    if (!isInvalid(bounds)) {
                        continue;
                    }
                    elements.add(new BookPageElement.BoxElement(
                            bounds.bounds().x(),
                            bounds.bounds().y(),
                            bounds.bounds().width(),
                            bounds.bounds().height(),
                            DEBUG_INVALID_FILL,
                            DEBUG_INVALID_BORDER,
                            BookPageElement.PanelVisualStyle.EMPHASIS
                    ));
                }
            }
        }

        private boolean isInvalid(PlacedBounds bounds) {
            return bounds.bounds().x() < bounds.region().x()
                    || bounds.bounds().right() > bounds.region().right()
                    || bounds.bounds().y() < bounds.region().y()
                    || bounds.bounds().bottom() > bounds.region().bottom();
        }

        private FittedLine fitSingleLine(String text, JournalTextRole role, SlotDefinition definition) {
            String normalized = singleLine(text);
            int availableWidth = definition.region().width();
            BookTextBlock.Kind kind = kindFor(role);
            float scale = fittedScale(normalized, kind, availableWidth, definition.fit());
            String fitted = applyOverflow(normalized, availableWidth, scale, kind, definition.fit().overflowPolicy());
            int width = measuredWidth(fitted, kind, scale);
            int x = switch (definition.fit().alignment()) {
                case CENTER -> centeredX(definition.region(), width);
                case LEFT -> definition.region().x();
            };
            int height = measuredHeight(kind, scale);
            return new FittedLine(fitted, x, width, height, scale);
        }

        private List<String> fitWrappedText(String text, SlotDefinition definition) {
            List<String> lines = clampBodyLines(
                    text,
                    definition.region().width(),
                    effectiveWrappedLineLimit(definition, kindFor(JournalTextRole.BODY))
            );
            if (lines.isEmpty() && !singleLine(text).isBlank()) {
                validationErrors.add("Body produced no visible lines");
            }
            return lines;
        }

        private int effectiveWrappedLineLimit(SlotDefinition definition, BookTextBlock.Kind kind) {
            int regionCapacity = maxLinesThatFit(definition.region(), kind);
            if (definition.fit().wrapAllowed()) {
                return regionCapacity;
            }
            return Math.min(definition.fit().maxLines(), regionCapacity);
        }

        private List<String> fitWrappedStaticText(String text, SlotDefinition definition, JournalTextRole role) {
            int maxLines = Math.min(definition.fit().maxLines(), maxLinesThatFit(definition.region(), kindFor(role)));
            return clampStaticLines(text, definition.region().width(), maxLines);
        }

        private int maxLinesThatFit(SlotRegion region, BookTextBlock.Kind kind) {
            int lineHeight = JournalLayoutMetrics.lineHeightFor(kind);
            return Math.max(1, region.height() / Math.max(1, lineHeight));
        }

        private float fittedScale(String text, BookTextBlock.Kind kind, int availableWidth, SlotFit fit) {
            float preferredScale = Math.min(1.0f, fit.maxScale());
            if (measuredWidth(text, kind, preferredScale) <= availableWidth) {
                return preferredScale;
            }
            float minScale = Math.min(preferredScale, fit.minScale());
            float idealScale = availableWidth <= 0
                    ? minScale
                    : (float) availableWidth / Math.max(1.0f, baseMeasuredWidth(text, kind));
            return Math.max(minScale, Math.min(preferredScale, idealScale));
        }

        private String applyOverflow(String text, int width, float scale, BookTextBlock.Kind kind, OverflowPolicy policy) {
            if (measuredWidth(text, kind, scale) <= width) {
                return text;
            }
            return switch (policy) {
                case ELLIPSIZE, CLAMP -> withEllipsis(text, unscaledWidthBudget(width, kind, scale));
                case INVALID -> {
                    validationErrors.add("Text overflow for page " + template.purpose());
                    yield text;
                }
            };
        }

        private int measuredWidth(String text, BookTextBlock.Kind kind, float scale) {
            return Math.max(1, Math.round(baseMeasuredWidth(text, kind) * Math.max(0.1f, scale)));
        }

        private int measuredHeight(BookTextBlock.Kind kind, float scale) {
            return Math.max(1, Math.round(JournalLayoutMetrics.lineHeightFor(kind) * Math.max(0.1f, scale)));
        }

        private float baseMeasuredWidth(String text, BookTextBlock.Kind kind) {
            float kindScale = kind == BookTextBlock.Kind.LEVEL ? 2.0f : 1.0f;
            return Math.max(1, Minecraft.getInstance().font.width(text)) * kindScale;
        }

        private int unscaledWidthBudget(int width, BookTextBlock.Kind kind, float scale) {
            float renderScale = (kind == BookTextBlock.Kind.LEVEL ? 2.0f : 1.0f) * Math.max(0.1f, scale);
            return Math.max(1, Math.round(width / renderScale));
        }

        private void trackMeasuredBounds(
                JournalPageSlot slot,
                SlotRegion region,
                JournalTextRole role,
                boolean interactive,
                BookPageElement element,
                float scale
        ) {
            SlotRegion bounds = switch (element) {
                case BookPageElement.TextElement text -> toSlotRegion(PageCanvasRenderer.geometryForTextElement(text));
                case BookPageElement.InteractiveTextElement text -> toSlotRegion(PageCanvasRenderer.inlineInteractionGeometryFor(text));
                case BookPageElement.DecorationElement decoration -> toSlotRegion(PageCanvasRenderer.geometryForDecorationElement(decoration));
                case BookPageElement.ButtonElement button -> new SlotRegion(button.x(), button.y(), button.width(), button.height());
                default -> new SlotRegion(element.x(), element.y(), element.width(), element.height());
            };
            measuredBounds.add(new PlacedBounds(slot, bounds, region, role, interactive, scale));
        }

        private static SlotRegion toSlotRegion(PageCanvasRenderer.RenderedTextGeometry geometry) {
            return new SlotRegion(geometry.drawX(), geometry.drawY(), geometry.width(), geometry.height());
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

    static List<String> clampStaticLines(String text, int width, int maxLines) {
        List<JournalPaginationEngine.MeasuredLine> wrapped = JournalPaginationEngine.wrapStaticText(text, width);
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

    private static Map<JournalPagePurpose, Map<JournalPageSlot, DefaultSlotSpec>> createDefaultTemplates() {
        Map<JournalPagePurpose, Map<JournalPageSlot, DefaultSlotSpec>> templates = new EnumMap<>(JournalPagePurpose.class);
        templates.put(JournalPagePurpose.CHARACTER_IDENTITY, Map.of(
                JournalPageSlot.TITLE, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.0, 10.0 / 145.0, 1.0, 12.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 0.85f, 1.0f)
                ),
                JournalPageSlot.FOCAL, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.0, 42.0 / 145.0, 1.0, 24.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 0.8f, 1.0f)
                ),
                JournalPageSlot.SUBTITLE, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.035, 82.0 / 145.0, 0.93, 12.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 2, true, OverflowPolicy.ELLIPSIZE, 0.9f, 1.0f)
                ),
                JournalPageSlot.BODY, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.105, 96.0 / 145.0, 0.79, 42.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 4, true, OverflowPolicy.CLAMP, 1.0f, 1.0f)
                ),
                JournalPageSlot.FOOTER, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.0, (double) (JournalLayoutMetrics.PAGE_CONTENT_HEIGHT - 6) / 145.0, 1.0, 8.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 0.95f, 1.0f)
                )
        ));
        templates.put(JournalPagePurpose.CHARACTER_STANDING, Map.of(
                JournalPageSlot.TITLE, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.0, 10.0 / 145.0, 1.0, 12.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 0.85f, 1.0f)
                ),
                JournalPageSlot.BODY, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.07, 26.0 / 145.0, 0.86, 22.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 2, true, OverflowPolicy.CLAMP, 1.0f, 1.0f)
                ),
                JournalPageSlot.RADAR, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.03, 60.0 / 145.0, 0.94, 58.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.INVALID, 1.0f, 1.0f)
                ),
                JournalPageSlot.INTERACTION, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.0, (double) (JournalLayoutMetrics.PAGE_CONTENT_HEIGHT - 8) / 145.0, 1.0, 8.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 0.9f, 1.0f)
                )
        ));
        templates.put(JournalPagePurpose.LEDGER, Map.of(
                JournalPageSlot.TITLE, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.0, 12.0 / 145.0, 1.0, 12.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 0.9f, 1.0f)
                ),
                JournalPageSlot.ROWS, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.045, 28.0 / 145.0, 0.91, (double) (JournalLayoutMetrics.PAGE_CONTENT_HEIGHT - 38) / 145.0),
                        new SlotFit(Alignment.LEFT, Integer.MAX_VALUE, false, OverflowPolicy.INVALID, 1.0f, 1.0f)
                ),
                JournalPageSlot.FOOTER, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.0, (double) (JournalLayoutMetrics.PAGE_CONTENT_HEIGHT - 8) / 145.0, 1.0, 8.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 0.95f, 1.0f)
                )
        ));
        templates.put(JournalPagePurpose.SKILL_DETAIL, Map.of(
                JournalPageSlot.TITLE, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.0, 10.0 / 145.0, 1.0, 12.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 0.85f, 1.0f)
                ),
                JournalPageSlot.FOCAL, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.0, 46.0 / 145.0, 1.0, 24.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 0.8f, 1.0f)
                ),
                JournalPageSlot.BODY, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.088, 88.0 / 145.0, 0.824, 28.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 4, true, OverflowPolicy.CLAMP, 1.0f, 1.0f)
                ),
                JournalPageSlot.INTERACTION, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.0, (double) (JournalLayoutMetrics.PAGE_CONTENT_HEIGHT - 10) / 145.0, 1.0, 10.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 0.9f, 1.0f)
                )
        ));
        templates.put(JournalPagePurpose.FRONT_MATTER, Map.of(
                JournalPageSlot.TITLE, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.0, 16.0 / 145.0, 1.0, 14.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 0.85f, 1.0f)
                ),
                JournalPageSlot.BODY, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.07, 52.0 / 145.0, 0.86, 44.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 4, true, OverflowPolicy.CLAMP, 1.0f, 1.0f)
                ),
                JournalPageSlot.FOOTER, new DefaultSlotSpec(
                        new JournalTemplateStore.NormalizedSlotRegion(0.0, (double) (JournalLayoutMetrics.PAGE_CONTENT_HEIGHT - 10) / 145.0, 1.0, 10.0 / 145.0),
                        new SlotFit(Alignment.CENTER, 1, false, OverflowPolicy.ELLIPSIZE, 1.0f, 1.0f)
                )
        ));
        templates.put(JournalPagePurpose.SYNTHETIC_EMPTY, Map.of());
        return templates;
    }

    private static Set<JournalPageSlot> requiredSlotsFor(JournalPagePurpose purpose) {
        return switch (purpose) {
            case CHARACTER_IDENTITY -> EnumSet.of(JournalPageSlot.TITLE, JournalPageSlot.FOCAL, JournalPageSlot.SUBTITLE);
            case CHARACTER_STANDING -> EnumSet.of(JournalPageSlot.TITLE, JournalPageSlot.BODY, JournalPageSlot.RADAR, JournalPageSlot.INTERACTION);
            case LEDGER -> EnumSet.of(JournalPageSlot.ROWS);
            case SKILL_DETAIL -> EnumSet.of(JournalPageSlot.TITLE, JournalPageSlot.FOCAL, JournalPageSlot.BODY, JournalPageSlot.INTERACTION);
            case FRONT_MATTER -> EnumSet.of(JournalPageSlot.TITLE, JournalPageSlot.BODY);
            case SYNTHETIC_EMPTY -> EnumSet.noneOf(JournalPageSlot.class);
        };
    }

    private static int maxVisibleInteractionsFor(JournalPagePurpose purpose) {
        return switch (purpose) {
            case CHARACTER_STANDING, SKILL_DETAIL -> 1;
            default -> 0;
        };
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

    public record TemplateSpec(
            JournalPagePurpose purpose,
            BookPageSide pageSide,
            Map<JournalPageSlot, SlotDefinition> slots,
            Set<JournalPageSlot> requiredSlots,
            int maxVisibleInteractions
    ) {
        public SlotDefinition slot(JournalPageSlot slot) {
            return slots.get(slot);
        }
    }

    public record SlotDefinition(SlotRegion region, SlotFit fit) {}

    public record SlotRegion(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }
    }

    public record SlotFit(
            Alignment alignment,
            int maxLines,
            boolean wrapAllowed,
            OverflowPolicy overflowPolicy,
            float minScale,
            float maxScale
    ) {}

    private record DefaultSlotSpec(
            JournalTemplateStore.NormalizedSlotRegion region,
            SlotFit fit
    ) {}

    private record FittedLine(String text, int x, int width, int height, float scale) {}

    private record PlacedBounds(
            JournalPageSlot slot,
            SlotRegion bounds,
            SlotRegion region,
            JournalTextRole role,
            boolean interactive,
            float scale
    ) {}

    public enum Alignment {
        LEFT,
        CENTER
    }

    public enum OverflowPolicy {
        CLAMP,
        ELLIPSIZE,
        INVALID
    }
}
