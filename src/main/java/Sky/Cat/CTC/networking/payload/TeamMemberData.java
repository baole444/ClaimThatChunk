package Sky.Cat.CTC.networking.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.UUID;

/**
 * A record of TeamMember built for Minecraft's packet.
 * @param playerUUID uuid of the player.
 * @param playerName name of the player
 * @param permissionFlags permission flag number of the player.
 */
public record TeamMemberData(UUID playerUUID, String playerName, int permissionFlags) {
    public static final PacketCodec<RegistryByteBuf, TeamMemberData> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString),
            TeamMemberData::playerUUID,

            PacketCodecs.STRING,
            TeamMemberData::playerName,

            PacketCodecs.INTEGER,
            TeamMemberData::permissionFlags,

            TeamMemberData::new
    );
}