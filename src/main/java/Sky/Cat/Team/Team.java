package Sky.Cat.Team;

import Sky.Cat.CTC.permission.PermType;
import Sky.Cat.CTC.permission.Permission;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Team {
    // UUID assigned to the team.
    private UUID teamId;

    // The name of the team
    // TODO: If not specified, use leader's name + team, for example "Wuan_Si's Team"
    private String teamName;

    // The leader of the team, often the team's original creator unless transferred.
    private UUID leaderUUID;

    // The Name of the leader. Can be useful for display methods.
    private String leaderName;

    // Members of the team.
    private Map<UUID, TeamMember> teamMember;

    // Default setup permission for team member.
    private Permission defaultPermission;

    // Limit the number of member a team can have
    // Store in config, not editable by player. l
    // Limit can be updated by server.
    // TODO: if 0, team have no size limit. Team limit can store in config. Add exception to prevent updating new limit from affecting old team.
    private int teamSizeLimit;

    // Time stamp of when the team was created, can be pass into to create a Date object for display purpose.
    private long createTime;

    //
    public Team(UUID leaderID, String leaderName, String teamName) {
        this.teamId = UUID.randomUUID();

        if (teamName.isBlank()) {
            this.teamName = leaderName +  "'s Team";
        } else {
            this.teamName = teamName;
        }

        this.leaderUUID = leaderID;
        this.leaderName = leaderName;

        this.teamMember = new HashMap<>();

        // Set the team default permission which will be assigned to newly added member.
        this.defaultPermission = new Permission();
        defaultPermission.setPermission(PermType.BUILD, true);
        defaultPermission.setPermission(PermType.INTERACT, true);

        this.createTime = System.currentTimeMillis();

        // Add leader to the team member
        TeamMember leader = new TeamMember(leaderUUID, leaderName);
        leader.getPermission().grantAllPermission();
        this.teamMember.put(leaderUUID, leader);
    }

    // Deserialization method for Team codec.
    public static Team fromCodec(UUID teamId, String teamName, UUID leaderUUID, String leaderName, Map<UUID, TeamMember> members, Permission defaultPermission, int teamSizeLimit, long createTime) {
        // Create a team normally.
        Team team = new Team(leaderUUID, leaderName, teamName);

        // Override its uuid.
        team.teamId = teamId;

        // Override its members.
        team.teamMember = new HashMap<>(members);

        // Override its default permissions.
        team.defaultPermission = defaultPermission;

        // Override its size limit.
        team.teamSizeLimit = teamSizeLimit;

        // Override its created time.
        team.createTime = createTime;

        return team;
    }

    public boolean addMember(UUID uuid, String name) {
        if (teamMember.containsKey(uuid)) return false;

        if (teamSizeLimit > 0 && teamMember.size() >= teamSizeLimit) return false;

        TeamMember newMember = new TeamMember(uuid, name, defaultPermission);
        teamMember.put(uuid, newMember);

        return true;
    }

    /**
     * Remove a member of the team.
     * @param uuid the uuid of the member to be removed.
     * @return false if it is the team leader / don't exist. True when successfully removed a member.
     */
    public boolean removeMember(UUID uuid) {
        if (uuid.equals(leaderUUID)) {
            return false; // Cannot remove the current leader.
        }

        // Return true on removing existing uuid.
        return teamMember.remove(uuid) != null;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public UUID getLeaderUUID() {
        return leaderUUID;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public Map<UUID, TeamMember> getTeamMember() {
        return teamMember;
    }

    public Permission getDefaultPermission() {
        return defaultPermission;
    }

    public int getTeamSizeLimit() {
        return teamSizeLimit;
    }

    public long getCreateTime() {
        return createTime;
    }
}
