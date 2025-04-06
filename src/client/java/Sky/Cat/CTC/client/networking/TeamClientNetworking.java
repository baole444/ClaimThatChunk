package Sky.Cat.CTC.client.networking;

import Sky.Cat.CTC.client.Team.TeamClientData;
import Sky.Cat.CTC.networking.payload.RequestTeamDataPayload;
import Sky.Cat.CTC.networking.payload.TeamDataPayload;
import Sky.Cat.CTC.networking.payload.TeamDisbandPayload;
import Sky.Cat.CTC.networking.payload.TeamMemberUpdatePayload;
import Sky.Cat.CTC.permission.PermType;
import Sky.Cat.CTC.permission.Permission;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class TeamClientNetworking {

    /**
     * Register client-side network handlers.
     */
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(TeamDataPayload.ID, (payload, context) -> {
           context.client().execute(() -> {
               TeamClientData.getInstance().updateTeamData(
                       payload.teamId(),
                       payload.teamName(),
                       payload.leaderId(),
                       payload.leaderName(),
                       payload.members()
               );
           });
        });

        ClientPlayNetworking.registerGlobalReceiver(TeamMemberUpdatePayload.ID, (payload, context) -> {
           context.client().execute(() -> {
               if (payload.isJoining()) {
                   TeamClientData.getInstance().addOrUpdateMember(
                           payload.teamId(),
                           payload.playerUUID(),
                           payload.playerName(),
                           intToPermission(payload.permissionFlags())
                   );
               } else {
                   TeamClientData.getInstance().removeMember(
                           payload.teamId(),
                           payload.playerUUID()
                   );
               }
           });
        });

        ClientPlayNetworking.registerGlobalReceiver(TeamDisbandPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                TeamClientData teamClientData = TeamClientData.getInstance();

                // Is the client part of the disbanded team?
                if (teamClientData.hasTeam() && teamClientData.getTeamId().equals(payload.teamId())) {
                    teamClientData.clearTeamData();

                    context.player().sendMessage(Text.literal("Your team has been disbanded."), false);
                }

            });
        });
    }

    /**
     * Convert the integer permission flags back to Permission Object.
     */
    public static Permission intToPermission(int flags) {
        Permission permission = new Permission();

        permission.setPermission(PermType.INVITE, (flags & 1) != 0);
        permission.setPermission(PermType.KICK, (flags & 2) != 0);
        permission.setPermission(PermType.CLAIM, (flags & 4) != 0);
        permission.setPermission(PermType.BUILD, (flags & 8) != 0);
        permission.setPermission(PermType.BREAK, (flags & 16) != 0);
        permission.setPermission(PermType.INTERACT, (flags & 32) != 0);
        permission.setPermission(PermType.MODIFY_PERMISSION, (flags & 64) != 0);
        permission.setPermission(PermType.DISBAND, (flags & 128) != 0);

        return permission;
    }

    /**
     * Request team data from the server.
     * Called when player logged in or on demand.
     */
    public static void requestTeamData() {
        // Check if the client is ready
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.getNetworkHandler() != null) {
            ClientPlayNetworking.send(new RequestTeamDataPayload());
        }
    }
}
