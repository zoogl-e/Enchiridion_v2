package net.zoogle.enchiridion.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public sealed interface BookPageElement permits
        BookPageElement.TextElement,
        BookPageElement.InteractiveTextElement,
        BookPageElement.DecorationElement,
        BookPageElement.BoxElement,
        BookPageElement.ImageElement,
        BookPageElement.WidgetElement {

    int x();

    int y();

    int width();

    int height();

    record TextElement(
            BookTextBlock.Kind kind,
            Component text,
            int x,
            int y,
            int width,
            int height
    ) implements BookPageElement {
        public TextElement {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(text, "text");
            width = Math.max(1, width);
            height = Math.max(1, height);
        }
    }

    record InteractiveTextElement(
            BookTextBlock.Kind kind,
            Component text,
            int x,
            int y,
            int width,
            int height,
            Component tooltip,
            BookRegionAction action
    ) implements BookPageElement {
        public InteractiveTextElement {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(action, "action");
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
            int borderColor
    ) implements BookPageElement {
        public BoxElement {
            width = Math.max(1, width);
            height = Math.max(1, height);
        }
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
}
