package net.zoogle.enchiridion.client.levelrpg.bridge;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Asks the LevelRPG server for a full profile snapshot before the journal UI builds.
 * Implemented with reflection so Enchiridion does not require a compile dependency on LevelRPG.
 */
public final class LevelRpgProfileSyncHook {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String REQUEST_PAYLOAD_CLASS = "net.zoogle.levelrpg.net.payload.RequestProfileSyncPayload";
    private static final String PACKET_DISTRIBUTOR_CLASS = "net.neoforged.neoforge.network.PacketDistributor";

    private LevelRpgProfileSyncHook() {}

    public static void requestServerSnapshotIfAvailable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getConnection() == null) {
            return;
        }
        try {
            Class<?> payloadClass = Class.forName(REQUEST_PAYLOAD_CLASS);
            Field instanceField = payloadClass.getField("INSTANCE");
            Object payload = instanceField.get(null);
            Class<?> distributorClass = Class.forName(PACKET_DISTRIBUTOR_CLASS);
            // NeoForge: sendToServer(CustomPacketPayload, CustomPacketPayload... varargs)
            Method sendToServer = distributorClass.getMethod(
                    "sendToServer",
                    CustomPacketPayload.class,
                    CustomPacketPayload[].class
            );
            sendToServer.invoke(null, payload, new CustomPacketPayload[0]);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            LOGGER.debug("Could not request LevelRPG profile sync (LevelRPG missing or NeoForge API mismatch)", exception);
        }
    }
}
