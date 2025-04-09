package Sky.Cat.CTC.networking.payload;

import Sky.Cat.CTC.Main;
import Sky.Cat.CTC.chunk.ChunkPosition;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ChunkUnClaimPayload(ChunkPosition position) implements CustomPayload {
    private static final Identifier CHUNK_UNCLAIM_PAYLOAD_ID = Identifier.of(Main.MOD_ID, "chunk_unclaim");

    public static final CustomPayload.Id<ChunkUnClaimPayload> ID = new CustomPayload.Id<>(CHUNK_UNCLAIM_PAYLOAD_ID);

    public static final PacketCodec<RegistryByteBuf, ChunkUnClaimPayload> PACKET_CODEC = PacketCodec.tuple(
            ChunkPosition.PACKET_CODEC,
            ChunkUnClaimPayload::position,

            ChunkUnClaimPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
