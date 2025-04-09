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
 * A record of claim chunk packet payload for synchronization.
 * @param position position of the chunk.
 * @param teamId uuid of the team that claimed the chunk.
 * @param claimedTime the time when the chunk was claimed.
 */
public record ChunkClaimPayload(ChunkPosition position, UUID teamId, long claimedTime) implements CustomPayload {
    private static final Identifier CHUNK_CLAIM_PAYLOAD_ID = Identifier.of(Main.MOD_ID, "chunk_claim");

    public static final CustomPayload.Id<ChunkClaimPayload> ID = new CustomPayload.Id<>(CHUNK_CLAIM_PAYLOAD_ID);

    public static final PacketCodec<RegistryByteBuf, ChunkClaimPayload> PACKET_CODEC = PacketCodec.tuple(
            ChunkPosition.PACKET_CODEC,
            ChunkClaimPayload::position,

            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString),
            ChunkClaimPayload::teamId,

            PacketCodecs.LONG,
            ChunkClaimPayload::claimedTime,

            ChunkClaimPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
