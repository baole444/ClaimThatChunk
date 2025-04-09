package Sky.Cat.CTC.networking.payload;

import Sky.Cat.CTC.Main;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * A record of chunk data request packet payload for synchronization.
 */
public record RequestChunkDataPayload() implements CustomPayload {
    private static final Identifier REQUEST_CHUNK_DATA_PAYLOAD_ID = Identifier.of(Main.MOD_ID, "request_chunk_data");

    public static final CustomPayload.Id<RequestChunkDataPayload> ID = new CustomPayload.Id<>(REQUEST_CHUNK_DATA_PAYLOAD_ID);

    public static final PacketCodec<RegistryByteBuf, RequestChunkDataPayload> PACKET_CODEC = PacketCodec.unit(new RequestChunkDataPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
