package net.zoogle.enchiridion.client.levelrpg.bridge;

import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.client.levelrpg.archetype.ArchetypeBindFeedback;
import net.zoogle.enchiridion.client.levelrpg.archetype.ArchetypeBindGateway;

public final class LevelRpgArchetypeBindGateway implements ArchetypeBindGateway {
    @Override
    public boolean requestBind(BookContext context, String focusId) {
        return LevelRpgArchetypeBindingBridge.requestBindArchetype(context, focusId);
    }

    @Override
    public ArchetypeBindFeedback latestFeedback() {
        return LevelRpgArchetypeBindingBridge.latestBindFeedback();
    }
}
