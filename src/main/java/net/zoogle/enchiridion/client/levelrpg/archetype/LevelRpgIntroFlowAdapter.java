package net.zoogle.enchiridion.client.levelrpg.archetype;

import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.client.ui.IntroFlowPort;

public final class LevelRpgIntroFlowAdapter implements IntroFlowPort {
    @Override
    public boolean tick(BookContext context) {
        return LevelRpgJournalIntroFlowState.get().tick(context);
    }

    @Override
    public void beginBinding(BookContext context, String focusId) {
        LevelRpgJournalIntroFlowState.get().beginBinding(context, focusId);
    }

    @Override
    public boolean shouldResetOnClose(BookContext context) {
        return LevelRpgJournalIntroFlowState.get().shouldResetOnClose(context);
    }

    @Override
    public void reset() {
        LevelRpgJournalIntroFlowState.get().reset();
    }
}
