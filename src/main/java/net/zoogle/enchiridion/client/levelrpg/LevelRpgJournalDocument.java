package net.zoogle.enchiridion.client.levelrpg;

import net.zoogle.enchiridion.api.BookInteractiveRegion;
import net.zoogle.enchiridion.api.BookSpread;

import java.util.List;
import java.util.Map;

record LevelRpgJournalDocument(
        List<BookSpread> spreads,
        Map<Integer, List<BookInteractiveRegion>> interactiveRegionsBySpread,
        Map<Integer, String> pagePurposeByPage,
        Map<Integer, String> pageIdByPage,
        List<String> projectionFocusOrder,
        Map<String, Integer> projectionSpreadByFocus,
        Map<String, Integer> projectionPageByFocus
) {}
