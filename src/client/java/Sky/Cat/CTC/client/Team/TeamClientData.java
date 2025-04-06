package Sky.Cat.CTC.client.Team;

import Sky.Cat.CTC.client.networking.TeamClientNetworking;
import Sky.Cat.CTC.networking.payload.TeamMemberData;
import Sky.Cat.CTC.permission.Permission;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side caching for team data.
 * Store team information of which the client received from the server.
 */
public class TeamClientData {
    private static TeamClientData TeamClientDataInstance;

    // Current player's team data.
    private UUID teamId;
    private String teamName;
    private UUID leaderId;
    private String leaderName;
    private final Map<UUID, ClientTeamMember> members = new HashMap<>();
    private Permission defaultPermission;

    // Flag for checking if the client is in a team or not.
    private boolean hasTeam = false;

    private TeamClientData() {} // Singleton pattern.

    public static TeamClientData getInstance() {
        if (TeamClientDataInstance == null) {
            TeamClientDataInstance = new TeamClientData();
        }

        return TeamClientDataInstance;
    }

    /**
     * Update team data with information received from the server.
     */
    public void updateTeamData(UUID teamId, String teamName, UUID leaderId, String leaderName, Map<UUID, TeamMemberData> members) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.leaderId = leaderId;
        this.leaderName = leaderName;

        // Convert network member data to client member objects.
        this.members.clear();

        for (Map.Entry<UUID, TeamMemberData> entry : members.entrySet()) {
            TeamMemberData data = entry.getValue();
            this.members.put(entry.getKey(), new ClientTeamMember(
                    data.playerUUID(),
                    data.playerName(),
                    TeamClientNetworking.intToPermission(data.permissionFlags())
            ));
        }

        this.hasTeam = true;
    }

    /**
     * Update the team client default permission
     * @param permissionFlag the permission flag received from the server.
     */
    public void updateDefaultPermission(int permissionFlag) {
        this.defaultPermission = TeamClientNetworking.intToPermission(permissionFlag);
    }

    /**
     * Add or update a team member.
     */
    public void addOrUpdateMember(UUID teamId, UUID playerUUID, String playerName, Permission permission) {
        if (this.hasTeam && this.teamId.equals(teamId)) {
            this.members.put(playerUUID, new ClientTeamMember(playerUUID, playerName, permission));
        }
    }

    /**
     * Remove a team member.
     */
    public void removeMember(UUID teamId, UUID playerUUID) {
        if (this.hasTeam && this.teamId.equals(teamId)) {
            this.members.remove(playerUUID);
        }
    }

    /**
     * Clear all team data.
     * Called when the player leaves a team or logout.
     */
    public void clearTeamData() {
        this.teamId = null;
        this.teamName = null;
        this.leaderId = null;
        this.leaderName = null;
        this.members.clear();
        this.hasTeam = false;
    }

    public boolean hasTeam() {
        return hasTeam;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public String getLeaderName() {
        return leaderName;
    }

    /**
     * Get the client's team members.
     */
    public Map<UUID, ClientTeamMember> getMembers() {
        return members;
    }

    /**
     * Check if the client is the team leader
     */
    public boolean isLeader(UUID playerUUID) {
        return hasTeam && leaderId.equals(playerUUID);
    }

    /**
     * Get a team member by UUID
     */
    public ClientTeamMember getMember(UUID playerUUID) {
        return members.get(playerUUID);
    }
}
