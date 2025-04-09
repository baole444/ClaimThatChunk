package Sky.Cat.CTC.networking.payload;

import Sky.Cat.CTC.Main;
import Sky.Cat.CTC.chunk.ChunkPosition;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * A record of chunk permission override update packet payload for synchronization.
 * @param position position of the chunk.
 * @param playerUUID uuid of the player to have their permission override.
 * @param permissionFlags permission flag number for permission override.
 */
public record ChunkPermissionUpdatePayload(ChunkPosition position, UUID playerUUID, int permissionFlags) implements CustomPayload {
    private static final Identifier CHUNK_PERMISSION_UPDATE_PAYLOAD_ID = Identifier.of(Main.MOD_ID, "chunk_permission_update");

    public static final CustomPayload.Id<ChunkPermissionUpdatePayload> ID = new CustomPayload.Id<>(CHUNK_PERMISSION_UPDATE_PAYLOAD_ID);

    public static final PacketCodec<RegistryByteBuf, ChunkPermissionUpdatePayload> PACKET_CODEC = PacketCodec.tuple(
            ChunkPosition.PACKET_CODEC,
            ChunkPermissionUpdatePayload::position,

            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString),
            ChunkPermissionUpdatePayload::playerUUID,

            PacketCodecs.INTEGER,
            ChunkPermissionUpdatePayload::permissionFlags,

            ChunkPermissionUpdatePayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
