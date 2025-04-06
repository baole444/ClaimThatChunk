package Sky.Cat.CTC.networking.payload;

import Sky.Cat.CTC.Main;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record TeamDefaultPermissionPayload(UUID teamId, int permissionFlags) implements CustomPayload {
    private static final Identifier TEAM_DEFAULT_PERMISSION_PAYLOAD_ID = Identifier.of(Main.MOD_ID, "team_default_permission");

    public static final CustomPayload.Id<TeamDefaultPermissionPayload> ID = new CustomPayload.Id<>(TEAM_DEFAULT_PERMISSION_PAYLOAD_ID);

    public static final PacketCodec<RegistryByteBuf, TeamDefaultPermissionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString),
            TeamDefaultPermissionPayload::teamId,

            PacketCodecs.INTEGER,
            TeamDefaultPermissionPayload::permissionFlags,

            TeamDefaultPermissionPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
