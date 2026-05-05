package net.zoogle.enchiridion.client.levelrpg.archetype;

import net.zoogle.enchiridion.api.BookContext;

public interface ArchetypeBindGateway {
    boolean requestBind(BookContext context, String focusId);

    default ArchetypeBindFeedback latestFeedback() {
        return null;
    }
}
