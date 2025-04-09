package Sky.Cat.CTC.networking.payload;

import Sky.Cat.CTC.Main;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * A record of team data request packet payload for synchronization.
 */
public record RequestTeamDataPayload() implements CustomPayload {
    private static final Identifier REQUEST_TEAM_DATA_PAYLOAD_ID = Identifier.of(Main.MOD_ID, "request_team_data");

    public static final CustomPayload.Id<RequestTeamDataPayload> ID = new CustomPayload.Id<>(REQUEST_TEAM_DATA_PAYLOAD_ID);

    public static final PacketCodec<RegistryByteBuf, RequestTeamDataPayload> PACKET_CODEC = PacketCodec.unit(new RequestTeamDataPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
