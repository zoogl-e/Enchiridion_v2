package net.zoogle.enchiridion.client.levelrpg;

import net.zoogle.enchiridion.api.BookContext;

public interface ArchetypeBindGateway {
    boolean requestBind(BookContext context, String focusId);
}
