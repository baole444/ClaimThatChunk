package Sky.Cat.CTC.networking.payload;

import Sky.Cat.CTC.Main;
import Sky.Cat.CTC.chunk.ChunkData;
import Sky.Cat.CTC.chunk.ClaimedChunk;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A record of a list for chunk data packet payload for synchronization.
 * @param chunks a list of chunks.
 */
public record ChunksDataPayload(List<ChunkData> chunks) implements CustomPayload {
    private static final Identifier CHUNKS_DATA_PAYLOAD_ID = Identifier.of(Main.MOD_ID, "chunks_data");

    public static final CustomPayload.Id<ChunksDataPayload> ID = new CustomPayload.Id<>(CHUNKS_DATA_PAYLOAD_ID);

    public static final PacketCodec<RegistryByteBuf, ChunksDataPayload> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.collection(ArrayList::new, ChunkData.PACKET_CODEC),
            ChunksDataPayload::chunks,

            ChunksDataPayload::new
    );

    /**
     * Constructor for this packet payload.
     */
    public ChunksDataPayload(Collection<ClaimedChunk> claimedChunks) {
        this(claimedChunks.stream().
                map(chunk -> new ChunkData(
                        chunk.getPosition(),
                        chunk.getOwnerTeamId(),
                        chunk.getClaimedTime()
                        )
                ).toList()
        );
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
