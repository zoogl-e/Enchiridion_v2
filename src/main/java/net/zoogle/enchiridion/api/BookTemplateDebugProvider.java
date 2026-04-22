package net.zoogle.enchiridion.api;

import org.jetbrains.annotations.Nullable;

public interface BookTemplateDebugProvider {
    @Nullable String templatePurposeForPageIndex(BookContext context, int pageIndex);
}
