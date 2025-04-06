package Sky.Cat.CTC.networking.payload;

import Sky.Cat.CTC.Main;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A record of Team packet payload for server to client synchronization.
 * @param teamId uuid of the team.
 * @param teamName name of the team.
 * @param leaderId uuid of the team's leader.
 * @param leaderName name of the team's leader.
 * @param members hash map of the team's members.
 */
public record TeamDataPayload(UUID teamId, String teamName, UUID leaderId, String leaderName, Map<UUID, TeamMemberData> members) implements CustomPayload {
    private static final Identifier TEAM_DATA_PAYLOAD_ID = Identifier.of(Main.MOD_ID, "team_data");

    public static final CustomPayload.Id<TeamDataPayload> ID = new CustomPayload.Id<>(TEAM_DATA_PAYLOAD_ID);

    public static final PacketCodec<RegistryByteBuf, TeamDataPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString),
            TeamDataPayload::teamId,

            PacketCodecs.STRING,
            TeamDataPayload::teamName,

            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString), // leaderId
            TeamDataPayload::leaderId,

            PacketCodecs.STRING,
            TeamDataPayload::leaderName,

            PacketCodecs.map(
                    HashMap::new,
                    PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString),
                    TeamMemberData.CODEC
            ),
            TeamDataPayload::members,

            TeamDataPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
