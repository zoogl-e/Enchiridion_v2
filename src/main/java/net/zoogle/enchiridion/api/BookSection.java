package net.zoogle.enchiridion.api;

/**
 * Physical location section of the book.
 *
 * <p>This is intentionally separate from {@link BookContentMode}, which selects interior document content
 * (e.g. READING vs BOUNTY). During migration, front/back sections may still be represented through spread indices.
 */
public enum BookSection {
    /**
     * Front cover/front binding/archetype section. Long-term this is not intended to be modeled as a normal interior spread.
     */
    FRONT_SPECIAL,
    /**
     * Normal spread-indexed interior content. {@link BookContentMode} variants live here.
     */
    INTERIOR,
    /**
     * Back/final-page section (including missing/ripped page presentation in future phases).
     * Long-term this is not intended to be modeled as a normal interior spread.
     */
    BACK_SPECIAL
}
