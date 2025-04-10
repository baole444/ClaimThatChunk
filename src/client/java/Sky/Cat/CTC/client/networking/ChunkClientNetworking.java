package Sky.Cat.CTC.client.networking;

import Sky.Cat.CTC.chunk.ChunkData;
import Sky.Cat.CTC.client.chunk.ChunkClientData;
import Sky.Cat.CTC.networking.payload.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import static Sky.Cat.CTC.Utilities.intToPermission;

public class ChunkClientNetworking {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ChunkClaimPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ChunkClientData.getInstance().addOrUpdateChunk(
                        payload.position(),
                        payload.teamId(),
                        payload.claimedTime()
                );
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ChunkUnClaimPayload.ID, (payload, context) -> {
           context.client().execute(() -> {
               ChunkClientData.getInstance().removeChunk(payload.position());
           });
        });

        ClientPlayNetworking.registerGlobalReceiver(ChunkPermissionUpdatePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ChunkClientData.getInstance().updatePermissionOverride(
                        payload.position(),
                        payload.playerUUID(),
                        intToPermission(payload.permissionFlags())
                );
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ChunksDataPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ChunkClientData.getInstance().clearChunkData();

                for (ChunkData data : payload.chunks()) {
                    ChunkClientData.getInstance().addOrUpdateChunk(
                            data.position(),
                            data.teamId(),
                            data.claimedTime()
                    );
                }
            });
        });
    }

    public static void requestChunkData() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.getNetworkHandler() != null) {
            ClientPlayNetworking.send(new RequestChunkDataPayload());
        }
    }
}
