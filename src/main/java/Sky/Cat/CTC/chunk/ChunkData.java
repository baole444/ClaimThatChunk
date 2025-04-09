package Sky.Cat.CTC.chunk;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.UUID;

/**
 * A record of ClaimedChunk built for Minecraft's packet.
 * @param position position of the chunk.
 * @param teamId uuid of the team that own the chunk.
 * @param claimedTime timestamp when the chunk was claimed.
 */
public record ChunkData(ChunkPosition position, UUID teamId, long claimedTime) {
    public static final PacketCodec<RegistryByteBuf, ChunkData> PACKET_CODEC = PacketCodec.tuple(
            ChunkPosition.PACKET_CODEC,
            ChunkData::position,

            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString),
            ChunkData::teamId,

            PacketCodecs.LONG,
            ChunkData::claimedTime,

            ChunkData::new
    );
}
