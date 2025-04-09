package Sky.Cat.CTC.networking;

import Sky.Cat.CTC.Main;
import Sky.Cat.CTC.chunk.ChunkManager;
import Sky.Cat.CTC.chunk.ChunkPosition;
import Sky.Cat.CTC.chunk.ClaimedChunk;
import Sky.Cat.CTC.networking.payload.*;
import Sky.Cat.CTC.permission.Permission;
import Sky.Cat.CTC.team.Team;
import Sky.Cat.CTC.team.TeamManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import static Sky.Cat.CTC.Utilities.permissionToInt;

public class ChunkNetworking {
    // Register handler methods.
    public static void register() {
        // Server -> Client
        PayloadTypeRegistry.playS2C().register(ChunkClaimPayload.ID, ChunkClaimPayload.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(ChunkUnClaimPayload.ID, ChunkUnClaimPayload.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(ChunksDataPayload.ID, ChunksDataPayload.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(ChunkPermissionUpdatePayload.ID, ChunkPermissionUpdatePayload.PACKET_CODEC);

        // Client -> Server
        PayloadTypeRegistry.playC2S().register(RequestChunkDataPayload.ID, RequestChunkDataPayload.PACKET_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RequestChunkDataPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();

                ChunkManager.getInstance().sendChunkDataToPlayer(player);
            });
        });
    }

    /**
     * Inform all clients on the chunk and clients in the team about the newly claimed chunk.
     * @param chunk the chunk that the team just claimed.
     */
    public static void broadcastChunkClaimed(ClaimedChunk chunk) {
        ChunkClaimPayload payload = new ChunkClaimPayload(
                chunk.getPosition(),
                chunk.getOwnerTeamId(),
                chunk.getClaimedTime()
        );

        Team team = TeamManager.getInstance().getTeamById(chunk.getOwnerTeamId());

        if (team != null) {
            for (UUID memberId : team.getTeamMember().keySet()) {
                ServerPlayerEntity player = Main.getServer().getPlayerManager().getPlayer(memberId);

                if (player != null) {
                    ServerPlayNetworking.send(player,payload);
                }
            }
        }

        sendToPlayersInChunk(payload, chunk.getPosition());
    }

    /**
     * Inform all clients about a team unclaimed a chunk.
     * @param position position of the unclaimed chunk.
     */
    public static void broadcastChunkUnclaimed(ChunkPosition position) {
        ChunkUnClaimPayload payload = new ChunkUnClaimPayload(position);

        for (ServerPlayerEntity player : Main.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void broadcastChunkPermissionUpdate(ClaimedChunk chunk, UUID playerUUID, Permission permission) {
        ChunkPermissionUpdatePayload payload = new ChunkPermissionUpdatePayload(
                chunk.getPosition(),
                playerUUID,
                permissionToInt(permission)
        );

        //  Inform the targeted player.
        ServerPlayerEntity player = Main.getServer().getPlayerManager().getPlayer(playerUUID);
        if (player != null) {
            ServerPlayNetworking.send(player, payload);
        }

        // Inform other team's members.
        Team team = TeamManager.getInstance().getTeamById(chunk.getOwnerTeamId());
        if (team != null) {
            for (UUID memberId : team.getTeamMember().keySet()) {
                ServerPlayerEntity member = Main.getServer().getPlayerManager().getPlayer(memberId);

                if (member != null && !member.getUuid().equals(playerUUID)) {
                    ServerPlayNetworking.send(member, payload);
                }
            }
        }
    }

    public static void sendAllClaimedChunksToPlayer(ServerPlayerEntity player, Collection<ClaimedChunk> chunks) {
        ChunksDataPayload payload = new ChunksDataPayload(chunks);

        ServerPlayNetworking.send(player, payload);
    }

    private static void sendToPlayersInChunk(CustomPayload payload, ChunkPosition position) {
        for (ServerPlayerEntity player : Main.getServer().getPlayerManager().getPlayerList()) {
            ChunkPosition playerPosition = new ChunkPosition(player.getChunkPos(), player.getWorld().getRegistryKey());

            if (playerPosition.equals(position)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
}
