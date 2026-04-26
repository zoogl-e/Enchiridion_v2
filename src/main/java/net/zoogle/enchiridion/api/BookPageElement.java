package net.zoogle.enchiridion.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

public sealed interface BookPageElement permits
        BookPageElement.TextElement,
        BookPageElement.InteractiveElement,
        BookPageElement.DecorationElement,
        BookPageElement.BoxElement,
        BookPageElement.ProgressBarElement,
        BookPageElement.ImageElement,
        BookPageElement.WidgetElement,
        BookPageElement.RadarChartElement {

    int x();

    int y();

    int width();

    int height();

    sealed interface InteractiveElement extends BookPageElement permits
            BookPageElement.InteractiveTextElement,
            BookPageElement.ButtonElement {

        String stableId();

        Component tooltip();

        BookRegionAction action();

        InteractiveVisualStyle visualStyle();

        boolean enabled();
    }

    enum InteractiveVisualStyle {
        MANUSCRIPT_LINK,
        BUTTON
    }

    record TextElement(
            BookTextBlock.Kind kind,
            Component text,
            int x,
            int y,
            int width,
            int height,
            float scale
    ) implements BookPageElement {
        public TextElement {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(text, "text");
            width = Math.max(1, width);
            height = Math.max(1, height);
            scale = Math.max(0.1f, scale);
        }
    }

    record InteractiveTextElement(
            String stableId,
            BookTextBlock.Kind kind,
            Component text,
            int x,
            int y,
            int width,
            int height,
            float scale,
            Component tooltip,
            BookRegionAction action,
            InteractiveVisualStyle visualStyle,
            boolean enabled
    ) implements InteractiveElement {
        public InteractiveTextElement {
            Objects.requireNonNull(stableId, "stableId");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(visualStyle, "visualStyle");
            width = Math.max(1, width);
            height = Math.max(1, height);
            scale = Math.max(0.1f, scale);
        }
    }

    record ButtonElement(
            String stableId,
            Component label,
            int x,
            int y,
            int width,
            int height,
            Component tooltip,
            BookRegionAction action,
            InteractiveVisualStyle visualStyle,
            boolean enabled
    ) implements InteractiveElement {
        public ButtonElement {
            Objects.requireNonNull(stableId, "stableId");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(visualStyle, "visualStyle");
            width = Math.max(1, width);
            height = Math.max(1, height);
        }
    }

    record DecorationElement(
            BookTextBlock.Kind kind,
            Component text,
            int x,
            int y,
            int width,
            int height
    ) implements BookPageElement {
        public DecorationElement {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(text, "text");
            width = Math.max(1, width);
            height = Math.max(1, height);
        }
    }

    record BoxElement(
            int x,
            int y,
            int width,
            int height,
            int fillColor,
            int borderColor,
            PanelVisualStyle visualStyle
    ) implements BookPageElement {
        public BoxElement {
            width = Math.max(1, width);
            height = Math.max(1, height);
            Objects.requireNonNull(visualStyle, "visualStyle");
        }
    }

    record ProgressBarElement(
            int x,
            int y,
            int width,
            int height,
            float progress,
            int fillColor,
            int trackColor,
            int borderColor
    ) implements BookPageElement {
        public ProgressBarElement {
            width = Math.max(1, width);
            height = Math.max(1, height);
            progress = Math.clamp(progress, 0.0f, 1.0f);
        }
    }

    enum PanelVisualStyle {
        PANEL,
        EMPHASIS
    }

    record ImageElement(
            int x,
            int y,
            int width,
            int height,
            ResourceLocation image
    ) implements BookPageElement {
        public ImageElement {
            Objects.requireNonNull(image, "image");
            width = Math.max(1, width);
            height = Math.max(1, height);
        }
    }

    record WidgetElement(
            String widgetType,
            Component label,
            int x,
            int y,
            int width,
            int height
    ) implements BookPageElement {
        public WidgetElement {
            Objects.requireNonNull(widgetType, "widgetType");
            Objects.requireNonNull(label, "label");
            width = Math.max(1, width);
            height = Math.max(1, height);
        }
    }

    record RadarChartElement(
            int x,
            int y,
            int width,
            int height,
            List<Float> values,
            List<Float> masteryLevelValues,
            List<Float> nextMasteryRingValues,
            List<String> labels,
            int fillColor,
            int strokeColor,
            int axisColor,
            int gridColor,
            /** Stroke color for the current mastery level ring (outline only). */
            int masteryFillColor,
            /** Stroke color for the (mastery + 1) soft-cap ring (outline only). */
            int nextMasteryRingColor
    ) implements BookPageElement {
        public RadarChartElement {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(masteryLevelValues, "masteryLevelValues");
            Objects.requireNonNull(nextMasteryRingValues, "nextMasteryRingValues");
            Objects.requireNonNull(labels, "labels");
            values = List.copyOf(values);
            masteryLevelValues = List.copyOf(masteryLevelValues);
            nextMasteryRingValues = List.copyOf(nextMasteryRingValues);
            labels = List.copyOf(labels);
            width = Math.max(1, width);
            height = Math.max(1, height);
        }
    }
}
