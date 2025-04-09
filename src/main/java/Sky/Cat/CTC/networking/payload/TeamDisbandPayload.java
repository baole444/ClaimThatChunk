package Sky.Cat.CTC.networking.payload;

import Sky.Cat.CTC.Main;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * A record of team disband packet payload for synchronization.
 * @param teamId uuid of the team to be disbanded.
 */
public record TeamDisbandPayload(UUID teamId) implements CustomPayload {
    private static final Identifier TEAM_DISBAND_PAYLOAD_ID = Identifier.of(Main.MOD_ID, "team_disband");

    public static final CustomPayload.Id<TeamDisbandPayload> ID = new CustomPayload.Id<>(TEAM_DISBAND_PAYLOAD_ID);

    public static final PacketCodec<RegistryByteBuf, TeamDisbandPayload> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString),
            TeamDisbandPayload::teamId,

            TeamDisbandPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
