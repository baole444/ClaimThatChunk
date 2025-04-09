package Sky.Cat.CTC.team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import Sky.Cat.CTC.Main;
import Sky.Cat.CTC.networking.TeamNetworking;
import Sky.Cat.CTC.permission.PermType;
import Sky.Cat.CTC.permission.Permission;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeamManager{
    // The singleton instance of TeamManager.
    private static TeamManager ManagerInstance;

    // Mapping between player's uuid and team uuid.
    private final Map<UUID, UUID> playerTeamMap;

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger("ClaimThatChunk/TeamManager");

    // Reference for persistent state
    private TeamState teamState;

    // Private constructor for singleton
    private TeamManager() {
        this.playerTeamMap = new ConcurrentHashMap<>();
    }

    /**
     * Get or create the singleton instance of TeamManager.
     * @return the TeamManager instance.
     */
    public static TeamManager getInstance() {
        if (ManagerInstance == null) {
            ManagerInstance = new TeamManager();
            LOGGER.info("Team Manager initialization completed");
        }
        return ManagerInstance;
    }

    /**
     * Create a new team with the creator as the leader.
     * @param creatorId the uuid of the player that created the team.
     * @param creatorName the name of the player that created the team.
     * @param teamName the name of the team.
     * @return the newly created team or null if player already in a team.
     */
    public Team createTeam(UUID creatorId, String creatorName, String teamName) {
        if (playerTeamMap.containsKey(creatorId)) {
            return null; // Cannot create team when is already in one.
        }

        Team newTeam = new Team(creatorId, creatorName, teamName);

        teamState.addTeam(newTeam);

        playerTeamMap.put(creatorId, newTeam.getTeamId());

        // Update to the team creator
        ServerPlayerEntity player = Main.getServer().getPlayerManager().getPlayer(creatorId);

        if (player != null) {
            TeamNetworking.sendTeamDataToPlayer(player, newTeam);
            TeamNetworking.broadcastTeamDefaultPermissionUpdate(newTeam);
        }
        
        return newTeam;
    }

    /**
     * Disband a team and remove all its members.
     * @param teamId the uuid of the team to disband.
     * @return true on disbanding successfully, false if no team founded.
     */
    public boolean disbandTeam(UUID teamId) {
        Team disbandTeam = getTeamById(teamId);

        if (disbandTeam == null) return false;

        TeamNetworking.broadcastTeamDisbanded(disbandTeam);

        for (UUID playerId : disbandTeam.getTeamMember().keySet()) {
            playerTeamMap.remove(playerId);
        }

        return teamState.removeTeam(teamId);
    }

    /**
     * Get a team via its uuid.
     * @param teamId targeted team's uuid.
     * @return The team or null if such a team doesn't exist.
     */
    public Team getTeamById(UUID teamId) {
        return teamState.getTeams().get(teamId);
    }

    /**
     * Get a team via a player's uuid.
     * @param playerUUID the player's uuid
     * @return The team the player belong to or null if the player doesn't belong to any team.
     */
    public Team getTeamByPlayer(UUID playerUUID) {
        UUID teamId = playerTeamMap.get(playerUUID);
        if (teamId == null) {
            return null;
        }
        return getTeamById(teamId);
    }

    /**
     * Change the team's name.
     * @param teamId uuid of the team that has its name changed.
     * @param newName the new name of the team.
     * @return true when changed successfully. If the team doesn't exist, return false.
     */
    public boolean updateTeamName(UUID teamId, String newName) {
        Team team = getTeamById(teamId);

        if (team == null) return false;

        team.setTeamName(newName);

        teamState.markDirty();

        for (UUID memberId : team.getTeamMember().keySet()) {
            ServerPlayerEntity player = Main.getServer().getPlayerManager().getPlayer(memberId);

            if (player != null) {
                TeamNetworking.sendTeamDataToPlayer(player, team);
            }
        }

        return true;
    }

    /**
     * Update team default permissions.
     * @param teamId uuid of the team that has its default permissions updated.
     * @param newDefaultPermission the default permission for the team.
     * @return true if update successfully. If the team doesn't exist, return false.
     */
    public boolean updateTeamDefaultPermission(UUID teamId, Permission newDefaultPermission) {
        Team team = getTeamById(teamId);

        if (team == null) return false;

        team.setDefaultPermission(newDefaultPermission);

        teamState.markDirty();

        TeamNetworking.broadcastTeamDefaultPermissionUpdate(team);

        return true;
    }

    /***
     * Add a player to a team.
     * @param teamId targeted team to add the player to.
     * @param playerUUID uuid of the player that will be added.
     * @param playerName name of the player.
     * @return true when added successfully. If the team doesn't exist or player already belongs to a team, then false.
     */
    public boolean addPlayerToTeam(UUID teamId, UUID playerUUID, String playerName) {
        if (playerTeamMap.containsKey(playerUUID)) return false; // Player already belong to a team

        Team team = getTeamById(teamId);
        if (team == null) return false; // There is no such team in data.

        boolean result = team.addMember(playerUUID, playerName);

        if (result) {
            playerTeamMap.put(playerUUID, teamId);
            teamState.markDirty();

            notifyTeamMemberJoined(team, playerUUID, playerName);
        }

        return result;
    }

    /**
     * Update a team member's permission.
     * @param teamId uuid of the team the member belong to.
     * @param playerUUID uuid of the player that will have their permission updated.
     * @param newPermission the new permission for the player
     * @return true when update successfully. If the team doesn't exist or the player is not part of the team, return false.
     */
    public boolean updateMemberPermission(UUID teamId, UUID playerUUID, Permission newPermission) {
        Team team = getTeamById(teamId);

        if (team == null) return false;

        TeamMember member = team.getTeamMember().get(playerUUID);

        if (member == null) return false;

        member.setPermission(newPermission);

        teamState.markDirty();

        TeamNetworking.broadcastTeamMemberUpdate(
                team,
                playerUUID,
                member.getPlayerName(),
                true, // Not really joining, but true will trigger update or create on client side
                newPermission
        );

        return true;
    }

    /**
     * Remove a player from a team.
     * @param kickerUUID uuid of the player that execute the command.
     * @param targetUUID uuid of the player that will be removed.
     * @return true when remove successfully.
     * If the team doesn't exist, return false.
     * Forcefully removed if the team they are in doesn't exist in data.
     */
    public boolean kickPlayerFromTeam(UUID kickerUUID, UUID targetUUID) {
        Team team = getTeamByPlayer(kickerUUID);

        if (team == null) return false;

        TeamMember kicker = team.getTeamMember().get(kickerUUID);
        if (kicker == null || !kicker.getPermission().hasPermission(PermType.KICK)) return false;

        UUID targetTeamId = playerTeamMap.get(targetUUID);

        if (targetTeamId == null || !targetTeamId.equals(team.getTeamId())) return false;

        String playerName = team.getTeamMember().get(targetUUID).getPlayerName();

        boolean result = team.removeMember(targetUUID);

        if (result) {
            playerTeamMap.remove(targetUUID);

            teamState.markDirty();

            notifyTeamMemberLeft(team, targetUUID, playerName);
        }

        return result;
    }

    /**
     * For when the player leaves the team on their own choice.
     */
    public boolean leaveTeam(UUID playerUUID) {
        return handlePlayerLeavingTeam(playerUUID);
    }

    /**
     * Handle a player leaving their team on their own.
     * @param playerUUID uuid of the leaving player.
     * @return true if removed successfully.
     */
    public boolean handlePlayerLeavingTeam(UUID playerUUID) {
        boolean success = false;

        UUID teamId = playerTeamMap.get(playerUUID);

        if (teamId == null) return success;

        Team team = getTeamById(teamId);

        // Prevent out of management modification.
        if (team == null) {
            playerTeamMap.remove(playerUUID);
            return success;
        }

        String playerName = team.getTeamMember().get(playerUUID).getPlayerName();

        boolean isLeader = playerUUID.equals(team.getLeaderUUID());

        // Case 1: there are still members left in the team.
        if (isLeader && team.getTeamMember().size() > 1) {
            UUID newLeadId = null;
            TeamMember newLeader = null;

            // Get the first member in entries and make them new leader
            for (Map.Entry<UUID, TeamMember> entry : team.getTeamMember().entrySet()) {
                if (!entry.getKey().equals(playerUUID)) {
                    newLeadId = entry.getKey();
                    newLeader = entry.getValue();
                    break;
                }
            }

            team.setLeaderUUID(newLeadId).setLeaderName(newLeader.getPlayerName());
            newLeader.getPermission().grantAllPermission();

            // Remove the old leader as normal member.
            team.removeMember(playerUUID);

            playerTeamMap.remove(playerUUID);

            teamState.markDirty();

            // Notify team's members about this change
            for (UUID memberId : team.getTeamMember().keySet()) {
                ServerPlayerEntity player = Main.getServer().getPlayerManager().getPlayer(memberId);
                if (player != null) {
                    TeamNetworking.sendTeamDataToPlayer(player, team);
                }
            }

            // Notify the leaving player
            notifyTeamMemberLeft(team, playerUUID, playerName);

            success = true;
        }

        // Case 2: there is no one else left in the team after they left.
        else if (isLeader && team.getTeamMember().size() <= 1) {
            TeamNetworking.broadcastTeamDisbanded(team);

            playerTeamMap.remove(playerUUID);

            teamState.removeTeam(teamId);

            success = true;
        }

        else {
            success = team.removeMember(playerUUID);

            if (success) {
                playerTeamMap.remove(playerUUID);

                teamState.markDirty();

                notifyTeamMemberLeft(team, playerUUID, playerName);
            }
        }

        return success;
    }

    /**
     * Transfer the team's leadership to another member.
     * @param teamId uuid of the team that will have its leadership transferred.
     * @param newLeaderId uuid of the team member to transfer the leadership to.
     * @return true when transfer successfully. If the team doesn't exist or the new leader is not part of the team, return false.
     */
    public boolean transferLeadership(UUID teamId, UUID newLeaderId) {
        Team team = getTeamById(teamId);

        // If the team is null or transferring to a member outside the team.
        if (team == null || !team.getTeamMember().containsKey(newLeaderId)) return false;

        // Get the current leader's uuid for permission transfer.
        UUID oldLeaderId = team.getLeaderUUID();

        String newLeaderName = team.getTeamMember().get(newLeaderId).getPlayerName();

        // Update team leadership parameters
        team.setLeaderUUID(newLeaderId).setLeaderName(newLeaderName);

        // Update permissions of new leader and the old leader now is normal a member.
        team.getTeamMember().get(newLeaderId).getPermission().grantAllPermission();

        TeamMember oldLeader = team.getTeamMember().get(oldLeaderId);

        Permission defaultPerm = new Permission();
        for (PermType type : PermType.values()) {
            defaultPerm.setPermission(type, team.getDefaultPermission().hasPermission(type));
        }

        oldLeader.setPermission(defaultPerm);

        teamState.markDirty();

        // Send update to all team's members.
        for (UUID memberId : team.getTeamMember().keySet()) {
            ServerPlayerEntity player = Main.getServer().getPlayerManager().getPlayer(memberId);
            if (player != null) {
                TeamNetworking.sendTeamDataToPlayer(player, team);
            }
        }

        return true;
    }

    /***
     * Rebuild player-team mapping from teams data.
     */
    private boolean rebuildPlayerTeamMap() {
        playerTeamMap.clear();

        for (Map.Entry<UUID, Team> entry : teamState.getTeams().entrySet()) {
            UUID teamId = entry.getKey();
            Team team = entry.getValue();

            for (UUID playerUUID : team.getTeamMember().keySet()) {
                playerTeamMap.put(playerUUID, teamId);
            }
        }

        LOGGER.info("Rebuilt player-team map with {} player mappings", playerTeamMap.size());

        return true;
    }

    /**
     * Load teams from persistent storage.
     * Called by server lifecycle event.
     */
    public void loadTeams() {
        if (Main.getServer() == null) {
            LOGGER.error("Cannot load team: server is null");
            return;
        }

        this.teamState = TeamState.getOrCreate(Main.getServer());

        boolean success = rebuildPlayerTeamMap();

        if (!success) {
            LOGGER.error("Failed to rebuild player-team mapping");
        }

        LOGGER.info("Loaded {} teams from storage", teamState.getTeams().size());
    }

    // For backward compatibility
    public void saveTeams() {
        LOGGER.info("Team saving is done automatically");
    }

    // When a member joins a team.
    private void notifyTeamMemberJoined(Team team, UUID playerUUID, String playerName) {
        TeamMember member = team.getTeamMember().get(playerUUID);

        if (member != null) {
            TeamNetworking.broadcastTeamMemberUpdate(
                    team,
                    playerUUID,
                    playerName,
                    true,
                    member.getPermission()
            );
        }
    }

    // When a member leaves a team.
    private void notifyTeamMemberLeft(Team team, UUID playerUUID, String playerName) {
        TeamNetworking.broadcastTeamMemberUpdate(
                team,
                playerUUID,
                playerName,
                false,
                new Permission()
        );
    }

    // When a player log in.
    // Called from Main connection event.
    public void sendTeamDataToPlayer(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        Team team = getTeamByPlayer(playerId);

        if (team != null) {
            TeamNetworking.sendTeamDataToPlayer(player, team);
        }
    }

}
