package net.zoogle.enchiridion.client.demo;

import net.minecraft.network.chat.Component;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.api.BookPage;
import net.zoogle.enchiridion.api.BookPageProvider;
import net.zoogle.enchiridion.api.BookSpread;

public final class DemoBookProvider implements BookPageProvider {
    @Override
    public int spreadCount(BookContext context) {
        return 4;
    }

    @Override
    public BookSpread getSpread(BookContext context, int spreadIndex) {
        return switch (spreadIndex) {
            case 0 -> BookSpread.of(
                    BookPage.of(
                            net.zoogle.enchiridion.api.BookTextBlock.title(Component.literal("Enchiridion")),
                            net.zoogle.enchiridion.api.BookTextBlock.body(Component.literal("This is the new v2 foundation. Rendering is owned by Enchiridion itself. Other mods should only provide page content."))
                    ),
                    BookPage.of(
                            net.zoogle.enchiridion.api.BookTextBlock.title(Component.literal("What changed")),
                            net.zoogle.enchiridion.api.BookTextBlock.body(Component.literal("The old Gecko-heavy Displayable API is no longer the center of the mod. Books are now registered with simple content providers."))
                    )
            );
            case 1 -> BookSpread.of(
                    BookPage.of(
                            net.zoogle.enchiridion.api.BookTextBlock.title(Component.literal("Milestone 1")),
                            net.zoogle.enchiridion.api.BookTextBlock.body(Component.literal("One book. One screen. One controller. One renderer path. No carousel, focus mode, or texture-slot gymnastics yet."))
                    ),
                    BookPage.of(
                            net.zoogle.enchiridion.api.BookTextBlock.title(Component.literal("Milestone 2")),
                            net.zoogle.enchiridion.api.BookTextBlock.body(Component.literal("Swap the placeholder scene renderer for a real Gecko-backed 3D book. Keep the same screen, registry, and provider flow intact."))
                    )
            );
            case 2 -> BookSpread.of(
                    BookPage.of(
                            net.zoogle.enchiridion.api.BookTextBlock.title(Component.literal("Milestone 3")),
                            net.zoogle.enchiridion.api.BookTextBlock.body(Component.literal("Render proper page textures instead of direct GUI text. That is where text wrapping and page atlas work should live."))
                    ),
                    BookPage.of(
                            net.zoogle.enchiridion.api.BookTextBlock.title(Component.literal("LevelRPG")),
                            net.zoogle.enchiridion.api.BookTextBlock.body(Component.literal("LevelRPG now registers its own journal book and can return spreads built from player skill data or mock fallback data. No Gecko knowledge required."))
                    )
            );
            case 3 -> BookSpread.of(
                    BookPage.of(
                            net.zoogle.enchiridion.api.BookTextBlock.title(Component.literal("End of demo")),
                            net.zoogle.enchiridion.api.BookTextBlock.body(Component.literal("You can now start replacing the placeholder renderer with the real animated book while keeping the rest of the architecture stable."))
                    ),
                    BookPage.empty()
            );
            default -> BookSpread.of(BookPage.empty(), BookPage.empty());
        };
    }
}
