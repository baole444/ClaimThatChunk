package Sky.Cat.CTC.networking;

import Sky.Cat.CTC.Main;
import Sky.Cat.CTC.Team.Team;
import Sky.Cat.CTC.Team.TeamManager;
import Sky.Cat.CTC.Team.TeamMember;
import Sky.Cat.CTC.networking.payload.*;
import Sky.Cat.CTC.permission.PermType;
import Sky.Cat.CTC.permission.Permission;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamNetworking {

    // Convert a permission object to integer representation.
    private static int permissionToInt(Permission permission) {
        int flags = 0;

        // Could replace this with bit shift operation in a for loop on a perm type.
        // However, it will be sensitive to the enum order.

        // int bit = 1;
        // for (PermType type : PermType.values()) {
        //     if (permission.hasPermission(type)) {
        //         flag |= bit;
        //     }
        //     bit <<= 1; // shifting bit to the left to move to the next value
        // }

        if (permission.hasPermission(PermType.INVITE)) flags |= 1;
        if (permission.hasPermission(PermType.KICK)) flags |= 2;
        if (permission.hasPermission(PermType.CLAIM)) flags |= 4;
        if (permission.hasPermission(PermType.BUILD)) flags |= 8;
        if (permission.hasPermission(PermType.BREAK)) flags |= 16;
        if (permission.hasPermission(PermType.INTERACT)) flags |= 32;
        if (permission.hasPermission(PermType.MODIFY_PERMISSION)) flags |= 64;
        if (permission.hasPermission(PermType.DISBAND)) flags |= 128;

        return flags;
    }

    // Register handlers method.
    public static void register() {
        PayloadTypeRegistry.playS2C().register(TeamDataPayload.ID, TeamDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TeamMemberUpdatePayload.ID, TeamMemberUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TeamDisbandPayload.ID, TeamDisbandPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestTeamDataPayload.ID, RequestTeamDataPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RequestTeamDataPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                Team team = TeamManager.getInstance().getTeamByPlayer(player.getUuid());
                if (team != null) {
                    sendTeamDataToPlayer(player, team);
                }
            });
        });
    }

    public static void sendTeamDataToPlayer(ServerPlayerEntity player, Team team) {
        Map<UUID, TeamMemberData> memberData = new HashMap<>();
        for (Map.Entry<UUID, TeamMember> entry : team.getTeamMember().entrySet()) {
            TeamMember member = entry.getValue();
            memberData.put(entry.getKey(), new TeamMemberData(
                    member.getPlayerUUID(),
                    member.getPlayerName(),
                    permissionToInt(member.getPermission())
            ));
        }

        // Create and send payload
        TeamDataPayload payload = new TeamDataPayload(
                team.getTeamId(),
                team.getTeamName(),
                team.getLeaderUUID(),
                team.getLeaderName(),
                memberData
        );

        ServerPlayNetworking.send(player, payload);
    }

    /**
     * Inform all clients about changes in a team's member.
     * @param team the team which the update happened.
     * @param playerUUID uuid of the player joining or leaving the team.
     * @param playerName name of the player joining or leaving the team.
     * @param isJoining is the player joining or leaving the team?
     * @param permission permission of the player.
     */
    public static void broadcastTeamMemberUpdate(Team team, UUID playerUUID, String playerName, boolean isJoining, Permission permission) {
        TeamMemberUpdatePayload payload = new TeamMemberUpdatePayload(
                team.getTeamId(),
                playerUUID,
                playerName,
                isJoining,
                permissionToInt(permission)
        );

        for (UUID memberId : team.getTeamMember().keySet()) {
            ServerPlayerEntity player = Main.getServer().getPlayerManager().getPlayer(memberId);
            if (player != null) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void broadcastTeamDisbanded(Team team) {
        TeamDisbandPayload payload = new TeamDisbandPayload(team.getTeamId());

        for (UUID memberId : team.getTeamMember().keySet()) {
            ServerPlayerEntity player = Main.getServer().getPlayerManager().getPlayer(memberId);
            if (player != null) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
}

