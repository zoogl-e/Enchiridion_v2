package net.zoogle.enchiridion.client.levelrpg.bounty;

public record JournalBountyOfferSnapshot(
        JournalBountyOffer leftOffer,
        JournalBountyOffer rightOffer,
        int spreadIndex,
        int totalSpreads
) {}

