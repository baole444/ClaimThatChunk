package Sky.Cat.CTC.client.networking;

import Sky.Cat.CTC.client.team.TeamClientData;
import Sky.Cat.CTC.networking.payload.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import static Sky.Cat.CTC.Utilities.intToPermission;

public class TeamClientNetworking {

    /**
     * Register client-side network handlers.
     */
    public static void register() {
        // Receive team update.
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

        // Receive default permissions.
        ClientPlayNetworking.registerGlobalReceiver(TeamDefaultPermissionPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                TeamClientData.getInstance().updateDefaultPermission(
                        payload.permissionFlags()
                );
            });
        });

        // Receive team member update.
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

        // Receive team disband update.
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
