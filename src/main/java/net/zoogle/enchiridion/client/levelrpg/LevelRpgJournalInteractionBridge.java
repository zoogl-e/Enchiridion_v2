package net.zoogle.enchiridion.client.levelrpg;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.api.BookContext;
import net.zoogle.enchiridion.client.ui.BookScreen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

public final class LevelRpgJournalInteractionBridge {
    private static final String LEVEL_RPG_SKILL_TREE_SCREEN_CLASS = "net.zoogle.levelrpg.client.ui.SkillTreeScreen";
    private static final String SPEND_SKILL_POINT_PAYLOAD_CLASS = "net.zoogle.levelrpg.net.payload.SpendSkillPointRequestPayload";
    private static final String PACKET_DISTRIBUTOR_CLASS = "net.neoforged.neoforge.network.PacketDistributor";

    private LevelRpgJournalInteractionBridge() {}

    public static List<JournalArchetypeChoice> availableArchetypes() {
        return LevelRpgArchetypeBindingBridge.availableArchetypes();
    }

    static boolean openSkillScreen(BookContext context, String journalSkillName) {
        if (context == null || context.minecraft() == null || context.player() == null || journalSkillName == null) {
            return false;
        }

        String path = LevelRpgJournalSnapshotFactory.CANONICAL_SKILL_PATHS.get(journalSkillName);
        if (path == null || path.isBlank()) {
            context.player().displayClientMessage(Component.literal("No linked LevelRPG screen is recorded for " + journalSkillName + "."), true);
            return false;
        }

        Minecraft minecraft = context.minecraft();
        ResourceLocation skillId = ResourceLocation.fromNamespaceAndPath("levelrpg", path);
        try {
            Class<?> screenClass = Class.forName(LEVEL_RPG_SKILL_TREE_SCREEN_CLASS);
            Constructor<?> constructor = screenClass.getConstructor(ResourceLocation.class);
            Object screen = constructor.newInstance(skillId);
            minecraft.setScreen((net.minecraft.client.gui.screens.Screen) screen);
            return true;
        } catch (ReflectiveOperationException | ClassCastException exception) {
            context.player().displayClientMessage(Component.literal("LevelRPG skill details are unavailable right now."), true);
            return false;
        }
    }

    static boolean requestSpendSkillPoint(BookContext context, String journalSkillName) {
        if (context == null || context.minecraft() == null || context.player() == null || journalSkillName == null) {
            return false;
        }

        String path = LevelRpgJournalSnapshotFactory.CANONICAL_SKILL_PATHS.get(journalSkillName);
        if (path == null || path.isBlank()) {
            context.player().displayClientMessage(Component.literal("No spend target is recorded for " + journalSkillName + "."), true);
            return false;
        }

        ResourceLocation skillId = ResourceLocation.fromNamespaceAndPath("levelrpg", path);
        try {
            Class<?> payloadClass = Class.forName(SPEND_SKILL_POINT_PAYLOAD_CLASS);
            Constructor<?> constructor = payloadClass.getConstructor(ResourceLocation.class);
            Object payload = constructor.newInstance(skillId);

            Class<?> packetDistributor = Class.forName(PACKET_DISTRIBUTOR_CLASS);
            Method sendToServer = packetDistributor.getMethod(
                    "sendToServer",
                    net.minecraft.network.protocol.common.custom.CustomPacketPayload.class,
                    net.minecraft.network.protocol.common.custom.CustomPacketPayload[].class
            );
            sendToServer.invoke(null, payload, (Object) new net.minecraft.network.protocol.common.custom.CustomPacketPayload[0]);
            return true;
        } catch (ReflectiveOperationException exception) {
            context.player().displayClientMessage(Component.literal("Skill investment is unavailable right now."), true);
            return false;
        }
    }

    static boolean jumpToJournalPage(BookContext context, int pageIndex) {
        if (context == null || context.minecraft() == null || pageIndex < 0) {
            return false;
        }
        if (!(context.minecraft().screen instanceof BookScreen bookScreen)) {
            return false;
        }
        return bookScreen.jumpToSpread(pageIndex / 2);
    }

    static boolean beginArchetypeSelection(BookContext context) {
        if (context == null || context.minecraft() == null) {
            return false;
        }
        if (!(context.minecraft().screen instanceof BookScreen bookScreen)) {
            return false;
        }
        LevelRpgJournalIntroFlowState.get().beginSelection(context);
        return bookScreen.openArchetypeReel();
    }

    static boolean beginArchetypeBinding(BookContext context, String focusId) {
        if (context == null || context.minecraft() == null || focusId == null || focusId.isBlank()) {
            return false;
        }
        if (!(context.minecraft().screen instanceof BookScreen bookScreen)) {
            return false;
        }
        return bookScreen.beginArchetypeBinding(focusId);
    }

    static boolean openArchetypeProjection(BookContext context, String focusId) {
        if (context == null || context.minecraft() == null || focusId == null || focusId.isBlank()) {
            return false;
        }
        if (!(context.minecraft().screen instanceof BookScreen bookScreen)) {
            return false;
        }
        LevelRpgJournalIntroFlowState.get().setSelectedFocus(focusId);
        return bookScreen.openArchetypeReel();
    }

}
