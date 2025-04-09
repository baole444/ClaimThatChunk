package Sky.Cat.CTC.networking.payload;

import Sky.Cat.CTC.Main;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Record of the team member's update packet for synchronization.
 * @param teamId uuid of the team.
 * @param playerUUID uuid of the player.
 * @param playerName name of the player.
 * @param isJoining is the player joining or leaving the team?
 * @param permissionFlags permission flag number of the player.
 */
public record TeamMemberUpdatePayload(UUID teamId, UUID playerUUID, String playerName, boolean isJoining, int permissionFlags) implements CustomPayload {
    private static final Identifier TEAM_MEMBER_UPDATE_PAYLOAD_ID = Identifier.of(Main.MOD_ID, "team_member_update");

    public static final CustomPayload.Id<TeamMemberUpdatePayload> ID = new CustomPayload.Id<>(TEAM_MEMBER_UPDATE_PAYLOAD_ID);

    public static final PacketCodec<RegistryByteBuf, TeamMemberUpdatePayload> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString),
            TeamMemberUpdatePayload::teamId,

            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString),
            TeamMemberUpdatePayload::playerUUID,

            PacketCodecs.STRING,
            TeamMemberUpdatePayload::playerName,

            PacketCodecs.BOOLEAN,
            TeamMemberUpdatePayload::isJoining,

            PacketCodecs.INTEGER,
            TeamMemberUpdatePayload::permissionFlags,

            TeamMemberUpdatePayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
