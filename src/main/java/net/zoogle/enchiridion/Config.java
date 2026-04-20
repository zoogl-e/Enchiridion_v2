package net.zoogle.enchiridion;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Minimal configuration holder for Enchiridion.
 * Add real config entries to the builder as needed.
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
