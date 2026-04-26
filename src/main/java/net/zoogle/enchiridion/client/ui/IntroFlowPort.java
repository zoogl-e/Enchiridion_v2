package net.zoogle.enchiridion.client.ui;

import net.zoogle.enchiridion.api.BookContext;

public interface IntroFlowPort {
    boolean tick(BookContext context);

    void beginBinding(BookContext context, String focusId);

    boolean shouldResetOnClose(BookContext context);

    void reset();
}
