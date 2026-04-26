package net.zoogle.enchiridion.client.levelrpg;

import net.zoogle.enchiridion.api.BookContext;

public final class LevelRpgArchetypeBindGateway implements ArchetypeBindGateway {
    @Override
    public boolean requestBind(BookContext context, String focusId) {
        return LevelRpgArchetypeBindingBridge.requestBindArchetype(context, focusId);
    }
}
